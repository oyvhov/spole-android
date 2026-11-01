package app.reelstack.ui.components

import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import coil3.compose.preferEndFirstIntrinsicSize
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Seen on the Shield: «Kongen befaler» lost the first letters of its title and «Skal vi danse» the
 * bottom of its logo, though both thumbnails are exactly 16:9. The crossfade from the 2:3 placeholder
 * left them drawn 1.26 times too large.
 */
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class ArtworkRequestTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun aThumbnailKeepsItsOwnSizeAfterFadingInOverThePortraitPlaceholder() {
        val resume = artworkRequest(context, "http://jf/Items/s/Images/Thumb?maxWidth=1080&quality=85&tag=t",
            R.drawable.media_placeholder, source = null, fadeDuration = 260, trimTransparent = false, requestSize = null)
        assertTrue("a card that gives no size, like «Sjå vidare»", resume.preferEndFirstIntrinsicSize)
        val library = artworkRequest(context, "http://jf/Items/s/Images/Thumb?maxWidth=1080&quality=85&tag=t",
            R.drawable.media_placeholder, source = null, fadeDuration = 260, trimTransparent = false, requestSize = IntSize(320, 180))
        assertTrue("a card that gives its size", library.preferEndFirstIntrinsicSize)
    }
}
