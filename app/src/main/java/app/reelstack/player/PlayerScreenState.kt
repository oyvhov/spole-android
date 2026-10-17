package app.reelstack.player

import app.reelstack.data.model.ServiceKind

data class PlayerScreenState(
    val title: String = "Spole", val subtitle: String = "", val busy: Boolean = true,
    val error: String? = null, val warning: String? = null,
    val choices: List<PlayableItem> = emptyList(), val browsing: Boolean = false, val hasMore: Boolean = false,
    val playing: Boolean = false, val ended: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0,
    val audio: List<PlaybackTrack> = emptyList(), val subtitles: List<PlaybackTrack> = emptyList(),
    val audioIndex: Int? = null, val subtitleIndex: Int = -1,
    val mode: PlaybackMode = PlaybackMode.DIRECT_PLAY,
    /** The server's own reason codes, mapped to a sentence only when they are shown. */
    val transcodeReasons: List<String> = emptyList(),
    val quality: Int = 0,
    /** Playback intent stays true while a seek buffers; playing alone cannot distinguish a pause. */
    val playWhenReady: Boolean = false,
    val awaitingResume: Boolean = false,
    val chapters: List<PlaybackChapter> = emptyList(),
    val trickplay: TrickplayGrid? = null,
    val itemId: String = "",
    val logoUrl: String? = null,
    /** The numbers behind [subtitle], so the header can write them out rather than show "S03 E01". */
    val season: Int? = null,
    val episode: Int? = null,
    /**
     * What follows this episode, and how long is left before it starts on its own. Null means
     * there is nothing after it — a film, the last episode, or a server that could not say.
     */
    val nextEpisode: PlayableItem? = null,
    /** Whether the server has conclusively answered the next-episode lookup for this episode. */
    val nextEpisodeResolved: Boolean = false,
    val nextEpisodeCountdown: Int? = null,
    val nextEpisodeCountdownTotalSeconds: Int = 12,
    val nextEpisodeOfferEnabled: Boolean = true,
    val nextEpisodeLeadSeconds: Int = 15,
    val nextEpisodeDismissed: Boolean = false,
    /** A child routine was reached. The current episode was allowed to finish first. */
    val bedtimeReached: Boolean = false,
    /** Title sequences and closing credits the server has marked, if anything has marked them. */
    val segments: List<PlaybackSegment> = emptyList(),
    val source: ServiceKind = ServiceKind.JELLYFIN,
    val videoCodec: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val videoBitrate: Int = 0,
    val videoFrameRate: Float = 0f,
    val videoHdr: String = "SDR",
    /**
     * The audio as it arrives, which is not the audio the file holds.
     *
     * The panel used to print the source track here -- "English EAC3 5.1" -- directly under a line
     * saying the server was converting the audio because this device cannot play that format. The
     * two contradicted each other, and the one in a diagnostics panel that must be true is the one
     * describing what is actually being decoded.
     */
    val audioCodec: String? = null,
    val audioChannels: Int = 0,
    val audioDecoder: String = "",
    val videoDecoder: String = "",
    val audioUnderruns: Int = 0,
    val subtitleUnavailable: Boolean = false,
    /**
     * The audio codecs this device told the server it can play, and up to how many channels.
     *
     * When a server answers "this device cannot play the audio format", the only way to tell a
     * correct refusal from a detection bug is to see what was actually claimed -- and until now
     * that was only visible by reading the source. It is one line in Stats for Nerds instead.
     */
    val advertisedAudio: String = "",
    /** The video codecs claimed, with the largest frame each was accepted at. */
    val advertisedVideo: String = "",
    /**
     * Why this stream stepped down, when it did.
     *
     * Empty for a stream that started and stayed where it started. Otherwise the Media3 error code,
     * the format the failing renderer choked on, and the rung playback dropped to — which used to
     * exist only in logcat, where a television cannot be read.
     */
    val fallback: String = "",
) {
    /** Kept so every reader that only cares whether the file is untouched still compiles. */
    val direct: Boolean get() = mode == PlaybackMode.DIRECT_PLAY
}

/**
 * The marked stretch the playhead is inside right now, if any.
 *
 * A small guard at each end keeps the offer from flickering on and off around the boundary, and
 * from appearing for the last half second of a title sequence nobody would bother skipping.
 */
fun PlayerScreenState.activeSegment(): PlaybackSegment? = segments.firstOrNull { segment ->
    positionMs >= segment.startMs && positionMs < segment.endMs - 1_500
}
