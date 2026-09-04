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
    @Test fun legacySeriesAndUnknownKeysAreHandled() {
        assertEquals(setOf(HomeSection.NOW_PLAYING, HomeSection.JELLYFIN_SERIES, HomeSection.EMBY_SERIES), decodeHomeSections(setOf("NOW_PLAYING", "RECENT_SERIES", "UNKNOWN")))
    }
}
