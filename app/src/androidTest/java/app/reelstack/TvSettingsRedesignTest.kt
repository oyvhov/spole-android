package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.*
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvSettingsRedesignTest {
    @get:Rule val rule = createComposeRule()
    private val repository get() = AppPreferencesRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    @Test fun nextEpisodeTimingAndLightweightModePersistAndRemainReachableAtDoubleText() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization()
            rule.setContent { Television(2f) { SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, {_,_->}) } }
            rule.onNodeWithTag("tv-lightweight").performScrollTo().performClick()
            rule.runOnIdle { assertTrue(repository.personalization.lightweightTv) }
            rule.onNodeWithTag("settings-category-PLAYBACK").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-next-episode-lead").performScrollTo().performClick()
            rule.onNodeWithTag("next-episode-lead-120").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-next-episode-delay").performScrollTo().performClick()
            rule.onNodeWithTag("next-episode-delay-20").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(120, repository.personalization.nextEpisodeLeadSeconds)
                assertEquals(20, repository.personalization.nextEpisodeDelaySeconds)
            }
            rule.onNodeWithTag("next-episode-auto").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-next-episode-delay").assertDoesNotExist()
            rule.runOnIdle { assertFalse(repository.personalization.autoPlayNextEpisode) }
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
    @Composable private fun Television(fontScale: Float = 1f, content: @Composable () -> Unit) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(880.dp, 540.dp))) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                val tv = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_TELEVISION
                }
                CompositionLocalProvider(LocalConfiguration provides tv) {
                    ReelstackTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() } }
                }
            }
        }
    }
    @Test fun televisionUsesCategoriesBelowTheTabletBreakpointAndRemoteMovesBetweenPanes() {
        rule.setContent { Television { SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, {_,_->}) } }
        rule.onNodeWithTag("settings-categories").assertIsDisplayed()
        rule.onNodeWithTag("settings-category-HOME").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("show-next-up").assertIsFocused()
        rule.onNodeWithTag("show-next-up").performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag("settings-category-HOME").assertIsFocused()
        rule.onNodeWithTag("theme-choice-mood").assertDoesNotExist()
        rule.onNodeWithTag("auto-resume").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-PLAYBACK").performClick()
        rule.onNodeWithTag("auto-resume").assertIsDisplayed()
        rule.onNodeWithTag("show-next-up").assertDoesNotExist()
    }
    @Test fun themeChoicesPersistImmediatelyAndChangeActualSurfacesWithoutChangingAccountOrMenu() {
        val original = repository.personalization
        val expected = Personalization(menuOrder = listOf("HOME","ACTIVITY","LIBRARY","DISCOVER","SETTINGS"), autoResume = false)
        var background = Color.Unspecified
        try {
            repository.personalization = expected
            rule.setContent { Television { background = MaterialTheme.colorScheme.background
                SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, {_,_->}) } }
            rule.onNodeWithTag("theme-choice-mood").performScrollTo().performClick()
            rule.onNodeWithTag("mood-MIDNIGHT").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(Color(VisualTheme.MIDNIGHT.background), background)
                assertEquals(expected.copy(visualTheme = VisualTheme.MIDNIGHT), repository.personalization)
            }
            rule.onNodeWithTag("theme-choice-accent").performScrollTo().performClick()
            rule.onNodeWithTag("accent-MINT").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-corners").performScrollTo().performClick()
            rule.onNodeWithTag("corners-CRISP").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-focus").performScrollTo().performClick()
            rule.onNodeWithTag("focus-BOLD").performScrollTo().performClick()
            rule.onNodeWithTag("theme-contrast").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(expected.copy(visualTheme = VisualTheme.MIDNIGHT, accent = AccentPalette.MINT,
                artworkCorners = ArtworkCorners.CRISP, focusStyle = FocusStyle.BOLD, highContrast = true), repository.personalization) }
            rule.onNodeWithTag("appearance-reset").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(expected.copy(focusStyle = FocusStyle.BOLD), repository.personalization) }
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
    @Test fun librarySettingsRemainInHomeAndMenuPanelContainsOnlyNavigationOptions() {
        var opened = false
        rule.setContent { Television { SettingsScreen(ReelstackUiState(connections = listOf(
            ServiceConnection(ServiceKind.JELLYFIN,"Fixture","https://example.com","fixture"))),
            PaddingValues(0.dp), {}, {}, {}, {_,_->}, onManageLibraries = { opened = true }) } }
        rule.onNodeWithTag("settings-category-HOME").performClick()
        rule.onNodeWithTag("library-manage").assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(opened) }
        rule.onNodeWithTag("settings-category-MENU").performClick().assertIsSelected()
        rule.onNodeWithTag("menu-option-HOME").assertIsDisplayed()
        rule.onNodeWithTag("show-next-up").assertDoesNotExist()
        rule.onNodeWithTag("theme-choice-mood").assertDoesNotExist()
    }
    @Test fun doubleTextSizeCanReachEveryCategoryAndThemeChoice() {
        rule.setContent { Television(2f) { SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, {_,_->}) } }
        rule.onNodeWithTag("settings-category-ABOUT").performScrollTo().performClick()
        rule.onNodeWithTag("settings-category-APPEARANCE").performScrollTo().performClick()
        rule.onNodeWithTag("theme-choice-focus").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithTag("focus-BOLD").performScrollTo().assertIsDisplayed()
    }
    @Test fun televisionMenuEditorReordersHidesAndProtectsRequiredPages() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization()
            rule.setContent { Television { SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, {_,_->}) } }
            rule.onNodeWithTag("settings-category-MENU").performClick()
            rule.onNodeWithTag("menu-option-ACTIVITY").performScrollTo().performClick()
            rule.onNodeWithTag("menu-up-ACTIVITY").performClick()
            rule.onNodeWithTag("menu-visible-ACTIVITY").performClick()
            rule.runOnIdle {
                assertEquals(listOf("HOME","LIBRARY","ACTIVITY","DISCOVER","SETTINGS"), repository.personalization.menuOrder)
                assertTrue("ACTIVITY" in repository.personalization.hiddenMenuItems)
                assertTrue(repository.personalization.visibleMenu().containsAll(listOf("HOME","SETTINGS")))
            }
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
}
