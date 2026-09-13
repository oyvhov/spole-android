package app.reelstack.data.network

import app.reelstack.data.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CombinedQuickConnectTest {
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example/jellyfin",
        "test-jellyfin-token", userId = "abcd")

    private class Transport(var authorized: Boolean = true, var ready: Boolean = true,
        var supported: Boolean = true, var sameServer: Boolean = true) : JsonHttpTransport {
        data class Call(val url: String, val headers: Map<String, String>, val body: String)
        val calls = mutableListOf<Call>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            calls += Call(url, headers, "")
            if (url.contains("/QuickConnect/Connect?")) return if (sameServer)
                HttpResponse(200, """{"Secret":"test-secret","Code":"123456","Authenticated":false}""")
                else HttpResponse(404, "{}")
            return if (url.contains("/check?")) HttpResponse(200, "{\"authenticated\":$ready}")
            else HttpResponse(200, "{}", listOf("_csrf=test-csrf; Path=/"))
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            calls += Call(url, headers, jsonBody)
            return when {
                url.contains("/Authorize?") -> HttpResponse(200, authorized.toString())
                url.endsWith("/initiate") -> if (supported) HttpResponse(200, """{"code":"123456","secret":"test-secret"}""") else HttpResponse(404, "{}")
                else -> HttpResponse(200, """{"id":7}""", listOf("connect.sid=test-seerr-session; Path=/"))
            }
        }
    }

    private suspend fun connect(t: Transport, mediaId: String = "abcd", jfId: String = "abcd",
        pause: suspend () -> Unit = {}): ServiceConnection = connectSeerrWithJellyfin(jellyfin,
        "https://requests.example/seerr", JellyfinAuthenticationClient(t), SeerrAuthenticationClient(t),
        { c -> ServiceAccount(c.kind, if (c.kind == ServiceKind.JELLYFIN) jfId else "7", "Test", mediaUserId = mediaId) }, pause)

    @Test fun oneApprovalProducesSeparateVerifiedSessionsAndNeverForwardsJellyfinToken() = runBlocking {
        val t = Transport()
        val result = connect(t)
        assertTrue(result.sessionCookie)
        assertEquals("7", result.userId)
        assertTrue(result.token.contains("connect.sid=test-seerr-session"))
        val approval = t.calls.single { it.url.contains("/Authorize?") }
        assertEquals("https://media.example/jellyfin/QuickConnect/Authorize?code=123456", approval.url)
        assertTrue(approval.headers.getValue("Authorization").contains("Token=\"${jellyfin.token}\""))
        t.calls.filter { it.url.startsWith("https://requests.example/") }.forEach {
            assertFalse(it.headers.values.any { value -> value.contains(jellyfin.token) })
            assertFalse(it.body.contains(jellyfin.token))
        }
    }

    @Test fun wrongServerApprovalCannotAuthenticateSeerr() = runBlocking {
        val t = Transport(authorized = false)
        assertTrue(runCatching { connect(t) }.isFailure)
        assertFalse(t.calls.any { it.url.endsWith("/authenticate") })
    }

    @Test fun mismatchedIdentityCannotBeReturnedForSaving() = runBlocking {
        assertTrue(runCatching { connect(Transport(), mediaId = "someone-else") }.isFailure)
    }

    @Test fun otherJellyfinServerCannotAuthorizeACoincidentallyMatchingShortCode() = runBlocking {
        val t = Transport(sameServer = false)
        assertTrue(runCatching { connect(t) }.isFailure)
        assertFalse(t.calls.any { it.url.contains("/Authorize?") })
    }

    @Test fun unverifiableJellyfinDoesNotStartSeerrLogin() = runBlocking {
        val t = Transport()
        assertTrue(runCatching { connect(t, jfId = "wrong") }.isFailure)
        assertTrue(t.calls.isEmpty())
    }

    @Test fun unsupportedSeerrDoesNotAuthorizeAnything() = runBlocking {
        val t = Transport(supported = false)
        assertTrue(runCatching { connect(t) }.isFailure)
        assertFalse(t.calls.any { it.url.contains("/Authorize?") })
    }

    @Test fun pollingIsBoundedAndNeverAuthenticatesAnUnapprovedChallenge() = runBlocking {
        val t = Transport(ready = false)
        assertTrue(runCatching { connect(t) }.isFailure)
        assertEquals(10, t.calls.count { it.url.contains("/check?") })
        assertFalse(t.calls.any { it.url.endsWith("/authenticate") })
    }

    @Test fun cancellationStopsBeforeAnySessionIsReturned() = runBlocking {
        val t = Transport(ready = false)
        val failure = runCatching { connect(t, pause = { throw CancellationException("cancelled") }) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertFalse(t.calls.any { it.url.endsWith("/authenticate") })
    }
}
