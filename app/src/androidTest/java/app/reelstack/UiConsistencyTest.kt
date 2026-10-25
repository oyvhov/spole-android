package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class UiConsistencyTest {
    @get:Rule val rule = createComposeRule()



    @OptIn(ExperimentalTestApi::class)
    @Test fun settingsToggleHasOneActionAndShowsLibraryNotifications() {
        assumeTouchLayout()
        val changes = mutableListOf<app.reelstack.data.model.HomeLayout>()
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(412.dp, 900.dp))) {
              ReelstackTheme {
                SettingsScreen(ReelstackUiState(), PaddingValues(0.dp), {}, {}, {}, { _, _ -> },
                    homeEditor = app.reelstack.ui.screens.HomeEditorActions(onLayoutChange = { changes += it }))
              }
            }
        }
        rule.onNodeWithText("Emby · Filmar").assertDoesNotExist()
        rule.onNodeWithTag("settings-category-HOME").performScrollTo().performClick()
        rule.onNodeWithTag("home-layout-open").performScrollTo().performClick()
        rule.onNodeWithTag("home-layout-list").performScrollToNode(hasTestTag("home-layout-visible-NEW_MOVIES:EMBY"))
        rule.onNodeWithTag("home-layout-visible-NEW_MOVIES:EMBY").performClick()
        assertEquals(1, changes.size)
        assertFalse(changes.single().isVisible(app.reelstack.data.model.HomeRowKey(
            app.reelstack.data.model.HomeRowKind.NEW_MOVIES, app.reelstack.data.model.ServiceKind.EMBY)))
        rule.onNodeWithTag("home-layout-done").performClick()
        rule.onNodeWithTag("settings-back").performClick()
        rule.onNodeWithTag("settings-category-UPDATES").performScrollTo().performClick()
        rule.onNodeWithText("Bibliotekvarsel").performScrollTo().assertIsDisplayed()
    }

    private fun assumeTouchLayout() {
        val configuration = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration
        org.junit.Assume.assumeFalse(configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
    }
}
