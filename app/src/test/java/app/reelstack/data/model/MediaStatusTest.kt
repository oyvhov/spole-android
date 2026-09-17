package app.reelstack.data.model

import app.reelstack.R
import org.junit.Assert.*
import org.junit.Test

class MediaStatusTest {
    private fun media(status: Int?) = DiscoverMedia("id", "Film", "Film · 2026", 0, false, seerrStatus = status)

    @Test fun onlyUnknownOrDeletedTitlesCanBeAdded() {
        listOf(null, 1, 7).forEach { assertTrue(media(it).canRequest) }
        (2..6).forEach { assertFalse(media(it).canRequest) }
        assertFalse(media(null).copy(inLibrary = true).canRequest)
        assertFalse(media(null).copy(requested = true).canRequest)
    }

    // The statuses are resource ids now, so the test names the message rather than its wording —
    // the sentences exist in three languages and matching one of them only ever checked that one.
    @Test fun distinguishesAllSeerrStatesWithoutClaimingDownloadProgress() {
        val labels = (1..7).map { seerrStatusLabel(it) }
        assertEquals(7, labels.distinct().size)
        assertEquals(R.string.seerr_status_awaiting, seerrStatusLabel(2))
        assertEquals(R.string.seerr_status_requested, seerrStatusLabel(3))
        assertEquals(R.string.seerr_status_partial, seerrStatusLabel(4))
        assertEquals(R.string.seerr_status_blocked, seerrStatusLabel(6, inLibrary = true))
        assertEquals(R.string.seerr_status_added, seerrStatusLabel(null, requested = true))
        // "Requested" must not promise a download that has not started.
        assertEquals(R.string.seerr_desc_requested, seerrStatusDescription(3))
        assertEquals(7, (1..7).map { seerrStatusDescription(it) }.distinct().size)
    }

    @Test fun resolvesMovieFallbackButNeverGuessesUnknownVideoIsASeries() {
        assertEquals("Movie", resolvedMediaType(null, "Film · 2026"))
        assertEquals("Series", resolvedMediaType(null, "Serie · 2026"))
        assertEquals("Episode", resolvedMediaType("Episode", "Serie · 2026"))
        assertEquals("Series", resolvedMediaType("tv", "2026"))
        assertNull(resolvedMediaType("Video", "2026"))
    }
}
