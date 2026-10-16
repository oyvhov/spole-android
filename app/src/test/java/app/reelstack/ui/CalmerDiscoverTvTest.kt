package app.reelstack.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.CastMember
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.screens.DiscoverHubActions
import app.reelstack.ui.screens.DiscoverHubContent
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class CalmerDiscoverTvTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
    private val wordy = DiscoverMedia("seerr-tv-1", "Crystal Lake", "2026", R.drawable.media_placeholder, false, remoteId = 1,
        mediaType = "tv", seerrStatus = 1, overview = "Single mother Pam Voorhees has been unable to shake her grief. ".repeat(4),
        facts = listOf("Serie", "2026", "★ 7.1", "Avslutta", "1 sesong", "8 episodar", "Peacock"))
    private val plain = DiscoverMedia("seerr-movie-2", "Masterplan", "2026", R.drawable.media_placeholder, false, remoteId = 2,
        mediaType = "movie", seerrStatus = 1)

    private fun keyboard(content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        ReelstackTheme { content() }
    }

    @Test fun theHeaderKeepsItsHeightAndSaysLittle() {
        keyboard {
            DiscoverHubContent(DiscoverUiState(connections = listOf(seerr),
                trackedRequests = listOf(TrackedRequest("r", 9, "movie", "Coraline", null, setOf())),
                hub = DiscoverHubState(rows = mapOf(DiscoverRowKind.TRENDING to DiscoverRowState(listOf(wordy, plain))))),
                PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {}))
        }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-request-r").assertIsFocused() }.isSuccess }
        val withRequest = rule.onNodeWithTag("discover-focus-header").fetchSemanticsNode().boundsInRoot.height
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNodeWithTag("discover-poster-seerr-tv-1").assertIsFocused()
        val header = rule.onNodeWithTag("discover-focus-header")
        assertEquals(withRequest, header.fetchSemanticsNode().boundsInRoot.height, 1f)
        // Year, rating, kind and status; the synopsis and the long list of facts belong to the title page.
        header.assert(hasAnyDescendant(hasText("★ 7.1", substring = true)))
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-focus-header")) and hasText("Single mother", substring = true)).assertCountEquals(0)
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-focus-header")) and hasText("Peacock", substring = true)).assertCountEquals(0)
    }

    @Test fun theCastOfASeerrFilmIsOnTheFirstScreen() {
        var cast = ""
        keyboard {
            cast = androidx.compose.ui.res.stringResource(R.string.details_cast)
            ReelstackSheets(ReelstackUiState(connections = listOf(seerr), discover = listOf(plain), activeSheet = AppSheet.TitleDetails(plain.id),
                contentDetails = ContentDetails(plain.id, plain.title, "Seerr", "Film · 2026", artworkRes = R.drawable.media_placeholder,
                    source = ServiceKind.SEERR, mediaType = "Movie", overview = "Ein omtale som er lang nok til to liner. ".repeat(3),
                    cast = listOf(CastMember("Ana de Armas", "Rolla", seerrPersonId = 5)))),
                null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        rule.waitForIdle()
        val heading = rule.onNodeWithText(cast).fetchSemanticsNode().boundsInRoot
        val page = rule.onNodeWithTag("tv-detail-page").fetchSemanticsNode().boundsInRoot
        assertTrue("cast heading starts at ${heading.top} of ${page.bottom}", heading.bottom < page.bottom * .8f)
    }

    @Test fun theRemoteReachesTheWholeSynopsis() {
        val state = ReelstackUiState(connections = listOf(seerr), discover = listOf(plain), activeSheet = AppSheet.TitleDetails(plain.id),
            contentDetails = ContentDetails(plain.id, plain.title, "Seerr", "Film · 2026", artworkRes = R.drawable.media_placeholder,
                source = ServiceKind.SEERR, mediaType = "Movie", tmdbId = 2,
                overview = "Ein omtale som er lang nok til mange liner på ein TV. ".repeat(8)))
        // The page itself, without the dialog window around it, which keeps a touch input mode in tests.
        keyboard {
            val scroll = androidx.compose.foundation.rememberScrollState()
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.size(960.dp, 540.dp)) {
                RichTitleDetailsSheet(state, onAddMedia = {}, onSeerrAccount = {}, scroll = scroll, entered = true)
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("detail-calendar-follow").requestFocus()
        rule.waitForIdle()
        // Up from the first row of actions: the synopsis, which opens in full.
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) }
        rule.waitForIdle()
        rule.onNodeWithTag("overview-expand").assertIsFocused()
    }
}
