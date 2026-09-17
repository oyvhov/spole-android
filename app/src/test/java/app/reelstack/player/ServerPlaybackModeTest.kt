package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Asking the server what it is doing, instead of reading it out of the URL it built.
 *
 * `VideoCodec=copy` in the transcoding URL is Jellyfin's spelling. Emby writes the target codec
 * there whether it re-encodes the picture or copies it straight through, so a URL read called every
 * Emby session a full transcode — in the overlay, and in the `PlayMethod` sent back to the server's
 * own dashboard. The session's `TranscodingInfo` is what both servers fill in from the decision
 * they actually made.
 */
class ServerPlaybackModeTest {
    private val connection = ServiceConnection(ServiceKind.EMBY, "Test", "https://media.example", "token", "u1")

    private class Sessions(private val json: String, private val code: Int = 200) : JsonHttpTransport {
        var url = ""
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            this.url = url
            return HttpResponse(code, json)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("Unexpected write")
    }

    private fun session(transcodingInfo: String?, device: String = "device-1", sourceId: String = "source-1") =
        """[{"Id":"s1","DeviceId":"$device","NowPlayingItem":{"Id":"film"},""" +
            """"PlayState":{"MediaSourceId":"$sourceId","PlayMethod":"Transcode"}""" +
            (transcodingInfo?.let { ""","TranscodingInfo":$it""" } ?: "") + "}]"

    private fun mode(json: String, code: Int = 200): PlaybackMode? {
        val transport = Sessions(json, code)
        return MediaPlaybackClient(transport, deviceId = "device-1").serverPlaybackMode(connection, "source-1")
    }

    @Test fun aCopiedPictureWithConvertedSoundIsNotAFullTranscodeHoweverEmbySpellsItsUrl() {
        assertEquals(PlaybackMode.AUDIO_TRANSCODE,
            mode(session("""{"VideoCodec":"h264","AudioCodec":"aac","IsVideoDirect":true,"IsAudioDirect":false}""")))
    }

    @Test fun bothStreamsCopiedIsARemux() {
        assertEquals(PlaybackMode.DIRECT_STREAM,
            mode(session("""{"IsVideoDirect":true,"IsAudioDirect":true}""")))
    }

    @Test fun aReEncodedPictureIsStillReportedAsOne() {
        assertEquals(PlaybackMode.FULL_TRANSCODE,
            mode(session("""{"VideoCodec":"h264","IsVideoDirect":false,"IsAudioDirect":false}""")))
    }

    @Test fun aServerThatHasNotOpenedTheSessionYetCorrectsNothing() {
        assertNull(mode(session(null)))
        assertNull(mode("[]"))
        assertNull(mode(""))
    }

    @Test fun aServerThatLeavesTheFlagsOutIsNotReadAsSayingNothingIsCopied() {
        assertNull(mode(session("""{"VideoCodec":"h264","AudioCodec":"aac"}""")))
    }

    @Test fun anotherDevicePlayingTheSameFileIsNotOurSession() {
        assertNull(mode(session("""{"IsVideoDirect":true,"IsAudioDirect":false}""", device = "someone-else")))
    }

    @Test fun ourOwnSessionStillCountsWhenTheServerNamesTheSourceDifferently() {
        // Emby leaves `MediaSourceId` out of `PlayState` on some versions; one session on our own
        // device with something playing is unambiguous enough to read.
        assertEquals(PlaybackMode.AUDIO_TRANSCODE,
            mode(session("""{"IsVideoDirect":true,"IsAudioDirect":false}""", sourceId = "other")))
    }

    @Test fun aUserWithoutSessionAccessGetsNoCorrectionRatherThanAFailedPlayback() {
        assertNull(mode(session("""{"IsVideoDirect":true,"IsAudioDirect":true}"""), code = 403))
    }

    @Test fun theSessionIsAskedForByDeviceAndTheIdIsEncoded() {
        val transport = Sessions(session("""{"IsVideoDirect":true,"IsAudioDirect":false}"""))
        MediaPlaybackClient(transport, deviceId = "device 1/2").serverPlaybackMode(connection, "source-1")
        assertTrue(transport.url.endsWith("/Sessions?DeviceId=device%201%2F2"))
    }
}
