package app.reelstack.data.network

import app.reelstack.data.model.*
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class EmbyAuthenticationTest {
    private class Fake(val response: HttpResponse) : JsonHttpTransport {
        var url = ""; var body = ""; var headers = emptyMap<String, String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse = error("No GET for login")
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            this.url = url; this.headers = headers; body = jsonBody; return response
        }
    }
    @Test fun usesServerAccountAndEscapesPasswordWithoutPuttingItInUrlOrHeaders() {
        val fake = Fake(HttpResponse(200, """{"AccessToken":"personal-token","User":{"Id":"me"}}"""))
        val auth = EmbyAuthenticationClient(fake).authenticate("https://emby.example/base", "Øyvind", "a\"b")
        assertEquals("me", auth.userId)
        assertEquals("personal-token", auth.accessToken)
        assertEquals("https://emby.example/base/Users/AuthenticateByName", fake.url)
        assertTrue(fake.headers.getValue("X-Emby-Authorization").startsWith("Emby "))
        assertEquals("""{"Username":"Øyvind","Pw":"a\"b"}""", fake.body)
        assertFalse(fake.url.contains("a\"b"))
        assertFalse(fake.headers.values.any { it.contains("a\"b") })
    }
    @Test fun acceptsEmptyPasswordForPasswordlessServerAccount() {
        val fake = Fake(HttpResponse(200, """{"AccessToken":"token","User":{"Id":"me"}}"""))
        EmbyAuthenticationClient(fake).authenticate("https://e.example", "me", "")
        assertTrue(fake.body.contains("\"Pw\":\"\""))
    }
    @Test fun rejectionHasUsefulMessageWithoutLeakingServerResponse() {
        val fake = Fake(HttpResponse(401, "private-password"))
        val error = runCatching { EmbyAuthenticationClient(fake).authenticate("https://e.example", "me", "secret") }.exceptionOrNull()
        assertEquals(app.reelstack.R.string.err_feil_emby_brukarnamn_eller, error?.localizedFailure()?.resId)
    }
    @Test fun incompleteAuthenticationIsNotSuccess() {
        listOf("{}", """{"AccessToken":"","User":{"Id":"me"}}""", """{"AccessToken":"token","User":{"Id":""}}""").forEach {
            assertTrue(runCatching { EmbyAuthenticationClient(Fake(HttpResponse(200, it))).authenticate("https://e.example", "me", "") }.isFailure)
        }
    }
    @Test fun serverFailureKeepsTheUsefulStatusCode() {
        val error = runCatching {
            EmbyAuthenticationClient(Fake(HttpResponse(500, "private server trace")))
                .authenticate("https://e.example", "me", "secret")
        }.exceptionOrNull()
        assertEquals(app.reelstack.R.string.err_emby_fekk_ein_tenarfeil, error?.localizedFailure()?.resId)
        assertFalse(error?.message.orEmpty().contains("private server trace"))
    }

    @Test fun networkFailureHasAnActionableSafeMessage() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = error("unused")
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
                throw IOException("private host detail")
        }
        val error = runCatching { EmbyAuthenticationClient(transport).authenticate("https://e.example", "me", "secret") }
            .exceptionOrNull()
        assertEquals(app.reelstack.R.string.err_fekk_ikkje_kontakt_med_2, error?.localizedFailure()?.resId)
    }
    @Test fun linkedAccountRequiresPersonalSeerrIdentityAndMatchingMediaId() {
        val account = ServiceAccount(ServiceKind.SEERR, "7", "Same name", mediaUserId = "abc-def")
        assertTrue(matchesJellyfinAccount(account, "ABCDEF"))
        assertFalse(matchesJellyfinAccount(account.copy(isPersonal = false), "abcdef"))
        assertFalse(matchesJellyfinAccount(account.copy(mediaUserId = null), "abcdef"))
        assertFalse(matchesJellyfinAccount(account, "another-user"))
        assertFalse(matchesJellyfinAccount(account.copy(source = ServiceKind.EMBY), "abcdef"))
        assertFalse(matchesJellyfinAccount(account, ""))
    }

    @Test fun loadsPublicUsersWithoutDeviceAuthorizationHeaders() {
        val fake = object : JsonHttpTransport {
            var sentHeaders = emptyMap<String, String>()
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                sentHeaders = headers
                return HttpResponse(200, """[{"Id":"kid-1","Name":"Eilev","HasPassword":true}]""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("unused")
        }
        val users = EmbyAuthenticationClient(fake).publicUsers("https://emby.example")
        assertEquals(1, users.size)
        assertEquals("Eilev", users.first().name)
        assertTrue(users.first().hasPassword)
        assertFalse(fake.sentHeaders.containsKey("X-Emby-Authorization"))
    }
}
