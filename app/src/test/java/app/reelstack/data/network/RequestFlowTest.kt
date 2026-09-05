package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RequestFlowTest {
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "connect.sid=test", "7", sessionCookie = true)
    private val payload = """{"name":"A series","seasons":[{"seasonNumber":0,"episodeCount":2},{"seasonNumber":1,"episodeCount":8},{"seasonNumber":2,"episodeCount":8},{"seasonNumber":3,"episodeCount":6},{"seasonNumber":4,"episodeCount":4}],"mediaInfo":{"status":4,"seasons":[{"seasonNumber":1,"status":5},{"seasonNumber":2,"status":3},{"seasonNumber":4,"status":4}]}}"""
    private class Transport(val detail: String, val responseCode: Int = 201) : JsonHttpTransport {
        var writes = 0
        var body = ""
        override fun get(url: String, headers: Map<String, String>) = HttpResponse(200,
            if (url.endsWith("auth/me")) """{"id":7,"displayName":"Maya"}""" else detail)
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            writes++; body = jsonBody; return HttpResponse(responseCode, "{}")
        }
    }
    @Test fun parsesMissingSeasonsWithoutOfferingAvailableRequestedOrPartialOnes() {
        val details = ServicePayloadParser.mediaDetails(payload)
        assertEquals(listOf(0, 3), details.seasons.filter { it.canRequest }.map { it.number })
        assertEquals("I biblioteket", details.seasons.first { it.number == 1 }.label)
    }
    @Test fun pendingRequestBlocksSeasonEvenBeforeMediaSeasonSync() {
        val data = """{"seasons":[{"seasonNumber":2}],"mediaInfo":{"requests":[{"status":1,"seasons":[{"seasonNumber":2}]}]}}"""
        assertFalse(ServicePayloadParser.mediaDetails(data).seasons.single().canRequest)
    }
    @Test fun sendsExplicitSelectedSeasonsAfterFreshCheck() {
        val transport = Transport(payload)
        SeerrServiceClient(transport).request(connection, "tv", 23, "7", setOf(3))
        assertEquals("""{"mediaType":"tv","mediaId":23,"seasons":[3]}""", transport.body)
    }
    @Test fun refusesAvailableOrUnknownSeasonAndEmptySelection() {
        listOf(setOf(1), setOf(2), setOf(4), setOf(99), emptySet()).forEach { selection ->
            val transport = Transport(payload)
            assertTrue(runCatching { SeerrServiceClient(transport).request(connection, "tv", 23, "7", selection) }.isFailure)
            assertEquals(0, transport.writes)
        }
    }
    @Test fun approvalAloneNeverMeansDownloading() {
        assertEquals(RequestStage.REQUESTED, requestProgress(3, emptyList(), emptySet(), emptyList(), 2).stage)
    }
    @Test fun acceptedWithoutNewSeasonsIsNotReportedAsCreated() {
        val transport = Transport(payload, 202)
        assertTrue(runCatching { SeerrServiceClient(transport).request(connection, "tv", 23, "7", setOf(3)) }.isFailure)
        assertEquals(1, transport.writes)
    }
    @Test fun fourKRequestDoesNotUseStandardLibraryOrDownloadStatus() {
        val request = ServicePayloadParser.requests("""{"results":[{"id":9,"is4k":true,"status":2,"seasons":[{"seasonNumber":1}],"media":{"status":5,"status4k":3,"seasons":[{"seasonNumber":1,"status":5,"status4k":3}],"downloadStatus":[{"episode":{"seasonNumber":1},"status":"completed","size":100,"sizeLeft":0}],"downloadStatus4k":[{"episode":{"seasonNumber":1},"status":"downloading","size":100,"sizeLeft":75}]}}]}""").single()
        assertTrue(request.is4k)
        assertEquals(RequestProgress(RequestStage.DOWNLOADING, 25), requestProgress(request.mediaStatus, request.availableSeasons, request.seasons, request.downloads, request.status))
    }
    @Test fun completedRequestFlagWithoutLibraryAvailabilityIsNotReady() {
        assertEquals(RequestStage.REQUESTED, requestProgress(3, emptyList(), emptySet(), emptyList(), 5).stage)
    }
    @Test fun onlySelectedSeasonsCountForReadiness() {
        val seasons = listOf(RequestSeason(1, "", 8, 5), RequestSeason(2, "", 8, 3))
        assertEquals(RequestStage.REQUESTED, requestProgress(5, seasons, setOf(2), emptyList()).stage)
        assertEquals(RequestStage.AVAILABLE, requestProgress(4, seasons, setOf(1), emptyList()).stage)
    }
    @Test fun unrelatedSeasonDownloadIsIgnored() {
        assertEquals(RequestStage.REQUESTED, requestProgress(3, emptyList(), setOf(2), listOf(RequestDownload(1, "downloading", 100.0, 50.0))).stage)
    }
    @Test fun downloadProgressRequiresActualMatchingDownload() {
        assertEquals(RequestProgress(RequestStage.DOWNLOADING, 50), requestProgress(3, emptyList(), setOf(2), listOf(RequestDownload(2, "downloading", 100.0, 50.0))))
    }
    @Test fun completedBytesWaitForLibraryImport() {
        assertEquals(RequestStage.IMPORTING, requestProgress(3, emptyList(), emptySet(), listOf(RequestDownload(null, "completed", 100.0, 0.0))).stage)
    }
    @Test fun failedAndDeclinedAreNotPretendProgress() {
        assertEquals(RequestStage.DECLINED, requestProgress(1, emptyList(), emptySet(), emptyList(), 3).stage)
        assertEquals(RequestStage.FAILED, requestProgress(3, emptyList(), emptySet(), emptyList(), 4).stage)
    }
    @Test fun readyLibraryOverridesStaleDownload() {
        assertEquals(RequestStage.AVAILABLE, requestProgress(5, emptyList(), emptySet(), listOf(RequestDownload(null, "downloading", 100.0, 40.0))).stage)
    }
    @Test fun seriesCanOpenSeasonPickerEvenWhenSomeOrAllExistingSeasonsAreAvailable() {
        (1..7).filter { it != 6 }.forEach { status ->
            assertTrue(DiscoverMedia("x", "Series", "", 0, status == 5, true, mediaType = "tv", seerrStatus = status).canRequest)
        }
    }
    @Test fun notificationOptInIsDefaultOnlyForNewDrafts() {
        assertTrue(RequestDraft(DiscoverMedia("x", "Film", "", 0, false)).notify)
    }
    @Test fun parserRetainsActualDownloadAndOwnerForRequestTracking() {
        val request = ServicePayloadParser.requests("""{"results":[{"id":9,"status":2,"requestedBy":{"id":7,"displayName":"Maya"},"seasons":[{"seasonNumber":3}],"media":{"tmdbId":23,"mediaType":"tv","status":3,"downloadStatus":[{"episode":{"seasonNumber":3},"status":"downloading","size":100,"sizeLeft":75}]}}]}""").single()
        assertEquals("7", request.ownerId)
        assertEquals(setOf(3), request.seasons)
        assertEquals(25, requestProgress(request.mediaStatus, request.availableSeasons, request.seasons, request.downloads, request.status).percent)
    }
}
