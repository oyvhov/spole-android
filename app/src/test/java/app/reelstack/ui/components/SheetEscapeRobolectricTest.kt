package app.reelstack.ui.components

import androidx.activity.ComponentDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * Back must take the sheet away, whenever it is pressed.
 *
 * [SheetExitTest] holds the rule; this holds the window. The freeze on 16 September was a real
 * dialog window that kept focus while drawing nothing, so the thing worth checking is a real dialog
 * window — pressed in the first frame, pressed repeatedly, and pressed while the sheet is refusing
 * every other way out.
 *
 * It runs under Robolectric rather than on a device. That is not a compromise: the failure was a
 * coroutine that never resumed, which a JVM clock reproduces exactly, and the suite stays runnable
 * on any machine instead of waiting for an emulator.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class SheetEscapeRobolectricTest {
    @get:Rule val rule = createComposeRule()

    /** The remote's Back key, arriving at the dialog window the way the platform delivers it. */
    private fun pressBack(): Boolean {
        val dialog = ShadowDialog.getLatestDialog() as? ComponentDialog ?: return false
        rule.runOnUiThread { dialog.onBackPressedDispatcher.onBackPressed() }
        return true
    }

    /**
     * Steps the clock until the dialog window exists, and answers how many frames that took.
     *
     * With the clock held still, the window is not up the instant the state flips — it takes the
     * recomposition that adds it. The frame it appears in is exactly the moment worth testing: the
     * entrance has started and finished nothing.
     */
    private fun advanceToFirstFrameWithDialog(limit: Int = 20): Int {
        var frames = 0
        while (ShadowDialog.getShownDialogs().isEmpty() && frames < limit) {
            // Both, in this order: the frame lets the recomposition that adds the window happen,
            // and draining after it lets the window actually attach. With the clock held still,
            // neither one alone gets there.
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            frames++
        }
        return frames
    }

    @Test fun backInTheFirstFrameStillClosesTheSheet() {
        var closed = 0
        val open = mutableStateOf(false)
        rule.setContent {
            if (open.value) {
                StableSheetDialog(dismissEnabled = true, onDismiss = { closed++; open.value = false }) { _, _, _ ->
                    Text("innhald")
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { open.value = true }
        // The very first frame the window is up. The entrance animation has started and finished
        // nothing — this is the moment that used to latch `closing` and swallow every later press.
        val frames = advanceToFirstFrameWithDialog()
        assertTrue("the dialog window never appeared (after $frames frames)", frames in 1..19)
        assertTrue("the dialog window must exist to be pressed", pressBack())
        rule.mainClock.advanceTimeBy(2_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals("Back in the first frame must dismiss exactly once", 1, closed)
    }

    @Test fun repeatedBackPressesDoNotLeaveTheSheetBehind() {
        var closed = 0
        val open = mutableStateOf(true)
        rule.setContent {
            if (open.value) {
                StableSheetDialog(dismissEnabled = true, onDismiss = { closed++; open.value = false }) { _, _, _ ->
                    Text("innhald")
                }
            }
        }
        rule.waitForIdle()
        repeat(3) { if (open.value) pressBack() }
        rule.waitForIdle()

        assertEquals("three presses are still one dismissal, and it did happen", 1, closed)
        assertTrue("the sheet is gone", !open.value)
    }

    @Test fun aSheetThatIsCommittingSomethingStillLetsBackOut() {
        // `dismissEnabled` is false while a Seerr request is on its way. Refusing an outside tap is
        // reasonable; refusing the only way out of a full-screen modal is not, because a request
        // that never returns would otherwise strand the reader in the sheet for good.
        var closed = 0
        val open = mutableStateOf(true)
        rule.setContent {
            if (open.value) {
                StableSheetDialog(dismissEnabled = false, onDismiss = { closed++; open.value = false }) { _, _, _ ->
                    Text("sender")
                }
            }
        }
        rule.waitForIdle()
        pressBack()
        rule.waitForIdle()

        assertEquals("Back must escape even while the sheet is committing", 1, closed)
    }
}
