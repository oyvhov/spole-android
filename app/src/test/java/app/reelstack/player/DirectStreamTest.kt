package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DirectStreamTest {
    private val item = PlayableItem("movie", "Fixture", "Movie")
    private class Server(val directUrl: String, val supported: Boolean = true) : JsonHttpTransport {
        val calls = mutableListOf<JsonObject>()
        override fun get(url: String, headers: Map<String, String>) = error("Unexpected read")
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            calls += Json.parseToJsonElement(jsonBody).jsonObject
            return HttpResponse(200, """{"PlaySessionId":"session","MediaSources":[{
                "Id":"source","SupportsDirectPlay":false,"SupportsDirectStream":$supported,
                "DirectStreamUrl":"$directUrl","TranscodingUrl":"/Videos/movie/master.m3u8?VideoCodec=copy&AudioCodec=aac",
                "DefaultAudioStreamIndex":1,"MediaStreams":[{"Type":"Audio","Index":1}]}]}""")
        }
    }
    @Test fun bothServersCanOfferAnUntouchedHttpFileWithoutFilesystemDirectPlay() {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            val server = Server("/Videos/movie/stream.mkv?Static=true&api_key=fixture")
            val account = ServiceConnection(kind, "Fixture", "https://media.example", "fixture", "viewer")
            val plan = MediaPlaybackClient(server, "device").prepare(account, "viewer", item, 120_000_000)
            assertTrue(plan.direct)
            assertEquals("https://media.example/Videos/movie/stream.mkv?Static=true", plan.url)
            assertEquals(1, server.calls.size)
        }
    }
    @Test fun playlistsAndUnapprovedUrlsAreNeverClassifiedAsOriginalFiles() {
        for (server in listOf(Server("/master.m3u8?Static=true"), Server("/stream.mkv"),
            Server("/stream.mkv?Static=true", supported = false))) {
            val account = ServiceConnection(ServiceKind.EMBY, "Fixture", "https://media.example", "fixture", "viewer")
            assertFalse(MediaPlaybackClient(server, "device").prepare(account, "viewer", item, 120_000_000).direct)
        }
    }
    @Test fun directStreamStillChecksTheSelectedAudioAndKeepsTheFallbackState() {
        val server = Server("/Videos/movie/stream.mkv?Static=true")
        val account = ServiceConnection(ServiceKind.EMBY, "Fixture", "https://media.example", "fixture", "viewer")
        val plan = MediaPlaybackClient(server, "device", sourceSupported = { _, _ -> false })
            .prepare(account, "viewer", item, 120_000_000)
        assertEquals(PlaybackCompatibility.AUDIO_ONLY, plan.compatibility)
        assertEquals(PlaybackMode.AUDIO_TRANSCODE, plan.mode)
        assertEquals(2, server.calls.size)
        assertTrue(server.calls.last().flag("AllowVideoStreamCopy"))
    }
}
