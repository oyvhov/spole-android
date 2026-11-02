package app.reelstack.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** An episode is a wide still. In a tall frame it was a thin band between two dark bars. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-television-xhdpi", application = app.reelstack.SheetTestApplication::class)
class LibraryHitCardTest {
    @get:Rule val rule = createComposeRule()

    private fun media(id: String, type: String) = LibraryMedia(id = id, title = "Tittel", subtitle = "2026",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN, remoteId = id, mediaType = type)

    private fun ratio(id: String, type: String): Float {
        rule.setContent { ReelstackTheme { Box(Modifier.width(240.dp)) { LibraryHitCard(media(id, type)) {} } } }
        rule.waitForIdle()
        val box = rule.onNodeWithTag("library-hit-artwork-$id", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return box.width / box.height
    }

    @Test fun anEpisodeHitIsWide() = assertEquals(16f / 9f, ratio("episode", "Episode"), .02f)

    @Test fun aFilmHitStaysTall() = assertEquals(2f / 3f, ratio("film", "Movie"), .02f)
}
