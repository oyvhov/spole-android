package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    @Test fun collapsingKeepsFullHeightFocusAndNavigation() {
        var expanded by mutableStateOf(true)
        var selected by mutableStateOf(AppTab.HOME)
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        rule.setContent {
            inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
            ReelstackTheme { Box(Modifier.height(600.dp)) {
                ReelstackNavigationRail(selected, { selected = it }, expanded, { expanded = it })
            } }
        }
        rule.runOnIdle { inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        rule.onNodeWithTag("sidebar-toggle").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp).assertHeightIsEqualTo(600.dp)
        rule.onNodeWithTag("sidebar-toggle").assertIsFocused().assertContentDescriptionEquals("Utvid menyen")
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("wide-tab-HOME").assertIsFocused().assertContentDescriptionEquals("Heim")
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("wide-tab-DISCOVER").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(AppTab.DISCOVER, selected) }
        rule.onNodeWithTag("sidebar-toggle").performClick()
        rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(200.dp).assertHeightIsEqualTo(600.dp)
        rule.onNodeWithTag("wide-tab-DISCOVER").assertIsSelected()
    }
    @Test fun widthAnimatesWithoutMovingTheIconColumnOrChangingHeight() {
        var expanded by mutableStateOf(true)
        rule.setContent { ReelstackTheme { Box(Modifier.height(600.dp)) {
            ReelstackNavigationRail(AppTab.HOME, {}, expanded, { expanded = it })
        } } }
        val initial = rule.onNodeWithTag("side-navigation").fetchSemanticsNode().boundsInRoot
        val iconColumn = rule.onNodeWithTag("wide-tab-HOME").fetchSemanticsNode().boundsInRoot.left
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeBy(96)
        val middle = rule.onNodeWithTag("side-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(middle.width < initial.width && middle.width > initial.width * .4f)
        assertEquals(initial.height, middle.height, 1f)
        assertEquals(iconColumn, rule.onNodeWithTag("wide-tab-HOME").fetchSemanticsNode().boundsInRoot.left, 1f)
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
        rule.mainClock.autoAdvance = true
    }
    @Test fun pageWidthSettlesOnceWhileTheRailAnimatesAboveIt() {
        var expanded by mutableStateOf(true)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1280.dp, 800.dp))) {
                ReelstackTheme { Row(Modifier.fillMaxSize()) {
                    SidebarSlot(expanded) { ReelstackNavigationRail(AppTab.HOME, {}, expanded, { expanded = it }) }
                    Box(Modifier.weight(1f).fillMaxHeight().testTag("page"))
                } }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { expanded = false }
        rule.mainClock.advanceTimeBy(64)
        val first = rule.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("sidebar-slot").assertWidthIsEqualTo(80.dp)
        rule.mainClock.advanceTimeBy(64)
        assertEquals(first, rule.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot)
        rule.mainClock.advanceTimeBy(400)
        assertEquals(first, rule.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot)
        rule.mainClock.autoAdvance = true
    }
}
