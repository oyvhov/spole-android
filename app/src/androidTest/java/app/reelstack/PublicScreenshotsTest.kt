package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.Personalization
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

/** Public documentation captures: isolated fixture device, no private accounts or addresses. */
class PublicScreenshotsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun captureActualAppWithDemoContent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val preferences = container.preferencesRepository
        val oldAppearance = preferences.personalization
        val oldOnboarding = preferences.onboardingCompleted
        val store = ViewModelStore()
        try {
            container.connectionRepository.signOutAll()
            container.mediaSnapshotStore.clear()
            preferences.personalization = Personalization()
            preferences.onboardingCompleted = true
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("public", model) }
            rule.setContent { ReelstackTheme { ReelstackApp(model) } }
            rule.waitForIdle()
            rule.onRoot().saveRoadmapImage("public-home.png")
            rule.runOnIdle { model.selectTab(AppTab.DISCOVER) }
            rule.onRoot().saveRoadmapImage("public-discover.png")
            rule.runOnIdle { model.selectTab(AppTab.SETTINGS) }
            rule.onRoot().saveRoadmapImage("public-settings.png")
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            preferences.personalization = oldAppearance
            preferences.onboardingCompleted = oldOnboarding
        }
    }
}
