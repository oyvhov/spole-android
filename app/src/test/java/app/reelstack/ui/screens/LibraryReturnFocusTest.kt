package app.reelstack.ui.screens

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class LibraryReturnFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Heim", "https://media.example", "token", userId = "me")
    private val libraries = listOf(
        RemoteLibraryItem("movies", "Filmar", "", null, "CollectionFolder", "movies", isFolder = true),
        RemoteLibraryItem("shows", "Seriar", "", null, "CollectionFolder", "tvshows", isFolder = true))

    @Test fun backFromALibraryReturnsTheRemoteToItsTile() {
        var state by mutableStateOf(ReelstackUiState(connections = listOf(jellyfin), libraryEntries = libraries))
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                LibraryScreen(state, {}, onOpen = { id -> state = state.copy(libraryPath = listOf(id to "Seriar"), libraryEntries = emptyList()) },
                    onBack = { state = state.copy(libraryPath = emptyList(), libraryEntries = libraries) })
            }
        }
        rule.onNodeWithTag("hub-library-shows").requestFocus()
        rule.onNodeWithTag("hub-library-shows").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionCenter) }
        rule.waitForIdle()
        rule.onNodeWithTag("hub-library-shows").assertDoesNotExist()
        rule.runOnIdle { state = state.copy(libraryPath = emptyList(), libraryEntries = libraries) }
        rule.waitForIdle()
        rule.onNodeWithTag("hub-library-shows").assertIsFocused()
    }

    @Test fun backFromBrowseAllReturnsToBrowseAll() {
        var state by mutableStateOf(ReelstackUiState(connections = listOf(jellyfin), libraryEntries = libraries,
            libraryPeeks = mapOf("shows" to listOf(app.reelstack.data.model.LibraryMedia(id = "jf-1", title = "Serie",
                subtitle = "2026", artworkRes = app.reelstack.R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
                remoteId = "1", mediaType = "Series")))))
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                LibraryScreen(state, {}, onOpen = { id -> state = state.copy(libraryPath = listOf(id to "Seriar"), libraryEntries = emptyList()) },
                    onBack = { state = state.copy(libraryPath = emptyList(), libraryEntries = libraries) })
            }
        }
        rule.onNodeWithTag("hub-all-shows").performScrollTo().requestFocus()
        rule.onNodeWithTag("hub-all-shows").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionCenter) }
        rule.waitForIdle()
        rule.runOnIdle { state = state.copy(libraryPath = emptyList(), libraryEntries = libraries) }
        rule.waitForIdle()
        rule.onNodeWithTag("hub-all-shows").assertIsFocused()
    }
}
