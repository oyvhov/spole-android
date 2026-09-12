package app.reelstack

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.asAndroidBitmap
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
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun nextEpisodeAppearsBeforeEndAndRemoteSelectDoesNotToggleCurrentVideo() {
        var next = 0
        var toggles = 0
        val state = androidx.compose.runtime.mutableStateOf(PlayerScreenState(busy = false, playing = true,
            positionMs = 1_730_000, durationMs = 1_800_000,
            nextEpisode = PlayableItem("next", "Testserie", "Episode", season = 2, episode = 3)))
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(state.value, null, {}, { toggles++ }, {}, {}, {}, {}, {}, {}, {}, {},
                onNextEpisode = { next++ }, onCancelNextEpisode = { state.value = state.value.copy(nextEpisodeDismissed = true) },
                isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("player-next-episode").assertDoesNotExist()
        rule.runOnIdle { state.value = state.value.copy(positionMs = 1_740_000) }
        capture("tv-polish-next-episode")
        rule.onNodeWithTag("player-next-play").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { assertEquals(1, next); assertEquals(0, toggles) }
        rule.onNodeWithTag("player-next-cancel").performClick()
        rule.onNodeWithTag("player-next-episode").assertDoesNotExist()
        rule.runOnIdle { state.value = state.value.copy(ended = true) }
        rule.onNodeWithTag("player-next-episode").assertDoesNotExist()
    }

    @Test fun nextEpisodeActionsFitAtDoubleTextSize() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
                PlayerScreen(PlayerScreenState(busy = false, ended = true, nextEpisodeCountdown = 12,
                    nextEpisode = PlayableItem("next", "Ei lang og lesbar serieoverskrift", "Episode", season = 2, episode = 3)),
                    null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
            } }
        }
        rule.onNodeWithTag("player-next-play").assertIsDisplayed()
        rule.onNodeWithTag("player-next-cancel").assertIsDisplayed()
        capture("tv-polish-next-episode-large")
    }

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
