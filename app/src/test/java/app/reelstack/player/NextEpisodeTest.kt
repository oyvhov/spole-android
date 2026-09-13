package app.reelstack.player

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What plays after an episode finishes.
 *
 * The server is asked with `adjacentTo`, which answers with the previous, the current and the next
 * in one request. That matters at a season boundary: the last episode of season one is followed by
 * the first of season two, and a client that counted index numbers within a season would have
 * stopped there and told the viewer the series was over.
 */
class NextEpisodeTest {
    @Test fun countdownStartsBeforeEndAndStopsForPauseSeekOrDismissal() {
        val ready = PlayerScreenState(busy = false, playing = true, positionMs = 1_740_000,
            durationMs = 1_800_000, nextEpisode = PlayableItem("next", "Series", "Episode"))
        org.junit.Assert.assertTrue(ready.canCountDownNextEpisode())
        org.junit.Assert.assertFalse(ready.copy(positionMs = 1_739_999).canCountDownNextEpisode())
        org.junit.Assert.assertFalse(ready.copy(playing = false).canCountDownNextEpisode())
        org.junit.Assert.assertFalse(ready.copy(nextEpisodeDismissed = true).canCountDownNextEpisode())
        org.junit.Assert.assertFalse(ready.copy(nextEpisodeOfferEnabled = false).canCountDownNextEpisode())
        org.junit.Assert.assertFalse(ready.copy(error = "offline").canCountDownNextEpisode())
        org.junit.Assert.assertTrue(ready.copy(playing = false, ended = true,
            nextEpisodeOfferEnabled = false).canCountDownNextEpisode())
    }

    @Test fun offerStartsAtConfiguredBoundaryAndNeverWithUnknownRuntime() {
        fun offer(position: Long, duration: Long = 1_800_000, lead: Int = 60) =
            shouldOfferNextEpisode(true, true, false, false, position, duration, lead)
        org.junit.Assert.assertFalse(offer(1_739_999))
        org.junit.Assert.assertTrue(offer(1_740_000))
        org.junit.Assert.assertFalse(offer(1_740_000, lead = 30))
        org.junit.Assert.assertFalse(offer(10_000, duration = 0))
        org.junit.Assert.assertFalse(offer(0, duration = 30_000))
        org.junit.Assert.assertFalse(offer(1_800_001))
        org.junit.Assert.assertFalse(offer(1_799_999, lead = 0))
    }

    @Test fun dismissalDisabledSettingAndMissingNextPreventOfferEvenAtTheEnd() {
        org.junit.Assert.assertFalse(shouldOfferNextEpisode(true, true, true, true, 1, 1, 60))
        org.junit.Assert.assertFalse(shouldOfferNextEpisode(true, false, false, true, 1, 1, 60))
        org.junit.Assert.assertFalse(shouldOfferNextEpisode(false, true, false, true, 1, 1, 60))
        org.junit.Assert.assertTrue(shouldOfferNextEpisode(true, true, false, true, 0, 0, 0))
    }

    @Test fun offerStaysOutOfErrorsLoadingBrowsingAndResumePrompts() {
        val ready = PlayerScreenState(busy = false, positionMs = 95_000, durationMs = 100_000,
            nextEpisode = PlayableItem("next", "Series", "Episode"))
        org.junit.Assert.assertTrue(ready.showNextEpisodeOffer())
        org.junit.Assert.assertFalse(ready.copy(busy = true).showNextEpisodeOffer())
        org.junit.Assert.assertFalse(ready.copy(error = "Error").showNextEpisodeOffer())
        org.junit.Assert.assertFalse(ready.copy(browsing = true).showNextEpisodeOffer())
        org.junit.Assert.assertFalse(ready.copy(awaitingResume = true).showNextEpisodeOffer())
        org.junit.Assert.assertTrue(ready.copy(ended = true, nextEpisodeOfferEnabled = false,
            nextEpisodeCountdown = 12).showNextEpisodeOffer())
        org.junit.Assert.assertFalse(ready.copy(ended = true, nextEpisodeOfferEnabled = false).showNextEpisodeOffer())
    }

