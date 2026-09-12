package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Jellyfin's own notification channel, used as a doorbell rather than as a data source.
 *
 * "Spelar no" was refreshed every five seconds whenever Home was open. Nothing is playing most of
 * the time, so that is twelve requests a minute to be told the same thing — paid by a self-hosted
 * server, a phone's radio and a battery. Jellyfin already pushes a message when a session starts,
 * stops or moves, and this listens for it.
 *
 * It deliberately does **not** parse the session list out of the message. Who is allowed to see
 * which sessions is decided in [MediaServerClient.sessions] against the viewer's verified rights,
 * and a second place that builds the same list from a different payload is a second place to get
 * that wrong — a non-admin seeing the household's other screens is exactly the kind of leak that
 * has to be impossible rather than merely unlikely. So the socket says "something changed" and the
 * ordinary, access-checked request answers what.
 *
 * The poll does not go away either. It slows to a safety net while the socket is up, and returns to
 * its old cadence the moment the socket drops — a server behind a proxy that strips upgrades, or a
 * network that forbids them, must lose nothing but the saving.
 */
class JellyfinSessionSocket(
    private val deviceId: String,
    private val clientFactory: () -> OkHttpClient = {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            // The server sends its own keep-alive interval; a read timeout would kill an idle but
            // perfectly healthy socket between them.
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    },
) {

    /** Closing is idempotent, so a caller may close on every lifecycle event without checking. */
    fun interface Connection : AutoCloseable

    /**
     * Opens the channel for [connection].
     *
     * [onChanged] fires on the socket's own thread whenever the server reports playback activity,
     * and [onLost] when the channel closes for any reason — the caller uses that to put the poll
     * back to its normal cadence. Returns null when this service has no such channel.
     */
    fun connect(
        connection: ServiceConnection,
        onChanged: () -> Unit,
        onLost: () -> Unit,
    ): Connection? {
        if (connection.kind != ServiceKind.JELLYFIN || connection.token.isBlank()) return null
        val address = socketAddress(connection) ?: return null
        val request = Request.Builder().url(address).build()
        var closed = false

        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Ask for session updates. The two numbers are the server's own "initial delay,
                // period" in milliseconds; it only sends when something actually changes, but the
                // period bounds how often it may repeat itself.
                webSocket.send("""{"MessageType":"SessionsStart","Data":"0,1500"}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when (reactionTo(text)) {
                    Reaction.KEEP_ALIVE -> webSocket.send("""{"MessageType":"KeepAlive"}""")
                    Reaction.CHANGED -> onChanged()
                    Reaction.IGNORE -> Unit
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!closed) onLost()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!closed) onLost()
            }
        })

        return Connection {
            if (!closed) {
                closed = true
                runCatching { socket.send("""{"MessageType":"SessionsStop"}""") }
                socket.close(NORMAL_CLOSURE, null)
            }
        }
    }

    /**
     * `http` becomes `ws`, `https` becomes `wss`, and nothing else about the address is invented.
     *
     * [EndpointValidator.resolve] does the work of keeping the path and rejecting anything that is
     * not the configured server, so the scheme swap is the only thing left — and a base address
     * that is neither http nor https is not a Jellyfin server this app configured.
     */
    private fun socketAddress(connection: ServiceConnection): String? {
        val resolved = runCatching {
            EndpointValidator.resolve(
                connection.baseUrl,
                "socket?api_key=${escape(connection.token)}&deviceId=${escape(deviceId)}",
            )
        }.getOrNull() ?: return null
        return when {
            resolved.startsWith("https://", ignoreCase = true) -> "wss://" + resolved.removePrefix("https://")
            resolved.startsWith("http://", ignoreCase = true) -> "ws://" + resolved.removePrefix("http://")
            else -> null
        }
    }

    /**
     * One client for the life of the app, not one per visit to Home.
     *
     * Each `OkHttpClient` owns a thread pool and a connection pool, and Home is opened many times
     * in a session. Closing a socket is enough to release what that socket held; the client itself
     * costs nothing while idle.
     */
    private val client by lazy(clientFactory)

    /** What one message from the server asks of us. */
    internal enum class Reaction { KEEP_ALIVE, CHANGED, IGNORE }

    /**
     * Reads one message without touching the socket, so the decision can be tested on its own.
     *
     * Only playback-shaped messages count as a change. Jellyfin sends a good deal else down
     * this channel — library scans, user updates, restart notices — and reacting to those
     * would put the five-second poll back under a different name.
     */
    internal fun reactionTo(text: String): Reaction {
        val message = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return Reaction.IGNORE
        return when ((message["MessageType"] as? JsonPrimitive)?.contentOrNull) {
            // Jellyfin asks for a heartbeat and hangs up if it never arrives.
            "ForceKeepAlive" -> Reaction.KEEP_ALIVE
            "Sessions", "PlaybackStart", "PlaybackStopped", "PlaybackProgress" -> Reaction.CHANGED
            else -> Reaction.IGNORE
        }
    }

    /** A token belongs in a query value, not in whatever characters it happens to contain. */
    private fun escape(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private companion object {
        const val NORMAL_CLOSURE = 1000
    }
}
