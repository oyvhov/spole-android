package app.reelstack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.SessionScope
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.state.toDiscoverUiState
import app.reelstack.ui.state.toHomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Navigation and session lifecycle; feature work belongs to the session's ReelstackFeatures. */
class ReelstackViewModel(
    private val container: AppContainer,
    /** Reads a title by server id for a launcher link; tests answer it themselves. */
    private val remoteDetailsLoader: suspend (ServiceConnection, String) -> app.reelstack.data.network.RemoteMediaDetails = { connection, id ->
        withContext(Dispatchers.IO) { container.mediaServerClient.details(connection, id) }
    },
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(container))
    val uiState: StateFlow<ReelstackUiState> = _uiState.asStateFlow()
    val sessionScope = container.connectionRepository.sessionScope
    val renderingSession = uiState.project { it.sessionScope }
    val homeUiState = uiState.project(ReelstackUiState::toHomeUiState)
    val discoverUiState: StateFlow<DiscoverUiState> = uiState.project(ReelstackUiState::toDiscoverUiState)
    val connectionDraft: StateFlow<ConnectionDraft?> = uiState.project { it.connectionDraft }
    private val channelState = MutableStateFlow(false)
    val sessionChannelLive: StateFlow<Boolean> = channelState.asStateFlow()
    private var boundSession = container.connectionRepository.captureSession()
    private var committingSession = false
    private var signOutJob: Job? = null
    private var features = newFeatures()

    private fun newFeatures() = ReelstackFeatures(
        container, viewModelScope, _uiState, channelState,
        isTelevision = ::isTelevision, isSigningOut = { signOutJob?.isActive == true },
        selectTab = ::selectTab, openSheet = ::openSheet, closeSheet = ::closeSheet,
        synchronizeSession = ::synchronizeSession,
        switchProfile = ::switchProfileNow, commitConnections = ::commitConnections, onConnected = ::onConnected,
        remoteDetails = remoteDetailsLoader,
    )

    init {
        viewModelScope.launch { container.connectionRepository.sessionScope.collect { synchronizeSession() } }
        viewModelScope.launch { container.connectionRepository.changes.collect { features.profiles.loadProfiles() } }
        viewModelScope.launch {
            container.localPlaybackStore.changes.collect {
                _uiState.update { it.copy(resume = features.home.localResume(it.resume, it.connections),
                    nextUp = features.home.localNextUp(it.nextUp, it.connections)) }
            }
        }
        if (_uiState.value.showOnboarding) discoverServers()
        features.profiles.loadProfiles()
        features.home.hydrateCachedFeed()
        refreshLiveData()
        if (_uiState.value.selectedTab == AppTab.LIBRARY) browseLibrary()
    }

    fun selectTab(tab: AppTab) {
        // Downloads are intentionally a personal mobile/tablet space. There is no TV or kids-mode
        // loophole through a notification, a restored tab, or a direct ViewModel call.
        if (tab == AppTab.DOWNLOADS && (_uiState.value.isKidMode || isTelevision())) return
        // Keep the last library location when moving between tabs on a phone. The library loader can
        // refresh it in place, but dropping the path made a return to Films look like a new visit to the
        // library root every time. On a television Bibliotek is the library page itself.
        _uiState.update {
            val root = tab == AppTab.LIBRARY && it.libraryPath.isNotEmpty() &&
                app.reelstack.data.model.libraryMenuOpensRoot(isTelevision(), it.selectedTab == AppTab.LIBRARY)
            (if (root) it.copy(libraryPath = emptyList(), libraryEntries = emptyList(), libraryOffset = 0, libraryHasMore = false,
                libraryFilters = app.reelstack.data.model.LibraryFilters(), libraryCollectionType = null) else it).copy(
                selectedTab = tab,
                downloadsReturnTab = if (tab == AppTab.DOWNLOADS && it.selectedTab != AppTab.DOWNLOADS) it.selectedTab
                    else it.downloadsReturnTab,
                activeSheet = null,
                returnToCalendar = false,
                globalSearchOpen = false,
            )
        }
        if (tab == AppTab.LIBRARY) browseLibrary(false)
        if (tab == AppTab.DISCOVER) features.discover.refresh(force = false)
        features.offline.selectedTabChanged(tab)
    }

    /** Where the remote left Oppdag's rows, to put it back there on return. */
    fun leaveDiscoverHub(point: app.reelstack.data.model.DiscoverReturn) =
        _uiState.update { it.copy(discoverHub = it.discoverHub.copy(returnTo = point)) }

    fun discoverHubReturned() = _uiState.update { it.copy(discoverHub = it.discoverHub.copy(returnTo = null)) }

    /** Oppdag with «Dine førespurnader» open: where a request notification lands. */
    fun openMyRequests() {
        selectTab(AppTab.DISCOVER)
        _uiState.update { it.copy(myRequestsOpen = true) }
        if (_uiState.value.requestHistory.items.isEmpty()) loadRequestHistory(false)
    }

    fun closeMyRequests() = _uiState.update { it.copy(myRequestsOpen = false) }

    fun openGlobalSearch() {
        if (_uiState.value.isKidMode) return
        features.globalSearch.cancel()
        _uiState.update {
            it.copy(
                globalSearchOpen = true,
                globalSearch = app.reelstack.ui.state.SearchSlice(),
                activeSheet = null,
                searchHistory = container.preferencesRepository.searchHistory(it.activeProfileId),
            )
        }
    }

    fun closeGlobalSearch() {
        features.globalSearch.cancel()
        _uiState.update { it.copy(globalSearchOpen = false, globalSearch = app.reelstack.ui.state.SearchSlice()) }
    }


    fun completeOnboarding() {
        container.preferencesRepository.onboardingCompleted = true
        _uiState.update { it.copy(showOnboarding = false, selectedTab = AppTab.HOME) }
        if (_uiState.value.configuredCount == 0) refreshLiveData()
    }

    fun openSheet(sheet: AppSheet) {
        if (sheet is AppSheet.ConnectionEditor && !features.connections.openEditor(sheet)) return
        _uiState.update { it.copy(activeSheet = sheet, returnToCalendar = false) }
    }

    /** An external launcher destination resets both the shared and the child-local navigation. */
    fun openLauncherHome() {
        // Clears detail history, the episode return and the person trail first, so closing the
        // sheet closes it instead of stepping back to an earlier title.
        features.details.closeDetails()
        closeSheet()
        closeKidsSeries()
        features.kids.closeLauncherLibrary()
        selectTab(AppTab.HOME)
        _uiState.update { it.copy(contentDetails = null, seriesBrowse = SeriesBrowse(),
            launcherHomeRequest = it.launcherHomeRequest + 1) }
    }

    /** Opens the server's id even before Home or Library has fetched a row containing it. */
    fun openRemoteDetails(kind: ServiceKind, itemId: String) {
        if (!isRemoteDetailsTarget(kind, itemId)) return
        synchronizeSession()
        openLauncherHome()
        features.details.openRemoteDetails(kind, itemId)
    }

    /** A library link remains bound to the user that the launcher receiver authenticated. */
    fun openRemoteLibrary(kind: ServiceKind, userId: String, libraryId: String) {
        synchronizeSession()
        if (_uiState.value.connections.none {
                it.kind == kind && it.userId == userId && it.token.isNotBlank() && it.baseUrl.isNotBlank()
            }) return
        openLauncherHome()
        if (_uiState.value.isKidMode) features.kids.openLauncherLibrary(kind, libraryId)
        else features.library.openLauncherLibrary(kind, libraryId)
    }

    fun closeSheet() {
        // Back from a title also drops any detail answer still in flight.
        if (returnFromDetail()) return
        if (_uiState.value.returnToCalendar) { backToCalendar(); return }
        if (!features.requests.closeComposer()) return
        features.details.closeDetails()
        features.connections.cancelConnectionSetup()
        _uiState.update { it.copy(activeSheet = null, contentDetails = null, returnToCalendar = false,
            seriesBrowse = SeriesBrowse(), connectionDraft = null) }
    }

    fun backToCalendar() {
        _uiState.update { it.copy(activeSheet = AppSheet.UpcomingCalendar, contentDetails = null, returnToCalendar = false) }
    }

    fun openAccountsSettings() {
        if (_uiState.value.isKidMode) return
        _uiState.update { it.copy(activeSheet = null, selectedTab = AppTab.SETTINGS,
            accountsSettingsRequest = it.accountsSettingsRequest + 1) }
    }

    private fun switchProfileNow(profileId: String) {
        container.connectionRepository.activeProfileId = profileId
        synchronizeSession()
    }

    /** Also called before foreground rendering, so a retained root cannot expose the old account. */
    fun synchronizeSession() {
        if (_uiState.value.signingOut || signOutJob?.isActive == true) return
        val next = container.connectionRepository.captureSession()
        if (next == boundSession || committingSession && next.profileId == boundSession.profileId) return
        val previousProfileId = boundSession.profileId
        features.close()
        boundSession = next
        val previousTab = _uiState.value.selectedTab
        _uiState.value = initialState(container).copy(
            sessionScope = next, selectedTab = if (next.kidsMode) AppTab.HOME else previousTab,
            resume = emptyList(), nextUp = emptyList(), favourites = emptyList(), sessions = emptyList(),
            recentMovies = emptyList(), recentSeries = emptyList(), upcoming = emptyList(), recentReleases = emptyList(),
            incoming = emptyList(), discover = emptyList(), recommendations = emptyList(), activity = emptyList(),
            loadedSources = emptySet(), lastUpdatedEpochMillis = null, isRefreshing = false,
        )
        features = newFeatures()
        if (!isTelevision() && previousProfileId != next.profileId) viewModelScope.launch(Dispatchers.IO) {
            container.offlineDownloads.setProfileActive(previousProfileId, active = false)
            if (!next.kidsMode && container.connectionRepository.isCurrent(next)) {
                container.offlineDownloads.setProfileActive(next.profileId, active = true)
            }
        }
        features.profiles.loadProfiles()
        features.home.hydrateCachedFeed()
        refreshLiveData(userInitiated = true)
        features.offline.selectedTabChanged(_uiState.value.selectedTab)
    }

    private suspend fun commitConnections(owner: SessionScope, candidates: List<ServiceConnection>): SessionScope {
        committingSession = true
        try {
            return withContext(Dispatchers.IO) {
                val committed = checkNotNull(container.connectionRepository.saveAndCaptureIfCurrent(owner, candidates))
                container.mediaSnapshotStore.clear()
                container.shelfAnswerCache.clear()
                container.localPlaybackStore.clear()
                committed
            }
        } finally { committingSession = false }
    }

    private fun onConnected(committed: SessionScope, saved: ServiceConnection, companion: ServiceConnection?, notice: String?) {
        val finishOnboarding = _uiState.value.connectionDraft?.let { it.simpleSetup || it.finishOnboarding } == true
        synchronizeSession()
        if (!container.connectionRepository.isCurrent(committed)) return
        _uiState.update { current -> current.copy(
            connections = current.connections.map {
                if (it.kind == saved.kind) saved
                else if (it.kind == companion?.kind) companion.copy(state = ConnectionState.CONNECTED) else it
            },
            snackbar = notice ?: if (companion != null) appString(R.string.notice_linked_signed_in)
                else appString(R.string.notice_service_signed_in, saved.kind.displayName),
            connectionDraft = null,
        ) }
        if (finishOnboarding) completeOnboarding()
    }

    fun signOutAll() {
        if (_uiState.value.signingOut || signOutJob?.isActive == true) return
        _uiState.update { it.copy(signingOut = true) }
        val profileId = container.connectionRepository.activeProfileId
        val pending = features.close()
        // This is the only operation that destroys every local account. Remove Media3's opaque
        // jobs before secure connection state, so no queued request can survive sign-out.
        container.offlineDownloads.removeAll()
        container.offlineStorage.clearAll()
        // Every download and account goes; so does every checkpoint waiting to be sent.
        container.playbackProgressOutbox.clear()
        container.connectionRepository.signOutAll(profileId)
        boundSession = container.connectionRepository.captureSession()
        container.preferencesRepository.onboardingCompleted = false
        val notifications = container.appContext.getSystemService(android.app.NotificationManager::class.java)
        notifications.activeNotifications.filter { item ->
            app.reelstack.background.NotificationEvent.entries.any { it.channelId == item.notification.channelId }
        }.forEach { notifications.cancel(it.tag, it.id) }
        app.reelstack.widget.NowPlayingWidget.requestUpdate(container.appContext)
        // A fresh state clears details, favourites, requests and every account-scoped pane.
        _uiState.value = ReelstackUiState(
            sessionScope = boundSession,
            signingOut = true, connections = container.connectionRepository.list(),
            sessions = emptyList(), resume = emptyList(), recentMovies = emptyList(), loadedSources = emptySet(), lastUpdatedEpochMillis = null,
            recentSeries = emptyList(), upcoming = emptyList(), calendarUndated = emptyList(), calendarHidden = emptySet(), recentReleases = emptyList(),
            incoming = emptyList(), discover = emptyList(), discoverHub = app.reelstack.data.model.DiscoverHubState(), recommendations = emptyList(), activity = emptyList(),
            notificationsEnabled = container.preferencesRepository.notificationsEnabled,
            wifiOnly = container.preferencesRepository.wifiOnly,
            homeSections = container.preferencesRepository.visibleHomeSections,
            homeRowOrder = container.preferencesRepository.homeRowOrder,
            homeLayout = container.preferencesRepository.homeLayout,
        )
        signOutJob = viewModelScope.launch {
            // Wait for any in-flight disk write before erasing the offline account snapshot.
            pending.join()
            // An authentication write already executing on IO may finish despite cancellation.
            container.connectionRepository.signOutAll(profileId)
            boundSession = container.connectionRepository.captureSession()
            withContext(Dispatchers.IO) { container.mediaSnapshotStore.clear(); container.shelfAnswerCache.clear(); container.localPlaybackStore.clear() }
            _uiState.update { it.copy(sessionScope = boundSession, signingOut = false, showOnboarding = true) }
            features = newFeatures()
            features.profiles.loadProfiles()
        }
    }

    fun removeConnection(kind: ServiceKind) {
        if (!_uiState.value.canEditConnection(kind) || _uiState.value.requestingMediaIds.isNotEmpty()) return
        features.close()
        committingSession = true
        try {
            container.connectionRepository.get(kind).takeIf { it.baseUrl.isNotBlank() }?.let {
                container.offlineDownloads.removeScope(boundSession.profileId, it)
                container.offlineStorage.clearForConnection(boundSession.profileId, it)
                container.forgetUnsentProgress(boundSession.profileId, listOf(it))
            }
            container.connectionRepository.signOut(kind)
            if (container.connectionRepository.list().none { it.baseUrl.isNotBlank() }) {
                container.preferencesRepository.onboardingCompleted = false
            }
            container.mediaSnapshotStore.clear()
            container.shelfAnswerCache.clear()
            container.localPlaybackStore.clear()
        } finally { committingSession = false }
        synchronizeSession()
        _uiState.update { it.copy(snackbar = appString(R.string.notice_signed_out_of, kind.displayName)) }
    }

    fun returnedToApp() {
        synchronizeSession()
        features.home.returnedToApp()
        features.shelves.loadHomeShelves()
        if (_uiState.value.selectedTab == AppTab.DOWNLOADS) refreshOfflineDownloads()
    }

    fun refreshLiveData(userInitiated: Boolean = false) {
        features.home.refresh(userInitiated)
        features.shelves.loadHomeShelves(userInitiated)
    }

    fun setHomeLayout(layout: HomeLayout) {
        features.home.setLayout(layout)
        features.shelves.loadHomeShelves()
    }

    fun setHomeLibraries(kind: ServiceKind, choice: HomeLibraryChoice) {
        val connection = _uiState.value.connections.firstOrNull { it.kind == kind && it.baseUrl.isNotBlank() } ?: return
        features.home.setLibraries(connection, choice)
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbar = null) }

    fun clearLibraryCache() = features.library.clearLibraryCache()
    fun selectLibrarySource(source: ServiceKind) = features.library.selectLibrarySource(source)
    fun openLibraryChoices() = features.library.openLibraryChoices()
    fun closeLibraryChoices() = features.library.closeLibraryChoices()
    fun saveLibraryChoices(ids: Set<String>, shortcuts: List<String> = _uiState.value.libraryShortcuts.map { it.first },
        icons: Map<String, app.reelstack.data.model.LibraryIcon> = _uiState.value.libraryIcons) = features.library.saveLibraryChoices(ids, shortcuts, icons)
    fun browseLibrary(more: Boolean = false) = features.library.browseLibrary(more)
    fun libraryBack() = features.library.libraryBack()
    fun filterLibrary(filters: app.reelstack.data.model.LibraryFilters) = features.library.filterLibrary(filters)
    fun openLibraryEntry(id: String) = features.library.openLibraryEntry(id)
    fun openLibraryShortcut(id: String) = features.library.openLibraryShortcut(id)
    fun openSeasonShelf(season: app.reelstack.data.model.Season) = features.shelves.openSeasonShelf(season)
    fun openSmartShelf(id: String) = features.shelves.openSmartShelf(id)
    fun openSmartShelfEditor(id: String? = null, template: String? = null) = features.shelves.openSmartShelfEditor(id, template)
    fun updateSmartShelfDraft(draft: SmartShelf) = features.shelves.updateSmartShelfDraft(draft)
    fun applySmartShelfTemplate(template: String) = features.shelves.applySmartShelfTemplate(template)
    fun closeSmartShelfEditor() = features.shelves.closeSmartShelfEditor()
    fun saveSmartShelf() = features.shelves.saveSmartShelf()
    fun setSmartShelfSort(id: String, sort: app.reelstack.data.model.SmartShelfSort) = features.shelves.setSmartShelfSort(id, sort)
    fun deleteSmartShelf(id: String) = features.shelves.deleteSmartShelf(id)
    fun retryCatalogueFacets() = features.shelves.retryCatalogueFacets()
    fun openPerson(id: Int) = features.details.openPerson(id)
    fun openPersonTitle(id: String) = features.details.openPersonTitle(id)
    fun focusRequestTitle(request: app.reelstack.data.model.TrackedRequest) = features.details.focusRequestTitle(request)
    fun returnFromDetail() = features.details.returnFromDetail()
    suspend fun personTitles(person: app.reelstack.data.model.CastMember, source: ServiceKind) = features.details.personTitles(person, source)
    fun openPersonTitle(media: LibraryMedia) = features.details.openPersonTitle(media)
    fun openEpisodeDetail(media: LibraryMedia) = features.details.openEpisodeDetail(media)
    fun openRelatedDetails(id: String) = features.details.openRelatedDetails(id)
    fun openEpisodeSeries() = features.details.openEpisodeSeries()
    fun openLibraryDetails(id: String, clearHistory: Boolean = true) = features.details.openLibraryDetails(id, clearHistory)
    fun selectSeason(seasonId: String) = features.details.selectSeason(seasonId)
    fun openDiscoverDetails(id: String) = features.details.openDiscoverDetails(id)
    fun openRecommendationDetails(id: String) = features.details.openRecommendationDetails(id)
    fun openCalendarTitle(title: app.reelstack.data.model.CalendarTitle) = features.details.openCalendarTitle(title)
    fun openUpcomingDetails(id: String) = features.details.openUpcomingDetails(id)
    fun openIncomingDetails(id: String) = features.details.openIncomingDetails(id)
    fun openActivityDetails(id: String) = features.details.openActivityDetails(id)
    fun hideFromResume(id: String) = features.mediaActions.hideFromResume(id)
    fun setMediaFavourite(id: String, favourite: Boolean) = features.mediaActions.setMediaFavourite(id, favourite)
    fun setMediaPlayed(id: String, played: Boolean) = features.mediaActions.setMediaPlayed(id, played)
    fun clearMediaActionError() = features.mediaActions.clearMediaActionError()
    fun requestMedia(id: String) = features.requests.requestMedia(id)
    fun setRequestEdition(fourK: Boolean) = features.requests.setRequestEdition(fourK)
    fun setRequestProfile(id: Int) = features.requests.setRequestProfile(id)
    fun setRequestFolder(path: String) = features.requests.setRequestFolder(path)
    fun setRequestSeason(number: Int, checked: Boolean) = features.requests.setRequestSeason(number, checked)
    fun setRequestNotification(enabled: Boolean) = features.requests.setRequestNotification(enabled)
    fun confirmRequest() = features.requests.confirmRequest()
    fun setSeasonWatch(number: Int, enabled: Boolean) = features.requests.setSeasonWatch(number, enabled)
    fun setFollowNotification(key: String, enabled: Boolean) = features.requests.setFollowNotification(key, enabled)
    fun cancelTrackedRequest(key: String) = features.requests.cancelTrackedRequest(key)
    fun openRequestHistory() = features.requests.openRequestHistory()
    fun closeRequestHistory() = features.requests.closeRequestHistory()
    fun loadRequestHistory(more: Boolean) = features.requests.loadRequestHistory(more)
    fun refreshTrackedRequests(background: Boolean = false) = features.requests.refreshTrackedRequests(background)
    fun refreshOfflineDownloads() = features.offline.refreshOfflineDownloads()
    fun enqueueOfflineDownload(media: LibraryMedia, audioIndex: Int? = null,
        subtitleIndex: Int? = null,
        versionId: String? = null, sourceDetailKey: String = media.id) = features.offline.enqueueOfflineDownload(media, audioIndex, subtitleIndex, versionId, sourceDetailKey)
    fun pauseOfflineDownload(id: String) = features.offline.pauseOfflineDownload(id)
    fun resumeOfflineDownload(id: String) = features.offline.resumeOfflineDownload(id)
    fun retryOfflineDownload(id: String) = features.offline.retryOfflineDownload(id)
    fun removeOfflineDownload(id: String) = features.offline.removeOfflineDownload(id)
    fun pauseOfflineDownloads() = features.offline.pauseOfflineDownloads()
    fun resumeOfflineDownloads() = features.offline.resumeOfflineDownloads()
    fun loadKidsLibrary() = features.kids.loadKidsLibrary()
    fun closeKidsLauncherLibrary() = features.kids.closeLauncherLibrary()
    fun retryKidsLauncherLibrary() {
        _uiState.value.kidsLauncherLibrary?.let { features.kids.openLauncherLibrary(it.source, it.libraryId) }
    }
    fun openKidsSeries(media: LibraryMedia) = features.kids.openKidsSeries(media)
    fun selectKidsSeason(seasonId: String) = features.kids.selectKidsSeason(seasonId)
    fun closeKidsSeries() = features.kids.closeKidsSeries()
    fun openProfileSwitcher() = features.profiles.openProfileSwitcher()
    fun selectProfile(profile: app.reelstack.data.model.UserProfile) = features.profiles.selectProfile(profile)
    fun submitPin(pin: String, targetProfileId: String, isSetup: Boolean) {
        synchronizeSession()
        features.profiles.submitPin(pin, targetProfileId, isSetup)
    }
    fun requestPinSetup(targetProfileId: String = _uiState.value.activeProfileId) {
        synchronizeSession()
        features.profiles.requestPinSetup(targetProfileId)
    }
    fun disablePinProtection() {
        synchronizeSession()
        features.profiles.disablePinProtection()
    }
    fun recoverPinWithPassword(password: String, targetProfileId: String) = features.profiles.recoverPinWithPassword(password, targetProfileId)
    fun openAddProfile() = features.profiles.openAddProfile()
    fun addKidProfile(user: app.reelstack.data.network.PublicUser, password: String) {
        synchronizeSession()
        features.profiles.addKidProfile(user, password)
    }
    fun addKidProfileManual(username: String, password: String, serverUrl: String? = null) {
        synchronizeSession()
        features.profiles.addKidProfileManual(username, password, serverUrl)
    }
    fun deleteKidProfile(profile: app.reelstack.data.model.UserProfile) = features.profiles.deleteKidProfile(profile)
    fun cancelConnectionSetup() = features.connections.cancelConnectionSetup()
    fun openCombinedSetup() = features.connections.openCombinedSetup()
    fun discoverServers() = features.connections.discoverServers()
    fun pickDiscoveredServer(server: app.reelstack.data.network.DiscoveredServer) = features.connections.pickDiscoveredServer(server)
    fun confirmConnectionAddress(then: (() -> Unit)? = null) = features.connections.confirmConnectionAddress(then)
    fun pickLoginUser(user: app.reelstack.data.network.PublicUser) = features.connections.pickLoginUser(user)
    fun importSetupLink(value: String) = features.connections.importSetupLink(value)
    fun openSeerrAccount() = features.connections.openSeerrAccount()
    fun updateConnectionName(value: String) = features.connections.updateConnectionName(value)
    fun updateConnectionUrl(value: String) = features.connections.updateConnectionUrl(value)
    fun updateConnectionToken(value: String) = features.connections.updateConnectionToken(value)
    fun updateConnectionUserId(value: String) = features.connections.updateConnectionUserId(value)
    fun updateConnectionAlternateUrl(value: String) = features.connections.updateConnectionAlternateUrl(value)
    fun updateConnectionAuthMode(value: ConnectionAuthMode) = features.connections.updateConnectionAuthMode(value)
    fun updateConnectionUsername(value: String) = features.connections.updateConnectionUsername(value)
    fun updateConnectionPassword(value: String) = features.connections.updateConnectionPassword(value)
    fun updateCompanionLogin(enabled: Boolean, url: String) = features.connections.updateCompanionLogin(enabled, url)
    fun testAndSaveConnection() = features.connections.testAndSaveConnection()
    fun toggleCalendarFollow(details: ContentDetails) {
        synchronizeSession()
        features.homeActions.toggleCalendarFollow(details)
    }
    fun togglePlayback(sessionKey: String) = features.homeActions.togglePlayback(sessionKey)
    fun setNotifications(enabled: Boolean) = features.homeActions.setNotifications(enabled)
    fun setWifiOnly(enabled: Boolean) = features.homeActions.setWifiOnly(enabled)
    fun loadHomeLibraryViews() = features.homeActions.loadHomeLibraryViews()

    fun refreshDiscoverHub(force: Boolean = false) = features.discover.refresh(force)
    fun openDiscoverGrid(target: DiscoverGridTarget) = features.discover.openGrid(target)
    fun loadMoreDiscoverGrid() = features.discover.loadMore()
    fun closeDiscoverGrid() = features.discover.closeGrid()
    fun setSearchQuery(value: String) = features.search.setQuery(value)
    fun loadMoreSearchResults() = features.search.loadMore()
    fun setGlobalSearchQuery(value: String) = features.globalSearch.setQuery(value)
    fun loadMoreGlobalSearch() = features.globalSearch.loadMore()
    fun openSessionChannel() = features.home.openSessionChannel()
    fun closeSessionChannel() = features.home.closeSessionChannel()
    suspend fun refreshPlayback(sources: Set<ServiceKind> = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) =
        features.home.refreshPlayback(sources)
    fun retryIncompleteHomeFeed() = features.home.retryIncompleteFeed()
    fun showHomeShelves() = features.shelves.loadHomeShelves()

    override fun onCleared() {
        features.close()
        super.onCleared()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ReelstackViewModel::class.java))
            return ReelstackViewModel(container) as T
        }
    }
    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)
    private fun isTelevision(): Boolean =
        (container.appContext.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
}
