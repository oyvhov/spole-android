package app.reelstack.player

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Knowing where the title sequence is.
 *
 * Two servers answer this question. Jellyfin 10.10 does it itself in ticks; before that the Intro
 * Skipper plugin did it in seconds under a different address. Both are asked, in that order,
 * because a household that installed the plugin years ago should not lose the one feature they
 * installed it for by updating this app. Neither answering is the normal case — most libraries have
 * never been scanned — so every failure is silent and the player simply offers nothing.
 */
class MediaSegmentTest {

    private class Routes(private val answers: Map<String, String>) : JsonHttpTransport {
        val requested = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            requested += url
            val body = answers.entries.firstOrNull { url.contains(it.key) }?.value
            return if (body == null) HttpResponse(statusCode = 404, body = "") else HttpResponse(200, body)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(204, "")
        override fun delete(url: String, headers: Map<String, String>) = HttpResponse(204, "")
    }

    private val connection = ServiceConnection(
        kind = ServiceKind.JELLYFIN, name = "Jellyfin", baseUrl = "https://media.example",
        token = "t", userId = "u1", state = ConnectionState.CONNECTED,
    )

    private fun client(answers: Map<String, String>) =
        JellyfinPlaybackClient(deviceId = "d", transport = Routes(answers))

    @Test
    fun `the modern route is read in ticks`() {
        val body = """{"Items":[{"Type":"Intro","StartTicks":0,"EndTicks":900000000},
            {"Type":"Outro","StartTicks":25000000000,"EndTicks":26000000000}]}"""
        val segments = client(mapOf("MediaSegments/" to body)).segments(connection, "u1", "e1")
        assertEquals(2, segments.size)
        assertEquals(PlaybackSegment.Kind.INTRO, segments.first().kind)
        assertEquals(0L, segments.first().startMs)
        assertEquals(90_000L, segments.first().endMs)
        assertEquals(PlaybackSegment.Kind.OUTRO, segments.last().kind)
    }

    @Test
    fun `the plugin route is read in seconds when the modern one says nothing`() {
        val answers = mapOf(
            "MediaSegments/" to """{"Items":[]}""",
            "IntroTimestamps" to """{"Valid":true,"IntroStart":12.5,"IntroEnd":95.0}""",
        )
        val segments = client(answers).segments(connection, "u1", "e1")
        assertEquals(1, segments.size)
        assertEquals(12_500L, segments.single().startMs)
        assertEquals(95_000L, segments.single().endMs)
    }

    /** The plugin's own way of saying it found nothing here. */
    @Test
    fun `an invalid plugin answer produces nothing`() {
        val answers = mapOf(
            "MediaSegments/" to """{"Items":[]}""",
            "IntroTimestamps" to """{"Valid":false,"IntroStart":0.0,"IntroEnd":0.0}""",
        )
        assertTrue(client(answers).segments(connection, "u1", "e1").isEmpty())
    }

    @Test
    fun `a server that knows neither route answers with nothing`() {
        assertTrue(client(emptyMap()).segments(connection, "u1", "e1").isEmpty())
    }

    /** A zero-length mark is not a stretch anyone can skip. */
    @Test
    fun `an empty stretch is discarded`() {
        val body = """{"Items":[{"Type":"Intro","StartTicks":900000000,"EndTicks":900000000}]}"""
        assertTrue(client(mapOf("MediaSegments/" to body)).segments(connection, "u1", "e1").isEmpty())
    }

    @Test
    fun `a type nobody offers to skip is ignored`() {
        val body = """{"Items":[{"Type":"Commercial","StartTicks":0,"EndTicks":900000000}]}"""
        assertTrue(client(mapOf("MediaSegments/" to body)).segments(connection, "u1", "e1").isEmpty())
    }

    /**
     * The offer appears inside the stretch and stops a little before the end, so it neither
     * flickers around the boundary nor shows up for the last half second of a title sequence.
     */
    @Test
    fun `the active stretch follows the playhead`() {
        val state = PlayerScreenState(
            segments = listOf(PlaybackSegment(PlaybackSegment.Kind.INTRO, 5_000, 90_000)),
        )
        assertEquals(null, state.copy(positionMs = 4_000).activeSegment())
        assertEquals(PlaybackSegment.Kind.INTRO, state.copy(positionMs = 5_000).activeSegment()?.kind)
        assertEquals(PlaybackSegment.Kind.INTRO, state.copy(positionMs = 80_000).activeSegment()?.kind)
        assertEquals(null, state.copy(positionMs = 89_000).activeSegment())
        assertEquals(null, state.copy(positionMs = 120_000).activeSegment())
    }
}
