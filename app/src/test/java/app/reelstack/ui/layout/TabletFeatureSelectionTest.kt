package app.reelstack.ui.layout

import app.reelstack.data.model.*
import app.reelstack.ui.components.tabletFeaturedTitle
import org.junit.Assert.*
import org.junit.Test

class TabletFeatureSelectionTest {
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
