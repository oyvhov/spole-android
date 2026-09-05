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
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.network.EndpointValidator
import kotlinx.coroutines.Dispatchers
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
    val recentMovies: List<LibraryMedia> = demoRecentMovies(),
    val recentSeries: List<LibraryMedia> = demoRecentSeries(),
    val upcoming: List<UpcomingMedia> = demoUpcoming(),
    val incoming: List<IncomingMedia> = demoIncoming(),
    val discover: List<DiscoverMedia> = demoDiscover(),
    val searchResults: List<DiscoverMedia> = emptyList(),
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
) {
    val visibleDiscover: List<DiscoverMedia>
        get() = if (searchQuery.isBlank()) discover else searchResults

    val configuredCount: Int
        get() = connections.count { it.baseUrl.isNotBlank() }

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
                url = existing.baseUrl,
                token = if (existing.sessionCookie) "" else existing.token,
                userId = existing.userId,
                authMode = if (existing.kind == ServiceKind.SEERR && (existing.token.isBlank() || existing.sessionCookie)) {
                    ConnectionAuthMode.ACCOUNT
                } else if (existing.kind == ServiceKind.JELLYFIN && existing.token.isBlank()) {
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
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        _uiState.update { it.copy(activeSheet = null, contentDetails = null, returnToCalendar = false) }
        connectionDraft.value = null
    }

    fun backToCalendar() {
        _uiState.update { it.copy(activeSheet = AppSheet.UpcomingCalendar, contentDetails = null, returnToCalendar = false) }
    }

    fun openLibraryDetails(id: String) {
        val media = (_uiState.value.recentMovies + _uiState.value.recentSeries).firstOrNull { it.id == id } ?: return
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
        val media = _uiState.value.visibleDiscover.firstOrNull { it.id == id } ?: return
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
                                statusDescription = remote.seerrStatus?.let { seerrStatusDescription(it) } ?: details.statusDescription,
                                tagline = remote.tagline ?: details.tagline,
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

    fun openUpcomingDetails(id: String) {
        val media = _uiState.value.upcoming.firstOrNull { it.id == id } ?: return
        val fromCalendar = _uiState.value.activeSheet == AppSheet.UpcomingCalendar
        showLocalDetails(
            ContentDetails(
                key = media.id,
                title = media.title,
                eyebrow = "Kjem snart · ${media.source.displayName}",
                subtitle = media.subtitle,
                overview = media.overview ?: "Denne tittelen er overvaka og planlagd i ${media.source.displayName}.",
                facts = (media.facts + media.dateLabel + media.source.displayName).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = media.mediaType,
                statusTitle = "Planlagd utgjeving",
                statusDescription = "${media.dateLabel} · Datoen er venta, ikkje ei stadfesting på at tittelen er tilgjengeleg.",
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
        if (query.isBlank()) {
            _uiState.update {
                it.copy(searchQuery = value, searchResults = emptyList(), isSearching = false, searchError = null)
            }
            return
        }
        if (seerr == null) {
            _uiState.update {
                it.copy(
                    searchQuery = value,
                    searchResults = it.discover.filter { media -> media.title.contains(query, ignoreCase = true) },
                    isSearching = false,
                    searchError = null,
                )
            }
            return
        }
        _uiState.update {
            it.copy(searchQuery = value, searchResults = emptyList(), isSearching = true, searchError = null)
        }
        searchJob = viewModelScope.launch {
            delay(350)
            val result = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query) }
            }
            if (_uiState.value.searchQuery.trim() != query) return@launch
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(searchResults = result.getOrThrow(), isSearching = false, searchError = null)
                } else {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        searchError = "Fekk ikkje søkt i Seerr. Sjekk tilkoplinga og prøv igjen.",
                    )
                }
            }
        }
    }

    fun requestMedia(id: String) {
        val state = _uiState.value
        val media = state.visibleDiscover.firstOrNull { it.id == id } ?: return
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
                    searchResults = it.searchResults.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    contentDetails = it.contentDetails?.let { details ->
                        if (details.key == id) details.copy(statusTitle = "Lagd til lokalt", statusDescription = "Dette er ei førehandsvising. Ingenting er sendt til Seerr.") else details
                    },
                    snackbar = "Tittelen er lagd til lokalt · kople til Seerr for å sende han vidare",
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
        _uiState.update { it.copy(requestingMediaIds = it.requestingMediaIds + id) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    // Reconfirm the exact identity shown in the UI before any write.
                    container.mediaSyncRepository.request(seerr, media, expectedUserId = account.id)
                }
            }
            _uiState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        discover = current.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
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
                                source = ServiceKind.SEERR,
                                artworkRes = media.artworkRes,
                                artworkUrl = media.artworkUrl,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Sendt til Seerr som ${account.displayName}",
                    )
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Fekk ikkje sendt som ${account.displayName}. Sjekk Seerr-kontoen og tilgangen din.",
                    )
                }
            }
        }
    }

    fun refreshLiveData(userInitiated: Boolean = false) {
        refreshAccounts()
        if (refreshJob?.isActive == true) return
        val state = _uiState.value
        val configured = state.connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        if (configured.isEmpty()) {
            _uiState.update {
                it.copy(
                    sessions = demoSessions(),
                    recentMovies = demoRecentMovies(),
                    recentSeries = demoRecentSeries(),
                    upcoming = demoUpcoming(),
                    incoming = demoIncoming(),
                    discover = demoDiscover(),
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
            val snapshot = withContext(Dispatchers.IO) {
                container.mediaSyncRepository.refresh(
                    connections = _uiState.value.connections,
                )
            }
            if (snapshot.successfulServices.isNotEmpty() && snapshot.errors.isEmpty()) {
                withContext(Dispatchers.IO) { container.mediaSnapshotStore.save(snapshot) }
            }
            _uiState.update { current ->
                val configuredKinds = current.connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
                val configuredMedia = configuredKinds.intersect(setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
                val configuredQueue = configuredKinds.intersect(setOf(ServiceKind.RADARR, ServiceKind.SONARR))
                val mediaLive = snapshot.successfulServices.any { it in configuredMedia }
                val queueLive = snapshot.successfulServices.any { it in configuredQueue }
                val seerrLive = ServiceKind.SEERR in snapshot.successfulServices
                val activityLive = queueLive || seerrLive
                val anySuccess = snapshot.successfulServices.isNotEmpty()

                current.copy(
                    sessions = when {
                        configuredMedia.isEmpty() -> emptyList()
                        mediaLive -> snapshot.sessions
                        current.liveSession || current.hasCachedData -> current.sessions
                        else -> emptyList()
                    },
                    recentMovies = when {
                        configuredMedia.isEmpty() -> emptyList()
                        mediaLive -> (snapshot.recentMovies + current.recentMovies.filter { it.source in snapshot.errors })
                            .distinctBy(LibraryMedia::id)
                        current.liveLibrary || current.hasCachedData -> current.recentMovies
                        else -> emptyList()
                    },
                    recentSeries = when {
                        configuredMedia.isEmpty() -> emptyList()
                        mediaLive -> (snapshot.recentSeries + current.recentSeries.filter { it.source in snapshot.errors })
                            .distinctBy(LibraryMedia::id)
                        current.liveLibrary || current.hasCachedData -> current.recentSeries
                        else -> emptyList()
                    },
                    upcoming = when {
                        configuredQueue.isEmpty() -> emptyList()
                        queueLive -> (snapshot.upcoming + current.upcoming.filter { it.source in snapshot.errors })
                            .distinctBy(UpcomingMedia::id)
                            .sortedBy(UpcomingMedia::airDateEpochMillis)
                        current.liveIncoming || current.hasCachedData -> current.upcoming
                        else -> emptyList()
                    },
                    incoming = when {
                        configuredQueue.isEmpty() -> emptyList()
                        queueLive -> (snapshot.incoming + current.incoming.filter { it.source in snapshot.errors })
                            .distinctBy(IncomingMedia::id)
                        current.liveIncoming || current.hasCachedData -> current.incoming
                        else -> emptyList()
                    },
                    discover = when {
                        ServiceKind.SEERR !in configuredKinds -> emptyList()
                        seerrLive -> snapshot.discover
                        current.liveDiscover || current.hasCachedData -> current.discover
                        else -> emptyList()
                    },
                    activity = when {
                        configuredQueue.isEmpty() && ServiceKind.SEERR !in configuredKinds -> emptyList()
                        activityLive -> (snapshot.activity + current.activity.filter { event ->
                            event.source?.let(snapshot.errors::containsKey) == true
                        })
                            .distinctBy(ActivityEvent::id)
                        current.liveActivity || current.hasCachedData -> current.activity
                        else -> emptyList()
                    },
                    connections = current.connections.map { connection ->
                        when {
                            connection.baseUrl.isBlank() -> connection.copy(state = ConnectionState.DEMO, detail = "Demodata")
                            connection.kind in snapshot.errors -> connection.copy(
                                state = ConnectionState.ERROR,
                                detail = snapshot.errors.getValue(connection.kind),
                            )
                            connection.kind in snapshot.successfulServices -> connection.copy(
                                state = ConnectionState.CONNECTED,
                                detail = snapshot.warnings[connection.kind]?.let { "Tilkopla · $it" }
                                    ?: "Aktiv · oppdatert no",
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
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR) && it.baseUrl.isNotBlank() && it.token.isNotBlank()
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

    fun testAndSaveConnection() {
        val draft = connectionDraft.value ?: return
        val normalizedUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .getOrElse {
                updateDraft { copy(error = it.message ?: "Skriv inn ei gyldig tenaradresse") }
                return
            }
        val useJellyfinAccount = (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.ACCOUNT
        val useQuickConnect = (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (useQuickConnect) {
            startQuickConnect(draft, normalizedUrl)
            return
        }
        if (useJellyfinAccount && draft.username.isBlank()) {
            updateDraft { copy(error = "Skriv inn Jellyfin-brukarnamnet") }
            return
        }
        if (!useJellyfinAccount && draft.token.isBlank()) {
            updateDraft { copy(error = "Skriv inn ein API-nøkkel eller eit tilgangsteikn") }
            return
        }
        updateDraft { copy(url = normalizedUrl, saving = true, error = null) }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            val credentials = if (useJellyfinAccount) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        if (draft.kind == ServiceKind.SEERR) {
                            container.seerrAuthenticationClient.authenticate(normalizedUrl, draft.username.trim(), draft.password)
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
                state = ConnectionState.TESTING,
            )
            verifyAndSaveConnection(candidate)
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

    private suspend fun verifyAndSaveConnection(candidate: ServiceConnection) {
        val result = runCatching {
            withContext(Dispatchers.IO) { container.connectionTester.test(candidate) }
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

        withContext(Dispatchers.IO) { container.connectionRepository.save(candidate) }
        val saved = candidate.copy(
            state = ConnectionState.CONNECTED,
            latencyMs = result.latencyMs,
            detail = result.message,
        )
        _uiState.update { state ->
            state.copy(
                connections = state.connections.map { if (it.kind == saved.kind) saved else it },
                accounts = state.accounts - saved.kind,
                accountErrors = state.accountErrors - saved.kind,
                sessions = if (state.configuredCount == 0) emptyList() else state.sessions,
                recentMovies = if (state.configuredCount == 0) emptyList() else state.recentMovies,
                recentSeries = if (state.configuredCount == 0) emptyList() else state.recentSeries,
                upcoming = if (state.configuredCount == 0) emptyList() else state.upcoming,
                incoming = if (state.configuredCount == 0) emptyList() else state.incoming,
                discover = if (state.configuredCount == 0) emptyList() else state.discover,
                activity = if (state.configuredCount == 0) emptyList() else state.activity,
                searchQuery = "",
                searchResults = emptyList(),
                activeSheet = null,
                snackbar = "${saved.kind.displayName} er klar. Hentar innhaldet ditt…",
            )
        }
        connectionDraft.value = null
        refreshLiveData()
    }

    fun removeConnection(kind: ServiceKind) {
        if (_uiState.value.requestingMediaIds.isNotEmpty()) return
        container.connectionRepository.delete(kind)
        val remaining = container.connectionRepository.list()
        if (remaining.none { it.baseUrl.isNotBlank() }) container.mediaSnapshotStore.clear()
        _uiState.update {
            it.copy(
                connections = remaining,
                accounts = it.accounts - kind,
                accountErrors = it.accountErrors - kind,
                activeSheet = null,
                failedServices = it.failedServices - kind,
                snackbar = "Tilkoplinga til ${kind.displayName} er fjerna",
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
    val cached = if (configuredKinds.isNotEmpty()) container.mediaSnapshotStore.read() else null

    return ReelstackUiState(
        showOnboarding = configuredKinds.isEmpty() && !container.preferencesRepository.onboardingCompleted,
        connections = connections,
        sessions = when {
            hasMediaServer && cached != null -> cached.sessions
            hasMediaServer -> emptyList()
            else -> if (configuredKinds.isEmpty()) demoSessions() else emptyList()
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

private fun demoRecentMovies() = listOf(
    LibraryMedia(
        id = "recent-odyssey",
        title = "The Odyssey",
        subtitle = "Film · 2026",
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.JELLYFIN,
        mediaType = "Movie",
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
    ),
)

private fun demoUpcoming() = listOf(
    UpcomingMedia(
        id = "upcoming-andor",
        title = "Andor",
        subtitle = "S02 E07 · Messenger",
        dateLabel = "I kveld · 21:00",
        airDateEpochMillis = System.currentTimeMillis() + 3_600_000,
        artworkRes = R.drawable.kitchen_request,
        source = ServiceKind.SONARR,
        mediaType = "Episode",
    ),
    UpcomingMedia(
        id = "upcoming-odyssey",
        title = "The Odyssey",
        subtitle = "Film · 2026",
        dateLabel = "I morgon",
        airDateEpochMillis = System.currentTimeMillis() + 86_400_000,
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.RADARR,
        mediaType = "Movie",
    ),
)

private fun demoIncoming() = listOf(
    IncomingMedia(
        id = "dune-messiah",
        title = "Dune: Messiah",
        source = ServiceKind.RADARR,
        status = "Lastar ned 68 %",
        state = IncomingState.DOWNLOADING,
        artworkRes = R.drawable.desert_arrival,
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

private fun demoDiscover() = listOf(
    DiscoverMedia("last-horizon", "The Last Horizon", "Film · 2026", R.drawable.desert_arrival, false),
    DiscoverMedia("service", "Service", "Serie · 3 sesongar", R.drawable.kitchen_request, true),
)

private fun demoActivity() = listOf(
    ActivityEvent("odyssey", "The Odyssey", "Godkjend i Seerr", "For 2 min sidan", complete = false, source = ServiceKind.SEERR, artworkRes = R.drawable.desert_arrival),
    ActivityEvent("alien-earth", "Alien: Earth", "Sonarr · lastar ned 42 %", "For 8 min sidan", progress = 42, source = ServiceKind.SONARR, artworkRes = R.drawable.kitchen_request),
    ActivityEvent("mickey-17", "Mickey 17", "Importert av Radarr", "I går", complete = true, source = ServiceKind.RADARR, artworkRes = R.drawable.desert_arrival),
)
