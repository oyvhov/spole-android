package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns calendar follow actions, remote playback, and Home settings for the current session. Created and retired by ReelstackFeatures. */
internal class HomeActionsCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val accountProfileClient: app.reelstack.data.network.AccountProfileClient,
    private val appContext: Context,
    private val calendarFollowStore: app.reelstack.data.repository.CalendarFollowStore,
    private val connectionRepository: app.reelstack.data.repository.ConnectionRepository,
    private val homeLibraries: (List<ServiceConnection>) -> Map<ServiceKind, app.reelstack.data.model.HomeLibraryChoice>,
    private val mediaFingerprint: (List<ServiceConnection>) -> String,
    private val mediaServerClient: app.reelstack.data.network.MediaServerClient,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val offlineDownloads: app.reelstack.offline.OfflineDownloadRepository,
    private val preferencesRepository: app.reelstack.data.repository.AppPreferencesRepository,
    private val cancelHomeRefresh: () -> Unit,
    private val refreshLiveData: () -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    fun toggleCalendarFollow(details: ContentDetails) {
        val owner = connectionRepository.captureSession()
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() } ?: return
        val id = details.tmdbId ?: return
        if (owner.kidsMode || state.sessionScope != owner) return
        val type = if (details.mediaType.equals("Movie", true)) "movie" else "tv"
        val title = app.reelstack.data.model.CalendarTitle(id, type, details.title, details.artworkUrl)
        val fingerprint = mediaFingerprint(state.connections)
        operationScope.launch {
            val outcome = attempt { withContext(ioDispatcher) {
                val actor = accountProfileClient.load(connection)
                check(actor.id == connection.userId && actor.isPersonal)
                check(mediaFingerprint(_uiState.value.connections) == fingerprint && !_uiState.value.isKidMode)
                val store = calendarFollowStore
                val scope = store.scope(owner.profileId, connection, actor.id)
                checkNotNull(connectionRepository.withCurrentSession(owner) {
                    val existing = store.read(scope).titles.firstOrNull { it.key == title.key }
                    val automatic = state.upcoming.any { it.tmdbId == id && (if (it.mediaType == "Movie") "movie" else "tv") == type } ||
                        state.calendarUndated.any { it.key == title.key }
                    val followed = if (existing != null) !existing.hidden else automatic
                    store.set(scope, title, !followed)
                    !followed
                })
            } }
            outcome.onSuccess { followed ->
                if (!connectionRepository.isCurrent(owner) || _uiState.value.activeProfileId != state.activeProfileId ||
                    _uiState.value.connections != state.connections || _uiState.value.isKidMode) return@onSuccess
                _uiState.update { it.copy(calendarHidden = if (followed) it.calendarHidden - title.key else it.calendarHidden + title.key,
                    snackbar = appString(if (followed) R.string.calendar_followed else R.string.calendar_unfollowed)) }
                cancelHomeRefresh()
                refreshLiveData()
            }.onFailure { _uiState.update { it.copy(snackbar = appString(R.string.error_content_details)) } }
        }
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
        operationScope.launch {
            val result = attempt {
                withContext(ioDispatcher) {
                    mediaSyncRepository.setPlaybackPaused(
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
    fun setNotifications(enabled: Boolean) {
        preferencesRepository.notificationsEnabled = enabled
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }
    fun setWifiOnly(enabled: Boolean) {
        preferencesRepository.wifiOnly = enabled
        BackgroundRefreshScheduler.schedule(appContext, enabled)
        // The same explicit preference applies to the phone/tablet offline worker. TV never
        // exposes a download entry point, so it cannot create a job here.
        offlineDownloads.onlyWifi(enabled)
        _uiState.update { it.copy(wifiOnly = enabled, snackbar = appString(R.string.notice_wifi_policy_updated)) }
    }
    private var homeLibraryViewsJob: Job? = null

    /**
     * Every library each media server offers this account, for the Home editor to choose from.
     * Read unfiltered: a library hidden from the Library tab can still belong on Home.
     */
    fun loadHomeLibraryViews() {
        val connections = _uiState.value.connections.filter {
            it.kind in app.reelstack.data.model.HOME_MEDIA_SOURCES && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update { it.copy(homeLibraries = homeLibraries(it.connections),
            homeLibraryViewsLoading = connections.isNotEmpty(), homeLibraryViewsFailed = emptySet()) }
        val profileId = _uiState.value.activeProfileId
        homeLibraryViewsJob?.cancel()
        homeLibraryViewsJob = operationScope.launch {
            val loaded = connections.map { connection ->
                async(ioDispatcher) { connection.kind to runCatching { mediaServerClient.browseLibraries(connection) } }
            }.map { it.await() }
            // Another profile's libraries must never be offered as this one's.
            if (!isActive || _uiState.value.activeProfileId != profileId) return@launch
            _uiState.update { current -> current.copy(
                homeLibraryViews = loaded.mapNotNull { (kind, result) -> result.getOrNull()?.let { kind to it } }.toMap(),
                homeLibraryViewsFailed = loaded.filter { it.second.isFailure }.mapTo(mutableSetOf()) { it.first },
                homeLibraryViewsLoading = false,
            ) }
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
