package app.reelstack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.resolvedMediaType
import app.reelstack.data.model.seerrStatusLabel
import app.reelstack.data.model.seerrStatusDescription
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.canRequestType
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.RequestSeason
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.network.EndpointValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

enum class AppTab { HOME, DISCOVER, ACTIVITY, SETTINGS }

enum class ConnectionAuthMode { QUICK_CONNECT, ACCOUNT, API_KEY }

sealed interface AppSheet {
    data class SessionDetails(val sessionKey: String) : AppSheet
    data class TitleDetails(val key: String) : AppSheet
    data object RequestComposer : AppSheet
    data object UpcomingCalendar : AppSheet
    data class ConnectionEditor(val kind: ServiceKind) : AppSheet
}

data class ConnectionDraft(
    val kind: ServiceKind,
    val name: String,
    val url: String,
    val token: String,
    val userId: String = "",
    val authMode: ConnectionAuthMode = ConnectionAuthMode.API_KEY,
    val username: String = "",
    val password: String = "",
    val alsoConnect: Boolean = false,
    val companionUrl: String = "",
    val alternateUrl: String = "",
    val saving: Boolean = false,
    val quickConnectCode: String? = null,
    val quickConnectWaiting: Boolean = false,
    val error: String? = null,
    val warning: String? = null,
)

data class ReelstackUiState(
    val showOnboarding: Boolean = false,
    val selectedTab: AppTab = AppTab.HOME,
    val activeSheet: AppSheet? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val accounts: Map<ServiceKind, ServiceAccount> = emptyMap(),
    val accountErrors: Map<ServiceKind, String> = emptyMap(),
    val loadingAccounts: Set<ServiceKind> = emptySet(),
    val sessions: List<PlaybackSession> = demoSessions(),
    val resume: List<LibraryMedia> = demoResume(),
    val recentMovies: List<LibraryMedia> = demoRecentMovies(),
    val recentSeries: List<LibraryMedia> = demoRecentSeries(),
    val upcoming: List<UpcomingMedia> = demoUpcoming(),
    val recentReleases: List<UpcomingMedia> = demoRecentReleases(),
    val upcomingError: String? = null,
    val recentReleasesError: String? = null,
    val incoming: List<IncomingMedia> = demoIncoming(),
    val discover: List<DiscoverMedia> = demoDiscover(),
    val recommendations: List<DiscoverMedia> = demoRecommendations(),
    val searchResults: List<DiscoverMedia> = emptyList(),
    /** Hits from your own Jellyfin/Emby libraries, shown above the Seerr results. */
    val librarySearchResults: List<LibraryMedia> = emptyList(),
    val searchPage: Int = 1,
    val searchHasMore: Boolean = false,
    val loadingMoreSearch: Boolean = false,
    val activity: List<ActivityEvent> = demoActivity(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val notificationsEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val isRefreshing: Boolean = false,
    val liveSession: Boolean = false,
    val liveLibrary: Boolean = false,
    val liveIncoming: Boolean = false,
    val liveDiscover: Boolean = false,
    val liveActivity: Boolean = false,
    val lastUpdatedEpochMillis: Long? = null,
    val failedServices: Set<ServiceKind> = emptySet(),
    val serviceWarnings: Map<ServiceKind, String> = emptyMap(),
    val requestingMediaIds: Set<String> = emptySet(),
    val pendingSessionKey: String? = null,
    val homeSections: Set<HomeSection> = HomeSection.entries.toSet(),
    val hasCachedData: Boolean = false,
    val snackbar: String? = null,
    val contentDetails: ContentDetails? = null,
    val returnToCalendar: Boolean = false,
    val requestDraft: RequestDraft? = null,
    val trackedRequests: List<TrackedRequest> = emptyList(),
    val trackingError: String? = null,
    val trackingLoading: Boolean = false,
    /** Keys of follows currently being withdrawn, so a card cannot be cancelled twice. */
    val cancellingRequestKeys: Set<String> = emptySet(),
    val adminView: Boolean = false,
) {
    val visibleDiscover: List<DiscoverMedia>
        get() = if (searchQuery.isBlank()) discover else searchResults

    val configuredCount: Int
        get() = connections.count { it.baseUrl.isNotBlank() }

    fun canEditConnection(kind: ServiceKind): Boolean = kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR) ||
        connections.none { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() } || adminView

    val onlineCount: Int
        get() = connections.count { it.state == ConnectionState.CONNECTED }

}

class ReelstackViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(container))
    val uiState: StateFlow<ReelstackUiState> = _uiState.asStateFlow()

    var connectionDraft = MutableStateFlow<ConnectionDraft?>(null)
        private set

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var quickConnectJob: Job? = null
    private var connectionJob: Job? = null
    private var accountsJob: Job? = null
    private var trackingJob: Job? = null
    private var requestDraftJob: Job? = null

    init {
        refreshLiveData()
    }

    fun selectTab(tab: AppTab) = _uiState.update { it.copy(selectedTab = tab, activeSheet = null, returnToCalendar = false) }

    fun completeOnboarding() {
        container.preferencesRepository.onboardingCompleted = true
        _uiState.update { it.copy(showOnboarding = false, selectedTab = AppTab.HOME) }
    }

    fun openSeerrAccount() {
        openSheet(AppSheet.ConnectionEditor(ServiceKind.SEERR))
        if (_uiState.value.activeSheet == AppSheet.ConnectionEditor(ServiceKind.SEERR)) {
            updateConnectionAuthMode(ConnectionAuthMode.ACCOUNT)
        }
    }

    fun openSheet(sheet: AppSheet) {
        if (sheet is AppSheet.ConnectionEditor) {
            if (!_uiState.value.canEditConnection(sheet.kind)) return
            if (_uiState.value.requestingMediaIds.isNotEmpty()) {
                _uiState.update { it.copy(snackbar = "Vent til førespurnaden er ferdig før du byter konto.") }
                return
            }
            connectionJob?.cancel()
            quickConnectJob?.cancel()
            val existing = _uiState.value.connections.first { it.kind == sheet.kind }
            connectionDraft.value = ConnectionDraft(
                kind = existing.kind,
                name = existing.name,
                url = existing.baseUrl.ifBlank { container.connectionRepository.rememberedUrl(existing.kind) },
                token = if (existing.sessionCookie || !_uiState.value.adminView) "" else existing.token,
                userId = existing.userId,
                alternateUrl = existing.alternateUrl,
                companionUrl = _uiState.value.connections.firstOrNull {
                    it.kind == if (existing.kind == ServiceKind.SEERR) ServiceKind.JELLYFIN else ServiceKind.SEERR
                }?.baseUrl.orEmpty(),
                authMode = if (existing.kind in setOf(ServiceKind.SEERR, ServiceKind.EMBY)) {
                    ConnectionAuthMode.ACCOUNT
                } else if (existing.kind == ServiceKind.JELLYFIN) {
                    ConnectionAuthMode.QUICK_CONNECT
                } else {
                    ConnectionAuthMode.API_KEY
                },
                warning = existing.baseUrl.takeIf(String::isNotBlank)?.let {
                    if (EndpointValidator.isCleartext(it)) "HTTP er ukryptert. Bruk helst HTTPS utanfor det trygge lokalnettet ditt." else null
                },
            )
        }
        _uiState.update { it.copy(activeSheet = sheet, returnToCalendar = false) }
    }

    fun closeSheet() {
        if (_uiState.value.requestDraft?.sending == true) return
        requestDraftJob?.cancel()
        _uiState.update { it.copy(requestDraft = null) }
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        _uiState.update { it.copy(activeSheet = null, contentDetails = null, returnToCalendar = false) }
        connectionDraft.value = null
    }

    fun backToCalendar() {
        _uiState.update { it.copy(activeSheet = AppSheet.UpcomingCalendar, contentDetails = null, returnToCalendar = false) }
    }

    fun openLibraryDetails(id: String) {
        val state = _uiState.value
        val media = (state.resume + state.recentMovies + state.recentSeries + state.librarySearchResults)
            .firstOrNull { it.id == id } ?: return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == media.source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.TitleDetails(media.id),
                contentDetails = ContentDetails(
                    key = media.id,
                    title = media.title,
                    eyebrow = "Bibliotek i ${media.source.displayName}",
                    subtitle = media.subtitle,
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    source = media.source,
                    mediaType = media.mediaType,
                    loading = connection != null && media.remoteId != null,
                    statusTitle = "I biblioteket",
                    libraryAvailable = true,
                    statusDescription = "Registrert i ${media.source.displayName}.",
                ),
            )
        }
        if (connection == null || media.remoteId == null) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.details(connection, media) }
            }
            _uiState.update { current ->
                val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                result.fold(
                    onSuccess = { remote ->
                        current.copy(
                            contentDetails = details.copy(
                                title = if (media.mediaType.equals("Episode", true) || media.mediaType.equals("Series", true)) {
                                    details.title
                                } else {
                                    remote.title ?: details.title
                                },
                                tagline = remote.tagline ?: details.tagline,
                                cast = remote.cast,
                                overview = remote.overview ?: details.overview,
                                facts = (remote.facts + details.facts).distinct(),
                                genres = (remote.genres + details.genres).distinct(),
                                artworkUrl = remote.artworkUrl ?: details.artworkUrl,
                                loading = false,
                            ),
                        )
                    },
                    onFailure = {
                        current.copy(contentDetails = details.copy(loading = false, error = "Fekk ikkje henta alle detaljane"))
                    },
                )
            }
        }
    }

    fun openDiscoverDetails(id: String) {
        val media = (_uiState.value.visibleDiscover + _uiState.value.recommendations).firstOrNull { it.id == id } ?: return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.TitleDetails(media.id),
                contentDetails = ContentDetails(
                    key = media.id,
                    title = media.title,
                    eyebrow = if (media.inLibrary) "I biblioteket ditt" else "Oppdag i Seerr",
                    subtitle = media.metadata,
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    source = ServiceKind.SEERR,
                    mediaType = resolvedMediaType(media.mediaType, media.metadata),
                    statusTitle = seerrStatusLabel(media.seerrStatus, media.inLibrary, media.requested),
                    libraryAvailable = media.inLibrary || media.seerrStatus == 5,
                    statusDescription = seerrStatusDescription(media.seerrStatus, media.inLibrary),
                    loading = connection != null && media.remoteId != null && media.mediaType != null,
                ),
            )
        }
        if (connection == null || media.remoteId == null || media.mediaType == null) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.details(connection, media) }
            }
            _uiState.update { current ->
                val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                result.fold(
                    onSuccess = { remote ->
                        current.copy(
                            contentDetails = details.copy(
                                title = remote.title ?: details.title,
                                statusTitle = remote.seerrStatus?.let { seerrStatusLabel(it) } ?: details.statusTitle,
                                libraryAvailable = remote.seerrStatus == 5,
                                statusDescription = remote.seerrStatus?.let { seerrStatusDescription(it) } ?: details.statusDescription,
                                tagline = remote.tagline ?: details.tagline,
                                cast = remote.cast,
                                overview = remote.overview ?: details.overview,
                                facts = (remote.facts + details.facts).distinct(),
                                genres = (remote.genres + details.genres).distinct(),
                                artworkUrl = remote.artworkUrl ?: details.artworkUrl,
                                loading = false,
                            ),
                            discover = current.discover.map { item ->
                                if (item.id == id && remote.seerrStatus != null) item.copy(seerrStatus = remote.seerrStatus,
                                    inLibrary = remote.seerrStatus == 5, requested = remote.seerrStatus in 2..4) else item
                            },
                            recommendations = current.recommendations.map { item ->
                                if (item.id == id && remote.seerrStatus != null) item.copy(seerrStatus = remote.seerrStatus,
                                    inLibrary = remote.seerrStatus == 5, requested = remote.seerrStatus in 2..4) else item
                            },
                            searchResults = current.searchResults.map { item ->
                                if (item.id == id && remote.seerrStatus != null) item.copy(seerrStatus = remote.seerrStatus,
                                    inLibrary = remote.seerrStatus == 5, requested = remote.seerrStatus in 2..4) else item
                            },
                        )
                    },
                    onFailure = {
                        current.copy(contentDetails = details.copy(loading = false, error = "Fekk ikkje henta alle detaljane"))
                    },
                )
            }
        }
    }

    fun openRecommendationDetails(id: String) {
        // Opening a Home recommendation must preserve the user's separate Discover search.
        if (_uiState.value.recommendations.none { it.id == id }) return
        openDiscoverDetails(id)
    }

    fun openUpcomingDetails(id: String) {
        val recent = _uiState.value.recentReleases.firstOrNull { it.id == id }
        val media = _uiState.value.upcoming.firstOrNull { it.id == id } ?: recent ?: return
        val fromCalendar = _uiState.value.activeSheet == AppSheet.UpcomingCalendar
        showLocalDetails(
            ContentDetails(
                key = media.id,
                title = media.title,
                eyebrow = if (recent == null) "Kjem snart · ${media.source.displayName}" else "Nyleg tilgjengeleg · ${media.source.displayName}",
                subtitle = media.subtitle,
                overview = media.overview ?: "Omtalen er ikkje tilgjengeleg frå ${media.source.displayName} enno.",
                facts = (media.facts + media.dateLabel + media.source.displayName).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = media.mediaType,
                statusTitle = if (recent == null) "Planlagd utgjeving" else if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) "I biblioteket" else "Heimeutgjeven",
                statusDescription = if (recent == null) {
                    "${media.dateLabel} · Datoen er venta, ikkje ei stadfesting på at tittelen er tilgjengeleg."
                } else {
                    if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
                        "Tilgjengeleg i ${media.source.displayName}. Datoen er utgjevingsdatoen, ikkje datoen tittelen vart lagd til."
                    else "${media.dateLabel} · Utgjevingsdato frå ${media.source.displayName}. Bibliotektilgjenge blir vist under Oppdag."
                },
            ),
        )
        _uiState.update { it.copy(returnToCalendar = fromCalendar) }
    }

    fun openIncomingDetails(id: String) {
        val media = _uiState.value.incoming.firstOrNull { it.id == id } ?: return
        showLocalDetails(
            ContentDetails(
                key = media.id,
                title = media.title,
                eyebrow = "Nedlasting · ${media.source.displayName}",
                subtitle = media.status,
                overview = media.overview ?: "Sjå framdrift og kjelde for denne tittelen.",
                facts = (media.facts + media.source.displayName + media.status).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = if (media.source == ServiceKind.RADARR) "Movie" else "Episode",
                statusTitle = media.status,
                statusDescription = "Siste rapporterte tilstand frå ${media.source.displayName}.",
            ),
        )
    }

    fun openActivityDetails(id: String) {
        _uiState.value.trackedRequests.firstOrNull { it.key == id }?.let { tracked ->
            val media = app.reelstack.data.model.DiscoverMedia(
                id = "seerr-${tracked.mediaType}-${tracked.mediaId}", title = tracked.title,
                metadata = if (tracked.mediaType == "tv") "Serie" else "Film", artworkRes = R.drawable.media_placeholder,
                artworkUrl = tracked.artworkUrl, inLibrary = tracked.stage == app.reelstack.data.model.RequestStage.AVAILABLE,
                remoteId = tracked.mediaId, mediaType = tracked.mediaType)
            _uiState.update { it.copy(discover = it.discover.filterNot { entry -> entry.id == media.id } + media, searchQuery = "") }
            openDiscoverDetails(media.id)
            return
        }
        val event = _uiState.value.activity.firstOrNull { it.id == id } ?: return
        showLocalDetails(
            ContentDetails(
                key = event.id,
                title = event.title,
                eyebrow = "Aktivitet${event.source?.let { " · ${it.displayName}" }.orEmpty()}",
                subtitle = event.detail,
                overview = "Denne hendinga vart registrert ${event.time.lowercase()}.",
                facts = listOfNotNull(event.source?.displayName, event.progress?.let { "$it %" }, event.time),
                artworkRes = event.artworkRes ?: R.drawable.media_placeholder,
                artworkUrl = event.artworkUrl,
                source = event.source,
                mediaType = if (event.source == ServiceKind.RADARR) "Movie" else null,
            ),
        )
    }

    private fun showLocalDetails(details: ContentDetails) {
        _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(details.key), contentDetails = details, returnToCalendar = false) }
    }

    fun togglePlayback(sessionKey: String) {
        val state = _uiState.value
        val session = state.sessions.firstOrNull { it.key == sessionKey } ?: return
        if (!state.liveSession) {
            _uiState.update { current ->
                current.copy(sessions = current.sessions.map { item ->
                    if (item.key == sessionKey) item.copy(paused = !item.paused) else item
                })
            }
            return
        }
        if (state.pendingSessionKey != null) return
        val targetPaused = !session.paused
        _uiState.update { it.copy(pendingSessionKey = sessionKey) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    container.mediaSyncRepository.setPlaybackPaused(
                        connections = _uiState.value.connections,
                        session = session,
                        paused = targetPaused,
                    )
                }
            }
            _uiState.update { current ->
                current.copy(
                    sessions = if (result.isSuccess) {
                        current.sessions.map { item -> if (item.key == sessionKey) item.copy(paused = targetPaused) else item }
                    } else current.sessions,
                    pendingSessionKey = null,
                    snackbar = if (result.isSuccess) {
                        if (targetPaused) "Avspelinga er sett på pause" else "Avspelinga held fram"
                    } else {
                        "Medietenaren klarte ikkje å endre avspelinga"
                    },
                )
            }
        }
    }

    fun setSearchQuery(value: String) {
        searchJob?.cancel()
        val query = value.trim()
        val state = _uiState.value
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val mediaServers = state.connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        if (query.isBlank()) {
            _uiState.update {
                it.copy(searchQuery = value, searchResults = emptyList(), librarySearchResults = emptyList(),
                    isSearching = false, searchError = null, searchPage = 1, searchHasMore = false)
            }
            return
        }
        if (seerr == null && mediaServers.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchQuery = value,
                    searchResults = it.discover.filter { media -> media.title.contains(query, ignoreCase = true) },
                    librarySearchResults = emptyList(),
                    isSearching = false,
                    searchError = null,
                )
            }
            return
        }
        _uiState.update {
            it.copy(searchQuery = value, searchResults = emptyList(), librarySearchResults = emptyList(),
                isSearching = true, searchError = null, searchPage = 1, searchHasMore = false)
        }
        searchJob = viewModelScope.launch {
            delay(350)
            // Your own libraries and Seerr answer independently: a title you already own must still
            // be findable when Seerr is down, and vice versa.
            val libraryDeferred = async {
                if (mediaServers.isEmpty()) Result.success(emptyList())
                else runCatching {
                    withContext(Dispatchers.IO) {
                        container.mediaSyncRepository.searchLibraries(mediaServers, query)
                    }
                }
            }
            val discoverResult = if (seerr == null) {
                Result.success(app.reelstack.data.repository.MediaSyncRepository.SearchPage(emptyList(), 1, false))
            } else runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query, page = 1) }
            }
            val libraryResult = libraryDeferred.await()
            if (!isActive || _uiState.value.searchQuery.trim() != query) return@launch
            _uiState.update {
                it.copy(
                    searchResults = discoverResult.getOrNull()?.items.orEmpty(),
                    searchPage = discoverResult.getOrNull()?.page ?: 1,
                    searchHasMore = discoverResult.getOrNull()?.hasMore == true,
                    librarySearchResults = libraryResult.getOrDefault(emptyList()),
                    isSearching = false,
                    searchError = when {
                        discoverResult.isFailure && libraryResult.isFailure ->
                            "Fekk ikkje søkt. Sjekk tilkoplingane og prøv igjen."
                        discoverResult.isFailure && seerr != null ->
                            "Fekk ikkje søkt i Seerr. Sjekk tilkoplinga og prøv igjen."
                        libraryResult.isFailure ->
                            "Fekk ikkje søkt i biblioteka dine. Sjekk tilkoplinga og prøv igjen."
                        else -> null
                    },
                )
            }
        }
    }

    /** Appends the next page of Seerr results. Existing hits stay put; only the tail grows. */
    fun loadMoreSearchResults() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        if (query.isBlank() || !state.searchHasMore || state.loadingMoreSearch || state.isSearching) return
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        val next = state.searchPage + 1
        _uiState.update { it.copy(loadingMoreSearch = true) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query, next) }
            }
            _uiState.update { current ->
                if (current.searchQuery.trim() != query) return@update current.copy(loadingMoreSearch = false)
                val page = result.getOrNull()
                current.copy(
                    loadingMoreSearch = false,
                    searchResults = if (page == null) current.searchResults
                    else (current.searchResults + page.items).distinctBy { item -> item.id },
                    searchPage = page?.page ?: current.searchPage,
                    searchHasMore = page?.hasMore == true,
                    searchError = if (result.isFailure) "Fekk ikkje henta fleire treff. Prøv igjen." else current.searchError,
                )
            }
        }
    }

    fun requestMedia(id: String) {
        val state = _uiState.value
        val media = (state.discover + state.searchResults + state.recommendations).firstOrNull { it.id == id } ?: return
        if (!media.canRequest || state.requestingMediaIds.isNotEmpty()) return
        if (state.configuredCount > 0 && state.accounts[ServiceKind.SEERR]?.isPersonal == true &&
            state.accounts[ServiceKind.SEERR]?.canRequestType(media.mediaType ?: "movie") != true) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
        if (state.configuredCount > 0 && (connection?.sessionCookie != true || state.accounts[ServiceKind.SEERR]?.isPersonal != true)) {
            openSeerrAccount()
            return
        }
        requestDraftJob?.cancel()
        _uiState.update { it.copy(activeSheet = AppSheet.RequestComposer, requestDraft = RequestDraft(media), returnToCalendar = false) }
        if (connection == null && state.configuredCount == 0) {
            val seasons = if (media.mediaType == "tv") listOf(RequestSeason(1, "Sesong 1", 8, 1)) else emptyList()
            _uiState.update { it.copy(requestDraft = RequestDraft(media, seasons, seasons.map { s -> s.number }.toSet(), loading = false)) }
            return
        }
        requestDraftJob = viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { container.mediaSyncRepository.details(requireNotNull(connection), media) } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val draft = current.requestDraft?.takeIf { it.media.id == id } ?: return@update current
                if (current.activeSheet != AppSheet.RequestComposer) return@update current
                current.copy(requestDraft = result.fold(onSuccess = { remote ->
                    val eligible = remote.seasons.filter { it.canRequest && it.number > 0 }
                    draft.copy(loading = false, seasons = remote.seasons, selected = eligible.map { it.number }.toSet(),
                        mediaStatus = remote.seerrStatus,
                        error = when {
                            remote.seerrStatus == 6 -> "Tittelen er blokkert av administratoren."
                            media.mediaType == "tv" && remote.seasons.isEmpty() -> "Seerr gav ingen sesongar. Prøv igjen seinare."
                            media.mediaType != "tv" && remote.seerrStatus in 2..5 -> "Filmen er alt førespurd eller i biblioteket."
                            else -> null
                        })
                }, onFailure = { draft.copy(loading = false, error = "Fekk ikkje henta sesongar og tilgjenge. Prøv igjen.") }))
            }
        }
    }

    fun setRequestSeason(number: Int, checked: Boolean) = _uiState.update { state ->
        val draft = state.requestDraft ?: return@update state
        if (draft.sending || draft.seasons.none { it.number == number && it.canRequest }) return@update state
        state.copy(requestDraft = draft.copy(selected = if (checked) draft.selected + number else draft.selected - number))
    }

    fun setRequestNotification(enabled: Boolean) = _uiState.update { state ->
        state.copy(requestDraft = state.requestDraft?.takeUnless { it.sending }?.copy(notify = enabled) ?: state.requestDraft)
    }

    fun confirmRequest() {
        val draft = _uiState.value.requestDraft ?: return
        if (draft.loading || draft.sending || draft.error != null || (draft.media.mediaType == "tv" && draft.selected.isEmpty())) return
        sendRequest(draft.media.id)
    }

    fun setFollowNotification(key: String, enabled: Boolean) {
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR } ?: return
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal } ?: return
        val scope = container.requestTrackingRepository.scope(connection, actor.id)
        container.requestTrackingRepository.setNotify(scope, key, enabled)
        _uiState.update { it.copy(trackedRequests = container.requestTrackingRepository.list(scope)) }
    }

    fun cancelTrackedRequest(key: String) {
        val state = _uiState.value
        if (key in state.cancellingRequestKeys) return
        val connection = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank()
        } ?: return
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal } ?: run {
            openSeerrAccount()
            return
        }
        _uiState.update { it.copy(cancellingRequestKeys = it.cancellingRequestKeys + key) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    container.requestTrackingRepository.cancel(connection, actor.id, key)
                    container.requestTrackingRepository.list(
                        container.requestTrackingRepository.scope(connection, actor.id),
                    )
                }
            }
            _uiState.update { current ->
                current.copy(
                    cancellingRequestKeys = current.cancellingRequestKeys - key,
                    trackedRequests = result.getOrNull() ?: current.trackedRequests,
                    snackbar = if (result.isSuccess) "Førespurnaden er trekt tilbake"
                    else result.exceptionOrNull()?.message
                        ?: "Fekk ikkje trekt tilbake førespurnaden. Prøv igjen.",
                )
            }
        }
    }

    fun refreshTrackedRequests() {
        if (trackingJob?.isActive == true) return
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() } ?: run {
            _uiState.update { it.copy(trackedRequests = emptyList(), trackingError = null) }
            return
        }
        _uiState.update { it.copy(trackingLoading = true) }
        trackingJob = viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { container.requestTrackingRepository.refresh(connection) } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl || configured.sessionCookie != connection.sessionCookie) current.copy(trackingLoading = false)
                else current.copy(trackingLoading = false,
                    trackedRequests = result.getOrNull()?.second ?: current.trackedRequests,
                    trackingError = if (result.isFailure) "Fekk ikkje oppdatert førespurnadene. Sjekk Seerr-innlogginga og prøv igjen." else null)
            }
        }
    }

    private fun sendRequest(id: String) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        val media = draft.media
        if (!media.canRequest || id in state.requestingMediaIds) return
        val seerr = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }

        if (state.configuredCount > 0 && (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null)) {
            _uiState.update { it.copy(snackbar = "Fekk ikkje sendt. Kople til Seerr og opne tittelen på nytt.") }
            return
        }

        if (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null) {
            _uiState.update {
                it.copy(
                    discover = it.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    recommendations = it.recommendations.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    searchResults = it.searchResults.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    contentDetails = it.contentDetails?.let { details ->
                        if (details.key == id) details.copy(statusTitle = "Lagd til lokalt", statusDescription = "Dette er ei førehandsvising. Ingenting er sendt til Seerr.") else details
                    },
                    snackbar = "Tittelen er lagd til lokalt · kople til Seerr for å sende han vidare",
                    activeSheet = null,
                    requestDraft = null,
                )
            }
            return
        }

        val account = state.accounts[ServiceKind.SEERR]
        if (!seerr.sessionCookie || account?.isPersonal != true) {
            openSeerrAccount()
            _uiState.update { it.copy(snackbar = "Logg inn med Jellyfin-kontoen din i Seerr for å sende som deg sjølv.") }
            return
        }
        _uiState.update { it.copy(requestingMediaIds = it.requestingMediaIds + id, requestDraft = draft.copy(sending = true)) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    // Reconfirm the exact identity shown in the UI before any write.
                    container.mediaSyncRepository.request(seerr, media, expectedUserId = account.id, seasons = draft.selected)
                    container.requestTrackingRepository.follow(seerr, account.id, media, draft.selected, draft.notify)
                }
            }
            _uiState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        discover = current.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        recommendations = current.recommendations.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        searchResults = current.searchResults.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        contentDetails = current.contentDetails?.let { details ->
                            if (details.key == id) details.copy(statusTitle = "Sendt som ${account.displayName}", statusDescription = "Førespurnaden er registrert på Seerr-kontoen din. Oppdatert status kjem ved neste synkronisering.") else details
                        },
                        activity = listOf(
                            ActivityEvent(
                                id = "seerr-request-${media.id}",
                                title = media.title,
                                detail = "Sendt som ${account.displayName}",
                                time = "No nettopp",
                                timeEpochMillis = System.currentTimeMillis(),
                                source = ServiceKind.SEERR,
                                artworkRes = media.artworkRes,
                                artworkUrl = media.artworkUrl,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Sendt til Seerr som ${account.displayName}",
                        activeSheet = null,
                        requestDraft = null,
                        selectedTab = AppTab.ACTIVITY,
                    )
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Fekk ikkje sendt som ${account.displayName}. Sjekk Seerr-kontoen og tilgangen din.",
                        requestDraft = draft.copy(sending = false, error = "Fekk ikkje sendt. Sesongane kan vere endra, eller kontoen manglar tilgang. Opne førespurnaden på nytt før du prøver igjen."),
                    )
                }
            }
            if (result.isSuccess) refreshTrackedRequests()
        }
    }

    suspend fun refreshPlayback() {
        if (refreshJob?.isActive == true) return
        val connections = _uiState.value.connections
        if (connections.none { it.token.isNotBlank() && it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) }) return
        val sessions = withContext(Dispatchers.IO) { container.mediaSyncRepository.refreshPlayback(connections) }
        // A response from an account that has since signed out must never repopulate the screen.
        _uiState.update { current ->
            if (current.connections != connections || refreshJob?.isActive == true) current
            else current.copy(sessions = sessions)
        }
    }

    fun refreshLiveData(userInitiated: Boolean = false) {
        refreshAccounts()
        refreshTrackedRequests()
        if (refreshJob?.isActive == true) return
        val state = _uiState.value
        val configured = state.connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        if (configured.isEmpty()) {
            _uiState.update {
                it.copy(
                    sessions = demoSessions(),
                    resume = demoResume(),
                    recentMovies = demoRecentMovies(),
                    recentSeries = demoRecentSeries(),
                    upcoming = demoUpcoming(),
                    recentReleases = demoRecentReleases(),
                    incoming = demoIncoming(),
                    discover = demoDiscover(),
                    recommendations = demoRecommendations(),
                    activity = demoActivity(),
                    isRefreshing = false,
                    liveSession = false,
                    liveLibrary = false,
                    liveIncoming = false,
                    liveDiscover = false,
                    liveActivity = false,
                    failedServices = emptySet(),
                    hasCachedData = false,
                    snackbar = if (userInitiated) "Kople til ei teneste for å starte synkronisering" else it.snackbar,
                )
            }
            return
        }

        _uiState.update { it.copy(isRefreshing = true) }
        refreshJob = viewModelScope.launch {
            val outcome = runCatching {
                val snapshot = withContext(Dispatchers.IO) {
                    container.mediaSyncRepository.refresh(
                        connections = _uiState.value.connections,
                    )
                }
                if (snapshot.successfulServices.isNotEmpty() && snapshot.errors.isEmpty()) {
                    // Writing the offline copy is a convenience. A full disk must not take the
                    // refresh down with it.
                    runCatching {
                        withContext(Dispatchers.IO) {
                            container.mediaSnapshotStore.save(
                                snapshot,
                                app.reelstack.data.repository.MediaSnapshotStore.fingerprint(_uiState.value.connections),
                            )
                        }
                    }
                }
                snapshot
            }
            val snapshot = outcome.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                // Per-service failures are already reported inside the snapshot. Anything that
                // escapes to here is unexpected, and it used to leave the spinner turning forever
                // and take the process down with it.
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        snackbar = "Fekk ikkje oppdatert innhaldet. Dra ned for å prøve igjen.",
                    )
                }
                return@launch
            }
            // A service that only answered on its alternate address keeps that address next time.
            if (snapshot.switchedToAlternate.isNotEmpty()) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        snapshot.switchedToAlternate.forEach(container.connectionRepository::promoteAlternate)
                    }
                }
            }
            val refreshedConnections = if (snapshot.switchedToAlternate.isEmpty()) null
            else runCatching { container.connectionRepository.list() }.getOrNull()
            _uiState.update { current ->
                val connectionsNow = refreshedConnections ?: current.connections
                val configuredKinds = connectionsNow.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
                val configuredMedia = configuredKinds.intersect(setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
                val configuredQueue = configuredKinds.intersect(setOf(ServiceKind.RADARR, ServiceKind.SONARR))
                val mediaLive = snapshot.successfulServices.any { it in configuredMedia }
                val queueLive = snapshot.successfulServices.any { it in configuredQueue }
                val seerrLive = ServiceKind.SEERR in snapshot.successfulServices
                val activityLive = queueLive || seerrLive
                val anySuccess = snapshot.successfulServices.isNotEmpty()

                current.copy(
                    adminView = snapshot.adminView,
                    sessions = snapshot.sessions,
                    // Do not retain library data after the current profile or library scope fails verification.
                    resume = snapshot.resume,
                    recentMovies = snapshot.recentMovies,
                    recentSeries = snapshot.recentSeries,
                    // Release metadata does not require administrator queue credentials.
                    recentReleases = snapshot.recentReleases,
                    upcoming = snapshot.upcoming,
                    recentReleasesError = snapshot.recentReleasesError,
                    upcomingError = snapshot.upcomingError,
                    incoming = snapshot.incoming,
                    discover = when {
                        ServiceKind.SEERR !in configuredKinds -> emptyList()
                        seerrLive -> snapshot.discover
                        current.liveDiscover || current.hasCachedData -> current.discover
                        else -> emptyList()
                    },
                    recommendations = when {
                        snapshot.recommendationsError == null -> snapshot.recommendations
                        current.hasCachedData -> current.recommendations
                        else -> emptyList()
                    },
                    activity = snapshot.activity,
                    connections = connectionsNow.map { connection ->
                        when {
                            connection.baseUrl.isBlank() -> connection.copy(state = ConnectionState.DEMO, detail = "Demodata")
                            connection.kind in snapshot.errors -> connection.copy(
                                state = ConnectionState.ERROR,
                                detail = snapshot.errors.getValue(connection.kind),
                            )
                            connection.kind in snapshot.successfulServices -> connection.copy(
                                state = ConnectionState.CONNECTED,
                                detail = when {
                                    connection.kind in snapshot.switchedToAlternate -> "Aktiv · bytta til den andre adressa"
                                    else -> snapshot.warnings[connection.kind]?.let { "Tilkopla · $it" }
                                        ?: "Aktiv · oppdatert no"
                                },
                            )
                            else -> connection
                        }
                    },
                    isRefreshing = false,
                    liveSession = configuredMedia.isNotEmpty() && (mediaLive || current.liveSession),
                    liveLibrary = configuredMedia.isNotEmpty() && (mediaLive || current.liveLibrary),
                    liveIncoming = configuredQueue.isNotEmpty() && (queueLive || current.liveIncoming),
                    liveDiscover = ServiceKind.SEERR in configuredKinds && (seerrLive || current.liveDiscover),
                    liveActivity = (configuredQueue.isNotEmpty() || ServiceKind.SEERR in configuredKinds) &&
                        (activityLive || current.liveActivity),
                    lastUpdatedEpochMillis = if (anySuccess) snapshot.refreshedAt.toEpochMilli() else current.lastUpdatedEpochMillis,
                    hasCachedData = !anySuccess && current.hasCachedData,
                    failedServices = snapshot.errors.keys,
                    serviceWarnings = snapshot.warnings,
                    snackbar = if (userInitiated) {
                        when {
                            snapshot.errors.isNotEmpty() -> "Oppdatert · ${snapshot.errors.size} teneste${if (snapshot.errors.size == 1) "" else "r"} må sjekkast"
                            snapshot.warnings.isNotEmpty() -> "Tilkopla, men nokre delar må sjekkast"
                            else -> "Alt er oppdatert"
                        }
                    } else current.snackbar,
                )
            }
        }
    }

    fun setNotifications(enabled: Boolean) {
        container.preferencesRepository.notificationsEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    private fun refreshAccounts() {
        accountsJob?.cancel()
        val targets = _uiState.value.connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR) && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val kinds = targets.map { it.kind }.toSet()
        _uiState.update { it.copy(accounts = it.accounts.filterKeys(kinds::contains),
            accountErrors = it.accountErrors.filterKeys(kinds::contains), loadingAccounts = kinds) }
        accountsJob = viewModelScope.launch {
            targets.forEach { connection ->
                launch profile@ {
                    val result = runCatching { withContext(Dispatchers.IO) { container.accountProfileClient.load(connection) } }
                    if (!isActive) return@profile
                    _uiState.update { current ->
                        val configured = current.connections.firstOrNull { it.kind == connection.kind }
                        if (configured == null || configured.baseUrl != connection.baseUrl || configured.token != connection.token ||
                            configured.userId != connection.userId || configured.sessionCookie != connection.sessionCookie) current
                        else current.copy(
                            accounts = if (result.isSuccess) current.accounts + (connection.kind to result.getOrThrow()) else current.accounts - connection.kind,
                            accountErrors = if (result.isSuccess) current.accountErrors - connection.kind else current.accountErrors +
                                (connection.kind to "Fekk ikkje stadfesta kontoen. Sjekk innlogginga eller prøv å oppdatere igjen."),
                            loadingAccounts = current.loadingAccounts - connection.kind,
                        )
                    }
                }
            }
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        container.preferencesRepository.wifiOnly = enabled
        BackgroundRefreshScheduler.schedule(container.appContext, enabled)
        _uiState.update { it.copy(wifiOnly = enabled, snackbar = "Bakgrunnsoppdateringa er endra") }
    }

    fun setHomeSectionVisible(section: HomeSection, visible: Boolean) {
        _uiState.update { current ->
            val updated = if (visible) current.homeSections + section else current.homeSections - section
            container.preferencesRepository.visibleHomeSections = updated
            current.copy(homeSections = updated)
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbar = null) }

    fun updateConnectionName(value: String) = updateDraft { copy(name = value, error = null) }
    fun updateConnectionUrl(value: String) {
        quickConnectJob?.cancel()
        updateDraft {
            copy(
                url = value,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
                warning = runCatching {
                    if (value.isNotBlank() && EndpointValidator.isCleartext(value)) {
                        "HTTP er ukryptert. Bruk helst HTTPS utanfor det trygge lokalnettet ditt."
                    } else null
                }.getOrNull(),
            )
        }
    }
    fun updateConnectionToken(value: String) = updateDraft { copy(token = value, error = null) }
    fun updateConnectionUserId(value: String) = updateDraft { copy(userId = value, error = null) }
    fun updateConnectionAlternateUrl(value: String) = updateDraft { copy(alternateUrl = value, error = null) }
    fun updateConnectionAuthMode(value: ConnectionAuthMode) {
        quickConnectJob?.cancel()
        updateDraft {
            copy(
                authMode = value,
                saving = false,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
            )
        }
    }
    fun updateConnectionUsername(value: String) = updateDraft { copy(username = value, error = null) }
    fun updateConnectionPassword(value: String) = updateDraft { copy(password = value, error = null) }
    fun updateCompanionLogin(enabled: Boolean, url: String) = updateDraft {
        copy(alsoConnect = enabled, companionUrl = url, error = null)
    }

    fun testAndSaveConnection() {
        val draft = connectionDraft.value ?: return
        val normalizedUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .getOrElse {
                updateDraft { copy(error = it.message ?: "Skriv inn ei gyldig tenaradresse") }
                return
            }
        val useJellyfinAccount = draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR, ServiceKind.EMBY) && draft.authMode == ConnectionAuthMode.ACCOUNT
        val useQuickConnect = (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (useQuickConnect) {
            startQuickConnect(draft, normalizedUrl)
            return
        }
        if (useJellyfinAccount && draft.username.isBlank()) {
            updateDraft { copy(error = "Skriv inn brukarnamnet ditt") }
            return
        }
        if (!useJellyfinAccount && draft.token.isBlank()) {
            updateDraft { copy(error = "Skriv inn ein API-nøkkel eller eit tilgangsteikn") }
            return
        }
        val alternateUrl = draft.alternateUrl.takeIf(String::isNotBlank)?.let { entered ->
            runCatching { EndpointValidator.normalizeBaseUrl(entered) }.getOrElse {
                updateDraft { copy(error = "Sjekk den andre adressa: ${it.message ?: "Skriv inn ei gyldig tenaradresse"}") }
                return
            }
        }.orEmpty()
        if (alternateUrl.isNotBlank() && alternateUrl == normalizedUrl) {
            updateDraft { copy(error = "Den andre adressa er den same som den vanlege.") }
            return
        }
        val companionUrl = if (useJellyfinAccount && draft.alsoConnect && draft.kind != ServiceKind.EMBY) {
            runCatching { EndpointValidator.normalizeBaseUrl(draft.companionUrl) }.getOrElse {
                updateDraft { copy(error = "Sjekk adressa til den andre tenesta: ${it.message ?: "Skriv inn ei gyldig tenaradresse"}") }
                return
            }
        } else null
        updateDraft { copy(url = normalizedUrl, companionUrl = companionUrl ?: this.companionUrl, saving = true, error = null, password = "") }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            val credentials = if (useJellyfinAccount) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (draft.kind == ServiceKind.SEERR) {
                            container.seerrAuthenticationClient.authenticate(normalizedUrl, draft.username.trim(), draft.password)
                        } else if (draft.kind == ServiceKind.EMBY) {
                            container.embyAuthenticationClient.authenticate(normalizedUrl, draft.username.trim(), draft.password)
                        } else container.jellyfinAuthenticationClient.authenticate(
                            baseUrl = normalizedUrl,
                            username = draft.username.trim(),
                            password = draft.password,
                        )
                    }
                }.getOrElse { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    updateDraft { copy(saving = false, error = error.message ?: "Jellyfin avviste innlogginga") }
                    return@launch
                }
            } else null
            val candidate = ServiceConnection(
                kind = draft.kind,
                name = draft.name.ifBlank { draft.kind.displayName },
                baseUrl = normalizedUrl,
                token = credentials?.accessToken ?: draft.token,
                sessionCookie = draft.kind == ServiceKind.SEERR && credentials != null,
                userId = credentials?.userId ?: draft.userId,
                alternateUrl = alternateUrl,
                state = ConnectionState.TESTING,
            )
            val companion = if (companionUrl != null) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val otherKind = if (draft.kind == ServiceKind.SEERR) ServiceKind.JELLYFIN else ServiceKind.SEERR
                        val auth = if (otherKind == ServiceKind.SEERR) container.seerrAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                            else container.jellyfinAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                        val other = ServiceConnection(otherKind, otherKind.displayName, companionUrl, auth.accessToken,
                            userId = auth.userId, sessionCookie = otherKind == ServiceKind.SEERR)
                        val seerr = container.accountProfileClient.load(if (draft.kind == ServiceKind.SEERR) candidate else other)
                        val jellyfinId = if (draft.kind == ServiceKind.JELLYFIN) candidate.userId else other.userId
                        check(app.reelstack.data.model.matchesJellyfinAccount(seerr, jellyfinId)) {
                            "Seerr-kontoen er ikkje knytt til denne Jellyfin-kontoen. Logg inn på tenestene kvar for seg."
                        }
                        other
                    }
                }.getOrElse { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    updateDraft { copy(saving = false, error = "Ingen tilkoplingar vart endra. ${error.message ?: "Den andre innlogginga feila."} Prøv igjen, eller slå av felles innlogging.") }
                    return@launch
                }
            } else null
            verifyAndSaveConnection(candidate, companion)
        }
    }

    private fun startQuickConnect(draft: ConnectionDraft, normalizedUrl: String) {
        quickConnectJob?.cancel()
        updateDraft {
            copy(
                url = normalizedUrl,
                saving = true,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = null,
            )
        }
        quickConnectJob = viewModelScope.launch {
            var quickConnect = runCatching {
                withContext(Dispatchers.IO) {
                    if (draft.kind == ServiceKind.SEERR) container.seerrAuthenticationClient.initiateQuickConnect(normalizedUrl)
                    else container.jellyfinAuthenticationClient.initiateQuickConnect(normalizedUrl)
                }
            }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                updateDraft {
                    copy(
                        saving = false,
                        quickConnectWaiting = false,
                        error = error.message ?: "Fekk ikkje starta Quick Connect",
                    )
                }
                return@launch
            }
            updateDraft {
                copy(
                    saving = false,
                    quickConnectCode = quickConnect.code,
                    quickConnectWaiting = true,
                    error = null,
                )
            }

            repeat(QUICK_CONNECT_MAX_POLLS) {
                if (quickConnect.authenticated) {
                    updateDraft { copy(saving = true, quickConnectWaiting = false, error = null) }
                    val credentials = runCatching {
                        withContext(Dispatchers.IO) {
                            if (draft.kind == ServiceKind.SEERR) container.seerrAuthenticationClient.authenticateWithQuickConnect(normalizedUrl, quickConnect)
                            else container.jellyfinAuthenticationClient.authenticateWithQuickConnect(
                                normalizedUrl,
                                quickConnect.secret,
                            )
                        }
                    }.getOrElse { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        updateDraft {
                            copy(
                                saving = false,
                                quickConnectWaiting = false,
                                error = error.message ?: "Quick Connect vart ikkje fullført",
                            )
                        }
                        return@launch
                    }
                    verifyAndSaveConnection(
                        ServiceConnection(
                            kind = draft.kind,
                            name = draft.name.ifBlank { draft.kind.displayName },
                            sessionCookie = draft.kind == ServiceKind.SEERR,
                            baseUrl = normalizedUrl,
                            token = credentials.accessToken,
                            userId = credentials.userId,
                            alternateUrl = runCatching {
                                draft.alternateUrl.takeIf(String::isNotBlank)
                                    ?.let(EndpointValidator::normalizeBaseUrl).orEmpty()
                            }.getOrDefault(""),
                            state = ConnectionState.TESTING,
                        ),
                    )
                    return@launch
                }

                delay(QUICK_CONNECT_POLL_INTERVAL_MS)
                quickConnect = runCatching {
                    withContext(Dispatchers.IO) {
                        if (draft.kind == ServiceKind.SEERR) container.seerrAuthenticationClient.quickConnectState(normalizedUrl, quickConnect)
                        else container.jellyfinAuthenticationClient.quickConnectState(
                            normalizedUrl,
                            quickConnect.secret,
                        )
                    }
                }.getOrElse { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    updateDraft {
                        copy(
                            saving = false,
                            quickConnectWaiting = false,
                            error = error.message ?: "Mista kontakten med Quick Connect",
                        )
                    }
                    return@launch
                }
                updateDraft { copy(quickConnectCode = quickConnect.code) }
            }

            updateDraft {
                copy(
                    saving = false,
                    quickConnectWaiting = false,
                    error = "Quick Connect-koden gjekk ut. Lag ein ny kode og prøv igjen.",
                )
            }
        }
    }

    private suspend fun verifyAndSaveConnection(candidate: ServiceConnection, companion: ServiceConnection? = null) {
        val personalLogin = connectionDraft.value?.authMode != ConnectionAuthMode.API_KEY
        fun verify(connection: ServiceConnection): app.reelstack.data.model.ConnectionTestResult {
            if (personalLogin && connection.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
                val account = container.accountProfileClient.load(connection)
                check(account.id == connection.userId) { "Tenaren stadfesta ikkje den innlogga kontoen." }
                return app.reelstack.data.model.ConnectionTestResult(true, 0, "Logga inn")
            }
            return container.connectionTester.test(connection)
        }
        if (companion != null) {
            val otherResult = runCatching { withContext(Dispatchers.IO) { verify(companion) } }
            if (otherResult.exceptionOrNull() is kotlinx.coroutines.CancellationException) throw otherResult.exceptionOrNull()!!
            if (otherResult.getOrNull()?.success != true) {
                updateDraft { copy(saving = false, error = "Fekk ikkje stadfesta ${companion.kind.displayName}. Ingen tilkoplingar vart endra.") }
                return
            }
        }
        val result = runCatching {
            withContext(Dispatchers.IO) { verify(candidate) }
        }.getOrElse { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            updateDraft {
                copy(saving = false, error = error.message ?: "Fekk ikkje kontakt med tenesta")
            }
            return
        }

        if (!result.success) {
            updateDraft { copy(saving = false, error = result.message) }
            return
        }

        // The server accepted us, but the token still has to reach the keystore. If that write
        // fails, say so rather than reporting a connection the app cannot actually reuse.
        val stored = runCatching {
            withContext(Dispatchers.IO) {
                container.connectionRepository.save(candidate)
                companion?.let { container.connectionRepository.save(it) }
                container.mediaSnapshotStore.clear()
            }
        }
        stored.exceptionOrNull()?.let { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            updateDraft { copy(saving = false, error = "Fekk ikkje lagra innlogginga trygt på denne eininga. Prøv igjen.") }
            return
        }
        refreshJob?.cancel()
        refreshJob = null
        searchJob?.cancel()
        trackingJob?.cancel()
        val saved = candidate.copy(
            state = ConnectionState.CONNECTED,
            latencyMs = result.latencyMs,
            detail = result.message,
        )
        _uiState.update { state ->
            state.copy(
                connections = state.connections.map { if (it.kind == saved.kind) saved else if (it.kind == companion?.kind) companion.copy(state = ConnectionState.CONNECTED) else it },
                accounts = state.accounts - saved.kind - listOfNotNull(companion?.kind).toSet(),
                trackedRequests = if (saved.kind == ServiceKind.SEERR || companion?.kind == ServiceKind.SEERR) emptyList() else state.trackedRequests,
                accountErrors = state.accountErrors - saved.kind,
                adminView = false,
                sessions = emptyList(),
                resume = emptyList(),
                recentMovies = emptyList(),
                recentSeries = emptyList(),
                upcoming = emptyList(),
                recentReleases = emptyList(),
                incoming = emptyList(),
                discover = emptyList(),
                recommendations = emptyList(),
                activity = emptyList(),
                searchQuery = "",
                searchResults = emptyList(),
                librarySearchResults = emptyList(),
                activeSheet = null,
                isSearching = false,
                snackbar = if (companion != null) "Jellyfin og Seerr er klare. Hentar innhaldet ditt…" else "${saved.kind.displayName} er klar. Hentar innhaldet ditt…",
            )
        }
        connectionDraft.value = null
        refreshLiveData()
    }

    fun removeConnection(kind: ServiceKind) {
        if (!_uiState.value.canEditConnection(kind)) return
        if (_uiState.value.requestingMediaIds.isNotEmpty()) return
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        accountsJob?.cancel()
        trackingJob?.cancel()
        searchJob?.cancel()
        refreshJob?.cancel()
        refreshJob = null
        container.connectionRepository.signOut(kind)
        val remaining = container.connectionRepository.list()
        val signedOutEverywhere = remaining.none { it.baseUrl.isNotBlank() }
        if (signedOutEverywhere) container.preferencesRepository.onboardingCompleted = false
        container.mediaSnapshotStore.clear()
        _uiState.update {
            it.copy(
                connections = remaining,
                showOnboarding = signedOutEverywhere,
                adminView = false,
                sessions = emptyList(),
                activity = emptyList(),
                incoming = emptyList(),
                resume = emptyList(),
                recentMovies = emptyList(),
                recentSeries = emptyList(),
                upcoming = emptyList(),
                recentReleases = emptyList(),
                discover = emptyList(),
                recommendations = emptyList(),
                searchResults = emptyList(),
                librarySearchResults = emptyList(),
                searchQuery = "",
                isSearching = false,
                accounts = it.accounts - kind,
                trackedRequests = if (kind == ServiceKind.SEERR) emptyList() else it.trackedRequests,
                accountErrors = it.accountErrors - kind,
                activeSheet = null,
                failedServices = it.failedServices - kind,
                snackbar = "Logga ut av ${kind.displayName} på denne eininga",
            )
        }
        connectionDraft.value = null
        refreshLiveData()
    }

    private inline fun updateDraft(transform: ConnectionDraft.() -> ConnectionDraft) {
        connectionDraft.update { draft -> draft?.transform() }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReelstackViewModel::class.java))
            return ReelstackViewModel(container) as T
        }
    }

    private companion object {
        const val QUICK_CONNECT_POLL_INTERVAL_MS = 3_000L
        const val QUICK_CONNECT_MAX_POLLS = 60
    }
}

