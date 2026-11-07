package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A television row runs on under the page's end margin to the edge of the screen and fades there, as on Home. On the
 * library and detail pages «Neste», «Medvirkende» and the episodes stopped 32 dp short of the edge and cut a card
 * through there (Loftet, 2026-11-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class RowBleedTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun aRowRunsOnUnderTheMarginToTheEdge() {
        rule.setContent { ReelstackTheme {
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
                Box(Modifier.fillMaxWidth().height(40.dp).bleedToEnd(32.dp).testTag("row"))
            }
        } }
        val screen = rule.onRoot().getUnclippedBoundsInRoot().right.value
        assertEquals(screen, rule.onNodeWithTag("row").getUnclippedBoundsInRoot().right.value, 0.5f)
        assertEquals(32f, rule.onNodeWithTag("row").getUnclippedBoundsInRoot().left.value, 0.5f)
    }

    @Test fun theDetailPagesBottomFadeLetsARowRunOnSideways() {
        rule.setContent { ReelstackTheme {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                Column(Modifier.fillMaxSize().padding(end = 32.dp).safeBottomFade()) {
                    Box(Modifier.fillMaxWidth().height(40.dp).bleedToEnd(32.dp).background(Color.Red))
                }
            }
        } }
        val bitmap = rule.runOnIdle { app.reelstack.ui.drawnContent(rule.activity) }
        val d = rule.density.density
        val pixel = bitmap.getPixel(bitmap.width - (8 * d).toInt(), (20 * d).toInt())
        assertEquals("the row is drawn in the margin", 255, android.graphics.Color.red(pixel))
    }
}
