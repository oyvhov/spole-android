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
    fun jellyfinAccountLoginReturnsTokenAndProfileWithoutLeakingPassword() {
        val transport = RecordingTransport(
            postResponse = HttpResponse(
                200,
                """{"User":{"Id":"profile-7"},"AccessToken":"fresh-token"}""",
            ),
        )

        val login = JellyfinAuthenticationClient(transport, deviceId = "android-42")
            .authenticate("https://media.example.com", "ø yvind", "p@ss\"word")

        assertEquals("fresh-token", login.accessToken)
        assertEquals("profile-7", login.userId)
        assertTrue(transport.lastUrl.endsWith("/Users/AuthenticateByName"))
        assertTrue(transport.lastHeaders["Authorization"].orEmpty().contains("DeviceId=\"android-42\""))
        assertFalse(transport.lastHeaders.containsKey("X-Emby-Authorization"))
        assertFalse(transport.lastHeaders.values.any { it.contains("p@ss") })
        assertFalse(transport.lastUrl.contains("p@ss"))
        assertEquals("{\"Username\":\"ø yvind\",\"Pw\":\"p@ss\\\"word\"}", transport.lastBody)
    }

    @Test
    fun jellyfinAccountLoginExplainsRejectedCredentials() {
        val transport = RecordingTransport(postResponse = HttpResponse(401, "{}"))

        val error = runCatching {
            JellyfinAuthenticationClient(transport).authenticate("https://media.example.com", "wrong", "wrong")
        }.exceptionOrNull()

        assertEquals("Feil brukarnamn eller passord", error?.message)
    }

    @Test
    fun jellyfinQuickConnectUsesNativeThreeStepFlow() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(
                    200,
                    """{"Secret":"secret-123","Code":"ABC123","Authenticated":true}""",
                ),
            ),
            postResponses = mutableListOf(
                HttpResponse(
                    200,
                    """{"Secret":"secret-123","Code":"ABC123","Authenticated":false}""",
                ),
                HttpResponse(
                    200,
                    """{"User":{"Id":"profile-7"},"AccessToken":"quick-token"}""",
                ),
            ),
        )
        val client = JellyfinAuthenticationClient(transport, deviceId = "android-42")

        val initiated = client.initiateQuickConnect("https://media.example.com")
        val approved = client.quickConnectState("https://media.example.com", initiated.secret)
        val login = client.authenticateWithQuickConnect("https://media.example.com", approved.secret)

        assertEquals("ABC123", initiated.code)
        assertFalse(initiated.authenticated)
        assertTrue(approved.authenticated)
        assertEquals("quick-token", login.accessToken)
        assertEquals("profile-7", login.userId)
        assertTrue(transport.urls[0].endsWith("/QuickConnect/Initiate"))
        assertTrue(transport.urls[1].endsWith("/QuickConnect/Connect?secret=secret-123"))
        assertTrue(transport.urls[2].endsWith("/Users/AuthenticateWithQuickConnect"))
        assertTrue(transport.headers.all { "Authorization" in it })
        assertTrue(transport.headers.none { "X-Emby-Authorization" in it })
        assertEquals("{\"Secret\":\"secret-123\"}", transport.lastBody)
    }

    @Test
    fun seerrConnectionProbeUsesAuthenticatedEndpoint() {
        val transport = RecordingTransport(getResponses = mutableListOf(HttpResponse(401, "{}")))

        val result = ServiceConnectionTester(transport).test(connection(ServiceKind.SEERR, "bad-key"))

        assertFalse(result.success)
        assertEquals("API-nøkkelen vart avvist", result.message)
        assertTrue(transport.lastUrl.contains("/api/v1/request?take=1&skip=0"))
    }

    @Test
    fun jellyfinTokenUsesModernAuthorizationHeader() {
        val transport = RecordingTransport(getResponses = mutableListOf(HttpResponse(200, "[]")))
        val connection = connection(ServiceKind.JELLYFIN, "very-secret-token")

        MediaServerClient(transport, deviceId = "android-42").sessions(connection)

        assertTrue(
            transport.lastHeaders["Authorization"].orEmpty()
                .contains("Token=\"very-secret-token\""),
        )
        assertTrue(
            transport.lastHeaders["Authorization"].orEmpty()
                .contains("DeviceId=\"android-42\""),
        )
        assertFalse(transport.lastHeaders.containsKey("X-Emby-Token"))
        assertFalse(transport.lastUrl.contains("very-secret-token"))
        assertTrue(transport.lastUrl.endsWith("/Sessions"))
    }

    @Test
    fun jellyfinConnectionProbeUsesModernAuthorizationHeader() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(HttpResponse(200, "{\"Version\":\"10.11.11\"}")),
        )

        val result = ServiceConnectionTester(transport, deviceId = "android-42")
            .test(connection(ServiceKind.JELLYFIN, "fresh-token"))

        assertTrue(result.success)
        assertTrue(transport.lastHeaders["Authorization"].orEmpty().contains("Token=\"fresh-token\""))
        assertFalse(transport.lastHeaders.containsKey("X-Emby-Token"))
    }

    @Test
    fun jellyfinFeedLoadsSeparateMovieAndSeriesRows() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, """[{
                    "Id":"child-session","UserId":"child-user","UserName":"Barn","DeviceName":"TV",
                    "NowPlayingItem":{"Id":"episode-bluey","Name":"Bluey","SeriesName":"Bluey"},
                    "PlayState":{}
                }]"""),
                HttpResponse(200, """{"Items":[]}"""),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"episode-1","Name":"The Signal","SeriesName":"Foundation","Type":"Episode","ImageTags":{"Thumb":"wide-tag"}}]"""),
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
        assertTrue(transport.urls[1].contains("UserViews?userId=user%209"))
        assertTrue(transport.urls[2].contains("Items/Latest?UserId=user%209"))
        assertTrue(transport.urls[2].contains("IncludeItemTypes=Movie"))
        assertTrue(transport.urls[3].contains("IncludeItemTypes=Episode"))
        assertTrue(transport.urls[3].contains("GroupItems=false"))
        assertEquals(
            "https://media.example.com/Items/episode-1/Images/Thumb?maxWidth=960&quality=90",
            feed.recentSeries.single().artworkUrl,
        )
        assertTrue(transport.headers.all { it["Authorization"].orEmpty().contains("Token=\"secret\"") })
    }

    @Test
    fun embyFeedUsesUserScopedMovieAndSeriesRoutes() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(200, """{"Items":[]}"""),
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
        assertTrue(transport.urls[1].contains("Users/emby-user/Views?"))
        assertTrue(transport.urls[2].contains("Users/emby-user/Items/Latest?"))
        assertTrue(transport.urls[2].contains("IncludeItemTypes=Movie"))
        assertTrue(transport.urls[3].contains("IncludeItemTypes=Episode"))
    }

    @Test
    fun embyFeedPrefersFullAccessProfileAndBalancesLibraries() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, """[{
                    "Id":"child-session","UserId":"child-user","UserName":"Barn","DeviceName":"TV",
                    "NowPlayingItem":{"Id":"episode-bluey","Name":"Bluey","SeriesName":"Bluey"},
                    "PlayState":{}
                }]"""),
                HttpResponse(401, "{}"),
                HttpResponse(200, """[
                    {"Id":"child-user","Policy":{"IsDisabled":false,"EnableAllFolders":false}},
                    {"Id":"admin-user","Policy":{"IsDisabled":false,"IsAdministrator":true}}
                ]"""),
                HttpResponse(200, """{"Items":[
                    {"Id":"movies-main","Name":"Filmar","CollectionType":"movies","IsFolder":true},
                    {"Id":"movies-kids","Name":"Barnefilmar","CollectionType":"movies","IsFolder":true},
                    {"Id":"series-main","Name":"Seriar","CollectionType":"tvshows","IsFolder":true},
                    {"Id":"series-kids","Name":"Barneseriar","CollectionType":"tvshows","IsFolder":true}
                ]}"""),
                HttpResponse(200, """[
                    {"Id":"movie-main-1","Name":"The Odyssey","Type":"Movie"},
                    {"Id":"movie-main-2","Name":"Mickey 17","Type":"Movie"}
                ]"""),
                HttpResponse(200, """[{"Id":"movie-kids-1","Name":"Curious George","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-main-1","Name":"Foundation","Type":"Series"}]"""),
                HttpResponse(200, """[{"Id":"series-kids-1","Name":"Bluey","Type":"Series"}]"""),
            ),
        )

        val feed = MediaServerClient(transport).feed(connection(ServiceKind.EMBY, "server-api-key"))

        assertEquals(listOf("The Odyssey", "Curious George", "Mickey 17"), feed.recentMovies.map { it.title })
        assertEquals(listOf("Foundation", "Bluey"), feed.recentSeries.map { it.title })
        assertTrue(transport.urls[1].endsWith("/Users/Me"))
        assertTrue(transport.urls[2].endsWith("/Users"))
        assertTrue(transport.urls[3].contains("Users/admin-user/Views"))
        assertTrue(transport.urls[4].contains("Users/admin-user/Items/Latest"))
        assertTrue(transport.urls[4].contains("ParentId=movies-main"))
        assertTrue(transport.urls[5].contains("ParentId=movies-kids"))
    }

    @Test
    fun jellyfinFeedFallsBackToLegacyUserRoutes() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(200, """{"Items":[]}"""),
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
        assertTrue(transport.urls[3].contains("Users/legacy-user/Items/Latest?"))
        assertTrue(transport.urls[5].contains("Users/legacy-user/Items/Latest?"))
    }

    @Test
    fun jellyfinLoadsLatestRowsWithoutAProfileId() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, "[]"),
                HttpResponse(401, "{}"),
                HttpResponse(403, "{}"),
                HttpResponse(200, """[{"Id":"movie-1","Name":"The Odyssey","Type":"Movie"}]"""),
                HttpResponse(200, """[{"Id":"series-1","Name":"Foundation","Type":"Series"}]"""),
            ),
        )

        val feed = MediaServerClient(transport).feed(connection(ServiceKind.JELLYFIN, "server-api-key"))

        assertEquals("The Odyssey", feed.recentMovies.single().title)
        assertEquals("Foundation", feed.recentSeries.single().title)
        assertTrue(feed.warning == null)
        assertTrue(transport.urls[1].endsWith("/Users/Me"))
        assertTrue(transport.urls[2].endsWith("/Users"))
        assertTrue(transport.urls[3].contains("Items/Latest?Limit=12"))
    }

    @Test
    fun mediaLibraryStillLoadsWhenPlaybackSessionsAreForbidden() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(403, "{}"),
                HttpResponse(200, """{"Items":[]}"""),
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

    @Test
    fun seerrSearchUsesRemoteSearchEndpointAndEncodesSpaces() {
        val transport = RecordingTransport(
            getResponses = mutableListOf(
                HttpResponse(200, """{"results":[{"id":101,"mediaType":"movie","title":"Dune"}]}"""),
            ),
        )

        val results = SeerrServiceClient(transport).search(connection(ServiceKind.SEERR, "seerr-secret"), "Dune Part Two")

        assertEquals("Dune", results.single().title)
        assertTrue(transport.lastUrl.contains("/api/v1/search?query=Dune%20Part%20Two"))
        assertEquals("seerr-secret", transport.lastHeaders["X-Api-Key"])
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
        private val postResponses: MutableList<HttpResponse> = mutableListOf(),
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
            return if (postResponses.isNotEmpty()) postResponses.removeAt(0) else postResponse
        }
    }
}
