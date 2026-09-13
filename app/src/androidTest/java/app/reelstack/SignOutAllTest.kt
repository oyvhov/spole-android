package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SignOutAllTest {
    @get:Rule val rule = createComposeRule()

    @Test fun allSecretsRemovedWhileLayoutAndRememberedAddressesSurvive() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ConnectionRepository(context)
        val prefs = AppPreferencesRepository(context)
        val order = prefs.homeRowOrder
        val appearance = prefs.personalization
        try {
            prefs.homeRowOrder = HomeRow.entries.reversed()
            ServiceKind.entries.forEach { kind -> repository.save(ServiceConnection(kind, "Example",
                "https://${kind.name.lowercase()}.example", "fixture-secret", "fixture-user", sessionCookie = true)) }
            repository.signOut(ServiceKind.EMBY)
            repository.signOutAll()
            val recreated = ConnectionRepository(context)
            ServiceKind.entries.forEach { kind ->
                val c = recreated.get(kind)
                assertEquals("", c.token)
                assertEquals("", c.userId)
                assertEquals("", c.baseUrl)
                assertFalse(c.sessionCookie)
                assertEquals("https://${kind.name.lowercase()}.example", recreated.rememberedUrl(kind))
            }
            assertEquals(HomeRow.entries.reversed(), prefs.homeRowOrder)
            assertEquals(appearance, prefs.personalization)
        } finally { prefs.homeRowOrder = order; repository.signOutAll() }
    }

    @Test fun servicesOfferOneTapSignOutWithLargeText() {
        var calls = 0
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            ReelstackTheme { androidx.compose.material3.Surface { SettingsScreen(ReelstackUiState(connections = listOf(ServiceConnection(
                ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "fixture"))), PaddingValues(),
                {}, {}, {}, { _, _ -> }, onSignOutAll = { calls++ }) } }
        } }
        rule.onNodeWithTag("settings-category-ACCOUNTS").performScrollTo().performClick()
        rule.onNodeWithTag("sign-out-all").performScrollTo().assertIsDisplayed()
        rule.onRoot().saveRoadmapImage("sign-out-all-large.png")
        rule.onNodeWithTag("sign-out-all").performClick()
        rule.runOnIdle { assertEquals(1, calls) }
        rule.onAllNodes(isDialog()).assertCountEquals(0)
    }
}
