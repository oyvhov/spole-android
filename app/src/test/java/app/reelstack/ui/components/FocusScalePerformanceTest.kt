package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.LocalMotionEnabled
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Measure the shared focus lift at its caller, with a controlled clock and no network/images. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class FocusScalePerformanceTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val interaction = MutableInteractionSource()
    private val motion = mutableStateOf(true)
    private val probe = Probe()

    private class Probe {
        var compositions = 0
        var scale = 1f
        var size = IntSize.Zero
    }

    @Composable
    private fun Card() {
        val focused by interaction.collectIsFocusedAsState()
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFocusScale(focused, pressed)
        SideEffect { probe.compositions++ }
        Box(Modifier.size(100.dp).onSizeChanged { probe.size = it }.graphicsLayer {
            scaleX = scale
            scaleY = scale
            probe.scale = scaleX
        }.background(Color.White))
    }

    private fun content() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides motion.value) {
                Box(Modifier.padding(32.dp)) { Card() }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
    }

    private fun emit(event: Interaction) {
        runTest { interaction.emit(event) }
        rule.waitForIdle()
    }

    private fun frames(): List<Float> = (1..12).map {
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.runOnIdle { probe.scale }
    }

    @Test fun focusLiftChangesTheLayerWithoutRecomposingForEveryFrame() {
        content()
        val initialSize = rule.runOnIdle { probe.size }
        val before = rule.runOnIdle { probe.compositions }
        val focus = FocusInteraction.Focus()
        emit(focus)
        val samples = frames()
        val compositions = rule.runOnIdle { probe.compositions - before }
        println("focus-lift: compositions=$compositions, scale=${samples.last()}, frames=${samples.size}")
        assertTrue("The lift must still animate", samples.any { it > 1f && it < 1.035f })
        assertEquals(1.035f, samples.last(), .0001f)
        assertEquals(initialSize, rule.runOnIdle { probe.size })
        assertTrue("Focus state may recompose the card; animation frames must not: $compositions", compositions <= 2)

        emit(FocusInteraction.Unfocus(focus))
        assertEquals(1f, frames().last(), .0001f)
    }

    @Test fun pressOverridesFocusAndReducedMotionRestoresUnscaledContent() {
        content()
        emit(FocusInteraction.Focus())
        assertEquals(1.035f, frames().last(), .0001f)
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)
        assertEquals(.985f, frames().last(), .0001f)
        emit(PressInteraction.Cancel(press))
        assertEquals(1.035f, frames().last(), .0001f)

        rule.runOnIdle { motion.value = false }
        assertEquals(1f, frames().last(), .0001f)
        val stillPress = PressInteraction.Press(Offset.Zero)
        emit(stillPress)
        assertEquals(1f, frames().last(), .0001f)
        emit(PressInteraction.Release(stillPress))
        assertEquals(1f, frames().last(), .0001f)
    }
}