private fun initialState(container: AppContainer): ReelstackUiState {
    val connections = container.connectionRepository.list()
    val configuredKinds = connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
    val hasMediaServer = configuredKinds.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val hasQueueService = configuredKinds.any { it == ServiceKind.RADARR || it == ServiceKind.SONARR }
    val hasSeerr = ServiceKind.SEERR in configuredKinds
    // Show the last verified feed while the first refresh runs, instead of empty rails. The store
    // only returns a copy written by this feed version for exactly these signed-in accounts, so
    // identities and library exclusions are revalidated before anything is exposed.
    val cached = runCatching {
        container.mediaSnapshotStore.read(
            app.reelstack.data.repository.MediaSnapshotStore.fingerprint(connections),
        )
    }.getOrNull()

    return ReelstackUiState(
        showOnboarding = configuredKinds.isEmpty() && !container.preferencesRepository.onboardingCompleted,
        connections = connections,
        sessions = when {
            hasMediaServer && cached != null -> cached.sessions
            hasMediaServer -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoSessions() else emptyList()
        },
        resume = when {
            hasMediaServer && cached != null -> cached.resume
            hasMediaServer -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoResume() else emptyList()
        },
        recentMovies = when {
            hasMediaServer && cached != null -> cached.recentMovies
            hasMediaServer -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoRecentMovies() else emptyList()
        },
        recentSeries = when {
            hasMediaServer && cached != null -> cached.recentSeries
            hasMediaServer -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoRecentSeries() else emptyList()
        },
        upcoming = when {
            hasQueueService && cached != null -> cached.upcoming
            hasQueueService -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoUpcoming() else emptyList()
        },
        recentReleases = when {
            hasQueueService && cached != null -> cached.recentReleases
            hasQueueService -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoRecentReleases() else emptyList()
        },
        incoming = when {
            hasQueueService && cached != null -> cached.incoming
            hasQueueService -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoIncoming() else emptyList()
        },
        discover = when {
            hasSeerr && cached != null -> cached.discover
            hasSeerr -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoDiscover() else emptyList()
        },
        recommendations = when {
            cached != null -> cached.recommendations
            configuredKinds.isEmpty() -> demoRecommendations()
            else -> emptyList()
        },
        activity = when {
            (hasQueueService || hasSeerr) && cached != null -> cached.activity
            hasQueueService || hasSeerr -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoActivity() else emptyList()
        },
        notificationsEnabled = container.preferencesRepository.notificationsEnabled,
        wifiOnly = container.preferencesRepository.wifiOnly,
        homeSections = container.preferencesRepository.visibleHomeSections,
        lastUpdatedEpochMillis = cached?.refreshedAtEpochMillis,
        hasCachedData = cached != null,
        isRefreshing = configuredKinds.isNotEmpty(),
    )
}

