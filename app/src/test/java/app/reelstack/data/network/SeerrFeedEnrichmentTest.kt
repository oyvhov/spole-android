package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SeerrFeedEnrichmentTest {
    private val connection = ServiceConnection(
        kind = ServiceKind.SEERR, name = "Seerr", baseUrl = "https://media.example.com", token = "test-api-key",
    )

    @Test
    fun enrichesRequestsBeyondFourthPositionAndDeduplicatesByMediaTypeAndId() {
        val requests = (1..6).joinToString(",") { request(it, it) } + "," + request(7, 5) + "," + request(8, 5, "tv")
        val transport = FeedTransport(requests)
        val client = SeerrServiceClient(transport)

        val feed = client.feed(connection)

        assertEquals(8, feed.requests.size)
        assertTrue(feed.requests.all { it.title != null && it.artworkUrl != null })
        assertEquals("Title movie/5", feed.requests[4].title)
        assertEquals("Title movie/5", feed.requests[6].title)
        assertEquals("Title tv/5", feed.requests[7].title)
        assertEquals(7, transport.detailUrls.size)
        assertEquals(feed.requests, client.feed(connection).requests)
        assertEquals(7, transport.detailUrls.size)
    }

    @Test
    fun reusesDiscoverMetadataAndPreservesExistingRequestFields() {
        val requests = request(1, 5) + "," + request(2, 6, extra = ",\"title\":\"Existing title\"")
        val transport = FeedTransport(
            requests,
            discover = """{"results":[{"id":5,"mediaType":"movie","title":"Discovered title","posterPath":"/discovered.jpg"}]}""",
        )

        val feed = SeerrServiceClient(transport).feed(connection)

        assertEquals("Discovered title", feed.requests[0].title)
        assertEquals("https://image.tmdb.org/t/p/w500/discovered.jpg", feed.requests[0].artworkUrl)
        assertEquals("Existing title", feed.requests[1].title)
        assertEquals(1, transport.detailUrls.size)
        assertTrue(transport.detailUrls.single().endsWith("/movie/6"))
    }

    @Test
    fun failedLookupsRetainRequestsAndDoNotPreventLaterEnrichment() {
        val requests = (1..4).joinToString(",") { request(it, it) } + "," + request(5, 1)
        val transport = FeedTransport(requests, failFirstThree = true)

        val feed = SeerrServiceClient(transport).feed(connection)

        assertEquals(5, feed.requests.size)
        assertTrue(feed.requests.take(3).all { it.title == null })
        assertEquals("Title movie/4", feed.requests[3].title)
        assertEquals(null, feed.requests[4].title)
        assertEquals(4, transport.detailUrls.size)
    }

    @Test
    fun enrichmentRemainsBoundedToTwentyDistinctLookups() {
        val transport = FeedTransport((1..25).joinToString(",") { request(it, it) })

        val feed = SeerrServiceClient(transport).feed(connection)

        assertEquals(25, feed.requests.size)
        assertEquals(20, transport.detailUrls.size)
        assertTrue(feed.requests.take(20).all { it.title != null })
        assertTrue(feed.requests.drop(20).all { it.title == null })
    }

    @Test
    fun secondRefreshReusesMetadataButFetchesCurrentRequestStatus() {
        val transport = FeedTransport(request(1, 5))
        val client = SeerrServiceClient(transport)
        val first = client.feed(connection)
        transport.requests = request(1, 5).replace("\"status\":2", "\"status\":5")

        val second = client.feed(connection)

        assertEquals(first.requests.single().title, second.requests.single().title)
        assertEquals(first.requests.single().artworkUrl, second.requests.single().artworkUrl)
        assertEquals(5, second.requests.single().status)
        assertEquals(1, transport.detailUrls.size)
        assertEquals(4, transport.feedCalls)
    }

    @Test
    fun cacheIsIsolatedByEndpointTokenAuthenticationModeAndUser() {
        val transport = FeedTransport(request(1, 5))
        val client = SeerrServiceClient(transport)
        val first = client.feed(connection)
        val changedConnections = listOf(
            connection.copy(baseUrl = "https://other.example.com"),
            connection.copy(baseUrl = "${connection.baseUrl}/other-seerr"),
            connection.copy(token = "other-test-key"),
            connection.copy(sessionCookie = true),
            connection.copy(userId = "other-user"),
        )
        changedConnections.forEachIndexed { index, changed ->
            transport.detailResponse = HttpResponse(200, """{"title":"Other $index","posterPath":"/other-$index.jpg"}""")

            val refreshed = client.feed(changed).requests.single()

            assertEquals("Other $index", refreshed.title)
            assertEquals("https://image.tmdb.org/t/p/w500/other-$index.jpg", refreshed.artworkUrl)
            assertEquals(index + 2, transport.detailUrls.size)
        }

        assertEquals(first.requests, client.feed(connection).requests)
        assertEquals(6, transport.detailUrls.size)
    }

    @Test
    fun equivalentNormalizedEndpointReusesCache() {
        val transport = FeedTransport(request(1, 5))
        val client = SeerrServiceClient(transport)
        client.feed(connection)

        client.feed(connection.copy(baseUrl = "${connection.baseUrl}/", name = "Renamed connection"))

        assertEquals(1, transport.detailUrls.size)
    }

    @Test
    fun cacheBelongsToClientInstance() {
        val transport = FeedTransport(request(1, 5))
        SeerrServiceClient(transport).feed(connection)

        SeerrServiceClient(transport).feed(connection)

        assertEquals(2, transport.detailUrls.size)
    }

    @Test
    fun successfulMetadataExpiresTenMinutesAfterFetchEvenIfRecentlyUsed() {
        var now = 0L
        val transport = FeedTransport(request(1, 5))
        val client = SeerrServiceClient(transport, nanoTime = { now })
        client.feed(connection)
        now = 599_999_999_999L
        client.feed(connection)
        assertEquals(1, transport.detailUrls.size)
        transport.detailResponse = HttpResponse(200, """{"title":"Updated title","posterPath":"/updated.jpg"}""")
        now = 600_000_000_000L

        val refreshed = client.feed(connection)

        assertEquals("Updated title", refreshed.requests.single().title)
        assertEquals(2, transport.detailUrls.size)
    }

    @Test
    fun failedAndMalformedLookupsAreRetriedOnNextRefresh() {
        val transport = FeedTransport((1..3).joinToString(",") { request(it, it) }, failFirstThree = true)
        val client = SeerrServiceClient(transport)
        assertTrue(client.feed(connection).requests.all { it.title == null })
        transport.failFirstThree = false

        assertTrue(client.feed(connection).requests.all { it.title != null })
        assertEquals(6, transport.detailUrls.size)
        client.feed(connection)
        assertEquals(6, transport.detailUrls.size)
    }

    @Test
    fun emptySuccessfulResponsesAreNotCachedAcrossRefreshes() {
        val transport = FeedTransport(request(1, 5))
        transport.detailResponse = HttpResponse(200, """{"title":" ","posterPath":null}""")
        val client = SeerrServiceClient(transport)
        assertEquals(null, client.feed(connection).requests.single().title)
        transport.detailResponse = null

        assertEquals("Title movie/5", client.feed(connection).requests.single().title)
        assertEquals(2, transport.detailUrls.size)
    }

    @Test
    fun cacheEvictsLeastRecentlyUsedEntryBeyondSixtyFour() {
        val transport = FeedTransport("")
        val client = SeerrServiceClient(transport, nanoTime = { 0L })
        (1..64).chunked(20).forEach { ids ->
            transport.requests = ids.joinToString(",") { request(it, it) }
            client.feed(connection)
        }
        assertEquals(64, transport.detailUrls.size)
        transport.requests = request(1, 1)
        client.feed(connection) // Keep entry 1 most recently used; entry 2 should be evicted next.
        transport.requests = request(65, 65)
        client.feed(connection)
        assertEquals(65, transport.detailUrls.size)
        transport.requests = request(1, 1)
        client.feed(connection)
        assertEquals(65, transport.detailUrls.size)

        transport.requests = request(2, 2)
        client.feed(connection)

        assertEquals(66, transport.detailUrls.size)
        assertTrue(transport.detailUrls.last().endsWith("/movie/2"))
    }

    @Test
    fun cacheHitsDoNotConsumeTwentyLookupBudget() {
        val transport = FeedTransport((1..20).joinToString(",") { request(it, it) })
        val client = SeerrServiceClient(transport)
        client.feed(connection)
        transport.requests = (1..45).joinToString(",") { request(it, it) }

        val refreshed = client.feed(connection)

        assertEquals(40, transport.detailUrls.size)
        assertTrue(refreshed.requests.take(40).all { it.title != null })
        assertTrue(refreshed.requests.drop(40).all { it.title == null })
    }

    @Test
    fun freshDiscoverMetadataTakesPriorityOverCachedDetails() {
        val transport = FeedTransport(request(1, 5))
        val client = SeerrServiceClient(transport)
        client.feed(connection)
        transport.discover = """{"results":[{"id":5,"mediaType":"movie","title":"Fresh title","posterPath":"/fresh.jpg"}]}"""

        val refreshed = client.feed(connection).requests.single()

        assertEquals("Fresh title", refreshed.title)
        assertEquals("https://image.tmdb.org/t/p/w500/fresh.jpg", refreshed.artworkUrl)
        assertEquals(1, transport.detailUrls.size)
    }

    private fun request(id: Int, mediaId: Int, type: String = "movie", extra: String = "") =
        """{"id":$id,"status":2,"media":{"tmdbId":$mediaId,"mediaType":"$type"$extra}}"""

    private class FeedTransport(
        var requests: String,
        var discover: String = """{"results":[]}""",
        var failFirstThree: Boolean = false,
    ) : JsonHttpTransport {
        val detailUrls = mutableListOf<String>()
        var feedCalls = 0
        var detailResponse: HttpResponse? = null

        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            if (url.endsWith("/auth/me")) return HttpResponse(200, """{"id":1,"displayName":"Admin","permissions":2}""")
            if (url.contains("/discover/trending?")) {
                feedCalls++
                return HttpResponse(200, discover)
            }
            if (url.contains("/request?")) {
                feedCalls++
                return HttpResponse(200, """{"results":[$requests]}""")
            }
            detailUrls += url
            if (failFirstThree) {
                when (url.substringAfterLast('/')) {
                    "1" -> throw IOException("Test transport timeout")
                    "2" -> return HttpResponse(404, "{}")
                    "3" -> return HttpResponse(200, "invalid json")
                }
            }
            return detailResponse ?: HttpResponse(200, """{"title":"Title ${url.substringAfter("/api/v1/")}","posterPath":"/poster.jpg"}""")
        }

        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("Feed enrichment must only use GET")
    }
}
