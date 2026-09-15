package app.reelstack.data.network

import app.reelstack.data.model.*
import app.reelstack.data.repository.encodeLibraryMetadata
import app.reelstack.data.repository.restoreLibraryMetadata
import app.reelstack.ui.components.SafeArtworkCrop
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DesignRefreshTest {
    @Test fun futureEpisodesRequireAnnouncedDatesAndHaveNoPlayableId() {
        val result = parseUpcomingSeason("""{"episodes":[
            {"name":"Old","episodeNumber":1,"seasonNumber":3,"airDate":"2026-08-01"},
            {"name":"New","episodeNumber":3,"seasonNumber":3,"airDate":"2026-09-20","stillPath":"/still.jpg"},
            {"name":"Unknown","episodeNumber":4,"seasonNumber":3,"airDate":null},
            {"name":"Other season","episodeNumber":1,"seasonNumber":4,"airDate":"2026-10-01"}
        ]}""", 22, 3, LocalDate.of(2026, 9, 15))
        assertEquals(1, result.size)
        assertEquals(3, result.single().episode)
        assertFalse(result.single().available)
        assertNull(result.single().remoteId)
        assertEquals("https://image.tmdb.org/t/p/w780/still.jpg", result.single().artworkUrl)
    }

    private fun card(id: String, number: Int? = 2) = LibraryMedia(id, "Series", "Episode",
        artworkRes = 0, source = ServiceKind.JELLYFIN, mediaType = "Episode",
        season = 3, episode = number, remoteId = id)

    @Test fun localPlayableEpisodeWinsAndNumberlessItemsAreNotLost() {
        val own = card("own")
        val announced = own.copy(id = "metadata", available = false, remoteId = null)
        val result = mergeUpcomingEpisodes(listOf(own, card("one", null), card("two", null)), listOf(announced, card("future", 3)))
        assertEquals(4, result.size)
        assertEquals("own", result.first().id)
        assertTrue(result.first().available)
    }

    @Test fun cachedEpisodePreservesIdentityArtworkAndAvailability() {
        val before = card("episode").copy(seriesId = "series", libraryId = "library", logoUrl = "https://example.org/logo",
            favourite = true, played = true, runtimeMinutes = 42, childCount = 9, available = false, premiereDate = "2026-10-01")
        val restored = card("episode").restoreLibraryMetadata(encodeLibraryMetadata(before))
        assertEquals(before, restored)
        assertEquals(card("episode"), card("episode").restoreLibraryMetadata("old invalid metadata"))
    }

    @Test fun portraitFallbackFitsBeforeFirstFrame() {
        val fit = SafeArtworkCrop.computeScaleFactor(Size(400f, 600f), Size(320f, 180f))
        assertEquals(.3f, fit.scaleX, .001f)
        assertEquals(fit.scaleX, fit.scaleY, 0f)
        val wide = SafeArtworkCrop.computeScaleFactor(Size(1920f, 1080f), Size(320f, 180f))
        assertEquals(1f / 6f, wide.scaleX, .001f)
    }

    @Test fun alphabetFiltersRemainServerScopedAndEncoded() {
        assertTrue(LibraryFilters(initial = "Å").query().contains("NameStartsWith=%C3%85"))
        assertFalse(LibraryFilters(initial = "A&B").query().contains("NameStartsWith"))
        assertEquals(1, LibraryFilters(initial = "A").activeCount)
    }

    @Test fun libraryStartCannotHideItsOwnNavigationEntry() {
        assertTrue("LIBRARY" in Personalization(startInLibrary = true, hiddenMenuItems = setOf("LIBRARY")).visibleMenu())
        assertFalse("LIBRARY" in Personalization(startInLibrary = false, hiddenMenuItems = setOf("LIBRARY")).visibleMenu())
    }

    @Test fun tabletSettingsCollapseForLargeFonts() {
        assertTrue(app.reelstack.ui.layout.WindowLayoutPolicy(820f, 1000f).useSettingsPanes())
        assertFalse(app.reelstack.ui.layout.WindowLayoutPolicy(820f, 1000f).useSettingsPanes(2f))
    }
}
