package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverHubStateTest {
    private fun media(id: Int) = DiscoverMedia("seerr-movie-$id", "T$id", "2026", 0, false, remoteId = id, mediaType = "movie")

    @Test fun focusDetailsKeepTheHundredMostRecent() {
        var state = DiscoverHubState()
        (1..101).forEach { state = state.withFocusDetails("t$it", FocusDetails(listOf("$it min"), null)) }
        assertEquals(100, state.focusDetails.size)
        assertEquals(null, state.focusDetails["t1"])
        // Looking at a title again makes it recent, so it outlives the ones after it.
        state = state.withFocusDetails("t2", FocusDetails(listOf("2 min"), "Om")).withFocusDetails("t102", FocusDetails(emptyList(), null))
        assertEquals(FocusDetails(listOf("2 min"), "Om"), state.focusDetails["t2"])
        assertEquals(null, state.focusDetails["t3"])
    }

    @Test fun aNextPageDropsTitlesTheListAlreadyHas() {
        // TMDB lists shift while they are read, so page two often repeats a title from page one; a
        // repeated id is a repeated key in the grid, and Compose stops on that.
        val first = DiscoverRowState().withPage(listOf(media(1), media(2), media(2)), page = 1, hasMore = true)
        val second = first.withPage(listOf(media(2), media(3)), page = 2, hasMore = false)
        assertEquals(listOf(1, 2), first.items.map { it.remoteId })
        assertEquals(listOf(1, 2, 3), second.items.map { it.remoteId })
        assertEquals(2, second.page)
        assertEquals(false, second.hasMore)
    }

    @Test fun aSentRequestReadsAsRequestedOrWaiting() {
        val open = media(1).copy(seerrStatus = 1)
        assertEquals(3, open.asRequested(waitsForApproval = false).seerrStatus)
        assertEquals(2, open.asRequested(waitsForApproval = true).seerrStatus)
        assertEquals(true, open.asRequested(waitsForApproval = false).requested)
        // A series already partly in the library stays partly in the library.
        assertEquals(4, open.copy(seerrStatus = 4).asRequested(waitsForApproval = false).seerrStatus)
    }

    @Test fun aRequestLineOpensItsTitleWithTheRequestsStatus() {
        assertEquals(5, RequestStage.AVAILABLE.seerrStatus())
        assertEquals(3, RequestStage.DOWNLOADING.seerrStatus())
        assertEquals(3, RequestStage.REQUESTED.seerrStatus())
    }
}
