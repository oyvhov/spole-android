package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.model.mergedSmartShelves
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class AppTab { HOME, LIBRARY, DOWNLOADS, DISCOVER, SETTINGS }

enum class ConnectionAuthMode { QUICK_CONNECT, ACCOUNT, API_KEY }

sealed interface AppSheet {
    data class SessionDetails(val sessionKey: String) : AppSheet
    data class TitleDetails(val key: String) : AppSheet
    data class Person(val id: Int) : AppSheet
    data object RequestComposer : AppSheet
    data object UpcomingCalendar : AppSheet
    data class ConnectionEditor(val kind: ServiceKind) : AppSheet
    data object ProfileSwitcher : AppSheet
    data object AddProfile : AppSheet
    data class PinPrompt(
        val targetProfileId: String,
        val isSetup: Boolean = false,
        val switchAfterSetup: Boolean = true,
        val disableAfterVerification: Boolean = false,
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
    /** The server's own name once it has answered, e.g. "Stova". Empty until then. */
    val serverName: String = "",
    /** [url] is an address that answered as a server of [kind]; nothing needs guessing any more. */
    val addressResolved: Boolean = false,
    val resolvingAddress: Boolean = false,
    /** Seerr answered next to the chosen LAN server, so its address was filled in for the user. */
    val seerrFound: Boolean = false,
    /** Users the server lists on its sign-in screen; picking one saves typing a name with a remote. */
    val loginUsers: List<app.reelstack.data.network.PublicUser> = emptyList(),
    /** Started from the first-run server list: a successful sign-in goes straight home. */
    val finishOnboarding: Boolean = false,
)

data class ReelstackUiState(
    val connectionDraft: ConnectionDraft? = null,
    val sessionScope: app.reelstack.data.repository.SessionScope? = null,
    val signingOut: Boolean = false,
    val showOnboarding: Boolean = false,
    val selectedTab: AppTab = AppTab.HOME,
    /**
     * Where Back leads from Downloads when they are not a menu item: Settings when opened from
     * there, or the tab a queued download or its notification came from.
     */
    val downloadsReturnTab: AppTab = AppTab.SETTINGS,
    /** A destination over the current adult screen, deliberately not another navigation tab. */
    val globalSearchOpen: Boolean = false,
    /** The global search's own query and answers, apart from Discover's field. */
    val globalSearch: app.reelstack.ui.state.SearchSlice = app.reelstack.ui.state.SearchSlice(),
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
    /** Jellyfin and Emby servers that answered the LAN broadcast, best first. */
    val discoveredServers: List<app.reelstack.data.network.DiscoveredServer> = emptyList(),
    val discoveringServers: Boolean = false,
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
    val playbackUnavailableSources: Set<ServiceKind> = emptySet(),
    val resume: List<LibraryMedia> = demoResume(),
    val nextUp: List<LibraryMedia> = emptyList(),
    val favourites: List<LibraryMedia> = emptyList(),
    val recentMovies: List<LibraryMedia> = demoRecentMovies(),
    val recentSeries: List<LibraryMedia> = demoRecentSeries(),
    val upcoming: List<UpcomingMedia> = demoUpcoming(),
    val recentReleases: List<UpcomingMedia> = demoRecentReleases(),
    val upcomingError: String? = null,
    val calendarUndated: List<app.reelstack.data.model.CalendarTitle> = emptyList(),
    val calendarHidden: Set<String> = emptySet(),
    val recentReleasesError: String? = null,
    val incoming: List<IncomingMedia> = demoIncoming(),
    val discover: List<DiscoverMedia> = demoDiscover(),
    val recommendations: List<DiscoverMedia> = demoRecommendations(),
    val searchResults: List<DiscoverMedia> = emptyList(),
    val searchPeople: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),
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
    /** This profile's own smart shelves and changed presets. See [app.reelstack.data.model.mergedSmartShelves]. */
    val smartShelves: List<app.reelstack.data.model.SmartShelf> = emptyList(),
    /** A few titles per shelf for its tile in the library, keyed by shelf id. */
    val smartShelfPeeks: Map<String, List<app.reelstack.data.network.RemoteLibraryItem>> = emptyMap(),
    val smartShelfEditor: app.reelstack.data.model.SmartShelfEditor? = null,
    /** The titles of each smart shelf's row on Home, keyed by [app.reelstack.data.model.HomeRowKey.id]. */
    val homeShelfMedia: Map<String, List<LibraryMedia>> = emptyMap(),
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
    /** Detail sheets show preparation immediately instead of looking as though their button did nothing. */
    val offlinePreparingDetailKeys: Set<String> = emptySet(),
    /** A refusal belongs to one detail sheet only; it must not be mistaken for a server-wide error. */
    val offlineDownloadNoticeDetailKey: String? = null,
    val offlineDownloadNotice: String? = null,
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
    /**
     * Every Home row per server, in order, with its switch. The app always sets it; null lets a
     * test describe Home with [homeSections] and [homeRowOrder] as before.
     */
    val homeLayout: app.reelstack.data.model.HomeLayout? = null,
    /** Which libraries feed each Home row, per media server. Separate from the Library tab. */
    val homeLibraries: Map<ServiceKind, app.reelstack.data.model.HomeLibraryChoice> = emptyMap(),
    /** Each media server's libraries, loaded when the Home editor opens, to choose from. */
    val homeLibraryViews: Map<ServiceKind, List<app.reelstack.data.network.RemoteLibraryView>> = emptyMap(),
    val homeLibraryViewsLoading: Boolean = false,
    val homeLibraryViewsFailed: Set<ServiceKind> = emptySet(),
    val hasCachedData: Boolean = false,
    /**
     * Services whose Home rows have been filled once, from the cache or live, for this set of
     * accounts. Only a row that has never loaded shows a skeleton; later refreshes update in place.
     */
    val loadedSources: Set<ServiceKind> = emptySet(),
    val snackbar: String? = null,
    val contentDetails: ContentDetails? = null,
    val returnToCalendar: Boolean = false,
    val requestDraft: RequestDraft? = null,
    val trackedRequests: List<TrackedRequest> = emptyList(),
    val requestHistory: app.reelstack.data.model.RequestHistoryState = app.reelstack.data.model.RequestHistoryState(),
    val showRequestHistory: Boolean = false,
    /** «Dine førespurnader» is a page inside Oppdag; a notification or a tap on «Alle» opens it. */
    val myRequestsOpen: Boolean = false,
    /** A title opened from outside the Discover lists, such as a request line, so details and requests can find it. */
    val detailMedia: DiscoverMedia? = null,
    /** The Seerr rows of Oppdag, each loaded on its own. */
    val discoverHub: app.reelstack.data.model.DiscoverHubState = app.reelstack.data.model.DiscoverHubState(),
    /** The Seerr person page that is open, or was last open. */
    val personPage: app.reelstack.data.model.PersonPage? = null,
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

