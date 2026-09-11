package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Taking a title out of "Continue watching".
 *
 * The list is derived from the playback position the server stores, so the only honest removal is
 * to clear that position — a local "hidden" list would disagree with every other client and be lost
 * on reinstall. Three server shapes answer that write, and the order matters: the modern one first,
 * the user-scoped one for older Jellyfin and Emby, and a stop report at position zero for a server
 * that knows neither. Only "this endpoint does not exist" may move on to the next; a rejected token
 * means the same thing on all three, and retrying it would turn one clear failure into three.
 */
class ResumeShelfTest {

    private class Recording(private vararg val responses: HttpResponse) : JsonHttpTransport {
        val posts = mutableListOf<Pair<String, String>>()
        private var index = 0
        private fun next(): HttpResponse = responses[index.coerceAtMost(responses.lastIndex)].also { index++ }
        override fun get(url: String, headers: Map<String, String>): HttpResponse = next()
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            posts += url to jsonBody
            return next()
        }
        override fun delete(url: String, headers: Map<String, String>): HttpResponse = next()
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
    fun `a modern Jellyfin takes the user-data write and nothing else`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .clearResume(connection(ServiceKind.JELLYFIN), "u1", "item9")
        assertEquals(1, transport.posts.size)
        assertTrue(transport.posts.single().first, transport.posts.single().first.endsWith("/UserItems/item9/UserData"))
    }

    /** Zero is the whole point: the position is what the resume list is built from. */
    @Test
    fun `the write clears the stored position`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .clearResume(connection(ServiceKind.JELLYFIN), "u1", "item9")
        assertTrue(transport.posts.single().second, transport.posts.single().second.contains("\"PlaybackPositionTicks\":0"))
    }

    @Test
    fun `an older server falls back to the user-scoped path`() {
        val transport = Recording(notFound(), ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .clearResume(connection(ServiceKind.JELLYFIN), "u1", "item9")
        assertEquals(2, transport.posts.size)
        assertTrue(transport.posts[1].first, transport.posts[1].first.endsWith("/Users/u1/Items/item9/UserData"))
    }

    /** A server with neither user-data route still understands a client that stopped at zero. */
    @Test
    fun `a server without user data writes falls back to a stop report`() {
        val transport = Recording(notFound(), notFound(), ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .clearResume(connection(ServiceKind.JELLYFIN), "u1", "item9")
        assertEquals(3, transport.posts.size)
        assertTrue(transport.posts[2].first, transport.posts[2].first.endsWith("/Sessions/Playing/Stopped"))
        assertTrue(transport.posts[2].second, transport.posts[2].second.contains("\"PositionTicks\":0"))
    }

    @Test
    fun `Emby never tries the Jellyfin-only path`() {
        val transport = Recording(ok())
        MediaServerClient(transport = transport, deviceId = "d")
            .clearResume(connection(ServiceKind.EMBY), "u1", "item9")
        assertTrue(transport.posts.single().first, transport.posts.single().first.endsWith("/Users/u1/Items/item9/UserData"))
    }

    @Test
    fun `a rejected token stops at the first answer`() {
        val transport = Recording(HttpResponse(statusCode = 401, body = ""))
        val failure = runCatching {
            MediaServerClient(transport = transport, deviceId = "d")
                .clearResume(connection(ServiceKind.JELLYFIN), "u1", "item9")
        }
        assertTrue("Ei avvist innlogging skal ikkje prøvast på nytt", failure.isFailure)
        assertEquals(1, transport.posts.size)
    }

    @Test
    fun `a missing profile is refused before anything is sent`() {
        val transport = Recording(ok())
        val failure = runCatching {
            MediaServerClient(transport = transport, deviceId = "d")
                .clearResume(connection(ServiceKind.JELLYFIN).copy(userId = ""), "", "item9")
        }
        assertTrue(failure.isFailure)
        assertTrue("Ingen kall utan profil", transport.posts.isEmpty())
    }
}
