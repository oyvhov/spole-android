package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class EmbyPlaybackTest {
    private val account = ServiceConnection(ServiceKind.EMBY, "Fixture", "https://media.example/emby", "fixture", "viewer")
    private val movie = PlayableItem("movie", "Film", "Movie")
    private class Fixture : JsonHttpTransport {
        val reads = mutableListOf<String>()
        val writes = mutableListOf<Pair<String, JsonObject>>()
        var user = "viewer"
        var allowed = true
        var body = """{"PlaySessionId":"session","MediaSources":[{"Id":"source","SupportsDirectPlay":true,"TranscodingUrl":"/emby/Videos/movie/master.m3u8?api_key=fixture&Quality=1","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":-1,"MediaStreams":[{"Index":1,"Type":"Audio","DisplayTitle":"Norsk"},{"Index":2,"Type":"Subtitle","Codec":"srt","Language":"nor"}]}]}"""
        var itemBody: String? = null
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            assertEquals("fixture", headers["X-Emby-Token"])
            assertFalse(url.contains("fixture"))
            reads += url
            return HttpResponse(200, itemBody ?: """{"Id":"$user","Policy":{"EnableMediaPlayback":$allowed}}""")
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            assertEquals("fixture", headers["X-Emby-Token"])
            assertTrue(headers.getValue("Authorization").contains("Client=\"Spole\""))
            assertFalse(url.contains("fixture"))
            writes += url to Json.parseToJsonElement(jsonBody).jsonObject
            return HttpResponse(200, body)
        }
    }

    @Test fun verifiesStoredEmbyUserAndNeverCallsUsersMe() {
        val fixture = Fixture()
        val client = MediaPlaybackClient(fixture, "device")
        assertEquals("viewer", client.verify(account))
        assertEquals(listOf("https://media.example/emby/Users/viewer"), fixture.reads)
        fixture.user = "other"
        assertTrue(runCatching { client.verify(account) }.isFailure)
        fixture.user = "viewer"; fixture.allowed = false
        assertTrue(runCatching { client.verify(account) }.isFailure)
        val count = fixture.reads.size
        assertTrue(runCatching { client.verify(account.copy(userId = "")) }.isFailure)
        assertEquals(count, fixture.reads.size)
    }

    @Test fun preparesSelectedVersionTracksAndReportsProgressToEmby() {
        val fixture = Fixture()
        val client = MediaPlaybackClient(fixture, "device")
        val plan = client.prepare(account, "viewer", movie, 4_000_000, audio = 1, subtitle = 2, sourceId = "source")
        assertTrue(plan.direct)
        assertEquals(2, plan.subtitleIndex)
        assertEquals("https://media.example/emby/Videos/movie/source/Subtitles/2/Stream.vtt", plan.subtitleUrl)
        val request = fixture.writes.single().second
        assertEquals("viewer", request.str("UserId"))
        assertEquals("source", request.str("MediaSourceId"))
        for (event in listOf("", "/Progress", "/Stopped")) client.report(account, plan, event, 12_345, true)
        assertTrue(fixture.writes.last().first.endsWith("Sessions/Playing/Stopped"))
        assertEquals(123_450_000L, fixture.writes.last().second.num("PositionTicks"))
        assertTrue(fixture.writes.last().second.flag("IsPaused"))
        val transcode = client.prepare(account, "viewer", movie, 4_000_000, compatible = true)
        assertFalse(transcode.direct)
        assertEquals("https://media.example/emby/Videos/movie/master.m3u8?Quality=1", transcode.url)
    }

    @Test fun imageSubtitlesRenegotiateForBurnIn() {
        val fixture = Fixture().apply { body = body.replace("\"srt\"", "\"pgssub\"") }
        val plan = MediaPlaybackClient(fixture, "device").prepare(account, "viewer", movie, 4_000_000, subtitle = 2)
        assertFalse(plan.direct)
        assertNull(plan.subtitleUrl)
        assertEquals(2, fixture.writes.size)
        assertEquals(JsonPrimitive(false), fixture.writes.last().second["EnableDirectPlay"])
    }

    @Test fun nextEpisodeCanCrossSeasonBoundaryAndLastEpisodeHasNoSuccessor() {
        val fixture = Fixture().apply { itemBody = """{"Items":[{"Id":"last","Type":"Episode","SeriesId":"show","ParentIndexNumber":1,"IndexNumber":10},{"Id":"next","Type":"Episode","SeriesId":"show","ParentIndexNumber":2,"IndexNumber":1}]}""" }
        val client = MediaPlaybackClient(fixture, "device")
        val current = PlayableItem("last", "Series", "Episode", seriesId = "show")
        assertEquals(2, client.nextEpisode(account, "viewer", current)?.season)
        assertNull(client.nextEpisode(account, "viewer", current.copy(id = "next")))
        assertTrue(fixture.reads.first().contains("adjacentTo=last"))
    }

    @Test fun readsEmbyMarkersWithoutCallingJellyfinPlugins() {
        val fixture = Fixture().apply { itemBody = """{"RunTimeTicks":600000000,"Chapters":[{"MarkerType":"IntroStart","StartPositionTicks":10000000},{"MarkerType":"IntroEnd","StartPositionTicks":100000000},{"MarkerType":"CreditsStart","StartPositionTicks":500000000}]}""" }
        val segments = MediaPlaybackClient(fixture, "device").segments(account, "viewer", "episode")
        assertEquals(listOf(PlaybackSegment(PlaybackSegment.Kind.INTRO, 1000, 10000),
            PlaybackSegment(PlaybackSegment.Kind.OUTRO, 50000, 60000)), segments)
        assertEquals(1, fixture.reads.size)
        assertTrue(fixture.reads.single().contains("Users/viewer/Items/episode?Fields=Chapters"))
        assertTrue(embyPlaybackSegments(Json.parseToJsonElement("""{"Chapters":[{"MarkerType":"IntroStart","StartPositionTicks":10000000}]}""").jsonObject).isEmpty())
    }
}
