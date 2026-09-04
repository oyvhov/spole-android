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
        composeRule.onNodeWithText("Bestill").performClick()

        composeRule.onNodeWithText("Bestilt").assertIsDisplayed()
        composeRule.onNodeWithText("Demobestillinga er lagra lokalt · kople til Seerr for å sende henne").assertIsDisplayed()
    }

    @Test
    fun libraryRailOpensTitleDetails() {
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onAllNodesWithText("The Odyssey")[0].assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Bibliotek i Jellyfin").assertIsDisplayed()
        composeRule.onNodeWithText("Opne Jellyfin for å spele av tittelen.").assertIsDisplayed()
    }

    @Test
    fun homeSectionsCanBeEditedInSettings() {
        composeRule.onNodeWithText("Innstillingar").performClick()

        composeRule.onNodeWithText("Heimskjerm").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nyleg lagde til filmar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nyleg lagde til seriar").performScrollTo().assertIsDisplayed()
    }
}
