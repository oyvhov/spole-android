package app.reelstack.data.model

import org.junit.Assert.*
import org.junit.Test

class ActivityRequestGroupsTest {
    private fun request(key: String, mediaId: Int = 12, season: Int = 1) =
        TrackedRequest(key, mediaId, "tv", "Same title", null, setOf(season), requestId = season)

    @Test fun seasonsShareArtworkWithoutCombiningTheirActionsOrStatuses() {
        val first = request("first")
        val second = request("second", season = 2).copy(stage = RequestStage.DOWNLOADING, percent = 48, notify = false)
        val group = groupActivityRequests(listOf(first, second)).single()
        assertEquals(listOf(first, second), group.requests)
        assertEquals(1, group.requests[0].requestId)
        assertEquals(2, group.requests[1].requestId)
        assertEquals(RequestStage.REQUESTED, group.requests[0].stage)
        assertEquals(48, group.requests[1].percent)
        assertFalse(group.requests[1].notify)
    }

    @Test fun equalTitlesWithDifferentProviderIdsStaySeparate() {
        assertEquals(2, groupActivityRequests(listOf(request("a"), request("b", mediaId = 13))).size)
        assertEquals(1, groupActivityRequests(listOf(request("a"), request("b").copy(title = "Another translation"))).size)
    }

    @Test fun qualityAndAvailabilityFollowsKeepSeparateControls() {
        val request = request("a")
        assertEquals(3, groupActivityRequests(listOf(request,
            request.copy(key = "4k", is4k = true), request.copy(key = "follow", availabilityOnly = true))).size)
    }

    @Test fun moviesAndMissingIdsAreNeverAssumedToBeTheSameRequest() {
        val movie = request("movie").copy(mediaType = "movie", seasons = emptySet())
        assertEquals(4, groupActivityRequests(listOf(movie, movie.copy(key = "other-movie"),
            request("unknown-a", mediaId = 0), request("unknown-b", mediaId = 0))).size)
    }

    @Test fun groupingRetainsTheFeedOrderAndWorksOnTheCurrentFilteredList() {
        val requests = listOf(request("a"), request("b", mediaId = 13), request("c", season = 2))
        assertEquals(listOf(listOf("a", "c"), listOf("b")),
            groupActivityRequests(requests).map { it.requests.map(TrackedRequest::key) })
        assertEquals(listOf("c"), groupActivityRequests(requests.filter { it.key == "c" }).single().requests.map { it.key })
    }
}
