package app.reelstack.data.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.concurrent.thread
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanServerDiscoveryTest {
    private fun ip(text: String) = InetAddress.getByName(text)

    @Test
    fun replyFromTheLanKeepsItsOwnAddress() {
        val server = parseDiscoveryReply(
            DiscoveredServerKind.JELLYFIN,
            """{"Address":"http://192.168.1.20:8097","Id":"4f1c0d2a","Name":"Stova","EndpointAddress":null}""",
            ip("192.168.1.20"),
        )
        assertEquals(DiscoveredServer(DiscoveredServerKind.JELLYFIN, "4f1c0d2a", "Stova", "http://192.168.1.20:8097"), server)
    }

    @Test
    fun virtualAdapterAddressIsReplacedByTheSender() {
        // Measured on the owner's Windows host: the WSL adapter answered with the Hyper-V address.
        val server = parseDiscoveryReply(
            DiscoveredServerKind.JELLYFIN,
            """{"Address":"http://172.26.192.1:8097","Id":"4f1c0d2a","Name":"Stova","EndpointAddress":null}""",
            ip("172.26.192.1"),
        )
        assertEquals("http://172.26.192.1:8097", server?.address)
    }

    @Test
    fun publishedHostNameIsKept() {
        val server = parseDiscoveryReply(
            DiscoveredServerKind.EMBY,
            """{"Address":"https://media.example.org","Id":"9b3e","Name":"Kjellaren"}""",
            ip("192.168.1.20"),
        )
        assertEquals("https://media.example.org", server?.address)
    }

    @Test
    fun defaultPortsAreLeftOutAndMissingNameFallsBackToHost() {
        val server = parseDiscoveryReply(DiscoveredServerKind.EMBY,
            """{"Address":"http://10.0.0.5:80","Id":"x"}""", ip("10.0.0.5"))
        assertEquals("http://10.0.0.5", server?.address)
        assertEquals("10.0.0.5", server?.name)
    }

    @Test
    fun malformedRepliesAreIgnored() {
        val sender = ip("192.168.1.2")
        listOf(
            "not json",
            """{"Id":"x","Name":"no address"}""",
            """{"Address":"http://192.168.1.2:8096","Name":"no id"}""",
            """{"Address":"ftp://192.168.1.2","Id":"x"}""",
            """{"Address":"","Id":"x"}""",
        ).forEach { assertNull(it, parseDiscoveryReply(DiscoveredServerKind.JELLYFIN, it, sender)) }
    }

    @Test
    fun consistentReplyReplacesAnEarlierRewrittenOne() {
        val rewritten = DiscoveredServer(DiscoveredServerKind.JELLYFIN, "a", "Stova", "http://172.26.192.1:8097")
        val lan = rewritten.copy(address = "http://192.168.1.20:8097")
        val merged = mergeDiscovered(listOf(rewritten to false), lan, consistent = true)
        assertEquals(listOf(lan to true), merged)
        assertEquals(merged, mergeDiscovered(merged, rewritten, consistent = false))
    }

    @Test
    fun jellyfinListsBeforeEmbyThenByName() {
        val emby = DiscoveredServer(DiscoveredServerKind.EMBY, "1", "Kjellaren", "http://a:8096")
        val zulu = DiscoveredServer(DiscoveredServerKind.JELLYFIN, "2", "zulu", "http://b:8096")
        val alfa = DiscoveredServer(DiscoveredServerKind.JELLYFIN, "3", "Alfa", "http://c:8096")
        assertEquals(listOf(alfa, zulu, emby), orderDiscovered(listOf(emby, zulu, alfa)))
    }

    @Test
    fun broadcastAddressFollowsThePrefix() {
        val home = ip("192.168.1.20") as Inet4Address
        assertEquals("192.168.1.255", ipv4Broadcast(home, 24)?.hostAddress)
        assertEquals("192.168.3.255", ipv4Broadcast(home, 22)?.hostAddress)
        assertEquals("10.255.255.255", ipv4Broadcast(ip("10.1.2.3") as Inet4Address, 8)?.hostAddress)
        assertNull(ipv4Broadcast(home, 32))
    }

    @Test
    fun discoveryAttributesEachReplyToTheProbeItAnswers() = runBlocking {
        val responder = DatagramSocket(InetSocketAddress(ip("127.0.0.1"), 0)).apply { soTimeout = 3_000 }
        val replies = mapOf(
            "who is JellyfinServer?" to """{"Address":"http://127.0.0.1:8097","Id":"jf","Name":"Stova","EndpointAddress":null}""",
            "who is EmbyServer?" to """{"Address":"http://127.0.0.1:8096","Id":"em","Name":"Kjellaren"}""",
        )
        val worker = thread(isDaemon = true) {
            runCatching {
                repeat(2) {
                    val packet = DatagramPacket(ByteArray(512), 512)
                    responder.receive(packet)
                    val reply = replies[String(packet.data, 0, packet.length)]?.toByteArray() ?: return@repeat
                    responder.send(DatagramPacket(reply, reply.size, packet.socketAddress))
                }
            }
        }
        val discovery = LanServerDiscovery(
            broadcastTargets = { listOf(ip("127.0.0.1")) },
            port = responder.localPort,
            timeoutMillis = 800,
        )
        val found = discovery.discover().last()
        worker.join(2_000)
        responder.close()
        assertEquals(
            listOf(
                DiscoveredServer(DiscoveredServerKind.JELLYFIN, "jf", "Stova", "http://127.0.0.1:8097"),
                DiscoveredServer(DiscoveredServerKind.EMBY, "em", "Kjellaren", "http://127.0.0.1:8096"),
            ),
            found,
        )
    }

    /**
     * Opt-in check against the real network: `SPOLE_LAN_DISCOVERY=1 gradlew testDebugUnitTest --tests '*LanServerDiscoveryTest*'`.
     * Prints what answered; it asserts only that every address is one this machine can open.
     */
    @Test
    fun realNetworkWhenAskedFor() = runBlocking {
        org.junit.Assume.assumeTrue(System.getenv("SPOLE_LAN_DISCOVERY") == "1")
        val targets = java.net.NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.interfaceAddresses }
            .mapNotNull { address -> (address.address as? Inet4Address)?.let { ipv4Broadcast(it, address.networkPrefixLength.toInt()) } }
        val found = LanServerDiscovery({ targets + LanServerDiscovery.GLOBAL_BROADCAST }).discover().last()
        found.forEach { println("SPOLE-DISCOVERY ${it.kind} ${it.name} ${it.address} ${it.id}") }
        assertTrue(found.isNotEmpty())
        found.forEach { server ->
            val code = (java.net.URI("${server.address}/System/Info/Public").toURL().openConnection() as java.net.HttpURLConnection)
                .apply { connectTimeout = 3_000; readTimeout = 3_000 }.responseCode
            assertEquals(server.address, 200, code)
        }
    }

    @Test
    fun emptyNetworkEndsWithAnEmptyList() = runBlocking {
        val silent = DatagramSocket(InetSocketAddress(ip("127.0.0.1"), 0))
        val found = LanServerDiscovery({ listOf(ip("127.0.0.1")) }, port = silent.localPort, timeoutMillis = 300)
            .discover().last()
        silent.close()
        assertTrue(found.isEmpty())
    }
}
