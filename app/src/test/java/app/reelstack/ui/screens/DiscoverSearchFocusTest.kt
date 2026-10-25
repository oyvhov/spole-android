package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DiscoverSearchFocusTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun theFirstLetterKeepsTheFieldAndItsKeyboard() {
        val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "c", userId = "7", sessionCookie = true)
        var query by mutableStateOf("")
        rule.setContent {
            ReelstackTheme {
                DiscoverScreen(DiscoverUiState(connections = listOf(seerr), searchQuery = query), PaddingValues(),
                    onSearch = { query = it }, onDetails = {}, hubActions = DiscoverHubActions({}, {}, {}, {}, {}, {}, {}))
            }
        }
        rule.onNodeWithTag("discover-search").performClick().assertIsFocused()
        rule.onNodeWithTag("discover-search").performTextInput("S")
        rule.waitForIdle()
        rule.onNodeWithTag("discover-search").assertIsFocused().assertTextContains("S")
        // And back again when the field is emptied.
        rule.onNodeWithTag("discover-search").performTextClearance()
        rule.waitForIdle()
        rule.onNodeWithTag("discover-search").assertIsFocused()
    }
}
