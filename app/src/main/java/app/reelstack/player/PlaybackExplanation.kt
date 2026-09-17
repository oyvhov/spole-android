package app.reelstack.player

import androidx.annotation.StringRes
import app.reelstack.R

/**
 * Turning what the media server did into something a person can act on.
 *
 * Jellyfin and Emby both answer `PlaybackInfo` with a reason code when they cannot send the file as
 * it is — `AudioCodecNotSupported`, `SubtitleCodecNotSupported`, and a couple of dozen more. Spole
 * has always had these in the response and thrown them away, which is why the player could only say
 * "adapted playback" and leave the household guessing.
 *
 * The codes are the server's, not ours: unknown ones fall back to a general sentence rather than
 * being shown raw, because a line of English CamelCase in a nynorsk interface helps nobody.
 */

/** The line the OSD shows: what is happening to the file, in words. */
@StringRes
fun playbackModeLabel(mode: PlaybackMode): Int = when (mode) {
    PlaybackMode.DIRECT_PLAY -> R.string.player_mode_direct
    PlaybackMode.DIRECT_STREAM -> R.string.player_mode_remux
    PlaybackMode.AUDIO_TRANSCODE -> R.string.player_mode_audio
    PlaybackMode.FULL_TRANSCODE -> R.string.player_mode_transcode
}

/**
 * The reason that matters, out of however many the server listed.
 *
 * A server that has to convert the sound *and* burn in a subtitle reports both, and a sentence that
 * lists everything is a sentence nobody finishes. The codes are ordered by what actually decides
 * how much work the server does, and the first match wins.
 */
@StringRes
fun playbackReasonLabel(reasons: List<String>): Int? {
    if (reasons.isEmpty()) return null
    val codes = reasons.map { it.trim().lowercase() }
    fun any(vararg needles: String) = codes.any { code -> needles.any { code.contains(it) } }
    return when {
        any("videocodecnotsupported") -> R.string.player_reason_video_codec
        any("videoprofilenotsupported", "videolevelnotsupported", "refframes", "anamorphic", "interlaced",
            "videobitdepth", "videoframerate") -> R.string.player_reason_profile
        any("videorangetype", "videorange") -> R.string.player_reason_video_range
        any("videoresolutionnotsupported") -> R.string.player_reason_resolution
        any("subtitlecodecnotsupported") -> R.string.player_reason_subtitle
        any("audiochannelsnotsupported") -> R.string.player_reason_audio_channels
        any("audiocodecnotsupported", "audioprofile", "audiosamplerate", "audiobitdepth", "audioisexternal",
            "secondaryaudio") -> R.string.player_reason_audio_codec
        any("bitrate") -> R.string.player_reason_bitrate
        any("containernotsupported") -> R.string.player_reason_container
        else -> R.string.player_reason_other
    }
}

/**
 * The reason resource for a state, or null when the file is playing untouched and there is nothing
 * to explain. Both the player model and the Compose layer go through this, so they cannot drift.
 */
@StringRes
fun playbackReasonFor(state: PlayerScreenState): Int? =
    if (state.mode == PlaybackMode.DIRECT_PLAY) null else playbackReasonLabel(state.transcodeReasons)