    private class Answering(private val body: String?) : JsonHttpTransport {
        var requested: String? = null
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            requested = url
            return if (body == null) HttpResponse(statusCode = 500, body = "") else HttpResponse(200, body)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(204, "")
        override fun delete(url: String, headers: Map<String, String>) = HttpResponse(204, "")
    }

    private val connection = ServiceConnection(
        kind = ServiceKind.JELLYFIN, name = "Jellyfin", baseUrl = "https://media.example",
        token = "t", userId = "u1", state = ConnectionState.CONNECTED,
    )

    private fun episode(id: String, season: Int, number: Int, name: String) =
        """{"Id":"$id","Name":"$name","SeriesName":"Silo","SeriesId":"series-1","Type":"Episode",
            "ParentIndexNumber":$season,"IndexNumber":$number,"RunTimeTicks":30000000000}"""

    private fun client(body: String?) = JellyfinPlaybackClient(deviceId = "d", transport = Answering(body))

    private val current = PlayableItem(
        id = "e2", title = "Silo", type = "Episode", subtitle = "S01 E02 · Holston", seriesId = "series-1",
        season = 1, episode = 2,
    )

    @Test
    fun `the episode after the current one is returned`() {
        val body = """{"Items":[${episode("e1", 1, 1, "Freedom Day")},${episode("e2", 1, 2, "Holston")},
            ${episode("e3", 1, 3, "Machines")}]}"""
        val next = client(body).nextEpisode(connection, "u1", current)
        assertEquals("e3", next?.id)
        assertEquals(3, next?.episode)
    }

    /** The point of asking the server: the next episode can be in the next season. */
    @Test
    fun `a season boundary is followed through`() {
        val body = """{"Items":[${episode("e9", 1, 9, "Outside")},${episode("e10", 1, 10, "Finale")},
            ${episode("n1", 2, 1, "The Engineer")}]}"""
        val last = current.copy(id = "e10", season = 1, episode = 10)
        val next = client(body).nextEpisode(connection, "u1", last)
        assertEquals("n1", next?.id)
        assertEquals(2, next?.season)
    }

    @Test
    fun `the last episode of a series offers nothing`() {
        val body = """{"Items":[${episode("e9", 1, 9, "Outside")},${episode("e10", 1, 10, "Finale")}]}"""
        assertNull(client(body).nextEpisode(connection, "u1", current.copy(id = "e10")))
    }

    @Test
    fun `a film is never asked about`() {
        val transport = Answering("""{"Items":[]}""")
        val film = PlayableItem(id = "m1", title = "Dune", type = "Movie")
        assertNull(JellyfinPlaybackClient(deviceId = "d", transport = transport).nextEpisode(connection, "u1", film))
        assertNull("Ingen førespurnad skal ha gått ut", transport.requested)
    }

    /** An episode whose series the server never named cannot be followed, and must not throw. */
    @Test
    fun `a missing series id answers with nothing`() {
        val transport = Answering("""{"Items":[]}""")
        val orphan = current.copy(seriesId = "")
        assertNull(JellyfinPlaybackClient(deviceId = "d", transport = transport).nextEpisode(connection, "u1", orphan))
        assertNull(transport.requested)
    }

    @Test
    fun `a failed request answers with nothing rather than an error`() {
        assertNull(client(null).nextEpisode(connection, "u1", current))
    }

    @Test
    fun `the request names the series and the episode it is adjacent to`() {
        val transport = Answering("""{"Items":[]}""")
        JellyfinPlaybackClient(deviceId = "d", transport = transport).nextEpisode(connection, "u1", current)
        val url = transport.requested.orEmpty()
        assertEquals(true, url.contains("Shows/series-1/Episodes"))
        assertEquals(true, url.contains("adjacentTo=e2"))
    }
}
