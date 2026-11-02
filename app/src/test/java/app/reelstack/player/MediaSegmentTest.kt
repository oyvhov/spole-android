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

    /** Recap and preview are marked by the same route and read the same way. */
    @Test
    fun `recap and preview are read next to intro and outro`() {
        val body = """{"Items":[{"Type":"Recap","StartTicks":0,"EndTicks":450000000},
            {"Type":"Intro","StartTicks":450000000,"EndTicks":900000000},
            {"Type":"Preview","StartTicks":25000000000,"EndTicks":25400000000},
            {"Type":"Outro","StartTicks":25400000000,"EndTicks":26000000000}]}"""
        val segments = client(mapOf("MediaSegments/" to body)).segments(connection, "u1", "e1")
        assertEquals(
            listOf(PlaybackSegment.Kind.RECAP, PlaybackSegment.Kind.INTRO, PlaybackSegment.Kind.PREVIEW, PlaybackSegment.Kind.OUTRO),
            segments.map { it.kind },
        )
        assertEquals(PlaybackSegment(PlaybackSegment.Kind.PREVIEW, 2_500_000, 2_540_000), segments[2])
    }

    /** Commercials and anything a newer server invents are left out without losing the rest. */
    @Test
    fun `commercials and unknown types are dropped but the known ones stay`() {
        val body = """{"Items":[{"Type":"Commercial","StartTicks":0,"EndTicks":900000000},
            {"Type":"Unknown","StartTicks":0,"EndTicks":900000000},
            {"Type":"Banana","StartTicks":0,"EndTicks":900000000},
            {"Type":"Recap","StartTicks":0,"EndTicks":450000000}]}"""
        val segments = client(mapOf("MediaSegments/" to body)).segments(connection, "u1", "e1")
        assertEquals(listOf(PlaybackSegment.Kind.RECAP), segments.map { it.kind })
    }

    @Test
    fun `the server is asked for all four kinds and for no commercials`() {
        val transport = Routes(mapOf("MediaSegments/" to """{"Items":[]}"""))
        JellyfinPlaybackClient(deviceId = "d", transport = transport).segments(connection, "u1", "e1")
        val asked = transport.requested.first { it.contains("MediaSegments/") }
        for (type in listOf("Intro", "Outro", "Recap", "Preview")) assertTrue(type, asked.contains("includeSegmentTypes=$type"))
        assertTrue(!asked.contains("Commercial"))
    }

    /**
     * The offer appears inside the stretch and stops a little before the end, so it neither
     * flickers around the boundary nor shows up for the last half second of a title sequence.
     */
    @Test
    fun `the offer follows the playhead`() {
        val state = PlayerScreenState(
            segments = listOf(PlaybackSegment(PlaybackSegment.Kind.INTRO, 5_000, 90_000)),
        )
        fun kindAt(position: Long) = (state.copy(positionMs = position).segmentOffer(controlsVisible = true)
            ?.action as? SegmentAction.Skip)?.kind
        assertEquals(null, kindAt(4_000))
        assertEquals(PlaybackSegment.Kind.INTRO, kindAt(5_000))
        assertEquals(PlaybackSegment.Kind.INTRO, kindAt(80_000))
        assertEquals(null, kindAt(89_000))
        assertEquals(null, kindAt(120_000))
    }
}
