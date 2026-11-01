package app.reelstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import app.reelstack.ui.AppTab
import app.reelstack.ui.TvTopBar
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.ui.theme.Ink
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvTopBarUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var input: InputModeManager

    @Test fun emptyPinsGiveLibraryAndFocusAloneNeverNavigates() {
        var opened: AppTab? = null
        var downs = 0
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme { Box(Modifier.fillMaxSize()) {
                TvTopBar(AppTab.HOME, { opened = it }, emptyList(), null, {}, true, false, {},
                    null, null, {}, true, { downs++ })
            } }
        }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("tv-top-HOME").assertIsSelected()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("tv-top-LIBRARY").assertIsFocused()
        assertNull(opened)
        rule.onNodeWithTag("tv-top-LIBRARY").performKeyInput { pressKey(Key.Enter) }
        assertEquals(AppTab.LIBRARY, opened)
        rule.onNodeWithTag("tv-top-LIBRARY").performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(1, downs)
        rule.onNodeWithTag("tv-top-SETTINGS").assertDoesNotExist()
    }

    @Test fun pinnedDestinationsKeepTheirOrderAndOpenTheirOwnId() {
        var selected: String? = null
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme { Box(Modifier.fillMaxSize()) {
                TvTopBar(AppTab.LIBRARY, {}, listOf("winter" to "Vinter", "films" to "Filmar"), "winter",
                    { selected = it }, false, false, {}, null, null, {}, true, {})
            } }
        }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("tv-top-library-winter").assertIsSelected()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("tv-top-library-films").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals("films", selected)
        rule.onNodeWithTag("tv-top-LIBRARY").assertDoesNotExist()
        rule.onNodeWithTag("tv-top-DISCOVER").assertDoesNotExist()
    }

    @Test fun aSheetHidesTheMenuAndRemovesItsFocusableControls() {
        val visible = mutableStateOf(true)
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme { Box(Modifier.fillMaxSize()) {
                TvTopBar(AppTab.HOME, {}, emptyList(), null, {}, true, false, {},
                    null, null, {}, visible.value, {})
            } }
        }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("tv-top-HOME").performSemanticsAction(SemanticsActions.RequestFocus) { it() }.assertIsFocused()
        rule.runOnIdle { visible.value = false }
        rule.onNodeWithTag("tv-top-HOME").assertDoesNotExist()
        rule.onNodeWithTag("tv-top-account").assertDoesNotExist()
        rule.onAllNodes(isFocused()).assertCountEquals(0)
        rule.runOnIdle { visible.value = true }
        rule.onNodeWithTag("tv-top-HOME").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun searchHasItsOwnSelectedTabAndLargeTextKeepsTheAccountVisible() {
        rule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides Density(density.density, 2f)) {
                ReelstackTheme { Box(Modifier.fillMaxSize().background(Ink)) {
                    TvTopBar(AppTab.HOME, {}, (1..12).map { "$it" to "Bibliotek $it" }, null, {}, true, true, {},
                        null, null, {}, true, {})
                } }
            }
        }
        rule.onNodeWithTag("tv-top-search").assertIsSelected().performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("tv-top-HOME").assertIsNotSelected()
        rule.onNodeWithTag("tv-top-account").assertIsDisplayed()
        val account = rule.onNodeWithTag("tv-top-account").getUnclippedBoundsInRoot()
        val search = rule.onNodeWithTag("tv-top-search").getUnclippedBoundsInRoot()
        assertTrue("Scrollable destinations must not run underneath the profile", search.right <= account.left)
        saveTvReview("top-menu-large-text", rule.onRoot().captureToImage())
    }
}
