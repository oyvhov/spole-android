package app.reelstack.ui.components

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvHeroFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val film = LibraryMedia(id = "jf-1", title = "Film", subtitle = "2026", progress = .4f,
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
    private val series = LibraryMedia(id = "jf-2", title = "Serie", subtitle = "2026",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = "2", mediaType = "Series")

    @Test fun focusStaysInTheHeroWhenTheNextTitleCannotBePlayed() {
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme { TabletLibraryFeature(film, onOpen = {}, candidates = listOf(film, series)) }
        }
        rule.onNodeWithTag("tablet-feature-play").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        // The hero turns to the series, which has nothing to play, and the play button goes.
        rule.mainClock.advanceTimeBy(8_500)
        rule.waitForIdle()
        rule.onNodeWithTag("tablet-feature-play").assertDoesNotExist()
        rule.onNodeWithTag("tablet-feature-open").assertIsFocused()
    }
}
