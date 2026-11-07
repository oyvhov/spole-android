package app.reelstack.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class CardAppearedTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    /**
     * A card that is not revealed is built once. Writing "appeared" a frame after it was built made Compose build it
     * again: the six cards of a shelf coming into view were built twice, and the second time was a 14 ms frame at every
     * press on the Shield (2026-11-07).
     */
    @Test fun aCardWithoutARevealIsBuiltOnce() {
        var builds = 0
        rule.setContent { val appeared = rememberCardAppeared("card", reveal = false); builds++; Text("$appeared") }
        rule.waitForIdle()
        assertEquals(1, builds)
    }

    @Test fun aRevealedCardAppearsAfterItsFirstFrame() {
        val seen = mutableListOf<Boolean>()
        rule.setContent { seen += rememberCardAppeared("card", reveal = true); Text("${seen.last()}") }
        rule.waitForIdle()
        assertEquals(listOf(false, true), seen)
    }
}
