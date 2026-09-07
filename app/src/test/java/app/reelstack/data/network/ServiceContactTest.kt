package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A transport failure is what the user actually hits first: a typo in the address, a server that
 * is off, or a self-signed certificate. `UnknownHostException` carries only the hostname and
 * `ConnectException` carries an internal address and port, so neither is usable as an error line.
 */
class ServiceContactTest {
    private class Failing(private val error: IOException) : JsonHttpTransport {
        override fun get(url: String, headers: Map<String, String>): HttpResponse = throw error
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = throw error
    }

    private fun message(block: () -> Unit): String = runCatching { block() }.exceptionOrNull()?.message.orEmpty()

    @Test fun jellyfinLoginReportsContactFailureInsteadOfTheRawHostname() {
        val transport = Failing(UnknownHostException("jellyfin.tunet"))
        val client = JellyfinAuthenticationClient(transport)
        listOf<() -> Unit>(
            { client.authenticate("https://jellyfin.example", "me", "secret") },
            { client.initiateQuickConnect("https://jellyfin.example") },
            { client.quickConnectState("https://jellyfin.example", "secret") },
            { client.authenticateWithQuickConnect("https://jellyfin.example", "secret") },
        ).forEach { call ->
            val text = message(call)
            assertEquals("Fekk ikkje kontakt med Jellyfin. Sjekk tenaradressa og nettet.", text)
            assertFalse(text.contains("jellyfin.tunet"))
        }
    }

    @Test fun seerrLoginReportsContactFailureInsteadOfTheRawAddressAndPort() {
        val transport = Failing(ConnectException("Failed to connect to /10.0.0.5:5055"))
        val text = message { SeerrAuthenticationClient(transport).authenticate("https://seerr.example", "me", "secret") }
        assertEquals("Fekk ikkje kontakt med Seerr. Sjekk tenaradressa og nettet.", text)
        assertFalse(text.contains("10.0.0.5"))
    }

    /** A self-hosted certificate is a different problem from an unreachable server. */
    @Test fun anUntrustedCertificateGetsItsOwnNextStep() {
        val transport = Failing(SSLHandshakeException("Trust anchor for certification path not found."))
        val text = message { JellyfinAuthenticationClient(transport).authenticate("https://jellyfin.example", "me", "s") }
        assertEquals(
            "Klarte ikkje å opprette ei trygg HTTPS-tilkopling til Jellyfin. " +
                "Sjekk at sertifikatet på tenaren er gyldig og tiltrudd.",
            text,
        )
        assertFalse(text.contains("Trust anchor"))
    }

    @Test fun theConnectionTestNamesTheServiceItCouldNotReach() {
        val transport = Failing(UnknownHostException("radarr.tunet"))
        val text = message {
            ServiceConnectionTester(transport).test(
                ServiceConnection(ServiceKind.RADARR, "Radarr", "https://radarr.example", "key"),
            )
        }
        assertEquals("Fekk ikkje kontakt med Radarr. Sjekk tenaradressa og nettet.", text)
        assertFalse(text.contains("radarr.tunet"))
    }

    /** A reverse proxy that answers 200 with an HTML error page must not leak parser text. */
    @Test fun aNonJsonSeerrResponseIsReportedAsAnUnexpectedReply() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, "<html>502</html>")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) =
                HttpResponse(200, "<html>502 Bad Gateway</html>")
        }
        val text = message { SeerrAuthenticationClient(transport).initiateQuickConnect("https://seerr.example") }
        assertEquals("Seerr sende eit uventa svar. Sjekk at adressa peikar på Seerr.", text)
        assertFalse(text.contains("JSON"))
        assertFalse(text.contains("html"))
    }

    @Test fun aReachableServerStillReportsItsOwnStatusCode() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(401, "")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(401, "")
        }
        assertEquals(
            "Feil brukarnamn eller passord",
            message { JellyfinAuthenticationClient(transport).authenticate("https://jellyfin.example", "me", "s") },
        )
        val result = ServiceConnectionTester(transport)
            .test(ServiceConnection(ServiceKind.SONARR, "Sonarr", "https://sonarr.example", "key"))
        assertFalse(result.success)
        assertEquals("API-nøkkelen vart avvist", result.message)
    }

    @Test fun aWorkingConnectionTestStillSucceeds() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, """{"version":"4.0.0"}""")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        val result = ServiceConnectionTester(transport)
            .test(ServiceConnection(ServiceKind.RADARR, "Radarr", "https://radarr.example", "key"))
        assertTrue(result.success)
        assertEquals("Tilkopla · v4.0.0", result.message)
    }
}
