package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import app.reelstack.ui.components.LanguagePreference
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

class LanguagePickerLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeTextKeepsEveryLanguageAndCloseReachable() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                ReelstackTheme { LanguagePreference() }
            }
        }
        rule.onNodeWithTag("language-picker").performClick()
        rule.onNodeWithText("Følg eininga").performScrollTo().assertIsDisplayed()
        rule.onNode(hasText("Norsk nynorsk") and hasAnyAncestor(isDialog())).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("English · Førehandsvising").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Lukk").assertIsDisplayed().performClick()
    }
}
