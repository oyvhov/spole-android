package app.reelstack.ui.components

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.VisualTheme
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.drawnContent
import app.reelstack.ui.theme.ReelstackTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The block shown where a picture is still on its way, or never came, is the chosen mood's own
 * surface. `media_placeholder.xml` is fixed to FOREST's #191C19, so MIDNIGHT, PLUM and CINEMA used
 * to show greenish blocks on a blue, purple or black ground.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class MediaPlaceholderTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun choose(mood: VisualTheme) {
        AppPreferencesRepository(ApplicationProvider.getApplicationContext<Context>())
            .personalization = Personalization(visualTheme = mood)
    }

    /** A card with no picture at all, as every demo title and every title the server has no art for is. */
    private fun pixelOfEmptyCard(): Int {
        rule.setContent {
            ReelstackTheme {
                MediaArtwork(null, null, Modifier.size(100.dp, 150.dp).testTag("card"), fallbackRes = R.drawable.media_placeholder)
            }
        }
        rule.waitForIdle()
        val bounds = rule.onNodeWithTag("card", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val density = rule.density.density
        val image = rule.runOnIdle { drawnContent(rule.activity) }
        return image.getPixel(((bounds.left.value + 50) * density).toInt(), ((bounds.top.value + 75) * density).toInt())
    }

    /** The painter that fills the card while a picture loads and when it fails, drawn on its own. */
    private fun pixelOfLoadingPainter(): Int {
        var painter: Painter? = null
        rule.setContent { ReelstackTheme { painter = placeholderPainter(R.drawable.media_placeholder) } }
        rule.waitForIdle()
        val bitmap = ImageBitmap(100, 150)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(100f, 150f)) {
            with(painter!!) { draw(size) }
        }
        val pixels = IntArray(100 * 150)
        bitmap.readPixels(pixels, 0, 0, 100, 150)
        return pixels[75 * 100 + 50]
    }

    @Test fun midnightShowsMidnightsSurfaceWhereThereIsNoPicture() {
        choose(VisualTheme.MIDNIGHT)
        assertEquals(Color(VisualTheme.MIDNIGHT.surface).toArgb(), pixelOfEmptyCard())
    }

    @Test fun plumShowsPlumsSurfaceWhereThereIsNoPicture() {
        choose(VisualTheme.PLUM)
        assertEquals(Color(VisualTheme.PLUM.surface).toArgb(), pixelOfEmptyCard())
    }

    @Test fun cinemaShowsCinemasSurfaceWhereThereIsNoPicture() {
        choose(VisualTheme.CINEMA)
        assertEquals(Color(VisualTheme.CINEMA.surface).toArgb(), pixelOfEmptyCard())
    }

    @Test fun forestStaysExactlyTheOldBlock() {
        choose(VisualTheme.FOREST)
        assertEquals(Color(0xFF191C19).toArgb(), pixelOfEmptyCard())
    }

    @Test fun midnightShowsMidnightsSurfaceWhileAPictureLoadsAndWhenItFails() {
        choose(VisualTheme.MIDNIGHT)
        assertEquals(Color(VisualTheme.MIDNIGHT.surface).toArgb(), pixelOfLoadingPainter())
    }

    @Test fun otherDrawablesAreLeftAlone() {
        // The painter for anything but the placeholder is the resource itself, so a demo still keeps its picture.
        choose(VisualTheme.MIDNIGHT)
        var painter: Painter? = null
        var plain: Painter? = null
        rule.setContent {
            ReelstackTheme {
                painter = placeholderPainter(R.drawable.session_still)
                plain = androidx.compose.ui.res.painterResource(R.drawable.session_still)
            }
        }
        rule.waitForIdle()
        assertEquals(plain!!.intrinsicSize, painter!!.intrinsicSize)
    }

    /** Same size as the resource it stands in for, so the crossfade sizing the artwork code relies on does not move. */
    @Test fun theThemedBlockIsTheSizeOfTheDrawableItReplaces() {
        val xml = File("src/main/res/drawable/media_placeholder.xml").readText()
        fun attribute(name: String) = Regex("""android:$name="([^"]+)"""").find(xml)!!.groupValues[1]
        val vector = placeholderVector(Color.Red)
        assertEquals(attribute("width"), "${vector.defaultWidth.value.toInt()}dp")
        assertEquals(attribute("height"), "${vector.defaultHeight.value.toInt()}dp")
        assertEquals(attribute("viewportWidth").toFloat(), vector.viewportWidth, 0f)
        assertEquals(attribute("viewportHeight").toFloat(), vector.viewportHeight, 0f)
    }
}
