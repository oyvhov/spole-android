package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.ActivityScreen
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiConsistencyTest {
    @get:Rule val rule = createComposeRule()

    @Test fun activityFiltersSourceAndOpensExactTitle() {
        var opened = ""
        rule.setContent {
            ReelstackTheme {
                ActivityScreen(ReelstackUiState(activity = listOf(
                    ActivityEvent("one", "Film A", "Ventar på godkjenning", "No", source = ServiceKind.SEERR),
                    ActivityEvent("two", "Film B", "Lastar ned", "No", source = ServiceKind.RADARR),
                )), PaddingValues(0.dp), { opened = it })
            }
        }
        rule.onNodeWithText("Radarr").performClick()
        rule.onNodeWithText("Film A").assertDoesNotExist()
        rule.onNodeWithText("Film B").performClick()
        assertEquals("two", opened)
        rule.onNodeWithText("Sonarr").performClick()
        rule.onNodeWithText("Ingen hendingar her enno").assertIsDisplayed()
        rule.onNodeWithText("Alt").performClick()
        rule.onNodeWithText("Film A").assertIsDisplayed()
    }

    @Test fun settingsToggleHasOneActionAndDoesNotPromiseUnbuiltNotifications() {
        var changes = 0
        var changedSection: HomeSection? = null
        rule.setContent {
            ReelstackTheme {
                SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { section, _ ->
                    changes++
                    changedSection = section
                })
            }
        }
        rule.onNodeWithText("Emby · Filmar").performScrollTo().performClick()
        assertEquals(1, changes)
        assertEquals(HomeSection.EMBY_MOVIES, changedSection)
        rule.onNodeWithText("Kjem seinare. Sjå oppdateringar under Aktivitet.").performScrollTo().assertIsDisplayed()
    }
}