    /** Every Seerr card on screen anywhere, so details and requests work from the global search too. */
    val knownDiscoverMedia: List<DiscoverMedia>
        get() = listOfNotNull(detailMedia) + personPage?.inLibrary.orEmpty() + personPage?.more.orEmpty() +
            contentDetails?.similar.orEmpty() + contentDetails?.recommended.orEmpty() + discoverHub.allMedia + discover + searchResults + globalSearch.results + recommendations +
            upcoming.mapNotNull { item -> item.tmdbId?.let { id ->
                DiscoverMedia(item.id, item.title, item.subtitle, item.artworkRes, false,
                    artworkUrl = item.artworkUrl, remoteId = id, mediaType = if (item.mediaType == "Movie") "movie" else "tv",
                    overview = item.overview, facts = item.facts + item.dateLabel, genres = item.genres)
            } } + calendarUndated.map { title ->
                DiscoverMedia("calendar:${title.key}", title.title, "", R.drawable.media_placeholder, false,
                    artworkUrl = title.artworkUrl, remoteId = title.tmdbId, mediaType = title.mediaType)
            }

    /** Applies one change to a Seerr title wherever it is listed. */
    fun mapDiscoverMedia(transform: (DiscoverMedia) -> DiscoverMedia): ReelstackUiState = copy(
        detailMedia = detailMedia?.let(transform),
        personPage = personPage?.let { it.copy(inLibrary = it.inLibrary.map(transform), more = it.more.map(transform)) },
        contentDetails = contentDetails?.let { it.copy(similar = it.similar.map(transform), recommended = it.recommended.map(transform)) },
        discoverHub = discoverHub.copy(
            rows = discoverHub.rows.mapValues { (_, row) -> row.copy(items = row.items.map(transform)) },
            gridRow = discoverHub.gridRow.copy(items = discoverHub.gridRow.items.map(transform)),
        ),
        discover = discover.map(transform),
        recommendations = recommendations.map(transform),
        searchResults = searchResults.map(transform),
        globalSearch = globalSearch.copy(results = globalSearch.results.map(transform)),
    )

    val configuredCount: Int
        get() = connections.count { it.baseUrl.isNotBlank() }

    /** The layout Home renders: the saved one, or the one the older fields describe. */
    val effectiveHomeLayout: app.reelstack.data.model.HomeLayout
        get() {
            val rows = homeShelfRows
            val layout = (homeLayout ?: app.reelstack.data.model.HomeLayout.fromLegacy(homeRowOrder, homeSections, showNextUp = true))
                .withShelfRows(rows)
            val scopes = mergedSmartShelves(smartShelves).mapNotNull { shelf -> shelf.scope?.let { shelf.id to it } }.toMap()
            return layout.copy(order = layout.order.filter { row ->
                val scope = row.shelf?.let { scopes[it] }
                scope == null || connections.any { it.kind == row.source && scope.matches(it) }
            })
        }

