package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.ActivityScreen
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

class ViewerExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://test.example", "session", sessionCookie = true)
    @Test fun ordinaryActivityShowsOnlyPersonalItemsWithoutAdminFilters() {
        rule.setContent { ReelstackTheme { ActivityScreen(ReelstackUiState(connections = listOf(connection), adminView = false,
            activity = listOf(ActivityEvent("other", "Other user's title", "", "", source = ServiceKind.RADARR)),
            trackedRequests = listOf(TrackedRequest("mine", 1, "movie", "My title", null, emptySet()))), PaddingValues(0.dp), {}) } }
        rule.onNodeWithText("My title").assertIsDisplayed()
        rule.onNodeWithText("Other user's title").assertDoesNotExist()
        rule.onNodeWithText("Alt").assertDoesNotExist()
        rule.onNodeWithText("Radarr").assertDoesNotExist()
        rule.onNodeWithText("Mine").assertDoesNotExist()
    }
    @Test fun verifiedAdminHasOverviewFilters() {
        rule.setContent { ReelstackTheme { ActivityScreen(ReelstackUiState(connections = listOf(connection), adminView = true), PaddingValues(0.dp), {}) } }
        rule.onNodeWithText("Alt").assertIsDisplayed()
        rule.onNodeWithText("Radarr").assertIsDisplayed()
    }
    @Test fun multiplePlaybackHasExplicitCountAndRestoredBrandHeader() {
        val demo = ReelstackUiState()
        rule.setContent { ReelstackTheme { HomeScreen(demo.copy(sessions = listOf(demo.sessions.first(), demo.sessions.first().copy(sessionId = "second")),
            homeSections = setOf(HomeSection.NOW_PLAYING)), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Reelune").assertIsDisplayed()
        rule.onNodeWithText("2 avspelingar").assertIsDisplayed()
    }
}
