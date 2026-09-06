package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SheetInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val details = ContentDetails("fixture", "Ein film", "Seerr", "Film",
        artworkRes = R.drawable.media_placeholder, mediaType = "movie",
        overview = "Ei lang forteljing med plass til å rulle. ".repeat(100))

    @Composable
    private fun Sheets(state: ReelstackUiState, draft: ConnectionDraft? = null, onClose: () -> Unit = {}) {
        ReelstackSheets(state, draft, onClose, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    private fun bounds(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    @Test fun draggingAtTopDoesNotMoveSheetEvenWhileFingerIsStillDown() {
        var closed = false
        rule.setContent { ReelstackTheme {
            Sheets(ReelstackUiState(activeSheet = AppSheet.TitleDetails("fixture"), contentDetails = details), onClose = { closed = true })
        } }
        val frame = bounds("sheet-viewport")
        val close = bounds("sheet-close")
        val body = rule.onNodeWithTag("detail-scroll")
        body.performTouchInput {
            down(Offset(centerX, 40f))
            repeat(10) { moveBy(Offset(0f, 22f), delayMillis = 30) }
        }
        // Checking during the gesture catches a bounce that a settled screenshot misses.
        assertEquals(frame, bounds("sheet-viewport"))
        assertEquals(close, bounds("sheet-close"))
        body.performTouchInput { up() }
        assertFalse(closed)
        rule.onNodeWithTag("sheet-close").performClick()
        rule.waitUntil { closed }
    }

    @Test fun longFlingAndAsyncUpdateKeepClosePinnedAndReadingPosition() {
        val state = mutableStateOf(ReelstackUiState(activeSheet = AppSheet.TitleDetails("fixture"), contentDetails = details))
        rule.setContent { ReelstackTheme { Sheets(state.value) } }
        val frame = bounds("sheet-viewport")
        val close = bounds("sheet-close")
        rule.onNodeWithTag("overview-expand").performScrollTo().performClick()
        rule.onNodeWithTag("detail-scroll").performTouchInput { swipeUp(durationMillis = 180) }
        rule.waitForIdle()
        val scroll = rule.onNodeWithTag("detail-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue("The test must actually scroll", scroll > 0)
        rule.runOnIdle { state.value = state.value.copy(contentDetails = details.copy(cast = listOf(CastMember("Testperson", "Ei rolle")))) }
        rule.waitForIdle()
        assertEquals(scroll, rule.onNodeWithTag("detail-scroll").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value())
        assertEquals(frame, bounds("sheet-viewport"))
        assertEquals(close, bounds("sheet-close"))
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
    }

    @Test fun everyRouteUsesSameTopRightCloseTarget() {
        val initial = ReelstackUiState(activeSheet = AppSheet.TitleDetails("fixture"), contentDetails = details)
        val state = mutableStateOf(initial)
        val connection = ConnectionDraft(ServiceKind.EMBY, "Emby", "https://emby.example", "")
        rule.setContent { ReelstackTheme { Sheets(state.value, connection) } }
        val close = bounds("sheet-close")
        val frame = bounds("sheet-viewport")
        assertTrue(close.left > frame.center.x)
        assertTrue("Close must be at the trailing edge, not in the middle of the row", frame.right - close.right <= close.width / 2)
        assertTrue(close.top - frame.top < close.height)
        val routes = listOf(
            initial.copy(activeSheet = AppSheet.UpcomingCalendar),
            initial.copy(activeSheet = AppSheet.RequestComposer, requestDraft = RequestDraft(
                DiscoverMedia("film", "Ein film", "Film", R.drawable.media_placeholder, true, mediaType = "movie"))),
            initial.copy(activeSheet = AppSheet.ConnectionEditor(ServiceKind.EMBY)),
            initial.copy(activeSheet = AppSheet.SessionDetails(initial.sessions.first().key)),
        )
        for (route in routes) {
            rule.runOnIdle { state.value = route }
            rule.waitForIdle()
            rule.onAllNodesWithTag("sheet-close").assertCountEquals(1)
            assertEquals(frame, bounds("sheet-viewport"))
            assertEquals(close, bounds("sheet-close"))
        }
    }

    @Test fun connectionErrorAndKeyboardDoNotScrollCloseOutOfView() {
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.EMBY, "Emby", "https://emby.example", ""))
        rule.setContent { ReelstackTheme {
            Sheets(ReelstackUiState(activeSheet = AppSheet.ConnectionEditor(ServiceKind.EMBY)), draft)
        } }
        val close = bounds("sheet-close")
        rule.onNodeWithText("Tenaradresse").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
        rule.onNodeWithTag("connection-scroll").performTouchInput { swipeUp() }
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
        rule.runOnIdle { draft = draft.copy(error = "Ei lang feilmelding. ".repeat(20)) }
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
        // Keyboard dismissal is local to the isolated test app, not a real account.
        rule.onNodeWithTag("connection-scroll").performTouchInput { swipeDown() }
        assertEquals(close.width, bounds("sheet-close").width)
    }

    @Test fun largeTextAndRequestSendingKeepCloseAccessibleButGuardSubmission() {
        var closed = false
        var sending by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                ReelstackTheme { Sheets(ReelstackUiState(activeSheet = AppSheet.RequestComposer,
                    requestDraft = RequestDraft(DiscoverMedia("series", "Ein serie", "Serie", R.drawable.media_placeholder, true, mediaType = "tv"),
                        loading = false, sending = sending)), onClose = { closed = true }) }
            }
        }
        rule.onNodeWithTag("sheet-close").assertIsDisplayed().assertIsNotEnabled()
        val close = bounds("sheet-close")
        rule.onNodeWithTag("request-scroll").performTouchInput { swipeUp() }
        assertEquals(close, bounds("sheet-close"))
        assertFalse(closed)
        rule.runOnIdle { sending = false }
        rule.onNodeWithTag("sheet-close").assertIsEnabled().performClick()
        rule.waitUntil { closed }
    }

    @Test fun androidBackDismissesReadingSheet() {
        var closed = false
        rule.setContent { ReelstackTheme {
            Sheets(ReelstackUiState(activeSheet = AppSheet.TitleDetails("fixture"), contentDetails = details), onClose = { closed = true })
        } }
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
        androidx.test.espresso.Espresso.pressBack()
        rule.waitUntil { closed }
    }

    @Test fun androidBackCannotDismissRequestDuringSubmission() {
        var closed = false
        rule.setContent { ReelstackTheme {
            Sheets(ReelstackUiState(activeSheet = AppSheet.RequestComposer, requestDraft = RequestDraft(
                DiscoverMedia("film", "Ein film", "Film", R.drawable.media_placeholder, true, mediaType = "movie"),
                loading = false, sending = true)), onClose = { closed = true })
        } }
        rule.onNodeWithTag("sheet-close").assertIsNotEnabled()
        androidx.test.espresso.Espresso.pressBack()
        rule.waitForIdle()
        assertFalse(closed)
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
    }
}
