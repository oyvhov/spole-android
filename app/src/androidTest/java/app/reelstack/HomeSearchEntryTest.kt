package app.reelstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.HomeSearchEntry
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeSearchEntryTest {
    @get:Rule val rule = createComposeRule()

    @Test fun wholeSearchSurfaceIsOneAction() {
        var clicks = 0
        rule.setContent { ReelstackTheme { HomeSearchEntry(onClick = { clicks++ }) } }
        rule.onAllNodes(hasClickAction()).assertCountEquals(1)
        rule.onNodeWithTag("home-search").performClick()
        assertEquals(1, clicks)
    }

    @Test fun narrowSearchSurfaceGrowsForDoubleSizeText() {
        rule.setContent {
            ReelstackTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    Box(Modifier.width(300.dp)) { HomeSearchEntry(onClick = {}) }
                }
            }
        }
        val surface = rule.onNodeWithTag("home-search").fetchSemanticsNode().boundsInRoot
        listOf("Søk etter filmar og seriar").forEach { text ->
            val node = rule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("Text must remain within the search surface", bounds.top >= surface.top &&
                bounds.bottom <= surface.bottom && bounds.right <= surface.right)
        }
    }
}
