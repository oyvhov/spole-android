package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.SessionScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Composition only: one set of feature owners per authenticated session. No feature receives the
 * ViewModel or the container. Retiring this graph revokes its state writer before cancelling work;
 * a fresh graph has fresh jobs, navigation history and caches even on an away-and-back switch.
 */
internal class ReelstackFeatures(
    container: AppContainer,
    parent: CoroutineScope,
    snapshot: MutableStateFlow<ReelstackUiState>,
    private val channelState: MutableStateFlow<Boolean>,
    isTelevision: () -> Boolean,
    isSigningOut: () -> Boolean,
    selectTab: (AppTab) -> Unit,
    openSheet: (AppSheet) -> Unit,
    closeSheet: () -> Unit,
    synchronizeSession: () -> Unit,
    switchProfile: (String) -> Unit,
    commitConnections: suspend (SessionScope, List<ServiceConnection>) -> SessionScope,
    onConnected: (SessionScope, ServiceConnection, ServiceConnection?, String?) -> Unit,
) {
    private val state = SessionUiState(snapshot)
    private val lifetime = SupervisorJob(parent.coroutineContext[Job])
    private val scope = CoroutineScope(parent.coroutineContext + lifetime)
    private fun featureScope() = CoroutineScope(scope.coroutineContext + SupervisorJob(lifetime))

    val accounts = AccountRefreshCoordinator(accountProfileClient = container.accountProfileClient, appContext = container.appContext, connectionRepository = container.connectionRepository,
        scope = featureScope(), readState = { state.value }, updateState = state::update)
    val search = DiscoverSearchCoordinator(appContext = container.appContext, mediaSyncRepository = container.mediaSyncRepository, preferencesRepository = container.preferencesRepository,
        scope = featureScope(), readState = { state.value }, updateState = state::update)
    val globalSearch = DiscoverSearchCoordinator(appContext = container.appContext, mediaSyncRepository = container.mediaSyncRepository, preferencesRepository = container.preferencesRepository,
        scope = featureScope(), readState = { state.value }, updateState = state::update,
        slice = { it.globalSearch }, withSlice = { current, search -> current.copy(globalSearch = search) })
    val discover = DiscoverHubCoordinator(mediaSyncRepository = container.mediaSyncRepository,
        scope = featureScope(), readState = { state.value }, updateState = state::update)
    val home: HomeFeedCoordinator = HomeFeedCoordinator(appContext = container.appContext, connectionRepository = container.connectionRepository, localPlaybackStore = container.localPlaybackStore, mediaFingerprint = container::mediaFingerprint, mediaSnapshotStore = container.mediaSnapshotStore, mediaSyncRepository = container.mediaSyncRepository, preferencesRepository = container.preferencesRepository, sessionSocket = container.sessionSocket,
        scope = featureScope(), readState = { state.value }, updateState = state::update,
        refreshAccounts = { accounts.refresh() }, refreshTrackedRequests = { requests.refreshTrackedRequests() },
        openLibraryDetails = { details.openLibraryDetails(it) }, isSigningOut = isSigningOut)

    val library: LibraryCoordinator = LibraryCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        mediaServerClient = container.mediaServerClient,
        mediaSyncRepository = container.mediaSyncRepository,
        preferencesRepository = container.preferencesRepository,
        closeSmartShelfEditor = { shelves.closeSmartShelfEditor() },
        cancelSmartPeeks = { shelves.cancelPeeks() }, loadSmartShelfPeeks = { shelves.loadSmartShelfPeeks() },
        smartShelfOfPath = { shelves.smartShelfOfPath(it) }, openSmartShelf = { shelves.openSmartShelf(it) },
        cancelSearch = { search.cancel(); globalSearch.cancel() }, setSearchQuery = { search.setQuery(it) },
        openLibraryDetails = { details.openLibraryDetails(it) },
    )

    val shelves: SmartShelfCoordinator = SmartShelfCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        mediaServerClient = container.mediaServerClient,
        mediaSyncRepository = container.mediaSyncRepository,
        preferencesRepository = container.preferencesRepository,
        selectLibrarySource = { library.selectLibrarySource(it) }, browseLibrary = { library.browseLibrary() },
        invalidateLibraryPages = { library.invalidatePages() }, setHomeLayout = { home.setLayout(it) },
    )

    val details: DetailsCoordinator = DetailsCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        mediaSyncRepository = container.mediaSyncRepository,
        isTelevision = isTelevision,
    )

    val mediaActions: MediaActionsCoordinator = MediaActionsCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        localPlaybackStore = container.localPlaybackStore,
        mediaSyncRepository = container.mediaSyncRepository,
        invalidateLibraryPages = { library.invalidatePages(it) },
    )

    val requests: RequestsCoordinator = RequestsCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        mediaSyncRepository = container.mediaSyncRepository,
        requestHistoryRepository = container.requestHistoryRepository,
        requestRulesClient = container.requestRulesClient,
        requestTrackingRepository = container.requestTrackingRepository,
        openSeerrAccount = { connections.openSeerrAccount() },
    )

    val offline: OfflineDownloadsCoordinator = OfflineDownloadsCoordinator(
        _uiState = state, operationScope = featureScope(),
        appContext = container.appContext,
        offlineDownloads = container.offlineDownloads,
        isTelevision = isTelevision, selectTab = { if (state.isCurrent) selectTab(it) },
    )

    val kids: KidsLibraryCoordinator = KidsLibraryCoordinator(
        _uiState = state, operationScope = featureScope(),
        mediaServerClient = container.mediaServerClient,
        mediaSyncRepository = container.mediaSyncRepository,

    )

    val profiles: ProfileCoordinator = ProfileCoordinator(
        _uiState = state, operationScope = featureScope(),
        accountProfileClient = container.accountProfileClient,
        appContext = container.appContext,
        connectionRepository = container.connectionRepository,
        embyAuthenticationClient = container.embyAuthenticationClient,
        jellyfinAuthenticationClient = container.jellyfinAuthenticationClient,
        offlineDownloads = container.offlineDownloads,
        offlineStorage = container.offlineStorage,
        pinSecurity = container.pinSecurity,
        switchProfileNow = { if (state.isCurrent) switchProfile(it) },
        closeSheet = { if (state.isCurrent) closeSheet() },
    )

    val connections: ConnectionSetupCoordinator = ConnectionSetupCoordinator(
        _uiState = state, operationScope = featureScope(),
        accountProfileClient = container.accountProfileClient,
        appContext = container.appContext,
        connectionRepository = container.connectionRepository,
        connectionTester = container.connectionTester,
        embyAuthenticationClient = container.embyAuthenticationClient,
        jellyfinAuthenticationClient = container.jellyfinAuthenticationClient,
        seerrAuthenticationClient = container.seerrAuthenticationClient,
        serverDiscovery = container.serverDiscovery,
        serverProbe = container.serverProbe,
        openSheet = { if (state.isCurrent) openSheet(it) }, synchronizeSession = synchronizeSession,
        commitConnections = commitConnections, onConnected = onConnected,
    )

    val homeActions: HomeActionsCoordinator = HomeActionsCoordinator(
        _uiState = state, operationScope = featureScope(),
        accountProfileClient = container.accountProfileClient,
        appContext = container.appContext,
        calendarFollowStore = container.calendarFollowStore,
        connectionRepository = container.connectionRepository,
        homeLibraries = container::homeLibraries,
        mediaFingerprint = container::mediaFingerprint,
        mediaServerClient = container.mediaServerClient,
        mediaSyncRepository = container.mediaSyncRepository,
        offlineDownloads = container.offlineDownloads,
        preferencesRepository = container.preferencesRepository,
        cancelHomeRefresh = { home.cancelRefresh() },
        refreshLiveData = { home.refresh(true); shelves.loadHomeShelves(true) },
    )

    init {
        scope.launch { home.sessionChannelLive.collect { channelState.value = it } }
    }

    /** Returns the cancelled job so sign-out can await disk writers before erasing snapshots. */
    fun close(): Job {
        state.revoke()
        home.cancel()
        channelState.value = false
        lifetime.cancel()
        return lifetime
    }
}
