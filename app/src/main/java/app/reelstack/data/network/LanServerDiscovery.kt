package app.reelstack.data.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Which server answered. Both speak the same broadcast protocol on UDP 7359, with their own probe. */
enum class DiscoveredServerKind(internal val probe: String) {
    JELLYFIN("who is JellyfinServer?"),
    EMBY("who is EmbyServer?"),
}

/** A media server on the local network, with an address this device can actually reach. */
data class DiscoveredServer(
    val kind: DiscoveredServerKind,
    val id: String,
    val name: String,
    val address: String,
)

/**
 * Turn one discovery reply into a server, or null when it is not one.
 *
 * The server names its own address, but it names the address of the interface it *thinks* the
 * request came in on. A Windows host with WSL or Hyper-V answers with a virtual adapter
 * (`172.26.192.1`) that no television can reach, while the packet itself arrived from the LAN
 * address. When the advertised host is an IP literal that is not the sender, the sender wins and
 * the advertised scheme and port are kept. A host *name* is left alone: it is usually the
 * owner's published address, and its port says nothing about the LAN.
 */
internal fun parseDiscoveryReply(
    kind: DiscoveredServerKind,
    payload: String,
    sender: InetAddress?,
): DiscoveredServer? {
    val json = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
    val id = json.text("Id")?.takeIf { it.isNotBlank() } ?: return null
    val advertised = json.text("Address")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = runCatching { URI(advertised) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()?.takeIf { it == "http" || it == "https" } ?: return null
    val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
    val port = if (uri.port > 0) uri.port else if (scheme == "https") 443 else 80
    val senderHost = (sender as? Inet4Address)?.hostAddress
    val reachableHost = if (senderHost != null && isIpv4Literal(host) && host != senderHost) senderHost else host
    val defaultPort = (scheme == "http" && port == 80) || (scheme == "https" && port == 443)
    val address = "$scheme://$reachableHost" + (if (defaultPort) "" else ":$port")
    val name = json.text("Name")?.trim()?.takeIf { it.isNotEmpty() } ?: reachableHost
    return DiscoveredServer(kind, id, name, address)
}

/** True when the reply's own address and the packet's sender agree; such a reply is preferred. */
internal fun replyIsConsistent(payload: String, sender: InetAddress?): Boolean {
    val senderHost = (sender as? Inet4Address)?.hostAddress ?: return false
    val advertised = runCatching { Json.parseToJsonElement(payload).jsonObject.text("Address") }.getOrNull()
    return runCatching { URI(advertised).host }.getOrNull() == senderHost
}

/**
 * Keep one entry per server. A multi-homed host answers once per interface; the reply whose
 * advertised address matched its sender is the trustworthy one.
 */
internal fun mergeDiscovered(
    known: List<Pair<DiscoveredServer, Boolean>>,
    next: DiscoveredServer,
    consistent: Boolean,
): List<Pair<DiscoveredServer, Boolean>> {
    val index = known.indexOfFirst { it.first.kind == next.kind && it.first.id == next.id }
    if (index < 0) return known + (next to consistent)
    if (known[index].second || !consistent) return known
    return known.toMutableList().also { it[index] = next to true }
}

/** Jellyfin before Emby, then by name, so the same network always lists in the same order. */
internal fun orderDiscovered(servers: List<DiscoveredServer>): List<DiscoveredServer> =
    servers.sortedWith(compareBy<DiscoveredServer> { it.kind.ordinal }.thenBy { it.name.lowercase() }.thenBy { it.address })

private fun isIpv4Literal(host: String): Boolean =
    host.split('.').let { parts -> parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 } }

private fun JsonObject.text(key: String): String? =
    runCatching { get(key)?.jsonPrimitive?.contentOrNull }.getOrNull()

/**
 * Asks the local network which Jellyfin and Emby servers are there.
 *
 * Each kind gets its own socket, so a reply is attributed by the probe it answers and not guessed
 * from its shape. Replies are collected until [timeoutMillis] has passed, and each new server is
 * emitted at once: a list on the television fills in as answers arrive instead of after the full
 * wait. Nothing here signs in or calls the server's HTTP API.
 */
class LanServerDiscovery(
    private val broadcastTargets: () -> List<InetAddress>,
    private val port: Int = DISCOVERY_PORT,
    private val timeoutMillis: Long = 2_500,
    private val kinds: List<DiscoveredServerKind> = DiscoveredServerKind.entries,
) {
    fun discover(): Flow<List<DiscoveredServer>> = flow {
        var found = emptyList<Pair<DiscoveredServer, Boolean>>()
        emit(emptyList())
        val targets = broadcastTargets().distinct().ifEmpty { listOf(GLOBAL_BROADCAST) }
        val sockets = kinds.map { kind -> kind to openProbeSocket() }
        try {
            for ((kind, socket) in sockets) {
                val bytes = kind.probe.toByteArray(Charsets.UTF_8)
                for (target in targets) {
                    runCatching { socket.send(DatagramPacket(bytes, bytes.size, target, port)) }
                }
            }
            val buffer = ByteArray(4_096)
            val deadline = System.nanoTime() + timeoutMillis * 1_000_000
            while (System.nanoTime() < deadline) {
                for ((kind, socket) in sockets) {
                    currentCoroutineContext().ensureActive()
                    val packet = DatagramPacket(buffer, buffer.size)
                    try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val payload = String(packet.data, packet.offset, packet.length, Charsets.UTF_8)
                    val server = parseDiscoveryReply(kind, payload, packet.address) ?: continue
                    val merged = mergeDiscovered(found, server, replyIsConsistent(payload, packet.address))
                    if (merged != found) {
                        found = merged
                        emit(orderDiscovered(found.map { it.first }))
                    }
                }
            }
        } finally {
            sockets.forEach { (_, socket) -> runCatching { socket.close() } }
        }
    }.flowOn(Dispatchers.IO)

    private fun openProbeSocket(): DatagramSocket = DatagramSocket(null).apply {
        reuseAddress = true
        broadcast = true
        bind(InetSocketAddress(0))
        soTimeout = RECEIVE_SLICE_MILLIS
    }

    companion object {
        const val DISCOVERY_PORT = 7359
        private const val RECEIVE_SLICE_MILLIS = 100
        internal val GLOBAL_BROADCAST: InetAddress = InetAddress.getByAddress(byteArrayOf(-1, -1, -1, -1))
    }
}

/** IPv4 broadcast address for an interface address and prefix length, e.g. 192.168.1.20/24. */
internal fun ipv4Broadcast(address: Inet4Address, prefixLength: Int): Inet4Address? {
    if (prefixLength !in 1..30) return null
    val bytes = address.address
    val value = bytes.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
    val mask = (0xffffffffL shl (32 - prefixLength)) and 0xffffffffL
    val broadcast = (value and mask) or (mask.inv() and 0xffffffffL)
    val out = ByteArray(4) { i -> ((broadcast shr (24 - 8 * i)) and 0xff).toByte() }
    return InetAddress.getByAddress(out) as Inet4Address
}
