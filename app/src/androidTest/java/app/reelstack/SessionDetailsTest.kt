package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic UI states run only on the isolated instrumenting device. Real-art review is separate. */
class SessionDetailsTest {
    @get:Rule val rule = createComposeRule()
    private val first = PlaybackSession("Testperson", "Stova", "Ein film", "Movie", .3f, 20, false,
        "1080p", false, sessionId = "first", source = ServiceKind.EMBY)
    private val second = first.copy(title = "Ein annan film", deviceName = "Nettbrett", sessionId = "second")

    @Composable
    private fun Sheets(state: ReelstackUiState, select: (String) -> Unit = {}, toggle: (String) -> Unit = {}) {
        ReelstackSheets(state, null, {}, toggle, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
            onSessionSelected = select)
    }

    @Test fun livePauseStopAndSessionSwitchKeepThePopupFrameStable() {
        var state by mutableStateOf(ReelstackUiState(sessions = listOf(first, second),
            activeSheet = AppSheet.SessionDetails(first.key)))
        rule.setContent { ReelstackTheme { Sheets(state, select = { state = state.copy(activeSheet = AppSheet.SessionDetails(it)) }) } }
        rule.waitForIdle()
        rule.onNodeWithText("Movie").assertDoesNotExist()
        rule.onNodeWithText("Film").assertIsDisplayed()
        val frame = rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot
        val close = rule.onNodeWithTag("sheet-close").fetchSemanticsNode().boundsInRoot
        val action = rule.onNodeWithTag("session-toggle").fetchSemanticsNode().boundsInRoot
        val label = rule.onNodeWithTag("session-toggle").fetchSemanticsNode().config[SemanticsProperties.Text]
        rule.runOnIdle { state = state.copy(sessions = listOf(first.copy(paused = true, remainingMinutes = 19), second)) }
        rule.waitForIdle()
        assertNotEquals(label, rule.onNodeWithTag("session-toggle").fetchSemanticsNode().config[SemanticsProperties.Text])
        assertEquals(action, rule.onNodeWithTag("session-toggle").fetchSemanticsNode().boundsInRoot)
        rule.runOnIdle { state = state.copy(sessions = listOf(second)) }
        rule.onNodeWithTag("session-ended").assertIsDisplayed()
        rule.onNodeWithTag("session-toggle").assertDoesNotExist()
        rule.onNodeWithTag("session-switch-${second.key}").performClick()
        rule.onNodeWithTag("session-title").assertTextEquals(second.title)
        assertEquals(frame, rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot)
        assertEquals(close, rule.onNodeWithTag("sheet-close").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun largeTextScrollKeepsPauseAndCloseVisibleAndGuardsDuplicateCommands() {
        var pending by mutableStateOf<String?>(null)
        var calls = 0
        val longTitle = first.copy(title = "Ein lang filmtittel som treng fleire linjer", subtitle = "Ei lang forklaring. ".repeat(10))
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                ReelstackTheme { Sheets(ReelstackUiState(sessions = listOf(longTitle, second),
                    pendingSessionKey = pending, activeSheet = AppSheet.SessionDetails(first.key)),
                    toggle = { calls++; pending = it }) }
            }
        }
        rule.onNodeWithTag("session-toggle").assertIsDisplayed()
        val button = rule.onNodeWithTag("session-toggle").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("session-scroll").performTouchInput { swipeUp() }
        rule.onNodeWithText("Direkteavspeling").performScrollTo()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithText("Direkteavspeling").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertEquals("Playback method must fit without splitting the word at font scale 2", 1, layouts.single().lineCount)
        rule.onNodeWithTag("sheet-close").assertIsDisplayed()
        assertEquals(button, rule.onNodeWithTag("session-toggle").fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithTag("session-toggle").performClick()
        rule.onNodeWithTag("session-toggle").assertIsNotEnabled()
        rule.onNodeWithTag("session-toggle").performClick()
        assertEquals(1, calls)
    }

    @Test fun aServerFailureShowsRetryAndRecoversWithoutPretendingPlaybackStopped() {
        var state by mutableStateOf(ReelstackUiState(sessions = emptyList(), activeSheet = AppSheet.SessionDetails(first.key),
            playbackUnavailableSources = setOf(ServiceKind.EMBY)))
        rule.setContent { ReelstackTheme { Sheets(state) } }
        rule.onNodeWithTag("session-unavailable").assertIsDisplayed()
        rule.onNodeWithTag("session-ended").assertDoesNotExist()
        rule.runOnIdle { state = state.copy(sessions = listOf(first), playbackUnavailableSources = emptySet()) }
        rule.onNodeWithTag("session-title").assertTextEquals(first.title)
        rule.onNodeWithTag("session-unavailable").assertDoesNotExist()
    }

    @Test fun returningPlaybackReplacesEndedStateWithoutReopeningTheSheet() {
        var state by mutableStateOf(ReelstackUiState(sessions = emptyList(), activeSheet = AppSheet.SessionDetails(first.key)))
        rule.setContent { ReelstackTheme { Sheets(state) } }
        rule.onNodeWithTag("session-ended").assertIsDisplayed()
        val frame = rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { state = state.copy(sessions = listOf(first)) }
        rule.onNodeWithTag("session-title").assertTextEquals(first.title)
        rule.onNodeWithTag("session-toggle").assertIsDisplayed()
        assertEquals(frame, rule.onNodeWithTag("sheet-viewport").fetchSemanticsNode().boundsInRoot)
    }
}
