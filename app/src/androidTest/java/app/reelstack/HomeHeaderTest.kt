package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeHeaderTest {
    @get:Rule val rule = createComposeRule()
    private val seerr = ServiceAccount(ServiceKind.SEERR, "7", "Maya")
    private val jellyfin = ServiceAccount(ServiceKind.JELLYFIN, "jf", "Jonas")
    private val emby = ServiceAccount(ServiceKind.EMBY, "em", "Ada")
    private fun state() = ReelstackUiState(
        connections = listOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY).map {
            ServiceConnection(it, it.displayName, "https://test.example", "test", sessionCookie = it == ServiceKind.SEERR)
        },
        accounts = listOf(seerr, jellyfin, emby).associateBy { it.source },
        sessions = emptyList(), homeSections = setOf(HomeSection.NOW_PLAYING),
    )
    @Test fun seerrIdentityLeadsAndOpensAccountWithoutGreetingOrDate() {
        var opened = false
        rule.setContent { ReelstackTheme { HomeScreen(state(), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}, { opened = true }) } }
        rule.onNodeWithContentDescription("Opne kontoen til Maya · Seerr").assertIsDisplayed().performClick()
        assertTrue(opened)
        listOf("God morgon", "God ettermiddag", "God kveld", "Ingen aktive avspelingar", "Spelar no").forEach {
            rule.onNodeWithText(it).assertDoesNotExist()
        }
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d. MMMM", java.util.Locale.forLanguageTag("nn-NO")))
        rule.onNode(hasText(today, ignoreCase = true)).assertDoesNotExist()
        rule.onNodeWithText("Reelune").assertIsDisplayed()
    }
    @Test fun unverifiedOrRemovedSeerrFallsBackToJellyfinThenEmby() {
        val current = mutableStateOf(state().copy(accountErrors = mapOf(ServiceKind.SEERR to "Expired")))
        rule.setContent { ReelstackTheme { HomeScreen(current.value, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithContentDescription("Opne kontoen til Jonas · Jellyfin").assertIsDisplayed()
        rule.runOnIdle { current.value = current.value.copy(accounts = mapOf(ServiceKind.EMBY to emby)) }
        rule.onNodeWithContentDescription("Opne kontoen til Ada · Emby").assertIsDisplayed()
        rule.onNodeWithContentDescription("Opne kontoen til Jonas · Jellyfin").assertDoesNotExist()
    }
    @Test fun sharedApiKeyNeverBecomesPersonalAvatar() {
        val current = state().copy(accounts = mapOf(ServiceKind.SEERR to seerr.copy(isPersonal = false)))
        rule.setContent { ReelstackTheme { HomeScreen(current, PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithContentDescription("Opne kontoinnstillingar").assertIsDisplayed()
        rule.onNodeWithContentDescription("Opne kontoen til Maya · Seerr").assertDoesNotExist()
    }
    @Test fun refreshDoesNotInsertAnEmptyPlaybackMessage() {
        rule.setContent { ReelstackTheme { HomeScreen(state().copy(isRefreshing = true), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Ingen aktive avspelingar").assertDoesNotExist()
        rule.onNodeWithText("Sjekkar avspelingar…").assertDoesNotExist()
        rule.onNodeWithText("Spelar no").assertDoesNotExist()
        rule.onNodeWithTag("home-account").assertIsDisplayed()
    }
}
