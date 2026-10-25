package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toPixelMap
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlayerSeekLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val fixture = PlayerScreenState(busy = false, playing = false,
        positionMs = 60_000, durationMs = 600_000,
        chapters = listOf(PlaybackChapter("Preview chapter", 0, "https://example.test/chapter.jpg")))

    private fun show(onSeek: (Long) -> Unit = {}) {
        rule.setContent { ReelstackTheme {
            PlayerScreen(fixture, null, {}, {}, onSeek, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
        } }
    }

    private fun controls() = listOf("player-toggle", "player-rewind", "player-forward")
        .map { rule.onNodeWithTag(it).getUnclippedBoundsInRoot() }

    private fun bufferingScenario(size: DpSize, fontScale: Float = 1f) {
        rule.mainClock.autoAdvance = false
        var state by mutableStateOf(fixture.copy(playing = true, playWhenReady = true))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(density.density, fontScale)) {
                    ReelstackTheme {
                        PlayerScreen(state, null, {}, {}, { target ->
                            state = state.copy(busy = true, playing = false, positionMs = target)
                        }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(160)
        val before = controls()
        saveTvReview("player-ready-${size.width.value.toInt()}", rule.onNodeWithTag("jellyfin-player").captureToImage())
        saveTvReview("player-gesture-guide-${size.width.value.toInt()}-font$fontScale", rule.onNodeWithTag("jellyfin-player").captureToImage())
        saveTvReview("player-rewind-${size.width.value.toInt()}", rule.onNodeWithTag("player-rewind").captureToImage())
        saveTvReview("player-forward-${size.width.value.toInt()}", rule.onNodeWithTag("player-forward").captureToImage())
        rule.onNodeWithTag("player-forward").performClick()
        rule.mainClock.advanceTimeBy(160)
        rule.onNodeWithTag("player-buffering", useUnmergedTree = true).assertExists()
        assertEquals("Buffering must not move any transport button", before, controls())
        saveTvReview("player-buffering-${size.width.value.toInt()}", rule.onNodeWithTag("jellyfin-player").captureToImage())
        rule.runOnIdle { state = state.copy(busy = false, playing = true) }
        rule.mainClock.advanceTimeBy(160)
        assertEquals("Ready must not move any transport button", before, controls())
        rule.onNodeWithTag("player-rewind").performClick()
        rule.mainClock.advanceTimeBy(160)
        assertEquals("Repeated reverse seek must remain stable", before, controls())
        rule.runOnIdle { state = state.copy(busy = false, playing = true) }
        rule.mainClock.advanceTimeBy(2_400)
        assertEquals(before, controls())
    }

    @Test fun bufferingKeepsControlsFixedOnPortraitPhone() = bufferingScenario(DpSize(412.dp, 840.dp))

    @Test fun bufferingKeepsControlsFixedOnLandscapePhone() = bufferingScenario(DpSize(740.dp, 360.dp))
    @Test fun bufferingKeepsControlsFixedOnLandscapePhoneAtDoubleFontSize() = bufferingScenario(DpSize(740.dp, 360.dp), 2f)

    @Test fun skipIconsUseTheVideoSidesWithoutInstructionText() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
                PlayerScreen(fixture, null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
            } }
        }
        rule.onNodeWithText("Dobbeltrykk", substring = true).assertDoesNotExist()
        val viewport = rule.onNodeWithTag("jellyfin-player").fetchSemanticsNode().boundsInRoot
        val back = rule.onNodeWithTag("player-rewind").fetchSemanticsNode().boundsInRoot
        val forward = rule.onNodeWithTag("player-forward").fetchSemanticsNode().boundsInRoot
        assertTrue("Back belongs on the left side of the video", back.center.x < viewport.left + viewport.width * .35f)
        assertTrue("Forward belongs on the right side of the video", forward.center.x > viewport.left + viewport.width * .65f)
        saveTvReview("player-gesture-guide-font2", rule.onNodeWithTag("jellyfin-player").captureToImage())
    }

    @Test fun doubleTappingTheVideoSeeksAndBrieflyMarksTheSideWithoutText() {
        rule.mainClock.autoAdvance = false
        val targets = mutableListOf<Long>()
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
                PlayerScreen(fixture, null, {}, {}, { targets += it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
            } }
        }
        rule.mainClock.advanceTimeBy(160)
        val before = controls()
        val viewport = rule.onNodeWithTag("jellyfin-player").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("player-touch-surface").performTouchInput { doubleClick(Offset(width * .2f, height * .35f)) }
        rule.mainClock.advanceTimeBy(160)
        assertEquals(listOf(50_000L), targets)
        val left = rule.onNodeWithTag("player-touch-seek-area-back").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.left, left.left, 1f)
        assertEquals(viewport.height, left.height, 1f)
        assertEquals(viewport.width * .45f, left.width, 1f)
        assertEquals(before, controls())
        rule.onNodeWithText("−10 s").assertDoesNotExist()
        saveTvReview("player-gesture-feedback-back", rule.onNodeWithTag("jellyfin-player").captureToImage())
        rule.mainClock.advanceTimeBy(1_400)
        rule.onNodeWithTag("player-touch-seek-area-back").assertDoesNotExist()
        assertEquals(before, controls())
        rule.onNodeWithTag("player-touch-surface").performTouchInput { doubleClick(Offset(width * .8f, height * .35f)) }
        rule.mainClock.advanceTimeBy(160)
        assertEquals(listOf(50_000L, 60_000L), targets)
        val right = rule.onNodeWithTag("player-touch-seek-area-forward").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.right, right.right, 1f)
        assertEquals(viewport.height, right.height, 1f)
        assertEquals(viewport.width * .45f, right.width, 1f)
        rule.onNodeWithText("+10 s").assertDoesNotExist()
        saveTvReview("player-gesture-feedback-forward", rule.onNodeWithTag("jellyfin-player").captureToImage())
    }

    @Test fun doubleTapsSeekOnBothSidesAndAccumulateBeforePlayerStateCatchesUp() {
        val targets = mutableListOf<Long>()
        show { targets += it }
        rule.onNodeWithTag("player-touch-surface").performTouchInput {
            doubleClick(Offset(width * .8f, height * .35f))
        }
        assertEquals(listOf(70_000L), targets)
        rule.onNodeWithTag("player-touch-seek-feedback").assertIsDisplayed()
        rule.onNodeWithTag("player-touch-surface").performTouchInput {
            doubleClick(Offset(width * .8f, height * .35f))
        }
        assertEquals(listOf(70_000L, 80_000L), targets)
        saveTvReview("player-touch-forward", rule.onNodeWithTag("player-touch-seek-feedback").captureToImage())
        rule.onNodeWithTag("player-touch-surface").performTouchInput {
            doubleClick(Offset(width * .2f, height * .35f))
        }
        assertEquals(listOf(70_000L, 80_000L, 70_000L), targets)
        saveTvReview("player-touch-back", rule.onNodeWithTag("player-touch-seek-feedback").captureToImage())
    }

    @Test fun hiddenControlsAndSeekBufferingStillAllowDoubleTaps() {
        rule.mainClock.autoAdvance = false
        var state by mutableStateOf(fixture.copy(playing = true, playWhenReady = true))
        val targets = mutableListOf<Long>()
        rule.setContent { ReelstackTheme {
            PlayerScreen(state, null, {}, {}, { target ->
                targets += target
                state = state.copy(positionMs = target, busy = true, playing = false)
            }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
        } }
        rule.mainClock.advanceTimeBy(4_000)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
        repeat(2) {
            rule.onNodeWithTag("player-touch-surface").performTouchInput {
                doubleClick(Offset(width * .8f, height * .35f))
            }
            rule.mainClock.advanceTimeBy(160)
        }
        assertEquals(listOf(70_000L, 80_000L), targets)
        rule.mainClock.advanceTimeBy(160)
        rule.onNodeWithTag("player-toggle").assertDoesNotExist()
    }

    @Test fun centreButtonsAndUnavailableVideoNeverTriggerTheSeekGesture() {
        var state by mutableStateOf(fixture)
        val targets = mutableListOf<Long>()
        rule.setContent { ReelstackTheme {
            PlayerScreen(state, null, {}, {}, { targets += it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
        } }
        rule.onNodeWithTag("player-toggle").performTouchInput { doubleClick() }
        assertTrue(targets.isEmpty())
        for (unavailable in listOf(fixture.copy(awaitingResume = true), fixture.copy(error = "Testfeil"),
                fixture.copy(busy = true, durationMs = 0), fixture.copy(browsing = true))) {
            rule.runOnIdle { state = unavailable }
            rule.onNodeWithTag("player-touch-surface").performTouchInput {
                doubleClick(Offset(width * .8f, height * .35f))
            }
        }
        assertTrue(targets.isEmpty())
    }

    private fun tabletTimeline(size: DpSize) {
        var configuration = ""
        var sought = -1L
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                // ForcedSize changes constraints/density but retains the host screen dimensions.
                val config = android.content.res.Configuration(androidx.compose.ui.platform.LocalConfiguration.current).apply {
                    screenWidthDp = size.width.value.toInt()
                    screenHeightDp = size.height.value.toInt()
                    smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
                    orientation = if (screenWidthDp > screenHeightDp) android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        else android.content.res.Configuration.ORIENTATION_PORTRAIT
                }
                configuration = "${config.screenWidthDp} × ${config.screenHeightDp} dp"
                CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides config,
                    androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(density.density, 2f)) {
                    ReelstackTheme {
                        PlayerScreen(fixture, null, {}, {}, { sought = it }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
                    }
                }
            }
        }
        rule.onNodeWithTag("player-timeline").assertIsDisplayed()
        saveTvReview("player-tablet-${size.width.value.toInt()}-font2", rule.onNodeWithTag("jellyfin-player").captureToImage())
        // ForcedSize also transforms its subtree. Use the rendered viewport scale so these
        // assertions retain the requested dp contract on a smaller physical emulator.
        val viewport = rule.onNodeWithTag("jellyfin-player").fetchSemanticsNode().boundsInRoot
        val renderScale = viewport.width / size.width.value
        val bounds = rule.onNodeWithTag("player-timeline").fetchSemanticsNode().boundsInRoot
        assertTrue("Timeline height ${bounds.height / renderScale} dp at $configuration",
            bounds.height >= 64 * renderScale - 2)
        assertTrue("Timeline width ${bounds.width / renderScale} dp at $configuration",
            bounds.width >= (size.width.value - 64) * renderScale - 2)
        val handle = rule.onNodeWithTag("player-timeline-handle", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Tablet handle ${handle.width / renderScale} dp at $configuration",
            handle.width >= 28 * renderScale - 2)
        val track = rule.onNodeWithTag("player-timeline-track", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("The visible tablet track must be distinct from the generous hit area",
            track.height >= 10 * renderScale - 2)
        val before = controls()
        val transport = listOf("player-toggle", "player-rewind", "player-forward")
            .map { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        assertTrue("Tablet transport controls need an appropriate visual scale",
            transport.all { it.width >= 72 * renderScale - 2 && it.height >= 72 * renderScale - 2 })
        val tools = listOf("player-options")
            .map { rule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        assertTrue("Track controls retain at least 48 dp", tools.all { it.width >= 48 * renderScale - 2 })
        rule.onNodeWithTag("player-time-label").assertIsDisplayed()
        val restingTime = rule.onNodeWithTag("player-time-label").fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        // The position responds before the seek is committed, without adding a row or moving
        // transport controls. No preview picture or instructional copy is introduced.
        rule.onNodeWithTag("player-timeline").performTouchInput {
            down(Offset(width * .25f, height / 2f))
            moveTo(Offset(width * .6f, height / 2f), 300)
        }
        val draggingTime = rule.onNodeWithTag("player-time-label").fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        assertNotEquals(restingTime, draggingTime)
        assertEquals("Dragging must not commit a seek until finger release", -1L, sought)
        assertEquals("Scrub feedback must not move transport controls", before, controls())
        val activeTrack = rule.onNodeWithTag("player-timeline-track", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("The active track gives visible scrub feedback", activeTrack.height > track.height)
        rule.onNodeWithTag("player-timeline-preview").assertDoesNotExist()
        rule.onNodeWithText("Dobbeltrykk", substring = true).assertDoesNotExist()
        rule.onNodeWithTag("player-timeline").performTouchInput { up() }
        assertTrue("Release must commit the visible scrub position", sought in 300_000..400_000)
        rule.onNodeWithTag("player-time-label").assertTextEquals(draggingTime)
        assertEquals(before, controls())
        // The full height must seek, including well above and below the visible track.
        rule.onNodeWithTag("player-timeline").performTouchInput { click(Offset(width * .8f, height * .1f)) }
        assertTrue("Upper timeline target must seek", sought > 400_000)
        rule.onNodeWithTag("player-timeline").performTouchInput { click(Offset(width * .2f, height * .9f)) }
        assertTrue("Lower timeline target must seek", sought in 0..200_000)
    }

    @Test fun tabletLandscapeHasALargeTimelineAtDoubleFontSize() = tabletTimeline(DpSize(1280.dp, 800.dp))
    @Test fun tabletPortraitHasALargeTimelineAtDoubleFontSize() = tabletTimeline(DpSize(800.dp, 1280.dp))

    @Test fun pauseButtonIsCompactAndNeutral() {
        rule.setContent { ReelstackTheme {
            PlayerScreen(fixture.copy(playing = true, playWhenReady = true), null,
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
        } }
        val bounds = rule.onNodeWithTag("player-toggle").getUnclippedBoundsInRoot()
        assertEquals(56.dp, bounds.right - bounds.left)
        assertEquals(56.dp, bounds.bottom - bounds.top)
        val pixels = rule.onNodeWithTag("player-toggle").captureToImage().toPixelMap()
        var visiblePixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            if (c.alpha > .5f) {
                visiblePixels++
                assertTrue("Pause must use neutral colours", kotlin.math.abs(c.red - c.green) < .025f &&
                    kotlin.math.abs(c.green - c.blue) < .025f)
            }
        }
        assertTrue(visiblePixels > 20)
    }

    @Test fun skipButtonsNeverMoveWhileSeekingOrWhenFeedbackExpires() {
        var sought = -1L
        show { sought = it }
        val before = controls()
        rule.onNodeWithTag("player-forward").performClick()
        assertEquals(70_000L, sought)
        assertEquals(before, controls())
        rule.onNodeWithTag("player-rewind").performClick()
        assertEquals(60_000L, sought)
        assertEquals(before, controls())
        rule.onNodeWithTag("player-timeline-preview").assertDoesNotExist()
        rule.onNodeWithText("Preview chapter").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(2_400)
        assertEquals(before, controls())
    }

    @Test fun draggingTimelineKeepsTransportControlsFixed() {
        var sought = -1L
        show { sought = it }
        val before = controls()
        rule.onNodeWithTag("player-timeline").performTouchInput {
            down(Offset(width * .2f, height / 2f))
            moveTo(Offset(width * .8f, height / 2f), 300)
        }
        assertEquals(before, controls())
        rule.onNodeWithTag("player-timeline-preview").assertDoesNotExist()
        rule.onNodeWithText("Preview chapter").assertDoesNotExist()
        rule.onNodeWithTag("player-timeline").performTouchInput { up() }
        rule.runOnIdle { assertTrue(sought > 300_000) }
        assertEquals(before, controls())
    }

    @Test fun tvSeekingUsesOnlyTimeFeedbackEvenWithChapterImages() {
        rule.setContent { ReelstackTheme {
            Box(Modifier.fillMaxSize()) {
                TvPlaybackOverlay(fixture, false, 120_000, remember { FocusRequester() }, null,
                    {}, { _, _ -> }, null, {}, {}, remember { FocusRequester() }, {})
            }
        } }
        rule.onNodeWithTag("player-seek-feedback").assertIsDisplayed()
        rule.onNodeWithText("Preview chapter").assertDoesNotExist()
        rule.onNodeWithTag("player-timeline-preview").assertDoesNotExist()
    }
}
