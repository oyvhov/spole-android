package app.reelstack.player

import androidx.media3.common.PlaybackException
import org.junit.Assert.*
import org.junit.Test

class LocalAudioFallbackTest {
    private fun eligible(code: Int = PlaybackException.ERROR_CODE_DECODING_FAILED,
        renderer: String? = "MediaCodecAudioRenderer", mime: String? = "audio/eac3",
        encrypted: Boolean = false, supported: Boolean = true, tried: Boolean = false) =
        canRetryAudioLocally(code, renderer, mime, encrypted, supported, tried)

    @Test fun runtimeAudioFailureGetsOneLocalAttemptBeforeServerConversion() {
        assertTrue(eligible())
        assertTrue(eligible(code = PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
        assertFalse(eligible(tried = true))
    }
    @Test fun softwareFailureAndVideoFailureNeverLoopIntoSoftwareAudio() {
        assertFalse(eligible(renderer = "FfmpegAudioRenderer"))
        assertFalse(eligible(renderer = "MediaCodecVideoRenderer", mime = "video/avc"))
        assertFalse(eligible(renderer = null))
        assertFalse(eligible(mime = null))
    }
    @Test fun missingSoftwareOrEncryptedAudioKeepsExistingRecovery() {
        assertFalse(eligible(supported = false))
        assertFalse(eligible(encrypted = true))
    }
    @Test fun transportAndAudioOutputFailuresAreNotMisdiagnosedAsCodecFailures() {
        for (code in listOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED)) assertFalse(eligible(code = code))
    }
}
