package app.reelstack.data.model

import org.junit.Assert.*
import org.junit.Test

class HomeSectionTest {
    @Test fun newInstallShowsAllRows() {
        assertEquals(HomeSection.entries.toSet(), decodeHomeSections(null))
    }
    @Test fun hiddenRowsStayHidden() {
        assertEquals(emptySet<HomeSection>(), decodeHomeSections(emptySet()))
    }
    @Test fun oldMovieSwitchMigratesToBothMovieRowsOnly() {
        assertEquals(setOf(HomeSection.JELLYFIN_MOVIES, HomeSection.EMBY_MOVIES), decodeHomeSections(setOf("RECENT_MOVIES")))
    }
    @Test fun oldestCombinedSwitchMigratesToFourRows() {
        assertEquals(setOf(HomeSection.JELLYFIN_MOVIES, HomeSection.EMBY_MOVIES, HomeSection.JELLYFIN_SERIES, HomeSection.EMBY_SERIES), decodeHomeSections(setOf("RECENTLY_ADDED")))
    }
    @Test fun independentSelectionRoundTrips() {
        val selected = setOf(HomeSection.EMBY_MOVIES, HomeSection.JELLYFIN_SERIES, HomeSection.UPCOMING)
        assertEquals(selected, decodeHomeSections(selected.map { it.name }.toSet()))
    }
    /**
     * A new row has to survive `decodeHomeSections`, but decoding alone will never *add* it: a
     * saved set lists what is on, and a section invented afterwards is simply absent. That is why
     * `AppPreferencesRepository` carries `HOME_SECTIONS_VERSION` — without it, Favourites would
     * have stayed invisible for everyone who had ever opened the home settings.
     */
    @Test fun newSectionIsPresentForAFreshInstallAndAbsentFromAnOlderSavedSet() {
        assertTrue(HomeSection.FAVOURITES in decodeHomeSections(null))
        assertFalse(HomeSection.FAVOURITES in decodeHomeSections(setOf("NOW_PLAYING", "CONTINUE_WATCHING")))
    }
    @Test fun aTurnedOffNewSectionStaysOffOnceItIsPartOfTheSavedSet() {
        val chosen = setOf(HomeSection.NOW_PLAYING, HomeSection.CONTINUE_WATCHING)
        assertEquals(chosen, decodeHomeSections(chosen.map { it.name }.toSet()))
    }
    @Test fun legacySeriesAndUnknownKeysAreHandled() {
        assertEquals(setOf(HomeSection.NOW_PLAYING, HomeSection.JELLYFIN_SERIES, HomeSection.EMBY_SERIES), decodeHomeSections(setOf("NOW_PLAYING", "RECENT_SERIES", "UNKNOWN")))
    }
}
