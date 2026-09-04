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
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
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
    data class SessionDetails(val sessionKey: String) : AppSheet
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
    val activeSheet: AppSheet? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val sessions: List<PlaybackSession> = demoSessions(),
    val recentMovies: List<LibraryMedia> = demoRecentMovies(),
    val recentSeries: List<LibraryMedia> = demoRecentSeries(),
    val upcoming: List<UpcomingMedia> = demoUpcoming(),
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
    val serviceWarnings: Map<ServiceKind, String> = emptyMap(),
    val requestingMediaIds: Set<String> = emptySet(),
    val pendingSessionKey: String? = null,
    val homeSections: Set<HomeSection> = HomeSection.entries.toSet(),
    val hasCachedData: Boolean = false,
    val snackbar: String? = null,
) {
    val visibleDiscover: List<DiscoverMedia>
        get() = if (searchQuery.isBlank()) discover else discover.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }

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
                    if (EndpointValidator.isCleartext(it)) "HTTP er ukryptert. Bruk helst HTTPS utanfor det trygge lokalnettet ditt." else null
                },
            )
        }
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = null) }
        connectionDraft.value = null
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
                    snackbar = "Demobestillinga er lagra lokalt · kople til Seerr for å sende henne",
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
                                detail = "Sendt til Seerr",
                                time = "No nettopp",
                                source = ServiceKind.SEERR,
                                artworkRes = media.artworkRes,
                                artworkUrl = media.artworkUrl,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Bestillinga er send til Seerr",
                    )
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = "Seerr kunne ikkje ta imot bestillinga",
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
                        configuredMedia.isEmpty() -> demoSessions()
                        mediaLive -> snapshot.sessions
                        current.liveSession || current.hasCachedData -> current.sessions
                        else -> emptyList()
                    },
                    recentMovies = when {
                        configuredMedia.isEmpty() -> demoRecentMovies()
                        mediaLive -> (snapshot.recentMovies + current.recentMovies.filter { it.source in snapshot.errors })
                            .distinctBy(LibraryMedia::id)
                        current.liveLibrary || current.hasCachedData -> current.recentMovies
                        else -> emptyList()
                    },
                    recentSeries = when {
                        configuredMedia.isEmpty() -> demoRecentSeries()
                        mediaLive -> (snapshot.recentSeries + current.recentSeries.filter { it.source in snapshot.errors })
                            .distinctBy(LibraryMedia::id)
                        current.liveLibrary || current.hasCachedData -> current.recentSeries
                        else -> emptyList()
                    },
                    upcoming = when {
                        configuredQueue.isEmpty() -> demoUpcoming()
                        queueLive -> (snapshot.upcoming + current.upcoming.filter { it.source in snapshot.errors })
                            .distinctBy(UpcomingMedia::id)
                            .sortedBy(UpcomingMedia::airDateEpochMillis)
                        current.liveIncoming || current.hasCachedData -> current.upcoming
                        else -> emptyList()
                    },
                    incoming = when {
                        configuredQueue.isEmpty() -> demoIncoming()
                        queueLive -> (snapshot.incoming + current.incoming.filter { it.source in snapshot.errors })
                            .distinctBy(IncomingMedia::id)
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
    fun updateConnectionUrl(value: String) = updateDraft {
        copy(
            url = value,
            error = null,
            warning = runCatching {
                if (value.isNotBlank() && EndpointValidator.isCleartext(value)) {
                    "HTTP er ukryptert. Bruk helst HTTPS utanfor det trygge lokalnettet ditt."
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
                updateDraft { copy(error = it.message ?: "Skriv inn ei gyldig tenaradresse") }
                return
            }
        if (draft.token.isBlank()) {
            updateDraft { copy(error = "Skriv inn ein API-nøkkel eller eit tilgangsteikn") }
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
                    copy(saving = false, error = error.message ?: "Fekk ikkje kontakt med tenesta")
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
                    snackbar = "${saved.kind.displayName} vart kopla til på ${result.latencyMs} ms",
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
}

private fun initialState(container: AppContainer): ReelstackUiState {
    val connections = container.connectionRepository.list()
    val configuredKinds = connections.filter { it.baseUrl.isNotBlank() }.mapTo(mutableSetOf()) { it.kind }
    val hasMediaServer = configuredKinds.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val hasQueueService = configuredKinds.any { it == ServiceKind.RADARR || it == ServiceKind.SONARR }
    val hasSeerr = ServiceKind.SEERR in configuredKinds
    val cached = if (configuredKinds.isNotEmpty()) container.mediaSnapshotStore.read() else null

    return ReelstackUiState(
        connections = connections,
        sessions = when {
            hasMediaServer && cached != null -> cached.sessions
            hasMediaServer -> emptyList()
            else -> demoSessions()
        },
        recentMovies = when {
            hasMediaServer && cached != null -> cached.recentMovies
            hasMediaServer -> emptyList()
            else -> demoRecentMovies()
        },
        recentSeries = when {
            hasMediaServer && cached != null -> cached.recentSeries
            hasMediaServer -> emptyList()
            else -> demoRecentSeries()
        },
        upcoming = when {
            hasQueueService && cached != null -> cached.upcoming
            hasQueueService -> emptyList()
            else -> demoUpcoming()
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
        homeSections = container.preferencesRepository.visibleHomeSections,
        lastUpdatedEpochMillis = cached?.refreshedAtEpochMillis,
        hasCachedData = cached != null,
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
    ),
)

private fun demoRecentSeries() = listOf(
    LibraryMedia(
        id = "recent-severance",
        title = "Severance",
        subtitle = "Serie · 2 sesongar",
        artworkRes = R.drawable.session_still,
        source = ServiceKind.JELLYFIN,
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
    ),
    UpcomingMedia(
        id = "upcoming-odyssey",
        title = "The Odyssey",
        subtitle = "Film · 2026",
        dateLabel = "I morgon",
        airDateEpochMillis = System.currentTimeMillis() + 86_400_000,
        artworkRes = R.drawable.desert_arrival,
        source = ServiceKind.RADARR,
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
        status = "Bestilt",
        state = IncomingState.REQUESTED,
        artworkRes = R.drawable.kitchen_request,
    ),
)

private fun demoDiscover() = listOf(
    DiscoverMedia("last-horizon", "The Last Horizon", "Film · 2026", R.drawable.desert_arrival, false),
    DiscoverMedia("service", "Service", "Serie · 3 sesongar", R.drawable.kitchen_request, true),
)

private fun demoActivity() = listOf(
    ActivityEvent("odyssey", "The Odyssey", "Godkjend i Seerr", "For 2 min sidan", complete = true, source = ServiceKind.SEERR, artworkRes = R.drawable.desert_arrival),
    ActivityEvent("alien-earth", "Alien: Earth", "Sonarr · lastar ned 42 %", "For 8 min sidan", progress = 42, source = ServiceKind.SONARR, artworkRes = R.drawable.kitchen_request),
    ActivityEvent("mickey-17", "Mickey 17", "Importert av Radarr", "I går", complete = true, source = ServiceKind.RADARR, artworkRes = R.drawable.desert_arrival),
)
