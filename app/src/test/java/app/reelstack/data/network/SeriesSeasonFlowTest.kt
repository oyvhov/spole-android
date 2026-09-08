package app.reelstack.data.network

import app.reelstack.data.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class SeriesSeasonFlowTest {
    private val today = LocalDate.of(2026, 9, 8)

    @Test fun futureSeasonShowsPremiereInsteadOfAnOverdueLibraryGap() {
        val season = RequestSeason(3, "Sesong 3", 8, 1, today.plusDays(10))
        assertTrue(season.canRequest)
        assertTrue(season.description(today).startsWith("Kjem "))
        assertFalse(season.description(today).contains("Manglar"))
    }

    @Test fun undatedSeasonDoesNotPretendToHaveBeenReleased() {
        assertEquals("Premiere ikkje avklart", RequestSeason(2, "", 0, 1).description(today))
    }

    @Test fun airedSeasonCanBeMissingAndMayStillBeRequested() {
        assertEquals("Manglar i biblioteket", RequestSeason(2, "", 8, 7, today).description(today))
    }

    @Test fun availabilityAndRequestStatusTakePrecedenceOverPremiereMetadata() {
        for (status in 2..6) {
            val season = RequestSeason(2, "", 8, status, today.plusYears(1))
            assertEquals(season.label, season.description(today))
            assertFalse(season.canRequest)
        }
    }

    @Test fun returningSeriesIsNotProofOfMonitoringAndDoesNotPreselectAnything() {
        val data = """{"name":"Returning","status":"Returning Series","inProduction":true,
            "seasons":[{"seasonNumber":1,"airDate":"2025-01-01"},{"seasonNumber":2,"airDate":"2028-01-01"}]}"""
        val remote = ServicePayloadParser.mediaDetails(data)
        assertTrue(remote.seasons.all { it.canRequest })
        val draft = RequestDraft(DiscoverMedia("id", "Returning", "Serie", 0, false, mediaType = "tv"), remote.seasons)
        assertTrue(draft.selected.isEmpty())
        assertEquals(LocalDate.of(2028, 1, 1), remote.seasons.last().airDate)
    }

    @Test fun approvedSeasonIsRequestedNotWaitingForApproval() {
        val data = """{"seasons":[{"seasonNumber":2}],"mediaInfo":{"requests":[
            {"status":2,"seasons":[{"seasonNumber":2}]}]}}"""
        assertEquals(3, ServicePayloadParser.mediaDetails(data).seasons.single().status)
    }

    @Test fun fourKRequestsDoNotLockStandardSeasons() {
        val data = """{"seasons":[{"seasonNumber":2}],"mediaInfo":{"requests":[
            {"status":2,"is4k":true,"seasons":[{"seasonNumber":2}]}]}}"""
        val remote = ServicePayloadParser.mediaDetails(data)
        assertTrue(remote.seasons.single().canRequest)
        assertEquals(3, remote.seasons4k.single().status)
    }

    @Test fun malformedDatesAreOptionalNotLoadFailures() {
        val data = """{"seasons":[{"seasonNumber":2,"airDate":"bad-date"}],
            "nextEpisodeToAir":{"seasonNumber":2,"episodeNumber":1,"airDate":"bad-date"}}"""
        assertNull(ServicePayloadParser.mediaDetails(data).seasons.single().airDate)
        assertNull(ServicePayloadParser.mediaDetails(data).nextEpisode)
    }

    @Test fun nextEpisodeNeedsDateAndNumberAndNeverShowsOldNextDate() {
        val data = """{"nextEpisodeToAir":{"seasonNumber":2,"episodeNumber":3,"airDate":"2026-09-09"}}"""
        val next = ServicePayloadParser.mediaDetails(data).nextEpisode!!
        assertTrue(next.description(today)!!.contains("S02 E03"))
        assertNull(next.description(today.plusDays(2)))
        assertNull(ServicePayloadParser.mediaDetails("""{"nextEpisodeToAir":{"airDate":"2026-09-09"}}""").nextEpisode)
    }

    @Test fun onlyPartialAndAlreadyRequestedSeasonsOfferAvailabilityAlerts() {
        assertEquals(listOf(2, 3, 4), (0..8).filter { RequestSeason(1, "", 8, it).canWatch })
    }

    @Test fun localFollowIsNotReportedAsARequestOrDownload() {
        val partial = listOf(RequestSeason(1, "", 8, 4))
        assertEquals(RequestStage.WATCHING, availabilityWatchProgress(partial, setOf(1), emptyList(), 4).stage)
        assertEquals(RequestStage.WATCHING, availabilityWatchProgress(partial, setOf(1),
            listOf(RequestDownload(2, "downloading", 100.0, 50.0)), 4).stage)
    }

    @Test fun missingOrBlockedWatchStatusStaysUnknown() {
        assertEquals(RequestStage.UNKNOWN, availabilityWatchProgress(emptyList(), setOf(1), emptyList(), 5).stage)
        assertEquals(RequestStage.UNKNOWN, availabilityWatchProgress(listOf(RequestSeason(1, "", 8, 4)), setOf(1), emptyList(), 6).stage)
    }

    @Test fun watchWaitsForSeerrLibraryConfirmationNotDownloadCompletion() {
        val partial = listOf(RequestSeason(1, "", 8, 4))
        assertEquals(RequestStage.IMPORTING, availabilityWatchProgress(partial, setOf(1),
            listOf(RequestDownload(1, "completed", 100.0, 0.0)), 4).stage)
        assertEquals(RequestStage.AVAILABLE, availabilityWatchProgress(listOf(partial.single().copy(status = 5)), setOf(1), emptyList(), 4).stage)
    }
}
