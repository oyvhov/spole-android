package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

class HomeMediaRowsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun jellyfinAndEmbyRenderAsSeparateLibraryRows() {
        val state = ReelstackUiState(
            connections = listOf(
                connection(ServiceKind.JELLYFIN),
                connection(ServiceKind.EMBY),
            ),
            sessions = emptyList(),
            recentMovies = listOf(
                movie("jellyfin-movie", "Jellyfin movie", ServiceKind.JELLYFIN),
                movie("emby-movie", "Emby movie", ServiceKind.EMBY),
            ),
            recentSeries = emptyList(),
            upcoming = emptyList(),
            incoming = emptyList(),
            discover = emptyList(),
            activity = emptyList(),
            homeSections = setOf(HomeSection.RECENT_MOVIES),
        )

        composeRule.setContent {
            ReelstackTheme {
                HomeScreen(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    onSessionClick = {},
                    onPlaybackToggle = {},
                    onMediaClick = {},
                    onLibraryClick = {},
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithText("Jellyfin · Nyleg lagde til filmar")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Emby · Nyleg lagde til filmar")
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun connection(kind: ServiceKind) = ServiceConnection(
        kind = kind,
        name = kind.displayName,
        baseUrl = "https://${kind.name.lowercase()}.example.com",
        token = "test-token",
        state = ConnectionState.CONNECTED,
    )

    private fun movie(id: String, title: String, source: ServiceKind) = LibraryMedia(
        id = id,
        title = title,
        subtitle = "Film",
        artworkRes = R.drawable.media_placeholder,
        source = source,
    )
}
