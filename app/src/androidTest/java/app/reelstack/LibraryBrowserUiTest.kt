package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.LibraryScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LibraryBrowserUiTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example", "token", userId = "me")
    @Test fun foldersOpenAndPaginationRequestsTheNextPage() {
        var opened = ""
        var more = false
        val state = ReelstackUiState(connections = listOf(connection), libraryHasMore = true,
            libraryEntries = listOf(RemoteLibraryItem("movies", "Filmar", "", null, "CollectionFolder", null, isFolder = true)))
        rule.setContent { ReelstackTheme { LibraryScreen(state, { more = it }, { opened = it }, {}) } }
        rule.onNodeWithTag("library-item-movies").performClick()
        rule.runOnIdle { assertEquals("movies", opened) }
        rule.onNodeWithText("Last fleire").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(more) }
    }
    @Test fun pageFailureKeepsExistingItemsAndRetriesTheSamePage() {
        var more = false
        val state = ReelstackUiState(connections = listOf(connection), libraryHasMore = true, libraryError = "Offline",
            libraryEntries = listOf(RemoteLibraryItem("movies", "Filmar", "", null, "CollectionFolder", null, isFolder = true)))
        rule.setContent { ReelstackTheme { LibraryScreen(state, { more = it }, {}, {}) } }
        rule.onNodeWithTag("library-item-movies").assertIsDisplayed()
        rule.onNodeWithText("Offline").assertIsDisplayed()
        rule.onNodeWithText("Prøv igjen").performClick()
        rule.runOnIdle { assertTrue(more) }
    }
    @Test fun librarySelectionSavesNoneOnlyAfterExplicitSave() {
        var saved: Set<String>? = null
        val state = ReelstackUiState(connections = listOf(connection), libraryChoicesOpen = true,
            libraryChoices = listOf(app.reelstack.data.network.RemoteLibraryView("movies", "Filmar", "movies")),
            selectedLibraryIds = setOf("movies"))
        rule.setContent { ReelstackTheme { app.reelstack.ui.screens.LibraryChoicesDialog(state, {}, {}, { ids, _ -> saved = ids }) } }
        rule.onNodeWithTag("library-choice-movies").assertIsOn().performClick().assertIsOff()
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithTag("library-selection-save").performClick()
        rule.runOnIdle { assertEquals(emptySet<String>(), saved) }
    }
}
