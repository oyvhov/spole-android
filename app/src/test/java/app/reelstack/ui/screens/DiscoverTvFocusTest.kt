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
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.FocusDetails
import app.reelstack.data.model.PersonPage
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.ui.PersonSheet
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DiscoverTvFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun media(id: Int, overview: String? = null) = DiscoverMedia("seerr-movie-$id", "Tittel $id", "2026", R.drawable.media_placeholder,
        false, remoteId = id, mediaType = "movie", overview = overview)
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
    private val request = TrackedRequest("r", 4, "movie", "Coraline", null, emptySet())

    private fun keyboard(content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        ReelstackTheme { content() }
    }

    private fun hub(requests: List<TrackedRequest> = listOf(request), details: Map<String, FocusDetails> = emptyMap()) = DiscoverUiState(
        connections = listOf(seerr), trackedRequests = requests,
        hub = DiscoverHubState(rows = mapOf(DiscoverRowKind.TRENDING to DiscoverRowState(listOf(media(1), media(2)))), focusDetails = details))

    @Test fun theRemoteStartsOnYourFirstRequestAndTheHeaderNamesIt() {
        keyboard { DiscoverHubContent(hub(), PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-request-r").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText("Coraline")))
    }

    @Test fun withoutRequestsTheRemoteStartsOnTheFirstPosterAndItsOverviewComesFromTheDetails() {
        keyboard { DiscoverHubContent(hub(requests = emptyList(), details = mapOf("seerr-movie-1" to FocusDetails(listOf("1t 39m"), "Ein omtale."))),
            PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText("Ein omtale.")))
    }

    @Test fun theAllGridStartsOnItsFirstPoster() {
        keyboard { DiscoverGridContent("Populært no", DiscoverRowState(listOf(media(1), media(2))), PaddingValues(), {}, {}, {}) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
    }

    @Test fun yourRequestsStartOnTheChosenTab() {
        keyboard { MyRequestsScreen(ReelstackUiState(trackedRequests = listOf(request)), PaddingValues(), onOpen = {}, onBack = {}, onLoadOlder = {}) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-requests-tab-ALL").assertIsFocused() }.isSuccess }
    }

    @Test fun aPersonPageKeepsTheNameInViewWhenThePosterTakesFocus() {
        val page = PersonPage(DiscoverPerson(9, "Margaret Qualley", null), department = "Acting", birthday = "1994-10-23",
            inLibrary = listOf(media(1)), more = (2..12).map { media(it) })
        keyboard { PersonSheet(page, onTitle = {}) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
        val name = rule.onNodeWithText("Margaret Qualley").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("name starts above the screen: ${name.top}", name.top >= 0f)
    }

    @Test fun theFocusedRowLiesRightUnderTheHeader() {
        val rows = mapOf(
            DiscoverRowKind.TRENDING to DiscoverRowState((1..8).map { media(it) }),
            DiscoverRowKind.UPCOMING_MOVIES to DiscoverRowState((11..18).map { media(it) }),
            DiscoverRowKind.UPCOMING_SERIES to DiscoverRowState((21..28).map { media(it) }))
        keyboard { DiscoverHubContent(DiscoverUiState(connections = listOf(seerr), hub = DiscoverHubState(rows = rows)), PaddingValues(),
            DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
        // One press down, as on a remote: the middle row takes focus. (The last row cannot rise above
        // the end of the list, so it is the row with rows under it that shows the behaviour.)
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNodeWithTag("discover-poster-seerr-movie-11").assertIsFocused()
        val list = rule.onNodeWithTag("discover-hub-rows").fetchSemanticsNode().boundsInRoot
        val poster = rule.onNodeWithTag("discover-poster-seerr-movie-11").fetchSemanticsNode().boundsInRoot
        val gap = (poster.top - list.top) / rule.density.density
        // Only the heading of the focused row sits between the header and its posters; no half row above.
        org.junit.Assert.assertTrue("focused row is $gap dp under the header", gap in 30f..80f)
    }

    @Test fun aFocusedRequestShowsItsOwnPictureAndSynopsis() {
        keyboard { DiscoverHubContent(hub(details = mapOf("seerr-movie-4" to FocusDetails(listOf("2009"), "Ei jente finn ei dør.",
            "https://image.tmdb.org/t/p/w1280/r.jpg"))), PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-request-r").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText("Ei jente finn ei dør.")))
        rule.onNodeWithTag("discover-focus-backdrop").assertExists()
    }
}
