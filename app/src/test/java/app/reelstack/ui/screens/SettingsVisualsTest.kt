package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ArtworkCorners
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.ThemeChoice
import app.reelstack.ui.theme.LocalMotionEnabled
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xxhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class SettingsVisualsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun visualChoicesKeepRadioSelectionAndCanBeChangedWithDoubleText() {
        var selected by mutableStateOf(ArtworkCorners.SOFT)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                MaterialTheme {
                    CompositionLocalProvider(LocalMotionEnabled provides false) {
                        ThemeChoice("Hjørne", selected, ArtworkCorners.entries, "corners", { it.name }) { selected = it }
                    }
                }
            }
        }
        rule.onNodeWithTag("theme-choice-corners").performClick()
        rule.onNodeWithTag("corners-SOFT").assertIsSelected()
        rule.onNodeWithTag("corners-ROUND").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(ArtworkCorners.ROUND, selected) }
        rule.onNodeWithTag("corners-ROUND").assertDoesNotExist()
        rule.onNodeWithTag("theme-choice-corners").assertTextContains("ROUND")
    }

    @Test fun categoryCardsOpenAndReturnWithDoubleText() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                MaterialTheme {
                    MobileSettingsScreen(ReelstackUiState(), PaddingValues(0.dp),
                        onConnectionClick = {}, onNotificationsChange = {}, onWifiOnlyChange = {},
                        onHomeSectionChange = { _, _ -> }, onAccountClick = {}, onManageLibraries = {})
                }
            }
        }
        rule.onNodeWithTag("settings-category-APPEARANCE").performScrollTo().assertHasClickAction().performClick()
        rule.onNodeWithTag("theme-choice-corners").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithTag("corners-SOFT").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithTag("settings-back").performClick()
        rule.onNodeWithTag("settings-category-ABOUT").performScrollTo().assertIsDisplayed()
    }
}
