package app.reelstack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.player.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PlaybackTouchGesturesTest {
    @get:Rule val rule = createComposeRule()
    private val axes = mutableListOf<PlaybackTouchAxis>()
    private val levels = mutableListOf<Float>()
    private val zooms = mutableListOf<Float>()
    private var finished = 0
    private var clicked = 0
    private fun screen(enabled: Boolean = true) = rule.setContent {
        Box(Modifier.size(320.dp, 240.dp).testTag("video")
            .playbackTouchGestures(enabled, { axes += it }, { levels += it }, { zooms += it }, { finished++ })) {
            Box(Modifier.align(Alignment.Center).size(64.dp).testTag("control").clickable { clicked++ })
        }
    }
    @Test fun verticalSidesChangeTheirOwnLevelAndHorizontalMovementDoesNot() {
        screen()
        rule.onNodeWithTag("video").performTouchInput { swipe(Offset(width * .15f, height * .8f), Offset(width * .15f, height * .2f)) }
        rule.runOnIdle { assertEquals(listOf(PlaybackTouchAxis.BRIGHTNESS), axes); assertTrue(levels.last() > .5f) }
        rule.onNodeWithTag("video").performTouchInput { swipe(Offset(width * .85f, height * .2f), Offset(width * .85f, height * .8f)) }
        rule.runOnIdle { assertEquals(PlaybackTouchAxis.VOLUME, axes.last()); assertTrue(levels.last() < -.5f) }
        rule.onNodeWithTag("video").performTouchInput { swipe(Offset(width * .1f, height * .1f), Offset(width * .8f, height * .1f)) }
        rule.runOnIdle { assertEquals(2, axes.size); assertEquals(2, finished) }
    }
    @Test fun pinchUsesZoomAndControlsKeepTheirOwnTouches() {
        screen()
        rule.onNodeWithTag("control").performTouchInput { click() }
        rule.onNodeWithTag("video").performTouchInput {
            down(0, Offset(width * .3f, height * .2f)); down(1, Offset(width * .7f, height * .2f))
            moveTo(0, Offset(width * .1f, height * .2f)); moveTo(1, Offset(width * .9f, height * .2f))
            up(0); up(1)
        }
        rule.runOnIdle { assertEquals(1, clicked); assertTrue(axes.isEmpty()); assertTrue(zooms.last() > 1.08f); assertEquals(1, finished) }
    }
    @Test fun disabledGesturesDoNotChangeAnything() {
        screen(false)
        rule.onNodeWithTag("video").performTouchInput { swipeUp() }
        rule.runOnIdle { assertTrue(axes.isEmpty()); assertTrue(levels.isEmpty()); assertTrue(zooms.isEmpty()) }
    }
    @Test fun losingTheGestureSurfaceFinishesTheFeedback() {
        val enabled = androidx.compose.runtime.mutableStateOf(true)
        rule.setContent { Box(Modifier.size(320.dp, 240.dp).testTag("video")
            .playbackTouchGestures(enabled.value, { axes += it }, { levels += it }, { zooms += it }, { finished++ })) }
        rule.onNodeWithTag("video").performTouchInput { down(Offset(width * .1f, height * .8f)); moveTo(Offset(width * .1f, height * .2f)) }
        rule.runOnIdle { assertEquals(1, axes.size); enabled.value = false }
        rule.runOnIdle { assertEquals(1, finished) }
        rule.onNodeWithTag("video").performTouchInput { up() }
    }
}
