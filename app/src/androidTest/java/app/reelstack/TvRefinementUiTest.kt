package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.player.*
import app.reelstack.ui.*
import app.reelstack.ui.components.NavigationOptions
import app.reelstack.ui.screens.*
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvRefinementUiTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", userId = "me")
    @Test fun libraryServiceMenuWorksWithLargeTypeAndEmbyOnlyFallback() {
        val emby = connection.copy(kind = ServiceKind.EMBY)
        assertEquals(ServiceKind.EMBY, ReelstackUiState(connections = listOf(emby)).librarySource)
        rule.setContent { Tv(2f) {
            var source by remember { mutableStateOf(ServiceKind.JELLYFIN) }
            LibraryScreen(ReelstackUiState(connections = listOf(connection, emby), selectedLibrarySource = source),
                {}, {}, {}, onSource = { source = it })
        } }
        rule.onNodeWithText("Emby").assertDoesNotExist()
        rule.onNodeWithTag("library-source-menu").assertIsDisplayed().performClick()
        rule.onNodeWithTag("library-source-EMBY").assertIsDisplayed().performClick()
        rule.onNodeWithTag("library-source-menu").assertTextContains("Emby")
        rule.onNodeWithText("Jellyfin").assertDoesNotExist()
        rule.onNodeWithTag("library-source-menu").performClick()
        rule.onNodeWithTag("library-source-JELLYFIN").performClick()
        rule.onNodeWithTag("library-source-menu").assertTextContains("Jellyfin")
    }

    @Test fun televisionHeroUsesWindowHeightEvenWithCompactArtwork() {
        rule.setContent { Tv {
            CompositionLocalProvider(LocalPersonalization provides Personalization(heroCompact = false)) {
                app.reelstack.ui.components.TabletLibraryFeature(LibraryMedia("hero", "Testserie", "S02 E02",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, season = 2, episode = 2), {})
            }
        } }
        assertTrue(rule.onNodeWithTag("tablet-library-feature").getUnclippedBoundsInRoot().height >= 400.dp)
        rule.onNodeWithText("S2 - E2").assertIsDisplayed()
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed()
    }

    @Test fun preferredLibrarySourceSurvivesRepositoryRecreation() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = app.reelstack.data.repository.AppPreferencesRepository(context)
        val previous = preferences.preferredLibrarySource
        try {
            preferences.preferredLibrarySource = ServiceKind.EMBY
            assertEquals(ServiceKind.EMBY, app.reelstack.data.repository.AppPreferencesRepository(context).preferredLibrarySource)
        } finally { preferences.preferredLibrarySource = previous }
    }

    @Test fun osdLogoPixelsAlignWithEpisodeLabel() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File(context.cacheDir, "osd-alignment-fixture.png")
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        rule.setContent { Tv {
            PlayerScreen(PlayerScreenState(busy = false, playing = false, logoUrl = file.toURI().toString(),
                season = 2, episode = 2), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.waitUntil(10_000) {
            val pixels = rule.onNodeWithTag("player-clearlogo").captureToImage().toPixelMap()
            pixels[pixels.width / 10, pixels.height / 2].red > .95f
        }
        assertEquals(rule.onNodeWithTag("player-clearlogo").getUnclippedBoundsInRoot().left,
            rule.onNodeWithTag("player-episode-label").getUnclippedBoundsInRoot().left)
        rule.onNodeWithTag("player-episode-label").assertTextEquals("S2 - E2")
    }

    @Test fun criticRatingIsVisibleAtLargeTypeAndRespectsRatingPreference() {
        var visible by mutableStateOf(true)
        rule.setContent { Tv(2f) {
            CompositionLocalProvider(LocalPersonalization provides Personalization(showRatings = visible)) {
                app.reelstack.ui.components.PlaybackMetadata(ContentDetails("emby-film", "Film", "Emby", "",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.EMBY, criticRating = 91), listOf("2024", "148 min"))
            }
        } }
        rule.onNodeWithTag("critic-rating").assertIsDisplayed().assertTextEquals("Rotten Tomatoes  91%")
        rule.runOnIdle { visible = false }
        rule.onNodeWithTag("critic-rating").assertDoesNotExist()
    }
    @Test fun remoteFocusKeepsRailGeometryAndCaptionBaselinesStable() {
        val titles = listOf("Kort", "Ein mykje lengre serietittel som treng to linjer")
        val items = titles.mapIndexed { index, title -> LibraryMedia("steady-$index", title, "Episode",
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Episode") }
        rule.setContent { Tv {
            val mode = LocalInputModeManager.current
            SideEffect { mode.requestInputMode(InputMode.Keyboard) }
            Box(Modifier.padding(24.dp)) { ResumeRail(items, {}) }
        } }
        val first = rule.onNodeWithTag("resume-card-steady-0")
        val second = rule.onNodeWithTag("resume-card-steady-1")
        val initial = first.getUnclippedBoundsInRoot()
        first.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        rule.waitForIdle()
        assertEquals(initial, first.getUnclippedBoundsInRoot())
        assertEquals(first.getUnclippedBoundsInRoot().height, second.getUnclippedBoundsInRoot().height)
        first.performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        second.assertIsFocused()
        second.performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionLeft) }
        first.assertIsFocused()
        assertEquals(initial, first.getUnclippedBoundsInRoot())
    }
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Composable private fun Tv(fontScale: Float = 1f, content: @Composable () -> Unit) {
        val config = Configuration(LocalConfiguration.current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_TELEVISION
        }
        CompositionLocalProvider(LocalConfiguration provides config) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp, 540.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) { ReelstackTheme(content) }
            }
        }
    }
    @Test fun televisionDetailsFocusResumeShowProgressQualityAndOmitBackToolbar() {
        org.junit.Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        rule.setContent { Tv {
            val mode = LocalInputModeManager.current
            SideEffect { mode.requestInputMode(InputMode.Keyboard) }
            ReelstackSheets(ReelstackUiState(connections = listOf(connection), activeSheet = AppSheet.TitleDetails("jellyfin-episode"),
                contentDetails = ContentDetails("jellyfin-episode", "Testserie", "Jellyfin", "S02 E03 · Episode", artworkRes = R.drawable.media_placeholder,
                    source = ServiceKind.JELLYFIN, mediaType = "Episode", progress = .42f, remainingMinutes = 27,
                    quality = listOf("4K", "HDR10", "EAC3 5.1"), facts = listOf("2026", "47 min", "★ 8,5"),
                    overview = "Ein episode med full omtale.", libraryAvailable = true)), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed().assertIsFocused()
        rule.onNodeWithTag("play-in-spole").assertTextContains("Hald fram · 27 min att")
        rule.onNodeWithTag("tv-detail-hero").assertIsDisplayed()
        rule.onNodeWithTag("tv-detail-artwork").assertDoesNotExist()
        rule.onNodeWithText("4K", substring = true).assertIsDisplayed()
        rule.onNodeWithText("8,5", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Detaljar").assertDoesNotExist()
        rule.onNodeWithContentDescription("Tilbake").assertDoesNotExist()
        capture("alpha09-tv-details")
    }
    @Test fun televisionPlayerNeverDisplaysTouchBackButton() {
        rule.setContent { Tv { PlayerScreen(PlayerScreenState(busy = false, playing = false), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("player-toggle").assertIsDisplayed()
        rule.onNodeWithTag("player-close").assertDoesNotExist()
    }
    @Test fun movieDetailsKeepHeadingVisibleWhenPlayReceivesInitialFocus() {
        rule.setContent { Tv {
            ReelstackSheets(ReelstackUiState(connections = listOf(connection), activeSheet = AppSheet.TitleDetails("jellyfin-film"),
                contentDetails = ContentDetails("jellyfin-film", "Ein heil filmtittel", "Jellyfin", "2026",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Movie",
                    facts = listOf("2026", "118 min", "★ 8,5", "Eit filmstudio"), genres = listOf("Drama", "Mystery"),
                    overview = "Ei lang omtale. ".repeat(50), libraryAvailable = true)),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithTag("play-in-spole").assertIsFocused()
        assertTrue(rule.onNodeWithText("Ein heil filmtittel").getUnclippedBoundsInRoot().top >= 16.dp)
        rule.onNodeWithTag("tv-cinematic-detail").assertIsDisplayed()
        capture("tv-pass2-detail-heading")
        repeat(8) { rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) } }
        repeat(12) { rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) } }
        assertTrue("Returning up must reveal the heading", rule.onNodeWithTag("detail-title").getUnclippedBoundsInRoot().top >= 0.dp)
    }
    @Test fun seriesHeaderAndResumeRemainReachableAfterEpisodeNavigation() = checkSeriesHeader(1f)
    @Test fun seriesHeaderRemainsReachableAtDoubleTextSize() = checkSeriesHeader(2f)
    private fun checkSeriesHeader(fontScale: Float) {
        val episode = LibraryMedia("jellyfin-ep", "Testserie", "Episode 1", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "ep", mediaType = "Episode", season = 1, episode = 1)
        rule.setContent { Tv(fontScale) {
            ReelstackSheets(ReelstackUiState(connections = listOf(connection), activeSheet = AppSheet.TitleDetails("jellyfin-series"),
                contentDetails = ContentDetails("jellyfin-series", "Ei heilt ny serieside", "Jellyfin", "2026",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, mediaType = "Series",
                    overview = "Omtale av serien. ".repeat(12), libraryAvailable = true),
                seriesBrowse = SeriesBrowse(seriesId = "series", openedFor = "jellyfin-series", nextUp = episode,
                    seasons = listOf(episode.copy(id = "season", remoteId = "season", title = "Sesong 1")),
                    selectedSeasonId = "season", episodes = (1..12).map { episode.copy(id = "jellyfin-ep$it", remoteId = "ep$it", episode = it) })),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithTag("play-in-spole").assertIsFocused()
        rule.onNodeWithTag("tv-detail-hero").assertIsDisplayed()
        repeat(9) { rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) } }
        repeat(16) { rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) } }
        rule.onNodeWithTag("play-in-spole").assertIsDisplayed()
        capture("tv-pass3-series-$fontScale")
        assertTrue("Title bounds: ${rule.onNodeWithTag("detail-title").getUnclippedBoundsInRoot()}",
            rule.onNodeWithTag("detail-title").getUnclippedBoundsInRoot().top >= 0.dp)
    }
    @Test fun highContrastKeepsArtworkVisibleBesideTheReadingArea() {
        rule.setContent { Tv {
            CompositionLocalProvider(LocalPersonalization provides Personalization(highContrast = true)) {
                app.reelstack.ui.components.TvCinematicDetails(
                    ContentDetails("art", "Film", "Emby", "", artworkRes = R.drawable.session_still),
                    rememberScrollState(), heading = { androidx.compose.material3.Text("Film") }) {}
            }
        } }
        val pixels = rule.onNodeWithTag("tv-detail-hero").captureToImage().toPixelMap()
        val colours = mutableSetOf<Int>()
        for (y in 4 until pixels.height / 2 step 4) for (x in pixels.width * 3 / 4 until pixels.width step 4) {
            val colour = pixels[x, y]
            colours += (colour.red * 255).toInt() * 65536 + (colour.green * 255).toInt() * 256 + (colour.blue * 255).toInt()
        }
        assertTrue("High contrast must retain real artwork, not a blank background", colours.size > 100)
    }
    @Test fun embyDetailsOfferPlaybackAtDoubleTextSize() {
        val emby = connection.copy(kind = ServiceKind.EMBY)
        rule.setContent { Tv(2f) {
            ReelstackSheets(ReelstackUiState(connections = listOf(emby), activeSheet = AppSheet.TitleDetails("emby-film"),
                contentDetails = ContentDetails("emby-film", "Ein film frå Emby", "Emby", "2026",
                    artworkRes = R.drawable.media_placeholder, source = ServiceKind.EMBY, mediaType = "Movie",
                    overview = "Ein film med norsk lyd og undertekst.", libraryAvailable = true)),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithTag("play-in-spole").performScrollTo().assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("detail-played").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("detail-favourite").performScrollTo().assertIsEnabled()
        capture("emby-cinematic-large")
    }
    @Test fun libraryChooserKeepsSavedChoicesWhenServerListArrives() {
        val state = mutableStateOf(ReelstackUiState(libraryChoicesLoading = true))
        var saved: Set<String>? = null
        rule.setContent { Tv {
            LibraryChoicesDialog(state.value, {}, {}, { selected, _, _ -> saved = selected })
        } }
        rule.runOnIdle { state.value = state.value.copy(libraryChoicesLoading = false,
            libraryChoices = listOf(app.reelstack.data.network.RemoteLibraryView("saved", "Lagret bibliotek", "movies")),
            selectedLibraryIds = setOf("saved")) }
        rule.onNodeWithTag("library-choice-saved").assertIsOn()
        rule.onNodeWithTag("library-selection-save").performClick()
        rule.runOnIdle { assertEquals(setOf("saved"), saved) }
    }
    @Test fun quickConnectActionIsVisibleWithoutScrollingAtDoubleTextSize() {
        val draft = mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "", authMode = ConnectionAuthMode.QUICK_CONNECT))
        var started = false
        rule.setContent { Tv(2f) {
            ConnectionEditorSheet(draft.value, false, {}, {}, { draft.value = draft.value.copy(url = it) }, {}, {}, {}, {}, {}, { started = true }, {})
        } }
        rule.onNodeWithTag("connection-continue").performScrollTo().performClick()
        rule.onNodeWithTag("connection-submit").assertIsDisplayed().performClick()
        capture("alpha09-tv-quick-large")
        rule.runOnIdle { assertTrue(started) }
    }
    @Test fun libraryIsReadableAndContainsNoSettingsOrTvBackButtons() {
        rule.setContent { Tv {
            LibraryScreen(ReelstackUiState(connections = listOf(connection), libraryPath = listOf("movies" to "Filmar"),
                libraryEntries = listOf(RemoteLibraryItem("one", "Lesbar tittel", "2026", null, "Movie", null))), {}, {}, {})
        } }
        rule.onNodeWithText("Vel bibliotek").assertDoesNotExist()
        rule.onNodeWithText("Tilbake").assertDoesNotExist()
        val pixels = rule.onNodeWithText("Lesbar tittel").captureToImage().toPixelMap()
        var bright = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) if (pixels[x,y].red > .7f && pixels[x,y].green > .7f) bright++
        assertTrue("Titles must have light glyphs on the dark library background", bright > 20)
    }
    @Test fun rightmostLibraryToolbarMovesDownToFirstTitle() {
        rule.setContent { Tv {
            val mode = LocalInputModeManager.current
            SideEffect { mode.requestInputMode(InputMode.Keyboard) }
            LibraryScreen(ReelstackUiState(connections = listOf(connection), libraryPath = listOf("movies" to "Filmar"),
                libraryEntries = (1..8).map { RemoteLibraryItem("film-$it", "Film $it", "2026", null, "Movie", null) }), {}, {}, {})
        } }
        rule.onNodeWithTag("library-search-toggle").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
        rule.onNodeWithTag("library-search-toggle").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        rule.onNodeWithTag("library-item-film-1").assertIsFocused()
    }
    @Test fun unresolvedActivityIsGroupedAndCanBeOpenedWithoutBlankPosterRows() {
        val requests = (1..25).map { TrackedRequest("movie:$it", it, "movie", "Film", null, emptySet(), stage = RequestStage.UNKNOWN, requestId = it) }
        rule.setContent { ReelstackTheme {
            ActivityScreen(ReelstackUiState(connections = listOf(connection), trackedRequests = requests), PaddingValues(0.dp), {})
        } }
        rule.onNodeWithTag("tracked-request-movie:1").assertDoesNotExist()
        rule.onNodeWithTag("unresolved-toggle").assertIsDisplayed().performClick()
        rule.onNodeWithTag("unresolved-movie:1").assertIsDisplayed()
    }
    @Test fun combinedContinueRailDeduplicatesAndCanReturnToSeparateNextUp() {
        var preferences by mutableStateOf(Personalization(combineContinueWatching = true))
        val resume = LibraryMedia("episode", "Testserie", "S01 E01", .4f, R.drawable.media_placeholder, ServiceKind.JELLYFIN, mediaType = "Episode")
        val next = resume.copy(id = "next", subtitle = "S01 E02", progress = null)
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalPersonalization provides preferences) {
            HomeScreen(ReelstackUiState(connections = listOf(connection), sessions = emptyList(), resume = listOf(resume), nextUp = listOf(resume, next),
                homeSections = setOf(HomeSection.CONTINUE_WATCHING)), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, showSearch = false)
        } } }
        rule.onAllNodesWithTag("resume-card-episode").assertCountEquals(1)
        rule.onNodeWithText("Sjå vidare").assertIsDisplayed()
        rule.runOnIdle { preferences = preferences.copy(combineContinueWatching = false) }
        rule.onNodeWithText("Neste episode").performScrollTo().assertIsDisplayed()
    }
    @Test fun navigationOptionsReorderHideAndProtectSettings() {
        var value by mutableStateOf(Personalization())
        rule.setContent { ReelstackTheme { Column(Modifier.verticalScroll(rememberScrollState())) { NavigationOptions(value) { value = it } } } }
        rule.onNodeWithTag("navigation-options").performClick()
        rule.onNodeWithTag("menu-up-ACTIVITY").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf("HOME", "LIBRARY", "ACTIVITY", "DISCOVER", "SETTINGS"), value.menuOrder) }
        rule.onNode(hasText("Oppdag") and isToggleable()).performScrollTo().performClick()
        rule.runOnIdle { assertFalse("DISCOVER" in value.visibleMenu()); assertTrue("SETTINGS" in value.visibleMenu()) }
    }
    @Test fun televisionLibrarySettingsKeepSaveVisibleWithManyLibrariesAndLargeText() {
        org.junit.Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        val choices = (1..12).map { app.reelstack.data.network.RemoteLibraryView("library-$it", "Bibliotek $it", "movies") }
        var saved: List<String>? = null
        rule.setContent { Tv(2f) {
            LibraryChoicesDialog(ReelstackUiState(libraryChoices = choices, selectedLibraryIds = choices.map { it.id }.toSet()),
                {}, {}, { _, pins, _ -> saved = pins })
        } }
        rule.onNodeWithTag("library-selection-save").assertIsDisplayed()
        rule.onNodeWithTag("library-pin-library-1").performScrollTo().performClick()
        rule.onNodeWithTag("library-selection-save").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(listOf("library-1"), saved) }
        capture("alpha09-tv-library-settings-large")
    }
}
