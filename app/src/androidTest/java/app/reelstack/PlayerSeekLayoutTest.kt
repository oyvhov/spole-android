package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import app.reelstack.player.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

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
