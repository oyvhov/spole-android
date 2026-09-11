package app.reelstack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SettingsAccounts
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountPanelsTest {
    @get:Rule val rule = createComposeRule()

    private val seerr = ServiceAccount(
        source = ServiceKind.SEERR,
        id = "42",
        displayName = "Ingrid Marie",
        isPersonal = true,
    )
    private val jellyfin = ServiceAccount(
        source = ServiceKind.JELLYFIN,
        id = "jellyfin-test-user",
        displayName = "Ingrid på Jellyfin",
        isPersonal = true,
    )

    // In-memory fixtures only: no stored accounts, credentials or remote avatar URLs.
    private fun accountState() = ReelstackUiState(
        connections = listOf(
            ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", sessionCookie = true),
            ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example"),
        ),
        accounts = mapOf(ServiceKind.SEERR to seerr, ServiceKind.JELLYFIN to jellyfin),
    )

    @Test fun demoWithoutSeerrHasNoRequestIdentityPanel() {
        rule.setContent {
            ReelstackTheme { RequestIdentity(ReelstackUiState(), onSignIn = {}) }
        }
        rule.onNodeWithTag("request-identity").assertDoesNotExist()
    }

    @Test fun unconfiguredSeerrHidesEvenAStaleAccount() {
        val state = accountState().copy(connections = listOf(ServiceConnection(ServiceKind.SEERR, "Seerr", " ")))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = {}) }
        }
        rule.onNodeWithTag("request-identity").assertDoesNotExist()
        rule.onNodeWithText("Som ${seerr.displayName}").assertDoesNotExist()
    }

    @Test fun settingsShowsActualProfilesAndRoutesEachAccountAction() {
        var selected: ServiceKind? = null
        rule.setContent {
            ReelstackTheme { SettingsAccounts(accountState(), onConnectionClick = { selected = it }) }
        }
        rule.onNodeWithText(seerr.displayName).assertIsDisplayed()
        rule.onNodeWithText(jellyfin.displayName).assertIsDisplayed()
        rule.onNodeWithText("Førespurnader som deg").assertDoesNotExist()
        rule.onNodeWithContentDescription("Endre Seerr-konto").performClick()
        assertEquals(ServiceKind.SEERR, selected)
        rule.onNodeWithContentDescription("Endre Jellyfin-konto").performClick()
        assertEquals(ServiceKind.JELLYFIN, selected)
    }

    @Test fun settingsAdministratorProfileOffersPersonalSignIn() {
        var selected: ServiceKind? = null
        val state = accountState().copy(accounts = mapOf(ServiceKind.SEERR to seerr.copy(isPersonal = false)))
        rule.setContent {
            ReelstackTheme { SettingsAccounts(state, onConnectionClick = { selected = it }) }
        }
        rule.onNodeWithText(seerr.displayName).assertIsDisplayed()
        rule.onNodeWithText("Administratornøkkel · berre oversikt").assertIsDisplayed()
        rule.onNodeWithText("Førespurnader som deg").assertDoesNotExist()
        rule.onNodeWithContentDescription("Logg inn på Seerr").performClick()
        assertEquals(ServiceKind.SEERR, selected)
    }

    @Test fun requestIdentityShowsPersonalNameAndOpensAccountAction() {
        var opened = 0
        rule.setContent {
            ReelstackTheme { RequestIdentity(accountState(), onSignIn = { opened++ }) }
        }
        rule.onNodeWithText("Som ${seerr.displayName}").assertIsDisplayed()
        rule.onNodeWithContentDescription("Endre konto for førespurnader").performClick()
        assertEquals(1, opened)
    }

    @Test fun administratorAccountIsNeverPresentedAsPersonalRequestIdentity() {
        var opened = 0
        val state = accountState().copy(accounts = mapOf(ServiceKind.SEERR to seerr.copy(isPersonal = false)))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = { opened++ }) }
        }
        rule.onNodeWithText("Som ${seerr.displayName}").assertDoesNotExist()
        rule.onNodeWithText("Administratornøkkel · berre oversikt").assertIsDisplayed()
        rule.onNodeWithText("Logg inn med Jellyfin").performClick()
        assertEquals(1, opened)
    }

    @Test fun failedVerificationHidesStalePersonalIdentityAndOffersRetry() {
        var opened = 0
        val state = accountState().copy(accountErrors = mapOf(ServiceKind.SEERR to "Testfeil"))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = { opened++ }) }
        }
        rule.onNodeWithText("Som ${seerr.displayName}").assertDoesNotExist()
        rule.onNodeWithText("Kunne ikkje stadfeste Seerr-kontoen.").assertIsDisplayed()
        rule.onNodeWithText("Opne innlogginga for å prøve igjen.").assertIsDisplayed()
        rule.onNodeWithText("Prøv igjen").performClick()
        assertEquals(1, opened)
    }

    @Test fun backgroundVerificationKeepsTheVerifiedIdentityVisible() {
        val state = accountState().copy(loadingAccounts = setOf(ServiceKind.SEERR))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = {}) }
        }
        rule.onNodeWithText("Stadfestar kontoen for førespurnader…").assertDoesNotExist()
        rule.onNodeWithText("Som ${seerr.displayName}").assertIsDisplayed()
        rule.onNodeWithContentDescription("Endre konto for førespurnader").assertIsDisplayed()
    }

    @Test fun firstVerificationShowsLoadingWithoutInventingAnIdentity() {
        val state = accountState().copy(accounts = emptyMap(), loadingAccounts = setOf(ServiceKind.SEERR))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = {}) }
        }
        rule.onNodeWithText("Stadfestar kontoen for førespurnader…").assertIsDisplayed()
        rule.onNodeWithText("Som ${seerr.displayName}").assertDoesNotExist()
    }

    @Test fun failedJellyfinVerificationSaysTheStoredSignInIsStillThere() {
        var selected: ServiceKind? = null
        val state = accountState().copy(
            accounts = mapOf(ServiceKind.SEERR to seerr),
            accountErrors = mapOf(ServiceKind.JELLYFIN to "Mellombels feil"),
        )
        rule.setContent {
            ReelstackTheme { SettingsAccounts(state, onConnectionClick = { selected = it }) }
        }
        rule.onNodeWithText("Innlogginga er lagra · kunne ikkje stadfeste no").assertIsDisplayed()
        rule.onNodeWithContentDescription("Prøv igjen på Jellyfin").performClick()
        assertEquals(ServiceKind.JELLYFIN, selected)
    }

    @Test fun jellyfinIdentityAloneDoesNotInventASeerrRequestIdentity() {
        var opened = 0
        val state = accountState().copy(accounts = mapOf(ServiceKind.JELLYFIN to jellyfin))
        rule.setContent {
            ReelstackTheme { RequestIdentity(state, onSignIn = { opened++ }) }
        }
        rule.onNodeWithText("Som ${jellyfin.displayName}").assertDoesNotExist()
        rule.onNodeWithText("Logg inn for å sende førespurnader som deg.").assertIsDisplayed()
        rule.onNodeWithText("Logg inn med Jellyfin").performClick()
        assertEquals(1, opened)
    }
}
