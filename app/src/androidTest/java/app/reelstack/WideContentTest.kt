package app.reelstack

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.PlaybackSession
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WideContentTest {
    @get:Rule val rule = createComposeRule()

    @Test fun discoverSearchSharesHeaderButStacksAtLargeFont() {
        var font by mutableFloatStateOf(1f)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(900.dp, 540.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(font)) {
                    ReelstackTheme { DiscoverHeader(
                        heading = { Text("Oppdag", Modifier.testTag("heading")) },
                        account = { Box(Modifier.size(48.dp).testTag("account")) },
                        search = { Box(Modifier.fillMaxWidth().height(56.dp).testTag("search")) },
                    ) }
                }
            }
        }
        rule.onNodeWithTag("discover-header-wide").assertExists()
        val heading = rule.onNodeWithTag("heading").fetchSemanticsNode().boundsInRoot
        val search = rule.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot
        val account = rule.onNodeWithTag("account").fetchSemanticsNode().boundsInRoot
        assertTrue(heading.right < search.left && search.right < account.left)
        rule.runOnIdle { font = 2f }
        rule.onNodeWithTag("discover-header-wide").assertDoesNotExist()
        assertTrue(rule.onNodeWithTag("search").fetchSemanticsNode().boundsInRoot.top >
            rule.onNodeWithTag("heading").fetchSemanticsNode().boundsInRoot.bottom)
    }

    @Test fun sessionDetailsAndPlaybackAreSeparateAndPendingCannotRepeat() {
        val session = PlaybackSession("Test", "TV", "A long series title", "S01 E02 · Episode", .5f,
            "10 min", "Direct", "1080p", false, sessionId = "fixture")
        var pending by mutableStateOf(false)
        var opened = 0
        var toggled = 0
        rule.setContent { ReelstackTheme {
            CompactSessionCard(session, pending, false, { opened++ }, { toggled++ }, Modifier.width(480.dp))
        } }
        rule.onNodeWithTag("compact-session-${session.key}").performClick()
        rule.onNodeWithTag("compact-session-toggle-${session.key}").performClick()
        rule.runOnIdle { assertEquals(1, opened); assertEquals(1, toggled); pending = true }
        rule.onNodeWithTag("compact-session-toggle-${session.key}").assertIsNotEnabled()
    }
}
