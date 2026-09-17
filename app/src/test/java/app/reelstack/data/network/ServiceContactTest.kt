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

    /** Which sentence the failure names, and what it puts into it. */
    private fun failure(block: () -> Any?) = runCatching { block() }.exceptionOrNull()?.localizedFailure()

    private fun message(block: () -> Any?) = failure(block)?.resId

    private fun args(block: () -> Any?) = failure(block)?.args.orEmpty().joinToString(" ")

    @Test fun jellyfinLoginReportsContactFailureInsteadOfTheRawHostname() {
        val transport = Failing(UnknownHostException("jellyfin.tunet"))
        val client = JellyfinAuthenticationClient(transport)
        listOf<() -> Any?>(
            { client.authenticate("https://jellyfin.example", "me", "secret") },
            { client.initiateQuickConnect("https://jellyfin.example") },
            { client.quickConnectState("https://jellyfin.example", "secret") },
            { client.authenticateWithQuickConnect("https://jellyfin.example", "secret") },
        ).forEach { call ->
            assertEquals(app.reelstack.R.string.err_kontakt_sjekk_adresse, message(call))
            assertEquals("Jellyfin", args(call))
            assertFalse(args(call).contains("jellyfin.tunet"))
        }
    }

    @Test fun seerrLoginReportsContactFailureInsteadOfTheRawAddressAndPort() {
        val transport = Failing(ConnectException("Failed to connect to /10.0.0.5:5055"))
        val call = { SeerrAuthenticationClient(transport).authenticate("https://seerr.example", "me", "secret") }
        assertEquals(app.reelstack.R.string.err_kontakt_sjekk_adresse, message(call))
        assertEquals("Seerr", args(call))
        assertFalse(args(call).contains("10.0.0.5"))
    }

    /** A self-hosted certificate is a different problem from an unreachable server. */
    @Test fun anUntrustedCertificateGetsItsOwnNextStep() {
        val transport = Failing(SSLHandshakeException("Trust anchor for certification path not found."))
        val call = { JellyfinAuthenticationClient(transport).authenticate("https://jellyfin.example", "me", "s") }
        assertEquals(app.reelstack.R.string.err_klarte_ikkje_opprette_trygg, message(call))
        assertEquals("Jellyfin", args(call))
        assertFalse(args(call).contains("Trust anchor"))
    }

    @Test fun theConnectionTestNamesTheServiceItCouldNotReach() {
        val transport = Failing(UnknownHostException("radarr.tunet"))
        val call = {
            ServiceConnectionTester(transport).test(
                ServiceConnection(ServiceKind.RADARR, "Radarr", "https://radarr.example", "key"),
            )
        }
        assertEquals(app.reelstack.R.string.err_kontakt_sjekk_adresse, message(call))
        assertEquals("Radarr", args(call))
        assertFalse(args(call).contains("radarr.tunet"))
    }

    /** A reverse proxy that answers 200 with an HTML error page must not leak parser text. */
    @Test fun aNonJsonSeerrResponseIsReportedAsAnUnexpectedReply() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, "<html>502</html>")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) =
                HttpResponse(200, "<html>502 Bad Gateway</html>")
        }
        val call = { SeerrAuthenticationClient(transport).initiateQuickConnect("https://seerr.example") }
        assertEquals(app.reelstack.R.string.err_sende_eit_uventa_svar, message(call))
        assertFalse(args(call).contains("JSON"))
        assertFalse(args(call).contains("html"))
    }

    @Test fun aReachableServerStillReportsItsOwnStatusCode() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(401, "")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(401, "")
        }
        assertEquals(
            app.reelstack.R.string.err_feil_brukarnamn_eller_passord,
            message { JellyfinAuthenticationClient(transport).authenticate("https://jellyfin.example", "me", "s") },
        )
        val result = ServiceConnectionTester(transport)
            .test(ServiceConnection(ServiceKind.SONARR, "Sonarr", "https://sonarr.example", "key"))
        assertFalse(result.success)
        assertEquals(app.reelstack.R.string.conn_key_refused, result.message.resId)
    }

    @Test fun aWorkingConnectionTestStillSucceeds() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = HttpResponse(200, """{"version":"4.0.0"}""")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        val result = ServiceConnectionTester(transport)
            .test(ServiceConnection(ServiceKind.RADARR, "Radarr", "https://radarr.example", "key"))
        assertTrue(result.success)
        assertEquals(app.reelstack.R.string.conn_ok_version, result.message.resId)
        assertEquals(listOf<Any>("4.0.0"), result.message.args)
    }
}
