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

class RequestFlowUiTest {
    @get:Rule val rule = createComposeRule()
    private val media = DiscoverMedia("series", "Ein serie med ein lang tittel", "Serie", R.drawable.media_placeholder, true, mediaType = "tv")
    @Test fun availableAndRequestedSeasonsAreLockedWhileMissingSeasonCanBeSelected() {
        var draft by mutableStateOf(RequestDraft(media, listOf(RequestSeason(1, "Sesong 1", 8, 5),
            RequestSeason(2, "Sesong 2", 8, 3), RequestSeason(3, "Sesong 3", 8, 1)), selected = setOf(3), loading = false))
        rule.setContent { ReelstackTheme { RequestComposer(ReelstackUiState(requestDraft = draft), { n, checked ->
            draft = draft.copy(selected = if (checked) draft.selected + n else draft.selected - n)
        }, { draft = draft.copy(notify = it) }, {}, {}, {}, {}) } }
        rule.onNodeWithTag("request-season-1").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("request-season-2").assertIsNotEnabled()
        rule.onNodeWithTag("request-season-3").assertIsOn().performClick()
        rule.onNodeWithTag("confirm-request").assertIsNotEnabled()
        rule.onNodeWithTag("request-season-3").performClick()
        rule.onNodeWithTag("confirm-request").assertIsEnabled()
    }
    @Test fun notificationDefaultsOnAndCanBeDisabledBeforeExplicitConfirmation() {
        var draft by mutableStateOf(RequestDraft(media.copy(mediaType = "movie"), loading = false))
        var sent = 0
        rule.setContent { ReelstackTheme { RequestComposer(ReelstackUiState(requestDraft = draft), { _, _ -> },
            { draft = draft.copy(notify = it) }, { sent++ }, {}, {}, {}) } }
        rule.onNodeWithTag("request-notification").performScrollTo().assertIsOn().performClick().assertIsOff()
        assertEquals(0, sent)
        rule.onNodeWithTag("confirm-request").performClick()
        assertEquals(1, sent)
    }
    @Test fun loadingAndErrorsCannotSend() {
        var draft by mutableStateOf(RequestDraft(media))
        rule.setContent { ReelstackTheme { RequestComposer(ReelstackUiState(requestDraft = draft), { _, _ -> }, {}, {}, {}, {}, {}) } }
        rule.onNodeWithTag("confirm-request").assertIsNotEnabled()
        rule.runOnIdle { draft = draft.copy(loading = false, error = "Fekk ikkje henta sesongar") }
        rule.onNodeWithTag("confirm-request").assertIsNotEnabled()
        rule.onNodeWithText("Sjekk på nytt").performScrollTo().assertIsDisplayed()
    }
    @Test fun trackedRequestShowsRealStageAndNotificationChoice() {
        var notify = false
        val request = TrackedRequest("x", 2, "tv", "Testserie", null, setOf(2), false, RequestStage.IMPORTING)
        rule.setContent { ReelstackTheme { TrackedRequestCard(request, {}, { notify = it }) } }
        rule.onNodeWithText("Blir lagt i biblioteket").assertIsDisplayed()
        rule.onNodeWithText("Sesong 2").assertIsDisplayed()
        rule.onNodeWithText("Lastar ned").assertDoesNotExist()
        assertFalse(notify)
    }
}
