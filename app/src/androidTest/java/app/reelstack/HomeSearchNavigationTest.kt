package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI-only preview tests. Run on the isolated test AVD, never the signed-in review device. */
class HomeSearchNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun enterPreview() {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("startup-cover").fetchSemanticsNodes().isEmpty()
        }
        if (rule.onAllNodesWithText("Alt du ser.\nÉin stad.").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Utforsk med demodata først").performScrollTo().performClick()
        }
    }

    @Test fun homeSearchIsVisibleAndHandsOffToEditableDiscovery() {
        rule.onNodeWithTag("home-search").assertIsDisplayed().assertHasClickAction()
        rule.onNodeWithText("Søk etter filmar og seriar").assertIsDisplayed()
        rule.onNodeWithTag("home-search").performClick()
        rule.onNodeWithTag("discover-search").assertIsDisplayed().assertIsFocused()
            .performTextInput("Severance")
        rule.onNodeWithTag("discover-search").assertTextContains("Severance")
        rule.onNodeWithText("Heim").performClick()
        rule.onNodeWithTag("home-search").assertIsDisplayed()
    }

    @Test fun ordinaryDiscoverVisitDoesNotAutofocusAndExplicitSearchResetsFilters() {
        rule.onNodeWithText("Oppdag").performClick()
        // The tab change travels through StateFlow before AnimatedContent creates the field.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("discover-search").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithTag("discover-search").assertIsNotFocused()
        rule.onNodeWithText("Seriar").performClick()
        rule.onNodeWithText("Heim").performClick()
        rule.onNodeWithTag("home-search").performClick()
        rule.onNodeWithTag("discover-search").assertIsFocused()
        rule.onNodeWithText("Alt").assertIsSelected()
    }

    @Test fun repeatedSearchKeepsExistingQuery() {
        rule.onNodeWithTag("home-search").performClick()
        rule.onNodeWithTag("discover-search").performTextInput("Silo")
        rule.onNodeWithTag("discover-search").performImeAction()
        rule.onNodeWithText("Heim").performClick()
        rule.onNodeWithTag("home-search").performClick()
        rule.onNodeWithTag("discover-search").assertIsDisplayed().assertIsFocused().assertTextContains("Silo")
        rule.onNodeWithContentDescription("Tøm søket").performClick()
        rule.onNodeWithContentDescription("Tøm søket").assertDoesNotExist()
    }
}
