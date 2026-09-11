package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Writing watched and favourite state back to the media server.
 *
 * The endpoint moved in Jellyfin 10.9, so the client tries the current path and falls back to the
 * user-scoped one. What matters is that the fallback happens *only* for a missing endpoint: a
 * rejected token on the new path means the same thing on the old one, and retrying would turn one
 * clear failure into two confusing ones.
 */
class UserItemStateTest {

    private class Recording(private vararg val responses: HttpResponse) : JsonHttpTransport {
        val posts = mutableListOf<String>()
        val deletes = mutableListOf<String>()
        private var index = 0
        private fun next(): HttpResponse = responses[index.coerceAtMost(responses.lastIndex)].also { index++ }
        override fun get(url: String, headers: Map<String, String>): HttpResponse = next()
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            posts += url
            return next()
        }
        override fun delete(url: String, headers: Map<String, String>): HttpResponse {
            deletes += url
            return next()
        }
    }

    private fun connection(kind: ServiceKind) = ServiceConnection(
        kind = kind,
        name = kind.displayName,
        baseUrl = "https://media.example",
        token = "t",
        userId = "u1",
        state = ConnectionState.CONNECTED,
    )

    private fun ok() = HttpResponse(statusCode = 204, body = "")
    private fun notFound() = HttpResponse(statusCode = 404, body = "")

    @Test
    fun `marking played uses the current Jellyfin endpoint first`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .setPlayed(connection(ServiceKind.JELLYFIN), "u1", "item9", played = true)
        assertEquals(1, transport.posts.size)
        assertTrue(transport.posts.single(), transport.posts.single().endsWith("/UserPlayedItems/item9"))
        assertTrue("Ingen DELETE ved markering", transport.deletes.isEmpty())
    }

    @Test
    fun `unmarking played deletes rather than posts`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .setPlayed(connection(ServiceKind.JELLYFIN), "u1", "item9", played = false)
        assertTrue(transport.posts.isEmpty())
        assertTrue(transport.deletes.single().endsWith("/UserPlayedItems/item9"))
    }

    @Test
    fun `a missing endpoint falls back to the user-scoped path`() {
        val transport = Recording(notFound(), ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .setFavourite(connection(ServiceKind.JELLYFIN), "u1", "item9", favourite = true)
        assertEquals(2, transport.posts.size)
        assertTrue(transport.posts[0].endsWith("/UserFavoriteItems/item9"))
        assertTrue(transport.posts[1].endsWith("/Users/u1/FavoriteItems/item9"))
    }

    /**
     * A rejected token is not a missing endpoint. Retrying the older path would hide the real
     * reason behind a second identical failure.
     */
    @Test
    fun `a rejected request is not retried on the older path`() {
        val transport = Recording(HttpResponse(statusCode = 401, body = ""))
        val client = MediaServerClient(transport = transport, deviceId = "d")
        runCatching {
            client.setPlayed(connection(ServiceKind.JELLYFIN), "u1", "item9", played = true)
        }.also { assertTrue("Skulle ha feila", it.isFailure) }
        assertEquals(1, transport.posts.size)
    }

    @Test
    fun `Emby only has the user-scoped path`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .setPlayed(connection(ServiceKind.EMBY), "u1", "item9", played = true)
        assertEquals(1, transport.posts.size)
        assertTrue(transport.posts.single().endsWith("/Users/u1/PlayedItems/item9"))
    }

    @Test
    fun `a blank profile id is refused before any request goes out`() {
        val transport = Recording(ok())
        val client = MediaServerClient(transport = transport, deviceId = "d")
        runCatching { client.setPlayed(connection(ServiceKind.JELLYFIN), "", "item9", played = true) }
            .also { assertTrue("Skulle ha feila", it.isFailure) }
        assertTrue(transport.posts.isEmpty() && transport.deletes.isEmpty())
    }
}
