package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlaybackOptionsUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var host: android.view.View
    private lateinit var inputMode: androidx.compose.ui.input.InputModeManager
    private fun remote(key: Int) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(key)
    private fun focused(tag: String) {
        rule.waitUntilAtLeastOneExists(hasTestTag(tag) and isFocused(), 5_000)
        rule.onNodeWithTag(tag).assertIsFocused()
    }
    private fun startRemoteAtTimeline() {
        // Android windows share the platform touch mode. Changing only Compose's test host does
        // not establish keyboard input in a subsequently opened dialog on a phone.
        rule.waitUntil(5_000) { host.hasWindowFocus() }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        rule.waitUntil(5_000) { inputMode.inputMode == androidx.compose.ui.input.InputMode.Keyboard }
        rule.onNodeWithTag("player-timeline")
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
    }
    private fun openWithRemote() {
        startRemoteAtTimeline()
        remote(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
        focused("player-options")
        remote(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
        focused("player-option-speed")
    }
    @Test fun remoteOpensOneMenuAdjustsSpeedAndRestoresTheTrigger() {
        var adjustment by mutableStateOf(PlaybackAdjustments())
        rule.setContent { host = androidx.compose.ui.platform.LocalView.current
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, durationMs = 60_000), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true,
                adjustments = adjustment, onAdjust = { adjustment = it })
        } }
        listOf("player-audio", "player-subtitles", "player-quality", "player-stats").forEach { rule.onNodeWithTag(it).assertDoesNotExist() }
        openWithRemote()
        saveTvReview("player-options-tv", rule.onNodeWithTag("choice-dialog").captureToImage())
        rule.onNodeWithTag("player-option-speed").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("1,25×").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1.25f, adjustment.speed) }
        rule.onNodeWithTag("choice-dialog").assertDoesNotExist()
        focused("player-options")
    }

    @Test fun doubleFontMenuKeepsItsViewportAcrossDepthAndDelaySteps() {
        var adjustment by mutableStateOf(PlaybackAdjustments())
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, durationMs = 60_000), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true,
                adjustments = adjustment, onAdjust = { adjustment = it })
        } } }
        rule.onNodeWithTag("player-options").performClick()
        val bounds = rule.onNodeWithTag("choice-dialog").fetchSemanticsNode().boundsInRoot
        saveTvReview("player-options-tv-font2", rule.onNodeWithTag("choice-dialog").captureToImage())
        rule.onNodeWithTag("player-option-audio_delay").performScrollTo().performClick()
        assertEquals(bounds, rule.onNodeWithTag("choice-dialog").fetchSemanticsNode().boundsInRoot)
        saveTvReview("player-delay-tv-font2", rule.onNodeWithTag("choice-dialog").captureToImage())
        rule.onNodeWithTag("player-delay-later").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(100, adjustment.audioDelayMs) }
        rule.onNodeWithTag("player-delay-reset").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(0, adjustment.audioDelayMs) }
        assertEquals(bounds, rule.onNodeWithTag("choice-dialog").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun remoteBackUnwindsDepthAndCloseAlwaysDismissesTheWholeMenu() {
        rule.setContent { host = androidx.compose.ui.platform.LocalView.current
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, durationMs = 60_000), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        openWithRemote()
        remote(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
        focused("player-option-audio_delay")
        remote(android.view.KeyEvent.KEYCODE_DPAD_CENTER)
        focused("player-delay-earlier")
        remote(android.view.KeyEvent.KEYCODE_BACK)
        focused("player-option-audio_delay")
        rule.onNodeWithTag("player-option-audio_delay").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithContentDescription("Lukk").performClick()
        rule.onNodeWithTag("choice-dialog").assertDoesNotExist()
        focused("player-options")
    }
}
