package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The playback doorbell.
 *
 * Spole asked the server what was playing every five seconds while Home was open. Jellyfin already
 * pushes a message when playback starts, stops or moves, so the poll became a safety net and this
 * became the trigger — but only a trigger. The list of who may see which session is built once, in
 * [MediaServerClient.sessions], against the viewer's verified rights; a second place that built it
 * from a different payload would be a second place to leak the household's other screens to
 * somebody who is not an administrator.
 */
class SessionSocketTest {

    @Test fun handshakeUsesAHeaderAndFailureRestoresPolling() {
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        val received = java.util.concurrent.CopyOnWriteArrayList<Pair<String, String>>()
        server.createContext("/jellyfin/socket") { exchange ->
            received += exchange.requestURI.toString() to exchange.requestHeaders.getFirst("Authorization").orEmpty()
            exchange.sendResponseHeaders(401, -1); exchange.close()
        }
        server.start()
        val lost = java.util.concurrent.CountDownLatch(1)
        var channel: JellyfinSessionSocket.Connection? = null
        try {
            channel = JellyfinSessionSocket("device id").connect(
                connection(baseUrl = "http://127.0.0.1:${server.address.port}/jellyfin"), {}, lost::countDown)
            org.junit.Assert.assertTrue(lost.await(5, java.util.concurrent.TimeUnit.SECONDS))
            val (path, header) = received.single()
            assertEquals("/jellyfin/socket?deviceId=device%20id", path)
            org.junit.Assert.assertFalse(path.contains("abc"))
            org.junit.Assert.assertTrue(header.contains("Token=\"abc\""))
        } finally { channel?.close(); server.stop(0) }
    }

    private val socket = JellyfinSessionSocket(deviceId = "device") { OkHttpClient() }

    private fun connection(
        kind: ServiceKind = ServiceKind.JELLYFIN,
        baseUrl: String = "https://media.example",
        token: String = "abc",
    ) = ServiceConnection(
        kind = kind, name = kind.displayName, baseUrl = baseUrl, token = token,
        userId = "u1", state = ConnectionState.CONNECTED,
    )

    /** Only Jellyfin publishes this channel; nothing else should be probed for one. */
    @Test
    fun `only a signed-in Jellyfin connection opens a channel`() {
        assertNull(socket.connect(connection(kind = ServiceKind.EMBY), {}, {}))
        assertNull(socket.connect(connection(kind = ServiceKind.SEERR), {}, {}))
        assertNull(socket.connect(connection(token = ""), {}, {}))
    }

    /** An address the validator refuses must not be turned into a socket address by guessing. */
    @Test
    fun `an unusable address opens nothing`() {
        assertNull(socket.connect(connection(baseUrl = ""), {}, {}))
        assertNull(socket.connect(connection(baseUrl = "ftp://media.example"), {}, {}))
    }

    @Test
    fun `playback messages count as a change`() {
        listOf("Sessions", "PlaybackStart", "PlaybackStopped", "PlaybackProgress").forEach { type ->
            assertEquals(
                "$type skal telje som endring",
                JellyfinSessionSocket.Reaction.CHANGED,
                socket.reactionTo("""{"MessageType":"$type","Data":[]}"""),
            )
        }
    }

    /** The server hangs up on a client that never answers this one. */
    @Test
    fun `a keep-alive demand is answered rather than treated as a change`() {
        assertEquals(
            JellyfinSessionSocket.Reaction.KEEP_ALIVE,
            socket.reactionTo("""{"MessageType":"ForceKeepAlive","Data":30}"""),
        )
    }

    /**
     * Everything else on this channel is ignored on purpose. Reacting to a library scan or a user
     * update would put the five-second poll back under a different name.
     */
    @Test
    fun `other traffic on the channel is ignored`() {
        listOf(
            """{"MessageType":"KeepAlive"}""",
            """{"MessageType":"LibraryChanged","Data":{}}""",
            """{"MessageType":"UserUpdated","Data":{}}""",
            """{"MessageType":"RestartRequired"}""",
        ).forEach { message ->
            assertEquals(JellyfinSessionSocket.Reaction.IGNORE, socket.reactionTo(message))
        }
    }

    @Test
    fun `malformed traffic is ignored rather than thrown`() {
        listOf("", "not json", "[]", "{}", """{"MessageType":42}""").forEach { message ->
            assertEquals(JellyfinSessionSocket.Reaction.IGNORE, socket.reactionTo(message))
        }
    }
}
