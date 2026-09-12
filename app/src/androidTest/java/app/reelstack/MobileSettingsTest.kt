package app.reelstack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MobileSettingsTest {
    @get:Rule val rule = createComposeRule()
    private val repository get() = AppPreferencesRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun host(fontScale: Float = 1f) {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    ReelstackTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { _, _ -> })
                    } }
                }
            }
        }
    }
    @Test fun overviewOpensOnlyOneGroupAndBackReturnsToCategories() {
        host()
        rule.onNodeWithTag("theme-choice-season").assertDoesNotExist()
        rule.onNodeWithTag("auto-resume").assertDoesNotExist()
        capture("mobile-settings-index")
        rule.onNodeWithTag("settings-category-APPEARANCE").performClick()
        rule.onNodeWithTag("theme-choice-season").assertIsDisplayed()
        rule.onNodeWithTag("auto-resume").assertDoesNotExist()
        rule.onNodeWithTag("settings-back").performClick()
        rule.onNodeWithTag("settings-category-PLAYBACK").performScrollTo().performClick()
        rule.onNodeWithTag("auto-resume").assertIsDisplayed()
        rule.onNodeWithTag("theme-choice-season").assertDoesNotExist()
        androidx.test.espresso.Espresso.pressBack()
        rule.onNodeWithTag("settings-category-PLAYBACK").assertIsDisplayed()
    }
    @Test fun christmasAndHalloweenApplyTheirPaletteAndKeepOtherPreferences() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization(autoResume = false)
            host()
            rule.onNodeWithTag("settings-category-APPEARANCE").performClick()
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-CHRISTMAS").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(VisualTheme.NOEL, repository.personalization.visualTheme)
                assertEquals(AccentPalette.HOLLY, repository.personalization.accent)
                assertFalse(repository.personalization.autoResume)
            }
            capture("mobile-settings-christmas")
            rule.onNodeWithTag("brand-season-CHRISTMAS").assertExists()
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-HALLOWEEN").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(VisualTheme.HALLOWEEN, repository.personalization.visualTheme)
                assertEquals(AccentPalette.PUMPKIN, repository.personalization.accent)
            }
            capture("mobile-settings-halloween")
            rule.onNodeWithTag("brand-season-HALLOWEEN").assertExists()
            rule.onNodeWithTag("theme-ornament").performScrollTo().performClick()
            rule.onNodeWithTag("brand-season-HALLOWEEN").assertDoesNotExist()
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-NONE").performScrollTo().performClick()
            rule.onNodeWithTag("theme-ornament").assertDoesNotExist()
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
    @Test fun doubleTextCanReachAllGroupsAndChangePlaybackTime() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization()
            host(2f)
            for (name in listOf("APPEARANCE", "HOME", "MENU", "PLAYBACK", "ACCOUNTS", "UPDATES", "ABOUT")) {
                rule.onNodeWithTag("settings-category-$name").performScrollTo().assertIsDisplayed().performClick()
                rule.onNodeWithTag("settings-back").assertIsDisplayed().performClick()
            }
            rule.onNodeWithTag("settings-category-PLAYBACK").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-next-episode-lead").performScrollTo().performClick()
            rule.onNodeWithTag("next-episode-lead-120").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(120, repository.personalization.nextEpisodeLeadSeconds) }
            capture("mobile-settings-large-text")
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
    private fun capture(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
