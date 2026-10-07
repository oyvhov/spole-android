package app.reelstack

import androidx.compose.runtime.*
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
        rule.onNodeWithTag("setup-other").performClick()
        rule.onNodeWithTag("setup-combined").assertDoesNotExist()
    }

    @Test fun startStaysReachableWhileEmptyAndRetryPreservesAddresses() {
        // The button used to be disabled until both addresses were typed. A disabled button
        // cannot take focus, so the remote skipped it; the view model now answers an empty field.
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", "", "", simpleSetup = true, alsoConnect = true))
        var starts = 0
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ -> CombinedSetupSheet(draft,
            { draft = draft.copy(url = it) }, { draft = draft.copy(companionUrl = it) }, { starts++ }, {}) } } }
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        enter("setup-jellyfin-url", "https://media.example")
        enter("setup-seerr-url", "https://requests.example")
        rule.onNodeWithTag("setup-start").performScrollTo().performClick()
        assertEquals(1, starts)
        rule.runOnIdle { draft = draft.copy(error = "Prøv igjen") }
        rule.onNodeWithTag("setup-jellyfin-url").assertTextContains("https://media.example")
        rule.onNodeWithTag("setup-error").assertExists()
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
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("setup-seerr-enabled").performScrollTo().performClick()
        rule.onNodeWithTag("setup-seerr-url").assertDoesNotExist()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        rule.onNodeWithTag("setup-method").performScrollTo().performClick()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
        enter("setup-username", "viewer")
        rule.onNodeWithTag("setup-password").performScrollTo().assertExists()
        rule.onNodeWithTag("setup-start").performScrollTo().assertIsEnabled()
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

    private val stova = app.reelstack.data.network.DiscoveredServer(
        app.reelstack.data.network.DiscoveredServerKind.JELLYFIN, "4f1c0d2a", "Stova", "http://192.168.1.20:8097")

    private fun television() = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(android.app.UiModeManager::class.java).currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

    @Test fun serverOnTheNetworkIsListedAndTakesTheRemote() {
        var picked: app.reelstack.data.network.DiscoveredServer? = null
        rule.setContent { ReelstackTheme { androidx.compose.material3.Surface {
            WelcomeScreen(ReelstackUiState(discoveredServers = listOf(stova)), {}, {}, onPickServer = { picked = it })
        } } }
        val card = rule.onNodeWithTag("setup-server-4f1c0d2a").assertIsDisplayed()
        rule.onNodeWithText("Stova").assertIsDisplayed()
        rule.onNodeWithText("Jellyfin · 192.168.1.20:8097").assertIsDisplayed()
        if (television()) card.assertIsFocused().performKeyInput { pressKey(Key.Enter) } else card.performClick()
        assertEquals(stova, picked)
        rule.onNodeWithTag("setup-combined").assertExists()
    }

    @Test fun emptyNetworkSaysSoAndOffersAnotherSearch() {
        var rescans = 0
        rule.setContent { ReelstackTheme { androidx.compose.material3.Surface {
            WelcomeScreen(ReelstackUiState(), {}, {}, onRescanServers = { rescans++ })
        } } }
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        rule.onNodeWithText(context.getString(app.reelstack.R.string.setup_discovery_none)).assertIsDisplayed()
        rule.onNodeWithTag("setup-rescan").performScrollTo().performClick()
        assertEquals(1, rescans)
        // The quiet three-step picture belongs to the two-pane television screen and reads as one sentence.
        if (television()) rule.onNodeWithTag("setup-journey").assertExists()
            .assert(hasContentDescription(context.getString(app.reelstack.R.string.setup_journey_description)))
    }

    @Test fun pickedServerOpensOnApprovalWithNothingToType() {
        var closed = 0
        var starts = 0
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ ->
            CombinedSetupSheet(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", stova.address, "", simpleSetup = true,
                authMode = ConnectionAuthMode.QUICK_CONNECT, serverName = "Stova", addressResolved = true),
                {}, {}, { starts++ }, { closed++ })
        } } }
        rule.onNodeWithText("Logg inn på Stova").assertIsDisplayed()
        rule.onNodeWithTag("setup-jellyfin-url").assert(!hasSetTextAction())
        val start = rule.onNodeWithTag("setup-start")
        if (television()) start.assertIsFocused().performKeyInput { pressKey(Key.Enter) } else start.performScrollTo().performClick()
        assertEquals(1, starts)
        rule.onNodeWithTag("setup-change-server").performScrollTo().performClick()
        assertEquals(1, closed)
    }

    @Test fun usersTheServerListsFillInTheName() {
        val users = listOf(app.reelstack.data.network.PublicUser("u1", "Kari", hasPassword = true),
            app.reelstack.data.network.PublicUser("u2", "Åse", hasPassword = true))
        var draft by mutableStateOf(ConnectionDraft(ServiceKind.JELLYFIN, "Jellyfin", stova.address, "", simpleSetup = true,
            authMode = ConnectionAuthMode.ACCOUNT, addressResolved = true, serverName = "Stova", loginUsers = users))
        rule.setContent { ReelstackTheme { StableSheetDialog(true, {}) { _, _, _ ->
            CombinedSetupSheet(draft, {}, {}, {}, {}, onUsername = { draft = draft.copy(username = it) },
                onPickUser = { draft = draft.copy(username = it.name) })
        } } }
        rule.onNodeWithTag("login-user-u2").performScrollTo().performClick()
        assertEquals("Åse", draft.username)
        rule.onNodeWithTag("login-user-u2").assertIsSelected()
    }

    @Test fun welcomeAtDoubleFontSizeKeepsBothChoicesReachable() {
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) { ReelstackTheme { androidx.compose.material3.Surface {
            WelcomeScreen(ReelstackUiState(), {}, {}, onCombined = {})
        } } } }
        rule.onNodeWithTag("setup-combined").performScrollTo().assertIsDisplayed()
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
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val tv = context.getSystemService(android.app.UiModeManager::class.java).currentModeType ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        rule.onNodeWithTag(if (tv) "tv-quick-code" else "setup-code").performScrollTo().assertTextEquals("123  456")
        rule.onNodeWithText(context.getString(app.reelstack.R.string.quick_copy)).performScrollTo().performClick()
        rule.onNodeWithText(context.getString(app.reelstack.R.string.quick_copied)).assertIsDisplayed()
        rule.onNodeWithTag("setup-start").assertDoesNotExist()
        rule.onNodeWithTag("setup-cancel").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }
}
