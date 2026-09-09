package app.reelstack

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvPlaybackControlsTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var inputMode: androidx.compose.ui.input.InputModeManager

    @Test fun backHidesPlayingControlsThenLeavesPlayer() {
        var exits = 0
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = true, durationMs = 60_000), null,
                { exits++ }, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(0, exits)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        androidx.test.espresso.Espresso.pressBack()
        rule.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun hiddenControlsRespondToCenterOnceAndRestorePlayFocus() {
        var toggles = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = true, durationMs = 60_000), null,
                {}, { toggles++ }, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.mainClock.advanceTimeBy(4000)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(160)
        assertEquals(1, toggles)
        rule.onNodeWithTag("player-toggle").assertIsDisplayed().assertIsFocused()
        rule.onNodeWithContentDescription("Roter skjermen").assertDoesNotExist()
        rule.mainClock.autoAdvance = true
    }

    @Test fun hiddenLeftSeeksWithoutGoingBelowZeroAndVisibleArrowsNavigate() {
        var position = -1L
        rule.mainClock.autoAdvance = false
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = true, positionMs = 5000, durationMs = 60_000), null,
                {}, {}, { position = it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.mainClock.advanceTimeBy(4000)
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionLeft) }
        rule.mainClock.advanceTimeBy(160)
        assertEquals(0L, position)
        rule.onNodeWithTag("player-toggle").assertIsFocused()
        rule.onNodeWithTag("player-toggle").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0L, position) // focus movement is not an extra seek
        rule.onNodeWithTag("player-toggle").assertIsNotFocused()
        rule.mainClock.autoAdvance = true
    }
}
