package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import app.reelstack.ui.screens.WelcomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.ui.components.StableSheetDialog
import androidx.compose.ui.input.key.Key
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CombinedSetupUiTest {
    @get:Rule val rule = createComposeRule()
    private fun capture(name: String) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val target = if (rule.onAllNodesWithTag("adaptive-dialog").fetchSemanticsNodes().isNotEmpty())
            rule.onNodeWithTag("adaptive-dialog") else rule.onRoot()
        java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            target.captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun firstScreenOffersCombinedAndOtherMethods() {
        var combined = 0
        rule.setContent { ReelstackTheme { androidx.compose.material3.Surface {
            WelcomeScreen(ReelstackUiState(), {}, {}, onCombined = { combined++ })
        } } }
        val tv = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (tv) rule.onNodeWithTag("setup-combined").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        else rule.onNodeWithTag("setup-combined").assertIsDisplayed().performClick()
        assertEquals(1, combined)
        capture("setup-welcome")
        rule.onNodeWithTag("setup-other").performClick()
        rule.onNodeWithTag("setup-combined").assertDoesNotExist()
    }

    @Test fun addressesRequiredBeforeContinueAndRetryPreservesThem() {
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "", "", simpleSetup = true))
        var starts = 0
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ -> CombinedSetupSheet(draft,
            { draft = draft.copy(url = it) }, { draft = draft.copy(companionUrl = it) }, { starts++ }, {}) } } }
        rule.onNodeWithTag("setup-start").assertIsNotEnabled()
        capture("setup-addresses-empty")
        rule.onNodeWithTag("setup-jellyfin-url").performTextInput("https://media.example")
        rule.onNodeWithTag("setup-start").assertIsNotEnabled()
        rule.onNodeWithTag("setup-seerr-url").performTextInput("https://requests.example")
        rule.onNodeWithTag("setup-start").performScrollTo().performClick()
        assertEquals(1, starts)
        rule.runOnIdle { draft = draft.copy(error = "Prøv igjen") }
        rule.onNodeWithTag("setup-jellyfin-url").assertTextContains("https://media.example")
        rule.onNodeWithTag("setup-error").assertExists()
        capture("setup-addresses")
    }

    @Test fun welcomeAtDoubleFontSizeKeepsBothChoicesReachable() {
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme { androidx.compose.material3.Surface {
            WelcomeScreen(ReelstackUiState(), {}, {}, onCombined = {})
        } } } }
        rule.onNodeWithTag("setup-combined").performScrollTo().assertIsDisplayed()
        capture("setup-welcome-large")
        rule.onNodeWithTag("setup-other").performScrollTo().assertIsDisplayed()
    }

    @Test fun waitingShowsOneCodeAndCancellationAtDoubleFontSize() {
        var cancelled = 0
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
            StableSheetDialog(true, {}) { _, _, _ ->
            CombinedSetupSheet(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "",
                simpleSetup = true, quickConnectWaiting = true, quickConnectCode = "123456"), {}, {}, {}, { cancelled++ })
            }
        } } }
        rule.onNodeWithTag("setup-code").performScrollTo().assertTextEquals("123456")
        rule.onNodeWithTag("setup-start").assertDoesNotExist()
        capture("setup-code-large")
        rule.onNodeWithTag("setup-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }
}
