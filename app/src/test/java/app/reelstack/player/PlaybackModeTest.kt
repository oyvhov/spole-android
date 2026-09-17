package app.reelstack.player

import app.reelstack.R
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Four modes, not two.
 *
 * The plan used to carry a single `direct` flag, so copying the picture and converting only the
 * sound reported exactly the same thing as re-encoding the picture — and the server's own dashboard
 * was told "Transcode" for both. These tests pin the four cases against the shape of URL each
 * server actually builds.
 */
class PlaybackModeTest {
    private fun source(transcodingUrl: String = "", reasons: JsonElement? = null) = buildJsonObject {
        put("Id", "source-1")
        put("TranscodingUrl", transcodingUrl)
        reasons?.let { put("TranscodeReasons", it) }
    }

    @Test fun directPlayIsWhateverTheServerSaidItCouldSendUntouched() {
        assertEquals(PlaybackMode.DIRECT_PLAY, playbackModeFor(source(), direct = true))
        assertTrue(PlaybackMode.DIRECT_PLAY.copiesVideo)
    }

    @Test fun bothStreamsCopiedIsARemuxAndNotATranscode() {
        val url = "/videos/1/master.m3u8?VideoCodec=copy&AudioCodec=copy&PlaySessionId=x"
        assertEquals(PlaybackMode.DIRECT_STREAM, playbackModeFor(source(url), direct = false))
    }

    @Test fun copiedPictureWithConvertedSoundIsTheCommonTvCase() {
        val url = "/videos/1/master.m3u8?VideoCodec=copy&AudioCodec=aac&AudioChannels=2"
        val mode = playbackModeFor(source(url), direct = false)
        assertEquals(PlaybackMode.AUDIO_TRANSCODE, mode)
        assertTrue("audio-only conversion still copies the picture", mode.copiesVideo)
    }

    @Test fun reEncodingThePictureIsTheOnlyModeThatDoesNotCopyVideo() {
        val url = "/videos/1/master.m3u8?VideoCodec=h264&AudioCodec=aac"
        val mode = playbackModeFor(source(url), direct = false)
        assertEquals(PlaybackMode.FULL_TRANSCODE, mode)
        assertFalse(mode.copiesVideo)
    }

    @Test fun serversSpellTheQueryKeysDifferentlyAndBothAreRead() {
        val lower = "/videos/1/stream.m3u8?videocodec=copy&audiocodec=copy"
        assertEquals(PlaybackMode.DIRECT_STREAM, playbackModeFor(source(lower), direct = false))
    }

    @Test fun reasonsAreReadFromAnArrayAStringOrTheUrl() {
        val array = source(reasons = buildJsonArray { add("AudioCodecNotSupported") })
        assertEquals(listOf("AudioCodecNotSupported"), playbackTranscodeReasons(array))

        val csv = source(reasons = JsonPrimitive("ContainerNotSupported, AudioCodecNotSupported"))
        assertEquals(listOf("ContainerNotSupported", "AudioCodecNotSupported"), playbackTranscodeReasons(csv))

        val inUrl = source("/videos/1/master.m3u8?TranscodeReasons=VideoCodecNotSupported")
        assertEquals(listOf("VideoCodecNotSupported"), playbackTranscodeReasons(inUrl))

        assertEquals(emptyList<String>(), playbackTranscodeReasons(source()))
    }

    @Test fun theReasonShownIsTheOneThatDecidesHowMuchWorkTheServerDoes() {
        // Both are true; re-encoding the picture is the one that costs the server, so it wins.
        assertEquals(R.string.player_reason_video_codec,
            playbackReasonLabel(listOf("AudioCodecNotSupported", "VideoCodecNotSupported")))
        assertEquals(R.string.player_reason_audio_channels,
            playbackReasonLabel(listOf("AudioChannelsNotSupported")))
        assertEquals(R.string.player_reason_subtitle,
            playbackReasonLabel(listOf("SubtitleCodecNotSupported")))
        assertEquals(R.string.player_reason_video_range,
            playbackReasonLabel(listOf("VideoRangeTypeNotSupported")))
    }

    @Test fun anUnknownCodeBecomesASentenceRatherThanRawCamelCase() {
        assertEquals(R.string.player_reason_other, playbackReasonLabel(listOf("SomethingNobodyHasSeenYet")))
        assertNull(playbackReasonLabel(emptyList()))
    }

    @Test fun aDirectPlayingTitleHasNothingToExplain() {
        val direct = PlayerScreenState(mode = PlaybackMode.DIRECT_PLAY, transcodeReasons = listOf("AudioCodecNotSupported"))
        assertNull(playbackReasonFor(direct))
        assertTrue(direct.direct)

        val converted = PlayerScreenState(mode = PlaybackMode.AUDIO_TRANSCODE, transcodeReasons = listOf("AudioCodecNotSupported"))
        assertEquals(R.string.player_reason_audio_codec, playbackReasonFor(converted))
        assertFalse(converted.direct)
    }

    @Test fun everyModeHasItsOwnLine() {
        val labels = PlaybackMode.entries.map { playbackModeLabel(it) }
        assertEquals("each mode needs its own sentence", labels.size, labels.toSet().size)
    }

    @Test fun queryValuesSurviveEncodingAndMissingKeys() {
        assertEquals("", playbackUrlValue("/videos/1/stream", "VideoCodec"))
        assertEquals("", playbackUrlValue("/videos/1/stream?AudioCodec=aac", "VideoCodec"))
        assertEquals("h264,hevc", playbackUrlValue("/v?VideoCodec=h264%2Chevc", "VideoCodec"))
    }
}
