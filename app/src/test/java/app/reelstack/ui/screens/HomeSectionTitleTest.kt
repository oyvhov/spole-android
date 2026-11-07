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
        rule.onNodeWithText("Emby", substring = true, useUnmergedTree = true).assertExists()
    }

    /**
     * Two «Sjå vidare» headings told apart only by a small grey server name at the far right read as the same
     * row twice (the Shield, 2026-11-07). The server belongs in the heading itself.
     */
    @Test fun theServerIsPartOfTheHeadingNotALabelAtTheFarEnd() {
        rule.setContent { ReelstackTheme { MediaSectionTitle("Sjå vidare", ServiceKind.EMBY, showSource = true) } }
        rule.onNodeWithText("Sjå vidare · Emby", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Emby", useUnmergedTree = true).assertCountEquals(0)
    }

    /** «Sjå vidare · Jellyfin» in the page's large title size outshouted the cards under it (the Shield, 2026-11-07). */
    @Test fun aShelfHeadingIsOneQuietLine() {
        rule.setContent { ReelstackTheme { MediaSectionTitle("Sjå vidare", ServiceKind.EMBY, showSource = true) } }
        val height = rule.onNodeWithText("Sjå vidare · Emby", useUnmergedTree = true).getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }
        org.junit.Assert.assertTrue("the heading is $height dp tall", height <= 21f)
    }

    /**
     * After beta38 made the shelf headings quiet, «Anbefalinger», «Nylig tilgjengelig» and «Kommer snart» were still
     * in the large title size, the same page with two heading sizes (Loftet, 2026-11-07).
     */
    @Test fun theCalendarHeadingIsAsQuietAsTheShelves() {
        rule.setContent { ReelstackTheme { UpcomingSectionTitle(onCalendarClick = {}) } }
        val title = rule.activity.getString(app.reelstack.R.string.home_upcoming)
        val height = rule.onNodeWithText(title, useUnmergedTree = true).getUnclippedBoundsInRoot().let { (it.bottom - it.top).value }
        org.junit.Assert.assertTrue("the heading is $height dp tall", height <= 21f)
    }
}
