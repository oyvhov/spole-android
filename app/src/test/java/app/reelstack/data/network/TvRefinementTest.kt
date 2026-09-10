package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class TvRefinementTest {
    @Test fun nextUpUsesOnlySelectedSeriesLibrariesAndTheActiveProfile() {
        val urls = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                urls += url
                return HttpResponse(200, """{"Items":[{"Id":"episode","Name":"Next","SeriesName":"Show","Type":"Episode"}]}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
        }
        val client = MediaServerClient(transport, includeLibrary = { _, view -> view.id != "excluded" })
        val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example", "test-token", userId = "profile")
        val views = listOf(RemoteLibraryView("included", "Series", "tvshows"), RemoteLibraryView("excluded", "Other", "tvshows"),
            RemoteLibraryView("movies", "Films", "movies"), RemoteLibraryView("music", "Music", "music"))
        assertEquals("episode", client.nextUp(connection, "profile", views).single().id)
        assertEquals(1, urls.size)
        assertTrue(urls.single().contains("Shows/NextUp?UserId=profile&ParentId=included"))
        assertTrue(urls.single().contains("EnableResumable=false"))
        assertTrue(client.nextUp(connection, "profile", views.filter { it.id == "excluded" }).isEmpty())
        assertEquals(1, urls.size)
    }
    @Test fun detailsReadProgressRatingAndActualStreamQuality() {
        val detail = ServicePayloadParser.libraryDetails("""{"Name":"Episode","Type":"Episode","RunTimeTicks":6000000000,
            "UserData":{"PlaybackPositionTicks":1500000000},"CommunityRating":8.5,
            "MediaStreams":[{"Type":"Video","Width":3840,"Height":1600,"Codec":"hevc","VideoRangeType":"HDR10"},
                {"Type":"Audio","Codec":"eac3","Channels":6}]}""")
        assertEquals(.25f, detail.progress!!, .001f)
        assertEquals(8, detail.remainingMinutes)
        assertEquals(listOf("4K", "HDR10", "HEVC", "EAC3 5.1"), detail.quality)
        assertTrue(detail.facts.any { it.startsWith("★") })
    }
    @Test fun missingStreamsDoNotInventQualityOrProgress() {
        val detail = ServicePayloadParser.libraryDetails("""{"Name":"Test","Type":"Movie"}""")
        assertTrue(detail.quality.isEmpty())
        assertNull(detail.progress)
        assertNull(detail.remainingMinutes)
    }
    @Test fun mediaSourceStreamsAndPercentageAreSupportedAndClamped() {
        val detail = ServicePayloadParser.libraryDetails("""{"UserData":{"PlayedPercentage":140},"MediaSources":[{"MediaStreams":[
            {"Type":"Video","Width":1920,"Height":800,"Codec":"h264"}]}]}""")
        assertEquals(1f, detail.progress!!, .001f)
        assertEquals(listOf("1080p", "H264"), detail.quality)
    }
    @Test fun menuSanitizesDuplicatesUnknownItemsAndKeepsSettingsReachable() {
        val preferences = Personalization(menuOrder = listOf("ACTIVITY", "ACTIVITY", "UNKNOWN", "HOME"),
            hiddenMenuItems = setOf("HOME", "SETTINGS", "DISCOVER"))
        assertEquals(listOf("ACTIVITY", "HOME", "LIBRARY", "SETTINGS"), preferences.visibleMenu())
    }
    @Test fun unresolvedImportsAreDistinguishedFromActualTitles() {
        val item = TrackedRequest("tv:1", 1, "tv", "Serie", null, emptySet())
        assertFalse(item.hasTitleMetadata)
        assertTrue(item.copy(title = "Taskmaster").hasTitleMetadata)
        assertTrue(item.copy(artworkUrl = "https://images.example/poster").hasTitleMetadata)
    }
}
