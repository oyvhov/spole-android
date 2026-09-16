package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class EmbyPlaybackTest {
    private val account = ServiceConnection(ServiceKind.EMBY, "Fixture", "https://media.example/emby", "fixture", "viewer")
    private val movie = PlayableItem("movie", "Film", "Movie")
    private class Fixture(private val kind: ServiceKind = ServiceKind.EMBY) : JsonHttpTransport {
        val reads = mutableListOf<String>()
        val writes = mutableListOf<Pair<String, JsonObject>>()
        var postCalls = 0
        var user = "viewer"
        var allowed = true
        var transientPlaybackFailures = 0
        var body = """{"PlaySessionId":"session","MediaSources":[{"Id":"source","SupportsDirectPlay":true,"TranscodingUrl":"/emby/Videos/movie/master.m3u8?api_key=fixture&Quality=1","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":-1,"MediaStreams":[{"Index":1,"Type":"Audio","DisplayTitle":"Norsk"},{"Index":2,"Type":"Subtitle","Codec":"srt","Language":"nor"}]}]}"""
        var itemBody: String? = null
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            assertEquals(if (kind == ServiceKind.EMBY) "fixture" else null, headers["X-Emby-Token"])
            assertEquals(if (kind == ServiceKind.EMBY) "Emby UserId=\"viewer\", Client=\"Spole\", Device=\"Android\", DeviceId=\"device\", Version=\"${app.reelstack.BuildConfig.VERSION_NAME}\"" else null,
                headers["X-Emby-Authorization"])
            assertFalse(url.contains("fixture"))
            reads += url
            return HttpResponse(200, itemBody ?: """{"Id":"$user","Policy":{"EnableMediaPlayback":$allowed}}""")
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            postCalls++
            assertEquals(if (kind == ServiceKind.EMBY) "fixture" else null, headers["X-Emby-Token"])
            assertEquals(if (kind == ServiceKind.EMBY) "Emby UserId=\"viewer\", Client=\"Spole\", Device=\"Android\", DeviceId=\"device\", Version=\"${app.reelstack.BuildConfig.VERSION_NAME}\"" else null,
                headers["X-Emby-Authorization"])
            if (kind == ServiceKind.JELLYFIN) {
                assertTrue(headers.getValue("Authorization").contains("Client=\"Spole\""))
                assertTrue(headers.getValue("Authorization").contains("Token=\"fixture\""))
            }
            assertFalse(url.contains("fixture"))
            if (transientPlaybackFailures > 0) {
                transientPlaybackFailures--
                return HttpResponse(503, "")
            }
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

    @Test fun preferredLanguageIsNegotiatedWithBothMediaServers() {
        for (kind in listOf(ServiceKind.EMBY, ServiceKind.JELLYFIN)) {
            val fixture = Fixture(kind)
            val plan = MediaPlaybackClient(fixture, "device").prepare(account.copy(kind = kind), "viewer", movie,
                4_000_000, preferredLanguage = SubtitleLanguage.NORWEGIAN, fallbackLanguage = SubtitleLanguage.ENGLISH)
            assertEquals(2, plan.subtitleIndex)
            assertEquals(2, fixture.writes.size)
            val request = fixture.writes.last().second
            assertEquals(2L, request.num("SubtitleStreamIndex"))
            assertEquals(1L, request.num("AudioStreamIndex"))
            assertEquals("source", request.str("MediaSourceId"))
        }
    }

    @Test fun fallbackAndExplicitOffOverrideServerChoice() {
        val fixture = Fixture().apply { body = body.replace("\"nor\"", "\"eng\"") }
        val client = MediaPlaybackClient(fixture, "device")
        assertEquals(2, client.prepare(account, "viewer", movie, 4_000_000,
            preferredLanguage = SubtitleLanguage.NORWEGIAN, fallbackLanguage = SubtitleLanguage.ENGLISH).subtitleIndex)
        fixture.writes.clear()
        assertEquals(-1, client.prepare(account, "viewer", movie, 4_000_000, subtitle = -1,
            preferredLanguage = SubtitleLanguage.NORWEGIAN, fallbackLanguage = SubtitleLanguage.ENGLISH).subtitleIndex)
        assertEquals(1, fixture.writes.size)
        assertEquals(2, client.prepare(account, "viewer", movie, 4_000_000, subtitle = 2,
            preferredLanguage = SubtitleLanguage.NONE).subtitleIndex)
    }

    @Test fun imageSubtitlesRenegotiateForBurnIn() {
        val fixture = Fixture().apply { body = body.replace("\"srt\"", "\"pgssub\"") }
        val plan = MediaPlaybackClient(fixture, "device").prepare(account, "viewer", movie, 4_000_000, subtitle = 2)
        assertFalse(plan.direct)
        assertNull(plan.subtitleUrl)
        assertEquals(2, fixture.postCalls)
        assertEquals(JsonPrimitive(false), fixture.writes.last().second["EnableDirectPlay"])
    }

    @Test fun ordinaryLibraryVideosUseTheSamePlaybackNegotiation() {
        val item = parsePlayable(Json.parseToJsonElement("""{"Id":"movie","Name":"Video","Type":"Video","RunTimeTicks":600000000}""").jsonObject)
        assertEquals("Video", item.type)
        assertEquals(60_000L, item.durationMs)
        val plan = MediaPlaybackClient(Fixture(), "device").prepare(account, "viewer", item, 4_000_000)
        assertTrue(plan.direct)
        assertEquals("Video", plan.item.type)
    }

    @Test fun embyPlaybackInfoRetriesATransientServerFailure() {
        val fixture = Fixture().apply { transientPlaybackFailures = 1 }
        val plan = MediaPlaybackClient(fixture, "device").prepare(account, "viewer", movie, 4_000_000)
        assertTrue(plan.direct)
        assertEquals(2, fixture.postCalls)
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
