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
            nextEpisode = PlayableItem("next", "Testserie", "Episode", season = 2, episode = 3,
                artworkUrl = "android.resource://app.reelstack.debug/drawable/session_still")))
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(state.value, null, {}, { toggles++ }, {}, {}, {}, {}, {}, {}, {}, {},
                onNextEpisode = { next++ }, onCancelNextEpisode = { state.value = state.value.copy(nextEpisodeDismissed = true) },
                isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("player-next-episode").assertDoesNotExist()
        rule.runOnIdle { state.value = state.value.copy(positionMs = 1_740_000, nextEpisodeCountdown = 6) }
        rule.onNodeWithTag("player-next-play").assertIsDisplayed().assertIsFocused()
        rule.onNodeWithTag("player-next-artwork").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(4000)
        rule.onNodeWithTag("player-next-play").assertIsFocused()
        capture("tv-polish-next-episode")
        rule.onNodeWithTag("player-next-play").performKeyInput { pressKey(Key.DirectionCenter) }
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
        rule.onNodeWithTag("player-next-progress").assertIsDisplayed()
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

    @Test fun backClosesPausedOsdAndTrackDialogInOnePress() {
        var exits = 0
        rule.setContent { ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = false, durationMs = 60_000), null,
                { exits++ }, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.onNodeWithTag("player-quality").performClick()
        androidx.test.espresso.Espresso.pressBack()
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        assertEquals(0, exits)
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
        rule.mainClock.advanceTimeBy(240)
        assertEquals(0L, position)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionRight) }
        rule.mainClock.advanceTimeBy(240)
        assertEquals(20_000L, position)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("player-seek-feedback").assertIsDisplayed()
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionDown) }
        rule.mainClock.advanceTimeBy(160)
        rule.onNodeWithTag("player-toggle").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(20_000L, position) // focus movement is not an extra seek
        rule.onNodeWithTag("player-toggle").assertIsNotFocused()
        rule.mainClock.autoAdvance = true
    }

    @Test fun remoteReachesTimelineAndToolsAndReturnsToTransport() {
        var position = 0L
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = false, positionMs = 30_000, durationMs = 120_000,
                audio = listOf(PlaybackTrack(1, "Norsk", "nor")), subtitles = listOf(PlaybackTrack(2, "Nynorsk", "nno", true))), null,
                {}, {}, { position = it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("player-toggle").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("player-timeline").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { assertEquals(40_000L, position) }
        capture("tv-pass2-timeline")
        rule.onNodeWithTag("player-timeline").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("player-audio").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("player-subtitles").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("player-timeline").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("player-toggle").assertIsFocused()
    }

    @Test fun transportAndToolsStayVisibleAtDoubleTextSize() {
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = false, title = "Ei lang serieoverskrift",
                positionMs = 30_000, durationMs = 120_000), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } } }
        listOf("player-toggle", "player-timeline", "player-audio", "player-subtitles", "player-quality", "player-frame-mode")
            .forEach { rule.onNodeWithTag(it).assertIsDisplayed() }
        capture("tv-pass2-osd-large")
    }

    @Test fun bufferingDuringRemoteSeekKeepsControlsHiddenAndAcceptsAnotherSeek() {
        val state = androidx.compose.runtime.mutableStateOf(PlayerScreenState(busy = false, playing = true,
            positionMs = 30_000, durationMs = 120_000))
        var target = 0L
        rule.mainClock.autoAdvance = false
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(state.value, null, {}, {}, { target = it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.mainClock.advanceTimeBy(4000)
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionRight) }
        rule.mainClock.advanceTimeBy(240)
        rule.runOnIdle { state.value = state.value.copy(busy = true, playing = false, playWhenReady = true, positionMs = target) }
        rule.mainClock.advanceTimeBy(3000)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("jellyfin-player").performKeyInput { pressKey(Key.DirectionRight) }
        rule.mainClock.advanceTimeBy(240)
        assertEquals(50_000L, target)
        rule.runOnIdle { state.value = state.value.copy(busy = false, playing = true) }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.mainClock.autoAdvance = true
    }

    @Test fun finishedMovieKeepsReplayControlsVisible() {
        rule.setContent { ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, playing = false, ended = true,
                positionMs = 120_000, durationMs = 120_000), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.onNodeWithTag("player-toggle").assertIsDisplayed()
        rule.onNodeWithTag("player-timeline").assertIsDisplayed()
        rule.onNodeWithTag("player-next-episode").assertDoesNotExist()
    }

    @Test fun timelineKeepsFocusAcrossSeekBuffering() {
        val state = androidx.compose.runtime.mutableStateOf(PlayerScreenState(busy = false, playing = false,
            positionMs = 30_000, durationMs = 120_000))
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current; ReelstackTheme {
            PlayerScreen(state.value, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = true)
        } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("player-toggle").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("player-timeline").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        rule.runOnIdle { state.value = state.value.copy(busy = true) }
        rule.onNodeWithTag("player-timeline").assertIsFocused()
        rule.runOnIdle { state.value = state.value.copy(busy = false) }
        rule.onNodeWithTag("player-timeline").assertIsFocused()
    }
}
