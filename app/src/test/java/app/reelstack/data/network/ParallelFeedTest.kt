package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.ViewerAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/** Home's media feed asks many libraries; the answers must not depend on how the asking is split. */
class ParallelFeedTest {
    private class Server(override val supportsConcurrentCalls: Boolean) : JsonHttpTransport {
        val urls = ConcurrentLinkedQueue<String>()
        private val inFlight = AtomicInteger()
        val mostInFlight = AtomicInteger()

        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            val now = inFlight.incrementAndGet()
            mostInFlight.accumulateAndGet(now, ::maxOf)
            try {
                Thread.sleep(30)
                return HttpResponse(200, respond(url))
            } finally {
                inFlight.decrementAndGet()
            }
        }

        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("No write expected")

        private fun respond(url: String): String {
            if (url.contains("Views")) return """{"Items":[
                {"Id":"lib-movies","Name":"Filmar","CollectionType":"movies"},
                {"Id":"lib-series","Name":"Seriar","CollectionType":"tvshows"},
                {"Id":"lib-docs","Name":"Dokumentarar","CollectionType":"movies"}]}"""
            val parent = Regex("ParentId=([^&]+)").find(url)?.groupValues?.get(1) ?: "none"
            val row = listOf("Latest", "Resume", "NextUp", "IsFavorite", "PremiereDate").firstOrNull { it in url } ?: "Items"
            val type = Regex("IncludeItemTypes=([A-Za-z]+)").find(url)?.groupValues?.get(1) ?: "Movie"
            return """{"Items":[{"Id":"$row-$parent-$type","Name":"$row $parent","Type":"$type"}]}"""
        }
    }

    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Heimetenar", "https://media.example", "token", userId = "me")
    private val access = ViewerAccess(false, emptyMap())

    @Test fun theFeedIsTheSameWhetherLibrariesAreAskedInOrderOrAtOnce() {
        val sequential = Server(supportsConcurrentCalls = false)
        val concurrent = Server(supportsConcurrentCalls = true)

        val expected = MediaServerClient(sequential).feed(connection, access)
        val actual = MediaServerClient(concurrent).feed(connection, access)

        assertEquals(expected, actual)
        assertEquals(sequential.urls.sorted(), concurrent.urls.sorted())
        assertTrue(expected.recentMovies.isNotEmpty() && expected.resume.isNotEmpty())
    }

    @Test fun aTransportThatIsNotThreadSafeIsNeverCalledTwiceAtOnce() {
        val server = Server(supportsConcurrentCalls = false)
        MediaServerClient(server).feed(connection, access)
        assertEquals(1, server.mostInFlight.get())
    }

    @Test fun libraryRequestsOverlapButStayWithinTheBudget() {
        val server = Server(supportsConcurrentCalls = true)
        MediaServerClient(server).feed(connection, access)
        assertTrue("expected overlapping requests", server.mostInFlight.get() > 1)
        assertTrue(server.mostInFlight.get() <= FEED_PARALLEL_REQUESTS)
    }
}
