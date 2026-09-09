package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.*
import app.reelstack.ui.screens.TvWelcomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvSetupTest {
    @get:Rule val rule = createComposeRule()
    @org.junit.Before fun televisionOnly() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assume.assumeTrue(context.getSystemService(android.app.UiModeManager::class.java).currentModeType ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
    }
    @Test fun firstServiceIsFocusedAndAllPersonalServicesAreVisible() {
        var selected: ServiceKind? = null
        rule.setContent { ReelstackTheme { TvWelcomeScreen(ReelstackUiState(), { selected = it }, {}) } }
        rule.onNodeWithTag("tv-setup-JELLYFIN").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("tv-setup-EMBY").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("tv-setup-SEERR").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(ServiceKind.SEERR, selected) }
        rule.onNodeWithTag("tv-setup-RADARR").assertDoesNotExist()
    }
    @Test fun connectedUserStartsAtContinueWithoutDemo() {
        var opened = false
        rule.setContent { ReelstackTheme { TvWelcomeScreen(ReelstackUiState(connections = listOf(
            ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://example.com", "fixture")
        )), {}, { opened = true }) } }
        rule.onNodeWithTag("tv-setup-continue").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertTrue(opened) }
        rule.onNodeWithTag("tv-setup-demo").assertDoesNotExist()
    }
    @Test fun largeTextKeepsServiceActionsScrollableAndReachable() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                ReelstackTheme { TvWelcomeScreen(ReelstackUiState(), {}, {}) }
            }
        }
        rule.onNodeWithTag("tv-setup-SEERR").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("tv-setup-advanced").performScrollTo().performClick()
        rule.onNodeWithTag("tv-setup-SONARR").performScrollTo().assertIsDisplayed()
    }
    @Test fun televisionSeerrDefaultsToQuickConnectAfterAddressAndKeepsAccountAlternative() {
        val draft = mutableStateOf(ConnectionDraft(ServiceKind.SEERR, "Seerr", "https://example.com", "",
            authMode = ConnectionAuthMode.ACCOUNT))
        rule.setContent { ReelstackTheme { ConnectionEditorSheet(draft.value, false, {}, {},
            { draft.value = draft.value.copy(url = it) }, {}, {},
            { draft.value = draft.value.copy(authMode = it) }, {}, {}, {}, {}) } }
        rule.onNodeWithTag("connection-continue").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(ConnectionAuthMode.QUICK_CONNECT, draft.value.authMode) }
        rule.onNodeWithTag("tv-quick-panel").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Jellyfin-konto").performScrollTo().performClick()
        rule.onNode(hasText("Brukarnamn") and hasSetTextAction()).performScrollTo().assertIsDisplayed()
    }
}
