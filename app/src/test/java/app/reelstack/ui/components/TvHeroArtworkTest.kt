package app.reelstack.ui.components

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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

    /** The television's menu line is drawn over the hero: the picture starts at the top edge, the words below the line. */
    @Test fun theMenuLineSitsOnThePictureAndTheWordsStayBelowIt() {
        val film = LibraryMedia(id = "jf-1", title = "Film", subtitle = "2026", artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN, remoteId = "1", mediaType = "Movie")
        val line = 88.dp
        rule.setContent { ReelstackTheme { TabletLibraryFeature(film, onOpen = {}, candidates = listOf(film), topInset = line) } }
        rule.waitForIdle()
        val hero = rule.onNodeWithTag("tablet-library-feature").fetchSemanticsNode().boundsInRoot
        val picture = rule.onNodeWithTag("feature-artwork", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val caption = rule.onNodeWithTag("feature-caption", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val linePx = with(androidx.compose.ui.unit.Density(rule.activity.resources.displayMetrics.density)) { line.toPx() }
        assertEquals("the picture reaches the top edge", 0f, picture.top, 1f)
        assertEquals("the hero starts at the top edge", 0f, hero.top, 1f)
        assertTrue("the words start ${caption.top} px down, below the ${linePx} px line", caption.top >= linePx)
    }
}
