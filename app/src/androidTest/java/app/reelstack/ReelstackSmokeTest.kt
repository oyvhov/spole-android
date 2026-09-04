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
        composeRule.onNodeWithText("Preview mode · connect a service when you're ready.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Severance")[0].assertIsDisplayed()
    }

    @Test
    fun demoRequestIsClearlyKeptLocal() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Request").performClick()

        composeRule.onNodeWithText("Requested").assertIsDisplayed()
        composeRule.onNodeWithText("Demo request saved locally · connect Seerr to send it").assertIsDisplayed()
    }

    @Test
    fun libraryRailOpensTitleDetails() {
        composeRule.onRoot().performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Foundation").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Jellyfin library").assertIsDisplayed()
        composeRule.onNodeWithText("42% watched").assertIsDisplayed()
    }

    @Test
    fun homeSectionsCanBeEditedInSettings() {
        composeRule.onNodeWithText("Settings").performClick()

        composeRule.onNodeWithText("Home screen").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Upcoming").performScrollTo().assertIsDisplayed()
    }
}