private fun demoSessions() = listOf(
    PlaybackSession(
        userName = "Maya",
        deviceName = "TV i stova",
        title = "Severance",
        subtitle = "S02  E04",
        progress = 0.58f,
        timeLeft = "32 min att",
        streamMethod = "Direkteavspeling",
        quality = "4K",
        paused = false,
        sessionId = "demo-living-room",
        source = ServiceKind.JELLYFIN,
    ),
    PlaybackSession(
        userName = "Jonas",
        deviceName = "Pixel-nettbrett",
        title = "The Bear",
        subtitle = "S03  E02",
        progress = 0.31f,
        timeLeft = "24 min att",
        streamMethod = "Direkteavspeling",
        quality = "1080p",
        paused = true,
        sessionId = "demo-tablet",
        source = ServiceKind.EMBY,
    ),
)

// Demo carries the same fields a real service would, including an omtale: the detail sheet only
// shows its synopsis section when there is one, so a demo without omtale looks half-built.
private fun demoResume() = listOf(
    LibraryMedia(
        id = "resume-severance",
        title = "Severance",
        subtitle = "S02 E04 · Ein halvsett episode",
        progress = 0.58f,
        artworkRes = R.drawable.session_still,
        source = ServiceKind.JELLYFIN,
        mediaType = "Episode",
        overview = "Du er 58 % gjennom denne episoden.",
        facts = listOf("32 min att"),
    ),
    LibraryMedia(
        id = "resume-odyssey",
        title = "The Odyssey",
        subtitle = "Film · 2026",
        progress = 0.21f,
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.JELLYFIN,
        mediaType = "Movie",
        overview = "Du er 21 % gjennom denne filmen.",
        facts = listOf("2 t 10 min att"),
    ),
)

