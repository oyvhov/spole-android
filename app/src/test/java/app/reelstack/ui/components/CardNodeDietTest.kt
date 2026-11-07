package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What a card leaves in the semantics tree that it does not need.
 *
 * Compose sorts every node by position about every 100 ms while a page moves, and on the Shield that took a fifth of
 * the main thread between frames as well as inside them (2026-11-07). Nodes nobody reads cost the same as any other:
 * hidden texts that only measure a reserve, and the layer a clip makes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class CardNodeDietTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun all(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap { all(it) }

    @Test fun aCaptionComposesOnlyTheLinesItShows() {
        rule.setContent { ReelstackTheme {
            RailCardCaption("Serie", "S01E02 · Namn", titleMaxLines = 2, modifier = Modifier.width(200.dp).testTag("caption"))
        } }
        val nodes = all(rule.onNodeWithTag("caption", useUnmergedTree = true).fetchSemanticsNode()).drop(1)
        assertEquals("nodes under the caption: ${nodes.map { it.config.getOrNull(SemanticsProperties.Text) }}", 2, nodes.size)
    }

    @Test fun theProgressBarOnArtworkMakesNoClipLayer() {
        rule.setContent { ReelstackTheme {
            Box(Modifier.size(200.dp, 300.dp).background(Color.Black)) {
                WatchProgressBar(.4f, Modifier.align(Alignment.BottomStart).testTag("bar"))
            }
        } }
        val bar = rule.onNodeWithTag("bar", useUnmergedTree = true).fetchSemanticsNode()
        assertNull("the bar is clipped by a layer of its own", bar.config.getOrNull(SemanticsProperties.Shape))
    }
}
