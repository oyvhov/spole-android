package app.reelstack.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text over artwork is a solid colour, never white with an alpha.
 *
 * Alpha changes its effect as soon as something lies beneath it. White at 60 % over a dark scrim is
 * a calm secondary line; the same 60 % over a bright poster is a smudge, so the line that was
 * "quiet" becomes unreadable on exactly the posters that are bright. A solid grey is the same grey
 * on every poster, and the scrim underneath is then the only thing the contrast depends on.
 */
class ArtworkTextTest {
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double = value.toDouble().let { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }
        return .2126 * channel(color.red) + .7152 * channel(color.green) + .0722 * channel(color.blue)
    }
    private fun contrast(light: Color, dark: Color) = (luminance(light) + .05) / (luminance(dark) + .05)

    @Test fun theThreeLevelsAreTheSolidGreys() {
        assertEquals(Color(0xFFFFFFFF), ArtworkTextPrimary)
        assertEquals(Color(0xFFCCCCCC), ArtworkTextSecondary)
        assertEquals(Color(0xFF989898), ArtworkTextQuiet)
    }

    @Test fun noLevelIsTranslucent() {
        listOf(ArtworkTextPrimary, ArtworkTextSecondary, ArtworkTextQuiet).forEach { assertEquals(1f, it.alpha, 0f) }
    }

    /** The scrim under a hero fades to near black, but a caption can sit where it has only reached a dark grey. */
    @Test fun everyLevelReadsOnTheLightestDarkSurface() {
        val lightestSurface = Color(0xFF292929)
        assertTrue(contrast(ArtworkTextPrimary, lightestSurface) >= 12)
        assertTrue(contrast(ArtworkTextSecondary, lightestSurface) >= 7)
        assertTrue(contrast(ArtworkTextQuiet, lightestSurface) >= 4.5)
    }

    @Test fun theLevelsStepDownInBrightness() {
        assertTrue(luminance(ArtworkTextPrimary) > luminance(ArtworkTextSecondary))
        assertTrue(luminance(ArtworkTextSecondary) > luminance(ArtworkTextQuiet))
    }

    /**
     * The heroes and cards that draw text over a picture. This reads the source, because the thing
     * to keep out is a spelling, and a screenshot test would only notice it on the one poster it used.
     */
    @Test fun noTextOverArtworkIsWhiteWithAlpha() {
        val files = listOf(
            "src/main/java/app/reelstack/ui/components/TabletLibraryFeature.kt",
            "src/main/java/app/reelstack/ui/components/MobileLibraryFeature.kt",
            "src/main/java/app/reelstack/ui/screens/HomeScreen.kt",
        )
        // `color =` as a Text argument. containerColor, trackColor and tint are not text, and a scrim
        // is built from Color.Black or a Brush, so neither is caught here.
        val translucentText = Regex("""(?<![A-Za-z])color = (androidx\.compose\.ui\.graphics\.)?Color\.White\.copy\(alpha""")
        val offenders = files.flatMap { path ->
            File(path).readLines().withIndex().filter { translucentText.containsMatchIn(it.value) }
                .map { "$path:${it.index + 1}: ${it.value.trim()}" }
        }
        assertTrue("white-with-alpha text over artwork:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
