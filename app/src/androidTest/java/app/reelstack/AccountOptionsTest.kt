package app.reelstack

import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AccountOptionsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun remoteSignOutStartsAtCancelAndReturnsWithoutRemovingAccount() {
        var open by mutableStateOf(false)
        var removed = 0
        lateinit var input: InputModeManager
        rule.setContent { ReelstackTheme {
            input = LocalInputModeManager.current
            Button({ open = true }, Modifier.testTag("open")) { Text("Open") }
            if (open) SignOutConfirmation(ServiceKind.SEERR, { open = false }, { removed++; open = false })
        } }
        rule.runOnIdle { input.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("open").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("sign-out-cancel").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertFalse(open); assertEquals(0, removed) }
        rule.onNodeWithTag("open").assertIsFocused()
    }

    @Test fun englishConnectedAccountAndAdvancedOptionsRemainTranslatedAtLargeText() {
        rule.setContent {
            val localized = app.reelstack.localization.AppLanguages.wrap(LocalContext.current,
                app.reelstack.localization.AppLanguage.ENGLISH)
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides localized.resources.configuration) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme { ConnectionEditorSheet(ConnectionDraft(ServiceKind.EMBY, "Emby", "https://example.com", "",
                        authMode = ConnectionAuthMode.ACCOUNT), true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
                }
            }
        }
        rule.onNodeWithText("Emby is signed in").assertIsDisplayed()
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.onNodeWithText("Advanced options").performScrollTo().performClick()
        rule.onNodeWithText("Connection name").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Alternate address (optional)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Sign out").performScrollTo().performClick()
        rule.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "account-dialog.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        rule.onNodeWithText("Sign out of Emby?").assertIsDisplayed()
        rule.onNodeWithTag("sign-out-note").performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        val scrollRange = rule.onNodeWithTag("sign-out-note").fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange]
        rule.runOnIdle { assertTrue(scrollRange.value() > 0f) }
        rule.onNodeWithTag("sign-out-cancel").performClick()
    }

    @Test fun englishCompanionConsentNamesDestinationAndRemainsOptIn() {
        var value by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "https://example.com", "",
            authMode = ConnectionAuthMode.ACCOUNT))
        rule.setContent {
            val localized = app.reelstack.localization.AppLanguages.wrap(LocalContext.current,
                app.reelstack.localization.AppLanguage.ENGLISH)
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides localized.resources.configuration) {
                ReelstackTheme { ConnectionEditorSheet(value, true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    onCompanionLoginChange = { enabled, url -> value = value.copy(alsoConnect = enabled, companionUrl = url) }) }
            }
        }
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.runOnIdle { assertFalse(value.alsoConnect) }
        rule.onNodeWithText("Also sign in to Seerr").performScrollTo().performClick()
        rule.onNodeWithText("Address for Seerr").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("The same username and password", substring = true).performScrollTo().assertIsDisplayed()
        rule.runOnIdle { assertTrue(value.alsoConnect) }
    }
}
