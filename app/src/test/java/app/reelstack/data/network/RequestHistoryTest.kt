package app.reelstack.data.network

import app.reelstack.data.model.*
import app.reelstack.data.repository.RequestHistoryRepository
import org.junit.Assert.*
import org.junit.Test

class RequestHistoryTest {
    private val connection = ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "fixture", "7", sessionCookie = true)
    private fun row(id: Int, owner: Int = 7, tmdb: Int = id) =
        """{"id":$id,"status":2,"requestedBy":{"id":$owner},"media":{"tmdbId":$tmdb,"mediaType":"movie","status":5,"title":"Title $id","posterPath":"/poster.jpg"}}"""
    private class Recording(val respond: (String) -> HttpResponse) : JsonHttpTransport {
        val reads = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            reads += url
            assertTrue(headers.values.contains("fixture"))
            return respond(url)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("History must never write")
    }

    @Test fun pagingReachesRequestsBeyondTheOldHundredLimit() {
        val transport = Recording { url ->
            val skip = Regex("skip=(\\d+)").find(url)!!.groupValues[1].toInt()
            HttpResponse(200, """{"pageInfo":{"results":125},"results":[${(skip until minOf(skip + 20, 125)).joinToString(",") { row(it + 1) }}]}""")
        }
        val client = SeerrServiceClient(transport)
        var state = RequestHistoryState()
        do {
            val page = client.requestHistory(connection, "7", state.nextOffset)
            state = state.append(page.items.map { tracked(it.id) }, page.nextOffset, page.hasMore, page.total)
        } while (state.hasMore)
        assertEquals(125, state.items.size)
        assertEquals(125, state.nextOffset)
        assertEquals(7, transport.reads.size)
        assertTrue(transport.reads.all { it.contains("requestedBy=7") && it.contains("sortDirection=desc") })
    }
    @Test fun malformedAndForeignRowsStillAdvanceTheServerOffset() {
        val page = parseRequestHistoryPage("""{"pageInfo":{"results":25},"results":[${row(1)},${row(2,8)},{"missing":"id"}]}""", "7", 20, 20)
        assertEquals(listOf(1), page.items.map { it.id })
        assertEquals(23, page.nextOffset)
        assertTrue(page.hasMore)
    }
    @Test fun absentCountUsesFullPageThenShortPageAndEmptyPageAlwaysStops() {
        assertTrue(parseRequestHistoryPage("""{"results":[${row(1)},${row(2)}]}""", "7", 0, 2).hasMore)
        assertFalse(parseRequestHistoryPage("""{"results":[${row(3)}]}""", "7", 2, 2).hasMore)
        assertFalse(parseRequestHistoryPage("""{"pageInfo":{"results":100},"results":[]}""", "7", 2, 2).hasMore)
        assertThrows(Exception::class.java) { parseRequestHistoryPage("{}", "7", 0, 20) }
    }
    @Test fun newRequestsMovingPageBoundariesDoNotDuplicateOrCollapseDifferentRequestsForSameTitle() {
        val first = RequestHistoryState().append(listOf(tracked(1), tracked(2)), 2, true, 5)
        val next = first.append(listOf(tracked(2), tracked(3)), 4, true, 5)
        assertEquals(listOf(1,2,3), next.items.map { it.requestId })
        assertThrows(IllegalStateException::class.java) { next.append(listOf(tracked(2), tracked(3)), 6, true, 10) }
    }
    @Test fun differentActorPreventsHistoryRequestEvenWithAdminPermission() {
        val transport = Recording { HttpResponse(200, """{"id":8,"displayName":"Other","permissions":2}""") }
        val repo = RequestHistoryRepository(SeerrServiceClient(transport), AccountProfileClient(transport = transport))
        assertThrows(Exception::class.java) { repo.load(connection, "7", 0) }
        assertEquals(1, transport.reads.size)
        assertTrue(transport.reads.single().endsWith("auth/me"))
    }
    @Test fun historyDoesNotEnableAlertsAndSurvivesMissingMetadata() {
        val transport = Recording { url -> when {
            url.endsWith("auth/me") -> HttpResponse(200, """{"id":7,"displayName":"Me","permissions":0}""")
            url.contains("/request?") -> HttpResponse(200, """{"results":[{"id":1,"requestedBy":{"id":7},"media":{"tmdbId":42,"mediaType":"movie"}}]}""")
            else -> HttpResponse(503, "{}")
        } }
        val page = RequestHistoryRepository(SeerrServiceClient(transport), AccountProfileClient(transport = transport)).load(connection, "7", 0)
        assertEquals(1, page.items.size)
        assertFalse(page.items.single().notify)
        assertEquals(RequestStage.UNKNOWN, page.items.single().stage)
        assertEquals(1, page.items.single().requestId)
    }
    @Test fun cancellationStopsBeforeMoreMetadataRequests() {
        val transport = Recording { url -> if (url.endsWith("auth/me")) HttpResponse(200, """{"id":7,"permissions":0}""")
            else HttpResponse(200, """{"results":[${row(1)},${row(2)}]}""") }
        var checks = 0
        val repo = RequestHistoryRepository(SeerrServiceClient(transport), AccountProfileClient(transport = transport))
        assertThrows(kotlinx.coroutines.CancellationException::class.java) {
            repo.load(connection, "7", 0) { if (++checks == 3) throw kotlinx.coroutines.CancellationException() }
        }
        assertEquals(2, transport.reads.size)
    }
    @Test fun expiredSessionDoesNotLookLikeAnEmptyHistory() {
        val transport = Recording { HttpResponse(401, "{}") }
        assertThrows(Exception::class.java) { SeerrServiceClient(transport).requestHistory(connection, "7", 20) }
        assertEquals(1, transport.reads.size)
    }
    @Test fun declinedRequestIsNotRewrittenAsSuccessfulWhenTheFilmLaterBecomesAvailable() {
        val transport = Recording { url -> if (url.endsWith("auth/me")) HttpResponse(200, """{"id":7,"permissions":0}""")
            else HttpResponse(200, """{"results":[${row(1).replace("\"status\":2", "\"status\":3") }]}""") }
        val page = RequestHistoryRepository(SeerrServiceClient(transport), AccountProfileClient(transport = transport)).load(connection, "7", 0)
        assertEquals(RequestStage.DECLINED, page.items.single().stage)
    }
    @Test fun activeAndNotifyingFollowsAreNeverEvictedByNewCompletedHistory() {
        val active = (1..120).map { tracked(it).copy(stage = RequestStage.REQUESTED, notify = true) }
        val complete = (121..300).map { tracked(it).copy(stage = RequestStage.AVAILABLE, notify = false, updatedAt = it.toLong()) }
        val kept = retainTrackedRequests(active + complete)
        assertTrue(kept.containsAll(active))
        assertEquals(220, kept.size)
    }
    private fun tracked(id: Int) = TrackedRequest("history-$id", 42, "movie", "Same title", null, emptySet(), requestId = id)
}