private fun demoRecentMovies() = listOf(
    LibraryMedia(
        id = "recent-odyssey",
        title = "The Odyssey",
        subtitle = "Film · 2026",
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.JELLYFIN,
        mediaType = "Movie",
        overview = "Ein soldat legg ut på ei lang heimreise gjennom ukjende landskap, og oppdagar " +
            "at vegen heim krev meir av han enn krigen gjorde.",
        facts = listOf("2026", "2 t 45 min", "Eventyr"),
        genres = listOf("Eventyr", "Drama"),
    ),
)

private fun demoRecentSeries() = listOf(
    LibraryMedia(
        id = "recent-severance",
        title = "Severance",
        subtitle = "Serie · 2 sesongar",
        artworkRes = R.drawable.session_still,
        source = ServiceKind.JELLYFIN,
        mediaType = "Series",
        overview = "Tilsette ved eit kontor har delt minna sine i to: eitt for arbeid og eitt for " +
            "livet utanfor. Så byrjar dei to sidene å lekke over i kvarandre.",
        facts = listOf("2 sesongar", "Mystikk"),
        genres = listOf("Mystikk", "Thriller"),
    ),
)

// The label and the timestamp are the same moment. They used to disagree, so Home said 21:00
// while the calendar computed a different time from the timestamp for the same episode.
private fun demoUpcoming(): List<UpcomingMedia> {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    val tonight = today.atTime(21, 0).atZone(zone).toInstant().toEpochMilli()
    val tomorrow = today.plusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    return listOf(
        UpcomingMedia(
            id = "upcoming-andor",
            title = "Andor",
            subtitle = "S02 E07 · Messenger",
            dateLabel = "I kveld · 21:00",
            airDateEpochMillis = tonight,
            artworkRes = R.drawable.kitchen_request,
            source = ServiceKind.SONARR,
            mediaType = "Episode",
        ),
        UpcomingMedia(
            id = "upcoming-odyssey",
            title = "The Odyssey",
            subtitle = "Film · 2026",
            dateLabel = "I morgon",
            airDateEpochMillis = tomorrow,
            artworkRes = R.drawable.desert_arrival,
            source = ServiceKind.RADARR,
            mediaType = "Movie",
        ),
    )
}

