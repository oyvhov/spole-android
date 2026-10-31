package app.reelstack.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The seek picture sits just above the point being sought, not in the middle of the screen.
 * It follows the position along the timeline and stops at the timeline's ends.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-mdpi", application = app.reelstack.SheetTestApplication::class)
class SeekPreviewPlacementTest {
    @get:Rule val rule = createComposeRule()
    private val track = Rect(100f, 900f, 1100f, 948f)
    private val size = IntSize(200, 112)

    @Test fun thePictureSitsAboveTheSoughtPointAndStaysWithinTheTimeline() {
        assertEquals(IntOffset(500, 900 - 12 - 112), seekPreviewOffset(track, .5f, size, 12))
        assertEquals(IntOffset(100, 776), seekPreviewOffset(track, 0f, size, 12))
        assertEquals(IntOffset(900, 776), seekPreviewOffset(track, 1f, size, 12))
        assertEquals(IntOffset(150, 776), seekPreviewOffset(track, .15f, size, 12))
        // Never above the screen, whatever the timeline reports.
        assertEquals(0, seekPreviewOffset(Rect(0f, 40f, 1000f, 88f), .5f, size, 12).y)
    }

    @Test fun theLayerPlacesItsPictureOverThePosition() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                SeekPreviewPlacement(Rect(100f, 400f, 900f, 448f), .25f, gap = 8.dp) {
                    Box(Modifier.size(200.dp, 112.dp).testTag("picture"))
                }
            }
        }
        val bounds = rule.onNodeWithTag("picture").getUnclippedBoundsInRoot()
        // mdpi: 1 dp = 1 px. Centre at 100 + 0.25 × 800 = 300; bottom 8 above the timeline's top.
        assertEquals(300f, (bounds.left.value + bounds.right.value) / 2, 1f)
        assertEquals(392f, bounds.bottom.value, 1f)
        assertTrue(bounds.top.value > 200f)
    }
}
