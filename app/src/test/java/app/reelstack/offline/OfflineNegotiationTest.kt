package app.reelstack.offline

import androidx.media3.exoplayer.scheduler.Requirements
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.player.MediaPlaybackClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineNegotiationTest {
    /** A Blu-ray rip: direct playable, with an image subtitle as the server's default. */
    private class Server : JsonHttpTransport {
        val playbackInfo = mutableListOf<JsonObject>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse = when {
            url.endsWith("/Users/Me") -> HttpResponse(200, """{"Id":"viewer","Policy":{}}""")
            url.contains("/Users/viewer/Items/movie") ->
                HttpResponse(200, """{"Id":"movie","Name":"Fixture","Type":"Movie","RunTimeTicks":72000000000}""")
            else -> error("Unexpected read $url")
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = when {
            url.endsWith("/Sessions/Capabilities/Full") -> HttpResponse(204, "")
            url.endsWith("/Items/movie/PlaybackInfo") -> {
                playbackInfo += Json.parseToJsonElement(jsonBody).jsonObject
                HttpResponse(200, """{"PlaySessionId":"session","MediaSources":[{
                    "Id":"source","SupportsDirectPlay":true,"SupportsDirectStream":true,
                    "DirectStreamUrl":"/Videos/movie/stream.mkv?Static=true&api_key=secret",
                    "DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":3,"MediaStreams":[
                    {"Type":"Video","Index":0,"Codec":"hevc"},
                    {"Type":"Audio","Index":1,"Codec":"eac3","Channels":6},
                    {"Type":"Subtitle","Index":3,"Codec":"pgssub","IsTextSubtitleStream":false,"Language":"nor"}]}]}""")
            }
            else -> error("Unexpected write $url")
        }
    }

    private val account = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://media.example", "token", "viewer")

    @Test fun anImageSubtitleDoesNotTurnTheOriginalFileIntoARefusal() {
        val server = Server()
        val request = negotiateOfflineDownload(MediaPlaybackClient(server, "device"), "adult", account, "movie")

        assertFalse(request.candidate.requiresTranscode)
        assertNull(request.candidate.offlineRefusal())
        assertEquals("https://media.example/Videos/movie/stream.mkv?Static=true", request.candidate.directDownloadUrl)
        // One negotiation: no burn-in retry for the default PGS track.
        assertEquals(1, server.playbackInfo.size)
        assertEquals(-1, server.playbackInfo.single()["SubtitleStreamIndex"]!!.jsonPrimitive.int)
    }

    @Test fun aDownloadIsNotLimitedByAStreamingBitrate() {
        val server = Server()
        negotiateOfflineDownload(MediaPlaybackClient(server, "device"), "adult", account, "movie")

        val sent = server.playbackInfo.single()
        assertEquals(OFFLINE_NEGOTIATION_BITRATE, sent["MaxStreamingBitrate"]!!.jsonPrimitive.int)
        assertTrue(OFFLINE_NEGOTIATION_BITRATE > 120_000_000)
    }

    @Test fun aJobQueuedAtHomeFollowsTheAccountToItsOtherAddress() {
        val moved = account.copy(baseUrl = "https://media.example/jellyfin", alternateUrl = "http://192.168.1.20:8096")
        assertEquals(
            "https://media.example/jellyfin/Videos/movie/stream.mkv?Static=true&MediaSourceId=source",
            offlineRebasedUrl(moved, "http://192.168.1.20:8096/Videos/movie/stream.mkv?Static=true&MediaSourceId=source"),
        )
    }

    @Test fun aJobAlreadyOnTheCurrentAddressOrOnAForeignHostIsLeftAlone() {
        val moved = account.copy(alternateUrl = "http://192.168.1.20:8096")
        assertNull(offlineRebasedUrl(moved, "https://media.example/Videos/movie/stream.mkv?Static=true"))
        assertNull(offlineRebasedUrl(moved, "https://elsewhere.example/Videos/movie/stream.mkv?Static=true"))
        assertNull(offlineRebasedUrl(account, "http://192.168.1.20:8096/Videos/movie/stream.mkv"))
    }

    @Test fun wifiOnlyMeansAnUnmeteredNetwork() {
        assertTrue(offlineRequirements(wifiOnly = true).isUnmeteredNetworkRequired)
        assertFalse(offlineRequirements(wifiOnly = false).isUnmeteredNetworkRequired)
        assertTrue(offlineRequirements(wifiOnly = false).isNetworkRequired)
    }
}
