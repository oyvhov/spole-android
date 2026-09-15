package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.*
import app.reelstack.ui.components.*
import app.reelstack.ui.screens.*
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesignRefreshUiTest {
    @get:Rule val rule = createComposeRule()
    private fun fixtures() = ReelstackUiState(
        libraryEntries = listOf(
            RemoteLibraryItem("movies", "Filmar", "", null, "CollectionFolder", null, isFolder = true, collectionType = "movies"),
            RemoteLibraryItem("series", "Seriar", "", null, "CollectionFolder", null, isFolder = true, collectionType = "tvshows"),
            RemoteLibraryItem("collections", "Samlingar", "", null, "CollectionFolder", null, isFolder = true, collectionType = "boxsets")
        ),
        libraryPeeks = mapOf("movies" to demoRecentMovies(), "series" to demoRecentSeries()),
        nextUp = demoNextUp(), favourites = demoFavourites(),
    )

    @Composable private fun Canvas(width: Int, height: Int, tv: Boolean = false, font: Float = 1f, body: @Composable () -> Unit) {
        val config = Configuration(LocalConfiguration.current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or
                if (tv) Configuration.UI_MODE_TYPE_TELEVISION else Configuration.UI_MODE_TYPE_NORMAL
        }
        CompositionLocalProvider(LocalConfiguration provides config) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font)) {
                    ReelstackTheme {
                        CompositionLocalProvider(LocalPersonalization provides Personalization(reduceMotion = true, heroRotate = false),
                            LocalMotionEnabled provides false, LocalTabletCanvas provides (width >= 720)) {
                            androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) { body() }
                        }
                    }
                }
            }
        }
    }

    @Test fun libraryTvIsACompleteFrontPageAndOpensExactLibrary() {
        var selected = ""
        rule.setContent { Canvas(960, 540, tv = true) { LibraryHub(fixtures(), { selected = it }, {}, null, {}) } }
        rule.onNodeWithTag("hub-library-movies").assertIsDisplayed().performClick()
        assertEquals("movies", selected)
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed()
        rule.onNodeWithText("Jellyfin").assertDoesNotExist()
        rule.onRoot().saveRoadmapImage("design-library-tv.png")
    }

    @Test fun libraryPhoneUsesTouchLayout() {
        rule.setContent { Canvas(360, 780) { LibraryHub(fixtures(), {}, {}, null, {}) } }
        rule.onNodeWithTag("hub-library-movies").assertIsDisplayed()
        rule.onNodeWithTag("tablet-library-feature").assertDoesNotExist()
        rule.onRoot().saveRoadmapImage("design-library-phone.png")
    }

    @Test fun tabletUsesSameSettingsCategoriesAsTv() {
        rule.setContent { Canvas(900, 900) { SettingsScreen(fixtures(), PaddingValues(), {}, {}, {}, { _, _ -> }) } }
        rule.onNodeWithTag("settings-categories").assertIsDisplayed()
        rule.onNodeWithTag("settings-category-HOME").performClick()
        rule.onNodeWithTag("theme-choice-start-page").performScrollTo().assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("design-settings-tablet.png")
    }

    @Test fun phoneUsesCategoriesWithoutSearch() {
        rule.setContent { Canvas(360, 780) { SettingsScreen(fixtures(), PaddingValues(), {}, {}, {}, { _, _ -> }) } }
        rule.onNodeWithTag("settings-search").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-HOME").performClick()
        rule.onNodeWithTag("hero-compact").performScrollTo().assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("design-settings-phone.png")
    }

    @Test fun hugeFontKeepsControlsInsidePhone() {
        rule.setContent { Canvas(360, 900, font = 2f) { SettingsScreen(fixtures(), PaddingValues(), {}, {}, {}, { _, _ -> }) } }
        rule.onNodeWithTag("settings-category-HOME").performScrollTo().performClick()
        rule.onNodeWithTag("theme-choice-start-page").performScrollTo().assertIsDisplayed()
        val root = rule.onNodeWithTag("mobile-settings").getUnclippedBoundsInRoot()
        val row = rule.onNodeWithTag("theme-choice-start-page").getUnclippedBoundsInRoot()
        rule.onRoot().saveRoadmapImage("design-settings-phone-large.png")
        assertTrue("row=$row root=$root", row.left >= root.left && row.right <= root.right)
    }

    @Test fun changingHeroMetadataNeverMovesAction() {
        val media = mutableStateOf(demoRecentSeries().first().copy(logoUrl = null))
        rule.setContent { Canvas(1000, 900) { TabletLibraryFeature(media.value, {}, candidates = listOf(media.value)) } }
        val frame = rule.onNodeWithTag("tablet-library-feature").getUnclippedBoundsInRoot()
        val button = rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot()
        rule.runOnIdle { media.value = media.value.copy(title = "Lang tittel ".repeat(20), overview = "Ei lang omtale ".repeat(50), season = 4, episode = 9) }
        assertEquals(frame, rule.onNodeWithTag("tablet-library-feature").getUnclippedBoundsInRoot())
        assertEquals(button, rule.onNodeWithTag("tablet-feature-open").getUnclippedBoundsInRoot())
        rule.onRoot().saveRoadmapImage("design-hero-stable.png")
    }

    @Test fun upcomingHasDateAndCannotStartPlayback() {
        val episode = demoNextUp().first().copy(id = "future", remoteId = "future", available = false, premiereDate = "2099-10-04")
        rule.setContent { Canvas(960, 540, tv = true) {
            SeriesEpisodes(SeriesBrowse(openedFor = "series", seasons = listOf(episode.copy(title = "Sesong 2")),
                selectedSeasonId = "s2", episodes = listOf(episode)), "series", {})
        } }
        rule.onNodeWithTag("episode-future").assertIsNotEnabled()
        rule.onRoot().saveRoadmapImage("design-upcoming-tv.png")
    }

    @Test fun appearancePreferencesSurviveRepositoryRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppPreferencesRepository(context)
        val old = repository.personalization
        try {
            val expected = old.copy(startInLibrary = true, heroRotate = false, heroLogo = false,
                heroCompact = true, reduceMotion = true, showUpcomingEpisodes = false,
                homeRowFormats = mapOf("NEXT_UP" to "THUMB"), showLibraryTitle = true, libraryCardsWide = false,
                libraryHubOrder = DEFAULT_LIBRARY_HUB.reversed(), libraryHubHidden = setOf("FEATURE"), libraryOrder = listOf("second", "first"))
            repository.personalization = expected
            assertEquals(expected, AppPreferencesRepository(context).personalization)
        } finally { repository.personalization = old }
    }

    @Test fun combinedLibraryStillShowsNextEpisodeWhenResumeIsEmpty() {
        val next = demoNextUp().first()
        rule.setContent { Canvas(360, 780) {
            CompositionLocalProvider(LocalPersonalization provides Personalization(combineContinueWatching = true)) {
                LibraryHub(fixtures().copy(resume = emptyList(), nextUp = listOf(next)), {}, {}, null, {})
            }
        } }
        rule.onNodeWithTag("resume-card-${next.id}").assertIsDisplayed()
    }

    @Test fun seriesPhoneSummarySharesOneCompactRow() {
        rule.setContent { Canvas(360, 780) {
            Box(Modifier.padding(24.dp)) {
                CinematicTitleHero("Ei serie", "Jellyfin", "Sesong 3", null, R.drawable.session_still, ServiceKind.JELLYFIN, true)
            }
        } }
        val art = rule.onNodeWithTag("episode-detail-artwork").getUnclippedBoundsInRoot()
        val title = rule.onNodeWithText("Ei serie").getUnclippedBoundsInRoot()
        assertTrue(title.left >= art.right)
        assertEquals(104f, art.width.value, 1f)
        rule.onRoot().saveRoadmapImage("design-series-phone.png")
    }
    @Test fun libraryEditorChangesOrderVisibilityAndHeading() {
        val value = mutableStateOf(Personalization())
        rule.setContent { Canvas(960, 540, tv = true) {
            LibraryCustomizationDialog(fixtures(), value.value, { value.value = it }, {})
        } }
        rule.onNodeWithTag("library-title").performClick()
        rule.onNodeWithTag("hub-down-FEATURE").performScrollTo().performClick()
        rule.onNodeWithTag("hub-visible-FAVOURITES").performScrollTo().performClick()
        rule.runOnIdle {
            assertTrue(value.value.showLibraryTitle)
            assertEquals("CONTINUE", value.value.libraryHubOrder.first())
            assertTrue("FAVOURITES" in value.value.libraryHubHidden)
        }
    }

    @Test fun savedLookCanBeNamedLoadedAndRetainedAcrossRepositoryInstances() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppPreferencesRepository(context)
        val old = repository.savedAppearances
        val value = mutableStateOf(Personalization(accent = AccentPalette.CORAL))
        try {
            repository.savedAppearances = emptyList()
            rule.setContent { Canvas(360, 780) { Column { AppearancePresets(value.value) { value.value = it } } } }
            rule.onNodeWithTag("saved-looks").performClick()
            rule.onNodeWithTag("look-name").performTextInput("Filmtest")
            rule.onNodeWithTag("look-save").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals("Filmtest", AppPreferencesRepository(context).savedAppearances.single().name)
                value.value = value.value.copy(accent = AccentPalette.OCEAN)
            }
            rule.onNodeWithTag("look-load-Filmtest").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(AccentPalette.CORAL, value.value.accent) }
        } finally { repository.savedAppearances = old }
    }

    @Test fun episodeHasAnExplicitSeriesLink() {
        var opened = false
        rule.setContent { Canvas(960, 540, tv = true) {
            ReelstackSheets(ReelstackUiState(connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", userId = "me")), activeSheet = AppSheet.TitleDetails("jellyfin-ep"),
                seriesBrowse = SeriesBrowse(seriesId = "series", openedFor = "jellyfin-ep"),
                contentDetails = ContentDetails("jellyfin-ep", "Testserie", "", "Episode 3",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode", libraryAvailable = true)),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onEpisodeSeries = { opened = true })
        } }
        rule.onNodeWithTag("play-in-spole").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) }
        rule.onNodeWithTag("episode-series-link").assertIsFocused().assertIsDisplayed().performClick()
        assertTrue(opened)
    }

}
