package app.reelstack

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.AppFilterRow
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FocusOutlineTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var input: InputModeManager

    @Test fun discoverDetailsAndRequestRemainSeparateKeyboardActions() {
        var details = 0
        var requests = 0
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme {
                app.reelstack.ui.screens.DiscoverScreen(
                    app.reelstack.ui.ReelstackUiState(discover = listOf(
                        app.reelstack.data.model.DiscoverMedia("test", "Testfilm", "2026",
                            R.drawable.media_placeholder, false, mediaType = "movie", seerrStatus = 1))),
                    PaddingValues(0.dp), {}, { requests++ }, { details++ })
            }
        }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        val cover = rule.onNodeWithTag("discover-cover-test").performScrollTo()
        cover.performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, details)
        assertEquals(0, requests)
        rule.onNodeWithText("Legg til").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, details)
        assertEquals(1, requests)
    }

    @Test fun focusChangesArtworkPixelsWithoutChangingBoundsOrActivatingIt() {
        var clicks = 0
        rule.setContent {
            input = LocalInputModeManager.current
            val interaction = remember { MutableInteractionSource() }
            ReelstackTheme {
                Box(Modifier.size(160.dp, 100.dp).background(Color.Black)
                    .focusOutline(interaction, RoundedCornerShape(12.dp))
                    .clickable(interactionSource = interaction, indication = null) { clicks++ }.testTag("art"))
            }
        }
        val node = rule.onNodeWithTag("art")
        val bounds = node.fetchSemanticsNode().boundsInRoot
        fun brightPixels(): Int {
            val pixels = node.captureToImage().toPixelMap()
            return (0 until pixels.width).sumOf { x -> (0 until pixels.height).count { y -> pixels[x,y].red > .7f } }
        }
        assertEquals(0, brightPixels())
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        node.performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.waitForIdle()
        assertTrue(brightPixels() > 100)
        assertEquals(bounds, node.fetchSemanticsNode().boundsInRoot)
        assertEquals(0, clicks)
        node.performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, clicks)
    }

    @Test fun filterFocusDoesNotChangeSelectionUntilConfirmed() {
        var selected by mutableStateOf("Filmar")
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme { AppFilterRow(listOf("Filmar", "Seriar"), selected, { it }, { selected = it }) }
        }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithText("Filmar").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithText("Seriar").assertIsFocused().assertIsNotSelected()
        assertEquals("Filmar", selected)
        rule.onNodeWithText("Seriar").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithText("Seriar").assertIsSelected()
    }
}
