package app.reelstack

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.ui.components.rowEntrance
import app.reelstack.ui.theme.LocalMotionEnabled
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RowEntranceTest {
    @get:Rule val rule = createComposeRule()

    private fun red(tag: String): Float {
        val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2].red
    }

    @Test fun rowsWaitForTheCoverThenEnterInOrderWithoutMovingLayout() {
        rule.mainClock.autoAdvance = false
        val since = mutableStateOf<Long?>(null)
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) {
                Column {
                    for (index in 1..2) Box(Modifier.size(80.dp).background(Color.Black).testTag("row-$index")) {
                        Box(Modifier.size(80.dp).rowEntrance(index, since.value).background(Color.Red).testTag("content-$index"))
                    }
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, red("row-1"), .01f)
        val before = rule.onNodeWithTag("row-2").getUnclippedBoundsInRoot()
        rule.runOnIdle { since.value = SystemClock.uptimeMillis() }
        rule.mainClock.advanceTimeBy(64)
        assertEquals(0f, red("row-1"), .01f)
        rule.mainClock.advanceTimeBy(256)
        assertTrue("First row has faded in", red("row-1") > .9f)
        assertTrue("Second row is still fading", red("row-2") in .25f.. .9f)
        rule.mainClock.advanceTimeBy(500)
        assertEquals(1f, red("row-2"), .01f)
        assertEquals(before, rule.onNodeWithTag("row-2").getUnclippedBoundsInRoot())
    }

    @Test fun aRowComposedAfterItsWindowIsVisibleImmediately() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) {
                Box(Modifier.size(80.dp).background(Color.Black).testTag("row")) {
                    Box(Modifier.size(80.dp).rowEntrance(2, SystemClock.uptimeMillis() - 2_000).background(Color.Red))
                }
            }
        }
        assertEquals(1f, red("row"), .01f)
    }

    @Test fun reducedMotionOnlyFadesWithoutStaggerScaleOrTranslation() {
        rule.mainClock.autoAdvance = false
        val since = mutableStateOf<Long?>(null)
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides false) {
                Box(Modifier.size(80.dp).background(Color.Black).testTag("row")) {
                    Box(Modifier.size(80.dp).rowEntrance(20, since.value).background(Color.Red))
                }
            }
        }
        rule.runOnIdle { since.value = SystemClock.uptimeMillis() }
        rule.mainClock.advanceTimeBy(64)
        val pixels = rule.onNodeWithTag("row").captureToImage().toPixelMap()
        val centre = pixels[pixels.width / 2, pixels.height / 2].red
        assertTrue("Reduced motion still fades", centre in .05f.. .95f)
        assertEquals("No lift or shrink: the corner fades along with the centre", centre, pixels[1, 1].red, .01f)
        rule.mainClock.advanceTimeBy(100)
        assertEquals(1f, red("row"), .01f)
    }

    @Test fun aNewPresentationHoldsPreviouslyVisibleRowsAgain() {
        rule.mainClock.autoAdvance = false
        val since = mutableStateOf<Long?>(SystemClock.uptimeMillis() - 2_000)
        rule.setContent {
            Box(Modifier.size(80.dp).background(Color.Black).testTag("row")) {
                Box(Modifier.size(80.dp).rowEntrance(0, since.value).background(Color.Red))
            }
        }
        assertEquals(1f, red("row"), .01f)
        rule.runOnIdle { since.value = null }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(0f, red("row"), .01f)
        rule.runOnIdle { since.value = SystemClock.uptimeMillis() }
        rule.mainClock.advanceTimeBy(800)
        assertEquals(1f, red("row"), .01f)
    }

    @Test fun loadingAnotherSectionAboveDoesNotRestartAnExistingRow() {
        rule.mainClock.autoAdvance = false
        val index = mutableStateOf(0)
        val since = SystemClock.uptimeMillis()
        rule.setContent {
            CompositionLocalProvider(LocalMotionEnabled provides true) {
                Box(Modifier.size(80.dp).background(Color.Black).testTag("row")) {
                    Box(Modifier.size(80.dp).rowEntrance(index.value, since).background(Color.Red))
                }
            }
        }
        rule.mainClock.advanceTimeBy(192)
        val before = red("row")
        assertTrue(before > .5f)
        rule.runOnIdle { index.value = 20 }
        rule.mainClock.advanceTimeByFrame()
        assertTrue("An already visible row must not disappear when another section arrives", red("row") >= before)
        rule.mainClock.advanceTimeBy(100)
        assertEquals(1f, red("row"), .01f)
    }
}
