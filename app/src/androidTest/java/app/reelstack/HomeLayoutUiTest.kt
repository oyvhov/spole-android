package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.key.Key
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeEditorActions
import app.reelstack.ui.screens.HomeLayoutSetting
import app.reelstack.ui.screens.editableHomeRows
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** The Home editor with the real theme and device input, and Home following what it saves. */
@OptIn(ExperimentalTestApi::class)
class HomeLayoutUiTest {
    @get:Rule val rule = createComposeRule()

    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token", "me")

    @Test fun canMoveRepeatedlyResetAndReopenWithDeviceInput() {
        var state by mutableStateOf(ReelstackUiState(connections = listOf(jellyfin), homeLayout = HomeLayout.DEFAULT))
        rule.setContent { ReelstackTheme {
            HomeLayoutSetting(state, HomeEditorActions(onLayoutChange = { state = state.copy(homeLayout = it) }))
        } }
        val nextUp = HomeRowKey(HomeRowKind.NEXT_UP, ServiceKind.JELLYFIN)
        rule.onNodeWithTag("home-layout-open").performClick()
        val up = rule.onNodeWithTag("home-layout-up-${nextUp.id}")
        val television = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (television) {
            up.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            up.performKeyInput { pressKey(Key.Enter) }
            up.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        } else {
            up.performClick().performClick()
        }
        rule.runOnIdle { assertEquals(nextUp, editableHomeRows(state).first()) }
        if (television) rule.onNodeWithTag("home-layout-down-${nextUp.id}").assertIsFocused()
        rule.onNodeWithTag("home-layout-done").performClick()
        rule.onNodeWithTag("home-layout-open").performClick()
        rule.onNodeWithTag("home-layout-up-${nextUp.id}").assertIsNotEnabled()
        rule.onNodeWithTag("home-layout-reset").performClick()
        rule.runOnIdle { assertEquals(HomeLayout.DEFAULT, state.homeLayout) }
    }

    @Test fun layoutSurvivesRepositoryRecreationAndOlderSettingsHandItBack() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppPreferencesRepository(context)
        val oldOrder = preferences.homeRowOrder
        val oldVisible = preferences.visibleHomeSections
        val oldLayout = preferences.homeLayout
        try {
            val chosen = HomeLayout.DEFAULT.withSourceVisible(ServiceKind.JELLYFIN, false)
            preferences.homeLayout = chosen
            assertEquals(chosen, AppPreferencesRepository(context).homeLayout)

            // A test or an older build that writes the older keys gets a layout built from them.
            preferences.visibleHomeSections = setOf(HomeSection.FAVOURITES)
            val migrated = AppPreferencesRepository(context).homeLayout
            assertTrue(migrated.isVisible(HomeRowKey(HomeRowKind.FAVOURITES, ServiceKind.EMBY)))
            assertFalse(migrated.isVisible(HomeRowKey(HomeRowKind.UPCOMING)))
        } finally {
            preferences.homeRowOrder = oldOrder
            preferences.visibleHomeSections = oldVisible
            preferences.homeLayout = oldLayout
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
        rule.runOnIdle { state = state.copy(homeLayout = state.effectiveHomeLayout.moved(
            HomeRowKey(HomeRowKind.RECOMMENDATIONS), -1, HomeLayout.ALL_KEYS)) }
        rule.onNodeWithTag("home-feed").performScrollToIndex(1)
        assertTrue(rule.onNodeWithText(recommendations).fetchSemanticsNode().boundsInRoot.top <
            rule.onNodeWithText(releases).fetchSemanticsNode().boundsInRoot.top)
    }
}
