package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test

class ArtworkPolicyTest {
    @Test fun authenticatedArtworkDoesNotFollowSameOriginOrForeignRedirects() {
        val redirected = java.util.concurrent.atomic.AtomicInteger()
        val foreign = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        val origin = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        foreign.createContext("/") { exchange ->
            redirected.incrementAndGet(); exchange.sendResponseHeaders(204, -1); exchange.close()
        }
        origin.createContext("/outside") { exchange ->
            redirected.incrementAndGet(); exchange.sendResponseHeaders(204, -1); exchange.close()
        }
        val received = java.util.concurrent.CopyOnWriteArrayList<String>()
        origin.createContext("/jellyfin/artwork") { exchange ->
            received += exchange.requestHeaders.getFirst("X-Emby-Token").orEmpty()
            exchange.responseHeaders.add("Location", if (exchange.requestURI.rawQuery == "same") "/outside"
                else "http://127.0.0.1:${foreign.address.port}/foreign")
            exchange.sendResponseHeaders(302, -1); exchange.close()
        }
        foreign.start(); origin.start()
        try {
            for (destination in listOf("same", "foreign")) {
                val request = okhttp3.Request.Builder().url("http://127.0.0.1:${origin.address.port}/jellyfin/artwork?$destination")
                    .header("X-Emby-Token", "synthetic-artwork-token").build()
                HttpTransport.sharedClient.newCall(request).execute().use { assertEquals(302, it.code) }
            }
            assertEquals(listOf("synthetic-artwork-token", "synthetic-artwork-token"), received)
            assertEquals(0, redirected.get())
        } finally { origin.stop(0); foreign.stop(0) }
    }

    @Test fun credentialsStayWithinTheConfiguredOriginAndBasePath() {
        val base = "https://media.example/jellyfin"
        assertTrue(artworkBelongsTo(base, "$base/Items/1/Images/Primary?maxWidth=380"))
        assertFalse(artworkBelongsTo(base, "https://media.example/jellyfin-other/Items/1"))
        assertFalse(artworkBelongsTo(base, "https://other.example/jellyfin/Items/1"))
        assertFalse(artworkBelongsTo(base, "https://media.example:444/jellyfin/Items/1"))
        assertFalse(artworkBelongsTo(base, "$base/%2e%2e/private"))
        assertFalse(artworkBelongsTo(base, "$base/Items%2f../private"))
        assertFalse(artworkBelongsTo(base, "$base/Items/1?API_KEY=secret"))
        assertFalse(HttpTransport.sharedClient.followRedirects)
        assertFalse(HttpTransport.sharedClient.followSslRedirects)
    }
}
