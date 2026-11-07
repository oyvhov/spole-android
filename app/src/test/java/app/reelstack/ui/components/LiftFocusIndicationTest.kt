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
import app.reelstack.ui.ringAt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A focused LIFT card comes forward: it grows, rises, is lit and has a crisp edge, and the page around it stays
 * as it was. The glow Spole drew around the card (beta37) looked smudged on the Shield, and Stova's lift alone
 * left it hard to tell which card had focus there (2026-11-07).
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
        // The cards' own order: the lift before the clip, the illumination inside it.
        Box(Modifier.size(100.dp, 150.dp).testTag("card").focusLift { 1f }.clip(shape)
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
        assertEquals("no glow on the page around the focused card", pageBefore, pageAfter)
        assertTrue("the focused artwork is not brightened: $artBefore -> $artAfter", artAfter - artBefore >= 6)
        assertTrue("the brightening must stay light: $artBefore -> $artAfter", artAfter - artBefore <= 30)
    }

    @Test fun aFocusedLiftCardIsLitWithoutAGlow() = assertLit(motion = true)

    @Test fun reducedMotionKeepsTheBrightening() = assertLit(motion = false)

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

    @Test fun theLiftReadsFromTheSofa() {
        assertEquals(1.08f, LiftScale, 0f)
        assertEquals(2f, LiftRise.value, 0f)
    }

    /** A sharp light edge on the artwork itself, as Google TV draws: nothing soft spills onto the page. */
    @Test fun aFocusedLiftCardHasACrispEdge() {
        card(FocusStyle.LIFT, true)
        rule.waitForIdle()
        val box = bounds()
        val d = rule.density.density
        val before = rule.runOnIdle { app.reelstack.ui.drawnContent(rule.activity) }
        assertTrue("no edge before focus", !before.ringAt(1f, 75f, box, d))
        rule.onNodeWithTag("card").requestFocus()
        rule.waitForIdle()
        val after = rule.runOnIdle { app.reelstack.ui.drawnContent(rule.activity) }
        assertTrue("a light edge just inside the focused card", after.ringAt(1f, 75f, box, d))
        assertTrue("the edge is crisp: the art a few dp in is not lit by it", !after.ringAt(8f, 75f, box, d))
    }
}
