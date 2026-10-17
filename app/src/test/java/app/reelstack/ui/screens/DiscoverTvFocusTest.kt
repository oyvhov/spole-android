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

    @Test fun withoutRequestsTheRemoteStartsOnTheFirstPosterAndTheHeaderNamesIt() {
        keyboard { DiscoverHubContent(hub(requests = emptyList(), details = mapOf("seerr-movie-1" to FocusDetails(listOf("1t 39m"), "Ein omtale."))),
            PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-poster-seerr-movie-1").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText(media(1).title)))
        // The synopsis is on the title page; the header only names the title.
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-focus-header")) and hasText("Ein omtale.")).assertCountEquals(0)
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

    @Test fun aFocusedRequestShowsItsOwnPictureAndYear() {
        keyboard { DiscoverHubContent(hub(details = mapOf("seerr-movie-4" to FocusDetails(listOf("2009", "★ 7.7"), "Ei jente finn ei dør.",
            "https://image.tmdb.org/t/p/w1280/r.jpg"))), PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-request-r").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText("2009", substring = true)))
        rule.onNodeWithTag("discover-focus-header").assert(hasAnyDescendant(hasText("★ 7.7", substring = true)))
        rule.onAllNodes(hasAnyAncestor(hasTestTag("discover-focus-header")) and hasText("Ei jente finn ei dør.")).assertCountEquals(0)
        rule.onNodeWithTag("discover-focus-backdrop").assertExists()
    }

    @Test fun readMoreOnAPersonStaysAndReadsLessAfterwards() {
        val page = PersonPage(DiscoverPerson(9, "Margaret Qualley", null), department = "Acting",
            biography = "Margaret Qualley is an American actress. ".repeat(12))
        keyboard { PersonSheet(page, onTitle = {}) }
        rule.waitForIdle()
        rule.onNodeWithTag("person-read-more").requestFocus()
        rule.onNodeWithTag("person-read-more").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionCenter) }
        rule.waitForIdle()
        // The button the remote was on is still there, now to read less.
        rule.onNodeWithTag("person-read-more").assertIsFocused()
            .assert(hasText(rule.activity.getString(R.string.details_less)))
    }

    @Test fun backFromYourRequestsReturnsToAlle() {
        var returned = false
        val state = hub().let { it.copy(hub = it.hub.copy(returnTo = app.reelstack.data.model.DiscoverReturn("my-requests-all", 0, 0))) }
        keyboard { DiscoverHubContent(state, PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {}, onReturned = { returned = true })) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-my-requests-all").assertIsFocused() }.isSuccess }
        rule.runOnIdle { org.junit.Assert.assertTrue(returned) }
    }

    @Test fun backFromARowsAlleReturnsToItsTile() {
        val state = hub().let { it.copy(hub = it.hub.copy(returnTo = app.reelstack.data.model.DiscoverReturn("all:TRENDING", 0, 0))) }
        keyboard { DiscoverHubContent(state, PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("discover-all-TRENDING").assertIsFocused() }.isSuccess }
    }

    @Test fun returningFocusDoesNotTakeTheRemotesNextMoveBack() {
        var returns = 0
        val state = hub(requests = emptyList()).let { it.copy(hub = it.hub.copy(
            returnTo = app.reelstack.data.model.DiscoverReturn("all:TRENDING", 0, 0))) }
        rule.mainClock.autoAdvance = false
        keyboard { DiscoverHubContent(state, PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {},
            onReturned = { returns++ })) }
        // Inspect the first successful restoration, before the old forty-frame loop could finish.
        for (frame in 0 until 12) {
            rule.mainClock.advanceTimeByFrame()
            if (runCatching { rule.onNodeWithTag("discover-all-TRENDING").assertIsFocused() }.isSuccess) break
        }
        rule.onNodeWithTag("discover-all-TRENDING").assertIsFocused()
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionLeft) }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("discover-poster-seerr-movie-2").assertIsFocused()
        org.junit.Assert.assertEquals(1, returns)
    }

    @Test fun freshRemoteInputCancelsRestorationOfATileThatIsNotPresent() {
        var returns = 0
        val state = hub(requests = emptyList()).let { it.copy(hub = it.hub.copy(
            returnTo = app.reelstack.data.model.DiscoverReturn("missing-tile", 0, 0))) }
        rule.mainClock.autoAdvance = false
        keyboard { DiscoverHubContent(state, PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {},
            onReturned = { returns++ })) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("discover-poster-seerr-movie-1").requestFocus()
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        rule.mainClock.advanceTimeBy(100)
        // New input finishes the restore before the forty-frame deadline.
        org.junit.Assert.assertEquals(1, returns)
        rule.onNodeWithTag("discover-poster-seerr-movie-2").assertIsFocused()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("discover-poster-seerr-movie-2").assertIsFocused()
    }

    @Test fun leavingForAlleRemembersWhereTheRemoteWas() {
        var left: app.reelstack.data.model.DiscoverReturn? = null
        keyboard { DiscoverHubContent(hub(), PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {}, onLeave = { left = it })) }
        rule.onNodeWithTag("discover-my-requests-all").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("my-requests-all", left?.key) }
    }

    @Test fun rightAtTheEndOfARowStaysInTheRow() {
        keyboard { DiscoverHubContent(hub(), PaddingValues(), DiscoverHubActions({}, {}, {}, {}, {}, {}, {})) }
        rule.waitUntil(3_000) { runCatching { rule.onNodeWithTag("my-request-r").assertIsFocused() }.isSuccess }
        rule.onNodeWithTag("discover-all-TRENDING").requestFocus()
        rule.waitForIdle()
        // Nothing lies to the right of «Alle»; the link to your requests above must not catch the press.
        rule.onNode(isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        rule.waitForIdle()
        rule.onNodeWithTag("discover-all-TRENDING").assertIsFocused()
    }
}
