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
