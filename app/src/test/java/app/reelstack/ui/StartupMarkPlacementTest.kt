package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.components.SpoleStartupArt
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The launch window draws the mark in the middle of the screen (`spole_window_background`, 84 x 112 dp);
 * the cover must hold it on exactly that spot or the logo jumps when Compose takes over.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class StartupMarkPlacementTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun theMarkIsCentredOnTheScreenAndTheNameHangsBelowIt() {
        rule.setContent { ReelstackTheme { Box(Modifier.fillMaxSize().testTag("screen"), contentAlignment = androidx.compose.ui.Alignment.Center) {
            SpoleStartupArt(progress = { 0f }, modifier = Modifier.padding(24.dp), markProgress = { 1f })
        } } }
        rule.waitForIdle()
        val d = rule.density.density
        val screen = rule.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
        val mark = rule.onNodeWithTag("startup-logo").fetchSemanticsNode().boundsInRoot
        val name = rule.onNodeWithTag("startup-wordmark").fetchSemanticsNode().boundsInRoot
        assertEquals(84f * d, mark.width, 1f)
        assertEquals(112f * d, mark.height, 1f)
        assertEquals(screen.center.x, mark.center.x, 1f)
        assertEquals(screen.center.y, mark.center.y, 1f)
        // 20 dp between the mark and the name, as before.
        assertEquals(mark.bottom + 20f * d, name.top, 1f)
    }
}
