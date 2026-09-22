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
        rule.runOnUiThread { AppLanguages.select(rule.activity, AppLanguage.NYNORSK) }
        rule.waitForIdle()
        rule.runOnUiThread { AppLanguages.select(rule.activity, AppLanguage.ENGLISH) }
        rule.waitForIdle()
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(rule.activity))
        rule.activityRule.scenario.recreate()
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(rule.activity))
        rule.runOnUiThread { AppLanguages.select(rule.activity, AppLanguage.NYNORSK) }
        rule.waitForIdle()
        assertEquals(AppLanguage.NYNORSK, AppLanguages.selected(rule.activity))
    }
}
