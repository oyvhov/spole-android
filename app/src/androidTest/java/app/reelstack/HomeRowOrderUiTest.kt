package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.key.Key
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeRowOrderSetting
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HomeRowOrderUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun canMoveRepeatedlyResetAndReopenWithDeviceInput() {
        var state by mutableStateOf(ReelstackUiState())
        rule.setContent { ReelstackTheme { HomeRowOrderSetting(state, { state = state.copy(homeRowOrder = it) }) } }
        rule.onNodeWithTag("home-order-open").performClick()
        val up = rule.onNodeWithTag("home-order-FAVOURITES-0")
        val television = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (television) {
            up.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            up.performKeyInput { pressKey(Key.Enter) }
            up.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        } else {
            up.performClick().performClick()
        }
        rule.runOnIdle { assertEquals(HomeRow.FAVOURITES, state.homeRowOrder.first()) }
        if (television) rule.onNodeWithTag("home-order-FAVOURITES-1").assertIsFocused()
        rule.onNodeWithTag("home-order-done").performClick()
        rule.onNodeWithTag("home-order-open").performClick()
        rule.onNodeWithTag("home-order-FAVOURITES-0").assertIsNotEnabled()
        rule.onNodeWithTag("home-order-reset").performClick()
        rule.runOnIdle { assertEquals(HomeRow.entries, state.homeRowOrder) }
        capture("home-order")
        rule.onNodeWithTag("home-order-row-NOW_PLAYING").assertIsDisplayed()
    }

    @Test fun largeTextCanScrollAndMoveLastRow() {
        var state by mutableStateOf(ReelstackUiState())
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            ReelstackTheme { HomeRowOrderSetting(state, { state = state.copy(homeRowOrder = it) }) }
        } }
        rule.onNodeWithTag("home-order-open").performClick()
        rule.onNodeWithTag("home-order-list").performScrollToNode(hasTestTag("home-order-UPCOMING-0"))
        rule.onNodeWithTag("home-order-UPCOMING-0").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(HomeRow.UPCOMING, state.homeRowOrder[state.homeRowOrder.lastIndex - 1]) }
        capture("home-order-large")
        rule.onNodeWithTag("home-order-done").assertIsDisplayed().performClick()
    }

    @Test fun preferencesSurviveRepositoryRecreationWithoutChangingVisibility() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferencesRepository(context)
        val oldOrder = preferences.homeRowOrder
        val oldVisible = preferences.visibleHomeSections
        try {
            preferences.visibleHomeSections = setOf(HomeSection.FAVOURITES)
            preferences.homeRowOrder = HomeRow.entries.reversed()
            val reopened = AppPreferencesRepository(context)
            assertEquals(HomeRow.entries.reversed(), reopened.homeRowOrder)
            assertEquals(setOf(HomeSection.FAVOURITES), reopened.visibleHomeSections)
            reopened.homeRowOrder = HomeRow.entries
            assertEquals(setOf(HomeSection.FAVOURITES), reopened.visibleHomeSections)
        } finally {
            preferences.homeRowOrder = oldOrder
            preferences.visibleHomeSections = oldVisible
        }
    }

    @Test fun homeFeedUsesChosenOrder() {
        var state by mutableStateOf(ReelstackUiState(
            homeSections = setOf(HomeSection.RECOMMENDATIONS, HomeSection.RECENT_RELEASES),
            homeRowOrder = HomeRow.entries.reversed(),
        ))
        rule.setContent { ReelstackTheme {
            app.reelstack.ui.screens.HomeScreen(state, androidx.compose.foundation.layout.PaddingValues(),
                {}, {}, {}, {}, {}, onRefresh = {}, showSearch = false)
        } }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val releases = context.getString(R.string.home_recent_releases)
        val recommendations = context.getString(R.string.home_recommendations)
        rule.onNodeWithTag("home-feed").performScrollToIndex(1)
        assertTrue(rule.onNodeWithText(releases).fetchSemanticsNode().boundsInRoot.top <
            rule.onNodeWithText(recommendations).fetchSemanticsNode().boundsInRoot.top)
        rule.runOnIdle { state = state.copy(homeRowOrder = HomeRow.entries) }
        rule.onNodeWithTag("home-feed").performScrollToIndex(1)
        assertTrue(rule.onNodeWithText(recommendations).fetchSemanticsNode().boundsInRoot.top <
            rule.onNodeWithText(releases).fetchSemanticsNode().boundsInRoot.top)
    }

    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            rule.onNodeWithTag("home-order-dialog").captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
