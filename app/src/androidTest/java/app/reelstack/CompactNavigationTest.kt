package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.reelstack.ui.AppTab
import app.reelstack.ui.ReelstackBottomBar
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CompactNavigationTest {
    @get:Rule val rule = createComposeRule()

    @Test fun settingsFitsAndOpensOnNarrowPhone() {
        var selected = AppTab.HOME
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                ReelstackTheme { ReelstackBottomBar(AppTab.HOME) { selected = it } }
            }
        }
        val label = rule.onNodeWithText("Val").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val bar = rule.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(label.left >= bar.left && label.right <= bar.right)
        rule.onNodeWithContentDescription("Innstillingar").performClick()
        rule.runOnIdle { assertEquals(AppTab.SETTINGS, selected) }
        rule.onRoot().saveRoadmapImage("compact-navigation.png")
    }

    @Test fun largeTextKeepsFullSettingsNameInMenu() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme { ReelstackBottomBar(AppTab.SETTINGS) {} }
                }
            }
        }
        rule.onNodeWithText("Innstillingar").assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("compact-navigation-large.png")
    }
}
