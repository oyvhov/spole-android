package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A reverse proxy that answers a plain address with a redirect is the most common way a working
 * server still looks broken. Redirects are never followed — that could hand a token to whatever
 * host the redirect names — so the message has to carry the address the user should type instead.
 *
 * These assertions name the *message*, not its wording. The sentences live in three languages now,
 * and matching their text here would mean the tests only ever checked one of them.
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

        assertEquals(R.string.err_vidaresending_adresse, message.resId)
        assertEquals(listOf<Any>("Jellyfin", "https://media.example"), message.args)
        // The path is not something anyone types into an address field, and a query string could
        // carry a token, so neither belongs in a message.
        assertFalse(message.args.toString(), message.args.toString().contains("System/Info"))
    }

    @Test fun redirectKeepsAnExplicitPort() {
        val message = redirectMessage(ServiceKind.EMBY, "https://media.example:8920/emby")

        assertEquals(R.string.err_vidaresending_adresse, message.resId)
        assertEquals("https://media.example:8920", message.args[1])
    }

    @Test fun relativeRedirectStillExplainsItself() {
        val message = redirectMessage(ServiceKind.SEERR, "/login")

        assertEquals(R.string.err_vidaresending_ukjend, message.resId)
        assertFalse(message.args.toString(), message.args.toString().contains("/login"))
    }

    @Test fun missingLocationStillExplainsItself() {
        assertEquals(R.string.err_vidaresending_ukjend, redirectMessage(ServiceKind.SONARR, null).resId)
    }

    @Test fun connectionTestReportsTheRedirectRatherThanAStatusNumber() {
        val result = ServiceConnectionTester(
            Fixed(HttpResponse(301, "", location = "https://media.example/")),
        ).test(jellyfin)

        assertFalse(result.success)
        assertEquals(R.string.err_vidaresending_adresse, result.message.resId)
        assertEquals("https://media.example", result.message.args[1])
    }

    @Test fun connectionTestKeepsSayingSomethingUsefulForAServerError() {
        val result = ServiceConnectionTester(Fixed(HttpResponse(503, ""))).test(jellyfin)

        assertFalse(result.success)
        // Not "status 503": the message names the service and says it is unavailable.
        assertEquals(R.string.err_utilgjengeleg, result.message.resId)
    }

    @Test fun busyRepeatsTheServersOwnRetryAfter() {
        val seconds = busyMessage(ServiceKind.SEERR, 30)
        assertEquals(R.string.err_oppteken_sekund, seconds.resId)
        assertEquals(listOf<Any>("Seerr", 30L), seconds.args)

        val minutes = busyMessage(ServiceKind.SEERR, 120)
        assertEquals(R.string.err_oppteken_minutt, minutes.resId)
        assertEquals(listOf<Any>("Seerr", 2L), minutes.args)
    }

    @Test fun busyWithoutARetryAfterDoesNotInventOne() {
        for (value in listOf<Long?>(null, 0)) {
            val message = busyMessage(ServiceKind.RADARR, value)
            assertEquals(R.string.err_mellombels_oppteken, message.resId)
            assertEquals(listOf<Any>("Radarr"), message.args)
        }
    }

    @Test fun aServiceFailureIsMarkedAsWrittenForTheUser() {
        val failure = runCatching {
            AccountProfileClient(transport = Fixed(HttpResponse(401, ""))).load(jellyfin)
        }.exceptionOrNull()

        // The UI only repeats a message when it came from here. Anything else is a defect in
        // Spole, and its text is not advice.
        assertTrue(failure is ServiceMessage)
        assertEquals(R.string.err_avviste_kontotilgangen_logg_inn, failure?.localizedFailure()?.resId)
        assertNull(IllegalStateException("Index 3 out of bounds").localizedFailure())
    }

    @Test fun aRejectedAddressAlsoCarriesItsOwnSentence() {
        val failure = runCatching { EndpointValidator.normalizeBaseUrl("https://media .example") }
            .exceptionOrNull()

        assertTrue(failure is InvalidEndpoint)
        assertEquals(R.string.endpoint_whitespace, failure?.localizedFailure()?.resId)
    }
}
