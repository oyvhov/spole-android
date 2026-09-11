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
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            rule.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
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
        rule.onNodeWithText("Hald fram").assertIsDisplayed()
        rule.onNodeWithText("42% sett · 27 min att").assertIsDisplayed()
        rule.onNodeWithText("4K").assertIsDisplayed()
        rule.onNodeWithText("8,5").assertIsDisplayed()
        rule.onNodeWithText("Detaljar").assertDoesNotExist()
        rule.onNodeWithContentDescription("Tilbake").assertDoesNotExist()
        capture("alpha09-tv-details")
    }
    @Test fun televisionPlayerNeverDisplaysTouchBackButton() {
        rule.setContent { Tv { PlayerScreen(PlayerScreenState(busy = false, playing = false), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("player-toggle").assertIsDisplayed()
        rule.onNodeWithTag("player-close").assertDoesNotExist()
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
        rule.onNodeWithText("Hald fram å sjå · Neste episode").assertIsDisplayed()
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
