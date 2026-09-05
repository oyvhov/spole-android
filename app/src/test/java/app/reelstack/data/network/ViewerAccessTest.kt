package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ViewerAccessTest {
    private val seerr = ServiceAccount(ServiceKind.SEERR, "7", "Same name", permissions = 32, mediaUserId = "own")
    private val media = ServiceAccount(ServiceKind.JELLYFIN, "own", "Same name")
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Server", "https://media.example", "test", "wrong-form-id")
    private val payload = """[{"Id":"mine","UserId":"own","UserName":"Same name","NowPlayingItem":{"Id":"1","Name":"Own title"}},{"Id":"other","UserId":"other","UserName":"Same name","NowPlayingItem":{"Id":"2","Name":"Private title"}}]"""
    private class Transport(val respond: (String) -> HttpResponse) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        var writes = 0
        override fun get(url: String, headers: Map<String, String>): HttpResponse { urls += url; return respond(url) }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse { writes++; return HttpResponse(201, "{}") }
    }
    @Test fun normalUserOnlyReceivesOwnSessionEvenWithEqualNames() {
        val transport = Transport { HttpResponse(200, payload) }
        val access = ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr, ServiceKind.JELLYFIN to media))
        assertEquals(listOf("mine"), MediaServerClient(transport).sessions(connection, access).map { it.sessionId })
    }
    @Test fun missingIdentityMakesNoSessionRequest() {
        val transport = Transport { error("Must not fetch other viewers") }
        assertTrue(MediaServerClient(transport).sessions(connection, ViewerAccess(true, emptyMap())).isEmpty())
        assertTrue(transport.urls.isEmpty())
    }
    @Test fun sharedAdminMediaTokenDoesNotElevateOrdinarySeerrUser() {
        val access = ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr, ServiceKind.JELLYFIN to media.copy(id = "admin", isAdmin = true)))
        assertFalse(access.isAdmin)
        assertFalse(access.canSeeAllSessions(ServiceKind.JELLYFIN))
        assertNull(access.ownMediaUser(ServiceKind.JELLYFIN))
    }
    @Test fun verifiedAdminSeesAllSessions() {
        val transport = Transport { HttpResponse(200, payload) }
        val access = ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr.copy(isAdmin = true), ServiceKind.JELLYFIN to media.copy(isAdmin = true)))
        assertEquals(2, MediaServerClient(transport).sessions(connection, access).size)
    }
    @Test fun seerrRequestViewPermissionDoesNotOpenAdminExperience() {
        assertFalse(ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr.copy(permissions = 16384))).isAdmin)
    }
    @Test fun failedSeerrVerificationCannotFallBackToMediaAdmin() {
        val access = ViewerAccess(true, mapOf(ServiceKind.JELLYFIN to media.copy(isAdmin = true)))
        assertFalse(access.isAdmin)
        assertNull(access.ownMediaUser(ServiceKind.JELLYFIN))
    }
    @Test fun mediaAdminRemainsScopedIfSeerrAccountIsOrdinary() {
        val access = ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr, ServiceKind.JELLYFIN to media.copy(isAdmin = true)))
        assertFalse(access.canSeeAllSessions(ServiceKind.JELLYFIN))
        assertEquals("own", access.ownMediaUser(ServiceKind.JELLYFIN))
    }
    @Test fun permissionsDistinguishMovieAndTvAndDefaultDeny() {
        assertTrue(seerr.copy(permissions = 262144).canRequestType("movie"))
        assertFalse(seerr.copy(permissions = 262144).canRequestType("tv"))
        assertTrue(seerr.copy(permissions = 524288).canRequestType("tv"))
        assertFalse(seerr.copy(permissions = 0).canRequestType("movie"))
        assertTrue(seerr.copy(isAdmin = true, permissions = 0).canRequestType("tv"))
        assertFalse(seerr.copy(isAdmin = true, isPersonal = false).canRequestType("tv"))
    }
    @Test fun serverProfileParsesRolesWithoutTrustingFormNameOrId() {
        val transport = Transport { HttpResponse(200, """{"Id":"actual","Name":"Person","Policy":{"IsAdministrator":false}}""") }
        val profile = AccountProfileClient(transport = transport).load(connection)
        assertEquals("actual", profile.id)
        assertFalse(profile.isAdmin)
    }
    @Test fun nonAdminFeedRequestsAndReturnsOnlyOwnRequests() {
        val transport = Transport { url -> HttpResponse(200, if (url.contains("/request?"))
            """{"results":[{"id":1,"requestedBy":{"id":7},"media":{"tmdbId":1,"title":"Own","posterPath":"/a.jpg"}},{"id":2,"requestedBy":{"id":8},"media":{"tmdbId":2,"title":"Other","posterPath":"/b.jpg"}}]}""" else """{"results":[]}""") }
        val feed = SeerrServiceClient(transport).feed(connection.copy(kind = ServiceKind.SEERR), seerr)
        assertEquals(listOf(1), feed.requests.map { it.id })
        assertTrue(transport.urls.any { it.contains("requestedBy=7") })
    }
    @Test fun noPermissionPreventsPost() {
        val transport = Transport { HttpResponse(200, """{"id":7,"permissions":0}""") }
        assertTrue(runCatching { SeerrServiceClient(transport).request(connection.copy(kind = ServiceKind.SEERR, sessionCookie = true), "movie", 1, "7") }.isFailure)
        assertEquals(0, transport.writes)
    }
    @Test fun excludesOnlyNamedChildSeriesLibraries() {
        assertTrue(isExcludedHomeLibrary("Barneserier"))
        assertTrue(isExcludedHomeLibrary(" BARNE-SERIAR "))
        assertTrue(isExcludedHomeLibrary("Barne-TV"))
        assertTrue(isExcludedHomeLibrary("Barne-Tv Serier"))
        assertFalse(isExcludedHomeLibrary("Barnefilmar"))
        assertFalse(isExcludedHomeLibrary("Seriar"))
    }
    @Test fun excludedLibraryNeverGetsRequestedOrReintroducedByFallback() {
        val transport = Transport { url -> when {
            url.contains("Views") -> HttpResponse(200, """{"Items":[{"Id":"kids","Name":"Barneserier","CollectionType":"tvshows"}]}""")
            else -> error("Unexpected request: $url")
        } }
        val access = ViewerAccess(true, mapOf(ServiceKind.SEERR to seerr, ServiceKind.JELLYFIN to media))
        // Session errors are independent of the library selection.
        val feed = MediaServerClient(transport).feed(connection, access)
        assertTrue(feed.recentSeries.isEmpty())
        assertFalse(transport.urls.any { it.contains("Items/Latest") })
    }
    @Test fun ordinaryCalendarDoesNotFetchSharedQueue() {
        val transport = Transport { HttpResponse(200, "[]") }
        QueueServiceClient(transport).feed(connection.copy(kind = ServiceKind.SONARR), includeQueue = false)
        assertFalse(transport.urls.any { it.contains("/queue") })
    }
}
