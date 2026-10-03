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

    private fun bufferingScenario(size: DpSize) {
        rule.mainClock.autoAdvance = false
        var state by mutableStateOf(fixture.copy(playing = true, playWhenReady = true))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                ReelstackTheme {
                    PlayerScreen(state, null, {}, {}, { target ->
                        state = state.copy(busy = true, playing = false, positionMs = target)
                    }, {}, {}, {}, {}, {}, {}, {}, isTelevision = false)
                }
            }
        }
        rule.mainClock.advanceTimeBy(160)
        val before = controls()
        saveTvReview("player-ready-${size.width.value.toInt()}", rule.onNodeWithTag("jellyfin-player").captureToImage())
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
                    {}, { _, _ -> }, {}, {}, {}, null, {}, {})
            }
        } }
        rule.onNodeWithTag("player-seek-feedback").assertIsDisplayed()
        rule.onNodeWithText("Preview chapter").assertDoesNotExist()
        rule.onNodeWithTag("player-timeline-preview").assertDoesNotExist()
    }
}
