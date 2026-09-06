package app.reelstack.data.network

import app.reelstack.data.model.*
import app.reelstack.data.repository.MediaSyncRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Collections

class ReleaseCatalogueTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC)
    private val window = ReleaseWindow(clock)
    private fun movie(release: String, type: Int = 4, premiere: String = "2026-07-01") = """
        {"id":1,"title":"Ny film","releaseDate":"$premiere","posterPath":"/poster.jpg",
         "releases":{"results":[{"iso_3166_1":"NO","release_dates":[{"type":$type,"release_date":"$release"}]}]}}
    """.trimIndent()

    @Test fun cinemaDatesNeverBecomeHomeReleases() {
        assertTrue(seerrReleases(movie("2026-09-10", 3), "movie", window).isEmpty())
        assertTrue(seerrReleases("""{"id":1,"title":"Kino","releaseDate":"2026-09-10"}""", "movie", window).isEmpty())
    }
    @Test fun onlyDigitalDatesAreAcceptedAndPosterIsPreserved() {
        assertTrue(seerrReleases(movie("2026-09-10", 5), "movie", window).isEmpty())
        listOf(4).forEach { type ->
            val item = seerrReleases(movie("2026-09-10", type), "movie", window).single()
            assertEquals("2026-09-10", item.dateTime)
            assertEquals("Movie", item.mediaType)
            assertEquals(ServiceKind.SEERR, item.source)
            assertTrue(item.artworkUrl!!.endsWith("/poster.jpg"))
        }
    }
    @Test fun oldFilmsAndDatesOutsideWindowAreExcluded() {
        assertTrue(seerrReleases(movie("2026-09-10", premiere = "1995-01-01"), "movie", window).isEmpty())
        assertTrue(seerrReleases(movie("2026-07-30"), "movie", window).isEmpty())
        assertTrue(seerrReleases(movie("2026-11-01"), "movie", window).isEmpty())
    }
    @Test fun laterRegionalReleaseDoesNotMakeAnOldHomeReleaseNewAgain() {
        val payload = movie("2026-09-10").replace("\"release_dates\":[", "\"release_dates\":[{\"type\":4,\"release_date\":\"2026-07-02\"},")
        assertTrue(seerrReleases(payload, "movie", window).isEmpty())
    }
    @Test fun episodesUseActualEpisodeDatesAndNumbersNotSeriesPremiere() {
        val items = seerrReleases("""{"id":2,"name":"Lang serie","firstAirDate":"2001-01-01","backdropPath":"/wide.jpg",
            "lastEpisodeToAir":{"seasonNumber":7,"episodeNumber":3,"airDate":"2026-09-01","name":"Sist"},
            "nextEpisodeToAir":{"seasonNumber":7,"episodeNumber":4,"airDate":"2026-09-12","name":"Neste"}}
        """, "tv", window)
        assertEquals(2, items.size)
        assertEquals("S07 E04 · Neste", items.last().subtitle)
        assertEquals("2026-09-12", items.last().dateTime)
        assertTrue(items.last().artworkUrl!!.endsWith("/wide.jpg"))
    }
    @Test fun unknownEpisodeDatesAreNotGuessed() {
        assertTrue(seerrReleases("""{"id":2,"name":"Serie","nextEpisodeToAir":{"name":"TBA"}}""", "tv", window).isEmpty())
    }
    @Test fun libraryUsesPremiereDateNotDateAddedAndExcludesPlaceholders() {
        val items = ServicePayloadParser.libraryItems("""{"Items":[
            {"Id":"old","Name":"Gamal","Type":"Movie","PremiereDate":"1990-01-01","DateCreated":"2026-09-06"},
            {"Id":"new","Name":"Ny","Type":"Episode","PremiereDate":"2026-09-01"},
            {"Id":"cinema","Name":"Nett på kino","Type":"Movie","PremiereDate":"2026-09-01"},
            {"Id":"missing","Name":"Manglar","Type":"Episode","PremiereDate":"2026-09-01","IsMissing":true},
            {"Id":"virtual","Name":"Virtuell","Type":"Episode","PremiereDate":"2026-09-01","LocationType":"Virtual"}]}
        """)
        assertEquals(listOf("Ny"), items.mapNotNull { libraryRelease(it, ServiceKind.EMBY, window) }.map { it.title })
    }

    private fun candidates() = listOf(LibraryReleaseCandidate(RemoteLibraryItem("local", "Ny film", "2026", null,
        "Movie", "local", premiereDate = "2026-07-01", tmdbId = 1), ServiceKind.EMBY))

    @Test fun personalSeerrSessionLoadsOnlyLibraryReleasesAndCacheIsAccountScoped() = runBlocking {
        val transport = FixtureTransport()
        val client = SeerrReleaseClient(transport, clock)
        val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "connect.sid=personal", userId = "7", sessionCookie = true)
        assertTrue(client.feed(connection, emptyList()).recent.isEmpty())
        assertTrue(transport.urls.isEmpty())
        val first = client.feed(connection, candidates())
        assertTrue(first.recent.isNotEmpty())
        assertEquals(ServiceKind.EMBY, first.recent.single().source)
        val count = transport.urls.size
        assertEquals(first, client.feed(connection, candidates()))
        assertEquals(count, transport.urls.size)
        client.feed(connection.copy(userId = "8", token = "connect.sid=other"), candidates())
        assertTrue(transport.urls.size > count)
        assertTrue(transport.urls.none { "/queue" in it || "/settings" in it || "/service/" in it })
        assertTrue(transport.sentHeaders.all { "X-Api-Key" !in it && "Cookie" in it })
    }

    @Test fun seerrFailureIsNotCachedAsAnEmptySuccess() = runBlocking {
        val transport = FixtureTransport()
        transport.fail = true
        val client = SeerrReleaseClient(transport, clock)
        val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "connect.sid=test", sessionCookie = true)
        assertTrue(client.feed(connection, candidates()).incomplete)
        transport.fail = false
        assertTrue(client.feed(connection, candidates()).recent.isNotEmpty())
    }

    @Test fun ordinaryJellyfinAndEmbyUsersGetRecentReleasesWithoutArrCredentials() = runBlocking {
        for (kind in listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) {
            val transport = FixtureTransport(useToday = true)
            val repo = MediaSyncRepository(
                mediaServerClient = MediaServerClient(transport), queueServiceClient = QueueServiceClient(transport),
                seerrServiceClient = SeerrServiceClient(transport),
                recommendationsClient = RecommendationsClient(transport, "https://feed.example/items"),
                accountProfileClient = AccountProfileClient(transport = transport),
                seerrReleaseClient = SeerrReleaseClient(transport),
            )
            val state = repo.refresh(listOf(
                ServiceConnection(kind, "Media", "https://media.example", "personal", userId = "media-user"),
                ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "connect.sid=personal", userId = "7", sessionCookie = true),
            ))
            assertFalse(state.adminView)
            assertTrue("recent $kind", state.recentReleases.any { it.source == kind })
            assertTrue("No fabricated Seerr calendar", state.upcoming.isEmpty())
            assertTrue(state.sessions.isEmpty())
            assertTrue(state.incoming.isEmpty())
            assertTrue(state.activity.isEmpty())
            assertTrue(transport.urls.none { "ParentId=kids" in it || "/queue" in it || "/api/v3/" in it })
            assertTrue(transport.urls.any { "SortBy=PremiereDate" in it && "ParentId=allowed" in it })
            assertNull(state.upcomingError)
        }
    }

    private inner class FixtureTransport(private val useToday: Boolean = false) : JsonHttpTransport {
        val urls = Collections.synchronizedList(mutableListOf<String>())
        val sentHeaders = Collections.synchronizedList(mutableListOf<Map<String, String>>())
        var fail = false
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url; sentHeaders += headers
            if (fail) return HttpResponse(503, "{}")
            val today = if (useToday) LocalDate.now() else window.today
            return HttpResponse(200, when {
                "auth/me" in url -> """{"id":7,"displayName":"Person","jellyfinUserId":"media-user","permissions":32}"""
                url.endsWith("Users/Me") || url.endsWith("Users/media-user") -> """{"Id":"media-user","Name":"Person","Policy":{"IsAdministrator":false}}"""
                "/Views" in url || "/UserViews" in url -> """{"Items":[{"Id":"kids","Name":"Barneserier","CollectionType":"tvshows"},{"Id":"allowed","Name":"Media","CollectionType":"mixed"}]}"""
                "SortBy=PremiereDate" in url && "IncludeItemTypes=Movie" in url -> """{"Items":[{"Id":"new","Name":"Ny lokal film","Type":"Movie","PremiereDate":"${today.minusDays(30)}","ProviderIds":{"Tmdb":"1"}}]}"""
                "discover/movies?" in url -> """{"results":[{"id":1,"title":"Ny film","mediaType":"movie"}]}"""
                "/movie/1" in url -> movie(today.minusDays(2).toString(), premiere = today.minusDays(30).toString())
                "/request?" in url -> """{"results":[]}"""
                else -> """{"Items":[],"results":[],"items":[]}"""
            })
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Read-only test")
    }
}
