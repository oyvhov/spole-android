package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceClientsTest {
    @Test
    fun seerrConnectionProbeUsesAuthenticatedEndpoint() {
        val transport = RecordingTransport(getResponses = mutableListOf(HttpResponse(401, "{}")))

        val result = ServiceConnectionTester(transport).test(connection(ServiceKind.SEERR, "bad-key"))

        assertFalse(result.success)
        assertEquals("API-nøkkelen vart avvist", result.message)
        assertTrue(transport.lastUrl.contains("/api/v1/request?take=1&skip=0"))
    }

    @Test
    fun mediaServerTokenStaysInHeader() {
        val transport = RecordingTransport(getResponses = mutableListOf(HttpResponse(200, "[]")))
        val connection = connection(ServiceKind.JELLYFIN, "very-secret-token")

        MediaServerClient(transport).sessions(connection)

        assertEquals("very-secret-token", transport.lastHeaders["X-Emby-Token"])
        assertFalse(transport.lastUrl.contains("very-secret-token"))
        assertTrue(transport.lastUrl.endsWith("/Sessions"))
    }

    @Test
    fun jellyfinFeedLoadsSeparateMovieAndSeriesRows() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )
        val connection = connection(ServiceKind.JELLYFIN, "secret").copy(userId = "user 9")

        val feed = MediaServerClient(transport).feed(connection)

        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertEquals(
            "https://media.example.com/Items/movie-1/Images/Primary?maxHeight=720&quality=90",
            feed.recentMovies.single().artworkUrl,
        )
        assertFalse(feed.recentMovies.single().artworkUrl.orEmpty().contains("secret"))
        assertTrue(transport.urls[1].contains("Items/Latest?UserId=user%209"))
        assertTrue(transport.urls[1].contains("IncludeItemTypes=Movie"))
        assertTrue(transport.urls[2].contains("IncludeItemTypes=Episode"))
        assertTrue(transport.urls[2].contains("GroupItems=true"))
        assertTrue(transport.headers.all { it["X-Emby-Token"] == "secret" })
    }

    @Test
    fun embyFeedUsesUserScopedMovieAndSeriesRoutes() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )
        val connection = connection(ServiceKind.EMBY, "secret").copy(userId = "emby-user")

        val feed = MediaServerClient(transport).feed(connection)

        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertEquals(
            "https://media.example.com/Items/movie-1/Images/Primary?maxHeight=720&quality=90",
            feed.recentMovies.single().artworkUrl,
        )
        assertTrue(transport.urls[1].contains("Users/emby-user/Items/Latest?"))
        assertTrue(transport.urls[1].contains("IncludeItemTypes=Movie"))
        assertTrue(transport.urls[2].contains("IncludeItemTypes=Episode"))
    }

    @Test
    fun embyFeedDetectsFirstAvailableProfileForApiKey() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(401, "{}"),
                HttpResponse(200, """[{"Id":"detected-user","Policy":{"IsDisabled":false}}]"""),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )

        val feed = MediaServerClient(transport).feed(connection(ServiceKind.EMBY, "server-api-key"))

        assertEquals("Foundation", feed.recentSeries.single().title)
        assertTrue(transport.urls[1].endsWith("/Users/Me"))
        assertTrue(transport.urls[2].endsWith("/Users"))
        assertTrue(transport.urls[3].contains("Users/detected-user/Items/Latest"))
    }

    @Test
    fun jellyfinFeedFallsBackToLegacyUserRoutes() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(404, "{}"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(404, "{}"),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )
        val connection = connection(ServiceKind.JELLYFIN, "secret").copy(userId = "legacy-user")

        val feed = MediaServerClient(transport).feed(connection)

        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertTrue(transport.urls[2].contains("Users/legacy-user/Items/Latest?"))
        assertTrue(transport.urls[4].contains("Users/legacy-user/Items/Latest?"))
    }

    @Test
    fun jellyfinLoadsLatestRowsWithoutAProfileId() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )

        val feed = MediaServerClient(transport).feed(connection(ServiceKind.JELLYFIN, "server-api-key"))

        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertTrue(feed.warning == null)
        assertTrue(transport.urls[1].contains("Items/Latest?Limit=12"))
        assertTrue(transport.urls.none { it.endsWith("/Users/Me") || it.endsWith("/Users") })
    }

    @Test
    fun mediaLibraryStillLoadsWhenPlaybackSessionsAreForbidden() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(403, "{}"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )
        val connection = connection(ServiceKind.EMBY, "personal-token").copy(userId = "emby-user")

        val feed = MediaServerClient(transport).feed(connection)

        assertTrue(feed.sessions.isEmpty())
        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertTrue(feed.warning.orEmpty().contains("Avspelingsøkter er utilgjengelege"))
    }

    @Test
    fun validServerRemainsConnectedWhenPersonalFeedsAreForbidden() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(403, "{}"),
                HttpResponse(403, "{}"),
                HttpResponse(403, "{}"),
                HttpResponse(200, """{"Version":"10.11.0"}"""),
            ),
        )
        val connection = connection(ServiceKind.EMBY, "limited-api-key").copy(userId = "limited-user")

        val feed = MediaServerClient(transport).feed(connection)

        assertTrue(feed.sessions.isEmpty())
        assertTrue(feed.recentMovies.isEmpty())
        assertTrue(feed.recentSeries.isEmpty())
        assertTrue(feed.warning.orEmpty().contains("Mediedelane er utilgjengelege"))
        assertTrue(transport.urls.last().endsWith("/System/Info"))
    }

    @Test
    fun playbackPauseUsesAuthenticatedSessionCommand() {
        val transport = RecordingTransport(postResponse = HttpResponse(204, ""))
        val connection = connection(ServiceKind.EMBY, "emby-secret")

        MediaServerClient(transport).setPaused(connection, sessionId = "session 1", paused = true)

        assertTrue(transport.lastUrl.endsWith("/Sessions/session+1/Playing/Pause"))
        assertEquals("emby-secret", transport.lastHeaders["X-Emby-Token"])
        assertEquals("{}", transport.lastBody)
    }

    @Test
    fun radarrTokenStaysInHeader() {
        val transport = RecordingTransport(getResponses = mutableListOf(HttpResponse(200, "{\"records\":[]}")))
        val connection = connection(ServiceKind.RADARR, "arr-secret")

        QueueServiceClient(transport).queue(connection)

        assertEquals("arr-secret", transport.lastHeaders["X-Api-Key"])
        assertFalse(transport.lastUrl.contains("arr-secret"))
        assertTrue(transport.lastUrl.contains("/api/v3/queue?"))
    }

    @Test
    fun sonarrFeedLoadsQueueAndUpcomingCalendar() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, """{"records":[]}"""),
                HttpResponse(200, """[{"id":3,"airDateUtc":"2026-09-09T19:00:00Z","series":{"title":"Andor"}}]"""),
            ),
        )
        val connection = connection(ServiceKind.SONARR, "sonarr-secret")

        val feed = QueueServiceClient(transport).feed(connection)

        assertEquals("Andor", feed.upcoming.single().title)
        assertTrue(transport.urls[1].contains("/api/v3/calendar?"))
        assertTrue(transport.urls[1].contains("includeSeries=true"))
        assertTrue(transport.headers.all { it["X-Api-Key"] == "sonarr-secret" })
    }

    @Test
    fun televisionRequestIncludesAllSeasons() {
        val transport = RecordingTransport(postResponse = HttpResponse(201, "{}"))
        val connection = connection(ServiceKind.SEERR, "seerr-secret")

        SeerrServiceClient(transport).request(connection, mediaType = "tv", remoteId = 202)

        assertEquals("seerr-secret", transport.lastHeaders["X-Api-Key"])
        assertEquals("{\"mediaType\":\"tv\",\"mediaId\":202,\"seasons\":\"all\"}", transport.lastBody)
        assertFalse(transport.lastUrl.contains("seerr-secret"))
    }

    @Test
    fun seerrFeedEnrichesRequestWithRealArtwork() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, """{"results":[]}"""),
                HttpResponse(200, """{"results":[{"id":4,"status":2,"media":{"tmdbId":101,"mediaType":"movie"},"requestedBy":{"displayName":"Maya"}}]}"""),
                HttpResponse(200, """{"title":"The Odyssey","posterPath":"/odyssey.jpg"}"""),
            ),
        )

        val feed = SeerrServiceClient(transport).feed(connection(ServiceKind.SEERR, "seerr-secret"))

        assertEquals("The Odyssey", feed.requests.single().title)
        assertEquals("https://image.tmdb.org/t/p/w500/odyssey.jpg", feed.requests.single().artworkUrl)
        assertTrue(transport.lastUrl.endsWith("/api/v1/movie/101"))
    }

    private fun connection(kind: ServiceKind, token: String) = ServiceConnection(
        kind = kind,
        name = kind.displayName,
        baseUrl = "https://media.example.com",
        token = token,
        state = ConnectionState.CONNECTED,
    )

    private class RecordingTransport(
        private val getResponses: MutableList<HttpResponse> = mutableListOf(),
        private val postResponse: HttpResponse = HttpResponse(200, "{}"),
    ) : JsonHttpTransport {
        var lastUrl: String = ""
        var lastHeaders: Map<String, String> = emptyMap()
        var lastBody: String = ""
        val urls = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()

        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            lastUrl = url
            lastHeaders = headers
            urls += url
            this.headers += headers
            return getResponses.removeAt(0)
        }

        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            lastUrl = url
            lastHeaders = headers
            lastBody = jsonBody
            urls += url
            this.headers += headers
            return postResponse
        }
    }
}
