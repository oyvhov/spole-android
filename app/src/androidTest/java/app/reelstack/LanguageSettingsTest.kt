package app.reelstack

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LanguageSettingsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After fun restoreFixtureLanguage() {
        rule.runOnUiThread { AppLanguages.select(rule.activity, AppLanguage.NYNORSK) }
        rule.waitForIdle()
    }

    @Test fun languageChangeKeepsSettingsAndPersistsAfterRecreation() {
        if (rule.onAllNodesWithText("Alt du ser.\nÉin stad.").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Utforsk med demodata først").performScrollTo().performClick()
        }
        rule.onNodeWithText("Innstillingar").performClick()
        rule.onNodeWithTag("language-picker").performScrollTo().performClick()
        rule.onNodeWithText("English · Førehandsvising").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Your services").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(rule.activity))
        rule.onNodeWithText("Your services").assertIsDisplayed()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Your services").assertIsDisplayed()
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(rule.activity))
        rule.onNodeWithTag("language-picker").performScrollTo().performClick()
        rule.onNodeWithText("Norsk nynorsk").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Tenestene dine").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(AppLanguage.NYNORSK, AppLanguages.selected(rule.activity))
    }
}
