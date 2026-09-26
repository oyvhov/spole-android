package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, height.dp))) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font)) {
                CompositionLocalProvider(LocalConfiguration provides config) {
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
        org.junit.Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        var selected = ""
        rule.setContent { Canvas(960, 540, tv = true) { LibraryHub(fixtures(), { selected = it }, {}, null, {}) } }
        rule.onNodeWithTag("hub-library-movies").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("movies", selected)
        rule.onNodeWithText("Jellyfin").assertDoesNotExist()
        rule.onRoot().saveRoadmapImage("design-library-tv.png")
    }

    @Test fun libraryTvKeepsLastShelfAboveSafeBottomEdge() {
        // ForcedSize scales the canvas down on a phone, so the edge is measured in its density.
        var density = rule.density
        rule.setContent { Canvas(960, 540, tv = true, font = 2f) {
            density = androidx.compose.ui.platform.LocalDensity.current
            Box(Modifier.fillMaxSize().testTag("library-tv-frame")) {
                LibraryHub(fixtures(), {}, {}, null, {})
            }
        } }
        rule.onNodeWithTag("library-hub").performScrollToKey("customize")
        val screen = rule.onNodeWithTag("library-tv-frame").fetchSemanticsNode().boundsInRoot
        val viewport = rule.onNodeWithTag("library-hub").fetchSemanticsNode().boundsInRoot
        val footer = rule.onNodeWithTag("hub-footer").fetchSemanticsNode().boundsInRoot
        val safeEdge = with(density) { ReelLayout.TvSafeEdge.toPx() }
        assertTrue("The TV library must reserve the safe edge while scrolling",
            screen.bottom - viewport.bottom >= safeEdge - 1f)
        assertTrue("The last library action must stay above the safe edge",
            screen.bottom - footer.bottom >= safeEdge - 1f)
    }

    @Test fun libraryPhoneUsesTouchLayout() {
        rule.setContent { Canvas(360, 780) { LibraryHub(fixtures(), {}, {}, null, {}) } }
        rule.onNodeWithTag("hub-library-movies").assertIsDisplayed()
        rule.onNodeWithTag("tablet-library-feature").assertDoesNotExist()
        rule.onRoot().saveRoadmapImage("design-library-phone.png")
    }

    @Test fun libraryCustomizationLivesAfterAllShelvesOnTv() {
        rule.setContent { Canvas(960, 540, tv = true, font = 2f) { LibraryHub(fixtures(), {}, {}, null, {}) } }
        rule.onNodeWithTag("hub-customize").assertDoesNotExist()
        rule.onNodeWithTag("library-hub").performScrollToKey("customize")
        rule.onNodeWithTag("hub-customize").assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("library-customize-footer-tv.png")
        rule.onNodeWithTag("hub-customize").performClick()
        rule.onNodeWithTag("library-title").performScrollTo().assertIsDisplayed()
    }

    @Test fun appearanceSettingsKeepSeasonChoiceWithoutLookPresets() {
        rule.setContent { Canvas(960, 540, tv = true) { Column(Modifier.verticalScroll(rememberScrollState())) {
            VisualThemeSettings(Personalization(), {})
        } } }
        rule.onNodeWithText("Vel uttrykk").assertDoesNotExist()
        rule.onNodeWithTag("saved-looks").assertDoesNotExist()
        rule.onNodeWithTag("theme-gallery").assertDoesNotExist()
        rule.onNodeWithTag("theme-choice-season").performScrollTo().assertIsDisplayed()
    }

    @Test fun posterLongPressOffersDetailsWithoutOpeningThem() {
        var opened = ""
        val movie = demoRecentMovies().first()
        rule.setContent { Canvas(960, 540, tv = true) {
            LibraryRail(listOf(movie), { opened = it }, false, actions = MediaCardActions({}, { _, _ -> }, { _, _ -> }))
        } }
        rule.onNodeWithTag("library-artwork-${movie.id}", useUnmergedTree = true).performTouchInput { longClick() }
        rule.onNodeWithTag("card-details").assertIsDisplayed()
        assertEquals("", opened)
        rule.onNodeWithTag("card-details").performClick()
        assertEquals(movie.id, opened)
    }

    @Test fun choiceDialogKeepsCloseInsideNarrowPhoneAtDoubleFont() {
        rule.setContent { Canvas(320, 740, font = 2f) {
            SpoleChoiceDialog("Undertekstar", {}) { Text("Norsk") }
        } }
        rule.onNodeWithContentDescription("Lukk").assertIsDisplayed()
        rule.onNodeWithTag("choice-dialog").saveRoadmapImage("choice-phone-large.png")
    }

    @Test fun subtitleLanguagesOfferNorwegianAndEnglishAtDoubleFont() {
        var value by mutableStateOf(Personalization())
        rule.setContent { Canvas(360, 780, font = 2f) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SubtitleLanguageSettings(value) { value = it }
            }
        } }
        rule.onNodeWithText("Norsk").assertIsDisplayed()
        rule.onNodeWithText("Engelsk").assertIsDisplayed()
        rule.onNodeWithTag("theme-choice-subtitle-fallback").performScrollTo().performClick()
        rule.onNodeWithTag("subtitle-fallback-NORWEGIAN").assertDoesNotExist()
        rule.onNodeWithTag("subtitle-fallback-SWEDISH").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(SubtitleLanguage.SWEDISH, value.fallbackSubtitleLanguage) }
        rule.onNodeWithTag("theme-choice-subtitle-language").performScrollTo().performClick()
        rule.onNodeWithTag("subtitle-language-NONE").performScrollTo().performClick()
        rule.onNodeWithTag("theme-choice-subtitle-fallback").assertDoesNotExist()
    }

    @Test fun advancedOsdSelectsChapterAndUnwindsWithBack() {
        var seek = -1L
        val state = app.reelstack.player.PlayerScreenState(title = "Film", busy = false, durationMs = 90_000,
            chapters = listOf(app.reelstack.player.PlaybackChapter("Opning", 0), app.reelstack.player.PlaybackChapter("Andre del", 30_000)))
        rule.setContent { Canvas(960, 540, tv = true) {
            app.reelstack.player.PlayerScreen(state, null, {}, {}, { seek = it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.onNodeWithTag("player-chapters").performClick()
        rule.onNodeWithText("0:30 · Andre del", substring = true).performClick()
        assertEquals(30_000L, seek)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
        rule.onNodeWithTag("player-controls").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
        rule.onNodeWithTag("player-controls").assertDoesNotExist()
    }

    @Test fun libraryNamesStayBelowPicturesAndCanBeHiddenOnTv() {
        val names = mutableStateOf(true)
        rule.setContent { Canvas(960, 540, tv = true) {
            CompositionLocalProvider(LocalPersonalization provides Personalization(showLibraryCardNames = names.value)) {
                LibraryHub(fixtures(), {}, {}, null, {})
            }
        } }
        rule.onNodeWithText("Alt").assertDoesNotExist()
        rule.onNodeWithTag("library-hub").performScrollToKey("libraries")
        rule.onNodeWithTag("hub-library-name-movies").assertIsDisplayed()
        val picture = rule.onNodeWithTag("hub-library-movies").fetchSemanticsNode().boundsInRoot
        val label = rule.onNodeWithTag("hub-library-name-movies").fetchSemanticsNode().boundsInRoot
        assertTrue(label.top >= picture.bottom)
        rule.onRoot().saveRoadmapImage("library-labels-below-tv.png")
        rule.runOnIdle { names.value = false }
        rule.onNodeWithTag("hub-library-name-movies").assertDoesNotExist()
        rule.onNodeWithTag("hub-library-movies").assertIsDisplayed()
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
                preferredSubtitleLanguage = SubtitleLanguage.SWEDISH, fallbackSubtitleLanguage = SubtitleLanguage.DANISH,
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


    @Test fun episodeHasAnExplicitSeriesLink() {
        checkEpisodeReadingOrder(1f)
    }

    @Test fun subtitlePreviewAndPreferencesUpdateImmediately() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AppPreferencesRepository(context)
        val old = repository.personalization
        val value = mutableStateOf(old)
        val stop = repository.observePersonalization { value.value = it }
        try {
            rule.setContent { Canvas(360, 780) {
                SubtitleAppearanceSetting(value.value.subtitleStyle) { repository.personalization = value.value.copy(subtitleStyle = it) }
            } }
            rule.onNodeWithTag("subtitle-style").performClick()
            rule.onNodeWithTag("subtitle-preview").assertIsDisplayed()
            rule.onNodeWithTag("subtitle-style-LARGE").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(SubtitleStyle.LARGE, value.value.subtitleStyle)
                assertEquals(SubtitleStyle.LARGE, AppPreferencesRepository(context).personalization.subtitleStyle)
            }
        } finally { stop(); repository.personalization = old }
    }

    @Test fun episodeReadingOrderAtLargeFont() {
        checkEpisodeReadingOrder(2f)
    }

    private fun checkEpisodeReadingOrder(font: Float) {
        org.junit.Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        var opened = false
        rule.setContent { Canvas(960, 540, tv = true, font = font) {
            ReelstackSheets(ReelstackUiState(connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", userId = "me")), activeSheet = AppSheet.TitleDetails("jellyfin-ep"),
                seriesBrowse = SeriesBrowse(seriesId = "series", openedFor = "jellyfin-ep"),
                contentDetails = ContentDetails("jellyfin-ep", "Testserie", "", "Episode 3",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode", libraryAvailable = true,
                    overview = "Episodeteksten skal vere synleg ved tittelen, før avspeling.")),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onEpisodeSeries = { opened = true })
        } }
        rule.onNodeWithTag("overview-text", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Testserie").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("episode-series-name").assertIsDisplayed().assertTextEquals("Episode 3")
        val link = rule.onNodeWithTag("episode-series-link").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText("Testserie").fetchSemanticsNode().boundsInRoot
        val overview = rule.onNodeWithTag("overview-text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val play = rule.onNodeWithTag("play-in-spole").fetchSemanticsNode().boundsInRoot
        val favourite = rule.onNodeWithTag("detail-favourite").fetchSemanticsNode().boundsInRoot
        assertTrue("Serie skal liggje etter favoritt", link.left >= favourite.right || link.top >= favourite.bottom)
        assertTrue("Episodetekst skal kome etter tittelen", title.bottom <= overview.top)
        assertTrue("Episodetekst skal kome før avspeling", overview.bottom <= play.top)
        rule.onNodeWithTag("detail-scroll").saveRoadmapImage("episode-summary-tv-$font.png")
        rule.onNodeWithTag("detail-favourite").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        rule.onNodeWithTag("episode-series-link").assertIsFocused().assertIsDisplayed().performClick()
        assertTrue(opened)
    }

}
