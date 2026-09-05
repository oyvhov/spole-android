package app.reelstack.data.network

import app.reelstack.data.model.*
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
        assertEquals("Feil Emby-brukarnamn eller passord.", error?.message)
    }
    @Test fun incompleteAuthenticationIsNotSuccess() {
        listOf("{}", """{"AccessToken":"","User":{"Id":"me"}}""", """{"AccessToken":"token","User":{"Id":""}}""").forEach {
            assertTrue(runCatching { EmbyAuthenticationClient(Fake(HttpResponse(200, it))).authenticate("https://e.example", "me", "") }.isFailure)
        }
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
}
