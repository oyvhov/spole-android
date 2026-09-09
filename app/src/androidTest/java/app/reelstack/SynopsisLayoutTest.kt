package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.ui.components.ExpandableSynopsis
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SynopsisLayoutTest {
    @get:Rule val rule = createComposeRule()
    private val longText = "Ei ny oppdaging fører to vener ut på ei reise langs kysten. ".repeat(30)

    @Test fun shortAndMissingTextDoNotOfferAnUnnecessaryDisclosure() {
        var text by mutableStateOf<String?>("Ei kort forteljing.")
        rule.setContent { ReelstackTheme { ExpandableSynopsis("one", "Om filmen", text, false) } }
        rule.onNodeWithTag("overview-expand").assertDoesNotExist()
        rule.onNodeWithText("Ei kort forteljing.").assertIsDisplayed()
        rule.runOnIdle { text = null }
        rule.onNodeWithTag("overview-expand").assertDoesNotExist()
        rule.onNodeWithText("Ingen omtale tilgjengeleg.").assertIsDisplayed()
    }

    @Test fun largeTextKeepsTheHeadingAndControlSeparateAndInBounds() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme { Surface { Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                        ExpandableSynopsis("one", "Om episoden", longText, false)
                    } } }
                }
            }
        }
        val heading = rule.onNodeWithText("Om episoden").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val action = rule.onNodeWithTag("overview-expand").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(heading.right <= action.left)
        rule.onNodeWithTag("overview-expand").performClick().assertIsDisplayed()
        rule.onNodeWithText("Vis mindre").assertIsDisplayed()
        rule.onNodeWithTag("overview-expand").performClick().assertIsDisplayed()
    }

    @Test fun expansionIsPerTitleAndMetadataRefreshDoesNotCollapseIt() {
        var identity by mutableStateOf("one")
        var text by mutableStateOf(longText)
        rule.setContent { ReelstackTheme { Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
            ExpandableSynopsis(identity, "Om filmen", text, false)
        } } }
        rule.onNodeWithTag("overview-expand").performClick()
        rule.runOnIdle { text += " Slutt." }
        rule.onNodeWithText("Vis mindre").assertExists()
        rule.runOnIdle { identity = "two" }
        rule.onNodeWithText("Les heile omtalen").assertExists()
    }
}
