package app.reelstack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.data.repository.MediaSnapshotStore
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
import app.reelstack.data.model.quotaExceeded
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.network.EndpointValidator
import app.reelstack.data.network.readableMessage
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.state.toDiscoverUiState
import app.reelstack.ui.state.toHomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

enum class AppTab { HOME, LIBRARY, DOWNLOADS, DISCOVER, ACTIVITY, SETTINGS }

enum class ConnectionAuthMode { QUICK_CONNECT, ACCOUNT, API_KEY }

sealed interface AppSheet {
    data class SessionDetails(val sessionKey: String) : AppSheet
    data class TitleDetails(val key: String) : AppSheet
    data object RequestComposer : AppSheet
    data object UpcomingCalendar : AppSheet
    data class ConnectionEditor(val kind: ServiceKind) : AppSheet
    data object ProfileSwitcher : AppSheet
    data object AddProfile : AppSheet
    data class PinPrompt(
        val targetProfileId: String,
        val isSetup: Boolean = false,
        val switchAfterSetup: Boolean = true,
    ) : AppSheet
}

/** Framside → Episodar → Spelar. Three levels, and this is the middle one. */
data class KidsBrowse(
    val seriesId: String = "",
    val title: String = "",
    val source: ServiceKind? = null,
    val seasons: List<LibraryMedia> = emptyList(),
    val selectedSeasonId: String = "",
    val episodes: List<LibraryMedia> = emptyList(),
    val loading: Boolean = false,
) {
    val open: Boolean get() = seriesId.isNotBlank()
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
    val simpleSetup: Boolean = false,
    val setupImported: Boolean = false,
    val companionUrl: String = "",
    val alternateUrl: String = "",
    val saving: Boolean = false,
    val quickConnectCode: String? = null,
    val quickConnectWaiting: Boolean = false,
    val error: String? = null,
    val warning: String? = null,
)

