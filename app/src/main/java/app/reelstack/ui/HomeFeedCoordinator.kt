package app.reelstack.ui

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns Home's server feed and the lightweight playback notification channel.
 *
 * `ReelstackViewModel` remains the composition root and exposes one atomic screen snapshot. The feed's jobs,
 * retry clock and stale-while-revalidate cache belong to this session owner: Home data no longer needs to know about
 * navigation, PIN prompts or connection-editor state.
 */
internal class HomeFeedCoordinator(
    private val appContext: android.content.Context,
    private val connectionRepository: app.reelstack.data.repository.ConnectionRepository,
    private val localPlaybackStore: app.reelstack.data.repository.LocalPlaybackStore,
    private val mediaFingerprint: (List<ServiceConnection>) -> String,
    private val mediaSnapshotStore: app.reelstack.data.repository.MediaSnapshotStore,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val preferencesRepository: app.reelstack.data.repository.AppPreferencesRepository,
    private val sessionSocket: app.reelstack.data.network.JellyfinSessionSocket,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
    private val refreshAccounts: () -> Unit,
    private val refreshTrackedRequests: () -> Unit,
    private val openLibraryDetails: (String) -> Unit,
    private val isSigningOut: () -> Boolean,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var refreshJob: Job? = null
    private var cacheLoadJob: Job? = null
    private var playbackJob: Job? = null
    private val playbackLock = Mutex()
    private var playbackRevision = 0L
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
            sessionSocket.connect(
                connection = connection,
                onChanged = {
                    if (playbackJob?.isActive != true) {
                        playbackJob = scope.launch { refreshPlayback(setOf(ServiceKind.JELLYFIN)) }
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

    suspend fun refreshPlayback(sources: Set<ServiceKind> = MEDIA_SERVERS) = playbackLock.withLock {
        val owner = connectionRepository.captureSession()
        val before = readState()
        val connections = before.connections
        val requested = sources.intersect(MEDIA_SERVERS)
        if (connections.none { it.token.isNotBlank() && it.kind in requested }) return@withLock
        val result = withContext(ioDispatcher) {
            mediaSyncRepository.refreshPlaybackResult(connections, requested)
        }
        playbackRevision++
        // Never put a response from an account that has since signed out back on screen.
        updateState { current ->
            when {
                !connectionRepository.isCurrent(owner) || current.connections != connections -> current
                else -> current.copy(
                    sessions = mergePlaybackSessions(current.sessions, result.sessions, requested),
                    playbackUnavailableSources = (current.playbackUnavailableSources - requested) + result.unavailableSources,
                )
            }
        }
        // Session polling/push used to update only "Playing now". Other devices' stops and
        // episode changes must also update Continue Watching and Next up while Home stays open.
        val current = readState()
        if (connectionRepository.isCurrent(owner) && current.connections == connections &&
            personalProgressChanged(before.sessions, current.sessions, requested - result.unavailableSources)) refresh()
    }

    fun localResume(
        items: List<LibraryMedia>,
        connections: List<ServiceConnection>,
        freshSources: Set<ServiceKind> = emptySet(),
    ): List<LibraryMedia> = connections
        .filter { it.kind in MEDIA_SERVERS && it.token.isNotBlank() }
        .fold(items) { result, account -> localPlaybackStore.merge(account, result, account.kind in freshSources) }

    fun localNextUp(
        items: List<LibraryMedia>,
        connections: List<ServiceConnection>,
        freshSources: Set<ServiceKind> = emptySet(),
    ): List<LibraryMedia> = connections
        .filter { it.kind in MEDIA_SERVERS && it.token.isNotBlank() }
        .fold(items) { result, account -> localPlaybackStore.nextUp(account, result, account.kind in freshSources) }

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
        val owner = connectionRepository.captureSession()
        val fingerprint = mediaFingerprint(seedConnections)
        if (seedConnections.none { it.baseUrl.isNotBlank() && it.token.isNotBlank() }) return
        cancelCacheLoad()
        cacheLoadJob = scope.launch(ioDispatcher) {
            val cached = runCatching {
                mediaSnapshotStore.read(fingerprint)
            }.getOrNull()
            if (!isActive || !connectionRepository.isCurrent(owner) || cached == null) return@launch
            updateState { current ->
                if (!connectionRepository.isCurrent(owner) || current.connections != seedConnections) return@updateState current
                val configuredKinds = seedConnections.filter { it.baseUrl.isNotBlank() }
                    .mapTo(mutableSetOf()) { it.kind }
                val hasMediaServer = configuredKinds.any { it in MEDIA_SERVERS }
                val hasQueueService = configuredKinds.any { it in QUEUE_SERVERS }
                val hasSeerr = ServiceKind.SEERR in configuredKinds
                current.copy(
                    // Playback is live-only; a disk snapshot must not erase a fresh session list.
                    sessions = current.sessions,
                    resume = if (hasMediaServer) localResume(cached.resume, current.connections) else current.resume,
                    nextUp = if (hasMediaServer) localNextUp(cached.nextUp, current.connections) else current.nextUp,
                    favourites = if (hasMediaServer) cached.favourites else current.favourites,
                    recentMovies = if (hasMediaServer) cached.recentMovies else current.recentMovies,
                    recentSeries = if (hasMediaServer) cached.recentSeries else current.recentSeries,
                    // Personal calendar interests are shown only after this session is verified.
                    upcoming = emptyList(),
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
        preferencesRepository.homeLayout = layout
        updateState { it.copy(homeLayout = layout) }
        val newlyFetched = layout.order.any { key ->
            key.kind.skippedWhenHidden && layout.isVisible(key) && !before.isVisible(key)
        } || layout.isVisible(RECOMMENDATIONS_ROW) != before.isVisible(RECOMMENDATIONS_ROW)
        if (newlyFetched) restartRefresh()
    }

    fun setLibraries(connection: ServiceConnection, choice: HomeLibraryChoice) {
        preferencesRepository.setHomeLibraries(connection, choice)
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

        val refreshFingerprint = mediaFingerprint(state.connections)
        val refreshOwner = connectionRepository.captureSession()
        val revisionAtStart = playbackRevision
        lastFeedAttemptMillis = android.os.SystemClock.elapsedRealtime()
        updateState { it.copy(isRefreshing = true) }
        refreshJob = scope.launch {
            val started = System.nanoTime()
            val firstRow = java.util.concurrent.atomic.AtomicBoolean(true)
            val outcome = attempt {
                val snapshot = withContext(ioDispatcher) {
                    val layout = state.effectiveHomeLayout
                    mediaSyncRepository.refresh(
                        connections = state.connections,
                        includeRecommendations = layout.isVisible(HomeRowKey(HomeRowKind.RECOMMENDATIONS)),
                        homePlan = state.homeFetchPlan(),
                        calendarScope = refreshOwner.profileId, forceCalendar = userInitiated,
                        onCalendarReady = { update -> updateState { current ->
                            if (!connectionRepository.isCurrent(refreshOwner) || mediaFingerprint(current.connections) != refreshFingerprint) current
                            else current.copy(upcoming = update.upcoming, calendarUndated = update.undated,
                                calendarHidden = update.hidden, upcomingError = update.error?.text(appContext))
                        } },
                        onLibraryReady = libraryReady@ { update ->
                            if (!connectionRepository.isCurrent(refreshOwner)) return@libraryReady
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
            if (!isActive || !connectionRepository.isCurrent(refreshOwner) || mediaFingerprint(readState().connections) != refreshFingerprint) return@launch
            if (snapshot.switchedToAlternate.isNotEmpty()) {
                attempt {
                    withContext(ioDispatcher) {
                        snapshot.switchedToAlternate.forEach(connectionRepository::promoteAlternate)
                    }
                }
            }
            val refreshedConnections = if (snapshot.switchedToAlternate.isEmpty()) null
            else runCatching { connectionRepository.list() }.getOrNull()
            updateState { current -> applySnapshot(current, snapshot, refreshedConnections, userInitiated,
                keepLivePlayback = playbackRevision != revisionAtStart) }
            retryAttempts = if (snapshot.errors.keys.any { it in MEDIA_SERVERS } ||
                snapshot.warnings.keys.any { it in MEDIA_SERVERS }) retryAttempts else 0
            // The rows are on screen first; the copy for the next start follows. A disk-full cache
            // is not a reason to fail a live refresh.
            if (snapshot.successfulServices.isNotEmpty()) {
                attempt {
                    withContext(ioDispatcher) {
                        mediaSnapshotStore.save(snapshot, refreshFingerprint)
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
            if (mediaFingerprint(current.connections) != refreshFingerprint) current else {
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
        val demoContext = app.reelstack.localization.AppLanguages.wrap(appContext)
        val allowDemo = state.connections.none { it.baseUrl.isNotBlank() }
        fun <T> content(demo: () -> List<T>): List<T> = if (allowDemo) demo() else emptyList()
        updateState {
            it.copy(
                sessions = content { demoSessions(demoContext) },
                resume = content { demoResume(demoContext) },
                nextUp = content { demoNextUp(demoContext) },
                favourites = content { demoFavourites(demoContext) },
                recentMovies = content { demoRecentMovies(demoContext) },
                recentSeries = content { demoRecentSeries(demoContext) },
                upcoming = content { demoUpcoming(demoContext) },
                recentReleases = content { demoRecentReleases(demoContext) },
                incoming = content { demoIncoming(demoContext) },
                discover = content { demoDiscover(demoContext) },
                recommendations = content { demoRecommendations(demoContext) },
                activity = content(::demoActivity),
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
        keepLivePlayback: Boolean,
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
            sentences.joinToString(" · ") { it.text(appContext) }
        }
        return current.copy(
            adminView = snapshot.adminView,
            sessions = if (keepLivePlayback) current.sessions else snapshot.sessions,
            playbackUnavailableSources = if (keepLivePlayback) current.playbackUnavailableSources
                else snapshot.errors.keys.intersect(MEDIA_SERVERS),
            resume = localResume(snapshot.resume, connectionsNow, snapshot.successfulServices - snapshot.warnings.keys),
            nextUp = localNextUp(snapshot.nextUp, connectionsNow, snapshot.successfulServices - snapshot.warnings.keys),
            favourites = snapshot.favourites,
            libraryShortcuts = current.copy(connections = connectionsNow).libraryConnection
                ?.let(preferencesRepository::libraryShortcuts).orEmpty(),
            libraryIcons = current.copy(connections = connectionsNow).libraryConnection
                ?.let(preferencesRepository::libraryIcons).orEmpty(),
            recentMovies = snapshot.recentMovies,
            recentSeries = snapshot.recentSeries,
            recentReleases = snapshot.recentReleases,
            upcoming = snapshot.upcoming,
            calendarUndated = snapshot.calendarUndated,
            calendarHidden = snapshot.calendarHidden,
            recentReleasesError = snapshot.recentReleasesError?.text(appContext),
            upcomingError = snapshot.upcomingError?.text(appContext),
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
                        detail = snapshot.errors.getValue(connection.kind).text(appContext),
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
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)

    private fun appQuantityString(
        @androidx.annotation.PluralsRes resId: Int,
        quantity: Int,
        vararg args: Any,
    ): String = app.reelstack.localization.AppLanguages.wrap(appContext)
        .resources.getQuantityString(resId, quantity, *args)

    private companion object {
        val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
        val RECOMMENDATIONS_ROW = HomeRowKey(HomeRowKind.RECOMMENDATIONS)
        val QUEUE_SERVERS = setOf(ServiceKind.SEERR)
    }
}
