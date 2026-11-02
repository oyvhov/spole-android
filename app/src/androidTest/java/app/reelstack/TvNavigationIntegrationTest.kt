package app.reelstack

import android.content.res.Configuration
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvNavigationIntegrationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun appStartsCollapsedExpandsOnlyOnLeftAndCollapsesAfterSelection() = checkNavigation(false)
    @Test fun hiddenSidebarOpensFromLeftEdgeWithoutMovingContent() = checkNavigation(true)

    @Test fun topMenuReplacesTheRailAndRemoteCanReturnToThePage() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        org.junit.Assume.assumeTrue(context.getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        val container = AppContainer(context)
        val oldOptions = container.preferencesRepository.personalization
        val oldOnboarding = container.preferencesRepository.onboardingCompleted
        val store = ViewModelStore()
        try {
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.preferencesRepository.onboardingCompleted = true
            container.preferencesRepository.personalization = Personalization(tvMenuStyle = app.reelstack.data.model.TvMenuStyle.TOP_BAR)
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("top-navigation", model) }
            rule.setContent {
                val mode = androidx.compose.ui.platform.LocalInputModeManager.current
                androidx.compose.runtime.SideEffect { mode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                ReelstackTheme { androidx.compose.foundation.layout.Box { ReelstackApp(model) } }
            }
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithTag("side-navigation").assertDoesNotExist()
            rule.onNodeWithTag("home-account").assertDoesNotExist()
            saveTvReview("top-menu-home", rule.onRoot().captureToImage())
            // The picture runs behind the menu line, not below it: the hero starts at the top edge, its words under the line.
            rule.onNodeWithTag("tablet-library-feature").assertTopPositionInRootIsEqualTo(0.dp)
            val line = rule.onNodeWithTag("tv-top-bar").fetchSemanticsNode().boundsInRoot
            val words = rule.onNodeWithTag("feature-caption", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("the words start ${words.top} px down, above the line's ${line.bottom} px", words.top >= line.bottom)
            rule.onNodeWithTag("resume-card-resume-severance")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            repeat(8) {
                if (rule.onAllNodes(hasTestTag("tv-top-HOME") and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
            }
            rule.onNodeWithTag("tv-top-HOME").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            rule.runOnIdle { assertEquals(AppTab.HOME, model.uiState.value.selectedTab) }
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
            rule.onNodeWithTag("tv-top-HOME").assertIsNotFocused()
            rule.onNodeWithTag("tv-top-account").performClick()
            rule.onNodeWithTag("profile-menu").assertIsDisplayed()
            rule.onNodeWithTag("tv-top-HOME").assertDoesNotExist()
            rule.runOnIdle { model.closeSheet() }
            rule.onNodeWithTag("tv-top-search").performClick()
            rule.runOnIdle { assertTrue(model.uiState.value.globalSearchOpen) }
            rule.onNodeWithTag("tv-top-search").assertIsSelected().assertIsDisplayed()
            rule.onNodeWithTag("tv-top-search")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
                .performKeyInput { pressKey(Key.DirectionDown) }
            rule.onNodeWithTag("tv-top-search").assertIsNotFocused()
            rule.onNodeWithTag("resume-card-resume-severance").assertDoesNotExist()
            rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionUp) }
            rule.onNodeWithTag("tv-top-search").assertIsFocused()
            saveTvReview("top-menu-search", rule.onRoot().captureToImage())
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            container.preferencesRepository.personalization = oldOptions
            container.preferencesRepository.onboardingCompleted = oldOnboarding
        }
    }

    private fun checkNavigation(hidden: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        org.junit.Assume.assumeTrue(context.getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        val container = AppContainer(context)
        val oldOptions = container.preferencesRepository.personalization
        val oldOnboarding = container.preferencesRepository.onboardingCompleted
        val store = ViewModelStore()
        try {
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.preferencesRepository.onboardingCompleted = true
            container.preferencesRepository.personalization = Personalization(tvMenuStyle = if (hidden)
                app.reelstack.data.model.TvMenuStyle.HIDDEN_SIDEBAR else app.reelstack.data.model.TvMenuStyle.SIDEBAR)
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("navigation", model) }
            rule.setContent {
                val mode = androidx.compose.ui.platform.LocalInputModeManager.current
                androidx.compose.runtime.SideEffect { mode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                ReelstackTheme { ReelstackApp(model) }
            }
            // Finish the app's delayed initial focus hand-off before navigating a specific card.
            rule.mainClock.advanceTimeBy(1000)
            rule.waitForIdle()
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
            rule.onNodeWithTag("wide-tab-HOME").assertIsNotFocused()
            rule.onNodeWithTag("sidebar-slot").assertWidthIsEqualTo(if (hidden) 0.dp else 80.dp)
            val before = rule.onNodeWithTag("home-feed").getUnclippedBoundsInRoot()
            rule.onNodeWithTag("resume-card-resume-severance")
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            rule.onNodeWithTag("resume-card-resume-odyssey").assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionLeft) }
            rule.onNodeWithTag("resume-card-resume-severance").assertIsFocused()
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
            val railTabs = hasTestTag("wide-tab-HOME") or hasTestTag("wide-tab-LIBRARY") or
                hasTestTag("wide-tab-DISCOVER") or hasTestTag("wide-tab-SETTINGS")
            repeat(5) {
                if (rule.onAllNodes(railTabs and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
            }
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(200.dp)
            assertEquals(before, rule.onNodeWithTag("home-feed").getUnclippedBoundsInRoot())
            rule.onNode(railTabs and isFocused()).assertExists()
            repeat(5) {
                if (rule.onAllNodes(hasTestTag("wide-tab-SETTINGS") and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
            }
            rule.onNodeWithTag("wide-tab-SETTINGS").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
            rule.onNodeWithTag("wide-tab-SETTINGS").assertIsSelected().assertIsNotFocused()
            rule.runOnIdle { assertEquals(AppTab.SETTINGS, model.uiState.value.selectedTab) }
            repeat(5) {
                if (rule.onAllNodes(railTabs and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionLeft) }
            }
            rule.onNodeWithTag("wide-tab-SETTINGS").assertIsFocused()
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            container.preferencesRepository.personalization = oldOptions
            container.preferencesRepository.onboardingCompleted = oldOnboarding
        }
    }
}
