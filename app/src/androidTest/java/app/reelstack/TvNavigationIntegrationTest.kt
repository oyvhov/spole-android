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
    @Test fun appStartsCollapsedExpandsOnlyOnLeftAndCollapsesAfterSelection() {
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
            container.preferencesRepository.personalization = Personalization()
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("navigation", model) }
            rule.setContent {
                val mode = androidx.compose.ui.platform.LocalInputModeManager.current
                androidx.compose.runtime.SideEffect { mode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
                ReelstackTheme { ReelstackApp(model) }
            }
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
            rule.onNodeWithTag("wide-tab-HOME").assertIsNotFocused()
            val railTabs = hasTestTag("wide-tab-HOME") or hasTestTag("wide-tab-LIBRARY") or
                hasTestTag("wide-tab-DISCOVER") or hasTestTag("wide-tab-ACTIVITY") or hasTestTag("wide-tab-SETTINGS")
            repeat(5) {
                if (rule.onAllNodes(railTabs and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
            }
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(200.dp)
            rule.onNode(railTabs and isFocused()).assertExists()
            repeat(5) {
                if (rule.onAllNodes(hasTestTag("wide-tab-SETTINGS") and isFocused()).fetchSemanticsNodes().isEmpty())
                    rule.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
            }
            rule.onNodeWithTag("wide-tab-SETTINGS").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
            rule.onNodeWithTag("side-navigation").assertWidthIsEqualTo(80.dp)
            rule.onNodeWithTag("wide-tab-SETTINGS").assertIsSelected().assertIsNotFocused()
            rule.runOnIdle { assertEquals(AppTab.SETTINGS, model.uiState.value.selectedTab) }
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            container.preferencesRepository.personalization = oldOptions
            container.preferencesRepository.onboardingCompleted = oldOnboarding
        }
    }
}
