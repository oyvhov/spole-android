package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

/** Match server bitmap ordinals within their exact codec; sidecars and other codecs do not count. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun playbackSubtitleOverride(plan: PlaybackPlan, tracks: Tracks, readyTextIndices: Set<Int>,
    vttFallback: Set<Int> = emptySet()): TrackSelectionOverride? {
    val selected = plan.subtitles.firstOrNull { it.index == plan.subtitleIndex } ?: return null
    val textTracks = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.flatMap { group ->
        (0 until group.length).map { group to it }
    }
    val target = if (selected.localBitmap && plan.direct) {
        val mime = selected.embeddedBitmapMime ?: MimeTypes.APPLICATION_PGS
        val pgs = textTracks.filter { (group, index) ->
            val format = group.getTrackFormat(index)
            // Media3's extraction-time parser replaces the sample MIME and keeps the original in codecs.
            !format.id.orEmpty().substringAfterLast(':').startsWith("spole-subtitle-") &&
                (format.sampleMimeType == mime ||
                    format.sampleMimeType == MimeTypes.APPLICATION_MEDIA3_CUES && format.codecs == mime)
        }
        // A stream offered as a sidecar can still exist inside the original MKV. Count that
        // original in the ordinal mapping, but never count its separately merged sidecar twice.
        val advertised = plan.subtitles.filter { (it.embeddedPgs || it.embeddedBitmapMime != null) &&
            (it.embeddedBitmapMime ?: MimeTypes.APPLICATION_PGS) == mime }.sortedBy { it.index }
        // A mismatch is ambiguous: keep text off instead of silently showing a different language.
        if (pgs.size != advertised.size) return null
        pgs.getOrNull(advertised.indexOfFirst { it.index == selected.index })
    } else if (selected.sidecar && selected.index in readyTextIndices) {
        // An original ASS that libass could not take is replaced by the server's VTT of the same stream.
        val id = subtitleSidecarId(selected.index, vtt = selected.index in vttFallback)
        textTracks.firstOrNull { (group, index) -> group.getTrackFormat(index).id.orEmpty().substringAfterLast(':') == id }
    } else null
    return target?.takeIf { (group, index) -> group.isTrackSupported(index) }
        ?.let { (group, index) -> TrackSelectionOverride(group.mediaTrackGroup, index) }
}

internal fun subtitleSidecarId(index: Int, vtt: Boolean = false) = "spole-subtitle-$index" + if (vtt) "-vtt" else ""
