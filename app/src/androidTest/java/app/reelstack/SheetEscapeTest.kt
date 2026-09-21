package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * A sheet must always be leavable.
 *
 * On 16 September 2026 Spole was found on the TV home screen with a `DIM_BEHIND` dialog window on
 * top of it: focused, drawing nothing, and deaf to Back. The only way out was force-stopping the
 * app. The sheet's visibility and its dismissal both hung off one animation, so an animation that
 * never finished produced a modal nobody could see or close.
 *
 * These tests hold the two halves of that: the sheet leaves when asked no matter when it is asked,
 * and a sheet that refuses an outside tap still lets the remote's Back key out.
 *
 * The same three cases run on the JVM as `SheetEscapeRobolectricTest`, and that is the copy that
 * runs in every build. This one keeps them against a real window manager and real key events, for
 * the runs that happen on a device — it is the wider net, not the one the milestone waits on.
 */
class SheetEscapeTest {
    @get:Rule val rule = createComposeRule()

    private val details = ContentDetails("fixture", "Ein film", "Jellyfin", "Film",
        artworkRes = R.drawable.media_placeholder, mediaType = "movie", overview = "Ei kort omtale.")

    @Composable
    private fun Sheets(state: ReelstackUiState, draft: ConnectionDraft? = null,
        onDetailBack: () -> Boolean = { false }, onClose: () -> Unit) {
        ReelstackSheets(state, draft, onClose, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            onDetailBack = onDetailBack)
    }

    /** The sheet exposes the same unguarded dismissal that system Back invokes. */
    private fun pressBack() {
        // Later Back presses may land after the host has already removed the dialog. That is the
        // success case: there is no invisible window left to swallow them.
        if (rule.onAllNodesWithTag("adaptive-dialog", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("adaptive-dialog", useUnmergedTree = true)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.Dismiss)
        }
    }

    private fun openState() = ReelstackUiState(
        activeSheet = AppSheet.TitleDetails("fixture"), contentDetails = details,
    )

    @Test fun backInTheFirstFrameStillClosesTheSheet() {
        var closed = 0
        val state = mutableStateOf(ReelstackUiState(activeSheet = null))
        rule.setContent { ReelstackTheme { Sheets(state.value) { closed++; state.value = state.value.copy(activeSheet = null) } } }
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread { state.value = openState() }
        // One frame in, the entrance animation has barely started. This is the moment that used to
        // latch `closing` and swallow every later press.
        rule.mainClock.advanceTimeByFrame()
        pressBack()
        rule.mainClock.advanceTimeBy(2_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals("Back in the first frame must dismiss exactly once", 1, closed)
        rule.onNodeWithTag("sheet-viewport").assertDoesNotExist()
    }

    @Test fun repeatedBackPressesDoNotLeaveTheSheetBehind() {
        var closed = 0
        val state = mutableStateOf(ReelstackUiState(activeSheet = null))
        rule.setContent { ReelstackTheme { Sheets(state.value) { closed++; state.value = state.value.copy(activeSheet = null) } } }
        rule.runOnUiThread { state.value = openState() }
        rule.waitForIdle()
        repeat(3) { pressBack() }
        rule.waitForIdle()
        assertTrue("a second press must not be swallowed forever", closed >= 1)
        rule.onNodeWithTag("sheet-viewport").assertDoesNotExist()
    }

    @Test fun aSheetThatIsCommittingSomethingStillLetsBackOut() {
        // `dismissEnabled` is false while a Seerr request is on its way. Refusing an outside tap is
        // reasonable; refusing the only way out of a full-screen modal is not, because a request
        // that never returns would otherwise strand the user in the sheet for good.
        var closed = 0
        val sending = ReelstackUiState(
            activeSheet = AppSheet.RequestComposer,
            requestDraft = RequestDraft(
                media = DiscoverMedia("fixture", "Ein film", "", R.drawable.media_placeholder, inLibrary = false),
                sending = true,
            ),
        )
        val state = mutableStateOf(sending)
        rule.setContent { ReelstackTheme { Sheets(state.value) { closed++; state.value = state.value.copy(activeSheet = null) } } }
        rule.waitForIdle()
        pressBack()
        rule.waitForIdle()
        assertEquals("Back must escape even while the sheet is committing", 1, closed)
    }

    @Test fun detailHistoryBackStaysInTheSameSheetWindow() {
        // Episode → series is a navigation step, not a modal dismissal.  Returning true here must
        // leave the dialog composed, so TV never reveals the page behind it between the two titles.
        var dismissed = 0
        var returned = 0
        rule.setContent { ReelstackTheme {
            Sheets(openState(), onClose = { dismissed++ }, onDetailBack = { returned++; true })
        } }
        rule.waitForIdle()

        pressBack()
        rule.waitForIdle()

        assertEquals("detail history receives Back exactly once", 1, returned)
        assertEquals("a history return must not dismiss the dialog", 0, dismissed)
        rule.onNodeWithTag("sheet-viewport").assertExists()
    }
}
