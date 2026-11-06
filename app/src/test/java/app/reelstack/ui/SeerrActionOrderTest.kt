package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The one thing a reader came to do with a Seerr title comes first in its action group. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class SeerrActionOrderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
    private val account = ServiceAccount(ServiceKind.SEERR, "7", "Øyvind", permissions = 8224)
    private val film = DiscoverMedia("seerr-movie-11", "Masterplan", "2026", R.drawable.media_placeholder, false, remoteId = 11,
        mediaType = "movie", seerrStatus = 1)

    @Test fun askingComesBeforeFollowingAndTheTrailerAndTakesTheStartFocus() {
        val state = ReelstackUiState(connections = listOf(seerr), accounts = mapOf(ServiceKind.SEERR to account),
            discover = listOf(film), activeSheet = AppSheet.TitleDetails(film.id),
            contentDetails = ContentDetails(film.id, film.title, "Seerr", "Film · 2026", artworkRes = R.drawable.media_placeholder,
                source = ServiceKind.SEERR, mediaType = "Movie", tmdbId = 11, trailerUrl = "https://video.example/watch?v=abc",
                overview = "Ein kort omtale."))
        rule.setContent {
            val input = LocalInputModeManager.current
            SideEffect { input.requestInputMode(InputMode.Keyboard) }
            ReelstackTheme {
                Box(Modifier.size(960.dp, 540.dp)) {
                    RichTitleDetailsSheet(state, onAddMedia = {}, onSeerrAccount = {}, scroll = rememberScrollState(), entered = true)
                }
            }
        }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("title-request").assertIsFocused() }.isSuccess }
        val ask = rule.onNodeWithTag("title-request").fetchSemanticsNode().boundsInRoot
        val follow = rule.onNodeWithTag("detail-calendar-follow").fetchSemanticsNode().boundsInRoot
        // Leftmost of the row, or above it: never after a secondary action.
        assertTrue("ask $ask, follow $follow", ask.top < follow.bottom && ask.left < follow.left || ask.bottom <= follow.top)
    }
}
