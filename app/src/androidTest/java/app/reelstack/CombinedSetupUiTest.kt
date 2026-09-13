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
    private fun enter(tag: String, text: String) {
        // A focused editor's cursor keeps ticking while the TV IME is open. Drive the
        // clock explicitly during entry, then dismiss the IME before resuming idle checks.
        rule.onNodeWithTag(tag).performScrollTo()
        rule.mainClock.autoAdvance = false
        try {
            rule.onNodeWithTag(tag).performTextInput(text)
            rule.mainClock.advanceTimeBy(400)
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            rule.onNodeWithTag("setup-cancel").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            rule.mainClock.advanceTimeBy(200)
        } finally { rule.mainClock.autoAdvance = true }
    }
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
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "", "", simpleSetup = true, alsoConnect = true))
        var starts = 0
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ -> CombinedSetupSheet(draft,
            { draft = draft.copy(url = it) }, { draft = draft.copy(companionUrl = it) }, { starts++ }, {}) } } }
        rule.onNodeWithTag("setup-start").assertIsNotEnabled()
        capture("setup-addresses-empty")
        enter("setup-jellyfin-url", "https://media.example")
        rule.onNodeWithTag("setup-start").assertIsNotEnabled()
        enter("setup-seerr-url", "https://requests.example")
        rule.onNodeWithTag("setup-start").performScrollTo().performClick()
        assertEquals(1, starts)
        rule.runOnIdle { draft = draft.copy(error = "Prøv igjen") }
        rule.onNodeWithTag("setup-jellyfin-url").assertTextContains("https://media.example")
        rule.onNodeWithTag("setup-error").assertExists()
        capture("setup-addresses")
    }

    @Test fun passwordAndJellyfinOnlyRemainReachableAtDoubleFontSize() {
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "",
            simpleSetup = true, authMode = ConnectionAuthMode.QUICK_CONNECT, alsoConnect = true))
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme {
            StableSheetDialog(true, {}) { _, _, _ -> CombinedSetupSheet(draft, {}, {}, {}, {},
                onAuthMode = { draft = draft.copy(authMode = it) },
                onUsername = { draft = draft.copy(username = it) }, onPassword = { draft = draft.copy(password = it) },
                onSeerrEnabled = { draft = draft.copy(alsoConnect = it) }) }
        } } }
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("setup-seerr-enabled").performScrollTo().performClick()
        rule.onNodeWithTag("setup-seerr-url").assertDoesNotExist()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("setup-method").performScrollTo().performClick()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsNotEnabled()
        enter("setup-username", "viewer")
        rule.onNodeWithTag("setup-password").performScrollTo().assertExists()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        capture("setup-password-large")
    }

    @Test fun importingAddressesStillRequiresUserToStartLogin() {
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "", "", simpleSetup = true))
        var starts = 0
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ ->
            CombinedSetupSheet(draft, {}, {}, { starts++ }, {}, onImport = {
                val setup = app.reelstack.data.network.SetupLink.parse(it)
                draft = draft.copy(url = setup.jellyfin, companionUrl = setup.seerr, alsoConnect = setup.seerr.isNotBlank(), setupImported = true)
            })
        } } }
        rule.onNodeWithText("Eg har ei oppsettslenkje").performScrollTo().performClick()
        enter("setup-link",
            app.reelstack.data.network.SetupLink("https://media.example", "https://requests.example").encode())
        rule.onNodeWithTag("setup-import").performScrollTo().performClick()
        rule.onNodeWithTag("setup-jellyfin-url").assertTextContains("https://media.example", substring = true)
        rule.onNodeWithTag("setup-seerr-url").assertTextContains("https://requests.example", substring = true)
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        assertEquals(0, starts)
        rule.onNodeWithTag("setup-edit-addresses").performScrollTo().performClick()
        rule.onNodeWithTag("setup-jellyfin-url").assert(hasSetTextAction())
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
