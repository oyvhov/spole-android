package app.reelstack.ui

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A television shows no floating name beside a focused icon. «Home» stuck out over the hero from
 * the side menu; the same kind of label sat over the video in the player and over the library.
 * The focus ring is the answer on a remote; TalkBack still reads each name.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "b+nn-w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvNoFloatingLabelsTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun remote(content: @androidx.compose.runtime.Composable () -> Unit) = rule.setContent {
        val input = LocalInputModeManager.current
        SideEffect { input.requestInputMode(InputMode.Keyboard) }
        ReelstackTheme { content() }
    }

    @Test fun theCollapsedSideMenuShowsNoNameBesideTheFocusedIcon() {
        remote { ReelstackNavigationRail(AppTab.HOME, {}, expanded = false) }
        rule.onNodeWithTag("wide-tab-HOME").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertTrue(rule.onAllNodesWithTag("navigation-focus-label", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        rule.onAllNodesWithText("Heim", useUnmergedTree = true).fetchSemanticsNodes().forEach { node ->
            assertTrue("A visible «Heim» outside the rail: ${node.boundsInRoot}", node.boundsInRoot.left < 80f * node.layoutInfo.density.density)
        }
    }

    @Test fun thePlayersOptionsButtonShowsNoTooltipOverTheVideo() {
        remote { app.reelstack.player.TvPlayerAction(app.reelstack.ui.components.SpoleIcons.Tune, "Avspelingsval", "player-options") {} }
        rule.onNodeWithTag("player-options").performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertTrue("The name is a description, not a floating label",
            rule.onAllNodesWithText("Avspelingsval", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag("player-options").assertContentDescriptionEquals("Avspelingsval")
    }

    @Test fun theLibraryCustomiseButtonShowsNoTooltip() {
        remote { app.reelstack.ui.screens.LibraryHub(ReelstackUiState(), {}, {}, null, {}) }
        rule.onNodeWithTag("hub-customize").performScrollTo().performSemanticsAction(SemanticsActions.RequestFocus).assertIsFocused()
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertTrue(rule.onAllNodesWithText("Tilpass biblioteksida", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }
}
