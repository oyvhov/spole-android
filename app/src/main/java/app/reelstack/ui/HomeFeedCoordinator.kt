package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.HomeFetchPlan
import app.reelstack.data.model.HomeLayout
import app.reelstack.data.model.HomeLibraryChoice
import app.reelstack.data.model.HomeRowKey
import app.reelstack.data.model.HomeRowKind
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns Home's server feed and the lightweight playback notification channel.
 *
 * `ReelstackViewModel` remains the composition root while Library, Profile and Discover still
 * share its state object. Keeping the feed's jobs, retry clock and stale-while-revalidate cache
 * together here makes that temporary boundary explicit: Home data no longer needs to know about
 * navigation, PIN prompts or connection-editor state.
 */
internal class HomeFeedCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
    private val refreshAccounts: () -> Unit,
    private val refreshTrackedRequests: () -> Unit,
    private val openLibraryDetails: (String) -> Unit,
    private val isSigningOut: () -> Boolean,
) {
    private var refreshJob: Job? = null
    private var cacheLoadJob: Job? = null
    private var playbackJob: Job? = null
    private var sessionChannel: app.reelstack.data.network.JellyfinSessionSocket.Connection? = null
    private val sessionChannelState = MutableStateFlow(false)
    private var lastFeedAttemptMillis = -60_000L
    /** Automatic retries since the media feed was last complete; each one waits longer. */
    private var retryAttempts = 0

    /** Whether Jellyfin is currently pushing playback-change notifications. */
    val sessionChannelLive: StateFlow<Boolean> = sessionChannelState.asStateFlow()

    fun cancelRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun cancelCacheLoad() {
        cacheLoadJob?.cancel()
        cacheLoadJob = null
    }

    fun resetRetryClock() {
        lastFeedAttemptMillis = -60_000L
        retryAttempts = 0
    }

    /** Stops in-flight Home work without changing the UI-owned socket lifecycle. */
    fun cancelActiveWork() {
        cancelCacheLoad()
        cancelRefresh()
        playbackJob?.cancel()
        playbackJob = null
    }

    /** A notification channel is authenticated as one profile and may never cross that boundary. */
    fun resetForProfileChange() {
        closeSessionChannel()
        cancelActiveWork()
    }

    fun cancel() {
        resetForProfileChange()
    }

    /** Opens Jellyfin's optional notification channel. It is only a refresh doorbell. */
    fun openSessionChannel() {
        if (sessionChannel != null) return
        val connection = readState().connections.firstOrNull {
            it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank()
        } ?: return
        sessionChannel = runCatching {
            container.sessionSocket.connect(
                connection = connection,
                onChanged = {
                    if (playbackJob?.isActive != true) {
                        playbackJob = scope.launch { refreshPlayback() }
                    }
                },
                onLost = {
                    sessionChannel = null
                    sessionChannelState.value = false
                },
            )
        }.getOrNull()
        sessionChannelState.value = sessionChannel != null
    }

    fun closeSessionChannel() {
        sessionChannel?.let { runCatching { it.close() } }
        sessionChannel = null
        sessionChannelState.value = false
    }

    suspend fun refreshPlayback() {
        if (refreshJob?.isActive == true) return
        val connections = readState().connections
        if (connections.none { it.token.isNotBlank() && it.kind in MEDIA_SERVERS }) return
        val sessions = withContext(Dispatchers.IO) {
            container.mediaSyncRepository.refreshPlayback(connections)
        }
        // Never put a response from an account that has since signed out back on screen.
        updateState { current ->
            when {
                current.connections != connections || refreshJob?.isActive == true -> current
                current.sessions == sessions -> current
                else -> current.copy(sessions = sessions)
            }
        }
    }

    fun localResume(
        items: List<LibraryMedia>,
        connections: List<ServiceConnection>,
    ): List<LibraryMedia> = connections
        .filter { it.kind in MEDIA_SERVERS && it.token.isNotBlank() }
        .fold(items) { result, account -> container.localPlaybackStore.merge(account, result) }

    fun localNextUp(
        items: List<LibraryMedia>,
        connections: List<ServiceConnection>,
    ): List<LibraryMedia> = connections
        .filter { it.kind in MEDIA_SERVERS && it.token.isNotBlank() }
        .fold(items) { result, account -> container.localPlaybackStore.nextUp(account, result) }

    /** Returning from local playback refreshes personal progress and the next episode. */
    fun returnedToApp() {
        updateState { it.copy(resume = localResume(it.resume, it.connections)) }
        refresh()
        val key = (readState().activeSheet as? AppSheet.TitleDetails)?.key
        if (key != null && (key.startsWith("jellyfin-") || key.startsWith("emby-"))) {
            openLibraryDetails(key)
        }
    }

    /** Hydrates a profile-scoped feed before the network response arrives. */
    fun hydrateCachedFeed() {
        val seedConnections = readState().connections
        if (seedConnections.none { it.baseUrl.isNotBlank() && it.token.isNotBlank() }) return
        cancelCacheLoad()
        cacheLoadJob = scope.launch(Dispatchers.IO) {
            val cached = runCatching {
                container.mediaSnapshotStore.read(container.mediaFingerprint(seedConnections))
            }.getOrNull()
            if (!isActive || cached == null) return@launch
            updateState { current ->
                if (current.connections != seedConnections) return@updateState current
                val configuredKinds = seedConnections.filter { it.baseUrl.isNotBlank() }
                    .mapTo(mutableSetOf()) { it.kind }
                val hasMediaServer = configuredKinds.any { it in MEDIA_SERVERS }
                val hasQueueService = configuredKinds.any { it in QUEUE_SERVERS }
                val hasSeerr = ServiceKind.SEERR in configuredKinds
                current.copy(
                    sessions = if (hasMediaServer) cached.sessions else current.sessions,
                    resume = if (hasMediaServer) localResume(cached.resume, current.connections) else current.resume,
                    nextUp = if (hasMediaServer) localNextUp(cached.nextUp, current.connections) else current.nextUp,
                    favourites = if (hasMediaServer) cached.favourites else current.favourites,
                    recentMovies = if (hasMediaServer) cached.recentMovies else current.recentMovies,
                    recentSeries = if (hasMediaServer) cached.recentSeries else current.recentSeries,
                    upcoming = if (hasQueueService) cached.upcoming else current.upcoming,
                    recentReleases = if (hasQueueService) cached.recentReleases else current.recentReleases,
                    incoming = if (hasQueueService) cached.incoming else current.incoming,
                    discover = if (hasSeerr) cached.discover else current.discover,
                    recommendations = if (hasSeerr) cached.recommendations else current.recommendations,
                    activity = if (hasQueueService || hasSeerr) cached.activity else current.activity,
                    lastUpdatedEpochMillis = cached.refreshedAtEpochMillis,
                    hasCachedData = true,
                    // What the cache covers counts as loaded: a refresh then updates those rows in
                    // place instead of turning an empty row into a skeleton and back.
                    loadedSources = current.loadedSources + configuredKinds.filter { kind ->
                        (kind in MEDIA_SERVERS && hasMediaServer) || (kind in QUEUE_SERVERS && hasQueueService) ||
                            (kind == ServiceKind.SEERR && hasSeerr)
                    },
                )
            }
        }
    }

    fun retryIncompleteFeed() {
        val state = readState()
        if (shouldRetryHomeFeed(
                state.failedServices,
                state.serviceWarnings.keys,
                state.isRefreshing,
                android.os.SystemClock.elapsedRealtime() - lastFeedAttemptMillis,
                retryAttempts,
            )
        ) {
            retryAttempts++
            refresh()
        }
    }

    /**
     * Saves a new Home layout. A row that is shown again, or a different library choice, needs
     * data the last refresh did not fetch, so those changes start a new one straight away.
     */
    fun setLayout(layout: HomeLayout) {
        val before = readState().effectiveHomeLayout
        container.preferencesRepository.homeLayout = layout
        updateState { it.copy(homeLayout = layout) }
        val newlyFetched = layout.order.any { key ->
            key.kind.skippedWhenHidden && layout.isVisible(key) && !before.isVisible(key)
        } || layout.isVisible(RECOMMENDATIONS_ROW) != before.isVisible(RECOMMENDATIONS_ROW)
        if (newlyFetched) restartRefresh()
    }

    fun setLibraries(connection: ServiceConnection, choice: HomeLibraryChoice) {
        container.preferencesRepository.setHomeLibraries(connection, choice)
        updateState { it.copy(homeLibraries = it.homeLibraries + (connection.kind to choice)) }
        restartRefresh()
    }

    /** A refresh already running asks for the old rows; let the new one replace it. */
    private fun restartRefresh() {
        cancelRefresh()
        refresh()
    }

    fun refresh(userInitiated: Boolean = false) {
        val state = readState()
        if (isSigningOut() || (state.showOnboarding && state.configuredCount == 0)) return
        // Somebody asked: the automatic back-off starts again from its shortest wait.
        if (userInitiated) retryAttempts = 0
        refreshAccounts()
        refreshTrackedRequests()
        if (refreshJob?.isActive == true) return
        val configured = state.connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        if (configured.isEmpty()) {
            showDisconnectedHome(state, userInitiated)
            return
        }

        val refreshFingerprint = container.mediaFingerprint(state.connections)
        lastFeedAttemptMillis = android.os.SystemClock.elapsedRealtime()
        updateState { it.copy(isRefreshing = true) }
        refreshJob = scope.launch {
            val started = System.nanoTime()
            val firstRow = java.util.concurrent.atomic.AtomicBoolean(true)
            val outcome = attempt {
                val snapshot = withContext(Dispatchers.IO) {
                    val layout = readState().effectiveHomeLayout
                    container.mediaSyncRepository.refresh(
                        connections = readState().connections,
                        includeRecommendations = layout.isVisible(HomeRowKey(HomeRowKind.RECOMMENDATIONS)),
                        homePlan = HomeFetchPlan(layout, container.homeLibraries(readState().connections)),
                        onLibraryReady = { update ->
                            if (firstRow.getAndSet(false)) app.reelstack.data.network.PerfLog.milestone("home first-rows ${update.source}", started)
                            updateLibraryRow(refreshFingerprint, update)
                        },
                    )
                }
                app.reelstack.data.network.PerfLog.milestone("home all-rows", started)
                snapshot
            }
            val snapshot = outcome.getOrElse {
                updateState {
                    it.copy(isRefreshing = false, snackbar = appString(R.string.error_feed_refresh))
                }
                return@launch
            }
            if (!isActive || container.mediaFingerprint(readState().connections) != refreshFingerprint) return@launch
            if (snapshot.switchedToAlternate.isNotEmpty()) {
                attempt {
                    withContext(Dispatchers.IO) {
                        snapshot.switchedToAlternate.forEach(container.connectionRepository::promoteAlternate)
                    }
                }
            }
            val refreshedConnections = if (snapshot.switchedToAlternate.isEmpty()) null
            else runCatching { container.connectionRepository.list() }.getOrNull()
            updateState { current -> applySnapshot(current, snapshot, refreshedConnections, userInitiated) }
            retryAttempts = if (snapshot.errors.keys.any { it in MEDIA_SERVERS } ||
                snapshot.warnings.keys.any { it in MEDIA_SERVERS }) retryAttempts else 0
            // The rows are on screen first; the copy for the next start follows. A disk-full cache
            // is not a reason to fail a live refresh.
            if (snapshot.successfulServices.isNotEmpty()) {
                attempt {
                    withContext(Dispatchers.IO) {
                        container.mediaSnapshotStore.save(snapshot, refreshFingerprint)
                    }
                }
            }
        }
    }

    private fun updateLibraryRow(
        refreshFingerprint: String,
        update: app.reelstack.data.repository.LibraryFeedUpdate,
    ) {
        updateState { current ->
            if (container.mediaFingerprint(current.connections) != refreshFingerprint) current else {
                // Merged in the order the final snapshot uses. Appending the fresh server after the
                // other one flipped the order at every update, and the tablet and TV hero, which
                // picks from these lists, changed title while it was on screen.
                val order = current.connections.map { it.kind }.filter { it in MEDIA_SERVERS }.distinct()
                fun replace(items: List<LibraryMedia>, fresh: List<LibraryMedia>): List<LibraryMedia> {
                    val bySource = (items.filterNot { it.source == update.source } + fresh).groupBy { it.source }
                    return app.reelstack.data.repository.mergeHomeRows(
                        order.mapNotNull(bySource::get) + bySource.filterKeys { it !in order }.values)
                }
                current.copy(
                    resume = localResume(replace(current.resume, update.resume), current.connections),
                    nextUp = localNextUp(replace(current.nextUp, update.nextUp), current.connections),
                    recentMovies = replace(current.recentMovies, update.recentMovies),
                    recentSeries = replace(current.recentSeries, update.recentSeries),
                    favourites = replace(current.favourites, update.favourites),
                    liveLibrary = true,
                    loadedSources = current.loadedSources + update.source,
                )
            }
        }
    }

    private fun showDisconnectedHome(state: ReelstackUiState, userInitiated: Boolean) {
        if (state.isKidMode) {
            updateState { it.copy(isRefreshing = false, kidsLibraryError = true) }
            return
        }
        val unreadable = state.connections.filter {
            it.baseUrl.isNotBlank() && it.state == ConnectionState.ERROR
        }
        updateState {
            it.copy(
                sessions = demoSessions(),
                resume = demoResume(),
                nextUp = demoNextUp(),
                favourites = demoFavourites(),
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
                snackbar = when {
                    unreadable.isNotEmpty() -> appString(R.string.error_login_unreadable)
                    userInitiated -> appString(R.string.notice_connect_to_sync)
                    else -> it.snackbar
                },
            )
        }
    }

    private fun applySnapshot(
        current: ReelstackUiState,
        snapshot: app.reelstack.data.repository.MediaSyncSnapshot,
        refreshedConnections: List<ServiceConnection>?,
        userInitiated: Boolean,
    ): ReelstackUiState {
        val connectionsNow = refreshedConnections ?: current.connections
        val configuredKinds = connectionsNow.filter { it.baseUrl.isNotBlank() }
            .mapTo(mutableSetOf()) { it.kind }
        val configuredMedia = configuredKinds.intersect(MEDIA_SERVERS)
        val configuredQueue = configuredKinds.intersect(QUEUE_SERVERS)
        val mediaLive = snapshot.successfulServices.any { it in configuredMedia }
        val queueLive = snapshot.successfulServices.any { it in configuredQueue }
        val seerrLive = ServiceKind.SEERR in snapshot.successfulServices
        val activityLive = queueLive || seerrLive
        val anySuccess = snapshot.successfulServices.isNotEmpty()
        val serviceWarnings = snapshot.warnings.mapValues { (_, sentences) ->
            sentences.joinToString(" · ") { it.text(container.appContext) }
        }
        return current.copy(
            adminView = snapshot.adminView,
            sessions = snapshot.sessions,
            resume = localResume(snapshot.resume, connectionsNow),
            nextUp = localNextUp(snapshot.nextUp, connectionsNow),
            favourites = snapshot.favourites,
            libraryShortcuts = current.copy(connections = connectionsNow).libraryConnection
                ?.let(container.preferencesRepository::libraryShortcuts).orEmpty(),
            libraryIcons = current.copy(connections = connectionsNow).libraryConnection
                ?.let(container.preferencesRepository::libraryIcons).orEmpty(),
            recentMovies = snapshot.recentMovies,
            recentSeries = snapshot.recentSeries,
            recentReleases = snapshot.recentReleases,
            upcoming = snapshot.upcoming,
            recentReleasesError = snapshot.recentReleasesError?.text(container.appContext),
            upcomingError = snapshot.upcomingError?.text(container.appContext),
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
                    connection.baseUrl.isBlank() -> connection.copy(
                        state = ConnectionState.DEMO,
                        detail = appString(R.string.connection_detail_demo),
                    )
                    connection.kind in snapshot.errors -> connection.copy(
                        state = ConnectionState.ERROR,
                        detail = snapshot.errors.getValue(connection.kind).text(container.appContext),
                    )
                    connection.kind in snapshot.successfulServices -> connection.copy(
                        state = ConnectionState.CONNECTED,
                        detail = when {
                            connection.kind in snapshot.switchedToAlternate ->
                                appString(R.string.connection_active_switched_alternate)
                            else -> serviceWarnings[connection.kind]?.let {
                                appString(R.string.connection_connected_with_warning, it)
                            } ?: appString(R.string.connection_active_updated_now)
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
            lastUpdatedEpochMillis = if (anySuccess) snapshot.refreshedAt.toEpochMilli()
            else current.lastUpdatedEpochMillis,
            hasCachedData = !anySuccess && current.hasCachedData,
            // A service that answered, or failed with a message of its own, has had its first load.
            loadedSources = current.loadedSources + snapshot.successfulServices + snapshot.errors.keys,
            failedServices = snapshot.errors.keys,
            serviceWarnings = serviceWarnings,
            snackbar = if (userInitiated) {
                when {
                    snapshot.errors.isNotEmpty() -> appQuantityString(
                        R.plurals.notice_sync_services_check,
                        snapshot.errors.size,
                        snapshot.errors.size,
                    )
                    snapshot.warnings.isNotEmpty() -> appString(R.string.notice_sync_warnings)
                    else -> appString(R.string.notice_sync_all_updated)
                }
            } else current.snackbar,
        )
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)

    private fun appQuantityString(
        @androidx.annotation.PluralsRes resId: Int,
        quantity: Int,
        vararg args: Any,
    ): String = app.reelstack.localization.AppLanguages.wrap(container.appContext)
        .resources.getQuantityString(resId, quantity, *args)

    private companion object {
        val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
        val RECOMMENDATIONS_ROW = HomeRowKey(HomeRowKind.RECOMMENDATIONS)
        val QUEUE_SERVERS = setOf(ServiceKind.RADARR, ServiceKind.SONARR)
    }
}
