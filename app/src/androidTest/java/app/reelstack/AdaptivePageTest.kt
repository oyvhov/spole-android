package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AdaptivePageTest {
    @get:Rule val rule = createComposeRule()

    @Test fun mediaUsesTabletSpaceAndKeepsStateWhenWindowNarrows() {
        val wide = mutableStateOf(true)
        var width = 0f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(
                if (wide.value) DpSize(1280.dp, 800.dp) else DpSize(400.dp, 800.dp))) {
                ReelstackTheme {
                    ReelPage(media = true) {
                        var clicks by remember { mutableIntStateOf(0) }
                        val density = LocalDensity.current.density
                        Column(Modifier.fillMaxSize().onSizeChanged { width = it.width / density }) {
                            Button(onClick = { clicks++ }, modifier = Modifier.testTag("retained-action")) { Text("Clicks $clicks") }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1120f, width, 2f)
        rule.onNodeWithTag("retained-action").performClick()
        rule.runOnIdle { wide.value = false }
        rule.waitForIdle()
        assertEquals(400f, width, 2f)
        rule.onNodeWithText("Clicks 1").assertIsDisplayed()
        rule.runOnIdle { wide.value = true }
        rule.onNodeWithText("Clicks 1").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1120f, width, 2f) }
    }

    @Test fun readingPagesRemainReadableOnAWideTablet() {
        var width = 0f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1400.dp, 900.dp))) {
                ReelstackTheme {
                    ReelPage {
                        val density = LocalDensity.current.density
                        Box(Modifier.fillMaxSize().onSizeChanged { width = it.width / density }) { Text("Reading") }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(840f, width, 2f)
        rule.onNodeWithText("Reading").assertIsDisplayed()
    }
}
