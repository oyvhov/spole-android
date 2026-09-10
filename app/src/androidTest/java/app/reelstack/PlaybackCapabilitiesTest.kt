package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.player.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackCapabilitiesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
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
