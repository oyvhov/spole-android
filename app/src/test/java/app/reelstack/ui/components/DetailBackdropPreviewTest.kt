package app.reelstack.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The first frame of a title page already has the list's picture on it, before any backdrop has loaded. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DetailBackdropPreviewTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val opening = ContentDetails("jellyfin-coco", "Coco", "", "", artworkRes = 0, loading = true,
        artworkUrl = "https://media.example/Items/coco/Images/Primary?maxWidth=480", source = ServiceKind.JELLYFIN, mediaType = "Movie")

    @Test fun theCardsPictureIsThereWhileTheBackdropIsStillUnknown() {
        rule.setContent { ReelstackTheme { TvCinematicDetails(opening, rememberScrollState(), heading = {}) {} } }
        rule.waitForIdle()
        rule.onNodeWithTag("detail-backdrop-preview").assertExists()
        rule.onNodeWithTag("tv-detail-backdrop").assertExists()
    }

    @Test fun noPictureAtAllLeavesNothingToStandUnderIt() {
        rule.setContent { ReelstackTheme { TvCinematicDetails(opening.copy(artworkUrl = null), rememberScrollState(), heading = {}) {} } }
        rule.waitForIdle()
        rule.onNodeWithTag("detail-backdrop-preview").assertDoesNotExist()
    }
}
