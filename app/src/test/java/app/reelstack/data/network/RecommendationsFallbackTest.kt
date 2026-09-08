package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalogue lives at a versioned path so its shape can change without breaking a phone that
 * is two releases behind. The unversioned file every build before 0.13.4 asked for stays the
 * fallback, so the Home row keeps working while the catalogue is moved.
 */
class RecommendationsFallbackTest {
    private class Recording(private val responses: (String) -> HttpResponse) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return responses(url)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("No POST expected")
    }

    private val feed = """[{"title":"Silo","tmdbId":125988,"mediaType":"tv"}]"""

    @Test fun theVersionedPathIsAskedForFirst() {
        val transport = Recording { HttpResponse(200, feed) }
        RecommendationsClient(transport).feed()

        assertEquals(1, transport.urls.size)
        assertTrue(transport.urls.single(), transport.urls.single().contains("/v1/recommendations.json"))
    }

    @Test fun aMissingVersionedFileFallsBackToTheOldPath() {
        val transport = Recording { url ->
            if (url.contains("/v1/")) HttpResponse(404, "") else HttpResponse(200, feed)
        }
        val items = RecommendationsClient(transport).feed()

        assertEquals(2, transport.urls.size)
        assertEquals(listOf("Silo"), items.map { it.title })
    }

    @Test fun aRefusedRequestIsNotRetriedAgainstTheOtherPath() {
        // 500 says the host is unhappy, not that the file moved. Asking the same host the same
        // question again just doubles the delay before the row gives up.
        val transport = Recording { HttpResponse(500, "") }
        val failure = runCatching { RecommendationsClient(transport).feed() }.exceptionOrNull()

        assertEquals(1, transport.urls.size)
        assertTrue(failure is ServiceMessage)
    }

    @Test fun bothPathsMissingReportsItAsAServiceMessage() {
        val transport = Recording { HttpResponse(404, "") }
        val failure = runCatching { RecommendationsClient(transport).feed() }.exceptionOrNull()

        assertEquals(2, transport.urls.size)
        assertTrue(failure is ServiceMessage)
    }
}
