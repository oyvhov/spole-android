package app.reelstack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    private fun host(fontScale: Float = 1f, onDownloads: () -> Unit = {}) {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                    val phone = android.content.res.Configuration(androidx.compose.ui.platform.LocalConfiguration.current).apply {
                        uiMode = (uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK.inv()) or android.content.res.Configuration.UI_MODE_TYPE_NORMAL
                    }
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides phone) {
                    ReelstackTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { _, _ -> }, onOpenDownloads = onDownloads)
                    } }
                    }
                }
            }
        }
    }
    private fun checkOverviewAlignment(fontScale: Float) {
        var opened = false
        host(fontScale) { opened = true }
        val rows = listOf("ACCOUNTS", "APPEARANCE", "HOME", "PLAYBACK", "UPDATES")
            .map { "settings-category-$it" } + "settings-downloads" + "settings-category-ABOUT"
        val columns = rows.map { tag ->
            rule.onNodeWithTag(tag).performScrollTo()
            listOf("icon", "text", "chevron").map { column ->
                rule.onNode(hasTestTag("settings-overview-$column") and hasAnyAncestor(hasTestTag(tag)),
                    useUnmergedTree = true).getUnclippedBoundsInRoot().left
            }
        }
        columns.forEach { assertEquals("Overview rows must share one grid", columns.first(), it) }
        rule.onNodeWithTag("settings-downloads").performScrollTo().assertIsDisplayed()
        val updates = rule.onNodeWithTag("settings-category-UPDATES").getUnclippedBoundsInRoot()
        val downloads = rule.onNodeWithTag("settings-downloads").getUnclippedBoundsInRoot()
        val about = rule.onNodeWithTag("settings-category-ABOUT").getUnclippedBoundsInRoot()
        assertTrue(downloads.top >= updates.bottom)
        assertTrue(about.top >= downloads.bottom)
        saveTvReview("settings-index-$fontScale", rule.onNodeWithTag("settings-feed").captureToImage())
        rule.onNodeWithTag("settings-downloads").performClick()
        assertTrue(opened)
    }
    @Test fun overviewRowsAlignAndDownloadsFollowUpdates() = checkOverviewAlignment(1f)
    @Test fun largeTextOverviewRowsAlignAndDownloadsRemainReachable() = checkOverviewAlignment(2f)
    @Test fun overviewOpensOnlyOneGroupAndBackReturnsToCategories() {
        host()
        rule.onNodeWithTag("theme-choice-season").assertDoesNotExist()
        rule.onNodeWithTag("auto-resume").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-APPEARANCE").performClick()
        rule.onNodeWithTag("theme-choice-season").assertIsDisplayed()
        rule.onNodeWithTag("auto-resume").assertDoesNotExist()
        rule.onNodeWithTag("settings-back").performClick()
        rule.onNodeWithTag("settings-category-PLAYBACK").performScrollTo().performClick()
        rule.onNodeWithTag("auto-resume").assertIsDisplayed()
        rule.onNodeWithTag("theme-choice-season").assertDoesNotExist()
        rule.onNodeWithTag("settings-back").performClick()
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
            rule.onNodeWithTag("brand-season-CHRISTMAS").assertExists()
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-HALLOWEEN").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(VisualTheme.HALLOWEEN, repository.personalization.visualTheme)
                assertEquals(AccentPalette.PUMPKIN, repository.personalization.accent)
            }
            rule.onNodeWithTag("brand-season-HALLOWEEN").assertExists()
            // «Litt» keeps the mark's ghost; «Av» leaves the colours alone.
            rule.onNodeWithTag("theme-choice-decor").performScrollTo().performClick()
            rule.onNodeWithTag("decor-CALM").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(SeasonalDecor.CALM, repository.personalization.seasonalDecor) }
            rule.onNodeWithTag("brand-season-HALLOWEEN").assertExists()
            rule.onNodeWithTag("theme-choice-decor").performScrollTo().performClick()
            rule.onNodeWithTag("decor-OFF").performScrollTo().performClick()
            rule.onNodeWithTag("brand-season-HALLOWEEN").assertDoesNotExist()
            rule.runOnIdle { assertEquals(VisualTheme.HALLOWEEN, repository.personalization.visualTheme) }
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-NONE").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-decor").assertDoesNotExist()
            rule.runOnIdle { assertEquals(VisualTheme.FOREST, repository.personalization.visualTheme) }
        } finally { rule.runOnIdle { repository.personalization = original } }
    }

    /** The calendar keeps the stored colours: whatever it lays over them is never written back. */
    @Test fun followingTheCalendarKeepsTheChosenColoursStored() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization(visualTheme = VisualTheme.MIDNIGHT, accent = AccentPalette.OCEAN)
            host()
            rule.onNodeWithTag("settings-category-APPEARANCE").performClick()
            rule.onNodeWithTag("theme-choice-season").performScrollTo().performClick()
            rule.onNodeWithTag("season-CALENDAR").performScrollTo().performClick()
            rule.runOnIdle {
                assertTrue(repository.personalization.seasonCalendar)
                assertEquals(VisualTheme.MIDNIGHT, repository.personalization.visualTheme)
                assertEquals(AccentPalette.OCEAN, repository.personalization.accent)
            }
            rule.onNodeWithTag("season-calendar-note").performScrollTo().assertIsDisplayed()
            rule.onNodeWithTag("theme-choice-decor").performScrollTo().assertIsDisplayed()
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
    @Test fun doubleTextCanReachAllGroupsAndChangePlaybackTime() {
        val original = repository.personalization
        try {
            repository.personalization = Personalization()
            host(2f)
            for (name in listOf("ACCOUNTS", "APPEARANCE", "HOME", "PLAYBACK", "UPDATES", "ABOUT")) {
                rule.onNodeWithTag("settings-category-$name").performScrollTo().assertIsDisplayed().performClick()
                rule.onNodeWithTag("settings-back").assertIsDisplayed().performClick()
            }
            rule.onNodeWithTag("settings-category-PLAYBACK").performScrollTo().performClick()
            rule.onNodeWithTag("theme-choice-next-episode-lead").performScrollTo().performClick()
            rule.onNodeWithTag("next-episode-lead-120").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(120, repository.personalization.nextEpisodeLeadSeconds) }
        } finally { rule.runOnIdle { repository.personalization = original } }
    }
}
