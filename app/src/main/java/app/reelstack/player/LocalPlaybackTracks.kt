package app.reelstack.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

/** Stable for a loaded media item, with no server round-trip or guessed stream index. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun localPlaybackTracks(tracks: Tracks, type: Int, fallback: (Int) -> String): List<PlaybackTrack> =
    tracks.groups.flatMapIndexed { groupIndex, group ->
        if (group.type != type) emptyList() else (0 until group.length).filter(group::isTrackSupported).map { index ->
            val format = group.getTrackFormat(index)
            PlaybackTrack(groupIndex * 1_000 + index,
                format.label?.takeIf(String::isNotBlank) ?: listOfNotNull(format.language, format.codecs ?: format.sampleMimeType?.substringAfter('/'))
                    .joinToString(" · ").ifBlank { fallback(index + 1) }, format.language, isText = type == C.TRACK_TYPE_TEXT)
        }
    }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun selectedLocalTrack(tracks: Tracks, type: Int): Int? = tracks.groups.withIndex().firstNotNullOfOrNull { (groupIndex, group) ->
    if (group.type != type) null else (0 until group.length).firstOrNull(group::isTrackSelected)?.let { groupIndex * 1_000 + it }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun selectLocalTrack(player: Player, type: Int, id: Int) {
    val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(type)
    if (id == -1 && type == C.TRACK_TYPE_TEXT) builder.setTrackTypeDisabled(type, true)
    else {
        val group = player.currentTracks.groups.getOrNull(id / 1_000) ?: return
        val index = id % 1_000
        if (group.type != type || index !in 0 until group.length || !group.isTrackSupported(index)) return
        builder.setTrackTypeDisabled(type, false).addOverride(TrackSelectionOverride(group.mediaTrackGroup, index))
    }
    player.trackSelectionParameters = builder.build()
}
