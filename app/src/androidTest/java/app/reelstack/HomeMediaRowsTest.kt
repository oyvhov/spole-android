package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
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
    fun calendarIsReachableWithoutScrollingAndEmptyPlaybackStaysQuiet() {
        var opened = false
        composeRule.setContent {
            ReelstackTheme {
                HomeScreen(
                    state = ReelstackUiState(connections = emptyList(), sessions = emptyList(),
                        homeSections = setOf(HomeSection.NOW_PLAYING)),
                    contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                    onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {},
                    onCalendarClick = { opened = true }, onRefresh = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Opne kalenderen").assertIsDisplayed().performClick()
        org.junit.Assert.assertTrue(opened)
        composeRule.onNodeWithText("Ingen aktive avspelingar").assertDoesNotExist()
        composeRule.onNodeWithText("Sjekkar avspelingar…").assertDoesNotExist()
        composeRule.onNodeWithText("Spelar no").assertDoesNotExist()
        composeRule.onNodeWithText("Reelune").assertIsDisplayed()
    }

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
            homeSections = setOf(HomeSection.JELLYFIN_MOVIES, HomeSection.EMBY_MOVIES),
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
                    onUpcomingClick = {},
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Jellyfin · Nyleg lagde til filmar")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Emby · Nyleg lagde til filmar")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun emptyConnectedLibraryUsesSkeletonWhileRefreshing() {
        val state = ReelstackUiState(
            connections = listOf(connection(ServiceKind.JELLYFIN)),
            sessions = emptyList(),
            recentMovies = emptyList(),
            recentSeries = emptyList(),
            upcoming = emptyList(),
            incoming = emptyList(),
            discover = emptyList(),
            activity = emptyList(),
            isRefreshing = true,
            homeSections = setOf(HomeSection.JELLYFIN_MOVIES),
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
                    onUpcomingClick = {},
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Lastar nyleg lagde til filmar frå Jellyfin")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ingen nyleg lagde til filmar.").assertDoesNotExist()
    }

    @Test
    fun everyServiceAndMediaTypeCanBeShownIndependently() {
        val selected = androidx.compose.runtime.mutableStateOf(HomeSection.EMBY_MOVIES)
        val labels = mapOf(
            HomeSection.EMBY_MOVIES to "Emby · Nyleg lagde til filmar",
            HomeSection.EMBY_SERIES to "Emby · Nyleg lagde til seriar",
            HomeSection.JELLYFIN_MOVIES to "Jellyfin · Nyleg lagde til filmar",
            HomeSection.JELLYFIN_SERIES to "Jellyfin · Nyleg lagde til seriar",
        )
        composeRule.setContent {
            ReelstackTheme {
                HomeScreen(
                    state = ReelstackUiState(connections = listOf(connection(ServiceKind.EMBY), connection(ServiceKind.JELLYFIN)), homeSections = setOf(selected.value)),
                    contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                    onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {}, onRefresh = {},
                )
            }
        }
        labels.forEach { (section, label) ->
            composeRule.runOnIdle { selected.value = section }
            composeRule.onNodeWithContentDescription(label).performScrollTo().assertIsDisplayed()
            labels.filterKeys { it != section }.values.forEach { hidden -> composeRule.onNodeWithContentDescription(hidden).assertDoesNotExist() }
        }
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
