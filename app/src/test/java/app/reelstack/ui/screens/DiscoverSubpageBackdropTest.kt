package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.FocusDetails
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The pages under Oppdag show the picture of what has focus, as Oppdag itself does. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DiscoverSubpageBackdropTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun keyboard(content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        ReelstackTheme { content() }
    }

    @Test fun yourRequestsShowThePictureOfTheOneInFocus() {
        val request = TrackedRequest("r", 9, "movie", "Coraline", null, setOf())
        val asked = mutableListOf<String>()
        val state = ReelstackUiState(trackedRequests = listOf(request), discoverHub = DiscoverHubState(
            focusDetails = mapOf("seerr-movie-9" to FocusDetails(listOf("2009"), null, "https://image.tmdb.org/t/p/w1280/c.jpg"))))
        keyboard { MyRequestsScreen(state, PaddingValues(), onOpen = {}, onBack = {}, onLoadOlder = {},
            onFocusRequest = { asked += it.key }) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-requests-tab-ALL").assertIsFocused() }.isSuccess }
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNodeWithTag("my-request-r").assertIsFocused()
        rule.onNodeWithTag("discover-focus-backdrop").assertExists()
        assertEquals(listOf("r"), asked)
    }

    @Test fun anAllPageShowsThePictureOfThePosterInFocus() {
        val media = DiscoverMedia("seerr-movie-1", "Tittel", "2026", R.drawable.media_placeholder, false, remoteId = 1,
            mediaType = "movie", backdropUrl = "https://image.tmdb.org/t/p/w1280/t.jpg")
        keyboard { DiscoverGridContent("Populært no", DiscoverRowState(listOf(media)), PaddingValues(), {}, {}, {}) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-backdrop").assertExists()
    }

    @Test fun anAllPageNamesItsPosters() {
        val media = DiscoverMedia("seerr-movie-1", "Tittel", "2026", R.drawable.media_placeholder, false, remoteId = 1, mediaType = "movie")
        keyboard { DiscoverGridContent("Populært no", DiscoverRowState(listOf(media)), PaddingValues(), {}, {}, {}) }
        rule.onNode(hasText("Tittel") and hasAnyAncestor(hasTestTag("discover-grid-all")), useUnmergedTree = true).assertExists()
    }
}
