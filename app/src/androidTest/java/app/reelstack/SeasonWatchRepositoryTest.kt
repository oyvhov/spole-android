package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.*
import org.junit.Assert.*
import org.junit.Test

/** Isolated AVD only: fixture transport and synthetic accounts, never production sessions. */
class SeasonWatchRepositoryTest {
    private class Transport : JsonHttpTransport {
        val reads = mutableListOf<String>()
        var writes = 0
        var user = 7
        var status = 4
        var remoteRequests = """{"results":[]}"""
        var beforeDetail: () -> Unit = {}
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            reads += url
            assertTrue(url.startsWith("https://seerr.example/"))
            assertTrue(headers.values.contains("fixture-session"))
            return when {
                url.endsWith("auth/me") -> HttpResponse(200, """{"id":$user,"displayName":"Testperson","permissions":0}""")
                url.contains("/tv/42?") -> {
                    beforeDetail()
                    HttpResponse(200, """{"name":"Testserie","mediaInfo":{"status":4,"seasons":[{"seasonNumber":2,"status":$status}]},"seasons":[{"seasonNumber":2,"episodeCount":8}]}""")
                }
                url.contains("/request?") -> HttpResponse(200, remoteRequests)
                else -> error("Unexpected endpoint: $url")
            }
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            writes++
            error("A local watch must never POST")
        }
    }

    private fun exercise(block: (RequestTrackingRepository, Transport, ServiceConnection, String, DiscoverMedia) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connections = ConnectionRepository(context)
        ServiceKind.entries.forEach(connections::delete)
        connections.save(ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "fixture-session", "7", sessionCookie = true))
        val connection = connections.get(ServiceKind.SEERR)
        val transport = Transport()
        val repository = RequestTrackingRepository(context, SeerrServiceClient(transport), AccountProfileClient(transport = transport))
        val scope = repository.scope(connection, "7")
        context.getSharedPreferences("request_follows", 0).edit().remove(scope).commit()
        try {
            assertTrue(connections.get(ServiceKind.SONARR).token.isEmpty())
            block(repository, transport, connection, scope, DiscoverMedia("series", "Testserie", "Serie", 0, false, remoteId = 42, mediaType = "tv"))
            assertEquals(0, transport.writes)
            assertFalse(transport.reads.any { it.contains("sonarr", ignoreCase = true) })
        } finally {
            context.getSharedPreferences("request_follows", 0).edit().remove(scope).commit()
            ServiceKind.entries.forEach(connections::delete)
        }
    }

    @Test fun ordinarySeerrUserCanWatchWithoutRequestPermissionOrSonarrLogin() = exercise { repo, _, connection, scope, media ->
        repo.setSeasonWatch(connection, "7", media, 2, true)
        val watch = repo.list(scope).single()
        assertTrue(watch.availabilityOnly)
        assertTrue(watch.readyNotificationOnly)
        assertNull(watch.requestId)
        assertEquals(RequestStage.WATCHING, watch.stage)
        assertEquals(setOf(2), repo.watchedSeasons(connection, "7", 42))
        assertTrue(RequestTrackingRepository(InstrumentationRegistry.getInstrumentation().targetContext).list(scope).single().availabilityOnly)
        repo.refresh(connection)
        assertNull(repo.list(scope).single().requestId)
        repo.cancel(connection, "7", watch.key)
        assertTrue(repo.list(scope).isEmpty())
    }

    @Test fun duplicateTapReusesOneLocalWatchAndTurningOffRemovesOnlyIt() = exercise { repo, _, connection, scope, media ->
        repeat(2) { repo.setSeasonWatch(connection, "7", media, 2, true) }
        assertEquals(1, repo.list(scope).size)
        repo.setSeasonWatch(connection, "7", media, 2, false)
        assertTrue(repo.list(scope).isEmpty())
    }

    @Test fun availableOrMissingSeasonsAreNotSilentlySubscribed() = exercise { repo, transport, connection, scope, media ->
        for (status in listOf(1, 5, 6, 7)) {
            transport.status = status
            assertTrue(runCatching { repo.setSeasonWatch(connection, "7", media, 2, true) }.isFailure)
        }
        assertTrue(repo.list(scope).isEmpty())
    }

    @Test fun changedIdentityOrSignedOutSessionCannotCreateWatch() = exercise { repo, transport, connection, scope, media ->
        transport.user = 8
        assertTrue(runCatching { repo.setSeasonWatch(connection, "7", media, 2, true) }.isFailure)
        transport.user = 7
        transport.beforeDetail = { ConnectionRepository(InstrumentationRegistry.getInstrumentation().targetContext).delete(ServiceKind.SEERR) }
        assertTrue(runCatching { repo.setSeasonWatch(connection, "7", media, 2, true) }.isFailure)
        assertTrue(repo.list(scope).isEmpty())
    }

    @Test fun existingSingleSeasonRequestKeepsIdentityAndDoesNotGetDuplicateWatch() = exercise { repo, _, connection, scope, media ->
        val request = TrackedRequest("tv:42:2", 42, "tv", "Testserie", null, setOf(2), notify = false, requestId = 91)
        repo.put(scope, request)
        repo.setSeasonWatch(connection, "7", media, 2, true)
        assertEquals(request.copy(notify = true, readyNotificationOnly = true), repo.list(scope).single())
        repo.setSeasonWatch(connection, "7", media, 2, false)
        assertEquals(request.copy(readyNotificationOnly = true), repo.list(scope).single())
    }

    @Test fun laterPersonalRequestMergesExactWatchButNotOtherUsersRequest() = exercise { repo, transport, connection, scope, media ->
        repo.setSeasonWatch(connection, "7", media, 2, true)
        transport.remoteRequests = """{"results":[{"id":91,"status":2,"requestedBy":{"id":8},"seasons":[{"seasonNumber":2}],"media":{"tmdbId":42,"mediaType":"tv","status":4}}]}"""
        repo.refresh(connection)
        assertTrue(repo.list(scope).single().availabilityOnly)
        transport.remoteRequests = transport.remoteRequests.replace("\"id\":8", "\"id\":7")
        repo.refresh(connection)
        val merged = repo.list(scope).single()
        assertFalse(merged.availabilityOnly)
        assertTrue(merged.notify)
        assertTrue(merged.readyNotificationOnly)
        assertEquals(91, merged.requestId)
    }

    @Test fun standardAvailabilityNeverCompletesFourKWatch() = exercise { repo, _, connection, scope, _ ->
        repo.put(scope, TrackedRequest("watch-4k", 42, "tv", "Testserie", null, setOf(2), notify = false, is4k = true, availabilityOnly = true))
        repo.refresh(connection)
        assertNotEquals(RequestStage.AVAILABLE, repo.list(scope).single().stage)
    }

    @Test fun turningOffWatchDoesNotNeedNetworkOrAlterGroupedRequest() = exercise { repo, transport, connection, scope, media ->
        repo.setSeasonWatch(connection, "7", media, 2, true)
        val grouped = TrackedRequest("tv:42:1,2", 42, "tv", "Testserie", null, setOf(1, 2), notify = true, requestId = 92)
        repo.put(scope, grouped)
        transport.reads.clear()
        repo.setSeasonWatch(connection, "7", media, 2, false)
        assertTrue(transport.reads.isEmpty())
        assertEquals(grouped, repo.list(scope).single())
    }
}
