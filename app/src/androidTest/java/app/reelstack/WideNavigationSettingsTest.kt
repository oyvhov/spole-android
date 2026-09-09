package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.ui.*
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WideNavigationSettingsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun expandedNavigationSupportsDpadAndClickSelection() {
        var selected by mutableStateOf(AppTab.HOME)
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        rule.setContent { inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            ReelstackTheme { ReelstackNavigationRail(selected, { selected = it }, expanded = true) } }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("wide-tab-HOME").assertIsSelected()
        rule.onNodeWithTag("wide-tab-HOME").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.onNodeWithTag("wide-tab-HOME").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("wide-tab-DISCOVER").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(AppTab.DISCOVER, selected) }
        rule.onNodeWithTag("wide-tab-SETTINGS").performClick().assertIsSelected()
    }
    @Test fun tabletSettingsHasIndependentPanesAndReturnsToOneColumnInSmallWindows() {
        var width by mutableStateOf(1280.dp)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 900.dp))) {
                ReelstackTheme { SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { _, _ -> }) }
            }
        }
        rule.onNodeWithTag("settings-categories").assertIsDisplayed()
        rule.onNodeWithTag("appearance-expand").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-APPEARANCE").performClick().assertIsSelected()
        rule.onNodeWithTag("appearance-expand").performScrollTo().performClick()
        rule.onNodeWithTag("accent-OCEAN").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("settings-category-ABOUT").performClick()
        rule.onNodeWithTag("appearance-expand").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-APPEARANCE").performClick()
        rule.onNodeWithTag("accent-OCEAN").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { width = 412.dp }
        rule.onNodeWithTag("settings-categories").assertDoesNotExist()
        rule.onNodeWithTag("appearance-expand").performScrollTo().assertIsDisplayed()
    }
}
