package app.reelstack.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session-local values shared by online/offline engines. Positive delay means later than video. */
data class PlaybackAdjustments(val speed: Float = 1f, val audioDelayMs: Int = 0, val subtitleDelayMs: Int = 0) {
    fun bounded() = copy(speed = speed.takeIf { it.isFinite() }?.coerceIn(.5f, 2f) ?: 1f,
        audioDelayMs = audioDelayMs.coerceIn(-2_000, 2_000), subtitleDelayMs = subtitleDelayMs.coerceIn(-10_000, 10_000))
}

internal class PlaybackAdjustmentController {
    private val mutable = MutableStateFlow(PlaybackAdjustments())
    val state = mutable.asStateFlow()
    val current: PlaybackAdjustments get() = mutable.value

    fun apply(player: Player, value: PlaybackAdjustments) {
        val next = value.bounded()
        val delayChanged = current.audioDelayMs != next.audioDelayMs || current.subtitleDelayMs != next.subtitleDelayMs
        mutable.value = next
        player.setPlaybackSpeed(next.speed)
        // Media3 keeps its audio clock monotonic between discontinuities. Re-anchor after changing
        // the A/V relation. Text parsers also need to replay cues already discarded before a new
        // delay. Keep the current position and pause/play intent for both changes.
        if (delayChanged && player.isCurrentMediaItemSeekable) player.seekTo(player.currentPosition)
    }
}

internal fun subtitleRenderPosition(positionUs: Long, delayMs: Int): Long = positionUs - delayMs.toLong() * 1_000
internal fun adjustedAudioClock(positionUs: Long, delayMs: Int): Long = positionUs + delayMs.toLong() * 1_000