private fun demoRecentReleases(): List<UpcomingMedia> {
    val zone = java.time.ZoneId.systemDefault()
    val today = java.time.LocalDate.now(zone)
    return listOf(
        UpcomingMedia(
            id = "recent-release-odyssey",
            title = "The Odyssey",
            subtitle = "Film · 2026",
            dateLabel = "I går",
            airDateEpochMillis = today.minusDays(1).atTime(20, 0).atZone(zone).toInstant().toEpochMilli(),
            artworkRes = R.drawable.desert_arrival,
            source = ServiceKind.RADARR,
            mediaType = "Movie",
        ),
        UpcomingMedia(
            id = "recent-release-andor",
            title = "Andor",
            subtitle = "S02 E06 · Ny episode",
            dateLabel = "I dag",
            airDateEpochMillis = today.atTime(18, 0).atZone(zone).toInstant().toEpochMilli(),
            artworkRes = R.drawable.kitchen_request,
            source = ServiceKind.SONARR,
            mediaType = "Episode",
        ),
    ).sortedByDescending(UpcomingMedia::airDateEpochMillis)
}

private fun demoIncoming() = listOf(
    IncomingMedia(
        id = "dune-messiah",
        title = "Dune: Messiah",
        source = ServiceKind.RADARR,
        status = "Lastar ned 68 %",
        state = IncomingState.DOWNLOADING,
        artworkRes = R.drawable.desert_arrival,
        progress = 68,
    ),
    IncomingMedia(
        id = "the-bear",
        title = "The Bear",
        source = ServiceKind.SONARR,
        status = "Lagd til",
        state = IncomingState.REQUESTED,
        artworkRes = R.drawable.kitchen_request,
    ),
)

