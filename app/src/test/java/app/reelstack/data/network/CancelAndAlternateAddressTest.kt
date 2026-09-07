package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CancelAndAlternateAddressTest {
    private class Fake(
        private val responder: (method: String, url: String) -> HttpResponse,
    ) : JsonHttpTransport {
        val calls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls += "GET $url"
            return responder("GET", url)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            calls += "POST $url"
            return responder("POST", url)
        }
        override fun delete(url: String, headers: Map<String, String>): HttpResponse {
            calls += "DELETE $url"
            return responder("DELETE", url)
        }
    }

    private val seerr = ServiceConnection(
        ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", userId = "7", sessionCookie = true,
    )

    private fun me(id: String) = """{"id":$id,"displayName":"Øyvind","permissions":32}"""

    @Test fun withdrawingSendsDeleteForYourOwnRequest() {
        val transport = Fake { method, url ->
            when {
                url.endsWith("auth/me") -> HttpResponse(200, me("7"))
                method == "GET" && url.endsWith("request/42") -> HttpResponse(200, """{"id":42,"requestedBy":{"id":7}}""")
                method == "DELETE" && url.endsWith("request/42") -> HttpResponse(204, "")
                else -> HttpResponse(404, "")
            }
        }
        SeerrServiceClient(transport).cancelRequest(seerr, 42, "7")
        assertTrue(transport.calls.any { it == "DELETE https://seerr.example/api/v1/request/42" })
    }

    /** Seerr would refuse anyway, but the app must not send the delete in the first place. */
    @Test fun anotherPersonsRequestIsNeverDeleted() {
        val transport = Fake { method, url ->
            when {
                url.endsWith("auth/me") -> HttpResponse(200, me("7"))
                method == "GET" && url.endsWith("request/42") -> HttpResponse(200, """{"id":42,"requestedBy":{"id":9}}""")
                else -> HttpResponse(204, "")
            }
        }
        val error = runCatching { SeerrServiceClient(transport).cancelRequest(seerr, 42, "7") }.exceptionOrNull()
        assertEquals("Denne førespurnaden tilhøyrer ein annan konto.", error?.message)
        assertFalse(transport.calls.any { it.startsWith("DELETE") })
    }

    @Test fun anAdministratorKeyCannotWithdrawAsSomeoneElse() {
        val apiKey = seerr.copy(sessionCookie = false)
        val transport = Fake { _, _ -> HttpResponse(200, me("7")) }
        val error = runCatching { SeerrServiceClient(transport).cancelRequest(apiKey, 42, "7") }.exceptionOrNull()
        assertEquals(
            "Logg inn personleg i Seerr for å trekkje tilbake ein førespurnad.",
            error?.message,
        )
        assertFalse(transport.calls.any { it.startsWith("DELETE") })
    }

    @Test fun aRequestAlreadyGoneIsReportedPlainly() {
        val transport = Fake { _, url ->
            if (url.endsWith("auth/me")) HttpResponse(200, me("7")) else HttpResponse(404, "")
        }
        val error = runCatching { SeerrServiceClient(transport).cancelRequest(seerr, 42, "7") }.exceptionOrNull()
        assertEquals("Førespurnaden finst ikkje lenger i Seerr.", error?.message)
    }

    /** The two addresses are two routes to one server, so local data must not be keyed on the route. */
    @Test fun theServerIdentityIsFixedWhileTheAddressCanChange() {
        val home = ServiceConnection(
            ServiceKind.JELLYFIN, "Heime", "http://192.168.1.20:8096", "token",
            alternateUrl = "https://spole.example", identityUrl = "http://192.168.1.20:8096",
        )
        val away = home.copy(baseUrl = home.alternateUrl, alternateUrl = home.baseUrl)

        assertEquals(home.identity, away.identity)
        assertTrue(home.hasAlternate)
        assertEquals("http://192.168.1.20:8096", away.identity)
    }

    @Test fun aConnectionWithoutAnIdentityFallsBackToItsAddress() {
        val legacy = ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example", "token")
        assertEquals("https://emby.example", legacy.identity)
        assertFalse(legacy.hasAlternate)
    }
}
