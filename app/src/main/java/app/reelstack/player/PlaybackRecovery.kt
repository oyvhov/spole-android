package app.reelstack.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource

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
