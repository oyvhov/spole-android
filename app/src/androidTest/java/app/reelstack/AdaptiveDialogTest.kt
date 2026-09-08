package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.StableSheetDialog
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Run unchanged on phone and on the isolated emulator's tablet-size window. */
class AdaptiveDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test fun lateContentDoesNotMoveTheFrameOrPinnedClose() {
        var text by mutableStateOf("Loading")
        var closed by mutableStateOf(false)
        var widthDp = 0
        var heightDp = 0
        var density = 1f
        rule.setContent {
            density = LocalDensity.current.density
            widthDp = (LocalWindowInfo.current.containerSize.width / density).toInt()
            heightDp = (LocalWindowInfo.current.containerSize.height / density).toInt()
            ReelstackTheme {
                if (!closed) StableSheetDialog(true, { closed = true }) { _, _, close ->
                    Column(Modifier.fillMaxSize().padding(20.dp)) {
                        TextButton(close, Modifier.testTag("dialog-test-close")) { Text("Close fixture") }
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Text(text) }
                    }
                }
            }
        }
        val frame = rule.onNodeWithTag("adaptive-dialog").fetchSemanticsNode().boundsInRoot
        val close = rule.onNodeWithTag("dialog-test-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        if (widthDp >= 840 && heightDp >= 480) {
            assertEquals(720f, frame.width / density, 2f)
            assertTrue("Wide dialog needs breathing room above", frame.top / density > 24f)
        }
        rule.runOnIdle { text = "A late description with many lines. ".repeat(200) }
        assertEquals(frame, rule.onNodeWithTag("adaptive-dialog").fetchSemanticsNode().boundsInRoot)
        assertEquals(close, rule.onNodeWithTag("dialog-test-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithTag("dialog-test-close").performClick()
        rule.waitUntil { closed }
    }

    @Test fun keyboardKeepsTheCloseActionReachable() {
        var closed by mutableStateOf(false)
        rule.setContent { ReelstackTheme {
            if (!closed) StableSheetDialog(true, { closed = true }) { _, _, close ->
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    TextButton(close, Modifier.testTag("dialog-test-close")) { Text("Close fixture") }
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth().testTag("dialog-field"))
                }
            }
        } }
        rule.onNodeWithTag("dialog-field").performClick().performTextInput("Test")
        rule.onNodeWithTag("dialog-test-close").assertIsDisplayed().performClick()
        rule.waitUntil { closed }
    }
}
