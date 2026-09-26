@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.reelstack.R
import app.reelstack.ReelstackApplication
import app.reelstack.data.model.ServiceKind
import app.reelstack.offline.OfflineDownloadRuntime
import app.reelstack.offline.OfflinePlaybackSource
import app.reelstack.offline.offlineServiceHash
import app.reelstack.ui.theme.ReelstackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A cache-only player. It has no network upstream and it is not exported, so a completed file
 * cannot become an accidental route to another account's media or to an external player.
 */
class OfflinePlayerActivity : app.reelstack.localization.LocalizedActivity() {
    private lateinit var model: OfflinePlayerModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            finish()
            return
        }
        val downloadId = intent.getStringExtra(DOWNLOAD_ID).orEmpty()
        if (!downloadId.matches(Regex("[A-Za-z0-9-]{1,160}"))) {
            finish()
            return
        }
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        enterFullscreen()
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OfflinePlayerModel((application as ReelstackApplication).container) as T
        })[OfflinePlayerModel::class.java]
        onBackPressedDispatcher.addCallback(this) { finish() }
        setContent {
            ReelstackTheme {
                LaunchedEffect(downloadId) { model.open(downloadId) }
                val state by model.state.collectAsStateWithLifecycle()
                DisposableEffect(state.playing, state.busy) {
                    if (state.playing || state.busy) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                }
                PlayerScreen(
                    state = state,
                    player = model.player,
                    onClose = ::finish,
                    onToggle = model::toggle,
                    onSeek = model::seek,
                    onRetry = model::retry,
                    onChoose = {},
                    onMore = {},
                    onAudio = {},
                    onSubtitle = {},
                    onQuality = {},
                    onExternal = {},
                    offline = true,
                    onRotate = {
                        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    },
                )
            }
        }
    }

    override fun onStop() {
        if (::model.isInitialized && !isChangingConfigurations) model.background()
        super.onStop()
    }

    private fun enterFullscreen() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }

    companion object {
        private const val DOWNLOAD_ID = "offline_download_id"

        fun open(context: Context, downloadId: String) {
            if (!downloadId.matches(Regex("[A-Za-z0-9-]{1,160}"))) return
            val intent = Intent(context, OfflinePlayerActivity::class.java).putExtra(DOWNLOAD_ID, downloadId)
            // Android refuses to start an activity from the application context unless it gets a
            // task of its own, and the refusal is a crash: «Sjå fråkopla» took the whole app down.
            if (context.hostActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        private tailrec fun Context.hostActivity(): android.app.Activity? = when (this) {
            is android.app.Activity -> this
            is android.content.ContextWrapper -> baseContext?.hostActivity()
            else -> null
        }
    }
}

private class OfflinePlayerModel(private val container: app.reelstack.AppContainer) : ViewModel() {
    private val mutable = MutableStateFlow(PlayerScreenState())
    val state = mutable.asStateFlow()
    private var source: OfflinePlaybackSource? = null
    // A download is accepted with the same FFmpeg audio the online player uses, so the local player
    // needs the same renderers. Without them an EAC3 or DTS film played with no sound.
    private val localAudioFallback = LocalAudioFallback()

    val player: ExoPlayer = ExoPlayer.Builder(container.appContext,
        PlaybackRenderersFactory(container.appContext, localAudioFallback))
        .setMediaSourceFactory(DefaultMediaSourceFactory(OfflineDownloadRuntime.offlineDataSourceFactory(container.appContext)))
        .build()
        .also { player ->
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutable.update { it.copy(playing = isPlaying, playWhenReady = player.playWhenReady) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val duration = player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                    mutable.update {
                        it.copy(
                            busy = playbackState == Player.STATE_BUFFERING,
                            ended = playbackState == Player.STATE_ENDED,
                            durationMs = duration,
                            positionMs = player.currentPosition.coerceAtLeast(0),
                            playWhenReady = player.playWhenReady,
                        )
                    }
                    if (playbackState == Player.STATE_ENDED) record(completed = true)
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (localAudioFallback.tryEnable(error)) {
                        // A platform decoder that claimed the track and then failed: hand the same
                        // local file to the bundled decoder, from the same position.
                        val position = player.currentPosition.coerceAtLeast(0)
                        val playWhenReady = player.playWhenReady
                        val item = player.currentMediaItem
                        if (item != null) {
                            player.stop()
                            player.setMediaItem(item, position)
                            mutable.update { it.copy(error = null, busy = true) }
                            player.prepare()
                            player.playWhenReady = playWhenReady
                            return
                        }
                    }
                    mutable.update { it.copy(busy = false, error = appString(R.string.offline_playback_failed)) }
                }
            })
        }

    init {
        viewModelScope.launch {
            while (isActive) {
                if (source != null) mutable.update { current ->
                    current.copy(positionMs = player.currentPosition.coerceAtLeast(0),
                        durationMs = player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: current.durationMs)
                }
                delay(500)
            }
        }
    }

    fun open(downloadId: String) {
        viewModelScope.launch {
            val activeProfile = container.connectionRepository.activeProfileId
            val resolved = withContext(Dispatchers.IO) { container.offlineDownloads.playbackSource(activeProfile, downloadId) }
            // The profile may have changed while the local database was read. Refuse rather than
            // briefly opening an adult file after a switch to a child profile.
            if (resolved == null || activeProfile != container.connectionRepository.activeProfileId) {
                mutable.update { it.copy(busy = false, error = appString(R.string.offline_playback_unavailable)) }
                return@launch
            }
            source = resolved
            localAudioFallback.reset()
            val item = MediaItem.Builder()
                .setMediaId(resolved.id)
                .setUri(resolved.uri)
                .setCustomCacheKey(resolved.cacheKey)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(resolved.title).setSubtitle(resolved.subtitle).build())
                .build()
            mutable.value = PlayerScreenState(
                title = resolved.title,
                subtitle = resolved.subtitle,
                busy = true,
                source = resolved.service,
                itemId = resolved.itemId,
                mode = PlaybackMode.DIRECT_PLAY,
            )
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }
    }

    fun toggle() {
        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
            player.play()
        } else if (player.playWhenReady) player.pause() else player.play()
    }

    fun seek(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, state.value.durationMs.coerceAtLeast(0)))
        mutable.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0), ended = false) }
    }

    fun retry() = source?.let { open(it.id) }

    fun background() {
        player.pause()
        record(completed = false)
    }

    private fun record(completed: Boolean) {
        val offline = source ?: return
        val connection = container.connectionRepository.list().firstOrNull {
            it.kind == offline.service && it.token.isNotBlank() && offlineServiceHash(it) == offline.serviceScope
        } ?: return
        container.localPlaybackStore.record(
            connection,
            PlayableItem(id = offline.itemId, title = offline.title, subtitle = offline.subtitle, type = offline.mediaType),
            player.currentPosition.coerceAtLeast(0),
            player.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: state.value.durationMs,
            completed = completed,
        )
    }

    private fun appString(@androidx.annotation.StringRes id: Int): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(id)

    override fun onCleared() {
        record(completed = player.playbackState == Player.STATE_ENDED)
        player.release()
        super.onCleared()
    }
}
