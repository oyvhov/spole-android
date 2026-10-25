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
import app.reelstack.ReelstackApplication
import app.reelstack.ui.theme.ReelstackTheme

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
                val adjustments by model.adjustments.collectAsStateWithLifecycle()
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
                    onAudio = model::audio,
                    onSubtitle = model::subtitles,
                    onQuality = {},
                    onExternal = {},
                    offline = true,
                    adjustments = adjustments,
                    onAdjust = model::adjust,
                    onRotate = {
                        requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT)
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    },
                )
            }
        }
    }

    override fun onResume() { super.onResume(); if (::model.isInitialized) model.foreground() }

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
