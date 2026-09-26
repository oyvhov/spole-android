package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI-only preview tests. Run on the isolated test AVD, never the signed-in review device. */
class HomeSearchNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun label(id: Int): String = rule.activity.getString(id)

    private fun selectTab(id: Int) {
        val name = label(id)
        if (rule.onAllNodesWithContentDescription(name).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription(name).performClick()
        } else {
            rule.onNodeWithContentDescription(
                label(R.string.settings_tv_navigation), substring = true,
            ).performClick()
            val option = rule.onNodeWithText(name)
            if (!option.isDisplayed()) option.performScrollTo()
            option.assertIsDisplayed().performClick()
        }
    }

    @Before fun enterPreview() {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("startup-cover").fetchSemanticsNodes().isEmpty()
        }
        // A fresh install first asks how to set up, then welcomes; the smoke test walks the same way.
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithTag("setup-other").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText(label(R.string.welcome_title)).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag("home-feed").fetchSemanticsNodes().isNotEmpty()
        }
        if (rule.onAllNodesWithTag("setup-other").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("setup-other").performClick()
        }
        if (rule.onAllNodesWithText(label(R.string.welcome_title)).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(label(R.string.welcome_demo)).performScrollTo().performClick()
        }
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodesWithTag("home-feed").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Search is its own screen over Home, not a jump to Discover (docs/GLOBALT_SOK_PLAN.md, GS-4). */
    @Test fun searchIconOpensItsOwnScreenAndClosingReturnsHome() {
        // The line under the header is off unless chosen in Settings; the icon is always there.
        rule.onNodeWithTag("home-search").assertDoesNotExist()
        rule.onNodeWithTag("home-search-button").assertIsDisplayed().assertHasClickAction().performClick()
        rule.onNodeWithTag("global-search-field").assertIsDisplayed().assertIsFocused()
            .performTextInput("Severance")
        rule.onNodeWithTag("global-search-field").assertTextContains("Severance")
        rule.onNodeWithTag("discover-search").assertDoesNotExist()
        rule.onNodeWithTag("global-search-close").performClick()
        rule.onNodeWithTag("global-search").assertDoesNotExist()
        rule.onNodeWithTag("home-search-button").assertIsDisplayed()
    }

    @Test fun ordinaryDiscoverVisitDoesNotAutofocusAndSearchLeavesDiscoverAlone() {
        selectTab(R.string.nav_discover)
        // The tab change travels through StateFlow before AnimatedContent creates the field.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("discover-search").fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithTag("discover-search").assertIsNotFocused()
        rule.onNodeWithText(label(R.string.filter_series)).performClick()
        selectTab(R.string.nav_home)
        rule.onNodeWithTag("home-search-button").performClick()
        rule.onNodeWithTag("global-search-field").performTextInput("Silo")
        rule.onNodeWithTag("global-search-close").performClick()
        selectTab(R.string.nav_discover)
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("discover-search").fetchSemanticsNodes().size == 1
        }
        // Discover keeps its own filter and its own, empty, field.
        rule.onNodeWithText(label(R.string.filter_series)).assertIsSelected()
        rule.onNodeWithTag("discover-search").assert(hasText("Silo", substring = true).not())
    }

    @Test fun searchOpensEmptyAndTheClearButtonEmptiesIt() {
        rule.onNodeWithTag("home-search-button").performClick()
        rule.onNodeWithTag("global-search-field").performTextInput("Silo")
        rule.onNodeWithTag("global-search-field").performImeAction()
        rule.onNodeWithContentDescription(label(R.string.search_clear)).performClick()
        rule.onNodeWithContentDescription(label(R.string.search_clear)).assertDoesNotExist()
        rule.onNodeWithTag("global-search-field").performTextInput("Dune")
        rule.onNodeWithTag("global-search-close").performClick()
        rule.onNodeWithTag("home-search-button").performClick()
        rule.onNodeWithTag("global-search-field").assert(hasText("Dune", substring = true).not())
    }

    @Test fun theSearchLineComesBackWhenChosenInSettings() {
        val repository = app.reelstack.data.repository.AppPreferencesRepository(rule.activity)
        val before = repository.personalization
        try {
            rule.runOnUiThread { repository.personalization = before.copy(showHomeSearchBar = true) }
            rule.waitUntil(timeoutMillis = 5_000) { rule.onAllNodesWithTag("home-search").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("home-search").performClick()
            rule.onNodeWithTag("global-search-field").assertIsDisplayed()
            rule.onNodeWithTag("global-search-close").performClick()
        } finally {
            rule.runOnUiThread { repository.personalization = before }
        }
    }
}
