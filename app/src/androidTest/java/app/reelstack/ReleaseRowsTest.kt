package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReleaseRowsTest {
    @get:Rule val rule = createComposeRule()
    private fun state() = ReelstackUiState(
        connections = listOf(
            ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "fixture", sessionCookie = true),
            ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example", "fixture", userId = "viewer"),
        ),
        adminView = false, sessions = emptyList(), recentMovies = emptyList(), recentSeries = emptyList(),
        upcoming = emptyList(), recentReleases = emptyList(),
        homeSections = setOf(HomeSection.RECENT_RELEASES, HomeSection.UPCOMING),
    )
    @Test fun personalViewerCanOpenVerifiedRecentTitleWithoutQueueConnections() {
        var opened: String? = null
        val media = UpcomingMedia("emby-film", "Ny lokal film", "Film · 2026", "I går", 1,
            R.drawable.media_placeholder, ServiceKind.EMBY, mediaType = "Movie")
        rule.setContent { ReelstackTheme { HomeScreen(
            state().copy(recentReleases = listOf(media)), PaddingValues(0.dp), {}, {}, {}, {}, { opened = it }, onRefresh = {},
        ) } }
        rule.onNodeWithText("Ny lokal film").performScrollTo().performClick()
        assertEquals("emby-film", opened)
        rule.onNodeWithText("Kalenderkjelda er ikkje kopla til enno.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Nedlastingar").assertDoesNotExist()
    }
    @Test fun failedReleaseLookupIsNotShownAsAValidEmptyLibrary() {
        rule.setContent { ReelstackTheme { HomeScreen(
            state().copy(recentReleasesError = "Fekk ikkje henta utgjevingsdatoar."), PaddingValues(0.dp), {}, {}, {}, {}, {}, onRefresh = {},
        ) } }
        rule.onNodeWithText("Fekk ikkje henta utgjevingsdatoar.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Ingen nye digitale filmutgjevingar eller episodar i biblioteka dine dei siste 28 dagane.").assertDoesNotExist()
    }
}
