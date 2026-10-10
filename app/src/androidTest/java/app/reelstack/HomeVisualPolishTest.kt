package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.components.CompactSessionCard
import app.reelstack.ui.screens.ResumeRail
import app.reelstack.ui.theme.LocalTabletCanvas
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HomeVisualPolishTest {
    @get:Rule val rule = createComposeRule()
    private val session = PlaybackSession("Fixture", "Living room TV", "Ein lang serietittel med fleire ord",
        "Episode", .4f, 35, false, "1080p", false, sessionId = "first", source = ServiceKind.EMBY,
        season = 6, episode = 12)

    @Test fun coverAndCaptionStaySeparateAndPendingLocksBothSessionControls() {
        var pending by mutableStateOf<String?>(null)
        var sent = 0
        var opened = 0
        val other = session.copy(sessionId = "second")
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(760.dp, 700.dp))) {
                ReelstackTheme { Row {
                    listOf(session, other).forEach { current ->
                        CompactSessionCard(current, pending == current.key, pending != null,
                            { opened++ }, { sent++; pending = current.key }, Modifier.width(360.dp))
                    }
                } }
            }
        }
        val art = rule.onNodeWithTag("compact-session-art-${session.key}", true).fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithTag("compact-session-title-${session.key}", true).fetchSemanticsNode().boundsInRoot
        assertTrue(art.right < title.left)
        rule.onNodeWithTag("compact-session-${session.key}").performClick()
        assertEquals(1, opened)
        rule.onNodeWithTag("compact-session-toggle-${session.key}").performClick().assertIsNotEnabled()
        rule.onNodeWithTag("compact-session-toggle-${other.key}").assertIsNotEnabled().performClick()
        assertEquals(1, sent)
    }

    @Test fun doubleTextStacksTheCoverWithoutCoveringThePauseAction() {
        var toggled = false
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 900.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    ReelstackTheme {
                        CompactSessionCard(session, false, false, {}, { toggled = true }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        val art = rule.onNodeWithTag("compact-session-art-${session.key}", true).fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithTag("compact-session-title-${session.key}", true).fetchSemanticsNode().boundsInRoot
        val action = rule.onNodeWithTag("compact-session-toggle-${session.key}").fetchSemanticsNode().boundsInRoot
        assertTrue(title.top >= art.bottom)
        assertTrue(action.top > title.bottom)
        rule.onNodeWithTag("compact-session-toggle-${session.key}").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }

    @Test fun compactLibraryShelfKeepsTheSameTitleAndRemoteAction() {
        var opened = ""
        lateinit var inputMode: androidx.compose.ui.input.InputModeManager
        val film = LibraryMedia("film", "Ein film med ein lang tittel", "2026", .4f,
            R.drawable.media_placeholder, ServiceKind.JELLYFIN, mediaType = "Movie")
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp, 540.dp))) {
                val config = Configuration(LocalConfiguration.current).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or Configuration.UI_MODE_TYPE_TELEVISION
                }
                inputMode = LocalInputModeManager.current
                CompositionLocalProvider(LocalConfiguration provides config, LocalTabletCanvas provides true) {
                    ReelstackTheme { ResumeRail(listOf(film), { opened = it }, compact = true) }
                }
            }
        }
        rule.onNodeWithTag("compact-resume-film", true).assertExists()
        val bounds = rule.onNodeWithTag("resume-card-film").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height < 120 * rule.density.density)
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithTag("resume-card-film").performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals("film", opened)
    }
}
