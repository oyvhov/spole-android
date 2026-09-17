package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Advice written in the data layer has to survive the trip to the screen.
 *
 * Every sentence below was already written, carefully and in the second person — "check the
 * sign-in before sending", "pick at least one season" — and every one of them was thrown as a
 * `check()`. The UI is right to treat an `IllegalStateException` as a defect and replace its text
 * with a generic message, because most of them really are defects. So the advice was written,
 * shipped, and never read by anyone.
 *
 * These tests do not check wording. They check that the failure carries a message at all, and that
 * it is the right one — which is the part that was broken, and the part that a translation cannot
 * fix on its own.
 */
class AdviceReachesTheReaderTest {
    private class Fake(
        private val responder: (method: String, url: String) -> HttpResponse,
    ) : JsonHttpTransport {
        override fun get(url: String, headers: Map<String, String>) = responder("GET", url)
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = responder("POST", url)
        override fun delete(url: String, headers: Map<String, String>) = responder("DELETE", url)
    }

    private val seerr = ServiceConnection(
        ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true,
    )

    private fun me(id: String, permissions: Int = 32) =
        """{"id":$id,"displayName":"Øyvind","permissions":$permissions}"""

    private fun failureOf(block: () -> Unit) = runCatching(block).exceptionOrNull()

    @Test fun anAdministratorKeyIsToldToSignInPersonally() {
        val apiKey = seerr.copy(sessionCookie = false)
        val error = failureOf { SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7")) }).request(apiKey, "movie", 11) }

        assertTrue("the UI only repeats a message it was given", error is ServiceMessage)
        assertEquals(R.string.err_seerr_logg_inn_personleg_send, error?.localizedFailure()?.resId)
    }

    @Test fun signingInAsSomeoneElseBetweenOpeningAndSendingIsNamed() {
        // The profile Seerr answers with is not the one the sheet was opened for.
        val error = failureOf {
            SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("9")) }).request(seerr, "movie", 11)
        }

        assertEquals(R.string.err_seerr_konto_endra_send, error?.localizedFailure()?.resId)
    }

    @Test fun anAccountWithoutPermissionForThatTypeIsToldWhy() {
        val error = failureOf {
            SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7", permissions = 0)) }).request(seerr, "movie", 11)
        }

        assertEquals(R.string.err_seerr_medietype_ikkje_lov, error?.localizedFailure()?.resId)
    }

    @Test fun aSeriesRequestWithoutSeasonsSaysWhatToDo() {
        val error = failureOf {
            SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7")) }).request(seerr, "tv", 11, seasons = emptySet())
        }

        assertEquals(R.string.err_vel_minst_ein_sesong, error?.localizedFailure()?.resId)
    }

    @Test fun aWithdrawalWithoutARequestIdIsNotACrash() {
        val error = failureOf {
            SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7")) }).cancelRequest(seerr, 0, "7")
        }

        assertEquals(R.string.err_forespurnad_manglar_id, error?.localizedFailure()?.resId)
    }

    @Test fun aSetupLinkIsRejectedWithTheReasonItWasRejectedFor() {
        val cases = mapOf(
            "https://media.example" to R.string.err_oppsettslenkje_ikkje_spole,
            "spole://setup?jellyfin" to R.string.err_oppsettslenkje_ufullstendig,
            "spole://setup?jellyfin=a&radarr=b" to R.string.err_oppsettslenkje_ukjende_felt,
            "spole://setup?" + "x".repeat(4200) to R.string.err_oppsettslenkje_for_lang,
        )
        for ((link, expected) in cases) {
            val error = failureOf { SetupLink.parse(link) }
            assertTrue(link, error is InvalidEndpoint)
            assertEquals(link, expected, error?.localizedFailure()?.resId)
        }
    }

    @Test fun aSetupLinkNeverCarriesCredentialsOutOfTheApp() {
        // The rule this guards is not a message: a link is how a server address leaves the device,
        // and a token in one would leave with it.
        val error = failureOf { SetupLink(jellyfin = "https://user:secret@media.example").encode() }

        assertEquals(R.string.err_oppsettslenkje_berre_adresser, error?.localizedFailure()?.resId)
    }

    @Test fun aDefectIsStillNotPresentedAsAdvice() {
        // The counterpart rule. Moving sentences into resources must not turn every internal
        // assertion into something the app repeats to the reader.
        assertNotNull(failureOf { SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7")) }).request(seerr, "book", 11) })
        assertEquals(null, failureOf {
            SeerrServiceClient(Fake { _, _ -> HttpResponse(200, me("7")) }).request(seerr, "book", 11)
        }?.localizedFailure())
    }
}
