package app.reelstack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import org.junit.Before
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Rule
import org.junit.Test

class ReelstackSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun enterExplicitPreviewIfFirstLaunch() {
        if (composeRule.onAllNodesWithText("Alt du ser.\nÉin stad.").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onRoot().performTouchInput { swipeUp() }
            composeRule.onNodeWithText("Utforsk med demodata først").performScrollTo().performClick()
        }
    }

    @Test
    fun homeScreenShowsCoreMediaState() {
        composeRule.onNodeWithText("HomeReel").assertDoesNotExist()
        composeRule.onNodeWithText("Spelar no").assertIsDisplayed()
        composeRule.onNodeWithText("Førehandsvising").assertDoesNotExist()
        composeRule.onAllNodesWithText("Severance")[0].assertIsDisplayed()
    }

    @Test
    fun upcomingOpensCalendarAgenda() {
        composeRule.onNodeWithTag("home-feed").performScrollToNode(hasText("Kalender"))
        composeRule.onNodeWithText("Kalender").performScrollTo().assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Filmar heime og nye episodar")
            .assertIsDisplayed()
    }

    @Test
    fun demoRequestIsClearlyKeptLocal() {
        composeRule.onNodeWithText("Oppdag").performClick()
        composeRule.onNodeWithText("Legg til").performClick()
        composeRule.onNodeWithTag("confirm-request").performClick()

        composeRule.onAllNodesWithText("Lagd til")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Tittelen er lagd til lokalt · kople til Seerr for å sende han vidare").assertIsDisplayed()
    }

    @Test
    fun libraryRailOpensTitleDetails() {
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onAllNodesWithText("The Odyssey")[0].assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Om filmen").assertIsDisplayed()
    }

    @Test
    fun discoverCardOpensRichDetailsWithAddLanguage() {
        composeRule.onNodeWithText("Oppdag").performClick()
        composeRule.onNodeWithText("The Last Horizon").performClick()

        composeRule.onNode(hasText("Seerr") and hasAnyAncestor(hasTestTag("sheet-viewport"))).assertIsDisplayed()
        composeRule.onNode(hasText("The Last Horizon") and hasAnyAncestor(hasTestTag("sheet-viewport"))).assertIsDisplayed()
        composeRule.onNodeWithText("Om filmen").assertIsDisplayed()
        composeRule.onNodeWithText("Legg til i mediesamlinga").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun jellyfinEditorOffersQuickConnectAndAccountLogin() {
        composeRule.onNodeWithText("Innstillingar").performClick()
        composeRule.onNode(hasText("Jellyfin") and hasClickAction()).performScrollTo().performClick()

        composeRule.onNodeWithText("Tenaradresse").performTextInput("https://media.example.com")
        composeRule.onNodeWithText("Hald fram").performClick()

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

        composeRule.onNodeWithTag("settings-feed").performScrollToNode(hasText("HomeReel"))
        composeRule.onNodeWithText("HomeReel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-feed").performScrollToNode(hasText("Heimskjerm"))
        composeRule.onNodeWithText("Heimskjerm").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-feed").performScrollToNode(hasText("Emby · Filmar"))
        composeRule.onNodeWithText("Emby · Filmar").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-feed").performScrollToNode(hasText("Emby · Seriar"))
        composeRule.onNodeWithText("Emby · Seriar").assertIsDisplayed()
    }

    @Test
    fun seerrOffersJellyfinAccountAndQuickConnect() {
        composeRule.onNodeWithText("Innstillingar").performClick()
        composeRule.onNode(hasText("Seerr") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNodeWithText("Tenaradresse").performTextInput("https://seerr.example.com")
        composeRule.onNodeWithText("Hald fram").performClick()
        composeRule.onNodeWithText("Jellyfin-konto").assertIsDisplayed()
        composeRule.onNodeWithText("Brukarnamn").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Passord").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Quick Connect").performScrollTo().performClick()
        composeRule.onNodeWithText("Start Quick Connect").performScrollTo().assertIsDisplayed()
    }
}
