package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.drawnContent
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * One bar for every place that shows how far through a title someone is.
 *
 * Five screens used to draw their own, at 3 dp or 4 dp, with a dark remainder or a white line or
 * nothing behind the fill. The rule they now share: 4 dp, flush with the artwork's bottom edge, a
 * white 30 % track across the *whole* length (the track is how long the title is, not what is
 * left of it) and the accent for the part already watched.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class WatchProgressBarTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val artworkWidth = 200.dp
    private val artworkHeight = 300.dp

    private var shown = false
    private var currentShare by mutableFloatStateOf(0f)

    /**
     * A bar at the bottom of a 200 x 300 dp piece of artwork, the way every card places it. The
     * content is set once; a later call only changes the share, because a rule takes one content.
     */
    private fun show(share: Float, round: Boolean = false) {
        currentShare = share
        if (!shown) {
            shown = true
            rule.setContent {
                ReelstackTheme {
                    Box(Modifier.size(artworkWidth, artworkHeight).background(Color.Black).testTag("art")) {
                        WatchProgressBar(currentShare, Modifier.align(Alignment.BottomStart).testTag("bar"), round = round)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun near(what: String, expected: Int, actual: Int, tolerance: Int = 2) =
        assertEquals(what, expected.toFloat(), actual.toFloat(), tolerance.toFloat())

    @Test fun theBarIsFourDpTall() {
        show(.4f)
        assertEquals(4f, bounds("bar").height.value, .01f)
    }

    @Test fun theTrackRunsTheWholeWidthOfTheArtwork() {
        show(.4f)
        assertEquals(artworkWidth.value, bounds("bar").width.value, .01f)
    }

    @Test fun theBarIsFlushWithTheBottomEdge() {
        show(.4f)
        assertEquals(bounds("art").bottom.value, bounds("bar").bottom.value, .01f)
        assertEquals(bounds("art").left.value, bounds("bar").left.value, .01f)
    }

    @Test fun theFillIsAsWideAsTheShare() {
        for (share in listOf(.1f, .25f, .5f, .875f)) {
            show(share)
            assertEquals("share $share", artworkWidth.value * share, bounds(WatchProgressFillTag).width.value, .5f)
            assertEquals(4f, bounds(WatchProgressFillTag).height.value, .01f)
        }
    }

    @Test fun aShareOutsideZeroToOneIsClamped() {
        show(1.7f)
        assertEquals(artworkWidth.value, bounds(WatchProgressFillTag).width.value, .5f)
        show(-.3f)
        assertEquals(0f, bounds(WatchProgressFillTag).width.value, .01f)
        show(Float.NaN)
        assertEquals(0f, bounds(WatchProgressFillTag).width.value, .01f)
    }

    @Test fun theTrackIsWhiteAtThirtyPercentAndTheFillIsTheAccent() {
        show(.5f)
        val image = rule.runOnIdle { drawnContent(rule.activity) }
        val density = rule.density.density
        val bar = bounds("bar")
        val y = ((bar.top.value + bar.height.value / 2) * density).toInt()
        val fill = image.getPixel((bar.left.value * density + 20).toInt(), y)
        val track = image.getPixel(((bar.left.value + artworkWidth.value * .75f) * density).toInt(), y)
        // White at 30 % over the black artwork is 0.3 * 255 = 77 on every channel.
        near("track red", 77, android.graphics.Color.red(track))
        near("track green", 77, android.graphics.Color.green(track))
        near("track blue", 77, android.graphics.Color.blue(track))
        // FOREST and LIME are the defaults: #D5F478.
        near("fill red", 0xD5, android.graphics.Color.red(fill))
        near("fill green", 0xF4, android.graphics.Color.green(fill))
        near("fill blue", 0x78, android.graphics.Color.blue(fill))
    }
}
