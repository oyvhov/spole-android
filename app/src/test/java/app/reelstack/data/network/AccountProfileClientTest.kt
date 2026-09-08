package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AccountProfileClientTest {
    private val jellyfin = ServiceConnection(
        kind = ServiceKind.JELLYFIN, name = "Manually entered label", baseUrl = "https://media.example.com/jellyfin",
        token = "test-user-token", userId = "manually-picked-id",
    )
    private val seerr = ServiceConnection(
        kind = ServiceKind.SEERR, name = "Manually entered label", baseUrl = "https://media.example.com/seerr",
        token = "connect.sid=test-session; XSRF-TOKEN=test%20csrf", userId = "99", sessionCookie = true,
    )
    private val emby = ServiceConnection(
        kind = ServiceKind.EMBY, name = "Emby", baseUrl = "https://emby.example.com",
        token = "personal-emby-token", userId = "emby-user-42",
    )

    @Test
    fun jellyfinIdentityComesFromTokenUserAndAvatarUsesServerIdAndImageTag() {
        val transport = RecordingTransport(HttpResponse(200, """{"Id":"actual-user","Name":"Server Name","PrimaryImageTag":"tag +1"}"""))
        val client = AccountProfileClient(deviceId = "test-device", transport = transport)

        val account = client.load(jellyfin)

        assertEquals(ServiceKind.JELLYFIN, account.source)
        assertEquals("actual-user", account.id)
        assertEquals("Server Name", account.displayName)
        assertEquals("Server Name", account.username)
        assertTrue(account.isPersonal)
        assertEquals("${jellyfin.baseUrl}/Users/actual-user/Images/Primary?tag=tag%20%2B1", account.avatarUrl)
        assertEquals(listOf("${jellyfin.baseUrl}/Users/Me"), transport.urls)
        val headers = transport.headers.single()
        assertTrue(headers["Authorization"].orEmpty().contains("DeviceId=\"test-device\""))
        assertTrue(headers["Authorization"].orEmpty().contains("Token=\"test-user-token\""))
        assertEquals(headers, client.avatarHeaders(jellyfin, account.avatarUrl))
        assertFalse(account.toString().contains(jellyfin.token))
    }

    @Test
    fun jellyfinWithoutAnImageTagHasNoInventedAvatar() {
        listOf("", ",\"PrimaryImageTag\":null", ",\"PrimaryImageTag\":\" \"").forEach { tag ->
            val transport = RecordingTransport(HttpResponse(200, """{"Id":"actual-user","Name":"Maya"$tag}"""))

            assertEquals(null, AccountProfileClient(transport = transport).load(jellyfin).avatarUrl)
        }
    }

    @Test
    fun embyLoadsTheAuthenticatedUserByIdInsteadOfJellyfinUsersMe() {
        val transport = RecordingTransport(HttpResponse(200,
            """{"Id":"emby-user-42","Name":"Øyvind","PrimaryImageTag":"portrait"}"""))

        val account = AccountProfileClient(deviceId = "test-device", transport = transport).load(emby)

        assertEquals(ServiceKind.EMBY, account.source)
        assertEquals("emby-user-42", account.id)
        assertEquals("Øyvind", account.displayName)
        assertEquals(listOf("https://emby.example.com/Users/emby-user-42"), transport.urls)
        assertEquals(mapOf("X-Emby-Token" to "personal-emby-token"), transport.headers.single())
        assertFalse(transport.urls.single().endsWith("/Users/Me"))
        assertEquals("https://emby.example.com/Users/emby-user-42/Images/Primary?tag=portrait", account.avatarUrl)
    }

    @Test
    fun embyWithoutAuthenticatedUserIdDoesNotProbeAFalseIdentity() {
        val transport = RecordingTransport(HttpResponse(200, "{}"))
        val error = runCatching { AccountProfileClient(transport = transport).load(emby.copy(userId = "")) }
            .exceptionOrNull()

        assertEquals("Logg inn med Emby-kontoen din for å stadfeste profilen.", error?.message)
        assertTrue(transport.urls.isEmpty())
    }

    @Test
    fun seerrUsesSessionAndCsrfHeadersAndServerIdentityWithoutImpersonation() {
        val transport = RecordingTransport(HttpResponse(200,
            """{"id":7,"displayName":"Maya at home","username":"maya","avatar":"/avatarproxy/jf-7?v=3","email":"unused@example.com"}"""))
        val client = AccountProfileClient(transport = transport)

        val account = client.load(seerr)

        assertEquals(ServiceKind.SEERR, account.source)
        assertEquals("7", account.id)
        assertEquals("Maya at home", account.displayName)
        assertEquals("maya", account.username)
        assertEquals("${seerr.baseUrl}/avatarproxy/jf-7?v=3", account.avatarUrl)
        assertTrue(account.isPersonal)
        assertEquals(listOf("${seerr.baseUrl}/api/v1/auth/me"), transport.urls)
        assertEquals(mapOf("Cookie" to seerr.token, "X-XSRF-TOKEN" to "test csrf"), transport.headers.single())
        assertEquals(transport.headers.single(), client.avatarHeaders(seerr, account.avatarUrl))
        assertFalse(transport.headers.single().containsKey("X-API-User"))
        assertFalse(account.toString().contains("unused@example.com"))
        assertFalse(account.toString().contains(seerr.token))
    }

    @Test
    fun apiKeyAccountIsServerReturnedAdministratorAndIsNotPersonal() {
        val transport = RecordingTransport(HttpResponse(200, """{"id":1,"displayName":"Server admin","username":"owner"}"""))
        val apiConnection = seerr.copy(token = "test-api-key", sessionCookie = false)

        val account = AccountProfileClient(transport = transport).load(apiConnection)

        assertEquals("1", account.id)
        assertEquals("Server admin", account.displayName)
        assertFalse(account.isPersonal)
        assertEquals(mapOf("X-Api-Key" to "test-api-key"), transport.headers.single())
        assertFalse(transport.urls.single().contains("test-api-key"))
        assertFalse(transport.urls.single().contains("99"))
    }

    @Test
    fun administratorSignedInBySessionIsStillPersonal() {
        val transport = RecordingTransport(HttpResponse(200, """{"id":1,"displayName":"Owner","permissions":2}"""))

        assertTrue(AccountProfileClient(transport = transport).load(seerr).isPersonal)
    }

    @Test
    fun seerrNameFallbackUsesServerUsernamesAndNeverConnectionLabelOrEmail() {
        val cases = listOf(
            """{"id":7,"username":"local"}""" to "local",
            """{"id":7,"username":" ","jellyfinUsername":"jellyfin-user"}""" to "jellyfin-user",
            """{"id":7,"plexUsername":"plex-user"}""" to "plex-user",
            """{"id":7,"email":"private@example.com"}""" to "Brukar 7",
        )
        cases.forEach { (payload, expected) ->
            val account = AccountProfileClient(transport = RecordingTransport(HttpResponse(200, payload))).load(seerr)
            assertEquals(expected, account.displayName)
            assertEquals(if (expected == "Brukar 7") null else expected, account.username)
        }
    }

    @Test
    fun resolvesRelativeRootPrefixedAndExternalSeerrAvatars() {
        val cases = mapOf(
            "avatarproxy/user?v=2" to "${seerr.baseUrl}/avatarproxy/user?v=2",
            "/avatarproxy/user?v=2" to "${seerr.baseUrl}/avatarproxy/user?v=2",
            "/seerr/avatarproxy/user?v=2" to "${seerr.baseUrl}/avatarproxy/user?v=2",
            "https://www.gravatar.com/avatar/hash?s=96" to "https://www.gravatar.com/avatar/hash?s=96",
            "//www.gravatar.com/avatar/hash" to "https://www.gravatar.com/avatar/hash",
        )
        cases.forEach { (avatar, expected) ->
            val transport = RecordingTransport(HttpResponse(200, """{"id":7,"avatar":"$avatar"}"""))

            assertEquals(expected, AccountProfileClient(transport = transport).load(seerr).avatarUrl)
            assertEquals(1, transport.urls.size) // Loading identity never fetches third-party avatars.
        }
    }

    @Test
    fun rejectsUnsafeAvatarUrlsWithoutLosingIdentity() {
        listOf(
            "javascript:alert(1)", "data:image/png;base64,abcd", "file:///etc/passwd", "ftp://media.example.com/a",
            "https://user:password@external.example.com/a", "https:///missing-host", "http://public.example.com/a",
            "../outside/avatar", "/avatarproxy/../../outside", "/%2e%2e/outside/avatar", "/avatar%5cproxy/x",
            "https://external.example.com/avatar#fragment", "https://external.example.com/%0aavatar",
        ).forEach { avatar ->
            val transport = RecordingTransport(HttpResponse(200, """{"id":7,"username":"Maya","avatar":"$avatar"}"""))

            val account = AccountProfileClient(transport = transport).load(seerr)

            assertEquals("Maya", account.displayName)
            assertEquals("Unexpected avatar accepted: $avatar", null, account.avatarUrl)
        }
    }

    @Test
    fun avatarHeadersNeverForwardCredentialsOutsideConnectionOriginAndBasePath() {
        val client = AccountProfileClient(transport = RecordingTransport())
        listOf(
            null, "https://www.gravatar.com/avatar/hash", "https://media.example.com.evil.example/seerr/avatar",
            "https://media.example.com:444/seerr/avatar", "http://media.example.com/seerr/avatar",
            "https://media.example.com/other-app/avatar", "https://media.example.com/seerr-other/avatar",
            "https://media.example.com/seerr/../other-app/avatar", "https://media.example.com/seerr/%2e%2e/avatar",
            "https://user:password@media.example.com/seerr/avatar", "file:///avatar", "/seerr/avatar",
        ).forEach { avatar ->
            assertTrue(client.avatarHeaders(seerr, avatar).isEmpty())
            assertTrue(client.avatarHeaders(seerr.copy(token = "test-api-key", sessionCookie = false), avatar).isEmpty())
            assertTrue(client.avatarHeaders(jellyfin, avatar).isEmpty())
        }
        assertEquals(seerrCookieHeaders(seerr.token), client.avatarHeaders(seerr, "https://media.example.com:443/seerr/avatar"))
    }

    @Test
    fun supportsExistingLanHttpPolicyForAvatars() {
        val connection = seerr.copy(baseUrl = "http://192.168.1.4:5055/seerr")
        val transport = RecordingTransport(HttpResponse(200, """{"id":7,"avatar":"/avatarproxy/user"}"""))
        val client = AccountProfileClient(transport = transport)

        val account = client.load(connection)

        assertEquals("${connection.baseUrl}/avatarproxy/user", account.avatarUrl)
        assertEquals(seerrCookieHeaders(connection.token), client.avatarHeaders(connection, account.avatarUrl))
    }

    @Test
    fun everyLoadRevalidatesIdentityIncludingAfterSwitchingAccounts() {
        val transport = RecordingTransport(
            HttpResponse(200, """{"id":7,"username":"First"}"""),
            HttpResponse(200, """{"id":8,"username":"Second"}"""),
            HttpResponse(401, "private response body"),
        )
        val client = AccountProfileClient(transport = transport)

        assertEquals("7", client.load(seerr).id)
        assertEquals("8", client.load(seerr.copy(token = "connect.sid=other-session")).id)
        assertTrue(runCatching { client.load(seerr) }.isFailure)
        assertEquals(3, transport.urls.size)
        assertEquals("connect.sid=other-session", transport.headers[1]["Cookie"])
    }

    @Test
    fun jellyfinApiKeyWithoutUserNeverFallsBackToManuallySelectedProfile() {
        val transport = RecordingTransport(HttpResponse(400, "test-token-private-body"))

        val failure = runCatching { AccountProfileClient(transport = transport).load(jellyfin) }.exceptionOrNull()

        assertTrue(failure is ServiceMessage)
        assertTrue(failure?.message.orEmpty().contains("personleg konto"))
        assertEquals(listOf("${jellyfin.baseUrl}/Users/Me"), transport.urls)
        assertFalse(failure.toString().contains("test-token-private-body"))
    }

    @Test
    fun errorsDoNotExposeResponseBodiesOrCredentials() {
        listOf(401, 403, 404, 429, 500, 503).forEach { code ->
            val transport = RecordingTransport(HttpResponse(code, "private-body-with-test-secret"))
            val failure = runCatching { AccountProfileClient(transport = transport).load(seerr) }.exceptionOrNull()

            assertTrue(failure is ServiceMessage)
            assertTrue(failure?.message.orEmpty().contains("Seerr"))
            assertFalse(failure.toString().contains("test-secret"))
            assertFalse(failure.toString().contains(seerr.token))
            assertEquals(null, failure?.cause)
        }
    }

    @Test
    fun rejectsMalformedProfilesAndMissingServerIds() {
        listOf("private invalid json", "null", "[]", "{}", """{"id":true}""", """{"id":0}""", """{"id":{}}""").forEach { body ->
            val transport = RecordingTransport(HttpResponse(200, body))
            val failure = runCatching { AccountProfileClient(transport = transport).load(seerr) }.exceptionOrNull()
            assertTrue(failure is ServiceMessage)
            assertEquals(null, failure?.cause)
        }
        listOf("{}", """{"Id":"user","Name":false}""", """{"Id":"user"}""").forEach { body ->
            assertTrue(runCatching { AccountProfileClient(transport = RecordingTransport(HttpResponse(200, body))).load(jellyfin) }.isFailure)
        }
    }

    @Test
    fun transportFailureHasSafeActionableMessage() {
        val failure = runCatching { AccountProfileClient(transport = RecordingTransport()).load(seerr) }.exceptionOrNull()

        assertTrue(failure is ServiceMessage)
        assertTrue(failure?.message.orEmpty().contains("Fekk ikkje kontakt"))
        assertFalse(failure.toString().contains("private transport context"))
        assertEquals(null, failure?.cause)
    }

    @Test
    fun rejectsUnsupportedSourcesAndMissingCredentialsBeforeRequest() {
        val transport = RecordingTransport()
        val client = AccountProfileClient(transport = transport)

        assertTrue(runCatching { client.load(seerr.copy(kind = ServiceKind.RADARR)) }.isFailure)
        assertTrue(runCatching { client.load(seerr.copy(token = "")) }.isFailure)
        assertTrue(transport.urls.isEmpty())
    }

    private class RecordingTransport(vararg responses: HttpResponse) : JsonHttpTransport {
        private val responses = responses.toMutableList()
        val urls = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()

        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            this.headers += headers
            if (responses.isEmpty()) throw IOException("private transport context")
            return responses.removeAt(0)
        }

        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("Account loading must only use GET")
    }
}
