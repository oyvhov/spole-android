package app.reelstack.ui.screens

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.state.GlobalSearchUiState
import app.reelstack.ui.state.SearchSlice
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvSearchRemoteTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun downFromTheSearchFieldReachesTheResults() {
        val owned = LibraryMedia(id = "jf-1", title = "Trolljegeren", subtitle = "2010", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                GlobalSearchScreen(state = GlobalSearchUiState(search = SearchSlice(query = "troll", libraryResults = listOf(owned))),
                    onQuery = {}, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {}, onLoadMore = {})
            }
        }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("global-search-field").assertIsFocused() }.isSuccess }
        // The remote's down: out of the field, past the clear button, to the first title.
        repeat(2) {
            rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
            rule.waitForIdle()
        }
        rule.onNodeWithTag("library-hit-jf-1").assertIsFocused()
    }

    @Test fun aTitleUnderInYourLibrariesDoesNotSaySoAgain() {
        val owned = LibraryMedia(id = "jf-1", title = "Trolljegeren", subtitle = "2010", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
        rule.setContent { ReelstackTheme {
            GlobalSearchScreen(state = GlobalSearchUiState(search = SearchSlice(query = "troll", libraryResults = listOf(owned))),
                onQuery = {}, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {}, onLoadMore = {})
        } }
        val badge = rule.activity.getString(R.string.filter_available)
        rule.onAllNodes(hasText(badge) and hasAnyAncestor(hasTestTag("library-hit-jf-1")), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun focusStaysOnTheResultsWhileTheyScroll() {
        val titles = List(30) { LibraryMedia(id = "jf-$it", title = "Troll $it", subtitle = "2010", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "$it", mediaType = "Movie") }
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                GlobalSearchScreen(state = GlobalSearchUiState(search = SearchSlice(query = "troll", libraryResults = titles)),
                    onQuery = {}, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {}, onLoadMore = {})
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("library-hit-jf-0").requestFocus()
        // Down a few rows: the grid scrolls, and the remote must still be on a title.
        repeat(3) {
            rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
            rule.waitForIdle()
        }
        rule.onNode(isFocused()).assert(hasTestTag("library-hit-jf-18") or hasTestTag("library-hit-jf-12") or hasTestTag("library-hit-jf-24")
            or hasAnyAncestor(hasTestTag("global-search-results")))
    }

    @Test fun aTitleToAskForIsNamedUnderItsPoster() {
        val wanted = app.reelstack.data.model.DiscoverMedia("seerr-movie-7", "Neuromancer", "2027", R.drawable.media_placeholder, false,
            remoteId = 7, mediaType = "movie", seerrStatus = 1)
        rule.setContent { ReelstackTheme {
            GlobalSearchScreen(state = GlobalSearchUiState(search = SearchSlice(query = "neuro", results = listOf(wanted))),
                onQuery = {}, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {}, onLoadMore = {})
        } }
        // There is no header on this page to name the poster in focus, so the name is under it.
        rule.onNode(hasText("Neuromancer") and hasAnyAncestor(hasTestTag("global-search-results")), useUnmergedTree = true).assertExists()
    }
}
