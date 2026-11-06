package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.FocusStyle
import app.reelstack.data.model.Personalization
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A focused LIFT card must read from the sofa: a glow on the page around it and a light
 * brightening of its own artwork, and neither may need motion.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class LiftFocusIndicationTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun card(style: FocusStyle, motion: Boolean) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        // Inside the theme, which provides both of these itself.
        ReelstackTheme {
            CompositionLocalProvider(LocalMotionEnabled provides motion, LocalPersonalization provides Personalization(focusStyle = style)) {
                Box(Modifier.background(Color.Black).padding(60.dp)) {
                    // Whatever comes first takes the initial focus; the card must start unfocused.
                    Box(Modifier.size(1.dp).focusable())
                    Card()
                }
            }
        }
    }

    @Composable private fun Card() {
        val source = remember { MutableInteractionSource() }
        val shape = RoundedCornerShape(12.dp)
        // The cards' own order: the shadow before the clip, the illumination inside it.
        Box(Modifier.size(100.dp, 150.dp).testTag("card").liftShadow(source, shape).clip(shape)
            .background(Color(0xFF404040)).focusOutline(source, shape, artwork = true).focusable(interactionSource = source))
    }

    private fun red(xDp: Float, yDp: Float, origin: Rect): Int {
        val bitmap = rule.runOnIdle { app.reelstack.ui.drawnContent(rule.activity) }
        val d = rule.density.density
        return android.graphics.Color.red(bitmap.getPixel((origin.left + xDp * d).toInt(), (origin.top + yDp * d).toInt()))
    }

    private fun bounds() = rule.onNodeWithTag("card").fetchSemanticsNode().boundsInRoot

    private fun assertLit(motion: Boolean) {
        card(FocusStyle.LIFT, motion)
        rule.waitForIdle()
        val box = bounds()
        // 5 dp left of the card, halfway down: page, not artwork.
        val pageBefore = red(-5f, 75f, box)
        val artBefore = red(50f, 75f, box)
        rule.onNodeWithTag("card").requestFocus()
        rule.waitForIdle()
        val pageAfter = red(-5f, 75f, box)
        val artAfter = red(50f, 75f, box)
        assertEquals("the page is black before focus", 0, pageBefore)
        assertTrue("no glow on the page around the focused card: $pageAfter", pageAfter >= 12)
        assertTrue("the focused artwork is not brightened: $artBefore -> $artAfter", artAfter - artBefore >= 6)
        assertTrue("the brightening must stay light: $artBefore -> $artAfter", artAfter - artBefore <= 30)
    }

    @Test fun aFocusedLiftCardGlowsAndIsBrightened() = assertLit(motion = true)

    @Test fun reducedMotionKeepsTheGlowAndTheBrightening() = assertLit(motion = false)

    @Test fun otherStylesAreNotBrightened() {
        card(FocusStyle.WHITE, true)
        rule.waitForIdle()
        val box = bounds()
        val before = red(50f, 75f, box)
        rule.onNodeWithTag("card").requestFocus()
        rule.waitForIdle()
        // WHITE has its ring; the lift's light on the artwork is not part of it.
        assertEquals("WHITE must keep its artwork as it was", before, red(50f, 75f, box))
    }

    @Test fun theLiftIsBigEnoughToSeeFromTheSofa() {
        assertEquals(1.09f, LiftScale, 0f)
    }
}
