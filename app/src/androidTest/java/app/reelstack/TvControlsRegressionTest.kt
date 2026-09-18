package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import app.reelstack.data.model.*
import app.reelstack.localization.LocalizedText
import app.reelstack.ui.*
import app.reelstack.ui.screens.LibraryFilterBar
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.update.ReleaseNotes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvControlsRegressionTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var input: InputModeManager

    @Composable private fun Television(scale: Float = 1f, content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_TELEVISION
        }
        CompositionLocalProvider(LocalConfiguration provides configuration,
            LocalDensity provides Density(LocalDensity.current.density, scale)) {
            input = LocalInputModeManager.current
            ReelstackTheme { androidx.compose.material3.Surface(Modifier.fillMaxSize()) { content() } }
        }
    }

    @Test fun remoteCanReadLockedSeasonsWithoutSelectingOrSendingThem() = seasons(1f)
    @Test fun seasonListAndCompactIdentityWorkAtDoubleTextSize() = seasons(2f)

    private fun seasons(scale: Float) {
        var sent = 0
        val media = DiscoverMedia("series", "Testserien", "Serie", R.drawable.media_placeholder, true, mediaType = "tv")
        val state = ReelstackUiState(
            connections = listOf(ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", sessionCookie = true)),
            accounts = mapOf(ServiceKind.SEERR to ServiceAccount(ServiceKind.SEERR, "7", "Maya", permissions = 32)),
            requestDraft = RequestDraft(media, (1..18).map { RequestSeason(it, LocalizedText.raw("Sesong $it"), 8, 5) }, loading = false))
        rule.setContent { Television(scale) {
            RequestComposer(state, { _, _ -> error("Locked season selected") }, {}, { sent++ }, {}, {}, {})
        } }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("request-season-1").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        repeat(11) {
            rule.onNodeWithTag("request-season-${it + 1}").performKeyInput { pressKey(Key.DirectionDown) }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("request-season-12").assertIsFocused().assertIsDisplayed().assertIsNotEnabled()
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(0, sent)
        val title = rule.onNodeWithTag("request-title").fetchSemanticsNode().boundsInRoot
        val identity = rule.onNodeWithTag("request-identity").fetchSemanticsNode().boundsInRoot
        // Wide TV window: the actor shares the title's row rather than taking a second full row.
        if (rule.onRoot().fetchSemanticsNode().boundsInRoot.width / rule.density.density >= 600) {
            assertTrue(identity.left >= title.right)
            assertTrue(identity.top < title.bottom && identity.bottom > title.top)
        }
        screenshot("seasons-$scale")
    }

    @Test fun filtersExposeOneValueEachAndOnlyApplyTheConfirmedChoice() = filters(1f)
    @Test fun compactFiltersRemainUsableAtDoubleTextSize() = filters(2f)

    private fun filters(scale: Float) {
        var filters by mutableStateOf(LibraryFilters())
        rule.setContent { Television(scale) { Column(Modifier.padding(24.dp)) {
            LibraryFilterBar(filters, { filters = it })
        } } }
        rule.onNodeWithTag("library-filters").performClick()
        rule.onNodeWithTag("library-sort-YEAR").assertDoesNotExist()
        screenshot("compact-filters-$scale")
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("library-sort-choice").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(LibrarySort.TITLE, filters.sort)
        rule.onNodeWithTag("library-sort-YEAR").performClick()
        assertEquals(LibrarySort.YEAR, filters.sort)
        rule.onNodeWithTag("library-sort-YEAR").assertDoesNotExist()
        rule.onNodeWithTag("library-resolution-choice").performClick()
        rule.onNodeWithTag("library-resolution-UHD").performClick()
        assertEquals(LibraryResolution.UHD, filters.resolution)
        rule.onNodeWithTag("library-filter-reset").performClick()
        assertEquals(LibraryFilters(), filters)
    }

    @Test fun focusedFilterFrameFollowsTheVisibleFillWithoutAnEmptyGutter() {
        rule.setContent { Television { Column(Modifier.padding(24.dp)) {
            app.reelstack.ui.components.AppFilterRow(listOf("Alle · 107", "Påbyrja"), "Alle · 107",
                { it }, {}, optionTag = { "focus-$it" })
        } } }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        val chip = rule.onNodeWithTag("focus-Alle · 107")
        val bounds = chip.fetchSemanticsNode().boundsInRoot
        chip.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.waitForIdle()
        assertEquals(bounds, chip.fetchSemanticsNode().boundsInRoot)
        val pixels = chip.captureToImage().toPixelMap()
        // An external minimum touch region used to leave a dark gap inside the focus outline.
        val depth = (7 * rule.density.density).toInt()
        for (y in 1..depth) assertTrue("Empty band at pixel $y", pixels[pixels.width / 2, y].red > .3f)
        screenshot("focused-filter")
    }

    @Test fun releaseNotesRenderAsReadableBlocksAtDoubleTextSize() {
        rule.setContent { Television(2f) { Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            ReleaseNotes("# Ny versjon\n\n- **Betre** avspeling\n- `AC3`\n\nVanleg tekst.")
        } } }
        rule.onNodeWithText("Ny versjon").assertIsDisplayed()
        rule.onNodeWithText("Betre avspeling").assertIsDisplayed()
        rule.onNodeWithText("AC3").assertIsDisplayed()
        rule.onNodeWithText("**Betre** avspeling").assertDoesNotExist()
        screenshot("release-notes")
    }

    private fun screenshot(name: String) {
        rule.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
}
