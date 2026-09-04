package app.reelstack.data.model

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

    @Test fun distinguishesAllSeerrStatesWithoutClaimingDownloadProgress() {
        val labels = (1..7).map { seerrStatusLabel(it) }
        assertEquals(7, labels.distinct().size)
        assertEquals("Ventar på godkjenning", seerrStatusLabel(2))
        assertEquals("Under behandling", seerrStatusLabel(3))
        assertEquals("Delvis tilgjengeleg", seerrStatusLabel(4))
        assertEquals("Blokkert i Seerr", seerrStatusLabel(6, inLibrary = true))
        assertTrue(seerrStatusDescription(3).contains("ikkje nødvendigvis starta"))
        assertEquals("Lagd til", seerrStatusLabel(null, requested = true))
    }

    @Test fun resolvesMovieFallbackButNeverGuessesUnknownVideoIsASeries() {
        assertEquals("Movie", resolvedMediaType(null, "Film · 2026"))
        assertEquals("Series", resolvedMediaType(null, "Serie · 2026"))
        assertEquals("Episode", resolvedMediaType("Episode", "Serie · 2026"))
        assertEquals("Series", resolvedMediaType("tv", "2026"))
        assertNull(resolvedMediaType("Video", "2026"))
    }
}
