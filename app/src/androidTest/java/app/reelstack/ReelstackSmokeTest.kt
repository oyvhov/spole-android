package app.reelstack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test

class ReelstackSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsCoreMediaState() {
        composeRule.onNodeWithText("HomeReel").assertIsDisplayed()
        composeRule.onNodeWithText("Spelar no").assertIsDisplayed()
        composeRule.onNodeWithText("Førehandsvising").assertDoesNotExist()
        composeRule.onAllNodesWithText("Severance")[0].assertIsDisplayed()
    }

    @Test
    fun demoRequestIsClearlyKeptLocal() {
        composeRule.onNodeWithText("Oppdag").performClick()
        composeRule.onNodeWithText("Legg til").performClick()

        composeRule.onNodeWithText("Lagd til").assertIsDisplayed()
        composeRule.onNodeWithText("Tittelen er lagd til lokalt · kople til Seerr for å sende han vidare").assertIsDisplayed()
    }

    @Test
    fun libraryRailOpensTitleDetails() {
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onAllNodesWithText("The Odyssey")[0].assertIsDisplayed().performClick()

        composeRule.onNodeWithText("BIBLIOTEK I JELLYFIN").assertIsDisplayed()
    }

    @Test
    fun discoverCardOpensRichDetailsWithAddLanguage() {
        composeRule.onNodeWithText("Oppdag").performClick()
        composeRule.onNodeWithText("The Last Horizon").performClick()

        composeRule.onNodeWithText("OPPDAG I SEERR").assertIsDisplayed()
        composeRule.onNodeWithText("Legg til i mediesamlinga").assertIsDisplayed()
    }

    @Test
    fun jellyfinEditorOffersQuickConnectAndAccountLogin() {
        composeRule.onNodeWithText("Innstillingar").performClick()
        composeRule.onNodeWithText("Jellyfin").performClick()

        composeRule.onNodeWithText("Quick Connect").assertIsDisplayed()
        composeRule.onNodeWithText("Start Quick Connect").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Brukarnamn").performClick()
        composeRule.onNodeWithText("Tilgangsteikn").assertIsDisplayed()
        composeRule.onAllNodesWithText("Brukarnamn")[1].performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Passord").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun homeSectionsCanBeEditedInSettings() {
        composeRule.onNodeWithText("Innstillingar").performClick()

        composeRule.onNodeWithText("Heimskjerm").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nyleg lagde til filmar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nyleg lagde til seriar").performScrollTo().assertIsDisplayed()
    }
}
