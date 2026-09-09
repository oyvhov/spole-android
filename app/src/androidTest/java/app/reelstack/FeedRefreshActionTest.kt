package app.reelstack

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.input.key.Key
import app.reelstack.ui.components.FeedRefreshAction
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FeedRefreshActionTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeTextKeepsRefreshActionVisible() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                androidx.compose.ui.unit.DpSize(androidx.compose.ui.unit.Dp(320f), androidx.compose.ui.unit.Dp(640f)))) {
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(androidx.compose.ui.platform.LocalDensity.current.density, 2f)) {
                    ReelstackTheme { FeedRefreshAction("Sist oppdatert 12:30", false, {}) }
                }
            }
        }
        rule.onNodeWithTag("feed-refresh").assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithText("Oppdater biblioteket").assertIsDisplayed()
    }

    @Test fun refreshKeepsRemoteFocusAndPreventsRepeatedSubmission() {
        val refreshing = mutableStateOf(false)
        var calls = 0
        lateinit var input: androidx.compose.ui.input.InputModeManager
        rule.setContent {
            input = androidx.compose.ui.platform.LocalInputModeManager.current
            ReelstackTheme {
                FeedRefreshAction("Status", refreshing.value, { calls++; refreshing.value = true })
            }
        }
        val action = rule.onNodeWithTag("feed-refresh")
        rule.runOnIdle { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        action.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        action.assertIsFocused()
        action.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(1, calls) }
        action.assertIsFocused().assertIsDisplayed()
        action.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { assertEquals(1, calls); refreshing.value = false }
        action.assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { assertEquals(2, calls) }
    }
}
