package app.reelstack.ui.components

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class TvHeroArtworkTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun thePictureReachesWellBelowTheButtonsAndFadesOut() {
        val film = LibraryMedia(id = "jf-1", title = "Film", subtitle = "2026", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
        rule.setContent { ReelstackTheme { TabletLibraryFeature(film, onOpen = {}, candidates = listOf(film)) } }
        rule.waitForIdle()
        val hero = rule.onNodeWithTag("tablet-library-feature").fetchSemanticsNode().boundsInRoot
        val picture = rule.onNodeWithTag("feature-artwork", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        // Under the first row's heading and into its posters, so the picture ends in a long fade and
        // not in a line across the screen.
        assertTrue("the picture ends ${picture.bottom - hero.bottom} px below a ${hero.height} px hero",
            picture.bottom - hero.bottom >= hero.height * .4f)
    }
}
