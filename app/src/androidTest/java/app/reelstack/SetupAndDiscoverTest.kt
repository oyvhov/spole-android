package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.DiscoverScreen
import app.reelstack.ui.screens.WelcomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SetupAndDiscoverTest {
    @get:Rule val rule = createComposeRule()

    @Test fun firstRunOffersServicesAndExplicitPreview() {
        var selected: ServiceKind? = null
        rule.setContent {
            ReelstackTheme {
                WelcomeScreen(ReelstackUiState(), onConnect = { selected = it }, onContinue = {})
            }
        }
        rule.onNodeWithText("Alt du ser.\nÉin stad.").assertIsDisplayed()
        rule.onNodeWithText("Jellyfin").performScrollTo().performClick()
        assertEquals(ServiceKind.JELLYFIN, selected)
        rule.onNodeWithText("Opne oversikta mi").assertDoesNotExist()
        rule.onRoot().performTouchInput { swipeUp() }
        rule.onNodeWithText("Utforsk med demodata først").assertIsDisplayed()
    }

    @Test fun connectedSetupOffersHomeInsteadOfDemo() {
        rule.setContent {
            ReelstackTheme {
                WelcomeScreen(ReelstackUiState(connections = listOf(
                    ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "cookie", sessionCookie = true),
                )), onConnect = {}, onContinue = {})
            }
        }
        rule.onRoot().performTouchInput { swipeUp() }
        rule.onNodeWithText("Opne oversikta mi").assertIsDisplayed()
        rule.onNodeWithText("Utforsk med demodata først").assertDoesNotExist()
    }

    @Test fun discoverFiltersChangeResultsAndDetailActionOpensTheTitle() {
        var opened: String? = null
        val movies = listOf(
            DiscoverMedia("film", "Testfilmen", "Film · 2026", R.drawable.media_placeholder, true, mediaType = "movie"),
            DiscoverMedia("series", "Testserien", "Serie · 2025", R.drawable.media_placeholder, false, mediaType = "tv"),
        )
        rule.setContent {
            ReelstackTheme {
                DiscoverScreen(ReelstackUiState(discover = movies), PaddingValues(0.dp), {}, {}, { opened = it })
            }
        }
        rule.onNodeWithText("Filmar").performClick()
        rule.onNodeWithText("Testserien").assertDoesNotExist()
        rule.onNodeWithTag("discover-cover-film").performScrollTo().performClick()
        assertEquals("film", opened)
        rule.onNodeWithText("Seriar").performClick()
        rule.onNodeWithText("Testfilmen").assertDoesNotExist()
        rule.onNodeWithText("Testserien").assertIsDisplayed()
    }
}
