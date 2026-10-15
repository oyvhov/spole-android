package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.toPixelMap
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JellyfinPlayerUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var inputMode: androidx.compose.ui.input.InputModeManager
    private lateinit var backDispatcher: androidx.activity.OnBackPressedDispatcher
    private fun screen(state: PlayerScreenState, scale: Float = 1f, close: () -> Unit = {}, subtitle: (Int) -> Unit = {},
        choose: (PlayableItem) -> Unit = {}, player: androidx.media3.common.Player? = null, television: Boolean = false) {
        rule.setContent { val density = LocalDensity.current
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            backDispatcher = checkNotNull(androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current)
                .onBackPressedDispatcher
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                ReelstackTheme { PlayerScreen(state, player, close, {}, {}, {}, choose, {}, {}, subtitle, {}, {}, isTelevision = television) }
            }
        }
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun decoderDiagnosticsScrollAtDoubleFontSize() {
        lateinit var player: androidx.media3.exoplayer.ExoPlayer
        rule.runOnIdle { player = androidx.media3.exoplayer.ExoPlayer.Builder(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext).build() }
        try {
            screen(PlayerScreenState(busy = false, durationMs = 20_000,
                audioDecoder = "ffmpeg6.1.4-eac3", videoDecoder = "c2.test.avc.decoder", audioUnderruns = 3,
                advertisedAudio = "aac 8 · ac3 8 · eac3 8 · dts 8 · truehd 8 · flac 8"),
                scale = 2f, player = player)
            // The statistics action is pinned in the player chrome, not in the document below it.
            // Asking Compose to scroll it therefore asserts an obsolete layout contract.
            rule.onNodeWithTag("player-stats").performClick()
            rule.onNodeWithTag("player-stats-overlay").assertIsDisplayed()
            val info = rule.onNodeWithTag("player-stats").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val panel = rule.onNodeWithTag("player-stats-overlay").fetchSemanticsNode().boundsInRoot
            assertTrue("Statistics must leave the information action unobscured", panel.top > info.bottom)
            val closeBefore = rule.onNodeWithTag("player-stats-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            rule.onNodeWithText("Lydavbrot · 3").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText("ffmpeg6.1.4-eac3", substring = true).performScrollTo().assertIsDisplayed()
            assertEquals("The close action stays pinned while diagnostics scroll", closeBefore,
                rule.onNodeWithTag("player-stats-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot)
            rule.onNodeWithTag("player-stats-close").performTouchInput { click() }
            rule.onNodeWithTag("player-stats-overlay").assertDoesNotExist()
            rule.onNodeWithTag("player-stats").performTouchInput { click() }
            rule.onNodeWithTag("player-stats-overlay").assertIsDisplayed()
            rule.onNodeWithTag("player-stats").performTouchInput { click() }
            rule.onNodeWithTag("player-stats-overlay").assertDoesNotExist()
        } finally { rule.runOnIdle { player.release() } }
    }
    private fun back() {
        // Exercise the registered Android callback on its owning thread. Injecting a legacy
        // BACK key into this standalone host does not synchronise API 36's predictive Back.
        rule.runOnIdle { backDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun backClosesStatisticsBeforeLeavingTouchPlayback() {
        lateinit var player: androidx.media3.exoplayer.ExoPlayer
        rule.runOnIdle { player = androidx.media3.exoplayer.ExoPlayer.Builder(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext).build() }
        var exits = 0
        try {
            screen(PlayerScreenState(busy = false, durationMs = 60_000),
                player = player, close = { exits++ })
            rule.onNodeWithTag("player-stats").performClick()
            rule.onNodeWithTag("player-stats-overlay").assertIsDisplayed()
            back()
            rule.onNodeWithTag("player-stats-overlay").assertDoesNotExist()
            rule.onNodeWithTag("player-toggle").assertIsDisplayed()
            assertEquals(0, exits)
            back()
            assertEquals(1, exits)
        } finally { rule.runOnIdle { player.release() } }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun televisionBackClosesStatisticsThenControlsThenPlayback() {
        lateinit var player: androidx.media3.exoplayer.ExoPlayer
        rule.runOnIdle { player = androidx.media3.exoplayer.ExoPlayer.Builder(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext).build() }
        var exits = 0
        try {
            screen(PlayerScreenState(busy = false, durationMs = 60_000),
                player = player, close = { exits++ }, television = true)
            rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            rule.onNodeWithTag("player-stats").performClick()
            rule.onNodeWithTag("player-stats-close").assertIsDisplayed().assertIsFocused()
            back()
            rule.onNodeWithTag("player-stats-overlay").assertDoesNotExist()
            rule.onNodeWithTag("player-timeline").assertIsDisplayed()
            assertEquals(0, exits)
            back()
            rule.onNodeWithTag("player-timeline").assertDoesNotExist()
            assertEquals(0, exits)
            back()
            assertEquals(1, exits)
        } finally { rule.runOnIdle { player.release() } }
    }
    @Test fun missingSubtitleWarningIsReadableAtDoubleFontSize() {
        screen(PlayerScreenState(busy = false, durationMs = 20_000, subtitleUnavailable = true), scale = 2f)
        rule.onNodeWithText("Underteksten kunne ikkje lastast.", substring = true).performScrollTo().assertIsDisplayed()
    }
    @Test fun pictureCanFillTheScreenAndReturnToUncroppedFit() {
        screen(PlayerScreenState(busy=false,durationMs=20000))
        rule.onNodeWithContentDescription("Fyll skjermen").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Heile biletet").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Fyll skjermen").assertIsDisplayed()
        rule.onNodeWithTag("player-close").assertIsDisplayed()
    }
    @Test fun closeIsAvailableWhileVideoLoads() {
        var closed=false
        screen(PlayerScreenState(),close={closed=true})
        rule.onNodeWithTag("player-close").assertIsDisplayed().performClick()
        assertTrue(closed)
        rule.onNodeWithTag("player-toggle").assertIsNotEnabled()
    }
    @Test fun hiddenControlsAppearOnTouchDownWithoutWaitingForFingerRelease() {
        rule.mainClock.autoAdvance = false
        screen(PlayerScreenState(busy=false,playing=true,durationMs=20000))
        rule.mainClock.advanceTimeBy(4_000)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        rule.onNodeWithTag("player-close").assertDoesNotExist()
        rule.onNodeWithTag("player-touch-surface").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(width * .75f, height * .4f))
        }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("player-toggle").assertExists()
        rule.onNodeWithTag("player-touch-surface").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(120)
        rule.onNodeWithTag("player-toggle").assertIsDisplayed().assertIsEnabled()
        rule.onNodeWithTag("player-close").assertIsDisplayed()
        rule.mainClock.autoAdvance = true
    }
    @Test fun backStaysPinnedAfterScrollingLargeTextControls() {
        var closed = false
        screen(PlayerScreenState(title="Ein lang filmtittel med fleire ord",busy=false,durationMs=20000),
            scale=2f, close={ closed=true })
        val before = rule.onNodeWithTag("player-close").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("player-quality").performScrollTo()
        val after = rule.onNodeWithTag("player-close").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(before,after)
        rule.onNodeWithTag("player-close").performTouchInput { click() }
        assertTrue(closed)
    }
    @Test fun textChoiceAndOffAreExplicit() {
        var selected=99
        screen(PlayerScreenState(busy=false,subtitles=listOf(PlaybackTrack(2,"Norsk", "nor",true)),subtitleIndex=2),subtitle={selected=it})
        rule.onNodeWithTag("player-subtitles").performScrollTo().performClick()
        rule.onNodeWithText("Norsk").assertIsDisplayed()
        rule.onNodeWithText("Av",substring=false).performClick()
        assertEquals(-1,selected)
    }
    @Test fun audioChoicesKeepTrackIdentityWithoutHandlerNoise() {
        var selected = -1
        rule.setContent { ReelstackTheme {
            PlayerScreen(PlayerScreenState(busy = false, durationMs = 60_000,
                audio = listOf(PlaybackTrack(3, "English · AAC 2.0 · Audio Media Handler", "eng"),
                    PlaybackTrack(8, "English · AC3 5.1 · Commentary", "eng"))),
                null, {}, {}, {}, {}, {}, {}, { selected = it }, {}, {}, {}, isTelevision = false)
        } }
        rule.onNodeWithTag("player-audio").performScrollTo().performClick()
        rule.onNodeWithText("Audio Media Handler", substring = true).assertDoesNotExist()
        rule.onNodeWithText("AAC 2.0", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Commentary", substring = true).assertIsDisplayed().performClick()
        assertEquals(8, selected)
    }
    @Test fun largeTextKeepsCloseAndQualityReachable() {
        screen(PlayerScreenState(title="Ein lang filmtittel som framleis skal vere lesbar",busy=false,durationMs=20000),scale=2f)
        rule.onNodeWithTag("player-close").assertIsDisplayed()
        rule.onNodeWithTag("player-quality").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Automatisk").assertIsDisplayed()
    }
    @Test fun episodesAreExplicitAndShowPersonalResume() {
        var selected=""
        val episode=PlayableItem("ep","Testserie","Episode","S01 E02 · Ny dag",resumeMs=5000)
        screen(PlayerScreenState(busy=false,browsing=true,choices=listOf(episode)),choose={selected=it.id})
        rule.onNodeWithText("Hald fram frå 0:05").assertIsDisplayed()
        rule.onNodeWithText(episode.subtitle).performClick()
        assertEquals("ep",selected)
    }
    @Test fun errorsHaveRetryAndExternalExit() {
        screen(PlayerScreenState(busy=false,error="Fekk ikkje starta videoen."))
        rule.onNodeWithText("Prøv igjen").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Opne i Jellyfin").performScrollTo().assertIsDisplayed()
    }
    @Test fun playerTitleIsLightOnTheBlackVideoSurface() {
        screen(PlayerScreenState(title="Spole testtittel",busy=false))
        val pixels=rule.onNodeWithText("Spole testtittel").captureToImage().toPixelMap()
        var brightPixels=0
        for(y in 0 until pixels.height) for(x in 0 until pixels.width) {
            val color=pixels[x,y]
            if(color.red>.8f && color.green>.8f && color.blue>.8f) brightPixels++
        }
        assertTrue("Player title must not inherit black text",brightPixels>20)
    }
}
