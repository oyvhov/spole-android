package app.reelstack.player

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

internal data class PlaybackDisplayMode(val id: Int, val width: Int, val height: Int, val refreshRate: Float)

/** Match an integer cadence, without changing resolution or confusing 23.976 with 24 fps. */
internal fun matchingDisplayMode(frameRate: Float, current: PlaybackDisplayMode,
    supported: List<PlaybackDisplayMode>): PlaybackDisplayMode? {
    if (!frameRate.isFinite() || frameRate !in 1f..240f) return null
    fun matches(mode: PlaybackDisplayMode): Boolean {
        if (mode.width != current.width || mode.height != current.height || !mode.refreshRate.isFinite()) return false
        val multiple = (mode.refreshRate / frameRate).roundToInt()
        return multiple in 1..10 && abs(mode.refreshRate - frameRate * multiple) <= 0.02f
    }
    return current.takeIf(::matches) ?: supported.filter(::matches).minByOrNull { it.refreshRate }
}

/** Containers such as Matroska may omit frame rate from the decoder's Format. */
internal fun sourceVideoFrameRate(source: JsonObject): Float {
    val video = source.objects("MediaStreams").firstOrNull { it.str("Type") == "Video" } ?: return 0f
    return listOf("AverageFrameRate", "RealFrameRate").firstNotNullOfOrNull { key ->
        (video[key] as? JsonPrimitive)?.floatOrNull?.takeIf { it.isFinite() && it in 1f..240f }
    } ?: 0f
}
