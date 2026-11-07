package app.reelstack.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The page behind the expanded television menu is dimmed, and the dimming never takes part in focus. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class RailScrimTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun theScrimIsThereWhileTheMenuIsExpandedAndGoneWhenItCollapses() {
        val expanded = mutableStateOf(true)
        rule.setContent { ReelstackTheme { Box(Modifier.fillMaxSize()) { RailScrim(expanded.value, Modifier.fillMaxSize()) } } }
        rule.onNodeWithTag("rail-scrim").assertExists()
        rule.runOnIdle { expanded.value = false }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("rail-scrim").assertDoesNotExist()
    }

    @Test fun theScrimStaysOutOfTheFocusOrder() {
        rule.setContent { ReelstackTheme { Box(Modifier.fillMaxSize()) {
            Box(Modifier.size(48.dp).testTag("page-button").focusable())
            RailScrim(true, Modifier.fillMaxSize())
        } } }
        rule.onNodeWithTag("page-button").requestFocus()
        rule.onNodeWithTag("page-button").assertIsFocused()
        rule.onNodeWithTag("rail-scrim").assertExists()
        rule.onNodeWithTag("rail-scrim").assertHasNoClickAction()
        rule.onNodeWithTag("rail-scrim").assert(isNotFocusable())
    }
}
