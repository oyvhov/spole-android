package app.reelstack.player

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException

/**
 * One rung down, not all the way to the floor.
 *
 * A failure during direct play used to drop straight to the conservative profile: H.264, SDR, at
 * most 1080p, stereo AAC, with stream copy switched off on both streams. That re-encodes a picture
 * the device was displaying perfectly well, and folds 5.1 down to two channels at 192 kbit/s — in
 * order to fix a problem that, nearly always, was the sound. A phone playing an EAC3 5.1 film had
 * both its picture and its surround taken away by one error, and kept them off for the rest of the
 * film, because the ladder had a single rung and it was the bottom one.
 *
 * Convert audio first unless the video renderer failed. A repeated audio failure stops playback;
 * it is not evidence that a working video stream needs re-encoding. Never loop indefinitely.
 */
internal fun nextPlaybackCompatibility(current: PlaybackCompatibility, videoFailed: Boolean): PlaybackCompatibility = when {
    current == PlaybackCompatibility.FULL -> PlaybackCompatibility.FULL
    videoFailed -> PlaybackCompatibility.FULL
    current == PlaybackCompatibility.AUDIO_ONLY -> PlaybackCompatibility.AUDIO_ONLY
    else -> PlaybackCompatibility.AUDIO_ONLY
}

/** Transport/authentication errors are not evidence of an unsupported video or audio format. */
internal fun playbackFailureNeedsConversion(errorCode: Int): Boolean = errorCode in setOf(
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
    PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
)

/**
 * Whether it was the picture that gave up.
 *
 * Media3 names the renderer and the format it choked on. A failure that names neither is not blamed
 * on the video: guessing wrong here is what costs a re-encode.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun playbackFailureIsVideo(error: PlaybackException): Boolean {
    val renderer = error as? ExoPlaybackException ?: return false
    if (renderer.type != ExoPlaybackException.TYPE_RENDERER) return false
    return MimeTypes.isVideo(renderer.rendererFormat?.sampleMimeType ?: return false)
}

/**
 * One line for the diagnostics panel: what gave up, on what, and where playback went next.
 *
 * Why a stream stepped down was only ever visible in logcat, which means it was visible to nobody
 * — a household with a television and no USB cable cannot answer "why did this transcode?" at all,
 * and that is the exact question this panel exists for. The code names are Media3's own, unmapped,
 * for the same reason the server's reason codes are printed unmapped two lines above.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun playbackFallbackLabel(error: PlaybackException, step: PlaybackCompatibility): String {
    return playbackFallbackLabel(error, step.name)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun playbackFallbackLabel(error: PlaybackException, destination: String): String {
    val format = (error as? ExoPlaybackException)
        ?.takeIf { it.type == ExoPlaybackException.TYPE_RENDERER }
        ?.rendererFormat?.sampleMimeType
    val renderer = (error as? ExoPlaybackException)?.rendererName
    return error.errorCodeName + (format?.let { " · $it" } ?: "") +
        (renderer?.let { " · $it" } ?: "") + " → " + destination
}

/** Only transient failures are retried; credentials and missing direct files need user action. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun recoverablePlaybackFailure(error: PlaybackException, transcoding: Boolean): Boolean {
    val http = generateSequence(error.cause) { it.cause }.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
    return recoverablePlaybackFailure(error.errorCode, http?.responseCode, transcoding)
}

internal fun recoverablePlaybackFailure(errorCode: Int, httpStatus: Int?, transcoding: Boolean): Boolean {
    if (httpStatus != null) return httpStatus in setOf(408, 429, 500, 502, 503, 504) ||
        (transcoding && httpStatus == 404)
    return errorCode in setOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
}
