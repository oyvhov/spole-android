package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvSettingsLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    /**
     * The settings on a TV stopped 72 dp above the bottom of the screen and cut «Legg til barneprofil» through the
     * middle of its line (Loftet, 2026-11-07). The list runs on to the edge and fades there, as the detail pages do.
     */
    @Test fun theSettingsRunOnToTheBottomOfTheScreenInsteadOfStoppingShortWithACut() {
        rule.setContent {
            app.reelstack.ui.theme.ReelstackTheme { Surface(Modifier.fillMaxSize()) {
                TvSettingsScreen(ReelstackUiState(), PaddingValues(bottom = 48.dp), {}, {}, {}, { _, _ -> }, {}, {})
            } }
        }
        val screen = rule.onRoot().getUnclippedBoundsInRoot().bottom.value
        val feed = rule.onNodeWithTag("settings-feed").getUnclippedBoundsInRoot().bottom.value
        assertTrue("the settings end at $feed dp on a $screen dp screen", feed >= screen - 1f)
    }
}
