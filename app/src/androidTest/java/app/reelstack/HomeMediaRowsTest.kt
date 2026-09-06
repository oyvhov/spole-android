package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
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
    fun calendarRemainsInUpcomingButNotInHeaderAndEmptyPlaybackStaysHidden() {
        var opened = false
        composeRule.setContent {
            ReelstackTheme {
                HomeScreen(
                    state = ReelstackUiState(connections = emptyList(), sessions = emptyList(),
                        homeSections = setOf(HomeSection.NOW_PLAYING, HomeSection.UPCOMING)),
                    contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                    onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {},
                    onCalendarClick = { opened = true }, onRefresh = {},
                )
            }
        }
        composeRule.onNodeWithContentDescription("Opne kalenderen").assertDoesNotExist()
        composeRule.onNodeWithText("Kalender").performScrollTo().assertIsDisplayed().performClick()
        org.junit.Assert.assertTrue(opened)
        composeRule.onNodeWithText("Ingen aktive avspelingar").assertDoesNotExist()
        composeRule.onNodeWithText("Sjekkar avspelingar…").assertDoesNotExist()
        composeRule.onNodeWithText("Spelar no").assertDoesNotExist()
        composeRule.onNodeWithText("Spole").assertIsDisplayed()
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

        composeRule.onNodeWithContentDescription("Nye filmar · Jellyfin")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Nye filmar · Emby")
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
            HomeSection.EMBY_MOVIES to "Nye filmar · Emby",
            HomeSection.EMBY_SERIES to "Nye episodar · Emby",
            HomeSection.JELLYFIN_MOVIES to "Nye filmar · Jellyfin",
            HomeSection.JELLYFIN_SERIES to "Nye episodar · Jellyfin",
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

    @Test fun shortEpisodeTitleDoesNotReserveBlankLineBecauseAnotherTitleIsLong() {
        val state = ReelstackUiState(
            connections = listOf(connection(ServiceKind.EMBY)),
            recentMovies = emptyList(),
            recentSeries = listOf(
                episode("short", "Lioness", "S03 E06 · Sugar Land"),
                episode("long", "Dette er ein mykje lengre serietittel", "S01 E01 · Starten"),
            ),
            sessions = emptyList(), upcoming = emptyList(), incoming = emptyList(),
            homeSections = setOf(HomeSection.EMBY_SERIES),
        )
        composeRule.setContent {
            ReelstackTheme { HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Lioness").performScrollTo()
        val title = composeRule.onNodeWithText("Lioness").fetchSemanticsNode().boundsInRoot
        val episode = composeRule.onNodeWithText("S03 E06 · Sugar Land").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Episode should follow the one-line title closely", episode.top - title.bottom < 16f)
    }

    @Test fun repeatedThirdPartyQueueIdsCannotCrashActiveDownloads() {
        val state = ReelstackUiState(
            connections = listOf(connection(ServiceKind.SONARR)), adminView = true,
            sessions = emptyList(), recentMovies = emptyList(), recentSeries = emptyList(), upcoming = emptyList(),
            incoming = listOf(
                IncomingMedia("same-download", "Episode 1", ServiceKind.SONARR, "Lastar ned 40 %", IncomingState.DOWNLOADING, R.drawable.media_placeholder, progress = 40),
                IncomingMedia("same-download", "Episode 2", ServiceKind.SONARR, "Lastar ned 40 %", IncomingState.DOWNLOADING, R.drawable.media_placeholder, progress = 40),
            ),
            homeSections = setOf(HomeSection.DOWNLOADS),
        )

        composeRule.setContent {
            ReelstackTheme { HomeScreen(state, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithText("Nedlastingar").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Lastar ned 40 %").assertCountEquals(2)
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

    private fun episode(id: String, title: String, subtitle: String) = LibraryMedia(
        id = id, title = title, subtitle = subtitle, artworkRes = R.drawable.media_placeholder,
        source = ServiceKind.EMBY, mediaType = "Episode",
    )
}
