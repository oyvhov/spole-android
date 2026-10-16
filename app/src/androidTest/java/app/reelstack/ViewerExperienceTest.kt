package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

class ViewerExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://test.example", "session", sessionCookie = true)
    @Test fun multiplePlaybackHasExplicitCountAndRestoredBrandHeader() {
        val demo = ReelstackUiState()
        rule.setContent { ReelstackTheme { HomeScreen(demo.copy(sessions = listOf(demo.sessions.first(), demo.sessions.first().copy(sessionId = "second")),
            homeSections = setOf(HomeSection.NOW_PLAYING)), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Spole").assertIsDisplayed()
        rule.onNodeWithText("2 avspelingar").assertIsDisplayed()
    }
}
