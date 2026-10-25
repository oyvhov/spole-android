package app.reelstack.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TextActionsTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun actionsAreTextWithoutAFilledSurface() {
        var clicked = ""
        rule.setContent { ReelstackTheme { TextActions(listOf(TextAction("Be om", SpoleIcons.Add, "title-request") { clicked = "be" },
            TextAction("Trekk tilbake", SpoleIcons.Close, "title-withdraw") { clicked = "trekk" })) } }
        rule.onNodeWithTag("title-withdraw").performClick()
        assertEquals("trekk", clicked)
        rule.onNodeWithTag("title-request").assertHasClickAction()
    }
}
