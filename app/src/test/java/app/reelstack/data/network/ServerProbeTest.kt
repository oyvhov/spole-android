package app.reelstack.data.network

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerProbeTest {
    // Bodies as the owner's servers returned them on 6 October 2026.
    private val jellyfin = """{"LocalAddress":"http://192.168.1.20:8097","ServerName":"Stova","Version":"12.1.0","ProductName":"Jellyfin Server","OperatingSystem":"","Id":"4f1c0d2a","StartupWizardCompleted":true}"""
    private val emby = """{"LocalAddresses":[],"RemoteAddresses":[],"ServerName":"Kjellaren","Version":"4.11.0.6","Id":"9b3e77aa"}"""

    @Test
    fun jellyfinAndEmbyAreToldApart() {
        assertEquals(
            PublicServerInfo(DiscoveredServerKind.JELLYFIN, "4f1c0d2a", "Stova", "http://192.168.1.20:8097", "12.1.0"),
            parsePublicServerInfo("http://192.168.1.20:8097", jellyfin),
        )
        assertEquals(DiscoveredServerKind.EMBY, parsePublicServerInfo("http://192.168.1.20:8096", emby)?.kind)
    }

    @Test
    fun pagesWithoutAnIdAreNotServers() {
        assertNull(parsePublicServerInfo("http://192.168.1.1", "<html>router</html>"))
        assertNull(parsePublicServerInfo("http://192.168.1.1", """{"status":"ok"}"""))
    }

    @Test
    fun earlierCandidateWinsEvenWhenALaterOneAnswersToo() = runBlocking {
        val calls = AtomicInteger()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                calls.incrementAndGet()
                return when {
                    url.startsWith("http://10.0.0.4:8096/") -> throw IOException("connection refused")
                    url.startsWith("https://10.0.0.4:8920/") -> HttpResponse(200, jellyfin)
                    url.startsWith("http://10.0.0.4/") -> HttpResponse(200, emby)
                    else -> throw IOException("no route")
                }
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        val found = ServerProbe(transport).firstAnswering(serverAddressCandidates("10.0.0.4"))
        assertEquals("https://10.0.0.4:8920", found?.baseUrl)
        assertEquals("Stova", found?.name)
    }

    @Test
    fun seerrIsFoundBesideALanServerButNeverGuessedForAPublicName() {
        val asked = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                asked += url
                return HttpResponse(200, """{"version":"3.0.1","commitTag":"local"}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        val probe = ServerProbe(transport)
        assertEquals("http://192.168.1.20:5055", probe.seerrBeside("http://192.168.1.20:8097"))
        assertNull(probe.seerrBeside("https://media.example.org"))
        assertEquals(listOf("http://192.168.1.20:5055/api/v1/status"), asked)
    }

    @Test
    fun aServiceWithoutAVersionIsNotSeerr() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, """{"ok":true}""")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        assertNull(ServerProbe(transport).seerrBeside("http://192.168.1.20:8097"))
    }

    @Test
    fun nothingAnsweringGivesNull() = runBlocking {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse = HttpResponse(404, "")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        assertNull(ServerProbe(transport).firstAnswering(serverAddressCandidates("10.0.0.4")))
    }
}
