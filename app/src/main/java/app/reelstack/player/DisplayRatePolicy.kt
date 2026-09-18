package app.reelstack.player

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

/** Containers such as Matroska may omit frame rate from the decoder's Format. */
internal fun sourceVideoFrameRate(source: JsonObject): Float {
    val video = source.objects("MediaStreams").firstOrNull { it.str("Type") == "Video" } ?: return 0f
    return listOf("AverageFrameRate", "RealFrameRate").firstNotNullOfOrNull { key ->
        (video[key] as? JsonPrimitive)?.floatOrNull?.takeIf { it.isFinite() && it in 1f..240f }
    } ?: 0f
}
