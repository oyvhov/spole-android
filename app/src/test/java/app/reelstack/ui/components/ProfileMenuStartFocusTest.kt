package app.reelstack.ui.components

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.UserProfile
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * On a television the first press must act on the menu, not on the page behind it.
 *
 * The panel is tested apart from its popup window: a window of its own never takes focus under
 * Robolectric, which would make every assertion about focus inside it fail for the wrong reason.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class ProfileMenuStartFocusTest {
    @get:Rule val rule = createComposeRule()

    private val main = UserProfile(id = "", name = "Øyvind", isKid = false)
    private val kid = UserProfile(id = "kid-1", name = "Mia", isKid = true)

    private fun show(active: String) = rule.setContent {
        // The remote opened the menu, so the screen is in keyboard mode, where clickable rows take focus.
        val mode = androidx.compose.ui.platform.LocalInputModeManager.current
        androidx.compose.runtime.SideEffect { mode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
        ReelstackTheme {
            ProfileMenuPanel(profiles = listOf(main, kid), activeProfileId = active, isKidMode = false,
                mainAccountName = null, mainAccountAvatarUrl = null, onSelectProfile = {}, onPicked = { _, _ -> },
                onOpenSettings = {}, onDeleteProfile = {})
        }
    }

    @Test fun theActiveProfileHasFocusTheMomentTheMenuIsOpen() {
        show(active = "kid-1")
        rule.waitForIdle()
        rule.onNodeWithTag("profile-row-kid-1").assertIsFocused()
    }

    @Test fun theMainProfileHasFocusWhenNoOtherIsActive() {
        show(active = "")
        rule.waitForIdle()
        rule.onNodeWithTag("profile-row-main").assertIsFocused()
    }
}
