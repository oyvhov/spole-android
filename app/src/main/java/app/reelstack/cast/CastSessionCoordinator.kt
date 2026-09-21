package app.reelstack.cast

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CastPhase { UNAVAILABLE, DISCONNECTED, CONNECTED, LOADING, PLAYING, ERROR }

data class CastUiState(
    val phase: CastPhase,
    val deviceName: String = "",
    val title: String = "",
    val subtitle: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: CastError? = null,
) {
    val active: Boolean get() = phase in setOf(CastPhase.CONNECTED, CastPhase.LOADING, CastPhase.PLAYING)
}

enum class CastError { NO_RECEIVER, SERVER_UNREACHABLE, CORS, EXPIRED_CREDENTIAL, UNSUPPORTED_MEDIA, UNKNOWN }

/** Small gateway surface keeps Cast framework code out of composables and is faked in unit tests. */
interface CastGateway {
    val state: StateFlow<CastUiState>
    /** [onReceiverStarted] is the local-player handover boundary. */
    fun load(request: CastLoadRequest, onReceiverStarted: () -> Unit = {}): Boolean
    fun playPause()
    fun seekBy(deltaMs: Long)
    fun stop()
    fun clearCredentials()
}

class CastSessionCoordinator(context: Context) : CastGateway {
    private val appContext = context.applicationContext
    private val castContext by lazy { CastContext.getSharedInstance(appContext) }
    private val mutableState = MutableStateFlow(
        CastUiState(if (CastConfiguration.isEnabled(appContext)) CastPhase.DISCONNECTED else CastPhase.UNAVAILABLE),
    )
    override val state: StateFlow<CastUiState> = mutableState.asStateFlow()
    private var activeRequest: CastLoadRequest? = null
    private var pendingLoad: Pair<CastLoadRequest, () -> Unit>? = null

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStarted(session: CastSession, sessionId: String) = connected(session)
        override fun onSessionStartFailed(session: CastSession, error: Int) = failed(CastError.NO_RECEIVER)
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionEnded(session: CastSession, error: Int) { clearCredentials(); mutableState.value = mutableState.value.copy(phase = CastPhase.DISCONNECTED, deviceName = "", title = "", subtitle = "") }
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = connected(session)
        override fun onSessionResumeFailed(session: CastSession, error: Int) = failed(CastError.NO_RECEIVER)
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    init {
        if (CastConfiguration.isEnabled(appContext)) castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
    }

    private fun connected(session: CastSession) {
        mutableState.value = mutableState.value.copy(phase = CastPhase.CONNECTED, deviceName = session.castDevice?.friendlyName.orEmpty(), error = null)
        pendingLoad?.let { (request, onStarted) ->
            pendingLoad = null
            load(request, onStarted)
        }
    }

    private fun failed(error: CastError) { clearCredentials(); mutableState.value = mutableState.value.copy(phase = CastPhase.ERROR, error = error) }

    override fun load(request: CastLoadRequest, onReceiverStarted: () -> Unit): Boolean {
        if (!CastConfiguration.isEnabled(appContext)) { failed(CastError.NO_RECEIVER); return false }
        val session = castContext.sessionManager.currentCastSession
        val client = session?.remoteMediaClient
        // The system route picker establishes a session after the user picks a device. Hold only
        // the in-memory envelope until that callback; a failed/ended session clears it below.
        if (client == null) { pendingLoad = request to onReceiverStarted; return true }
        val item = request.spec.items[request.spec.startIndex]
        activeRequest = request
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, item.title)
            putString(MediaMetadata.KEY_SUBTITLE, item.subtitle)
        }
        val media = MediaInfo.Builder("spole://load/${item.itemId}")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .setMetadata(metadata)
            .setCustomData(request.spec.receiverPayload())
            .build()
        mutableState.value = CastUiState(CastPhase.LOADING, session.castDevice?.friendlyName.orEmpty(), item.title, item.subtitle)
        client.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(media)
                .setAutoplay(true)
                .setCurrentTime(item.resumePositionMs)
                .setCredentials(request.credentials.loadCredentials())
                .build(),
        ).setResultCallback { result ->
            if (!result.status.isSuccess) failed(mapError(result.status.statusCode))
            else {
                mutableState.value = mutableState.value.copy(phase = CastPhase.PLAYING, error = null)
                onReceiverStarted()
            }
        }
        return true
    }

    override fun playPause() {
        castContext.sessionManager.currentCastSession?.remoteMediaClient?.let { client ->
            if (client.isPlaying) client.pause() else client.play()
        }
    }

    override fun seekBy(deltaMs: Long) {
        val client = castContext.sessionManager.currentCastSession?.remoteMediaClient ?: return
        client.seek((client.approximateStreamPosition + deltaMs).coerceAtLeast(0))
    }

    override fun stop() {
        if (CastConfiguration.isEnabled(appContext)) castContext.sessionManager.endCurrentSession(true)
        clearCredentials()
        mutableState.value = CastUiState(if (CastConfiguration.isEnabled(appContext)) CastPhase.DISCONNECTED else CastPhase.UNAVAILABLE)
    }

    override fun clearCredentials() { activeRequest = null; pendingLoad = null }

    private fun mapError(status: Int): CastError = when (status) {
        7, 8 -> CastError.SERVER_UNREACHABLE
        401, 403 -> CastError.EXPIRED_CREDENTIAL
        else -> CastError.UNKNOWN
    }
}
