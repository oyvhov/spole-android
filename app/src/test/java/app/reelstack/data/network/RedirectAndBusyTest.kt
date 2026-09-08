package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A reverse proxy that answers a plain address with a redirect is the most common way a working
 * server still looks broken. Redirects are never followed — that could hand a token to whatever
 * host the redirect names — so the message has to carry the address the user should type instead.
 */
class RedirectAndBusyTest {
    private class Fixed(private val response: HttpResponse) : JsonHttpTransport {
        override fun get(url: String, headers: Map<String, String>) = response
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = response
    }

    // https, because EndpointValidator refuses cleartext for a name that is not on the LAN — the
    // check under test here is the status handling, not the address rule.
    private val jellyfin = ServiceConnection(
        ServiceKind.JELLYFIN, "Heimetenar", "https://media.example", "token", userId = "me",
    )

    @Test fun redirectNamesTheAddressToUseInstead() {
        val message = redirectMessage(ServiceKind.JELLYFIN, "https://media.example/System/Info")

        assertTrue(message, message.contains("https://media.example"))
        assertTrue(message, message.contains("Innstillingar"))
        // The path is not something anyone types into an address field, and a query string could
        // carry a token, so neither belongs in a message.
        assertFalse(message, message.contains("System/Info"))
    }

    @Test fun redirectKeepsAnExplicitPort() {
        assertTrue(redirectMessage(ServiceKind.EMBY, "https://media.example:8920/emby").contains("https://media.example:8920"))
    }

    @Test fun relativeRedirectStillExplainsItself() {
        val message = redirectMessage(ServiceKind.SEERR, "/login")

        assertFalse(message, message.contains("/login"))
        assertTrue(message, message.contains("ei anna adresse"))
    }

    @Test fun missingLocationStillExplainsItself() {
        assertTrue(redirectMessage(ServiceKind.SONARR, null).contains("ei anna adresse"))
    }

    @Test fun connectionTestReportsTheRedirectRatherThanAStatusNumber() {
        val result = ServiceConnectionTester(
            Fixed(HttpResponse(301, "", location = "https://media.example/")),
        ).test(jellyfin)

        assertFalse(result.success)
        assertTrue(result.message, result.message.contains("https://media.example"))
        assertFalse(result.message, result.message.contains("301"))
    }

    @Test fun connectionTestKeepsSayingSomethingUsefulForAServerError() {
        val result = ServiceConnectionTester(Fixed(HttpResponse(503, ""))).test(jellyfin)

        assertFalse(result.success)
        assertTrue(result.message, result.message.contains("utilgjengeleg"))
    }

    @Test fun busyRepeatsTheServersOwnRetryAfter() {
        assertEquals(
            "Seerr er mellombels oppteken. Prøv igjen om 30 sekund.",
            busyMessage(ServiceKind.SEERR, 30),
        )
        assertEquals(
            "Seerr er mellombels oppteken. Prøv igjen om 2 minutt.",
            busyMessage(ServiceKind.SEERR, 120),
        )
    }

    @Test fun busyWithoutARetryAfterDoesNotInventOne() {
        assertEquals("Radarr er mellombels oppteken", busyMessage(ServiceKind.RADARR, null))
        assertEquals("Radarr er mellombels oppteken", busyMessage(ServiceKind.RADARR, 0))
    }

    @Test fun aServiceFailureIsMarkedAsWrittenForTheUser() {
        val failure = runCatching {
            AccountProfileClient(transport = Fixed(HttpResponse(401, ""))).load(jellyfin)
        }.exceptionOrNull()

        // The UI only repeats a message when it came from here. Anything else is a defect in
        // Spole, and its text is not advice.
        assertTrue(failure is ServiceMessage)
        assertEquals(failure?.message, failure?.readableMessage())
        assertEquals(null, IllegalStateException("Index 3 out of bounds").readableMessage())
    }
}
