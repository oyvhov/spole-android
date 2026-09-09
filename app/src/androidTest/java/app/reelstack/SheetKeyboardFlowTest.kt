package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SheetKeyboardFlowTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var input: InputModeManager
    private var open by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private val detail = ContentDetails("keyboard", "Ein film", "Seerr", "Film",
        artworkRes = R.drawable.media_placeholder, mediaType = "movie", overview = "Ei lang historie. ".repeat(100))

    private fun host(keyboard: Boolean = true) {
        // Android cannot enter touch mode through InputModeManager.requestInputMode on all APIs.
        // Set the actual window mode before creating the host, rather than assuming it changed.
        if (!keyboard) androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        rule.setContent {
            input = LocalInputModeManager.current
            ReelstackTheme {
                Column {
                    Button(onClick = { open = true }, modifier = Modifier.testTag("origin")) { Text("Opne film") }
                    Button(onClick = {}, modifier = Modifier.testTag("neighbour")) { Text("Neste film") }
                }
                ReelstackSheets(ReelstackUiState(activeSheet = if (open) AppSheet.TitleDetails("keyboard") else null,
                    contentDetails = detail.copy(loading = loading)), null, { open = false },
                    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.runOnIdle { input.requestInputMode(if (keyboard) InputMode.Keyboard else InputMode.Touch) }
        rule.runOnIdle { assertEquals(if (keyboard) InputMode.Keyboard else InputMode.Touch, input.inputMode) }
        if (keyboard) rule.onNodeWithTag("origin").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        else rule.onNodeWithTag("origin").performClick()
    }

    @Test fun touchOpeningDoesNotForceKeyboardFocus() {
        host(keyboard = false)
        rule.onNodeWithTag("sheet-close").assertIsNotFocused().performClick()
        rule.waitUntil { !open }
    }

    @Test fun systemBackRestoresTheOriginToo() {
        host()
        androidx.test.espresso.Espresso.pressBack()
        rule.waitUntil { !open }
        rule.onNodeWithTag("origin").assertIsFocused()
    }

    @Test fun remoteOpeningStartsAtVisibleCloseAndLateMetadataDoesNotStealFocus() {
        host()
        rule.onNodeWithTag("sheet-close").assertIsFocused().assertIsDisplayed()
        val bounds = rule.onNodeWithTag("sheet-close").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { loading = false }
        rule.onNodeWithTag("sheet-close").assertIsFocused()
        assertEquals(bounds, rule.onNodeWithTag("sheet-close").fetchSemanticsNode().boundsInRoot)
        // Local synthetic screenshot for visual review, never private account content.
        val bitmap = rule.onNodeWithTag("sheet-viewport").captureToImage()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "sheet-flow-review.png").outputStream().use {
            bitmap.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun scrimCannotTakeKeyboardFocusButCanStillDismissByTouch() {
        host()
        rule.onNodeWithContentDescription("Lukk popupen")
            .performSemanticsAction(SemanticsActions.RequestFocus).assertIsNotFocused()
        rule.onNodeWithContentDescription("Lukk popupen").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(10f, 10f))
        }
        rule.waitUntil { !open }
    }

    @Test fun repeatedCloseReturnsToTheOriginalCardNotItsNeighbour() {
        host()
        repeat(3) { index ->
            rule.onNodeWithTag("sheet-close").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            rule.waitUntil { !open }
            rule.onNodeWithTag("origin").assertIsFocused()
            rule.onNodeWithTag("neighbour").assertIsNotFocused()
            if (index < 2) rule.onNodeWithTag("origin").performKeyInput { pressKey(Key.Enter) }
        }
    }

    @Test fun expandingLongOverviewKeepsItsControlInViewAndDoesNotJumpToTheEnd() {
        loading = false
        host()
        val toggle = rule.onNodeWithTag("overview-expand").performScrollTo()
        toggle.performSemanticsAction(SemanticsActions.RequestFocus)
        val before = toggle.fetchSemanticsNode().boundsInRoot
        toggle.performKeyInput { pressKey(Key.Enter) }
        toggle.assertIsFocused().assertIsDisplayed()
        val after = toggle.fetchSemanticsNode().boundsInRoot
        assertEquals("Expanding should keep the reading anchor", before.top, after.top, 2f)
        val scroll = rule.onNodeWithTag("detail-scroll").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("Must not jump past the newly revealed synopsis", scroll.value() < scroll.maxValue() / 2)
    }
}
