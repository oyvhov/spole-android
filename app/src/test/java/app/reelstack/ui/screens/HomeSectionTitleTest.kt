package app.reelstack.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class HomeSectionTitleTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun aHomeHeadingWithoutAnotherServerSaysOnlyItsName() {
        rule.setContent { ReelstackTheme { MediaSectionTitle("Sjå vidare", ServiceKind.JELLYFIN, showSource = false) } }
        rule.onNodeWithText("Sjå vidare", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Jellyfin", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun twoServersWithTheSameRowAreToldApartByName() {
        rule.setContent { ReelstackTheme { MediaSectionTitle("Sjå vidare", ServiceKind.EMBY, showSource = true) } }
        rule.onNodeWithText("Emby", useUnmergedTree = true).assertExists()
    }
}
