package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class WatchingOrderTest {
    private fun media(id: String, time: Long? = null, progress: Float? = null) = LibraryMedia(id, id, "", progress,
        0, ServiceKind.JELLYFIN, lastActivityEpochMillis = time)

    @Test fun combinedRowInterleavesByActivityRatherThanRowType() {
        val result = combinedWatching(listOf(media("paused-old", 10, .5f), media("paused-new", 40, .2f)),
            listOf(media("next-middle", 30), media("next-newest", 50)))
        assertEquals(listOf("next-newest", "paused-new", "next-middle", "paused-old"), result.map { it.id })
    }
    @Test fun duplicatesKeepResumeProgressAndMissingDatesStayStableAtEnd() {
        val result = combinedWatching(listOf(media("same", 40, .6f), media("unknown-resume")),
            listOf(media("same", 90), media("unknown-next"), media("equal", 40)))
        assertEquals(listOf("same", "equal", "unknown-resume", "unknown-next"), result.map { it.id })
        assertEquals(.6f, result.first().progress!!, .001f)
    }
    @Test fun parserUsesPlaybackDateAndDoesNotInventItFromDateCreated() {
        val result = ServicePayloadParser.libraryItems("""{"Items":[
            {"Id":"1","Name":"One","SeriesId":"series","UserData":{"LastPlayedDate":"2026-09-10T10:20:30.1234567Z"}},
            {"Id":"2","Name":"Two","DateCreated":"2026-09-10T10:20:30Z"},
            {"Id":"3","Name":"Three","UserData":{"LastPlayedDate":"invalid"}}]}""")
        assertEquals("series", result[0].seriesId)
        assertEquals(Instant.parse("2026-09-10T10:20:30.1234567Z").toEpochMilli(), result[0].lastActivityEpochMillis)
        assertNull(result[1].lastActivityEpochMillis)
        assertNull(result[2].lastActivityEpochMillis)
    }
    @Test fun nextUpUsesLastWatchedEpisodeForTheSameProfileAndDoesNotLoadExcludedLibraries() {
        val urls = CopyOnWriteArrayList<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String,String>): HttpResponse {
                urls += url
                val body = when {
                    url.contains("Shows/NextUp") -> """{"Items":[{"Id":"next","Name":"Next","Type":"Episode","SeriesId":"show-id"}]}"""
                    url.contains("ParentId=show-id") -> """{"Items":[{"Id":"watched","Name":"Previous","UserData":{"LastPlayedDate":"2026-09-09T18:00:00Z"}}]}"""
                    else -> error("Unexpected URL")
                }
                return HttpResponse(200, body)
            }
            override fun post(url: String, headers: Map<String,String>, jsonBody: String): HttpResponse = error("Read only")
        }
        val client = MediaServerClient(transport, includeLibrary = { _, view -> view.id == "chosen" })
        val result = client.nextUp(ServiceConnection(ServiceKind.JELLYFIN,"Test","https://example.com","fixture"), "own-profile",
            listOf(RemoteLibraryView("chosen","TV","tvshows"), RemoteLibraryView("excluded","Other","tvshows")))
        assertEquals(Instant.parse("2026-09-09T18:00:00Z").toEpochMilli(), result.single().lastActivityEpochMillis)
        assertEquals(2, urls.size)
        assertTrue(urls.all { it.contains("UserId=own-profile") && !it.contains("excluded") })
        assertTrue(urls.last().contains("IsPlayed=true&SortBy=DatePlayed&SortOrder=Descending"))
    }
    @Test fun missingSeriesHistoryKeepsTheNextEpisodeWithoutAnInventedDate() {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String,String>) = if (url.contains("NextUp"))
                HttpResponse(200,"""{"Items":[{"Id":"n","Name":"Next","SeriesId":"s"}]}""") else HttpResponse(503,"{}")
            override fun post(url: String, headers: Map<String,String>, jsonBody: String): HttpResponse = error("Read only")
        }
        val result = MediaServerClient(transport).nextUp(ServiceConnection(ServiceKind.JELLYFIN,"Test","https://example.com","fixture"),
            "user", listOf(RemoteLibraryView("tv","TV","tvshows")))
        assertEquals("n", result.single().id)
        assertNull(result.single().lastActivityEpochMillis)
    }
}
