package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A refresh must not rebuild Home. Rows that already loaded — empty or not — keep their shape while
 * the next answer is on its way; only a row that has never loaded waits in a skeleton.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class HomeRefreshStabilityTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun refreshing(loaded: Set<ServiceKind>) = ReelstackUiState(
        connections = listOf(ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token", userId = "u")),
        isRefreshing = true,
        loadedSources = loaded,
        lastUpdatedEpochMillis = if (loaded.isEmpty()) null else 1_000L,
        sessions = emptyList(), resume = emptyList(), nextUp = emptyList(), favourites = emptyList(),
        recentMovies = emptyList(), recentSeries = emptyList(), upcoming = emptyList(), recentReleases = emptyList(),
        incoming = emptyList(), discover = emptyList(), recommendations = emptyList(), activity = emptyList(),
    )

    private fun show(state: ReelstackUiState) = rule.setContent {
        ReelstackTheme {
            HomeScreen(state = state, contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {}, onRefresh = {})
        }
    }

    @Test fun aRefreshOfALoadedEmptyRowKeepsItsLineInsteadOfASkeleton() {
        show(refreshing(setOf(ServiceKind.JELLYFIN)))
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.home_loading_movies, "Jellyfin")).assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.home_no_movies)).performScrollTo()
        // An empty resume list has no row at all, during a refresh as much as after it.
        rule.onNodeWithTag("continue-watching-JELLYFIN").assertDoesNotExist()
    }

    @Test fun theFirstLoadStillWaitsInSkeletons() {
        show(refreshing(emptySet()))
        rule.onNodeWithTag("continue-watching-JELLYFIN").assertExists()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.home_loading_movies, "Jellyfin")).performScrollTo()
    }
}
