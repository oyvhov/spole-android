package app.reelstack

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import app.reelstack.localization.localizedSeerrStatus
import org.junit.Rule
import org.junit.Test

class MediaStatusTextTest {
    @get:Rule val rule = createComposeRule()

    @Test fun partialPendingAndBlockedStayDistinctWhenLocalized() {
        rule.setContent { Column {
            Text(localizedSeerrStatus(4))
            Text(localizedSeerrStatus(2))
            Text(localizedSeerrStatus(6, inLibrary = true))
        } }
        rule.onNodeWithText("Delvis i biblioteket").assertExists()
        rule.onNodeWithText("Ventar på godkjenning").assertExists()
        rule.onNodeWithText("Blokkert i Seerr").assertExists()
        rule.onNodeWithText("I biblioteket ditt").assertDoesNotExist()
    }
}
