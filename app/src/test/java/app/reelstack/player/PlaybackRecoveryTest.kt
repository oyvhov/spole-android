package app.reelstack.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.*
import org.junit.Test

class PlaybackRecoveryTest {
    @Test fun onlyNetworkErrorsEnterNetworkRecovery() {
        for (code in listOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)) {
            assertTrue(recoverablePlaybackFailure(code, null, false))
        }
        for (code in listOf(PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, PlaybackException.ERROR_CODE_IO_NO_PERMISSION)) {
            assertFalse(recoverablePlaybackFailure(code, null, false))
        }
    }
    @Test fun onlyTranscodingCanRetryANotYetReadyPlaylist() {
        val code = PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        assertTrue(recoverablePlaybackFailure(code, 404, true))
        assertFalse(recoverablePlaybackFailure(code, 404, false))
        for (status in listOf(401, 403, 400, 302)) assertFalse(recoverablePlaybackFailure(code, status, true))
        for (status in listOf(408, 429, 500, 502, 503, 504)) assertTrue(recoverablePlaybackFailure(code, status, false))
    }
}
