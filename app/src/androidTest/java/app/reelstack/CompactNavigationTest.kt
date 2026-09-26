package app.reelstack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
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

    @Composable
    private fun InLanguage(language: AppLanguage, content: @Composable () -> Unit) {
        val context = AppLanguages.wrap(LocalContext.current, language)
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides context.resources.configuration,
        ) {
            ReelstackTheme { content() }
        }
    }

    private fun label(language: AppLanguage, id: Int): String =
        AppLanguages.wrap(InstrumentationRegistry.getInstrumentation().targetContext, language).getString(id)

    @Test fun settingsFitsAndOpensOnNarrowPhone() {
        var selected = AppTab.HOME
        val settings = label(AppLanguage.NYNORSK, R.string.nav_settings)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1f)) {
                    InLanguage(AppLanguage.NYNORSK) {
                        ReelstackBottomBar(AppTab.HOME, onSelect = { selected = it })
                    }
                }
            }
        }
        val item = rule.onNodeWithContentDescription(settings).assertIsDisplayed()
        val bounds = item.fetchSemanticsNode().boundsInRoot
        val bar = rule.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= bar.left && bounds.right <= bar.right)
        assertTrue(bounds.top >= bar.top && bounds.bottom <= bar.bottom)
        item.performClick()
        rule.runOnIdle { assertEquals(AppTab.SETTINGS, selected) }
        rule.onRoot().saveRoadmapImage("compact-navigation.png")
    }

    @Test fun largeTextKeepsFullSettingsNameInMenu() {
        val settings = label(AppLanguage.NYNORSK, R.string.nav_settings)
        val menu = label(AppLanguage.NYNORSK, R.string.settings_tv_navigation)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    InLanguage(AppLanguage.NYNORSK) {
                        ReelstackBottomBar(AppTab.SETTINGS, onSelect = {})
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("$menu · $settings").assertIsDisplayed()
        val label = rule.onNodeWithText(settings).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val bar = rule.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue(label.left >= bar.left && label.right <= bar.right)
        assertTrue(label.top >= bar.top && label.bottom <= bar.bottom)
        rule.onRoot().saveRoadmapImage("compact-navigation-large.png")
    }

    @Test fun nynorskUsesShortWordsAndFullLargeTextMenu() = checkCompactLabels(
        AppLanguage.NYNORSK,
        listOf(R.string.nav_library_short, R.string.nav_activity_short, R.string.nav_settings_short),
    )

    @Test fun englishUsesShortWordsAndFullLargeTextMenu() = checkCompactLabels(
        AppLanguage.ENGLISH,
        listOf(R.string.nav_discover_short, R.string.nav_settings_short),
    )

    private fun checkCompactLabels(language: AppLanguage, shortLabels: List<Int>) {
        val fontScale = mutableFloatStateOf(1.4f)
        val selected = mutableStateOf(AppTab.HOME)
        val home = label(language, R.string.nav_home)
        val library = label(language, R.string.nav_library)
        val discover = label(language, R.string.nav_discover)
        val activity = label(language, R.string.nav_activity)
        val settings = label(language, R.string.nav_settings)
        val menu = label(language, R.string.settings_tv_navigation)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale.floatValue)) {
                    InLanguage(language) {
                        // The tightest bar: every tab, with Downloads chosen into the menu as well.
                        // Without it five labels fit whole at 1.4 and no short word is needed.
                        CompositionLocalProvider(app.reelstack.ui.theme.LocalPersonalization provides
                            app.reelstack.ui.theme.LocalPersonalization.current.copy(showDownloadsInMenu = true)) {
                            ReelstackBottomBar(selected.value, onSelect = { selected.value = it })
                        }
                    }
                }
            }
        }
        shortLabels.forEach { id -> rule.onNodeWithText(label(language, id)).assertIsDisplayed() }
        rule.onRoot().saveRoadmapImage("compact-navigation-${language.tag}.png")

        rule.runOnIdle { fontScale.floatValue = 2f }
        rule.onNodeWithContentDescription("$menu · $home").assertIsDisplayed().performClick()
        listOf(library, discover, activity, settings).forEach { name ->
            rule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithText(settings).performClick()
        rule.runOnIdle { assertEquals(AppTab.SETTINGS, selected.value) }
        rule.onNodeWithContentDescription("$menu · $settings").assertIsDisplayed()
    }
}
