package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The flying picture starts over the menu's circle and ends exactly over the corner circle. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-mdpi", application = app.reelstack.SheetTestApplication::class)
class ProfileFlightLayerTest {
    @get:Rule val rule = createComposeRule()

    @Test fun thePictureTravelsFromTheMenuRowToTheCornerCircle() {
        val motion = ProfileSwitchMotion(CoroutineScope(Dispatchers.Main.immediate))
        val from = Rect(100f, 300f, 156f, 356f)
        val to = Rect(880f, 20f, 920f, 60f)
        rule.setContent { Box(Modifier.fillMaxSize()) { ProfileFlightLayer(motion) } }
        rule.waitForIdle()
        // Freeze the frame clock only once the empty layer stands, then start the flight on the UI thread.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            motion.reportTarget(to, rootWidthPx = 960f)
            motion.pickedFrom(from, ProfileSwitchMotion.Picture(null, "E"))
            motion.switch(motionWanted = true) {}
            // With the clock frozen nobody else tells the recomposer about the write.
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        rule.mainClock.advanceTimeBy(32)
        rule.waitForIdle()
        // mdpi: 1 dp = 1 px. Two frames in, the picture has barely left the row.
        val start = rule.onNodeWithTag("profile-flight").getBoundsInRoot()
        assertTrue("starts over the row: ${start.left}", start.left.value in 100f..220f)
        assertTrue("starts over the row: ${start.top}", start.top.value in 240f..300f)

        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val end = rule.onNodeWithTag("profile-flight").getBoundsInRoot()
        assertEquals(880f, end.left.value, 2f)
        assertEquals(20f, end.top.value, 2f)
    }
}
