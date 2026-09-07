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

    @Test fun personalActivityCanSeparateReadyTitlesFromRequestsInProgress() {
        rule.setContent {
            ReelstackTheme {
                ActivityScreen(ReelstackUiState(connections = listOf(
                    ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "fixture", sessionCookie = true)),
                    trackedRequests = listOf(
                        TrackedRequest("ready", 1, "movie", "Klar for filmkveld", null, emptySet(), notify = false, stage = RequestStage.AVAILABLE),
                        TrackedRequest("pending", 2, "movie", "På veg heim", null, emptySet(), notify = false, stage = RequestStage.REQUESTED),
                    )), PaddingValues(0.dp), {})
            }
        }
        rule.onNodeWithText("Klare · 1").performScrollTo().performClick()
        rule.onNodeWithText("Klar for filmkveld").assertIsDisplayed()
        rule.onNodeWithText("På veg heim").assertDoesNotExist()
        rule.onNodeWithContentDescription("Framdrift: I biblioteket").assertDoesNotExist()
        rule.onNodeWithText("På veg · 1").performClick()
        rule.onNodeWithText("På veg heim").assertIsDisplayed()
        rule.onNodeWithText("Klar for filmkveld").assertDoesNotExist()
        rule.onNodeWithContentDescription("Framdrift: Førespurd").assertIsDisplayed()
    }

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
        rule.onNodeWithTag("activity-scope").performClick()
        rule.onNodeWithText("Radarr").performClick()
        rule.onNodeWithText("Film A").assertDoesNotExist()
        rule.onNodeWithText("Film B").performClick()
        assertEquals("two", opened)
        rule.onNodeWithTag("activity-scope").performClick()
        rule.onNodeWithText("Sonarr").performClick()
        rule.onNodeWithText("Ingen hendingar her enno").assertIsDisplayed()
        rule.onNodeWithTag("activity-scope").performClick()
        rule.onNodeWithText("Alt").performClick()
        rule.onNodeWithText("Film A").assertIsDisplayed()
    }

    @Test fun settingsToggleHasOneActionAndShowsLibraryNotifications() {
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
        rule.onNodeWithText("Emby · Filmar").assertDoesNotExist()
        rule.onNodeWithText("Tilpass framsida").performScrollTo().performClick()
        rule.onNodeWithText("Emby · Filmar").performScrollTo().performClick()
        assertEquals(1, changes)
        assertEquals(HomeSection.EMBY_MOVIES, changedSection)
        rule.onNodeWithText("Bibliotekvarsel").performScrollTo().assertIsDisplayed()
    }
}