data class ReelstackUiState(
    val signingOut: Boolean = false,
    val showOnboarding: Boolean = false,
    val selectedTab: AppTab = AppTab.HOME,
    /** A destination over the current adult screen, deliberately not another navigation tab. */
    val globalSearchOpen: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val accountsSettingsRequest: Int = 0,
    val activeSheet: AppSheet? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val activeProfileId: String = "",
    val profiles: List<app.reelstack.data.model.UserProfile> = emptyList(),
    val allProfileConnections: Map<String, List<ServiceConnection>> = emptyMap(),
    val isKidMode: Boolean = false,
    val publicUsers: List<app.reelstack.data.network.PublicUser> = emptyList(),
    val loadingPublicUsers: Boolean = false,
    val pinConfigured: Boolean = false,
    val pinError: String? = null,
    val pinLockoutSeconds: Int = 0,
    val addProfileError: String? = null,
    /** False when no Jellyfin or Emby address is stored, so the add-profile sheet can say so instead of offering a form that cannot work. */
    val addProfileHasServer: Boolean = true,
    /** Media servers the add-profile sheet may sign in against, in the order it offers them. */
    val addProfileServers: List<ServiceConnection> = emptyList(),
    /**
     * The one series a kid has opened, if any.
     *
     * Kept apart from [seriesBrowse] on purpose: that one hangs off the adult detail sheet, and
     * the kid shell has no detail sheet to hang anything from.
     */
    val kidsBrowse: KidsBrowse = KidsBrowse(),
    /** Everything the kid's own account may open, from the server's own library views. */
    val kidsLibrary: List<LibraryMedia> = emptyList(),
    val kidsLibraries: List<app.reelstack.data.network.RemoteLibraryView> = emptyList(),
    val kidsLibraryNames: List<Pair<String, String>> = emptyList(),
    val kidsLibraryLoading: Boolean = false,
    val kidsLibraryError: Boolean = false,
    val accounts: Map<ServiceKind, ServiceAccount> = emptyMap(),
    val accountErrors: Map<ServiceKind, String> = emptyMap(),
    val loadingAccounts: Set<ServiceKind> = emptySet(),
    val sessions: List<PlaybackSession> = demoSessions(),
    val resume: List<LibraryMedia> = demoResume(),
    val nextUp: List<LibraryMedia> = emptyList(),
    val favourites: List<LibraryMedia> = emptyList(),
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
    val libraryChoices: List<app.reelstack.data.network.RemoteLibraryView> = emptyList(),
    val selectedLibrarySource: ServiceKind = ServiceKind.JELLYFIN,
    val selectedLibraryIds: Set<String> = emptySet(),
    val libraryShortcuts: List<Pair<String, String>> = emptyList(),
    val libraryIcons: Map<String, app.reelstack.data.model.LibraryIcon> = emptyMap(),
    val libraryFilters: app.reelstack.data.model.LibraryFilters = app.reelstack.data.model.LibraryFilters(),
    val libraryFacets: app.reelstack.data.model.LibraryFacets = app.reelstack.data.model.LibraryFacets(),
    val libraryChoicesOpen: Boolean = false,
    val libraryChoicesLoading: Boolean = false,
    val libraryChoicesError: String? = null,
    val libraryDetailMedia: LibraryMedia? = null,
    val libraryEntries: List<app.reelstack.data.network.RemoteLibraryItem> = emptyList(),
    val libraryShelves: app.reelstack.data.model.LibraryShelves = app.reelstack.data.model.LibraryShelves(),
    /**
     * What is newest in each library, keyed by library id.
     *
     * Only the page that lists the libraries uses this, and it asks once: arrowing between four
     * libraries must not fire four requests, so the whole map is loaded together and kept.
     */
    val libraryPeeks: Map<String, List<LibraryMedia>> = emptyMap(),
    val libraryPeeksLoading: Boolean = false,
    /** The open series' seasons and the episodes of the season being looked at. */
    val seriesBrowse: app.reelstack.data.model.SeriesBrowse = app.reelstack.data.model.SeriesBrowse(),
    /** Why the last watched/favourite/remove press did nothing. Cleared on the next attempt. */
    val mediaActionError: String? = null,
    val libraryPath: List<Pair<String, String>> = emptyList(),
    val libraryCollectionType: String? = null,
    val libraryLoading: Boolean = false,
    val libraryError: String? = null,
    val libraryOffset: Int = 0,
    val libraryHasMore: Boolean = false,
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
    /** Local Media3 state for the active adult profile. No server request is needed to render it. */
    val offlineDownloads: app.reelstack.offline.OfflineDownloadsSnapshot = app.reelstack.offline.OfflineDownloadsSnapshot(),
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
    val homeRowOrder: List<app.reelstack.data.model.HomeRow> = app.reelstack.data.model.HomeRow.entries,
    val hasCachedData: Boolean = false,
    val snackbar: String? = null,
    val contentDetails: ContentDetails? = null,
    val returnToCalendar: Boolean = false,
    val requestDraft: RequestDraft? = null,
    val trackedRequests: List<TrackedRequest> = emptyList(),
    val requestHistory: app.reelstack.data.model.RequestHistoryState = app.reelstack.data.model.RequestHistoryState(),
    val showRequestHistory: Boolean = false,
    val trackingError: String? = null,
    val trackingLoading: Boolean = false,
    /** Keys of follows currently being withdrawn, so a card cannot be cancelled twice. */
    val cancellingRequestKeys: Set<String> = emptySet(),
    val adminView: Boolean = false,
) {
    val libraryConnection: ServiceConnection?
        get() = connections.filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
            it.baseUrl.isNotBlank() && it.token.isNotBlank() }.let { available ->
            available.firstOrNull { it.kind == selectedLibrarySource } ?: available.firstOrNull()
        }
    val librarySource: ServiceKind get() = libraryConnection?.kind ?: selectedLibrarySource

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
    private data class LibraryCacheKey(
        val accountFingerprint: String,
        val source: ServiceKind,
        val path: String,
        val collectionType: String?,
        val filters: app.reelstack.data.model.LibraryFilters,
    )

    private data class CachedLibraryPage(
        val entries: List<app.reelstack.data.network.RemoteLibraryItem>,
        val facets: app.reelstack.data.model.LibraryFacets,
        val hasMore: Boolean,
    )

    /**
     * Keeps visited library pages warm while the app is alive. The server remains authoritative,
     * but returning to Films or Series should show the last page immediately while a quiet refresh
     * checks for new titles in the background.
     */
    private val libraryPageCache = LinkedHashMap<LibraryCacheKey, CachedLibraryPage>(8, .75f, true)

    private fun libraryCacheKey(connection: ServiceConnection, state: ReelstackUiState): LibraryCacheKey =
        LibraryCacheKey(
            accountFingerprint = MediaSnapshotStore.fingerprint(listOf(connection)),
            source = connection.kind,
            path = state.libraryPath.joinToString("/") { it.first },
            collectionType = state.libraryCollectionType,
            filters = state.libraryFilters,
        )

    /**
     * The word for a kind of title.
     *
     * The sync layer derives this from `mediaType` for the rows it builds; the sheet builds its own
     * from a card the user tapped, so it needs the same answer from the same place.
     */
    @androidx.annotation.StringRes
    private fun mediaKindRes(mediaType: String?): Int = when (mediaType?.lowercase()) {
        "movie" -> R.string.media_kind_movie
        "series", "tv" -> R.string.media_kind_series
        "episode" -> R.string.media_kind_episode
        else -> R.string.media_kind_video
    }

    /** A list of decisions, rendered once in the language the app is set to. */
    private fun words(items: List<app.reelstack.localization.LocalizedText>): List<String> =
        items.map { it.text(container.appContext) }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)

    private fun appQuantityString(@androidx.annotation.PluralsRes resId: Int, quantity: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).resources.getQuantityString(resId, quantity, *args)

    private val _uiState = MutableStateFlow(initialState(container))
    val uiState: StateFlow<ReelstackUiState> = _uiState.asStateFlow()
    /** Home observes only the data it renders, not every global sheet or profile mutation. */
    val homeUiState: StateFlow<app.reelstack.ui.state.HomeUiState> = uiState
        .map(ReelstackUiState::toHomeUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = _uiState.value.toHomeUiState(),
        )
    /** Discover and global search do not need to observe sheet, player or kid-profile mutations. */
    val discoverUiState: StateFlow<DiscoverUiState> = uiState
        .map(ReelstackUiState::toDiscoverUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = _uiState.value.toDiscoverUiState(),
        )

    var connectionDraft = MutableStateFlow<ConnectionDraft?>(null)
        private set

    private var signOutJob: Job? = null
    private var libraryChoicesJob: Job? = null
    private var libraryJob: Job? = null
    private var shelfJob: Job? = null
    private var peekJob: Job? = null
    // Two handles, not one. Loading the seasons ends by opening the first of them, and a single
    // handle meant that call cancelled the very coroutine it was running inside — correct today
    // only because nothing suspends between the cancel and the relaunch, which is not a property
    // anybody should have to verify to change this code.
    private var seasonsJob: Job? = null
    private var episodesJob: Job? = null
    private data class EpisodeReturn(val seriesKey: String, val connection: ServiceConnection,
        val details: ContentDetails, val browse: app.reelstack.data.model.SeriesBrowse)
    private var episodeReturn: EpisodeReturn? = null
    private val detailHistory = ArrayDeque<EpisodeReturn>()
    private var quickConnectJob: Job? = null
    private var connectionJob: Job? = null
    private var trackingJob: Job? = null
    private var historyJob: Job? = null
    private var requestDraftJob: Job? = null
    private var offlineRefreshJob: Job? = null
    /** In-memory only: duplicate presses must not create two jobs for the same private file. */
    private val preparingOfflineIds = mutableSetOf<String>()
    private val accountRefresh = AccountRefreshCoordinator(
        container = container,
        scope = viewModelScope,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
    )
    private val discoverSearch = DiscoverSearchCoordinator(
        container = container,
        scope = viewModelScope,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
    )
    private val homeFeed = HomeFeedCoordinator(
        container = container,
        scope = viewModelScope,
        readState = { _uiState.value },
        updateState = { transform -> _uiState.update(transform) },
        refreshAccounts = ::refreshAccounts,
        refreshTrackedRequests = { refreshTrackedRequests() },
        openLibraryDetails = { key -> openLibraryDetails(key) },
        isSigningOut = { signOutJob?.isActive == true },
    )

    /** Whether Jellyfin is currently pushing playback-change notifications. */
    val sessionChannelLive: StateFlow<Boolean> = homeFeed.sessionChannelLive

    val profileViewModel = app.reelstack.ui.viewmodels.ProfileViewModel(
        connectionRepository = container.connectionRepository,
        pinSecurity = container.pinSecurity,
        jellyfinAuthClient = container.jellyfinAuthenticationClient,
        embyAuthClient = container.embyAuthenticationClient,
        appContext = container.appContext,
    )
    val libraryViewModel = app.reelstack.ui.viewmodels.LibraryViewModel(
        preferencesRepository = container.preferencesRepository,
        mediaServerClient = container.mediaServerClient,
        appContext = container.appContext,
    )
    val requestsViewModel = app.reelstack.ui.viewmodels.RequestsViewModel(
        mediaServerClient = container.mediaServerClient,
        appContext = container.appContext,
    )

    init {
        viewModelScope.launch {
            container.localPlaybackStore.changes.collect {
                _uiState.update { it.copy(resume = localResume(it.resume, it.connections), nextUp = localNextUp(it.nextUp, it.connections)) }
            }
        }
        viewModelScope.launch {
            profileViewModel.uiState.collect { pState ->
                _uiState.update { current ->
                    current.copy(
                        activeProfileId = pState.activeProfileId,
                        profiles = pState.profiles,
                        allProfileConnections = pState.allProfileConnections,
                        isKidMode = pState.isKidMode,
                        publicUsers = pState.publicUsers,
                        loadingPublicUsers = pState.loadingPublicUsers,
                        pinConfigured = pState.pinConfigured,
                        pinError = pState.pinError,
                        pinLockoutSeconds = pState.pinLockoutSeconds,
                        addProfileError = pState.addProfileError,
                        addProfileHasServer = pState.addProfileHasServer,
                        addProfileServers = pState.addProfileServers,
                    )
                }
            }
        }
        viewModelScope.launch {
            requestsViewModel.uiState.collect { rState ->
                _uiState.update { current ->
                    current.copy(
                        requestDraft = rState.requestDraft,
                        requestingMediaIds = rState.requestingMediaIds,
                    )
                }
            }
        }
        hydrateCachedFeed()
        refreshLiveData()
        if (_uiState.value.selectedTab == AppTab.LIBRARY) browseLibrary(false)
    }

    fun selectTab(tab: AppTab) {
        // Downloads are intentionally a personal mobile/tablet space. There is no TV or kids-mode
        // loophole through a notification, a restored tab, or a direct ViewModel call.
        if (tab == AppTab.DOWNLOADS && (_uiState.value.isKidMode || isTelevision())) return
        // Keep the last library location when moving between tabs. The library loader can refresh
        // it in place, but dropping the path made a return to Films look like a new visit to the
        // library root every time.
        _uiState.update { it.copy(selectedTab = tab, activeSheet = null, returnToCalendar = false, globalSearchOpen = false) }
        if (tab == AppTab.LIBRARY) browseLibrary(false)
        if (tab == AppTab.DOWNLOADS) startOfflineRefresh()
        else offlineRefreshJob?.cancel()
    }

    private fun startOfflineRefresh() {
        offlineRefreshJob?.cancel()
        val profileId = _uiState.value.activeProfileId
        offlineRefreshJob = viewModelScope.launch {
            while (isActive && _uiState.value.selectedTab == AppTab.DOWNLOADS &&
                _uiState.value.activeProfileId == profileId && !_uiState.value.isKidMode) {
                val snapshot = withContext(Dispatchers.IO) { container.offlineDownloads.snapshot(profileId) }
                _uiState.update { current ->
                    if (current.activeProfileId == profileId && current.selectedTab == AppTab.DOWNLOADS) current.copy(offlineDownloads = snapshot)
                    else current
                }
                delay(1_000L)
            }
        }
    }

    fun refreshOfflineDownloads() {
        if (_uiState.value.isKidMode || isTelevision()) return
        val profileId = _uiState.value.activeProfileId
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { container.offlineDownloads.snapshot(profileId) }
            _uiState.update { current -> if (current.activeProfileId == profileId) current.copy(offlineDownloads = snapshot) else current }
        }
    }

    /**
     * A detail page chooses a concrete library file, then negotiates it without opening the
     * player. The Media3 job is created only after the server confirms untouched direct playback;
     * a profile switch during that short negotiation drops the result instead of lending the old
     * account to the new profile.
     */
    fun enqueueOfflineDownload(media: LibraryMedia, audioIndex: Int? = null, subtitleIndex: Int? = null,
        versionId: String? = null) {
        val state = _uiState.value
        if (state.isKidMode || isTelevision() || !media.mediaType.equals("Movie", true) &&
            !media.mediaType.equals("Episode", true) && !media.mediaType.equals("Video", true)) return
        val remoteId = media.remoteId ?: run {
            _uiState.update { it.copy(snackbar = appString(R.string.offline_prepare_failed)) }
            return
        }
        val connection = state.connections.firstOrNull { connection ->
            connection.kind == media.source && connection.baseUrl.isNotBlank() && connection.token.isNotBlank()
        } ?: run {
            _uiState.update { it.copy(snackbar = appString(R.string.offline_prepare_failed)) }
            return
        }
        val profileId = state.activeProfileId
        val pendingKey = "$profileId|${connection.identity}|$remoteId"
        if (!preparingOfflineIds.add(pendingKey)) return
        viewModelScope.launch {
            try {
                val prepared = runCatching {
                    withContext(Dispatchers.IO) {
                        val playback = app.reelstack.player.MediaPlaybackClient(
                            deviceId = app.reelstack.data.repository.DeviceIdentity.get(container.appContext),
                        )
                        val userId = playback.verify(connection)
                        val item = playback.item(connection, userId, remoteId)
                        val plan = playback.prepare(connection, userId, item, bitrate = 80_000_000,
                            audio = audioIndex, subtitle = subtitleIndex, sourceId = versionId)
                        app.reelstack.offline.OfflineDownloadRequest(
                            profileId = profileId,
                            connection = connection,
                            candidate = app.reelstack.offline.OfflineMediaCandidate(
                                itemId = item.id,
                                service = connection.kind,
                                requiresTranscode = !plan.direct,
                                directDownloadUrl = plan.url,
                            ),
                            title = item.title,
                            subtitle = item.subtitle,
                            mediaType = item.type,
                        )
                    }
                }.getOrNull()
                if (prepared == null) {
                    _uiState.update { it.copy(snackbar = appString(R.string.offline_prepare_failed)) }
                    return@launch
                }
                // The profile is the access boundary for the encrypted catalogue and the active
                // download worker. Never enqueue a request negotiated before a profile changed.
                if (_uiState.value.activeProfileId != profileId || _uiState.value.isKidMode || isTelevision()) return@launch
                val refusal = withContext(Dispatchers.IO) { container.offlineDownloads.enqueue(prepared) }
                _uiState.update { it.copy(snackbar = appString(if (refusal == null) R.string.offline_queued else R.string.offline_direct_only)) }
                if (refusal == null) refreshOfflineDownloads()
            } finally {
                preparingOfflineIds.remove(pendingKey)
            }
        }
    }

    fun pauseOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        container.offlineDownloads.pause(id)
        refreshOfflineDownloads()
    }

    fun resumeOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        container.offlineDownloads.resume(id)
        refreshOfflineDownloads()
    }

    fun retryOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        if (!container.offlineDownloads.retry(id)) return
        refreshOfflineDownloads()
    }

    fun removeOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        container.offlineDownloads.remove(_uiState.value.activeProfileId, id)
        refreshOfflineDownloads()
    }

    fun pauseOfflineDownloads() {
        if (_uiState.value.isKidMode) return
        container.offlineDownloads.pauseAll()
        refreshOfflineDownloads()
    }

    fun resumeOfflineDownloads() {
        if (_uiState.value.isKidMode) return
        container.offlineDownloads.resumeAll()
        refreshOfflineDownloads()
    }

    fun openGlobalSearch() {
        if (_uiState.value.isKidMode) return
        _uiState.update { it.copy(globalSearchOpen = true, activeSheet = null, searchHistory = container.preferencesRepository.searchHistory(it.activeProfileId)) }
    }

    fun closeGlobalSearch() {
        _uiState.update { it.copy(globalSearchOpen = false) }
    }

    /** Forget catalog pages without touching artwork, playback history, or server credentials. */
    fun clearLibraryCache() {
        libraryJob?.cancel()
        shelfJob?.cancel()
        peekJob?.cancel()
        libraryPageCache.clear()
        _uiState.update {
            it.copy(
                libraryEntries = emptyList(),
                libraryFacets = app.reelstack.data.model.LibraryFacets(),
                libraryShelves = app.reelstack.data.model.LibraryShelves(),
                libraryPeeks = emptyMap(),
                libraryPeeksLoading = false,
                libraryOffset = 0,
                libraryHasMore = false,
                libraryLoading = false,
                libraryError = null,
            )
        }
        if (_uiState.value.selectedTab == AppTab.LIBRARY) browseLibrary(false)
    }

    fun selectLibrarySource(source: ServiceKind) {
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == source && source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        container.preferencesRepository.preferredLibrarySource = source
        libraryJob?.cancel()
        peekJob?.cancel()
        shelfJob?.cancel()
        libraryChoicesJob?.cancel()
        libraryPageCache.clear()
        _uiState.update { it.copy(selectedLibrarySource = source,
            libraryPath = emptyList(), libraryCollectionType = null, libraryEntries = emptyList(),
            libraryPeeks = emptyMap(), libraryPeeksLoading = false,
            libraryShelves = app.reelstack.data.model.LibraryShelves(),
            libraryFilters = app.reelstack.data.model.LibraryFilters(),
            libraryFacets = app.reelstack.data.model.LibraryFacets(),
            libraryLoading = false, libraryError = null, libraryOffset = 0, libraryHasMore = false,
            libraryChoicesOpen = false, libraryChoices = emptyList(), selectedLibraryIds = emptySet(),
            libraryShortcuts = container.preferencesRepository.libraryShortcuts(connection),
            libraryIcons = container.preferencesRepository.libraryIcons(connection)) }
        browseLibrary()
    }

    fun openLibraryChoices() {
        val connection = _uiState.value.libraryConnection ?: return
        libraryChoicesJob?.cancel()
        _uiState.update { it.copy(libraryChoicesOpen = true, libraryChoicesLoading = true, libraryChoicesError = null, libraryChoices = emptyList()) }
        libraryChoicesJob = viewModelScope.launch {
            try {
                val choices = withContext(Dispatchers.IO) { container.mediaServerClient.browseLibraries(connection) }
                if (!isActive) return@launch
                _uiState.update { it.copy(libraryChoicesLoading = false, libraryChoices = choices,
                    selectedLibraryIds = choices.filter { view -> container.preferencesRepository.includesLibrary(connection, view) }.mapTo(mutableSetOf()) { view -> view.id }) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(libraryChoicesLoading = false,
                    libraryChoicesError = error.readableMessage(container.appContext) ?: appString(R.string.library_failed)) }
            }
        }
    }

    fun closeLibraryChoices() {
        libraryChoicesJob?.cancel()
        _uiState.update { it.copy(libraryChoicesOpen = false) }
    }

    /** [shortcuts] is ordered: the menu shows them in exactly the order it is given. */
    fun saveLibraryChoices(ids: Set<String>, shortcuts: List<String> = _uiState.value.libraryShortcuts.map { it.first },
        icons: Map<String, app.reelstack.data.model.LibraryIcon> = _uiState.value.libraryIcons) {
        val state = _uiState.value
        if (!state.libraryChoicesOpen || state.libraryChoicesLoading || state.libraryChoicesError != null) return
        val connection = state.libraryConnection ?: return
        container.preferencesRepository.setSelectedLibraryIds(connection, ids.intersect(state.libraryChoices.map { it.id }.toSet()))
        // The chosen order, not the server's. A menu the reader arranged has to stay arranged.
        val pinned = shortcuts.distinct().filter { it in ids }
            .mapNotNull { id -> state.libraryChoices.firstOrNull { it.id == id }?.let { id to it.name } }
        container.preferencesRepository.setLibraryShortcuts(connection, pinned)
        val savedIcons = icons.filterKeys { key -> state.libraryChoices.any { it.id == key } }
        container.preferencesRepository.setLibraryIcons(connection, savedIcons)
        homeFeed.cancelRefresh()
        libraryChoicesJob?.cancel()
        libraryJob?.cancel()
        discoverSearch.cancel()
        libraryPageCache.clear()
        _uiState.update { it.copy(libraryChoicesOpen = false, selectedLibraryIds = ids, libraryShortcuts = pinned, libraryIcons = savedIcons,
            libraryPath = emptyList(), libraryEntries = emptyList(), libraryDetailMedia = null,
            libraryShelves = app.reelstack.data.model.LibraryShelves(),
            libraryPeeks = emptyMap(), libraryPeeksLoading = false,
            resume = emptyList(), nextUp = emptyList(), recentMovies = emptyList(), recentSeries = emptyList(), recentReleases = emptyList(),
            librarySearchResults = emptyList(), isSearching = false, hasCachedData = false) }
        container.localPlaybackStore.clear()
        browseLibrary()
        refreshLiveData()
        if (state.searchQuery.isNotBlank()) setSearchQuery(state.searchQuery)
    }

    fun browseLibrary(more: Boolean = false) {
        if (!more) loadLibraryShelves()
        if (more && (_uiState.value.libraryLoading || !_uiState.value.libraryHasMore)) return
        libraryJob?.cancel()
        val state = _uiState.value
        val connection = state.libraryConnection ?: return
        val path = state.libraryPath
        val offset = if (more) state.libraryOffset else 0
        val cacheKey = libraryCacheKey(connection, state)
        val cached = libraryPageCache[cacheKey]
        _uiState.update { it.copy(libraryError = null,
            // Stale-while-revalidate: a visited page is useful immediately, while the request
            // below quietly confirms it against Jellyfin/Emby. New pages still show loading.
            libraryLoading = cached == null,
            libraryEntries = if (more) it.libraryEntries else cached?.entries ?: it.libraryEntries,
            libraryFacets = cached?.facets ?: it.libraryFacets,
            libraryOffset = if (more) it.libraryOffset else cached?.entries?.size ?: it.libraryOffset,
            libraryHasMore = if (more) it.libraryHasMore else cached?.hasMore ?: it.libraryHasMore) }
        libraryJob = viewModelScope.launch {
            try {
                val facetJob = async(Dispatchers.IO) {
                    if (path.isEmpty()) app.reelstack.data.model.LibraryFacets()
                    else if (state.libraryFacets.parentId == path.last().first) state.libraryFacets
                    else runCatching { container.mediaServerClient.libraryFacets(connection, path.last().first) }
                        .getOrDefault(app.reelstack.data.model.LibraryFacets())
                }
                val entries = withContext(Dispatchers.IO) {
                    if (path.isEmpty()) container.mediaServerClient.browseLibraries(connection).filter { container.preferencesRepository.includesLibrary(connection, it) }.map { view ->
                        app.reelstack.data.network.RemoteLibraryItem(view.id, view.name, "", null, "CollectionFolder", view.id,
                            artworkUrl = view.artworkUrl, isFolder = true, collectionType = view.collectionType)
                    } else {
                        val catalogueType = if (path.size == 1) state.libraryCollectionType ?:
                            container.mediaServerClient.browseLibraries(connection).firstOrNull { it.id == path.first().first }?.collectionType else null
                        container.mediaServerClient.browseLibrary(connection, path.last().first, offset, catalogueType, state.libraryFilters)
                    }
                }
                val facets = facetJob.await()
                if (!isActive || _uiState.value.connections.none { it.kind == connection.kind && it.baseUrl == connection.baseUrl && it.token == connection.token && it.userId == connection.userId }) return@launch
                _uiState.update { current ->
                    val merged = if (more) {
                        (current.libraryEntries + entries).distinctBy { entry -> entry.id }
                    } else {
                        // Keep already fetched pages after the freshly confirmed first page so a
                        // return to a scrolled library does not throw away the user's position.
                        (entries + (cached?.entries.orEmpty().drop(entries.size))).distinctBy { entry -> entry.id }
                    }
                    val hasMore = path.isNotEmpty() && (entries.size == 60 || (cached?.hasMore == true && !more))
                    libraryPageCache[cacheKey] = CachedLibraryPage(merged, facets, hasMore)
                    current.copy(libraryLoading = false, libraryFacets = facets,
                        libraryEntries = merged,
                        libraryOffset = merged.size,
                        libraryHasMore = hasMore)
                }
                // The libraries this listing actually found — not the pinned shortcuts, which are a
                // menu choice and can be empty while the page still shows every library there is.
                if (path.isEmpty()) loadLibraryPeeks(entries.map {
                    app.reelstack.data.network.RemoteLibraryView(it.id, it.title, it.collectionType, it.artworkUrl)
                })
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (isActive) _uiState.update { it.copy(libraryLoading = false, libraryError = error.readableMessage(container.appContext) ?: container.appContext.getString(R.string.library_failed)) }
            }
        }
    }

    fun libraryBack() {
        _uiState.update { it.copy(libraryPath = it.libraryPath.dropLast(1), libraryFilters = app.reelstack.data.model.LibraryFilters()) }
        browseLibrary()
    }

    /**
     * What this library was left half-watched at.
     *
     * Only for the root of one library: inside a series the grid is already the episode list, and a
     * shelf above it would repeat it. Failures are silent by design — a shelf is an extra, and a
     * library that cannot produce one should still open.
     */
    /**
     * The peek into every library, for the page that lists them.
     *
     * Asked once per sign-in and kept: the landing page is arrowed through, not scrolled, and a
     * request per keypress would make the page feel worse than the four tiles it replaces. Failure
     * is silent — the tiles are still there, they just have nothing under them.
     */
    private fun loadLibraryPeeks(views: List<app.reelstack.data.network.RemoteLibraryView>) {
        val state = _uiState.value
        // A library with nothing in it never lands in the map, so "already asked" cannot be read off
        // the keys. The loading flag covers the request in flight; the map covers the answer.
        if (views.isEmpty() || state.libraryPeeksLoading || state.libraryPeeks.isNotEmpty()) return
        val connection = state.libraryConnection ?: return
        peekJob?.cancel()
        _uiState.update { it.copy(libraryPeeks = emptyMap(), libraryPeeksLoading = true) }
        peekJob = viewModelScope.launch {
            // One request per library, all in flight together, and each row appears the moment its
            // own answer arrives. Waiting for the slowest library before drawing any of them is how
            // a page that is mostly ready still feels like it is loading.
            views.map { view ->
                async(Dispatchers.IO) { view.id to runCatching { container.mediaSyncRepository.libraryPeek(connection, view) } }
            }.forEach { pending ->
                val (id, loaded) = pending.await()
                if (!isActive) return@launch
                val items = loaded.getOrNull().orEmpty()
                if (items.isEmpty()) return@forEach
                _uiState.update { it.copy(libraryPeeks = it.libraryPeeks + (id to items)) }
            }
            if (isActive) _uiState.update { it.copy(libraryPeeksLoading = false) }
        }
    }

    private fun loadLibraryShelves() {
        val state = _uiState.value
        val path = state.libraryPath
        val library = path.singleOrNull()
        if (library == null) {
            shelfJob?.cancel()
            if (state.libraryShelves != app.reelstack.data.model.LibraryShelves()) {
                _uiState.update { it.copy(libraryShelves = app.reelstack.data.model.LibraryShelves()) }
            }
            return
        }
        if (state.libraryShelves.libraryId == library.first && !state.libraryShelves.loading) return
        val connection = state.libraryConnection ?: return
        shelfJob?.cancel()
        _uiState.update { it.copy(libraryShelves = app.reelstack.data.model.LibraryShelves(libraryId = library.first, loading = true)) }
        shelfJob = viewModelScope.launch {
            val view = app.reelstack.data.network.RemoteLibraryView(library.first, library.second, _uiState.value.libraryCollectionType)
            val loaded = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.libraryShelves(connection, view) }
            }.getOrNull()
            if (!isActive) return@launch
            _uiState.update { current ->
                if (current.libraryShelves.libraryId != library.first) return@update current
                current.copy(libraryShelves = current.libraryShelves.copy(
                    resume = loaded?.first.orEmpty(), nextUp = loaded?.second.orEmpty(), loading = false))
            }
        }
    }

    /**
     * Takes a title off the shelf, on the server.
     *
     * The list belongs to Jellyfin, so the write goes there first and the screen follows. Removing
     * it locally first would look right until the next refresh put it straight back.
     */
    fun hideFromResume(id: String) {
        writeMediaState(id) { connection, media -> container.mediaSyncRepository.clearResume(connection, media) }
    }

    fun setMediaFavourite(id: String, favourite: Boolean) {
        writeMediaState(id, favourite = favourite, removeFromResume = false) { connection, media ->
            container.mediaSyncRepository.setFavourite(connection, media, favourite)
        }
    }

    /** Marking something watched also takes it off the resume shelf, the way finishing it would. */
    fun setMediaPlayed(id: String, played: Boolean) {
        writeMediaState(id, played = played, removeFromResume = played) { connection, media ->
            container.mediaSyncRepository.setPlayed(connection, media, played)
        }
    }

    fun clearMediaActionError() {
        if (_uiState.value.mediaActionError != null) _uiState.update { it.copy(mediaActionError = null) }
    }

    private fun writeMediaState(
        id: String,
        favourite: Boolean? = null,
        played: Boolean? = null,
        removeFromResume: Boolean = true,
        write: (ServiceConnection, LibraryMedia) -> Unit,
    ) {
        val state = _uiState.value
        val media = mediaActionTarget(state, id)
        if (media == null) {
            _uiState.update { it.copy(mediaActionError = container.appContext.getString(R.string.library_action_failed)) }
            return
        }
        val connection = state.connections.firstOrNull {
            it.kind == media.source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        if (connection == null || media.remoteId == null) {
            _uiState.update { it.copy(mediaActionError = container.appContext.getString(R.string.library_action_failed)) }
            return
        }
        _uiState.update { current ->
            current.copy(
                mediaActionError = null,
                contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(updating = true)
                    ?: current.contentDetails,
            )
        }
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { write(connection, media) } }
            if (result.isFailure) {
                _uiState.update { current ->
                    current.copy(
                        mediaActionError = container.appContext.getString(R.string.library_action_failed),
                        contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(updating = false)
                            ?: current.contentDetails,
                    )
                }
                return@launch
            }
            if (played != null) container.localPlaybackStore.forget(connection, media.remoteId)
            // Flags change everywhere the title appears; only the resume shelves lose the card.
            fun update(list: List<LibraryMedia>) = list.map { entry ->
                if (entry.id != id) entry
                else entry.copy(favourite = favourite ?: entry.favourite, played = played ?: entry.played)
            }
            _uiState.update { current ->
                val drop: (List<LibraryMedia>) -> List<LibraryMedia> =
                    if (removeFromResume) { list -> list.filterNot { it.id == id } } else ::update
                current.copy(
                    resume = drop(current.resume),
                    nextUp = update(current.nextUp),
                    recentMovies = update(current.recentMovies),
                    recentSeries = update(current.recentSeries),
                    favourites = update(current.favourites).filter { it.favourite },
                    librarySearchResults = update(current.librarySearchResults),
                    libraryShelves = current.libraryShelves.copy(
                        resume = drop(current.libraryShelves.resume),
                        nextUp = update(current.libraryShelves.nextUp),
                    ),
                    contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(
                        favourite = favourite ?: current.contentDetails.favourite,
                        played = played ?: current.contentDetails.played,
                        updating = false,
                    ) ?: current.contentDetails,
                )
            }
        }
    }

    fun filterLibrary(filters: app.reelstack.data.model.LibraryFilters) {
        _uiState.update { it.copy(libraryFilters = filters) }
        browseLibrary()
    }

    fun openLibraryEntry(id: String) {
        val entry = _uiState.value.libraryEntries.firstOrNull { it.id == id } ?: return
        // A series is a folder to the server and a *title* to a person. Its detail page carries the
        // poster, the synopsis, the cast and every episode with its own still and resume point,
        // which is more than a grid of season posters says after two more presses. Every other kind
        // of folder — collections, music, the libraries themselves — still opens as a folder.
        if (entry.isFolder && !entry.mediaType.equals("Series", ignoreCase = true)) {
            _uiState.update { it.copy(libraryPath = it.libraryPath + (entry.id to entry.title),
                libraryCollectionType = if (it.libraryPath.isEmpty()) entry.collectionType else it.libraryCollectionType,
                libraryFilters = app.reelstack.data.model.LibraryFilters()) }
            browseLibrary()
        } else {
            val source = _uiState.value.librarySource
            val media = LibraryMedia("${source.name.lowercase(java.util.Locale.ROOT)}-${entry.id}", entry.title, entry.subtitle, entry.progress,
                R.drawable.media_placeholder, source, entry.artworkUrl, entry.id,
                entry.overview, words(entry.facts), entry.genres, entry.mediaType)
            _uiState.update { it.copy(libraryDetailMedia = media) }
            openLibraryDetails(media.id)
        }
    }

    fun completeOnboarding() {
        container.preferencesRepository.onboardingCompleted = true
        _uiState.update { it.copy(showOnboarding = false, selectedTab = AppTab.HOME) }
        if (_uiState.value.configuredCount == 0) refreshLiveData()
    }

    fun cancelConnectionSetup() {
        connectionJob?.cancel()
        quickConnectJob?.cancel()
    }

    fun openCombinedSetup() {
        // First-run only: never silently replace an existing service account.
        if (!_uiState.value.showOnboarding || _uiState.value.configuredCount > 0) return
        openSheet(AppSheet.ConnectionEditor(ServiceKind.JELLYFIN))
        updateDraft { copy(simpleSetup = true, alsoConnect = true, authMode = ConnectionAuthMode.QUICK_CONNECT) }
    }

    fun importSetupLink(value: String) {
        if (!_uiState.value.showOnboarding || _uiState.value.configuredCount > 0 ||
            connectionDraft.value?.saving == true || connectionDraft.value?.quickConnectWaiting == true) return
        val setup = runCatching { app.reelstack.data.network.SetupLink.parse(value) }
        openCombinedSetup()
        setup.onSuccess { link ->
            updateDraft { copy(url = link.jellyfin, companionUrl = link.seerr, alsoConnect = link.seerr.isNotBlank(), setupImported = true) }
        }.onFailure {
            updateDraft { copy(error = appString(R.string.error_setup_link_invalid)) }
        }
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
                _uiState.update { it.copy(snackbar = appString(R.string.warn_request_in_flight)) }
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
                    if (EndpointValidator.isCleartext(it)) appString(R.string.warn_cleartext_http) else null
                },
            )
        }
        _uiState.update { it.copy(activeSheet = sheet, returnToCalendar = false) }
    }

    /**
     * Restores the title that opened the current detail page, without tearing down the sheet.
     *
     * A close animation is only correct when the reader is actually returning to the underlying
     * screen.  Using it for episode → series → episode briefly made the full-screen TV dialog
     * transparent, which looked like a jump through Home before the episode reappeared.
     */
    fun returnFromDetail(): Boolean {
        val previous = detailHistory.removeLastOrNull() ?: episodeReturn
        episodeReturn = null
        if (previous != null && _uiState.value.contentDetails?.key == previous.seriesKey &&
            _uiState.value.connections.any { it.kind == previous.connection.kind && it.identity == previous.connection.identity &&
                it.userId == previous.connection.userId && it.token == previous.connection.token }) {
            seasonsJob?.cancel()
            episodesJob?.cancel()
            _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(previous.details.key),
                contentDetails = previous.details, seriesBrowse = previous.browse) }
            return true
        }
        return false
    }

    fun closeSheet() {
        if (returnFromDetail()) return
        detailHistory.clear()
        if (_uiState.value.requestDraft?.sending == true) return
        requestDraftJob?.cancel()
        _uiState.update { it.copy(requestDraft = null) }
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        seasonsJob?.cancel()
        episodesJob?.cancel()
        _uiState.update { it.copy(activeSheet = null, contentDetails = null, returnToCalendar = false,
            seriesBrowse = app.reelstack.data.model.SeriesBrowse()) }
        connectionDraft.value = null
    }

    fun backToCalendar() {
        _uiState.update { it.copy(activeSheet = AppSheet.UpcomingCalendar, contentDetails = null, returnToCalendar = false) }
    }

    fun openAccountsSettings() {
        if (_uiState.value.isKidMode) return
        _uiState.update { it.copy(activeSheet = null, selectedTab = AppTab.SETTINGS,
            accountsSettingsRequest = it.accountsSettingsRequest + 1) }
    }

    fun openProfileSwitcher() {
        profileViewModel.loadProfiles()
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.ProfileSwitcher,
            )
        }
    }

    fun selectProfile(profile: app.reelstack.data.model.UserProfile) {
        val currentActive = container.connectionRepository.activeProfileId
        if (profile.id == currentActive) {
            closeSheet()
            return
        }

        profileViewModel.selectProfile(
            profile = profile,
            onSwitchSuccess = { switchProfileNow(it) },
            onPromptPin = { targetProfileId, isSetup ->
                _uiState.update {
                    it.copy(
                        activeSheet = AppSheet.PinPrompt(targetProfileId = targetProfileId, isSetup = isSetup),
                    )
                }
            },
        )
    }

    fun switchProfileNow(profileId: String) {
        val previousProfileId = container.connectionRepository.activeProfileId
        container.offlineDownloads.setProfileActive(previousProfileId, active = false)
        offlineRefreshJob?.cancel()
        kidsLibraryJob?.cancel()
        kidsBrowseJob?.cancel()
        kidsEpisodesJob?.cancel()
        homeFeed.resetForProfileChange()
        accountRefresh.cancel()
        discoverSearch.cancel()
        trackingJob?.cancel()
        container.connectionRepository.activeProfileId = profileId
        if (profileId.isBlank()) container.offlineDownloads.setProfileActive(profileId, active = true)
        val connections = container.connectionRepository.list(profileId)
        _uiState.update { current ->
            current.copy(
                activeProfileId = profileId,
                isKidMode = profileId.isNotBlank(),
                selectedTab = if (profileId.isNotBlank()) AppTab.HOME else current.selectedTab,
                globalSearchOpen = false,
                searchHistory = container.preferencesRepository.searchHistory(profileId),
                searchQuery = "",
                searchResults = emptyList(),
                librarySearchResults = emptyList(),
                isSearching = false,
                searchError = null,
                searchPage = 1,
                searchHasMore = false,
                loadingMoreSearch = false,
                connections = connections,
                activeSheet = null,
                pinError = null,
                pinLockoutSeconds = 0,
                // Clear media from memory so previous profile data is never visible
                resume = emptyList(),
                nextUp = emptyList(),
                recentMovies = emptyList(),
                recentSeries = emptyList(),
                favourites = emptyList(),
                sessions = emptyList(),
                incoming = emptyList(),
                discover = emptyList(),
                recommendations = emptyList(),
                kidsLibrary = emptyList(),
                kidsLibraries = emptyList(),
                kidsLibraryNames = emptyList(),
                kidsLibraryLoading = false,
                kidsLibraryError = false,
                kidsBrowse = KidsBrowse(),
                accounts = emptyMap(),
                accountErrors = emptyMap(),
                serviceWarnings = emptyMap(),
                failedServices = emptySet(),
                offlineDownloads = app.reelstack.offline.OfflineDownloadsSnapshot(),
                isRefreshing = false,
            )
        }
        hydrateCachedFeed()
        refreshLiveData(userInitiated = true)
    }

    fun submitPin(pin: String, targetProfileId: String, isSetup: Boolean) {
        val switchAfterSetup = (_uiState.value.activeSheet as? AppSheet.PinPrompt)?.switchAfterSetup ?: true
        profileViewModel.submitPin(
            pin = pin,
            targetProfileId = targetProfileId,
            isSetup = isSetup,
            switchAfterSetup = switchAfterSetup,
            onSwitchSuccess = { switchProfileNow(it) },
            onSetupComplete = { _uiState.update { it.copy(activeSheet = null, pinError = null) } },
        )
    }

    /** Opens the existing PIN sheet without changing profile; used by parental settings. */
    fun requestPinSetup(targetProfileId: String = _uiState.value.activeProfileId) {
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.PinPrompt(
                    targetProfileId = targetProfileId,
                    isSetup = true,
                    switchAfterSetup = false,
                ),
                pinError = null,
            )
        }
    }

    fun disablePinProtection() {
        profileViewModel.clearPinProtection()
        _uiState.update { it.copy(activeSheet = null, pinError = null, pinLockoutSeconds = 0) }
    }

    private fun primaryMediaServer(): ServiceConnection? =
        _uiState.value.connections.firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        } ?: _uiState.value.connections.firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank()
        } ?: container.connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        } ?: container.connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank()
        }

    fun recoverPinWithPassword(password: String, targetProfileId: String) {
        val primaryServer = container.connectionRepository.list("").firstOrNull {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.token.isNotBlank() && it.userId.isNotBlank()
        }
        if (primaryServer == null) {
            _uiState.update { it.copy(pinError = "Vaksenkontoen må vere tilkopla for å nullstille koden.") }
            return
        }
        val recoveringProfile = _uiState.value.activeProfileId
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val account = container.accountProfileClient.load(primaryServer)
                    val user = account.displayName
                    val authentication = when (primaryServer.kind) {
                        ServiceKind.JELLYFIN -> container.jellyfinAuthenticationClient.authenticate(primaryServer.baseUrl, user, password)
                        ServiceKind.EMBY -> container.embyAuthenticationClient.authenticate(primaryServer.baseUrl, user, password)
                        else -> error("Ikkje-støtta teneste")
                    }
                    authentication.userId == primaryServer.userId
                }.getOrDefault(false)
            }
            if (_uiState.value.activeProfileId != recoveringProfile || _uiState.value.activeSheet !is AppSheet.PinPrompt) return@launch
            if (success) {
                container.pinSecurity.clearPin()
                profileViewModel.clearPinProtection()
                switchProfileNow(targetProfileId)
            } else {
                _uiState.update { it.copy(pinError = appString(R.string.err_feil_brukarnamn_eller_passord)) }
            }
        }
    }

    private var kidsBrowseJob: Job? = null
    private var kidsLibraryJob: Job? = null
    private var kidsEpisodesJob: Job? = null

    /**
     * Loads the whole of the kid's own library.
     *
     * Barnemodus does not filter: whatever the account can reach on the server, the shell shows.
     * Which libraries that is was decided in Emby or Jellyfin, not here.
     */
    fun loadKidsLibrary() {
        if (kidsLibraryJob?.isActive == true) return
        val profileId = _uiState.value.activeProfileId
        if (!_uiState.value.isKidMode) return
        val servers = _uiState.value.connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        }
        if (servers.isEmpty()) {
            _uiState.update { it.copy(kidsLibraryLoading = false, kidsLibraryError = true) }
            return
        }
        _uiState.update { it.copy(kidsLibraryLoading = true, kidsLibraryError = false) }
        kidsLibraryJob = viewModelScope.launch {
            var failed = false
            val all = withContext(Dispatchers.IO) {
                servers.flatMap { server ->
                    runCatching { container.mediaSyncRepository.accountLibrary(server) }
                        .onFailure { failed = true }.getOrDefault(emptyList())
                }
            }
            val libraries = withContext(Dispatchers.IO) {
                servers.flatMap { server ->
                    runCatching {
                        container.mediaServerClient.browseLibraries(server)
                    }.onFailure { failed = true }.getOrDefault(emptyList())
                }.filter { view ->
                    val type = view.collectionType?.lowercase(java.util.Locale.ROOT)
                    type in setOf("movies", "tvshows") || type.isNullOrBlank()
                }.distinctBy { it.id }
            }
            if (!isActive || _uiState.value.activeProfileId != profileId) return@launch
            _uiState.update {
                it.copy(
                    kidsLibraryLoading = false,
                    kidsLibraryError = failed,
                    kidsLibrary = all
                        .filter { media -> !media.remoteId.isNullOrBlank() }
                        .distinctBy { media -> media.source to media.remoteId }
                        .sortedBy { media -> media.title.lowercase() },
                    kidsLibraries = libraries,
                    kidsLibraryNames = libraries.map { it.id to it.name }
                )
            }
        }
    }

    /**
     * Opens a series for a kid: seasons first, then the episodes of the season they are actually in.
     *
     * A movie never reaches this — it plays on the tap, which is the point of the kid shell.
     */
    fun openKidsSeries(media: LibraryMedia) {
        val seriesId = media.remoteId?.takeIf { it.isNotBlank() } ?: return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == media.source && it.token.isNotBlank()
        } ?: return

        kidsBrowseJob?.cancel()
        _uiState.update {
            it.copy(kidsBrowse = KidsBrowse(
                seriesId = seriesId, title = media.title, source = media.source, loading = true,
            ))
        }

        kidsBrowseJob = viewModelScope.launch {
            val seasons = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.seasons(connection, seriesId) }
            }.getOrDefault(emptyList())
                // Specials are season zero and almost never where a child wants to start.
                .sortedWith(compareBy({ (it.episode ?: 0) == 0 }, { it.episode ?: Int.MAX_VALUE }))
            if (!isActive || _uiState.value.kidsBrowse.seriesId != seriesId) return@launch

            val nextUp = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.seriesNextUp(connection, seriesId) }
            }.getOrNull()
            val opening = nextUp?.let { next -> seasons.firstOrNull { it.episode == next.season } }
                ?: seasons.firstOrNull()

            _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId) it
                else it.copy(kidsBrowse = it.kidsBrowse.copy(seasons = seasons, loading = opening != null))
            }
            opening?.remoteId?.let { selectKidsSeason(it) } ?: _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId) it else it.copy(kidsBrowse = it.kidsBrowse.copy(loading = false))
            }
        }
    }

    fun selectKidsSeason(seasonId: String) {
        val browse = _uiState.value.kidsBrowse
        if (!browse.open) return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == browse.source && it.token.isNotBlank()
        } ?: return
        val seriesId = browse.seriesId
        val profileId = _uiState.value.activeProfileId
        kidsEpisodesJob?.cancel()
        _uiState.update { it.copy(kidsBrowse = it.kidsBrowse.copy(selectedSeasonId = seasonId, episodes = emptyList(), loading = true)) }
        kidsEpisodesJob = viewModelScope.launch {
            val episodes = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.episodes(connection, seriesId, seasonId) }
            }.getOrDefault(emptyList())
            if (!isActive || _uiState.value.activeProfileId != profileId) return@launch
            _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId || it.kidsBrowse.selectedSeasonId != seasonId) it
                else it.copy(kidsBrowse = it.kidsBrowse.copy(episodes = episodes, loading = false))
            }
        }
    }

    fun closeKidsSeries() {
        kidsBrowseJob?.cancel()
        kidsEpisodesJob?.cancel()
        _uiState.update { it.copy(kidsBrowse = KidsBrowse()) }
    }

    fun openAddProfile() {
        val mediaServers = (_uiState.value.connections +
                container.connectionRepository.list(""))
            .filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() }
            .distinctBy { it.baseUrl.trimEnd('/') }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.AddProfile,
                loadingPublicUsers = mediaServers.isNotEmpty(),
                publicUsers = emptyList(),
                addProfileError = null,
                addProfileHasServer = mediaServers.isNotEmpty(),
                addProfileServers = mediaServers,
            )
        }
        if (mediaServers.isEmpty()) return
        viewModelScope.launch {
            val allUsers = mutableListOf<app.reelstack.data.network.PublicUser>()
            for (server in mediaServers) {
                val users = withContext(Dispatchers.IO) {
                    runCatching {
                        when (server.kind) {
                            ServiceKind.JELLYFIN -> container.jellyfinAuthenticationClient.publicUsers(server.baseUrl)
                            ServiceKind.EMBY -> container.embyAuthenticationClient.publicUsers(server.baseUrl)
                            else -> emptyList()
                        }.map { it.copy(serverKind = server.kind, serverUrl = server.baseUrl) }
                    }.getOrElse { emptyList() }
                }
                allUsers.addAll(users)
            }
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    publicUsers = allUsers,
                )
            }
        }
    }

    fun addKidProfile(user: app.reelstack.data.network.PublicUser, password: String) {
        addKidProfileInternal(
            username = user.name,
            password = password,
            userId = user.id,
            avatarUrl = user.avatarUrl,
            serverKind = user.serverKind,
            serverUrl = user.serverUrl,
        )
    }

    fun addKidProfileManual(username: String, password: String, serverUrl: String? = null) {
        val server = serverUrl?.let { url ->
            _uiState.value.addProfileServers.firstOrNull { it.baseUrl.trimEnd('/') == url.trimEnd('/') }
        }
        addKidProfileInternal(
            username = username.trim(),
            password = password,
            userId = null,
            avatarUrl = null,
            serverKind = server?.kind,
            serverUrl = server?.baseUrl,
        )
    }

    private fun addKidProfileInternal(
        username: String,
        password: String,
        userId: String?,
        avatarUrl: String?,
        serverKind: ServiceKind? = null,
        serverUrl: String? = null,
    ) {
        val resolvedKind = serverKind ?: primaryMediaServer()?.kind
        val resolvedUrl = serverUrl ?: primaryMediaServer()?.baseUrl.orEmpty()
        val serverConnection = if (resolvedUrl.isNotBlank()) {
            (_uiState.value.connections + container.connectionRepository.list(""))
                .firstOrNull { it.baseUrl.trimEnd('/') == resolvedUrl.trimEnd('/') }
        } else null

        if (resolvedKind == null || resolvedUrl.isBlank() || serverConnection == null) {
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    addProfileError = appString(R.string.err_fekk_ikkje_kontakt_med_2),
                )
            }
            return
        }
        if (username.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingPublicUsers = true, addProfileError = null) }
            val authResult = withContext(Dispatchers.IO) {
                runCatching {
                    when (resolvedKind) {
                        ServiceKind.JELLYFIN -> container.jellyfinAuthenticationClient.authenticate(resolvedUrl, username, password)
                        ServiceKind.EMBY -> container.embyAuthenticationClient.authenticate(resolvedUrl, username, password)
                        else -> error("Ikkje-støtta teneste")
                    }
                }
            }
            val auth = authResult.getOrNull()
            if (auth == null) {
                val ex = authResult.exceptionOrNull()
                val readable = ex?.readableMessage(container.appContext)
                _uiState.update {
                    it.copy(
                        loadingPublicUsers = false,
                        addProfileError = readable ?: appString(R.string.err_feil_brukarnamn_eller_passord),
                    )
                }
                return@launch
            }

            val finalUserId = userId ?: auth.userId
            val finalAvatarUrl = avatarUrl ?: "${resolvedUrl.trimEnd('/')}/Users/$finalUserId/Images/Primary"
            val kidConnection = serverConnection.copy(
                token = auth.accessToken,
                userId = auth.userId,
                name = username,
            )
            container.connectionRepository.save(kidConnection, finalUserId)
            container.connectionRepository.registerKidProfile(finalUserId, username, finalAvatarUrl)

            val newProfiles = container.connectionRepository.listProfiles()
            _uiState.update {
                it.copy(
                    loadingPublicUsers = false,
                    profiles = newProfiles,
                    allProfileConnections = newProfiles.associate { p -> p.id to container.connectionRepository.list(p.id) },
                    activeSheet = null,
                )
            }
            val targetProfile = newProfiles.firstOrNull { it.id == finalUserId }
            if (targetProfile != null) {
                selectProfile(targetProfile)
            } else {
                switchProfileNow(finalUserId)
            }
        }
    }

    fun deleteKidProfile(profile: app.reelstack.data.model.UserProfile) {
        container.connectionRepository.list(profile.id).filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) }
            .forEach { container.offlineDownloads.removeScope(profile.id, it) }
        container.offlineStorage.clearForProfile(profile.id)
        profileViewModel.deleteKidProfile(profile)
    }

    suspend fun personTitles(person: app.reelstack.data.model.CastMember, source: ServiceKind): List<LibraryMedia> {
        val connection = _uiState.value.connections.firstOrNull { it.kind == source && it.token.isNotBlank() }
            ?: return emptyList()
        val id = person.remoteId ?: return emptyList()
        return withContext(Dispatchers.IO) { container.mediaSyncRepository.personTitles(connection, id) }
    }

    fun openPersonTitle(media: LibraryMedia) {
        _uiState.update { it.copy(libraryDetailMedia = media) }
        openLibraryDetails(media.id)
    }

    fun openEpisodeDetail(media: LibraryMedia) {
        val state = _uiState.value
        val details = state.contentDetails
        val connection = state.connections.firstOrNull { it.kind == media.source && it.token.isNotBlank() }
        _uiState.update { it.copy(libraryDetailMedia = media) }
        openLibraryDetails(media.id, clearHistory = false)
        if (details != null && connection != null) {
            val ret = EpisodeReturn(media.id, connection, details, state.seriesBrowse)
            episodeReturn = ret
            detailHistory.addLast(ret)
        }
    }

    fun openEpisodeSeries() {
        val state = _uiState.value
        val details = state.contentDetails?.takeIf { it.mediaType.equals("Episode", true) } ?: return
        val browse = state.seriesBrowse.takeIf { it.openedFor == details.key && it.seriesId.isNotBlank() } ?: return
        val connection = state.connections.firstOrNull { it.kind == details.source && it.token.isNotBlank() } ?: return
        val candidates = state.recentSeries + state.favourites + state.libraryPeeks.values.flatten()
        val series = candidates.firstOrNull { it.source == details.source && it.remoteId == browse.seriesId && it.mediaType == "Series" }
            ?: LibraryMedia(id = "${connection.kind.name.lowercase()}-${browse.seriesId}", title = details.title,
                subtitle = "", artworkRes = R.drawable.media_placeholder, source = connection.kind,
                remoteId = browse.seriesId, mediaType = "Series")
        val selectedSeason = browse.seasons.firstOrNull { it.remoteId == browse.selectedSeasonId }?.episode ?: details.season
        _uiState.update { it.copy(libraryDetailMedia = series.copy(season = selectedSeason)) }
        openLibraryDetails(series.id, clearHistory = false)
        val ret = EpisodeReturn(series.id, connection, details, browse)
        episodeReturn = ret
        detailHistory.addLast(ret)
    }

    fun openLibraryDetails(id: String, clearHistory: Boolean = true) {
        if (clearHistory) {
            episodeReturn = null
            detailHistory.clear()
        }
        val state = _uiState.value
        val media = (listOfNotNull(state.libraryDetailMedia) + state.resume + state.nextUp + state.favourites + state.recentMovies + state.recentSeries + state.librarySearchResults + state.libraryPeeks.values.flatten() + state.libraryShelves.resume + state.libraryShelves.nextUp)
            .firstOrNull { it.id == id } ?: return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == media.source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.TitleDetails(media.id),
                contentDetails = ContentDetails(
                    key = media.id,
                    remoteId = media.remoteId,
                    title = media.title,
                    eyebrow = "Bibliotek i ${media.source.displayName}",
                    subtitle = media.subtitle,
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    logoUrl = media.logoUrl,
                    backdropUrl = media.heroUrl,
                    source = media.source,
                    mediaType = media.mediaType,
                    loading = connection != null && media.remoteId != null,
                    statusTitle = appString(R.string.details_in_library_badge),
                    libraryAvailable = true,
                    progress = media.progress,
                    season = media.season,
                    episode = media.episode,
                    favourite = media.favourite,
                    played = media.played,
                    statusDescription = appString(R.string.details_in_library_source, media.source.displayName),
                ),
            )
        }
        if (connection == null || media.remoteId == null) return
        loadSeasons(connection, media)
        viewModelScope.launch {
            val result = attempt {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.details(connection, media) }
            }
            result.getOrNull()?.let { remote ->
                if (_uiState.value.contentDetails?.key == media.id && media.seriesId == null && remote.seriesId != null) {
                    loadSeasons(connection, media.copy(seriesId = remote.seriesId, season = remote.season, episode = remote.episode))
                }
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
                                backdropUrl = remote.backdropUrl ?: details.backdropUrl,
                                logoUrl = remote.logoUrl ?: details.logoUrl,
                                trailerUrl = remote.trailerUrl,
                                season = remote.season ?: details.season,
                                episode = remote.episode ?: details.episode,
                                overview = remote.overview ?: details.overview,
                                facts = words(remote.facts),
                                criticRating = remote.criticRating,
                                tmdbRating = remote.tmdbRating ?: details.tmdbRating,
                                mdblistRating = remote.mdblistRating ?: details.mdblistRating,
                                genres = (remote.genres + details.genres).distinct(),
                                artworkUrl = remote.artworkUrl ?: details.artworkUrl,
                                progress = remote.progress ?: details.progress,
                                remainingMinutes = remote.remainingMinutes,
                                quality = remote.quality,
                                favourite = remote.favourite,
                                played = remote.played,
                                audioTracks = remote.audioTracks,
                                subtitleTracks = remote.subtitleTracks,
                                versions = remote.versions.map { (id, name) ->
                                    app.reelstack.data.model.MediaVersion(id, name)
                                },
                                loading = false,
                            ),
                        )
                    },
                    onFailure = {
                        current.copy(contentDetails = details.copy(loading = false, error = appString(R.string.error_content_details)))
                    },
                )
            }
        }
    }

    /**
     * A series' seasons, and then the first season's episodes.
     *
     * Only for a series: a film has nothing under it, and an episode's siblings belong to the
     * series page rather than to the episode. The first season is opened straight away, because a
     * list of season names with nothing under them answers nothing.
     */
    private fun loadSeasons(connection: ServiceConnection, media: LibraryMedia) {
        seasonsJob?.cancel()
        episodesJob?.cancel()
        // A series is browsed by its own id; an episode is browsed by its parent's, because the
        // useful thing on an episode page is the rest of the season it came from — without it the
        // page was a paragraph of synopsis and half a screen of black.
        val seriesId = when {
            media.mediaType.equals("Series", true) -> media.remoteId
            media.mediaType.equals("Episode", true) -> media.seriesId
            else -> null
        }
        if (seriesId.isNullOrBlank()) {
            if (_uiState.value.seriesBrowse != app.reelstack.data.model.SeriesBrowse()) {
                _uiState.update { it.copy(seriesBrowse = app.reelstack.data.model.SeriesBrowse()) }
            }
            return
        }
        _uiState.update { it.copy(seriesBrowse = app.reelstack.data.model.SeriesBrowse(
            seriesId = seriesId, openedFor = media.id, loading = true)) }
        seasonsJob = viewModelScope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.seasons(connection, seriesId) }
            }
            if (!isActive || _uiState.value.seriesBrowse.seriesId != seriesId) return@launch
            // A season carries its own number in IndexNumber, which the parser maps to `episode`;
            // `season` on a season item is the series' own index and is usually absent. Specials are
            // season zero and almost never where anyone wants to start, so they go last.
            val seasons = loaded.getOrDefault(emptyList())
                .sortedWith(compareBy({ (it.episode ?: 0) == 0 }, { it.episode ?: Int.MAX_VALUE }))
            _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId) it
                else it.copy(seriesBrowse = it.seriesBrowse.copy(
                    seasons = seasons, loading = false,
                    error = if (loaded.isFailure) container.appContext.getString(R.string.detail_seasons_failed) else null,
                ))
            }
            // The season the reader is actually in the middle of, when the server knows one.
            val nextUp = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.seriesNextUp(connection, seriesId) }
            }.getOrNull()
            if (isActive && nextUp != null) _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId) it else it.copy(seriesBrowse = it.seriesBrowse.copy(nextUp = nextUp))
            }
            // An episode page opens on its own season, whatever the server thinks comes next in the
            // series: the reader is looking at episode eight of season seven, not at the series.
            val opening = media.season
                ?.let { own -> seasons.firstOrNull { it.episode == own } }
                ?: nextUp?.let { next -> seasons.firstOrNull { it.episode == next.season } }
                ?: seasons.firstOrNull()
            opening?.remoteId?.let { selectSeason(it) }
        }
    }

    /** Opens one season. The seasons themselves stay put; only the episode list changes. */
    fun selectSeason(seasonId: String) {
        val state = _uiState.value
        val browse = state.seriesBrowse
        if (browse.seriesId.isBlank() || browse.selectedSeasonId == seasonId && browse.episodes.isNotEmpty()) return
        val connection = state.connections.firstOrNull {
            it.kind == state.contentDetails?.source && it.token.isNotBlank()
        } ?: return
        val seriesId = browse.seriesId
        episodesJob?.cancel()
        _uiState.update { it.copy(seriesBrowse = it.seriesBrowse.copy(selectedSeasonId = seasonId, episodes = emptyList(), loading = true, upcomingError = null)) }
        episodesJob = viewModelScope.launch {
            val loaded = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.episodes(connection, seriesId, seasonId) }
            }
            if (!isActive) return@launch
            _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId || it.seriesBrowse.selectedSeasonId != seasonId) it
                else it.copy(seriesBrowse = it.seriesBrowse.copy(
                    episodes = app.reelstack.data.model.orderedSeasonEpisodes(loaded.getOrDefault(emptyList()),
                        browse.seasons.firstOrNull { season -> season.remoteId == seasonId }?.episode), loading = false,
                    error = if (loaded.isFailure) container.appContext.getString(R.string.detail_episodes_failed) else null,
                ))
            }
            // Publish playable episodes first. Optional catalogue dates never block this list.
            val seerr = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
            val number = browse.seasons.firstOrNull { it.remoteId == seasonId }?.episode
            if (loaded.isSuccess && seerr != null && number != null && container.preferencesRepository.personalization.showUpcomingEpisodes) {
                val announced = runCatching { withContext(Dispatchers.IO) {
                    container.mediaSyncRepository.upcomingSeason(connection, seerr, seriesId, number)
                } }
                if (!isActive) return@launch
                _uiState.update { current ->
                    if (current.seriesBrowse.seriesId != seriesId || current.seriesBrowse.selectedSeasonId != seasonId ||
                        current.connections.none { it.kind == seerr.kind && it.baseUrl == seerr.baseUrl && it.token == seerr.token && it.userId == seerr.userId } ||
                        current.connections.none { it.kind == connection.kind && it.baseUrl == connection.baseUrl && it.token == connection.token && it.userId == connection.userId }) current
                    else current.copy(seriesBrowse = current.seriesBrowse.copy(
                        episodes = app.reelstack.data.network.mergeUpcomingEpisodes(current.seriesBrowse.episodes, announced.getOrDefault(emptyList())),
                        upcomingError = if (announced.isFailure) appString(R.string.design_upcoming_failed) else null))
                }
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
                    eyebrow = appString(if (media.inLibrary) R.string.details_eyebrow_in_library else R.string.details_eyebrow_discover),
                    // The kind, then the year: the detail sheet has no type badge to carry it.
                    subtitle = listOfNotNull(appString(mediaKindRes(media.mediaType)),
                        media.metadata.takeIf { it.isNotBlank() }).joinToString(" · "),
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    source = ServiceKind.SEERR,
                    mediaType = resolvedMediaType(media.mediaType, media.metadata),
                    statusTitle = appString(seerrStatusLabel(media.seerrStatus, media.inLibrary, media.requested)),
                    libraryAvailable = media.inLibrary || media.seerrStatus == 5,
                    statusDescription = appString(seerrStatusDescription(media.seerrStatus, media.inLibrary)),
                    loading = connection != null && media.remoteId != null && media.mediaType != null,
                ),
            )
        }
        if (connection == null || media.remoteId == null || media.mediaType == null) return
        viewModelScope.launch {
            val result = attempt {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.details(connection, media) }
            }
            _uiState.update { current ->
                val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                result.fold(
                    onSuccess = { remote ->
                        current.copy(
                            contentDetails = details.copy(
                                title = remote.title ?: details.title,
                                statusTitle = remote.seerrStatus?.let { appString(seerrStatusLabel(it)) } ?: details.statusTitle,
                                libraryAvailable = remote.seerrStatus == 5,
                                statusDescription = remote.seerrStatus?.let { appString(seerrStatusDescription(it)) } ?: details.statusDescription,
                                tagline = remote.tagline ?: details.tagline,
                                cast = remote.cast,
                                overview = remote.overview ?: details.overview,
                                facts = (words(remote.facts) + details.facts).distinct(),
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
                        current.copy(contentDetails = details.copy(loading = false, error = appString(R.string.error_content_details)))
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
                eyebrow = if (recent == null) appString(R.string.details_coming_soon_source, media.source.displayName)
                    else appString(R.string.details_recent_available_source, media.source.displayName),
                subtitle = media.subtitle,
                overview = media.overview ?: appString(R.string.details_overview_unavailable, media.source.displayName),
                facts = (media.facts + media.dateLabel + media.source.displayName).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = media.mediaType,
                statusTitle = if (recent == null) appString(R.string.details_planned_release)
                    else if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) appString(R.string.details_in_library)
                    else appString(R.string.details_home_release_title),
                statusDescription = if (recent == null) {
                    appString(R.string.details_expected_date_note, media.dateLabel)
                } else {
                    if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
                        appString(R.string.details_library_release_date_note, media.source.displayName)
                    else appString(R.string.details_external_release_date_note, media.dateLabel, media.source.displayName)
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
                overview = media.overview ?: appString(R.string.details_tracked_fallback),
                facts = (media.facts + media.source.displayName + media.status).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = if (media.source == ServiceKind.RADARR) "Movie" else "Episode",
                statusTitle = media.status,
                statusDescription = appString(R.string.queue_last_reported_state, media.source.displayName),
            ),
        )
    }

    fun openActivityDetails(id: String) {
        (_uiState.value.trackedRequests + _uiState.value.requestHistory.items).firstOrNull { it.key == id }?.let { tracked ->
            if (tracked.mediaId <= 0 || tracked.mediaType !in setOf("movie", "tv")) return
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
                subtitle = event.detail.text(container.appContext),
                overview = appString(R.string.activity_registered, event.time.text(container.appContext).lowercase()),
                facts = listOfNotNull(event.source?.displayName, event.progress?.let { "$it %" },
                    event.time.text(container.appContext)),
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
            val result = attempt {
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
                        if (targetPaused) appString(R.string.notice_playback_paused) else appString(R.string.notice_playback_resumed)
                    } else {
                        appString(R.string.error_playback_control)
                    },
                )
            }
        }
    }

    fun setSearchQuery(value: String) = discoverSearch.setQuery(value)

    fun loadMoreSearchResults() = discoverSearch.loadMore()

    fun requestMedia(id: String) {
        val state = _uiState.value
        val media = (state.discover + state.searchResults + state.recommendations).firstOrNull { it.id == id } ?: return
        if (!media.canRequest || state.requestingMediaIds.isNotEmpty()) return
        if (media.mediaType != "tv" && state.configuredCount > 0 && state.accounts[ServiceKind.SEERR]?.isPersonal == true &&
            state.accounts[ServiceKind.SEERR]?.canRequestType(media.mediaType ?: "movie") != true) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
        if (state.configuredCount > 0 && (connection?.sessionCookie != true || state.accounts[ServiceKind.SEERR]?.isPersonal != true)) {
            openSeerrAccount()
            return
        }
        requestDraftJob?.cancel()
        _uiState.update { it.copy(activeSheet = AppSheet.RequestComposer, requestDraft = RequestDraft(media), returnToCalendar = false) }
        if (connection == null && state.configuredCount == 0) {
            val seasons = if (media.mediaType == "tv") listOf(RequestSeason(1, app.reelstack.localization.LocalizedText(R.string.media_season_number, 1), 8, 1)) else emptyList()
            _uiState.update { it.copy(requestDraft = RequestDraft(media, seasons, loading = false)) }
            return
        }
        requestDraftJob = viewModelScope.launch {
            val result = attempt { withContext(Dispatchers.IO) {
                val rules = async {
                    attempt { container.requestRulesClient.load(requireNotNull(connection),
                        requireNotNull(state.accounts[ServiceKind.SEERR]).id, media.mediaType ?: "movie") }.getOrNull()
                }
                val remote = container.mediaSyncRepository.details(requireNotNull(connection), media)
                val watched = state.accounts[ServiceKind.SEERR]?.let { actor ->
                    media.remoteId?.let { remoteId -> container.requestTrackingRepository.watchedSeasons(connection, actor.id, remoteId) }
                }.orEmpty()
                Triple(remote, watched, rules.await())
            } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val draft = current.requestDraft?.takeIf { it.media.id == id } ?: return@update current
                if (current.activeSheet != AppSheet.RequestComposer) return@update current
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection?.token || configured?.baseUrl != connection?.baseUrl) return@update current
                current.copy(requestDraft = result.fold(onSuccess = { (remote, watched, rules) ->
                    // No implicit "all seasons", including future or undated returning seasons.
                    draft.copy(loading = false, seasons = remote.seasons, selected = emptySet(),
                        mediaStatus = remote.seerrStatus, nextEpisode = remote.nextEpisode,
                        watchedSeasons = watched, rules = rules,
                        error = when {
                            remote.seerrStatus == 6 -> appString(R.string.flow_status_blocked_admin)
                            media.mediaType == "tv" && remote.seasons.isEmpty() -> appString(R.string.flow_status_no_seasons)
                            media.mediaType != "tv" && remote.seerrStatus in 2..5 -> appString(R.string.flow_status_movie_already)
                            else -> null
                        })
                }, onFailure = { draft.copy(loading = false, error = appString(R.string.error_request_seasons)) }))
            }
        }
    }

    fun setRequestSeason(number: Int, checked: Boolean) = _uiState.update { state ->
        val draft = state.requestDraft ?: return@update state
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.mediaStatus == 6 || draft.error != null ||
            draft.seasons.none { it.number == number && it.canRequest } ||
            (state.configuredCount > 0 && state.accounts[ServiceKind.SEERR]?.canRequestType("tv") != true)) return@update state
        state.copy(requestDraft = draft.copy(selected = if (checked) draft.selected + number else draft.selected - number))
    }

    fun setRequestNotification(enabled: Boolean) = _uiState.update { state ->
        state.copy(requestDraft = state.requestDraft?.takeUnless { it.sending }?.copy(notify = enabled) ?: state.requestDraft)
    }

    fun confirmRequest() {
        val draft = _uiState.value.requestDraft ?: return
        if (draft.quotaExceeded || draft.rules?.canRequest == false) return
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.error != null || (draft.media.mediaType == "tv" && draft.selected.isEmpty())) return
        sendRequest(draft.media.id)
    }

    fun setSeasonWatch(number: Int, enabled: Boolean) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.error != null || draft.mediaStatus == 6 ||
            draft.seasons.none { it.number == number && it.canWatch }) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() }
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal }
        if (connection == null || actor == null) return
        _uiState.update { it.copy(requestDraft = draft.copy(savingWatch = number, watchError = null)) }
        viewModelScope.launch {
            val result = attempt { withContext(Dispatchers.IO) {
                container.requestTrackingRepository.setSeasonWatch(connection, actor.id, draft.media, number, enabled)
                container.requestTrackingRepository.list(container.requestTrackingRepository.scope(connection, actor.id))
            } }
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl || !configured.sessionCookie ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id) return@update current
                val openDraft = current.requestDraft?.takeIf { it.media.id == draft.media.id }
                current.copy(
                    trackedRequests = result.getOrNull() ?: current.trackedRequests,
                    requestDraft = openDraft?.copy(savingWatch = null,
                        watchedSeasons = if (result.isSuccess) {
                            if (enabled) openDraft.watchedSeasons + number else openDraft.watchedSeasons - number
                        } else openDraft.watchedSeasons,
                        watchError = if (result.isFailure) appString(R.string.error_save_watch) else null,
                    ) ?: current.requestDraft,
                    snackbar = if (result.isSuccess) {
                        if (enabled) appString(R.string.flow_notify_season_on, number) else appString(R.string.flow_notify_season_off, number)
                    } else current.snackbar,
                )
            }
        }
    }

    fun setFollowNotification(key: String, enabled: Boolean) {
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR } ?: return
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal } ?: return
        // Move the switch now, store it after. This used to run on the caller's thread, which is
        // the UI one: three parses of the follow list — up to a hundred entries — and a blocking
        // commit() for a single tap on a bell.
        _uiState.update { current ->
            current.copy(
                trackedRequests = current.trackedRequests.map {
                    if (it.key == key) it.copy(notify = enabled) else it
                },
            )
        }
        viewModelScope.launch {
            val stored = attempt {
                withContext(Dispatchers.IO) {
                    val scope = container.requestTrackingRepository.scope(connection, actor.id)
                    // Read back what is on disk rather than what we asked for, so a write that
                    // did not happen puts the switch back instead of leaving a lie on screen.
                    runCatching { container.requestTrackingRepository.setNotify(scope, key, enabled) }
                    container.requestTrackingRepository.list(scope)
                }
            }.getOrNull()
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id
                ) {
                    return@update current
                }
                current.copy(trackedRequests = stored ?: current.trackedRequests)
            }
        }
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
            val result = attempt {
                withContext(Dispatchers.IO) {
                    container.requestTrackingRepository.cancel(connection, actor.id, key)
                    container.requestTrackingRepository.list(
                        container.requestTrackingRepository.scope(connection, actor.id),
                    )
                }
            }
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id) {
                    return@update current.copy(cancellingRequestKeys = current.cancellingRequestKeys - key)
                }
                current.copy(
                    cancellingRequestKeys = current.cancellingRequestKeys - key,
                    trackedRequests = result.getOrNull() ?: current.trackedRequests,
                    snackbar = if (result.isSuccess) {
                        if (state.trackedRequests.firstOrNull { it.key == key }?.availabilityOnly == true) appString(R.string.flow_unfollowed_season)
                        else appString(R.string.flow_withdrawn_request)
                    }
                    else result.exceptionOrNull()?.readableMessage(container.appContext)
                        ?: appString(R.string.error_withdraw_request),
                )
            }
        }
    }

    fun openRequestHistory() {
        _uiState.update { it.copy(showRequestHistory = true) }
        if (!_uiState.value.requestHistory.loaded) loadRequestHistory(false)
    }

    fun openLibraryShortcut(id: String) {
        val shortcut = _uiState.value.libraryShortcuts.firstOrNull { it.first == id } ?: return
        _uiState.update { it.copy(selectedTab = AppTab.LIBRARY, activeSheet = null, libraryPath = listOf(shortcut), libraryCollectionType = null,
            libraryFilters = app.reelstack.data.model.LibraryFilters(), libraryFacets = app.reelstack.data.model.LibraryFacets()) }
        browseLibrary()
    }

    fun closeRequestHistory() {
        historyJob?.cancel()
        _uiState.update { it.copy(showRequestHistory = false, requestHistory = it.requestHistory.copy(loading = false)) }
    }

    fun loadRequestHistory(more: Boolean) {
        val state = _uiState.value
        if (state.requestHistory.loading || (more && !state.requestHistory.hasMore)) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() }
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal }
        if (connection == null || actor == null) {
            _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                error = container.appContext.getString(R.string.history_sign_in))) }
            return
        }
        historyJob?.cancel()
        val starting = if (more) state.requestHistory else app.reelstack.data.model.RequestHistoryState()
        _uiState.update { it.copy(requestHistory = state.requestHistory.copy(loading = true, error = null)) }
        historyJob = viewModelScope.launch {
            try {
                val page = withContext(Dispatchers.IO) {
                    container.requestHistoryRepository.load(connection, actor.id, starting.nextOffset) { ensureActive() }
                }
                ensureActive()
                val current = _uiState.value
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.identity != connection.identity ||
                    !configured.sessionCookie || current.accounts[ServiceKind.SEERR]?.id != actor.id) {
                    _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                        error = container.appContext.getString(R.string.history_sign_in))) }
                    return@launch
                }
                val updated = starting.append(page.items, page.nextOffset, page.hasMore, page.total)
                _uiState.update { it.copy(requestHistory = updated) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: app.reelstack.data.repository.HistoryIdentityChangedException) {
                _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                    error = container.appContext.getString(R.string.history_sign_in))) }
            } catch (_: Exception) {
                _uiState.update { it.copy(requestHistory = state.requestHistory.copy(loading = false, retryFromStart = !more,
                    error = container.appContext.getString(R.string.history_failed))) }
            }
        }
    }

    /**
     * [background] is a poll nobody asked for, so it shows no spinner and, when the answer has not
     * changed, changes no state at all. A press of the refresh button is the opposite: it must show
     * that something is happening even when the result turns out identical.
     */
    fun refreshTrackedRequests(background: Boolean = false) {
        if (trackingJob?.isActive == true) return
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() } ?: run {
            _uiState.update {
                if (it.trackedRequests.isEmpty() && it.trackingError == null) it
                else it.copy(trackedRequests = emptyList(), trackingError = null)
            }
            return
        }
        if (!background) _uiState.update { it.copy(trackingLoading = true) }
        trackingJob = viewModelScope.launch {
            val result = attempt { withContext(Dispatchers.IO) { container.requestTrackingRepository.refresh(connection) } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl || configured.sessionCookie != connection.sessionCookie) current.copy(trackingLoading = false)
                else {
                    val requests = result.getOrNull()?.second ?: current.trackedRequests
                    val error = if (result.isFailure) appString(R.string.error_request_history) else null
                    // Same reasoning as the playback poll: an unchanged answer must not produce a
                    // new state object, or every screen rebuilds to show what it already showed.
                    if (!current.trackingLoading && current.trackedRequests == requests && current.trackingError == error) current
                    else current.copy(trackingLoading = false, trackedRequests = requests, trackingError = error)
                }
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
            _uiState.update { it.copy(snackbar = appString(R.string.error_send_connect_seerr)) }
            return
        }

        if (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null) {
            _uiState.update {
                it.copy(
                    discover = it.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    recommendations = it.recommendations.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    searchResults = it.searchResults.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    contentDetails = it.contentDetails?.let { details ->
                        if (details.key == id) details.copy(statusTitle = appString(R.string.flow_local_status_title), statusDescription = appString(R.string.flow_local_status_desc)) else details
                    },
                    snackbar = appString(R.string.flow_local_snackbar),
                    activeSheet = null,
                    requestDraft = null,
                )
            }
            return
        }

        val account = state.accounts[ServiceKind.SEERR]
        if (!seerr.sessionCookie || account?.isPersonal != true) {
            openSeerrAccount()
            _uiState.update { it.copy(snackbar = appString(R.string.flow_login_as_yourself_snackbar)) }
            return
        }
        _uiState.update { it.copy(requestingMediaIds = it.requestingMediaIds + id, requestDraft = draft.copy(sending = true)) }
        viewModelScope.launch {
            val result = attempt {
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
                            if (details.key == id) details.copy(statusTitle = appString(R.string.flow_sent_as_title, account.displayName), statusDescription = appString(R.string.flow_sent_as_desc)) else details
                        },
                        activity = listOf(
                            ActivityEvent(
                                id = "seerr-request-${media.id}",
                                title = media.title,
                                detail = app.reelstack.localization.LocalizedText(
                                    R.string.flow_sent_as_title, account.displayName),
                                time = app.reelstack.localization.LocalizedText(R.string.flow_just_now),
                                timeEpochMillis = System.currentTimeMillis(),
                                source = ServiceKind.SEERR,
                                artworkRes = media.artworkRes,
                                artworkUrl = media.artworkUrl,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = appString(R.string.flow_sent_to_seerr_as, account.displayName),
                        activeSheet = null,
                        requestDraft = null,
                        selectedTab = AppTab.ACTIVITY,
                    )
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = appString(R.string.error_send_as_person, account.displayName),
                        requestDraft = draft.copy(sending = false, error = appString(R.string.error_send_failed_retry)),
                    )
                }
            }
            if (result.isSuccess) refreshTrackedRequests()
        }
    }

    fun openSessionChannel() = homeFeed.openSessionChannel()

    fun closeSessionChannel() = homeFeed.closeSessionChannel()

    suspend fun refreshPlayback() = homeFeed.refreshPlayback()

    private fun localResume(items: List<LibraryMedia>, connections: List<ServiceConnection>): List<LibraryMedia> =
        homeFeed.localResume(items, connections)

    private fun localNextUp(items: List<LibraryMedia>, connections: List<ServiceConnection>): List<LibraryMedia> =
        homeFeed.localNextUp(items, connections)

    fun returnedToApp() {
        homeFeed.returnedToApp()
        if (_uiState.value.selectedTab == AppTab.DOWNLOADS) refreshOfflineDownloads()
    }

    private fun hydrateCachedFeed() = homeFeed.hydrateCachedFeed()

    fun retryIncompleteHomeFeed() = homeFeed.retryIncompleteFeed()

    fun refreshLiveData(userInitiated: Boolean = false) = homeFeed.refresh(userInitiated)

    fun setNotifications(enabled: Boolean) {
        container.preferencesRepository.notificationsEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    private fun refreshAccounts() = accountRefresh.refresh()

    fun setWifiOnly(enabled: Boolean) {
        container.preferencesRepository.wifiOnly = enabled
        BackgroundRefreshScheduler.schedule(container.appContext, enabled)
        // The same explicit preference applies to the phone/tablet offline worker. TV never
        // exposes a download entry point, so it cannot create a job here.
        container.offlineDownloads.onlyWifi(enabled)
        _uiState.update { it.copy(wifiOnly = enabled, snackbar = appString(R.string.notice_wifi_policy_updated)) }
    }

    fun setHomeRowOrder(order: List<app.reelstack.data.model.HomeRow>) = homeFeed.setRowOrder(order)

    fun setHomeSectionVisible(section: HomeSection, visible: Boolean) =
        homeFeed.setSectionVisible(section, visible)

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
                        appString(R.string.warn_cleartext_http)
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
        if (draft.saving || (draft.simpleSetup && draft.quickConnectWaiting)) return
        val normalizedUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .getOrElse {
                updateDraft { copy(error = it.readableMessage(container.appContext) ?: appString(R.string.error_enter_valid_url)) }
                return
            }
        val useJellyfinAccount = draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR, ServiceKind.EMBY) && draft.authMode == ConnectionAuthMode.ACCOUNT
        val useQuickConnect = (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (useQuickConnect) {
            if (draft.simpleSetup && draft.alsoConnect) {
                val companionUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.companionUrl) }.getOrElse {
                    updateDraft { copy(error = appString(R.string.error_check_seerr_url)) }
                    return
                }
                startQuickConnect(draft.copy(companionUrl = companionUrl), normalizedUrl)
                return
            }
            startQuickConnect(draft, normalizedUrl)
            return
        }
        if (useJellyfinAccount && draft.username.isBlank()) {
            updateDraft { copy(error = appString(R.string.error_enter_username)) }
            return
        }
        if (!useJellyfinAccount && draft.token.isBlank()) {
            updateDraft { copy(error = appString(R.string.error_enter_token)) }
            return
        }
        val alternateUrl = draft.alternateUrl.takeIf(String::isNotBlank)?.let { entered ->
            runCatching { EndpointValidator.normalizeBaseUrl(entered) }.getOrElse {
                updateDraft { copy(error = appString(R.string.error_check_alternate_url, it.readableMessage(container.appContext) ?: appString(R.string.error_enter_valid_url))) }
                return
            }
        }.orEmpty()
        if (alternateUrl.isNotBlank() && alternateUrl == normalizedUrl) {
            updateDraft { copy(error = appString(R.string.error_alternate_same_url)) }
            return
        }
        val companionUrl = if (useJellyfinAccount && draft.alsoConnect && draft.kind != ServiceKind.EMBY) {
            runCatching { EndpointValidator.normalizeBaseUrl(draft.companionUrl) }.getOrElse {
                updateDraft { copy(error = appString(R.string.error_check_companion_url, it.readableMessage(container.appContext) ?: appString(R.string.error_enter_valid_url))) }
                return
            }
        } else null
        updateDraft { copy(url = normalizedUrl, companionUrl = companionUrl ?: this.companionUrl, saving = true, error = null, password = "") }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            val credentials = if (useJellyfinAccount) {
                attempt {
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
                    updateDraft { copy(saving = false, error = error.readableMessage(container.appContext) ?: appString(R.string.error_jellyfin_rejected)) }
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
                attempt {
                    withContext(Dispatchers.IO) {
                        val otherKind = if (draft.kind == ServiceKind.SEERR) ServiceKind.JELLYFIN else ServiceKind.SEERR
                        val auth = if (otherKind == ServiceKind.SEERR) container.seerrAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                            else container.jellyfinAuthenticationClient.authenticate(companionUrl, draft.username.trim(), draft.password)
                        val other = ServiceConnection(otherKind, otherKind.displayName, companionUrl, auth.accessToken,
                            userId = auth.userId, sessionCookie = otherKind == ServiceKind.SEERR)
                        val seerr = container.accountProfileClient.load(if (draft.kind == ServiceKind.SEERR) candidate else other)
                        val jellyfinId = if (draft.kind == ServiceKind.JELLYFIN) candidate.userId else other.userId
                        check(app.reelstack.data.model.matchesJellyfinAccount(seerr, jellyfinId)) {
                            appString(R.string.error_seerr_not_linked)
                        }
                        other
                    }
                }.getOrElse { error ->
                    val failureDetail = error.readableMessage(container.appContext) ?: appString(R.string.error_companion_failed)
                    updateDraft { copy(saving = false, error = appString(R.string.error_no_connections_changed, failureDetail)) }
                    return@launch
                }
            } else null
            verifyAndSaveConnection(candidate, companion)
            if (draft.simpleSetup && _uiState.value.activeSheet == null) completeOnboarding()
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
            var quickConnect = attempt {
                withContext(Dispatchers.IO) {
                    if (draft.kind == ServiceKind.SEERR) container.seerrAuthenticationClient.initiateQuickConnect(normalizedUrl)
                    else container.jellyfinAuthenticationClient.initiateQuickConnect(normalizedUrl)
                }
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_start))
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

            quickConnect = attempt {
                app.reelstack.data.network.awaitQuickConnectApproval(
                    initial = quickConnect,
                    maxPolls = QUICK_CONNECT_MAX_POLLS,
                    pollIntervalMillis = QUICK_CONNECT_POLL_INTERVAL_MS,
                    onUpdate = { updateDraft { copy(quickConnectCode = it.code) } },
                    read = { current -> withContext(Dispatchers.IO) {
                        if (draft.kind == ServiceKind.SEERR) {
                            container.seerrAuthenticationClient.quickConnectState(normalizedUrl, current)
                        } else {
                            container.jellyfinAuthenticationClient.quickConnectState(normalizedUrl, current.secret)
                        }
                    } },
                )
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_lost))
                return@launch
            }
            if (!quickConnect.authenticated) {
                updateDraft { copy(saving = false, quickConnectWaiting = false,
                    error = appString(R.string.error_quick_connect_expired)) }
                return@launch
            }

            updateDraft { copy(saving = true, quickConnectWaiting = false, error = null) }
            val credentials = attempt {
                withContext(Dispatchers.IO) {
                    if (draft.kind == ServiceKind.SEERR) {
                        container.seerrAuthenticationClient.authenticateWithQuickConnect(normalizedUrl, quickConnect)
                    } else {
                        container.jellyfinAuthenticationClient.authenticateWithQuickConnect(normalizedUrl, quickConnect.secret)
                    }
                }
            }.getOrElse { error ->
                showQuickConnectFailure(draft.kind, error, appString(R.string.error_quick_connect_incomplete))
                return@launch
            }
            val candidate = ServiceConnection(
                kind = draft.kind,
                name = draft.name.ifBlank { draft.kind.displayName },
                sessionCookie = draft.kind == ServiceKind.SEERR,
                baseUrl = normalizedUrl,
                token = credentials.accessToken,
                userId = credentials.userId,
                alternateUrl = runCatching {
                    draft.alternateUrl.takeIf(String::isNotBlank)?.let(EndpointValidator::normalizeBaseUrl).orEmpty()
                }.getOrDefault(""),
                state = ConnectionState.TESTING,
            )
            val companion = if (draft.simpleSetup && draft.alsoConnect) {
                attempt {
                    withContext(Dispatchers.IO) {
                        app.reelstack.data.network.connectSeerrWithJellyfin(
                            candidate, draft.companionUrl, container.jellyfinAuthenticationClient,
                            container.seerrAuthenticationClient, container.accountProfileClient::load,
                        )
                    }
                }.getOrElse {
                    // The user's Jellyfin approval is already valid. Keep it instead of forcing a
                    // second code merely because Seerr was slow or unavailable at the last step.
                    verifyAndSaveConnection(candidate)
                    if (connectionDraft.value == null) {
                        _uiState.update { state -> state.copy(snackbar = appString(R.string.status_jellyfin_ready_seerr_retry)) }
                        if (draft.simpleSetup && _uiState.value.activeSheet == null) completeOnboarding()
                    }
                    return@launch
                }
            } else null
            verifyAndSaveConnection(candidate, companion)
            if (draft.simpleSetup && _uiState.value.activeSheet == null) completeOnboarding()
        }
    }

    private fun showQuickConnectFailure(kind: ServiceKind, error: Throwable, fallback: String) {
        updateDraft {
            copy(
                authMode = if (kind == ServiceKind.SEERR) ConnectionAuthMode.ACCOUNT else authMode,
                saving = false,
                quickConnectCode = null,
                quickConnectWaiting = false,
                error = if (kind == ServiceKind.SEERR) {
                    appString(R.string.error_quick_connect_seerr_unavailable)
                } else error.readableMessage(container.appContext) ?: fallback,
            )
        }
    }

    private suspend fun verifyAndSaveConnection(candidate: ServiceConnection, companion: ServiceConnection? = null) {
        val personalLogin = connectionDraft.value?.authMode != ConnectionAuthMode.API_KEY
        fun verify(connection: ServiceConnection): app.reelstack.data.model.ConnectionTestResult {
            if (personalLogin && connection.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
                val account = container.accountProfileClient.load(connection)
                check(account.id == connection.userId) { appString(R.string.error_account_verify) }
                return app.reelstack.data.model.ConnectionTestResult(true, 0, app.reelstack.localization.LocalizedText(R.string.status_signed_in))
            }
            return container.connectionTester.test(connection)
        }
        if (companion != null) {
            val otherResult = attempt { withContext(Dispatchers.IO) { verify(companion) } }
            if (otherResult.getOrNull()?.success != true) {
                updateDraft { copy(saving = false, error = appString(R.string.error_companion_verify, companion.kind.displayName)) }
                return
            }
        }
        val result = attempt {
            withContext(Dispatchers.IO) { verify(candidate) }
        }.getOrElse { error ->
            updateDraft {
                copy(saving = false, error = error.readableMessage(container.appContext) ?: appString(R.string.error_service_unreachable))
            }
            return
        }

        if (!result.success) {
            updateDraft { copy(saving = false, error = result.message.text(container.appContext)) }
            return
        }

        // The server accepted us, but the token still has to reach the keystore. If that write
        // fails, say so rather than reporting a connection the app cannot actually reuse.
        val stored = attempt {
            withContext(Dispatchers.IO) {
                container.connectionRepository.save(candidate)
                companion?.let { container.connectionRepository.save(it) }
                container.mediaSnapshotStore.clear(); container.localPlaybackStore.clear()
            }
        }
        stored.exceptionOrNull()?.let {
            updateDraft { copy(saving = false, error = appString(R.string.error_store_token_secure)) }
            return
        }
        homeFeed.cancelRefresh()
        libraryChoicesJob?.cancel()
        libraryJob?.cancel()
        discoverSearch.cancel()
        trackingJob?.cancel()
        historyJob?.cancel()
        val saved = candidate.copy(
            state = ConnectionState.CONNECTED,
            latencyMs = result.latencyMs,
            detail = result.message.text(container.appContext),
        )
        _uiState.update { state ->
            state.copy(
                libraryChoicesOpen = false, libraryChoices = emptyList(), libraryShortcuts = emptyList(), libraryIcons = emptyMap(), libraryFacets = app.reelstack.data.model.LibraryFacets(), libraryFilters = app.reelstack.data.model.LibraryFilters(), libraryDetailMedia = null, libraryEntries = emptyList(), libraryPath = emptyList(), libraryLoading = false, libraryHasMore = false, libraryError = null,
                requestHistory = app.reelstack.data.model.RequestHistoryState(), showRequestHistory = false,
                connections = state.connections.map { if (it.kind == saved.kind) saved else if (it.kind == companion?.kind) companion.copy(state = ConnectionState.CONNECTED) else it },
                accounts = state.accounts - saved.kind - listOfNotNull(companion?.kind).toSet(),
                trackedRequests = if (saved.kind == ServiceKind.SEERR || companion?.kind == ServiceKind.SEERR) emptyList() else state.trackedRequests,
                accountErrors = state.accountErrors - saved.kind,
                adminView = false,
                sessions = emptyList(),
                resume = emptyList(),
                nextUp = emptyList(),
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

    fun signOutAll() {
        if (signOutJob?.isActive == true) return
        val pending = viewModelScope.coroutineContext[Job]?.children?.toList().orEmpty()
        viewModelScope.coroutineContext.cancelChildren()
        closeSessionChannel()
        // This is the only operation that destroys every local account. Remove Media3's opaque
        // jobs before secure connection state, so no queued request can survive sign-out.
        container.offlineDownloads.removeAll()
        container.offlineStorage.clearAll()
        container.connectionRepository.signOutAll()
        container.preferencesRepository.onboardingCompleted = false
        val notifications = container.appContext.getSystemService(android.app.NotificationManager::class.java)
        notifications.activeNotifications.filter { item ->
            app.reelstack.background.NotificationEvent.entries.any { it.channelId == item.notification.channelId }
        }.forEach { notifications.cancel(it.tag, it.id) }
        app.reelstack.widget.NowPlayingWidget.requestUpdate(container.appContext)
        connectionDraft.value = null
        homeFeed.resetRetryClock()
        // A fresh state clears details, favourites, requests and every account-scoped pane.
        _uiState.value = ReelstackUiState(
            signingOut = true, connections = container.connectionRepository.list(),
            sessions = emptyList(), resume = emptyList(), recentMovies = emptyList(),
            recentSeries = emptyList(), upcoming = emptyList(), recentReleases = emptyList(),
            incoming = emptyList(), discover = emptyList(), recommendations = emptyList(), activity = emptyList(),
            notificationsEnabled = container.preferencesRepository.notificationsEnabled,
            wifiOnly = container.preferencesRepository.wifiOnly,
            homeSections = container.preferencesRepository.visibleHomeSections,
            homeRowOrder = container.preferencesRepository.homeRowOrder,
        )
        signOutJob = viewModelScope.launch {
            // Wait for any in-flight disk write before erasing the offline account snapshot.
            pending.joinAll()
            // An authentication write already executing on IO may finish despite cancellation.
            container.connectionRepository.signOutAll()
            withContext(Dispatchers.IO) { container.mediaSnapshotStore.clear(); container.localPlaybackStore.clear() }
            _uiState.update { it.copy(signingOut = false, showOnboarding = true) }
        }
    }

    fun removeConnection(kind: ServiceKind) {
        if (!_uiState.value.canEditConnection(kind)) return
        if (_uiState.value.requestingMediaIds.isNotEmpty()) return
        connectionJob?.cancel()
        quickConnectJob?.cancel()
        accountRefresh.cancel()
        trackingJob?.cancel()
        libraryChoicesJob?.cancel()
        libraryJob?.cancel()
        discoverSearch.cancel()
        homeFeed.cancelRefresh()
        historyJob?.cancel()
        container.connectionRepository.get(kind).takeIf { it.baseUrl.isNotBlank() }?.let {
            container.offlineDownloads.removeScope(_uiState.value.activeProfileId, it)
            container.offlineStorage.clearForConnection(_uiState.value.activeProfileId, it)
        }
        container.connectionRepository.signOut(kind)
        val remaining = container.connectionRepository.list()
        val signedOutEverywhere = remaining.none { it.baseUrl.isNotBlank() }
        if (signedOutEverywhere) container.preferencesRepository.onboardingCompleted = false
        container.mediaSnapshotStore.clear(); container.localPlaybackStore.clear()
        _uiState.update {
            it.copy(
                libraryChoicesOpen = false, libraryChoices = emptyList(), libraryShortcuts = emptyList(), libraryIcons = emptyMap(), libraryFacets = app.reelstack.data.model.LibraryFacets(), libraryFilters = app.reelstack.data.model.LibraryFilters(), libraryDetailMedia = null, libraryEntries = emptyList(), libraryPath = emptyList(), libraryLoading = false, libraryHasMore = false, libraryError = null,
                requestHistory = app.reelstack.data.model.RequestHistoryState(), showRequestHistory = false,
                connections = remaining,
                showOnboarding = signedOutEverywhere,
                adminView = false,
                sessions = emptyList(),
                activity = emptyList(),
                incoming = emptyList(),
                resume = it.resume.filterNot { item -> item.source == kind },
                nextUp = it.nextUp.filterNot { item -> item.source == kind },
                favourites = it.favourites.filterNot { item -> item.source == kind },
                recentMovies = it.recentMovies.filterNot { item -> item.source == kind },
                recentSeries = it.recentSeries.filterNot { item -> item.source == kind },
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
                snackbar = appString(R.string.notice_signed_out_of, kind.displayName),
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

    override fun onCleared() {
        homeFeed.cancel()
        requestDraftJob?.cancel()
        signOutJob?.cancel()
        libraryChoicesJob?.cancel()
        libraryJob?.cancel()
        shelfJob?.cancel()
        peekJob?.cancel()
        seasonsJob?.cancel()
        episodesJob?.cancel()
        discoverSearch.cancel()
        quickConnectJob?.cancel()
        connectionJob?.cancel()
        accountRefresh.cancel()
        trackingJob?.cancel()
        historyJob?.cancel()
        offlineRefreshJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val QUICK_CONNECT_POLL_INTERVAL_MS = 3_000L
        const val QUICK_CONNECT_MAX_POLLS = 60
    }

    private fun isTelevision(): Boolean =
        (container.appContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
}

/**
 * Whether a shelf may fall back to demo titles.
 *
 * The project rule is that demo content belongs to an app nobody has connected anything to yet. It
 * is emphatically not an empty state: a household whose Jellyfin is down must see that their
 * Jellyfin is down, not five films they do not own. Written once and tested, because it used to be
 * written out at every one of twelve call sites.
 */
internal fun <T> demoContent(
    configuredKinds: Set<ServiceKind>,
    servedByRealService: Boolean,
    demo: () -> List<T>,
): List<T> = if (servedByRealService || configuredKinds.isNotEmpty()) emptyList() else demo()

private fun initialState(container: AppContainer): ReelstackUiState {
    val connections = container.connectionRepository.list()
    val configuredKinds = connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
    val hasMediaServer = configuredKinds.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val hasQueueService = configuredKinds.any { it == ServiceKind.RADARR || it == ServiceKind.SONARR }
    val hasSeerr = ServiceKind.SEERR in configuredKinds

    return ReelstackUiState(
        selectedTab = if (container.preferencesRepository.personalization.startInLibrary) AppTab.LIBRARY else AppTab.HOME,
        showOnboarding = configuredKinds.isEmpty() && !container.preferencesRepository.onboardingCompleted,
        connections = connections,
        activeProfileId = container.connectionRepository.activeProfileId,
        searchHistory = container.preferencesRepository.searchHistory(container.connectionRepository.activeProfileId),
        profiles = container.connectionRepository.listProfiles(),
        allProfileConnections = container.connectionRepository.listProfiles().associate { it.id to container.connectionRepository.list(it.id) },
        isKidMode = container.connectionRepository.isKidMode,
        selectedLibrarySource = container.preferencesRepository.preferredLibrarySource,
        libraryShortcuts = connections.sortedBy { it.kind != container.preferencesRepository.preferredLibrarySource }
            .firstOrNull { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() && it.token.isNotBlank() }
            ?.let(container.preferencesRepository::libraryShortcuts).orEmpty(),
        libraryIcons = connections.sortedBy { it.kind != container.preferencesRepository.preferredLibrarySource }
            .firstOrNull { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() && it.token.isNotBlank() }
            ?.let(container.preferencesRepository::libraryIcons).orEmpty(),
        // Ten copies of the same rule, written out ten times, is ten chances to get it wrong once.
        // `demoContent` is the rule: show made-up titles only when the app has nothing at all set
        // up, and never as a fallback for a service that is configured but not answering.
        sessions = demoContent(configuredKinds, hasMediaServer, ::demoSessions),
        resume = demoContent(configuredKinds, hasMediaServer, ::demoResume),
        recentMovies = demoContent(configuredKinds, hasMediaServer, ::demoRecentMovies),
        nextUp = demoContent(configuredKinds, hasMediaServer, ::demoNextUp),
        favourites = demoContent(configuredKinds, servedByRealService = false, demo = ::demoFavourites),
        recentSeries = demoContent(configuredKinds, hasMediaServer, ::demoRecentSeries),
        upcoming = demoContent(configuredKinds, hasQueueService, ::demoUpcoming),
        recentReleases = demoContent(configuredKinds, hasQueueService, ::demoRecentReleases),
        incoming = demoContent(configuredKinds, hasQueueService, ::demoIncoming),
        discover = demoContent(configuredKinds, hasSeerr, ::demoDiscover),
        recommendations = demoContent(configuredKinds, servedByRealService = false, demo = ::demoRecommendations),
        activity = demoContent(configuredKinds, hasQueueService || hasSeerr, ::demoActivity),
        notificationsEnabled = container.preferencesRepository.notificationsEnabled,
        wifiOnly = container.preferencesRepository.wifiOnly,
        homeSections = container.preferencesRepository.visibleHomeSections,
        homeRowOrder = container.preferencesRepository.homeRowOrder,
        hasCachedData = false,
        isRefreshing = configuredKinds.isNotEmpty(),
    )
}
