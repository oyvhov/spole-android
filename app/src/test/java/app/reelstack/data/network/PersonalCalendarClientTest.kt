package app.reelstack.data.network

import app.reelstack.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PersonalCalendarClientTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC)
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "session", "7", sessionCookie = true)
    private val title = CalendarTitle(42, "tv", "Series")
    private class Transport(var responses: Map<String, HttpResponse>) : JsonHttpTransport {
        val reads = mutableListOf<String>()
        val sentHeaders = mutableListOf<Map<String, String>>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            reads += url; sentHeaders += headers
            return responses[url.substringAfter("/api/v1/")] ?: error("Unexpected calendar endpoint: $url")
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Calendar must be read only")
    }
    private fun episode(number: Int, date: String, season: Int = 2) =
        """{"seasonNumber":$season,"episodeNumber":$number,"airDate":"$date","name":"Episode $number","stillPath":"/still.jpg"}"""
    private fun tv(next: String = episode(1, "2026-10-05")) = HttpResponse(200,
        """{"id":42,"name":"Series","posterPath":"/poster.jpg","seasons":[{"seasonNumber":0,"airDate":"2026-10-06"},{"seasonNumber":2,"airDate":"2026-09-01"}],"nextEpisodeToAir":$next}""")
    private fun transport(vararg episodes: String) = Transport(mapOf("tv/42" to tv(),
        "tv/42/season/2" to HttpResponse(200,"""{"episodes":[${episodes.joinToString()}]}""")))

    @Test fun completeSeasonIncludesTodayAndDay28WithoutSpecialsOrExpiredDates() = runBlocking {
        val network = transport(episode(1,"2026-10-05"), episode(2,"2026-10-12"), episode(3,"2026-11-02"),
            episode(4,"2026-11-03"), episode(9,"2026-10-04"), episode(1,"2026-10-06",0))
        val feed = PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title))
        assertEquals(listOf(1,2,3),feed.upcoming.map { it.episode })
        assertFalse(feed.incomplete)
        assertTrue(feed.undated.isEmpty())
        assertTrue(network.reads.all { it.contains("/api/v1/tv/") })
        assertTrue(network.sentHeaders.all { it.keys.none { name -> name.equals("X-Api-Key",true) } && it["Cookie"]?.contains("session") == true })
    }
    @Test fun latestSeasonIsReadWhenNextEpisodeIsMissing() = runBlocking {
        val network=transport(episode(2,"2026-10-12"));network.responses=network.responses + ("tv/42" to tv("null"))
        assertEquals(1,PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title)).upcoming.size)
    }
    @Test fun titleWithoutLibraryOrRequestRecordStillHasCalendarDates() = runBlocking {
        val network = transport(episode(2, "2026-10-12"))
        val root = network.responses.getValue("tv/42")
        network.responses = network.responses + ("tv/42" to root.copy(body = root.body.dropLast(1) + ",\"mediaInfo\":null}"))
        val feed = PersonalCalendarClient(network, clock).feed(connection, "7", listOf(title))
        assertFalse(feed.incomplete)
        assertTrue(feed.upcoming.isNotEmpty())
    }
    @Test fun sameDayBatchKeepsEveryEpisodeAndHomeShowsNearestOnly() = runBlocking {
        val network=transport(episode(1,"2026-10-05"),episode(2,"2026-10-05"),episode(3,"2026-10-12"))
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title,title))
        assertEquals(3,feed.upcoming.size)
        assertEquals(2,network.reads.size)
        val items=feed.upcoming.map { UpcomingMedia(it.id,it.title,it.subtitle,"",0,0,it.source,
            mediaType=it.mediaType,tmdbId=it.tmdbId,season=it.season,episode=it.episode,releaseDate=it.dateTime) }
        assertEquals(1,calendarHomeItems(items).size)
        assertEquals(2,calendarItemsOnDate(items,LocalDate.parse("2026-10-05")).size)
    }
    @Test fun hiddenSelectionOverridesAllAutomaticSeedsBeforeNetwork() = runBlocking {
        val network=Transport(emptyMap())
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title,title.copy(hidden=true)))
        assertTrue(feed.upcoming.isEmpty());assertTrue(network.reads.isEmpty())
    }
    @Test fun seasonFailureKeepsKnownNextButMarksIncompleteAndRetries() = runBlocking {
        val network=transport();network.responses=network.responses+("tv/42/season/2" to HttpResponse(503,"{}"))
        val client=PersonalCalendarClient(network,clock)
        val partial=client.feed(connection,"7",listOf(title))
        assertEquals(1,partial.upcoming.size);assertTrue(partial.incomplete)
        network.responses=network.responses+("tv/42/season/2" to HttpResponse(200,"""{"episodes":[${episode(2,"2026-10-12")}]}"""))
        val retry=client.feed(connection,"7",listOf(title));assertEquals(2,retry.upcoming.size);assertFalse(retry.incomplete)
    }
    @Test fun forbiddenSessionFailsClosedInsteadOfShowingCachedCalendar() = runBlocking {
        val network=transport(episode(2,"2026-10-12"));val client=PersonalCalendarClient(network,clock)
        client.feed(connection,"7",listOf(title))
        network.responses=mapOf("tv/42" to HttpResponse(403,"{}"))
        assertTrue(runCatching { client.feed(connection,"7",listOf(title),force=true) }.exceptionOrNull() is CalendarAccessException)
    }
    @Test fun forceRefreshReplacesMovedDateWithSameIdentity() = runBlocking {
        val network=transport();val client=PersonalCalendarClient(network,clock)
        val before=client.feed(connection,"7",listOf(title)).upcoming.single()
        network.responses=network.responses+("tv/42" to tv(episode(1,"2026-10-10")))
        val after=client.feed(connection,"7",listOf(title),force=true).upcoming.single()
        assertEquals(before.id,after.id);assertEquals("2026-10-10",after.dateTime)
    }
    @Test fun differentUserProfileTokenAndServerNeverReusePrivateCache() = runBlocking {
        val network=transport();val client=PersonalCalendarClient(network,clock)
        client.feed(connection,"7",listOf(title));val first=network.reads.size
        client.feed(connection,"7",listOf(title));assertEquals(first,network.reads.size)
        client.feed(connection,"8",listOf(title));assertEquals(first*2,network.reads.size)
        client.feed(connection,"8",listOf(title),scopeSalt="other-profile");assertEquals(first*3,network.reads.size)
        client.feed(connection.copy(token="new-session"),"8",listOf(title),scopeSalt="other-profile");assertEquals(first*4,network.reads.size)
        client.feed(connection.copy(baseUrl="https://another.example"),"8",listOf(title));assertEquals(first*5,network.reads.size)
    }
    @Test fun missingDatesStayInFollowingOverviewWithoutInventedEvents() = runBlocking {
        val network=transport();network.responses=network.responses+("tv/42" to tv("null"))
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title))
        assertTrue(feed.upcoming.isEmpty());assertEquals(listOf("tv:42"),feed.undated.map { it.key });assertFalse(feed.incomplete)
    }
    @Test fun movieOnlyUsesDigitalDateAndRetainsItsRegion() = runBlocking {
        val network=Transport(mapOf("movie/42" to HttpResponse(200,"""{"id":42,"title":"Film","releaseDate":"2026-08-01","releases":{"results":[{"iso_3166_1":"NO","release_dates":[{"type":3,"release_date":"2026-10-05"},{"type":4,"release_date":"2026-10-09"}]}]}}""")))
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title.copy(mediaType="movie")))
        assertEquals("2026-10-09",feed.upcoming.single().dateTime);assertEquals("NO",feed.upcoming.single().region)
    }
    @Test fun blockedTitleDoesNotAppearEvenIfPreviouslyFollowed() = runBlocking {
        val network=Transport(mapOf("tv/42" to HttpResponse(200,"""{"id":42,"name":"Blocked","mediaInfo":{"status":6}}""")))
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title))
        assertTrue(feed.upcoming.isEmpty());assertTrue(feed.undated.isEmpty())
    }
    @Test fun rateLimitedMetadataIsReportedAndNotCachedAsEmptySuccess() = runBlocking {
        val network=Transport(mapOf("tv/42" to HttpResponse(429,"{}")));val client=PersonalCalendarClient(network,clock)
        assertTrue(client.feed(connection,"7",listOf(title)).incomplete)
        network.responses=transport().responses
        assertFalse(client.feed(connection,"7",listOf(title)).incomplete)
        assertEquals(3,network.reads.size)
    }
    @Test fun largeSeasonIsNotCutAtThirtyOrSixtyEpisodes() = runBlocking {
        val network=transport(*(2..86).map { episode(it,"2026-10-12") }.toTypedArray())
        val feed=PersonalCalendarClient(network,clock).feed(connection,"7",listOf(title))
        assertEquals(86,feed.upcoming.size) // Next episode today plus the complete later season batch.
    }
    @Test fun allPersonalRequestPagesAreReadAndForeignOwnersAreExcluded() {
        val paths=mutableListOf<String>()
        val network=object : JsonHttpTransport {
            override fun get(url: String,headers: Map<String,String>): HttpResponse {
                paths+=url
                val start=if ("skip=100" in url) 101 else 1
                val end=if(start==101) 105 else 100
                val rows=(start..end).joinToString { id ->
                    """{"id":$id,"status":1,"requestedBy":{"id":${if(id==105)8 else 7}},"media":{"tmdbId":$id,"mediaType":"tv"}}"""
                }
                return HttpResponse(200,"""{"results":[$rows]}""")
            }
            override fun post(url: String,headers: Map<String,String>,jsonBody: String): HttpResponse=error("Must not submit requests")
        }
        val items=SeerrServiceClient(network).calendarRequests(connection,"7")
        assertEquals(104,items.size);assertEquals(2,paths.size)
        assertTrue(paths.all { "requestedBy=7" in it && "/queue" !in it })
        assertTrue(items.all { it.ownerId=="7" })
    }
    @Test fun identicalNamesWithDifferentMetadataIdsRemainSeparate() {
        val a=UpcomingMedia("first","Same title","","",0,0,ServiceKind.SEERR,mediaType="Movie",tmdbId=1)
        val b=a.copy(id="second",tmdbId=2)
        assertEquals(2,app.reelstack.data.repository.mergeReleaseItems(listOf(a,b)).size)
        assertEquals(1,app.reelstack.data.repository.mergeReleaseItems(listOf(a,a.copy(id="other-source"))).size)
    }
}
