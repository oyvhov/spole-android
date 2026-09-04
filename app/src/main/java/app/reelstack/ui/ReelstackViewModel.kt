package app.reelstack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.EndpointValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab { HOME, DISCOVER, ACTIVITY, SETTINGS }

sealed interface AppSheet {
    data object ServerPicker : AppSheet
    data object SessionDetails : AppSheet
    data class MediaDetails(val mediaId: String) : AppSheet
    data class LibraryDetails(val mediaId: String) : AppSheet
    data class ConnectionEditor(val kind: ServiceKind) : AppSheet
}

data class ConnectionDraft(
    val kind: ServiceKind,
    val name: String,
    val url: String,
    val token: String,
    val userId: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val warning: String? = null,
)

data class ReelstackUiState(
    val selectedTab: AppTab = AppTab.HOME,
    val selectedServer: ServiceKind = ServiceKind.JELLYFIN,
    val activeSheet: AppSheet? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val session: PlaybackSession? = demoSession(),
    val continueWatching: List<LibraryMedia> = demoContinueWatching(),
    val recentlyAdded: List<LibraryMedia> = demoRecentlyAdded(),
    val incoming: List<IncomingMedia> = demoIncoming(),
    val discover: List<DiscoverMedia> = demoDiscover(),
    val activity: List<ActivityEvent> = demoActivity(),
    val searchQuery: String = "",
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
    val requestingMediaIds: Set<String> = emptySet(),
    val playbackControlPending: Boolean = false,
    val hasCachedData: Boolean = false,
    val snackbar: String? = null,
) {
    val visibleDiscover: List<DiscoverMedia>
        get() = if (searchQuery.isBlank()) discover else discover.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }

    val selectedConnection: ServiceConnection?
        get() = connections.firstOrNull { it.kind == selectedServer }

    val configuredCount: Int
        get() = connections.count { it.baseUrl.isNotBlank() }

    val onlineCount: Int
        get() = connections.count { it.state == ConnectionState.CONNECTED }

    val syncSummary: String
        get() = when {
            isRefreshing -> "Refreshing your media stack…"
            configuredCount == 0 -> "Preview mode · connect a service when you're ready."
            onlineCount == 0 && hasCachedData -> "Services need attention · showing cached data."
            onlineCount == 0 -> "Your services need attention."
            failedServices.isNotEmpty() -> "$onlineCount live · ${failedServices.size} need attention."
            else -> "Live data · updated just now."
        }
}

class ReelstackViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState(container))
    val uiState: StateFlow<ReelstackUiState> = _uiState.asStateFlow()

    var connectionDraft = MutableStateFlow<ConnectionDraft?>(null)
        private set

    private var refreshJob: Job? = null

    init {
        refreshLiveData()
    }

    fun selectTab(tab: AppTab) = _uiState.update { it.copy(selectedTab = tab, activeSheet = null) }

    fun openSheet(sheet: AppSheet) {
        if (sheet is AppSheet.ConnectionEditor) {
            val existing = _uiState.value.connections.first { it.kind == sheet.kind }
            connectionDraft.value = ConnectionDraft(
                kind = existing.kind,
                name = existing.name,
                url = existing.baseUrl,
                token = existing.token,
                userId = existing.userId,
                warning = existing.baseUrl.takeIf(String::isNotBlank)?.let {
                    if (EndpointValidator.isCleartext(it)) "HTTP is unencrypted. Prefer HTTPS outside your trusted LAN." else null
                },
            )
        }
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = null) }
        connectionDraft.value = null
    }

    fun selectServer(kind: ServiceKind) {
        container.preferencesRepository.selectedServer = kind
        _uiState.update { it.copy(selectedServer = kind, activeSheet = null) }
        refreshLiveData()
    }

    fun togglePlayback() {
        val state = _uiState.value
        val session = state.session ?: return
        if (!state.liveSession) {
            _uiState.update { it.copy(session = session.copy(paused = !session.paused)) }
            return
        }
        if (state.playbackControlPending) return
        val targetPaused = !session.paused
        _uiState.update { it.copy(playbackControlPending = true) }
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
                    session = if (result.isSuccess) current.session?.copy(paused = targetPaused) else current.session,
                    playbackControlPending = false,
                    snackbar = if (result.isSuccess) {
                        if (targetPaused) "Playback paused" else "Playback resumed"
                    } else {
                        "The media server could not change playback"
                    },
                )
            }
        }
    }

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }

    fun requestMedia(id: String) {
        val state = _uiState.value
        val media = state.discover.firstOrNull { it.id == id } ?: return
        if (media.inLibrary || media.requested || id in state.requestingMediaIds) return
        val seerr = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }

        if (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null) {
            _uiState.update {
                it.copy(
                    discover = it.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                    snackbar = "Demo request saved locally · connect Seerr to send it",
                )
            }
            return
        }

        _uiState.update { it.copy(requestingMediaIds = it.requestingMediaIds + id) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.request(seerr, media) }
            }
            _uiState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        discover = current.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        activity = listOf(
                            ActivityEvent(
                                id = "seerr-request-${media.id}",
                                title = media.title,
                                detail = "Sent to Seerr",
                                time = "Just now",
                                source = ServiceKind.SEERR,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Request sent to Seerr",
                    )
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Seerr could not accept this request",
                    )
                }
            }
        }
    }

    fun refreshLiveData(userInitiated: Boolean = false) {
        if (refreshJob?.isActive == true) return
        val state = _uiState.value
        val configured = state.connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        if (configured.isEmpty()) {
            _uiState.update {
                it.copy(
                    session = demoSession(),
                    continueWatching = demoContinueWatching(),
                    recentlyAdded = demoRecentlyAdded(),
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
                    snackbar = if (userInitiated) "Connect a service to start live sync" else it.snackbar,
                )
            }
            return
        }

        _uiState.update { it.copy(isRefreshing = true) }
        refreshJob = viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                container.mediaSyncRepository.refresh(
                    connections = _uiState.value.connections,
                    selectedServer = _uiState.value.selectedServer,
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
                    session = when {
                        configuredMedia.isEmpty() -> demoSession()
                        mediaLive -> snapshot.session
                        current.liveSession || current.hasCachedData -> current.session
                        else -> null
                    },
                    continueWatching = when {
                        configuredMedia.isEmpty() -> demoContinueWatching()
                        mediaLive -> snapshot.continueWatching
                        current.liveLibrary || current.hasCachedData -> current.continueWatching
                        else -> emptyList()
                    },
                    recentlyAdded = when {
                        configuredMedia.isEmpty() -> demoRecentlyAdded()
                        mediaLive -> snapshot.recentlyAdded
                        current.liveLibrary || current.hasCachedData -> current.recentlyAdded
                        else -> emptyList()
                    },
                    incoming = when {
                        configuredQueue.isEmpty() -> demoIncoming()
                        queueLive -> snapshot.incoming
                        current.liveIncoming || current.hasCachedData -> current.incoming
                        else -> emptyList()
                    },
                    discover = when {
                        ServiceKind.SEERR !in configuredKinds -> demoDiscover()
                        seerrLive -> snapshot.discover
                        current.liveDiscover || current.hasCachedData -> current.discover
                        else -> emptyList()
                    },
                    activity = when {
                        configuredQueue.isEmpty() && ServiceKind.SEERR !in configuredKinds -> demoActivity()
                        activityLive -> snapshot.activity
                        current.liveActivity || current.hasCachedData -> current.activity
                        else -> emptyList()
                    },
                    connections = current.connections.map { connection ->
                        when {
                            connection.baseUrl.isBlank() -> connection.copy(state = ConnectionState.DEMO, detail = "Demo data")
                            connection.kind in snapshot.errors -> connection.copy(
                                state = ConnectionState.ERROR,
                                detail = snapshot.errors.getValue(connection.kind),
                            )
                            connection.kind in snapshot.successfulServices -> connection.copy(
                                state = ConnectionState.CONNECTED,
                                detail = "Live · updated just now",
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
                    snackbar = if (userInitiated) {
                        if (snapshot.errors.isEmpty()) "Everything is up to date"
                        else "Updated with ${snapshot.errors.size} service issue${if (snapshot.errors.size == 1) "" else "s"}"
                    } else current.snackbar,
                )
            }
        }
    }

    fun setNotifications(enabled: Boolean) {
        container.preferencesRepository.notificationsEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setWifiOnly(enabled: Boolean) {
        container.preferencesRepository.wifiOnly = enabled
        BackgroundRefreshScheduler.schedule(container.appContext, enabled)
        _uiState.update { it.copy(wifiOnly = enabled, snackbar = "Background refresh updated") }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbar = null) }

    fun updateConnectionName(value: String) = updateDraft { copy(name = value, error = null) }
    fun updateConnectionUrl(value: String) = updateDraft {
        copy(
            url = value,
            error = null,
            warning = runCatching {
                if (value.isNotBlank() && EndpointValidator.isCleartext(value)) {
                    "HTTP is unencrypted. Prefer HTTPS outside your trusted LAN."
                } else null
            }.getOrNull(),
        )
    }
    fun updateConnectionToken(value: String) = updateDraft { copy(token = value, error = null) }
    fun updateConnectionUserId(value: String) = updateDraft { copy(userId = value, error = null) }

    fun testAndSaveConnection() {
        val draft = connectionDraft.value ?: return
        val normalizedUrl = runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .getOrElse {
                updateDraft { copy(error = it.message ?: "Enter a valid server address") }
                return
            }
        if (draft.token.isBlank()) {
            updateDraft { copy(error = "Enter an API key or access token") }
            return
        }

        val candidate = ServiceConnection(
            kind = draft.kind,
            name = draft.name.ifBlank { draft.kind.displayName },
            baseUrl = normalizedUrl,
            token = draft.token,
            userId = draft.userId,
            state = ConnectionState.TESTING,
        )
        updateDraft { copy(url = normalizedUrl, saving = true, error = null) }

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { container.connectionTester.test(candidate) }
            }.getOrElse { error ->
                updateDraft {
                    copy(saving = false, error = error.message ?: "Could not reach this service")
                }
                return@launch
            }

            if (!result.success) {
                updateDraft { copy(saving = false, error = result.message) }
                return@launch
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
                    activeSheet = null,
                    snackbar = "${saved.kind.displayName} connected in ${result.latencyMs} ms",
                )
            }
            connectionDraft.value = null
            refreshLiveData()
        }
    }

    fun removeConnection(kind: ServiceKind) {
        container.connectionRepository.delete(kind)
        val remaining = container.connectionRepository.list()
        if (remaining.none { it.baseUrl.isNotBlank() }) container.mediaSnapshotStore.clear()
        _uiState.update {
            it.copy(
                connections = remaining,
                activeSheet = null,
                failedServices = it.failedServices - kind,
                snackbar = "${kind.displayName} connection removed",
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
}

private fun initialState(container: AppContainer): ReelstackUiState {
    val connections = container.connectionRepository.list()
    val configuredKinds = connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
    val hasMediaServer = configuredKinds.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val hasQueueService = configuredKinds.any { it == ServiceKind.RADARR || it == ServiceKind.SONARR }
    val hasSeerr = ServiceKind.SEERR in configuredKinds
    val cached = if (configuredKinds.isNotEmpty()) container.mediaSnapshotStore.read() else null

    return ReelstackUiState(
        selectedServer = container.preferencesRepository.selectedServer,
        connections = connections,
        session = when {
            hasMediaServer && cached != null -> cached.session
            hasMediaServer -> null
            else -> demoSession()
        },
        continueWatching = when {
            hasMediaServer && cached != null -> cached.continueWatching
            hasMediaServer -> emptyList()
            else -> demoContinueWatching()
        },
        recentlyAdded = when {
            hasMediaServer && cached != null -> cached.recentlyAdded
            hasMediaServer -> emptyList()
            else -> demoRecentlyAdded()
        },
        incoming = when {
            hasQueueService && cached != null -> cached.incoming
            hasQueueService -> emptyList()
            else -> demoIncoming()
        },
        discover = when {
            hasSeerr && cached != null -> cached.discover
            hasSeerr -> emptyList()
            else -> demoDiscover()
        },
        activity = when {
            (hasQueueService || hasSeerr) && cached != null -> cached.activity
            hasQueueService || hasSeerr -> emptyList()
            else -> demoActivity()
        },
        notificationsEnabled = container.preferencesRepository.notificationsEnabled,
        wifiOnly = container.preferencesRepository.wifiOnly,
        lastUpdatedEpochMillis = cached?.refreshedAtEpochMillis,
        hasCachedData = cached != null,
    )
}

private fun demoSession() = PlaybackSession(
    userName = "Maya",
    deviceName = "Living room TV",
    title = "Severance",
    subtitle = "S02  E04",
    progress = 0.58f,
    timeLeft = "32 min left",
    streamMethod = "Direct play",
    quality = "4K",
    paused = false,
)

private fun demoContinueWatching() = listOf(
    LibraryMedia(
        id = "continue-foundation",
        title = "Foundation",
        subtitle = "S03 · E02",
        progress = 0.42f,
        artworkRes = R.drawable.session_still,
        source = ServiceKind.JELLYFIN,
    ),
    LibraryMedia(
        id = "continue-shogun",
        title = "Shōgun",
        subtitle = "S01 · E07",
        progress = 0.71f,
        artworkRes = R.drawable.kitchen_request,
        source = ServiceKind.JELLYFIN,
    ),
)

private fun demoRecentlyAdded() = listOf(
    LibraryMedia(
        id = "recent-odyssey",
        title = "The Odyssey",
        subtitle = "Movie · 2026",
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.JELLYFIN,
    ),
    LibraryMedia(
        id = "recent-severance",
        title = "Severance",
        subtitle = "Series · 2 seasons",
        artworkRes = R.drawable.session_still,
        source = ServiceKind.JELLYFIN,
    ),
)

private fun demoIncoming() = listOf(
    IncomingMedia(
        id = "dune-messiah",
        title = "Dune: Messiah",
        source = ServiceKind.RADARR,
        status = "Downloading 68%",
        state = IncomingState.DOWNLOADING,
        artworkRes = R.drawable.desert_arrival,
    ),
    IncomingMedia(
        id = "the-bear",
        title = "The Bear",
        source = ServiceKind.SONARR,
        status = "Requested",
        state = IncomingState.REQUESTED,
        artworkRes = R.drawable.kitchen_request,
    ),
)

private fun demoDiscover() = listOf(
    DiscoverMedia("last-horizon", "The Last Horizon", "Movie · 2026", R.drawable.desert_arrival, false),
    DiscoverMedia("service", "Service", "Series · 3 seasons", R.drawable.kitchen_request, true),
)

private fun demoActivity() = listOf(
    ActivityEvent("odyssey", "The Odyssey", "Approved by Seerr", "2 min ago", complete = true, source = ServiceKind.SEERR),
    ActivityEvent("alien-earth", "Alien: Earth", "Sonarr · Downloading 42%", "8 min ago", progress = 42, source = ServiceKind.SONARR),
    ActivityEvent("mickey-17", "Mickey 17", "Imported by Radarr", "Yesterday", complete = true, source = ServiceKind.RADARR),
)
