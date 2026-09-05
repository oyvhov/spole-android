package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class SeerrAuthenticationTest {
    @Test fun jellyfinAccountUsesSeerrSessionAndCsrfWithoutForwardingJellyfinToken() {
        val transport = FakeTransport(mutableListOf(
            HttpResponse(200, "{}", listOf("_csrf=secret; HttpOnly", "XSRF-TOKEN=csrf%2Btoken; Secure")),
            HttpResponse(200, "{\"id\":7}", listOf("connect.sid=s%3Asession; Path=/; HttpOnly")),
            HttpResponse(200, "{\"id\":7,\"displayName\":\"Maya\",\"permissions\":32}"),
            HttpResponse(201, "{}"),
        ))
        val login = SeerrAuthenticationClient(transport).authenticate("https://seerr.example.com/base", "ø yvind", "p@ss\"word")
        assertEquals("7", login.userId)
        assertTrue(login.accessToken.contains("connect.sid=s%3Asession"))
        assertFalse(login.accessToken.contains("HttpOnly"))
        assertEquals("https://seerr.example.com/base/api/v1/auth/jellyfin", transport.calls[1].url)
        assertEquals("csrf+token", transport.calls[1].headers["X-XSRF-TOKEN"])
        assertEquals("{\"username\":\"ø yvind\",\"password\":\"p@ss\\\"word\"}", transport.calls[1].body)
        val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example.com/base", login.accessToken, userId = login.userId, sessionCookie = true)
        SeerrServiceClient(transport).request(connection, "movie", 42)
        assertEquals(login.accessToken, transport.calls[3].headers["Cookie"])
        assertTrue(transport.calls[2].url.endsWith("/auth/me"))
        assertFalse(transport.calls.any { "X-Api-Key" in it.headers || "Authorization" in it.headers })
        assertFalse(transport.calls.any { it.url.contains("p@ss") || it.headers.values.any { value -> value.contains("p@ss") } })
    }

    @Test fun quickConnectCompletesAgainstSeerrAndKeepsItsOwnCookies() {
        val transport = FakeTransport(mutableListOf(
            HttpResponse(200, "{}"),
            HttpResponse(200, "{\"code\":\"123456\",\"secret\":\"private-secret\"}"),
            HttpResponse(200, "{\"authenticated\":true}"),
            HttpResponse(200, "{\"id\":9}", listOf("connect.sid=seerr-session; HttpOnly")),
        ))
        val client = SeerrAuthenticationClient(transport)
        val challenge = client.initiateQuickConnect("https://seerr.example.com")
        val approved = client.quickConnectState("https://seerr.example.com", challenge)
        assertTrue(approved.authenticated)
        assertEquals("123456", approved.code)
        val login = client.authenticateWithQuickConnect("https://seerr.example.com", approved)
        assertEquals("connect.sid=seerr-session", login.accessToken)
        assertTrue(transport.calls.all { it.url.startsWith("https://seerr.example.com/api/v1/") })
        assertEquals("{\"secret\":\"private-secret\"}", transport.calls.last().body)
    }

    @Test fun missingCookieIsNotTreatedAsSuccessfulLogin() {
        val transport = FakeTransport(mutableListOf(HttpResponse(200, "{}"), HttpResponse(200, "{\"id\":7}")))
        assertTrue(runCatching { SeerrAuthenticationClient(transport).authenticate("https://s.example", "a", "b") }.isFailure)
    }

    @Test fun oldSeerrOffersAccountLoginWhenQuickConnectIsMissing() {
        val transport = FakeTransport(mutableListOf(HttpResponse(200, "{}"), HttpResponse(404, "{}")))
        val error = runCatching { SeerrAuthenticationClient(transport).initiateQuickConnect("https://s.example") }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("Vel Jellyfin-konto"))
    }

    @Test fun rejectedCredentialsDoNotBecomeSession() {
        val transport = FakeTransport(mutableListOf(HttpResponse(200, "{}"), HttpResponse(401, "{}")))
        assertEquals("Feil Jellyfin-brukarnamn eller passord.", runCatching {
            SeerrAuthenticationClient(transport).authenticate("https://s.example", "a", "b")
        }.exceptionOrNull()?.message)
    }

    @Test fun cookieAllowlistDropsAttributesAndOtherSitesTrackingCookies() {
        assertEquals("connect.sid=new; _csrf=key", mergeSeerrCookies("connect.sid=old; XSRF-TOKEN=gone",
            listOf("connect.sid=new; Domain=example.com", "tracking=abc", "_csrf=key; HttpOnly", "XSRF-TOKEN=; Max-Age=0")))
        assertTrue(runCatching { seerrCookieHeaders("XSRF-TOKEN=%0D%0AInjected") }.isFailure)
    }

    @Test fun sessionProbeChecksOwnAccountAndApiKeyStillWorksForReads() {
        val transport = FakeTransport(mutableListOf(HttpResponse(200, "{\"id\":7}"), HttpResponse(200, "{}")))
        val account = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://s.example", "connect.sid=mine", sessionCookie = true)
        assertTrue(ServiceConnectionTester(transport).test(account).success)
        assertEquals("https://s.example/api/v1/auth/me", transport.calls.first().url)
        assertEquals("connect.sid=mine", transport.calls.first().headers["Cookie"])
        assertTrue(ServiceConnectionTester(transport).test(account.copy(token = "admin-key", sessionCookie = false)).success)
        assertEquals(mapOf("X-Api-Key" to "admin-key"), transport.calls.last().headers)
    }

    private data class Call(val url: String, val headers: Map<String, String>, val body: String = "")
    private class FakeTransport(val responses: MutableList<HttpResponse>) : JsonHttpTransport {
        val calls = mutableListOf<Call>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls += Call(url, headers)
            return responses.removeAt(0)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            calls += Call(url, headers, jsonBody)
            return responses.removeAt(0)
        }
    }
}
