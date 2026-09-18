package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.player.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackCapabilitiesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun localFallbackIsBoundedAndResetsForANewTitle() {
        val fallback = LocalAudioFallback()
        val format = androidx.media3.common.Format.Builder().setSampleMimeType("audio/eac3")
            .setChannelCount(6).setSampleRate(48000).build()
        val error = androidx.media3.exoplayer.ExoPlaybackException.createForRenderer(
            IllegalStateException("synthetic"), "MediaCodecAudioRenderer", 1, format,
            androidx.media3.common.C.FORMAT_HANDLED, null, false,
            androidx.media3.common.PlaybackException.ERROR_CODE_DECODING_FAILED)
        assertFalse(fallback.usesSoftware(format))
        assertTrue(fallback.tryEnable(error))
        assertTrue(fallback.usesSoftware(format))
        assertFalse(fallback.tryEnable(error))
        assertFalse(fallback.usesSoftware(format.buildUpon().setSampleMimeType("audio/ac3").build()))
        fallback.reset()
        assertFalse(fallback.usesSoftware(format))
        assertTrue(fallback.tryEnable(error))
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun bundledAudioDecoderReallyLoadsAndAdvertisesMultichannelSupport() {
        assertTrue(androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable())
        val audio = AndroidPlaybackCapabilities(context).snapshot().audio
        for (codec in listOf("dts", "ac3", "eac3", "truehd", "flac", "alac")) {
            assertTrue("Missing local $codec support", audio.any { it.codec == codec && it.channels >= 8 })
        }
    }
    @Test fun detectsRealPlatformCapabilitiesAndExportsOnlyNonSecretDiagnostics() {
        val snapshot = AndroidPlaybackCapabilities(context).snapshot()
        assertTrue(snapshot.video.any { it.codec == "h264" })
        assertTrue(snapshot.audio.any { it.codec == "aac" })
        val profile = devicePlaybackProfile(120_000_000, snapshot)
        java.io.File(context.getExternalFilesDir(null), "playback-capabilities.json").writeText(profile.toString())
        assertTrue(snapshot.video.all { it.width > 0 && it.height > 0 })
    }
    @Test fun verifiesConcreteAvcSourceAndRejectsUnknownHdrAndImpossibleDimensions() {
        val detector = AndroidPlaybackCapabilities(context)
        fun source(width: Int = 640, range: String = "SDR") = Json.parseToJsonElement("""{"MediaStreams":[
            {"Type":"Video","Codec":"h264","Profile":"baseline","Width":$width,"Height":360,"BitDepth":8,"AverageFrameRate":24,"VideoRangeType":"$range"},
            {"Type":"Audio","Codec":"aac","Channels":2,"SampleRate":48000,"Index":1}]}""").jsonObject
        assertTrue(detector.canDirectPlay(source(),1))
        assertFalse(detector.canDirectPlay(source(32000),1))
        assertFalse(detector.canDirectPlay(source(range="DOVI"),1))
        assertFalse(detector.canDirectPlay(source(),99))
    }
}
