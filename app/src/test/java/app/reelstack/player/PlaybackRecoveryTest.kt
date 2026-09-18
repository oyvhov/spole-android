package app.reelstack.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.*
import org.junit.Test

class PlaybackRecoveryTest {
    @Test fun containerErrorsTryRemuxOnceOnlyForAnUntouchedFile() {
        for (code in listOf(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED)) {
            assertEquals(PlaybackCompatibility.REMUX,
                playbackRecoveryCompatibility(PlaybackCompatibility.DIRECT, code, false, true))
            // A server-negotiated HLS stream may still have DIRECT compatibility. Never loop.
            assertNull(playbackRecoveryCompatibility(PlaybackCompatibility.DIRECT, code, false, false))
            for (mode in PlaybackCompatibility.entries.filter { it != PlaybackCompatibility.DIRECT }) {
                assertNull(playbackRecoveryCompatibility(mode, code, false, false))
            }
            assertFalse(recoverablePlaybackFailure(code, null, false))
        }
    }

    @Test fun remuxStillAllowsGenuineDecoderRecoveryButNotNetworkConversion() {
        assertEquals(PlaybackCompatibility.AUDIO_ONLY, playbackRecoveryCompatibility(
            PlaybackCompatibility.REMUX, PlaybackException.ERROR_CODE_DECODING_FAILED, false, false))
        assertEquals(PlaybackCompatibility.FULL, playbackRecoveryCompatibility(
            PlaybackCompatibility.REMUX, PlaybackException.ERROR_CODE_DECODING_FAILED, true, false))
        assertNull(playbackRecoveryCompatibility(PlaybackCompatibility.REMUX,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, false, false))
    }

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

    /**
     * The ladder, which used to be a cliff.
     *
     * One failure during direct play dropped straight to the conservative profile — H.264, 1080p,
     * stereo AAC, no stream copy — and stayed there for the rest of the film. A phone playing an
     * EAC3 5.1 film lost the picture to a re-encode and the surround to a 192 kbit/s downmix, to
     * fix what was almost always a sound problem.
     */
    @Test fun aSoundFailureCostsTheSoundAndNotThePicture() {
        assertEquals(PlaybackCompatibility.AUDIO_ONLY,
            nextPlaybackCompatibility(PlaybackCompatibility.DIRECT, videoFailed = false))
    }

    @Test fun aPictureThatFailedToDecodeGoesStraightToTheBottom() {
        assertEquals(PlaybackCompatibility.FULL,
            nextPlaybackCompatibility(PlaybackCompatibility.DIRECT, videoFailed = true))
    }

    @Test fun aSecondAudioFailureNeverCostsAWorkingPicture() {
        assertEquals(PlaybackCompatibility.AUDIO_ONLY,
            nextPlaybackCompatibility(PlaybackCompatibility.AUDIO_ONLY, videoFailed = false))
    }

    @Test fun theLadderEndsRatherThanLoops() {
        assertEquals(PlaybackCompatibility.FULL,
            nextPlaybackCompatibility(PlaybackCompatibility.FULL, videoFailed = false))
        assertEquals(PlaybackCompatibility.FULL,
            nextPlaybackCompatibility(PlaybackCompatibility.FULL, videoFailed = true))
        // Repeated audio failures must not escalate to video conversion.
        var step = PlaybackCompatibility.DIRECT
        val visited = mutableListOf(step)
        repeat(5) { step = nextPlaybackCompatibility(step, videoFailed = false); visited += step }
        assertEquals(listOf(PlaybackCompatibility.DIRECT, PlaybackCompatibility.AUDIO_ONLY,
            PlaybackCompatibility.AUDIO_ONLY, PlaybackCompatibility.AUDIO_ONLY, PlaybackCompatibility.AUDIO_ONLY,
            PlaybackCompatibility.AUDIO_ONLY), visited)
    }

    @Test fun networkAuthAndMissingFilesNeverAskForCodecConversion() {
        for (code in listOf(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, PlaybackException.ERROR_CODE_IO_NO_PERMISSION)) {
            assertFalse(playbackFailureNeedsConversion(code))
        }
        assertTrue(playbackFailureNeedsConversion(PlaybackException.ERROR_CODE_DECODING_FAILED))
        assertTrue(playbackFailureNeedsConversion(PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED))
    }
}
