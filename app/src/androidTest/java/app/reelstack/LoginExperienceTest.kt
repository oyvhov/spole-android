package app.reelstack

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.ui.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LoginExperienceTest {
    @get:Rule val rule = createComposeRule()
    private fun draft(kind: ServiceKind) = ConnectionDraft(kind, kind.displayName, "https://media.example", "", authMode = ConnectionAuthMode.ACCOUNT)

    @Test fun uppercaseAddressIsNormalizedBeforeCredentialsStep() {
        val value = mutableStateOf(draft(ServiceKind.EMBY).copy(url = "HTTPS://EMBY.EXAMPLE.COM/Media/"))
        rule.setContent { ReelstackTheme {
            ConnectionEditorSheet(value.value, false, {}, {},
                { value.value = value.value.copy(url = it) }, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithText("Hald fram").performScrollTo().performClick()
        assertEquals("https://emby.example.com/Media", value.value.url)
        rule.onNode(hasText("Brukarnamn") and hasSetTextAction()).assertIsDisplayed()
    }

    @Test fun malformedAddressStaysOnAddressStepAndCanBeCorrected() {
        val value = mutableStateOf(draft(ServiceKind.JELLYFIN).copy(url = "https://media .example.com"))
        rule.setContent { ReelstackTheme {
            ConnectionEditorSheet(value.value, false, {}, {},
                { value.value = value.value.copy(url = it) }, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithText("Hald fram").performScrollTo().performClick()
        rule.onNodeWithText("Tenaradressa inneheld mellomrom. Fjern dei og prøv igjen.").assertIsDisplayed()
        rule.onNodeWithText("Brukarnamn").assertDoesNotExist()
        rule.onNodeWithTag("connection-url").performTextReplacement("JELLYFIN.EXAMPLE.COM")
        rule.onNodeWithText("Hald fram").performScrollTo().performClick()
        assertEquals("https://jellyfin.example.com", value.value.url)
        rule.onNode(hasText("Brukarnamn") and hasSetTextAction()).assertIsDisplayed()
    }

    @Test fun signedInServiceStartsCompactAndRevealsAccountFieldsOnDemand() {
        rule.setContent { ReelstackTheme {
            ConnectionEditorSheet(draft(ServiceKind.EMBY), true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithText("Emby").assertIsDisplayed()
        rule.onNodeWithText("Emby er innlogga").assertIsDisplayed()
        rule.onNodeWithText("Passord").assertDoesNotExist()
        rule.onNodeWithText("Logg ut").assertDoesNotExist()
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.onNodeWithText("Quick Connect").assertDoesNotExist()
        rule.onNodeWithText("API-nøkkel").assertDoesNotExist()
        rule.onNodeWithText("Passord").assertIsDisplayed()
        rule.onNodeWithText("Logg ut").assertIsDisplayed()
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Passord").assertDoesNotExist()
    }

    @Test fun companionLoginIsOptInAndShowsDestinationAndConsent() {
        val value = mutableStateOf(draft(ServiceKind.JELLYFIN))
        rule.setContent { ReelstackTheme {
            ConnectionEditorSheet(value.value, true, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                onCompanionLoginChange = { enabled, url -> value.value = value.value.copy(alsoConnect = enabled, companionUrl = url) })
        } }
        assertFalse(value.value.alsoConnect)
        rule.onNodeWithText("Adresse til Seerr").assertDoesNotExist()
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.onNodeWithText("Logg inn på Seerr òg").performScrollTo().performClick()
        rule.onNodeWithText("Adresse til Seerr").performScrollTo().performTextInput("https://seerr.example")
        assertTrue(value.value.alsoConnect)
        assertEquals("https://seerr.example", value.value.companionUrl)
        rule.onNodeWithText("Logg inn på begge").performScrollTo().assertIsDisplayed()
    }

    @Test fun quickConnectCopiesOnlyVisibleCodeNotSecret() {
        rule.setContent { ReelstackTheme {
            QuickConnectPanel(draft(ServiceKind.JELLYFIN).copy(quickConnectCode = "123456", quickConnectWaiting = true))
        } }
        rule.onNodeWithText("Kopier kode").performClick()
        rule.onNodeWithText("Kopiert").assertIsDisplayed()
        rule.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals("123456", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test fun signOutRequiresExplicitConfirmationAndCanBeCancelled() {
        var removed = 0
        rule.setContent { ReelstackTheme {
            ConnectionEditorSheet(draft(ServiceKind.SEERR), true, {}, {}, {}, {}, {}, {}, {}, {}, {}, { removed++ })
        } }
        rule.onNodeWithTag("connected-service-summary").performClick()
        rule.onNodeWithText("Logg ut").performClick()
        rule.onNodeWithText("Logg ut av Seerr?").assertIsDisplayed()
        assertEquals(0, removed)
        rule.onNodeWithText("Avbryt").performClick()
        assertEquals(0, removed)
        rule.onNodeWithText("Logg ut").performClick()
        rule.onNode(hasText("Logg ut") and hasAnyAncestor(isDialog())).performClick()
        assertEquals(1, removed)
    }

    @Test fun signOutClearsIdentityAndSecretButRemembersOnlyAddress() {
        // Isolated instrumentation emulator only. Never run this suite against real accounts.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ConnectionRepository(context)
        repository.save(ServiceConnection(ServiceKind.EMBY, "Test", "https://emby.example", "test-token", userId = "test-user"))
        repository.signOut(ServiceKind.EMBY)
        val signedOut = ConnectionRepository(context).get(ServiceKind.EMBY)
        assertEquals("", signedOut.token)
        assertEquals("", signedOut.userId)
        assertEquals("", signedOut.baseUrl)
        assertEquals("https://emby.example", repository.rememberedUrl(ServiceKind.EMBY))
    }
}
