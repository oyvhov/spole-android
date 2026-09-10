package app.reelstack

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.ui.StartupCover
import app.reelstack.ui.components.SpoleFormationMark
import app.reelstack.ui.components.SpoleStartupArt
import app.reelstack.ui.theme.ReelstackTheme
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartupRevealTest {
    @get:Rule val rule = createComposeRule()

    @Test fun logoFormsAboveTheNameWithoutMovingItsLayout() {
        var progress by mutableFloatStateOf(0f)
        rule.setContent { ReelstackTheme {
            Box(Modifier.background(MaterialTheme.colorScheme.background).testTag("art")) {
                SpoleStartupArt({ progress }, Modifier.padding(24.dp))
            }
        } }
        val initial = rule.onNodeWithTag("startup-logo").fetchSemanticsNode().boundsInRoot
        var previousPixels = 0
        for ((index, frame) in listOf(.24f, .55f, 1f).withIndex()) {
            rule.runOnIdle { progress = frame }
            val logo = rule.onNodeWithTag("startup-logo")
            assertEquals(initial, logo.fetchSemanticsNode().boundsInRoot)
            val pixels = logo.captureToImage().toPixelMap()
            var limePixels = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                if (pixels[x,y].green > .65f && pixels[x,y].red > .4f) limePixels++
            }
            assertTrue("Each phase must add visible logo structure", limePixels > previousPixels)
            previousPixels = limePixels
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            File(context.getExternalFilesDir(null), "spole-startup-$index.png").outputStream().use {
                rule.onNodeWithTag("art").captureToImage().asAndroidBitmap()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        val text = rule.onNodeWithTag("startup-wordmark").fetchSemanticsNode().boundsInRoot
        assertTrue(initial.bottom < text.top)
    }

    @Test fun completedFormationMatchesTheExistingVector() {
        var showMaster by mutableStateOf(false)
        rule.setContent { ReelstackTheme {
            Box(Modifier.background(MaterialTheme.colorScheme.background)) {
                if (!showMaster) SpoleFormationMark({ 1f })
                else Image(painterResource(R.drawable.spole_mark), null,
                    Modifier.size(84.dp, 112.dp).testTag("master"),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
            }
        } }
        val actual = rule.onNodeWithTag("startup-logo").captureToImage().toPixelMap()
        // Use the same pixel origin: neighbouring dp slots can straddle fractional pixels.
        rule.runOnIdle { showMaster = true }
        val expected = rule.onNodeWithTag("master").captureToImage().toPixelMap()
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        var different = 0
        for (y in 0 until actual.height) for (x in 0 until actual.width) {
            if (actual[x,y] != expected[x,y]) different++
        }
        assertEquals("Final artwork must be the same vector, not an approximation", 0, different)
    }

    @Test fun coverFinishesAndReleasesUnderlyingAccessibility() {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { StartupCover({}) { Text("Innhald klart") } } }
        rule.onNodeWithTag("startup-cover").assertExists()
        rule.onNodeWithText("Innhald klart").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(1_200)
        rule.mainClock.autoAdvance = true
        rule.waitUntil(timeoutMillis = 2_000) {
            rule.onAllNodesWithTag("startup-cover").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Innhald klart").assertIsDisplayed()
    }

    @Test fun slowNetworkCannotTrapTheUserInTheSplash() {
        rule.setContent { ReelstackTheme { StartupCover({ awaitCancellation() }) { Text("Appen er open") } } }
        rule.waitUntil(timeoutMillis = 4_000) {
            rule.onAllNodesWithTag("startup-cover").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Appen er open").assertIsDisplayed()
    }

    @Test fun appCompositionStartsWhileTheLogoIsForming() {
        var contentComposed = false
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme {
            StartupCover({}) {
                SideEffect { contentComposed = true }
                Text("Heim")
            }
        } }
        rule.mainClock.advanceTimeBy(400)
        rule.runOnIdle { assertTrue(contentComposed) }
        rule.mainClock.advanceTimeBy(1100)
        rule.runOnIdle { assertTrue(contentComposed) }
        rule.mainClock.autoAdvance = true
    }

    @Test fun logoAndNameFitNarrowLandscapeWithLargeText() {
        rule.setContent { ReelstackTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                Box(Modifier.size(320.dp, 320.dp).testTag("viewport")) {
                    SpoleStartupArt({ 1f }, Modifier.align(androidx.compose.ui.Alignment.Center).padding(16.dp))
                }
            }
        } }
        val viewport = rule.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("startup-logo", "startup-wordmark")) {
            val bounds = rule.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= viewport.left && bounds.right <= viewport.right &&
                bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
        }
    }
}
