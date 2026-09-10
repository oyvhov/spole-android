package app.reelstack.ui.layout

import app.reelstack.data.model.*
import app.reelstack.ui.components.tabletFeaturedTitle
import app.reelstack.ui.components.tabletFeaturedTitles
import org.junit.Assert.*
import org.junit.Test

class TabletFeatureSelectionTest {
    @Test fun carouselContainsThreeDifferentSeriesNotThreeEpisodes() {
        val first = item(ServiceKind.JELLYFIN).copy(id = "episode-1", title = "Silo")
        val duplicate = first.copy(id = "episode-2", title = "  SILO  ")
        val mirror = duplicate.copy(id = "emby-episode", source = ServiceKind.EMBY)
        val second = first.copy(id = "episode-3", title = "Taskmaster")
        val third = first.copy(id = "episode-4", title = "Alone")
        val fourth = first.copy(id = "episode-5", title = "Another series")
        assertEquals(listOf(first, second, third), tabletFeaturedTitles(
            listOf(first, duplicate, mirror, second, third, fourth), HomeSection.entries.toSet()))
    }
    @Test fun fewerUniqueSeriesAreNotPaddedWithDuplicates() {
        val one = item(ServiceKind.JELLYFIN)
        assertEquals(listOf(one), tabletFeaturedTitles(listOf(one, one.copy(id = "another-episode")), HomeSection.entries.toSet()))
    }
    private fun item(source: ServiceKind, url: String? = "https://example.test/image") =
        LibraryMedia(source.name, "A series", "S01 E01", artworkRes = 0, source = source, artworkUrl = url)

    @Test fun hiddenServiceCannotSupplyTheFeature() {
        val emby = item(ServiceKind.EMBY)
        assertEquals(emby, tabletFeaturedTitle(listOf(item(ServiceKind.JELLYFIN), emby), setOf(HomeSection.EMBY_SERIES)))
    }
    @Test fun hiddenRowsAndMissingArtworkDoNotProduceADemoFeature() {
        assertNull(tabletFeaturedTitle(listOf(item(ServiceKind.JELLYFIN)), emptySet()))
        assertNull(tabletFeaturedTitle(listOf(item(ServiceKind.JELLYFIN, null)), HomeSection.entries.toSet()))
        assertNull(tabletFeaturedTitle(emptyList(), HomeSection.entries.toSet()))
    }
    @Test fun selectionDoesNotInventARecommendationOrPreferAHiddenMovieRow() {
        assertNull(tabletFeaturedTitle(listOf(item(ServiceKind.SEERR)), HomeSection.entries.toSet()))
        assertNull(tabletFeaturedTitle(listOf(item(ServiceKind.EMBY)), setOf(HomeSection.EMBY_MOVIES)))
    }
}
