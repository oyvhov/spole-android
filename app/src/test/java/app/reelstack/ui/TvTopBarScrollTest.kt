package app.reelstack.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The television's menu lies only at the top of the page: it rides up with what scrolls under it, and the
 * remote reaching it brings it back. The owner: «Topp baren skulle berre ligg øverst».
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvTopBarScrollTest {
    @get:Rule val rule = createComposeRule()
    private var lift by mutableIntStateOf(0)

    private fun show() = rule.setContent {
        val mode = androidx.compose.ui.platform.LocalInputModeManager.current
        androidx.compose.runtime.SideEffect { mode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        ReelstackTheme { Box(Modifier.fillMaxSize()) {
            TvTopBar(AppTab.HOME, {}, emptyList(), null, {}, true, false, {}, null, null, {}, true, {},
                lift = { lift })
        } }
    }

    @Test fun theMenuStandsAtTheTopOfAPageAtRest() {
        show()
        rule.onNodeWithTag("tv-top-HOME").assertIsDisplayed()
    }

    @Test fun theMenuRidesUpWithThePageAndComesBackWithIt() {
        show()
        rule.runOnIdle { lift = Int.MAX_VALUE }
        rule.onNodeWithTag("tv-top-HOME").assertIsNotDisplayed()
        rule.onNodeWithTag("tv-top-account").assertIsNotDisplayed()
        rule.runOnIdle { lift = 0 }
        rule.onNodeWithTag("tv-top-HOME").assertIsDisplayed()
    }

    @Test fun theRemoteReachingTheMenuBringsItBack() {
        show()
        rule.runOnIdle { lift = Int.MAX_VALUE }
        rule.onNodeWithTag("tv-top-HOME").performSemanticsAction(SemanticsActions.RequestFocus)
        rule.onNodeWithTag("tv-top-HOME").assertIsDisplayed()
    }
}
