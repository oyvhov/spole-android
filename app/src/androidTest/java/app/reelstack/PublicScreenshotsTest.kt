package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.Season
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.HomeRow
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

/** Public documentation captures: isolated fixture device, no private accounts or addresses. */
@OptIn(ExperimentalTestApi::class)
class PublicScreenshotsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun captureActualAppWithDemoContent() = captureDemo(Season.NONE)
    @Test fun christmasDemo() = captureDemo(Season.CHRISTMAS)
    @Test fun halloweenDemo() = captureDemo(Season.HALLOWEEN)
    @Test fun christmasDemoWithLargeText() = captureDemo(Season.CHRISTMAS, 2f)
    @Test fun halloweenDemoWithLargeText() = captureDemo(Season.HALLOWEEN, 2f)

    private fun captureDemo(season: Season, fontScale: Float = 1f) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val preferences = container.preferencesRepository
        val oldAppearance = preferences.personalization
        val oldOnboarding = preferences.onboardingCompleted
        val oldRows = preferences.visibleHomeSections
        val oldOrder = preferences.homeRowOrder
        val suffix = (if (season == Season.NONE) "" else "-${season.name.lowercase()}") + if (fontScale > 1f) "-large" else ""
        val store = ViewModelStore()
        try {
            container.connectionRepository.signOutAll()
            container.mediaSnapshotStore.clear()
            // The supported reduced-motion setting keeps captures deterministic and includes
            // the static seasonal illustration used on slower TVs.
            preferences.personalization = season.applyTo(Personalization(lightweightTv = true))
            preferences.onboardingCompleted = true
            preferences.visibleHomeSections = HomeSection.entries.toSet()
            preferences.homeRowOrder = HomeRow.entries
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("public", model) }
            rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                ReelstackTheme { ReelstackApp(model) }
            } }
            rule.waitForIdle()
            val hero = rule.onAllNodesWithTag("tablet-library-feature").fetchSemanticsNodes().isNotEmpty()
            if (hero) rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed()
            rule.onRoot().saveRoadmapImage("public-home$suffix.png")
            if (hero) {
                rule.onNodeWithTag("tablet-feature-open").performClick()
                rule.onNodeWithTag("sheet-viewport").assertExists()
                rule.runOnIdle { model.closeSheet() }
            }
            if (fontScale > 1f) return
            rule.onNodeWithTag("home-feed").performScrollToNode(hasText("The Odyssey", substring = true))
            rule.onRoot().saveRoadmapImage("public-rows$suffix.png")
            rule.runOnIdle { model.selectTab(AppTab.DISCOVER) }
            rule.onRoot().saveRoadmapImage("public-discover$suffix.png")
            rule.runOnIdle { model.selectTab(AppTab.SETTINGS) }
            if (season != Season.NONE) {
                if (rule.onAllNodesWithTag("theme-choice-season").fetchSemanticsNodes().isEmpty()) {
                    rule.onNodeWithTag("settings-category-APPEARANCE").performScrollTo().performClick()
                }
                if (rule.onAllNodesWithTag("appearance-expand").fetchSemanticsNodes().isNotEmpty()) {
                    rule.onNodeWithTag("appearance-expand").performScrollTo().performClick()
                }
                rule.onNodeWithTag("theme-choice-season").performScrollTo().assertIsDisplayed()
                org.junit.Assert.assertEquals(season, Season.of(preferences.personalization))
            }
            rule.onRoot().saveRoadmapImage("public-settings$suffix.png")
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            preferences.personalization = oldAppearance
            preferences.onboardingCompleted = oldOnboarding
            preferences.visibleHomeSections = oldRows
            preferences.homeRowOrder = oldOrder
        }
    }
}