// Demo is the showroom, so every field has to agree with every other one: a title marked as a
// series must carry mediaType "tv", and a labelled time must match its own timestamp.
private fun demoDiscover() = listOf(
    DiscoverMedia("last-horizon", "The Last Horizon", "Film · 2026", R.drawable.desert_arrival, false,
        mediaType = "movie",
        overview = "Eit mannskap følgjer eit signal ut til kanten av det kjende rommet, og finn " +
            "noko som har venta på dei mykje lenger enn dei har levd.",
        facts = listOf("2026", "1 t 58 min"), genres = listOf("Science fiction")),
    DiscoverMedia("service", "Service", "Serie · 3 sesongar", R.drawable.kitchen_request, true,
        mediaType = "tv",
        overview = "Ein kokk tek over restauranten til familien og oppdagar at kaoset på kjøkenet " +
            "er lettare å styre enn folka rundt han.",
        facts = listOf("3 sesongar"), genres = listOf("Drama", "Komedie")),
)

private fun demoRecommendations() = listOf(
    DiscoverMedia(
        id = "github-recommendation-tv-95396",
        title = "Severance",
        metadata = "Serie · 2022",
        artworkRes = R.drawable.session_still,
        inLibrary = false,
        remoteId = 95396,
        mediaType = "tv",
        overview = "Tilsette ved eit mystisk kontor får minna sine delte mellom arbeid og livet utanfor.",
        genres = listOf("Science fiction", "Thriller"),
    ),
    DiscoverMedia(
        id = "github-recommendation-movie-693134",
        title = "Dune: Part Two",
        metadata = "Film · 2024",
        artworkRes = R.drawable.desert_arrival,
        inLibrary = false,
        remoteId = 693134,
        mediaType = "movie",
        overview = "Paul Atreides slår seg saman med Chani og frimen-folket på vegen mot hemn og ei større skjebne.",
        genres = listOf("Science fiction", "Eventyr"),
    ),
)

private fun demoActivity() = listOf(
    ActivityEvent("odyssey", "The Odyssey", "Godkjend i Seerr", "For 2 min sidan", complete = false, source = ServiceKind.SEERR, artworkRes = R.drawable.desert_arrival, mediaType = "Movie"),
    ActivityEvent("alien-earth", "Alien: Earth", "Sonarr · lastar ned 42 %", "For 8 min sidan", progress = 42, source = ServiceKind.SONARR, artworkRes = R.drawable.kitchen_request, mediaType = "Episode"),
    ActivityEvent("mickey-17", "Mickey 17", "Importert av Radarr", "I går", complete = true, source = ServiceKind.RADARR, artworkRes = R.drawable.desert_arrival, mediaType = "Movie"),
)
