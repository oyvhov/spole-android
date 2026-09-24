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
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class UiConsistencyTest {
    @get:Rule val rule = createComposeRule()

    @Test fun personalActivityShowsReadyTitlesAndRequestsInProgress() {
        rule.setContent {
            ReelstackTheme {
                androidx.compose.runtime.key("personal-activity-filter") {
                    ActivityScreen(ReelstackUiState(connections = listOf(
                        ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "fixture", sessionCookie = true)),
                        accounts = mapOf(ServiceKind.SEERR to ServiceAccount(ServiceKind.SEERR, "me", "Testperson")),
                        trackedRequests = listOf(
                            TrackedRequest("ready", 1, "movie", "Klar for filmkveld", null, emptySet(), notify = false, stage = RequestStage.AVAILABLE),
                            TrackedRequest("pending", 2, "movie", "På veg heim", null, emptySet(), notify = false, stage = RequestStage.REQUESTED),
                        )), PaddingValues(0.dp), {})
                }
            }
        }
        rule.onNodeWithTag("activity-personal-ALL").performScrollTo().performClick()
        rule.onNodeWithText("Klar for filmkveld").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("På veg heim").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Framdrift: Førespurd").assertIsDisplayed()
    }

    @Test fun activityFiltersSourceAndOpensExactTitle() {
        var opened = ""
        rule.setContent {
            ReelstackTheme {
                ActivityScreen(ReelstackUiState(activity = listOf(
                    ActivityEvent("one", "Film A", app.reelstack.localization.LocalizedText(app.reelstack.R.string.stage_requested), app.reelstack.localization.LocalizedText(app.reelstack.R.string.time_now), source = ServiceKind.SEERR),
                    ActivityEvent("two", "Film B", app.reelstack.localization.LocalizedText(app.reelstack.R.string.stage_downloading), app.reelstack.localization.LocalizedText(app.reelstack.R.string.time_now), source = ServiceKind.RADARR),
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

    @OptIn(ExperimentalTestApi::class)
    @Test fun settingsToggleHasOneActionAndShowsLibraryNotifications() {
        val changes = mutableListOf<app.reelstack.data.model.HomeLayout>()
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(412.dp, 900.dp))) {
              ReelstackTheme {
                SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { _, _ -> },
                    homeEditor = app.reelstack.ui.screens.HomeEditorActions(onLayoutChange = { changes += it }))
              }
            }
        }
        rule.onNodeWithText("Emby · Filmar").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-HOME").performScrollTo().performClick()
        rule.onNodeWithTag("home-layout-open").performScrollTo().performClick()
        rule.onNodeWithTag("home-layout-list").performScrollToNode(hasTestTag("home-layout-visible-NEW_MOVIES:EMBY"))
        rule.onNodeWithTag("home-layout-visible-NEW_MOVIES:EMBY").performClick()
        assertEquals(1, changes.size)
        assertFalse(changes.single().isVisible(app.reelstack.data.model.HomeRowKey(
            app.reelstack.data.model.HomeRowKind.NEW_MOVIES, app.reelstack.data.model.ServiceKind.EMBY)))
        rule.onNodeWithTag("home-layout-done").performClick()
        rule.onNodeWithTag("settings-back").performClick()
        rule.onNodeWithTag("settings-category-UPDATES").performScrollTo().performClick()
        rule.onNodeWithText("Bibliotekvarsel").performScrollTo().assertIsDisplayed()
    }
}
