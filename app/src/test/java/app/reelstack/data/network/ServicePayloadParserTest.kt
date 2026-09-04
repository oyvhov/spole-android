package app.reelstack.data.network

import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServicePayloadParserTest {
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
        assertEquals(0.5f, item.progress ?: 0f, 0.001f)
    }

    @Test
    fun readsCurrentMediaServerUserId() {
        assertEquals("user-9", ServicePayloadParser.currentUserId("""{"Id":"user-9"}"""))
    }

    @Test
    fun readsAvailableMediaProfilesAndSkipsDisabledUsers() {
        val users = ServicePayloadParser.availableUserIds(
            """[
                {"Id":"active-user","Policy":{"IsDisabled":false}},
                {"Id":"disabled-user","Policy":{"IsDisabled":true}}
            ]""",
        )

        assertEquals(listOf("active-user"), users)
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
        assertEquals("Downloading 75%", item.status)
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
        assertEquals("Movie · 2026", item.subtitle)
        assertEquals("2026-09-08T00:00:00Z", item.dateTime)
        assertEquals("https://art.example/odyssey.jpg", item.artworkUrl)
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
        assertEquals("Movie · 2026", items[0].metadata)
        assertEquals("https://image.tmdb.org/t/p/w500/horizon.jpg", items[0].artworkUrl)
        assertTrue(items[1].requested)
        assertEquals("Series · 2025", items[1].metadata)
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
            """{"title":"The Odyssey","posterPath":"/odyssey.jpg"}""",
        )

        assertEquals("The Odyssey", details.title)
        assertEquals("https://image.tmdb.org/t/p/w500/odyssey.jpg", details.artworkUrl)
    }
}
