package app.reelstack.data.network

import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ServicePayloadParserTest {
    @Test
    fun preservesExactSeerrStatusInListsAndDetails() {
        (1..7).forEach { status ->
            val item = ServicePayloadParser.discover("""{"results":[{"id":12,"title":"Film","mediaType":"movie","mediaInfo":{"status":$status}}]}""").single()
            assertEquals(status, item.seerrStatus)
            val details = ServicePayloadParser.mediaDetails("""{"id":12,"title":"Film","mediaInfo":{"status":$status}}""")
            assertEquals(status, details.seerrStatus)
        }
        assertEquals(null, ServicePayloadParser.mediaDetails("""{"title":"Film"}""").seerrStatus)
    }

    @Test
    fun parsesGithubRecommendationsWithoutTurningThemIntoDiscoverResults() {
        val items = ServicePayloadParser.recommendations("""
            {"version":1,"items":[
              {"tmdbId":95396,"mediaType":"tv","title":"Severance","year":2022,
               "posterPath":"/poster.jpg","genres":["Thriller"]},
              {"tmdbId":9,"mediaType":"person","title":"Not a title"}
            ]}
        """.trimIndent())

        assertEquals(1, items.size)
        assertEquals("github-recommendation-tv-95396", items.single().id)
        assertEquals("Serie · 2022", items.single().metadata)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", items.single().artworkUrl)
    }

    @Test
    fun parsesJellyfinPlaybackSession() {
        val payload = """
            [{
              "Id": "session-1",
              "UserId": "user-9",
              "UserName": "Maya",
              "DeviceName": "Living room TV",
              "PlayState": {"PositionTicks": 18000000000, "IsPaused": false, "PlayMethod": "DirectPlay"},
              "NowPlayingItem": {
                "Id": "episode-4",
                "SeriesId": "series-1",
                "Name": "Woe's Hollow",
                "SeriesName": "Severance",
                "ParentIndexNumber": 2,
                "IndexNumber": 4,
                "RunTimeTicks": 36000000000,
                "Width": 3840
              }
            }]
        """.trimIndent()

        val session = ServicePayloadParser.playbackSessions(payload).single()

        assertEquals("Severance", session.title)
        assertEquals("session-1", session.sessionId)
        assertEquals("user-9", session.userId)
        assertEquals("S02 E04 · Woe's Hollow", session.subtitle)
        assertEquals("series-1", session.artworkItemId)
        assertEquals(0.5f, session.progress, 0.001f)
        assertEquals("4K", session.quality)
        assertFalse(session.paused)
    }

    @Test
    fun parsesContinueWatchingProgressAndEpisodeLabel() {
        val payload = """
            {"Items":[{
              "Id":"episode-4","Name":"Woe's Hollow","SeriesName":"Severance",
              "ParentIndexNumber":2,"IndexNumber":4,"RunTimeTicks":36000000000,
              "UserData":{"PlaybackPositionTicks":18000000000}
            }]}
        """.trimIndent()

        val item = ServicePayloadParser.libraryItems(payload).single()

        assertEquals("episode-4", item.id)
        assertEquals("Severance", item.title)
        assertEquals("S02 E04 · Woe's Hollow", item.subtitle)
        assertEquals("episode-4", item.artworkItemId)
        assertEquals(0.5f, item.progress ?: 0f, 0.001f)
    }

    @Test
    fun prefersWideThumbForRecentEpisode() {
        val payload = """
            {"Items":[{
              "Id":"episode-8","Name":"The Signal","SeriesName":"Foundation","Type":"Episode",
              "ParentIndexNumber":3,"IndexNumber":8,"ImageTags":{"Primary":"p","Thumb":"t"}
            }]}
        """.trimIndent()

        val item = ServicePayloadParser.libraryItems(payload).single()

        assertEquals("Foundation", item.title)
        assertEquals("S03 E08 · The Signal", item.subtitle)
        assertEquals("episode-8", item.artworkItemId)
        assertEquals("Thumb", item.artworkImageType)
    }

    @Test
    fun readsCurrentMediaServerUserId() {
        assertEquals("user-9", ServicePayloadParser.currentUserId("""{"Id":"user-9"}"""))
    }

    @Test
    fun prefersAdministratorMediaProfileAndSkipsDisabledUsers() {
        val users = ServicePayloadParser.availableUserIds(
            """[
                {"Id":"child-user","Policy":{"IsDisabled":false,"EnableAllFolders":false}},
                {"Id":"full-user","Policy":{"IsDisabled":false,"EnableAllFolders":true}},
                {"Id":"admin-user","Policy":{"IsDisabled":false,"IsAdministrator":true}},
                {"Id":"disabled-user","Policy":{"IsDisabled":true}}
            ]""",
        )

        assertEquals(listOf("admin-user", "full-user", "child-user"), users)
    }

    @Test
    fun readsMovieAndSeriesLibraryViews() {
        val views = ServicePayloadParser.libraryViews(
            """{"Items":[
                {"Id":"movies-main","Name":"Filmar","CollectionType":"movies","IsFolder":true},
                {"Id":"series-main","Name":"Seriar","CollectionType":"tvshows","IsFolder":true},
                {"Id":"not-a-view","Name":"A film","Type":"Movie","IsFolder":false}
            ]}""",
        )

        assertEquals(listOf("movies-main", "series-main"), views.map { it.id })
        assertEquals(listOf("movies", "tvshows"), views.map { it.collectionType })
    }

    @Test
    fun ignoresMediaServerSessionsWithoutPlayback() {
        val sessions = ServicePayloadParser.playbackSessions("""[{"Id":"idle-session","UserName":"Maya"}]""")

        assertTrue(sessions.isEmpty())
    }

    @Test
    fun parsesRadarrQueueProgressAndSecureArtwork() {
        val payload = """
            {"records":[{
              "id": 7,
              "downloadId": "download-7",
              "status": "downloading",
              "size": 1000,
              "sizeleft": 250,
              "movie": {
                "title": "Dune: Messiah",
                "images": [{"coverType":"poster","remoteUrl":"https://art.example/poster.jpg"}]
              }
            }]}
        """.trimIndent()

        val item = ServicePayloadParser.queue(payload, ServiceKind.RADARR).single()

        assertEquals("Dune: Messiah", item.title)
        assertEquals("Lastar ned 75 %", item.status)
        assertEquals(75, item.progress)
        assertEquals(IncomingState.DOWNLOADING, item.state)
        assertEquals("https://art.example/poster.jpg", item.artworkUrl)
    }

    @Test
    fun rejectsCleartextRemoteArtworkFromQueuePayload() {
        val payload = """
            {"records":[{"id":1,"title":"Unsafe art","status":"queued","size":0,
              "movie":{"title":"Unsafe art","images":[{"coverType":"poster","remoteUrl":"http://public.example/poster.jpg"}]}}
            ]}
        """.trimIndent()

        val item = ServicePayloadParser.queue(payload, ServiceKind.RADARR).single()

        assertEquals(null, item.artworkUrl)
    }

    @Test
    fun parsesRadarrCalendarMovie() {
        val payload = """
            [{"id":17,"title":"The Odyssey","year":2026,"digitalRelease":"2026-09-08T00:00:00Z",
              "images":[{"coverType":"poster","remoteUrl":"https://art.example/odyssey.jpg"}]}]
        """.trimIndent()

        val item = ServicePayloadParser.upcoming(payload, ServiceKind.RADARR).single()

        assertEquals("The Odyssey", item.title)
        assertEquals("Film · 2026", item.subtitle)
        assertEquals("2026-09-08T00:00:00Z", item.dateTime)
        assertEquals("https://art.example/odyssey.jpg", item.artworkUrl)
    }

    @Test
    fun radarrCalendarExcludesCinemaOnlyMovies() {
        val payload = """
            [
              {"id":17,"title":"Digital film","digitalRelease":"2026-09-08T00:00:00Z"},
              {"id":18,"title":"Cinema film","inCinemas":"2026-09-09T00:00:00Z"}
            ]
        """.trimIndent()

        val items = ServicePayloadParser.upcoming(payload, ServiceKind.RADARR)

        assertEquals(listOf("Digital film"), items.map { it.title })
        assertTrue(items.single().facts.contains("Digital utgjeving"))
    }

    @Test
    fun radarrCalendarUsesPhysicalReleaseWhenDigitalDateHasPassed() {
        val payload = """
            [{"id":19,"title":"Home release","digitalRelease":"2026-08-01T00:00:00Z",
              "physicalRelease":"2026-09-12T00:00:00Z"}]
        """.trimIndent()

        val item = ServicePayloadParser.upcoming(
            payload,
            ServiceKind.RADARR,
            notBefore = Instant.parse("2026-09-03T00:00:00Z"),
        ).single()

        assertEquals("2026-09-12T00:00:00Z", item.dateTime)
        assertTrue(item.facts.contains("Fysisk utgjeving"))
    }

    @Test
    fun parsesSonarrCalendarEpisode() {
        val payload = """
            [{"id":31,"seasonNumber":2,"episodeNumber":7,"title":"Messenger","airDateUtc":"2026-09-09T19:00:00Z",
              "series":{"title":"Andor","images":[{"coverType":"poster","remoteUrl":"https://art.example/andor.jpg"}]}}]
        """.trimIndent()

        val item = ServicePayloadParser.upcoming(payload, ServiceKind.SONARR).single()

        assertEquals("Andor", item.title)
        assertEquals("S02 E07 · Messenger", item.subtitle)
        assertEquals(ServiceKind.SONARR, item.source)
    }

    @Test
    fun mapsSeerrAvailabilityAndPosterUrl() {
        val payload = """
            {"results":[
              {"id":101,"mediaType":"movie","title":"The Horizon","releaseDate":"2026-04-18","posterPath":"/horizon.jpg","mediaInfo":{"status":5}},
              {"id":202,"mediaType":"tv","name":"Signal","firstAirDate":"2025-01-02","posterPath":"/signal.jpg","mediaInfo":{"status":2}}
            ]}
        """.trimIndent()

        val items = ServicePayloadParser.discover(payload)

        assertTrue(items[0].inLibrary)
        assertEquals("Film · 2026", items[0].metadata)
        assertEquals("https://image.tmdb.org/t/p/w500/horizon.jpg", items[0].artworkUrl)
        assertTrue(items[1].requested)
        assertEquals("Serie · 2025", items[1].metadata)
    }

    @Test
    fun ignoresPeopleInMixedSeerrTrendingResults() {
        val payload = """{"results":[{"id":9,"mediaType":"person","name":"An Actor","profilePath":"/actor.jpg"}]}"""

        assertTrue(ServicePayloadParser.discover(payload).isEmpty())
    }

    @Test
    fun parsesSeerrRequestActorAndStatus() {
        val payload = """
            {"results":[{"id":44,"status":2,"createdAt":"2026-09-03T20:30:00Z",
              "media":{"tmdbId":101,"mediaType":"movie"},
              "requestedBy":{"displayName":"Maya"}}]}
        """.trimIndent()

        val request = ServicePayloadParser.requests(payload).single()

        assertEquals(44, request.id)
        assertEquals(101, request.remoteId)
        assertEquals(2, request.status)
        assertEquals("Maya", request.requestedBy)
    }

    @Test
    fun parsesSeerrMediaDetailsArtwork() {
        val details = ServicePayloadParser.mediaDetails(
            """{"title":"The Odyssey","posterPath":"/odyssey.jpg","overview":"Ei lang reise.","releaseDate":"2026-07-17","runtime":149,"voteAverage":8.4,"genres":[{"name":"Eventyr"}]}""",
        )

        assertEquals("The Odyssey", details.title)
        assertEquals("https://image.tmdb.org/t/p/w500/odyssey.jpg", details.artworkUrl)
        assertEquals("Ei lang reise.", details.overview)
        assertEquals(listOf("Film", "2026", "149 min", "★ 8.4"), details.facts)
        assertEquals(listOf("Eventyr"), details.genres)
    }
}