    /** The Home rows smart shelves have today: one per shelf and per media server with an address. */
    val homeShelfRows: List<app.reelstack.data.model.HomeRowKey>
        get() = app.reelstack.data.model.homeShelves(smartShelves, java.time.LocalDate.now()).flatMap { shelf ->
            connections.filter { it.kind in homeMediaSources && it.token.isNotBlank() && shelf.appliesTo(it) }
                .map { app.reelstack.data.model.HomeRowKey.shelfRow(it.kind, shelf.id) }.distinct()
        }

    /** Media servers that have an address, and so rows of their own on Home. */
    val homeMediaSources: List<ServiceKind>
        get() = app.reelstack.data.model.HOME_MEDIA_SOURCES.filter { kind -> connections.any { it.kind == kind && it.baseUrl.isNotBlank() } }

    fun homeFetchPlan(): app.reelstack.data.model.HomeFetchPlan =
        app.reelstack.data.model.HomeFetchPlan(effectiveHomeLayout, homeLibraries)

    fun canEditConnection(kind: ServiceKind): Boolean = kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY, ServiceKind.SEERR) ||
        connections.none { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() } || adminView

    val onlineCount: Int
        get() = connections.count { it.state == ConnectionState.CONNECTED }

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

internal fun initialState(container: AppContainer): ReelstackUiState {
    val demoContext = app.reelstack.localization.AppLanguages.wrap(container.appContext)
    val owner = container.connectionRepository.captureSession()
    val connections = container.connectionRepository.list()
    val configuredKinds = connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
    val hasMediaServer = owner.kidsMode || configuredKinds.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val hasQueueService = owner.kidsMode || ServiceKind.SEERR in configuredKinds
    val hasSeerr = owner.kidsMode || ServiceKind.SEERR in configuredKinds

    return ReelstackUiState(
        sessionScope = owner,
        selectedTab = if (container.preferencesRepository.personalization.startInLibrary) AppTab.LIBRARY else AppTab.HOME,
        showOnboarding = configuredKinds.isEmpty() && !container.preferencesRepository.onboardingCompleted,
        connections = connections,
        activeProfileId = container.connectionRepository.activeProfileId,
        searchHistory = container.preferencesRepository.searchHistory(container.connectionRepository.activeProfileId),
        smartShelves = container.preferencesRepository.smartShelves(container.connectionRepository.activeProfileId),
        profiles = container.connectionRepository.listProfiles(),
        allProfileConnections = if (owner.kidsMode) emptyMap() else container.connectionRepository.listProfiles().associate { it.id to container.connectionRepository.list(it.id) },
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
        sessions = demoContent(configuredKinds, hasMediaServer, { demoSessions(demoContext) }),
        resume = demoContent(configuredKinds, hasMediaServer, { demoResume(demoContext) }),
        recentMovies = demoContent(configuredKinds, hasMediaServer, { demoRecentMovies(demoContext) }),
        nextUp = demoContent(configuredKinds, hasMediaServer, { demoNextUp(demoContext) }),
        favourites = demoContent(configuredKinds, servedByRealService = owner.kidsMode, demo = { demoFavourites(demoContext) }),
        recentSeries = demoContent(configuredKinds, hasMediaServer, { demoRecentSeries(demoContext) }),
        upcoming = demoContent(configuredKinds, hasQueueService, { demoUpcoming(demoContext) }),
        recentReleases = demoContent(configuredKinds, hasQueueService, { demoRecentReleases(demoContext) }),
        incoming = demoContent(configuredKinds, hasQueueService, { demoIncoming(demoContext) }),
        discover = demoContent(configuredKinds, hasSeerr, { demoDiscover(demoContext) }),
        recommendations = demoContent(configuredKinds, servedByRealService = owner.kidsMode, demo = { demoRecommendations(demoContext) }),
        activity = demoContent(configuredKinds, hasQueueService || hasSeerr, ::demoActivity),
        notificationsEnabled = container.preferencesRepository.notificationsEnabled,
        wifiOnly = container.preferencesRepository.wifiOnly,
        homeSections = container.preferencesRepository.visibleHomeSections,
        homeRowOrder = container.preferencesRepository.homeRowOrder,
        homeLayout = container.preferencesRepository.homeLayout,
        homeLibraries = container.homeLibraries(connections),
        hasCachedData = false,
        isRefreshing = configuredKinds.isNotEmpty(),
    )
}
