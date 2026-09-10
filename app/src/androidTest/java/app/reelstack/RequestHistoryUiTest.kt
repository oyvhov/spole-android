package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.ActivityScreen
import app.reelstack.ui.screens.RequestHistoryScreen
import app.reelstack.RoadmapTestTheme as ReelstackTheme
import androidx.compose.foundation.layout.PaddingValues
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RequestHistoryUiTest {
    @get:Rule val rule = createComposeRule()
    private val entry = TrackedRequest("history-1", 42, "movie", "Ein film frå historikken", null, emptySet(),
        notify = false, requestId = 1, updatedAt = 1_700_000_000_000L)
    private val populated = RequestHistoryState(items = listOf(entry), nextOffset = 20, loaded = true, hasMore = true, total = 125)

    @Test fun personalActivityHasAnExplicitHistoryEntry() {
        var opened = false
        val connection = ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "fixture", "7", sessionCookie = true)
        rule.setContent { ReelstackTheme { ActivityScreen(ReelstackUiState(connections = listOf(connection)), PaddingValues(0.dp), {}, onHistory = { opened = true }) } }
        rule.onNodeWithTag("open-request-history").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(opened) }
    }
    @Test fun loadOlderHasLoadingFeedbackAndNoNotificationOrWithdrawControls() {
        var state by mutableStateOf(populated)
        var calls = 0
        rule.setContent { ReelstackTheme { RequestHistoryScreen(state, {}, {}, { more ->
            assertTrue(more); calls++; state = state.copy(loading = true)
        }) } }
        rule.onNodeWithTag("request-options-history-1").assertDoesNotExist()
        rule.onNodeWithTag("history-more").performScrollTo().performClick()
        rule.onNodeWithTag("history-more").assertDoesNotExist()
        rule.onNodeWithTag("history-loading").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, calls) }
    }
    @Test fun pageFailureKeepsCardsAndRetriesTheNextPage() {
        var requestedMore: Boolean? = null
        rule.setContent { ReelstackTheme { RequestHistoryScreen(populated.copy(error = "Nettfeil"), {}, {}, { requestedMore = it }) } }
        rule.onNodeWithTag("tracked-request-history-1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("history-retry").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(true, requestedMore) }
    }
    @Test fun failedRefreshRetriesFromTheBeginningEvenWhenOldPagesExist() {
        var requestedMore: Boolean? = null
        rule.setContent { ReelstackTheme { RequestHistoryScreen(populated.copy(error = "Nettfeil", retryFromStart = true), {}, {}, { requestedMore = it }) } }
        rule.onNodeWithTag("history-retry").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(false, requestedMore) }
    }
    @Test fun unavailableMetadataStillShowsTheRequestAndDoesNotOfferDeadDetails() {
        rule.setContent { ReelstackTheme { RequestHistoryScreen(populated.copy(items = listOf(entry.copy(title = "", mediaId = 0))), {}, {}, {}) } }
        rule.onNodeWithText("Førespurnad #1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("tracked-details-history-1").assertIsNotEnabled()
    }
    @Test fun detailsAndBackRemainSeparateFromServerMutations() {
        var opened = ""
        var wentBack = false
        rule.setContent { ReelstackTheme { RequestHistoryScreen(populated, { opened = it }, { wentBack = true }, {}) } }
        rule.onNodeWithTag("tracked-details-history-1").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("history-1", opened) }
        rule.onNodeWithTag("history-back").performScrollTo().performClick()
        rule.onRoot().saveRoadmapImage("history-nn-phone.png")
        rule.runOnIdle { assertTrue(wentBack) }
    }
    @Test fun largeTypeInLandscapeKeepsPagingAndBackReachable() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp, 540.dp)) then DeviceConfigurationOverride.FontScale(2f)) {
                ReelstackTheme { RequestHistoryScreen(populated, {}, {}, {}) }
            }
        }
        rule.onNodeWithTag("request-history").performScrollToNode(hasTestTag("history-more"))
        rule.onNodeWithTag("history-more").assertIsDisplayed()
        rule.onNodeWithTag("request-history").performScrollToNode(hasTestTag("history-back"))
        rule.onNodeWithTag("history-back").assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("history-nn-landscape-2x.png")
    }
}
