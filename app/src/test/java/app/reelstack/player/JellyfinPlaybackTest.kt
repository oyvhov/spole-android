package app.reelstack.player

import app.reelstack.data.model.*
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class JellyfinPlaybackTest {
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example/jellyfin", "synthetic-token", "u1")
    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject
    private val movie = PlayableItem("film", "Testfilm", "Movie", durationMs = 20_000)
    private class Fixture : JsonHttpTransport {
        var user = "u1"
        var allowed = true
        var code = 200
        var body = """{"PlaySessionId":"session","MediaSources":[{"Id":"source","SupportsDirectPlay":true,"TranscodingUrl":"/jellyfin/Videos/film/master.m3u8?api_key=synthetic-token&x=1","DefaultAudioStreamIndex":1,"DefaultSubtitleStreamIndex":-1,"MediaStreams":[{"Index":1,"Type":"Audio","DisplayTitle":"Norsk"},{"Index":2,"Type":"Subtitle","Codec":"srt","Language":"nor","DisplayTitle":"Norsk tekst"}]}]}"""
        var posted = JsonObject(emptyMap())
        var path = ""
        override fun get(url: String, headers: Map<String,String>): HttpResponse {
            assertTrue(headers.getValue("Authorization").contains("Client=\"Spole\""))
            assertTrue(headers.getValue("Authorization").contains("synthetic-token"))
            return HttpResponse(code, """{"Id":"$user","Policy":{"EnableMediaPlayback":$allowed}}""")
        }
        override fun post(url: String, headers: Map<String,String>, jsonBody: String): HttpResponse {
            assertTrue(headers.getValue("Authorization").contains("Client=\"Spole\""))
            assertFalse(url.contains("synthetic-token"))
            posted = Json.parseToJsonElement(jsonBody).jsonObject; path = url
            return HttpResponse(code, body)
        }
    }
    @Test fun stripsCredentialsAndRetainsPlaybackParameters() {
        assertEquals("https://media.example/jellyfin/Videos/x?x=1&StartTimeTicks=0", safePlaybackUrl(connection.baseUrl,
            "/jellyfin/Videos/x?api_key=secret&x=1&TOKEN=secret&StartTimeTicks=0"))
    }
    @Test fun addsConfiguredBasePathToRelativeServerUrls() {
        assertEquals("https://media.example/jellyfin/Videos/x", safePlaybackUrl(connection.baseUrl, "/Videos/x"))
    }
    @Test fun refusesOtherHostsPortsSchemesAndUserInfo() {
        for (url in listOf("https://evil.example/video", "http://media.example/jellyfin/a", "https://media.example:8443/jellyfin/a",
            "//evil.example/a", "https://user@media.example/jellyfin/a", "file:///tmp/a", "https://media.example/other/a")) {
            assertTrue(url, runCatching { safePlaybackUrl(connection.baseUrl, url) }.isFailure)
        }
    }
    @Test fun refusesTraversalIncludingEncodedAndDoubleEncodedPaths() {
        for (url in listOf("../secret", "Videos/%2e%2e/a", "Videos/%252e%252e/a", "Videos/a\\b", "Videos/a#secret"))
            assertTrue(url, runCatching { safePlaybackUrl(connection.baseUrl, url) }.isFailure)
    }
    @Test fun parsesEpisodeAndAccurateResumeTicks() {
        val item = parsePlayable(obj("""{"Id":"ep","Type":"Episode","Name":"Ein ny dag","SeriesName":"Testserie","ParentIndexNumber":2,"IndexNumber":3,"RunTimeTicks":200000000,"UserData":{"PlaybackPositionTicks":50000000}}"""))
        assertEquals("Testserie", item.title); assertEquals("S02 E03 · Ein ny dag", item.subtitle); assertEquals(5000L,item.resumeMs)
    }
    @Test fun rejectsVirtualAndMissingEpisodes() {
        for (extra in listOf("\"IsMissing\":true", "\"LocationType\":\"Virtual\""))
            assertTrue(runCatching { parsePlayable(obj("""{"Id":"ep","Type":"Episode",$extra}""")) }.isFailure)
    }
    @Test fun watchedAndOutOfRangeResumeRestartAtZero() {
        assertEquals(0L,parsePlayable(obj("""{"Id":"film","Type":"Movie","RunTimeTicks":10,"UserData":{"Played":true,"PlaybackPositionTicks":90000000}}""")).resumeMs)
    }
    @Test fun verifiesPersonalIdentityAndPlaybackPolicy() {
        val fixture=Fixture(); val client=JellyfinPlaybackClient(fixture,"device")
        assertEquals("u1",client.verify(connection))
        fixture.user="someone-else"; assertTrue(runCatching { client.verify(connection) }.isFailure)
        fixture.user="u1"; fixture.allowed=false; assertTrue(runCatching { client.verify(connection) }.isFailure)
    }
    @Test fun verifiedPlaybackDoesNotAdvertiseUnimplementedRemoteControls() {
        val fixture = Fixture()
        JellyfinPlaybackClient(fixture, "device").verify(connection)
        assertTrue(fixture.path.endsWith("/Sessions/Capabilities/Full"))
        assertEquals(JsonPrimitive(false), fixture.posted["SupportsMediaControl"])
        assertTrue(fixture.posted.getValue("SupportedCommands").jsonArray.isEmpty())
        assertEquals(listOf(JsonPrimitive("Video")), fixture.posted.getValue("PlayableMediaTypes").jsonArray.toList())
    }
    @Test fun selectsDirectPlayAndKeepsSecretsOutOfPlanString() {
        val plan=JellyfinPlaybackClient(Fixture(),"device").prepare(connection,"u1",movie,4_000_000)
        assertTrue(plan.direct); assertFalse(plan.url.contains("synthetic-token")); assertFalse(plan.toString().contains("https"))
        assertEquals("source",plan.sourceId)
    }
    @Test fun fallbackNegotiatesCompatibilityWithoutVideoStreamCopy() {
        val fixture=Fixture(); val plan=JellyfinPlaybackClient(fixture,"device").prepare(connection,"u1",movie,2_000_000,compatible=true)
        assertFalse(plan.direct); assertFalse(plan.url.contains("api_key"))
        assertEquals(JsonPrimitive(false),fixture.posted["AllowVideoStreamCopy"])
        assertEquals(JsonPrimitive(0),fixture.posted["StartTimeTicks"])
    }
    @Test fun explicitTextUsesAuthenticatedVttAndCanBeDisabled() {
        val client=JellyfinPlaybackClient(Fixture(),"device")
        val plan=client.prepare(connection,"u1",movie,4_000_000,subtitle=2)
        assertTrue(plan.subtitleUrl!!.endsWith("/Subtitles/2/Stream.vtt")); assertEquals(2,plan.subtitleIndex)
        assertNull(client.prepare(connection,"u1",movie,4_000_000,subtitle=-1).subtitleUrl)
    }
    @Test fun noFallbackUrlOrSessionFailsClosed() {
        val fixture=Fixture(); fixture.body="""{"MediaSources":[]}"""
        assertTrue(runCatching { JellyfinPlaybackClient(fixture,"device").prepare(connection,"u1",movie,4_000_000) }.isFailure)
    }
    @Test fun reportsAccuratePositionAndOnlyOwnSessionNotRemoteCommands() {
        val fixture=Fixture(); val client=JellyfinPlaybackClient(fixture,"device")
        val plan=client.prepare(connection,"u1",movie,4_000_000)
        client.report(connection,plan,"/Stopped",12_345,true)
        assertTrue(fixture.path.endsWith("Sessions/Playing/Stopped"))
        assertEquals(JsonPrimitive(123450000L),fixture.posted["PositionTicks"])
        assertEquals(JsonPrimitive("session"),fixture.posted["PlaySessionId"])
        assertNull(fixture.posted["SessionId"])
    }
    @Test fun profileAndClockAreConservative() {
        val profile=phonePlaybackProfile(4_000_000)
        assertEquals(JsonPrimitive("h264"), profile.objects("DirectPlayProfiles").first()["VideoCodec"])
        assertEquals("1:02:03",playbackTime(3_723_000)); assertEquals("0:00",playbackTime(-1))
    }
}
