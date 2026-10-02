package app.reelstack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.ui.theme.ReelstackTheme
import app.reelstack.update.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvUpdateUiTest {
    @get:Rule val rule = createComposeRule()
    private val release = AppRelease("v0.18.0-beta14", "Ei oppdatering med betre TV-navigasjon.", 1, 11_000_000, "a".repeat(64))

    @Before fun requireTelevision() {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION)
    }

    @Test fun notificationTakesFocusFromTouchAndRemoteOpensDownloadDialog() {
        var banner by mutableStateOf(false)
        var open by mutableStateOf(false)
        rule.setContent { ReelstackTheme { Box(Modifier.fillMaxSize()) {
            Button(onClick = { banner = true }, modifier = Modifier.testTag("underlying-page")) { Text("Vis varsel") }
            if (banner) AppUpdateBanner(release.tag, { banner = false; open = true }, { banner = false })
            if (open) AppUpdateDialog(UpdateState(release = release), { open = false }, {}, {}, {}, {})
        } } }
        rule.onNodeWithTag("underlying-page").performClick()
        rule.onNodeWithTag("update-view").assertIsFocused().assertIsDisplayed()
        saveTvReview("update-banner", rule.onNode(isDialog()).captureToImage())
        rule.onNodeWithTag("update-view").performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("update-later").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag("update-view").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("update-download").assertIsFocused().assertIsDisplayed()
        rule.onNodeWithTag("underlying-page").assertIsNotFocused()
    }

    @Test fun laterAndBackDismissNotificationWithoutOpeningDownload() {
        var banner by mutableStateOf(true)
        var dismissals = 0
        rule.setContent { ReelstackTheme {
            if (banner) AppUpdateBanner(release.tag, { error("Must not open") }, { dismissals++; banner = false })
        } }
        rule.onNodeWithTag("update-view").performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag("update-later").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("update-banner").assertDoesNotExist()
        rule.runOnIdle { banner = true }
        rule.onNodeWithTag("update-view").assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
        rule.onNodeWithTag("update-banner").assertDoesNotExist()
        assertEquals(2, dismissals)
    }

    @Test fun downloadCancelAndInstallReceiveFocusWhenActionsChange() {
        var state by mutableStateOf(UpdateState(release = release))
        var installs = 0
        var checks = 0
        rule.setContent { ReelstackTheme {
            AppUpdateDialog(state, {}, { state = state.copy(downloading = false) }, { installs++ },
                { state = state.copy(downloading = true, progress = .3f) }, { checks++ })
        } }
        rule.onNodeWithTag("update-download").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("update-cancel").assertIsFocused().assertIsDisplayed()
        rule.onNodeWithTag("update-check").assertIsNotEnabled()
        rule.onNodeWithTag("update-cancel").performKeyInput { pressKey(Key.Enter) }
        rule.onNodeWithTag("update-download").assertIsFocused()
        rule.runOnIdle { state = state.copy(ready = true) }
        rule.onNodeWithTag("update-install").assertIsFocused().assertIsDisplayed()
        saveTvReview("update-dialog", rule.onNode(isDialog()).captureToImage())
        rule.onNodeWithTag("update-install").performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag("update-check").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, checks)
        rule.onNodeWithTag("update-check").performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag("update-install").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(1, installs)
        rule.onNodeWithTag("update-install").performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("update-close").assertIsFocused().assertIsDisplayed()
    }

    @Test fun checkingKeepsCloseAvailableThenReturnsFocusToCheck() {
        var state by mutableStateOf(UpdateState(checking = true))
        var closed = false
        rule.setContent { ReelstackTheme {
            AppUpdateDialog(state, { closed = true }, {}, {}, {}, {})
        } }
        rule.onNodeWithTag("update-close").assertIsFocused().assertIsDisplayed()
        rule.onNodeWithTag("update-check").assertIsNotEnabled()
        rule.runOnIdle { state = state.copy(checking = false) }
        rule.onNodeWithTag("update-check").assertIsFocused().assertIsDisplayed()
            .performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag("update-close").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(true, closed)
    }
}
