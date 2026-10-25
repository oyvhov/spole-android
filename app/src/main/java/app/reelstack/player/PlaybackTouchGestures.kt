package app.reelstack.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class PlaybackTouchAxis { BRIGHTNESS, VOLUME }
internal data class PlaybackTouchFeedback(val axis: PlaybackTouchAxis, val fraction: Float)

/** Takes ownership only after a video gesture wins slop; controls keep their original down event. */
internal fun Modifier.playbackTouchGestures(enabled: Boolean, onStart: (PlaybackTouchAxis) -> Unit,
    onLevel: (Float) -> Unit, onZoom: (Float) -> Unit, onFinish: () -> Unit): Modifier = pointerInput(enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Final)
        var axis: PlaybackTouchAxis? = null
        var zoom = 1f
        var pinching = false
        var ignored = false
        try { do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (!ignored && pressed.size > 1 && axis == null) {
                pinching = true
                zoom *= event.calculateZoom()
                if (abs(zoom - 1f) > .08f) onZoom(zoom)
                event.changes.forEach { it.consume() }
            } else if (!ignored && !pinching && pressed.size == 1) {
                val movement = pressed.single().position - down.position
                if (axis == null && movement.getDistance() > viewConfiguration.touchSlop) {
                    if (abs(movement.y) > abs(movement.x) * 1.5f) {
                        axis = if (down.position.x < size.width / 2f) PlaybackTouchAxis.BRIGHTNESS else PlaybackTouchAxis.VOLUME
                        onStart(axis)
                    } else ignored = true
                }
                if (axis != null) {
                    onLevel(-movement.y / (size.height * .65f).coerceAtLeast(1f))
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
        } finally { if (axis != null || pinching) onFinish() }
    }
}

/** Brightness belongs to this window. Leaving playback restores auto/system brightness. */
@Composable
internal fun rememberPlaybackTouchControls(): PlaybackTouchControls {
    val context = LocalContext.current
    val controls = remember(context) { PlaybackTouchControls(context) }
    DisposableEffect(controls) { onDispose { controls.close() } }
    return controls
}

internal class PlaybackTouchControls(context: Context) : AutoCloseable {
    private val activity = context.activity()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val initialBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
    private val defaultBrightness = (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f).coerceIn(.02f, 1f)
    private var start = 0f
    private var axis = PlaybackTouchAxis.VOLUME
    fun start(value: PlaybackTouchAxis) {
        axis = value
        start = if (axis == PlaybackTouchAxis.VOLUME)
            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        else activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: defaultBrightness
    }
    fun adjust(delta: Float): PlaybackTouchFeedback? {
        val level = (start + delta).coerceIn(if (axis == PlaybackTouchAxis.BRIGHTNESS) .02f else 0f, 1f)
        return runCatching {
            if (axis == PlaybackTouchAxis.VOLUME) {
                if (audio.isVolumeFixed) return null
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).roundToInt(), 0)
            } else {
                val window = activity?.window ?: return null
                window.attributes = window.attributes.apply { screenBrightness = level }
            }
            PlaybackTouchFeedback(axis, level)
        }.getOrNull()
    }
    override fun close() { activity?.window?.let { it.attributes = it.attributes.apply { screenBrightness = initialBrightness } } }
    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}
