package app.reelstack.ui.layout

import app.reelstack.data.model.AccentPalette
import app.reelstack.data.model.VisualTheme
import app.reelstack.ui.theme.Oklch
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentContrastTest {
    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 255).toDouble() / 255
            return if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
        }
        return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
    }
    private fun contrast(light: Long, dark: Long) = (luminance(light) + .05) / (luminance(dark) + .05)
    @Test fun allAccentPalettesKeepTextAndSelectedControlsReadable() {
        AccentPalette.entries.forEach { palette ->
            assertTrue("${palette.name} on ink", contrast(palette.argb, 0xFF101211) >= 7)
            assertTrue("${palette.name} soft on raised surface", contrast(palette.softArgb, 0xFF2E332E) >= 4.5)
            assertTrue("${palette.name} on its container", contrast(palette.argb, palette.containerArgb) >= 4.5)
        }
    }

    /**
     * An error and a selection must not be the same colour. HOLLY's accent sat 10 degrees from the
     * one fixed red and GOLD's 4 degrees from the one fixed amber, so in the Christmas season a
     * failed server and a chosen row looked alike. Every palette now carries its own pair.
     *
     * Hue is measured in OKLCH, where equal steps are equal to the eye. HSL puts LIME 38 degrees
     * from its amber, which is the pair LIME has always had and keeps; OKLCH puts them 44 apart.
     */
    @Test fun warningAndCautionStayAtLeastFortyDegreesFromTheAccent() {
        AccentPalette.entries.forEach { palette ->
            val accent = Oklch.of(palette.argb).hue
            val warning = Oklch.hueDistance(accent, Oklch.of(palette.warningArgb).hue)
            val caution = Oklch.hueDistance(accent, Oklch.of(palette.cautionArgb).hue)
            assertTrue("${palette.name} warning is $warning degrees from the accent", warning >= 40)
            assertTrue("${palette.name} caution is $caution degrees from the accent", caution >= 40)
        }
    }

    @Test fun warningAndCautionDoNotLookLikeEachOther() {
        AccentPalette.entries.forEach { palette ->
            val apart = Oklch.hueDistance(Oklch.of(palette.warningArgb).hue, Oklch.of(palette.cautionArgb).hue)
            assertTrue("${palette.name} warning and caution are $apart degrees apart", apart >= 25)
        }
    }

    /** They carry text («Feil», the reason a server failed), so they hold the accents' own bar on every mood. */
    @Test fun warningAndCautionStayReadableOnEveryMood() {
        AccentPalette.entries.forEach { palette ->
            VisualTheme.entries.forEach { mood ->
                listOf("warning" to palette.warningArgb, "caution" to palette.cautionArgb).forEach { (name, colour) ->
                    assertTrue("${palette.name} $name on ${mood.name} ink", contrast(colour, mood.background) >= 7)
                    assertTrue("${palette.name} $name on ${mood.name} raised", contrast(colour, mood.raised) >= 4.5)
                }
            }
        }
    }

    @Test fun limeKeepsTheErrorAndCautionColoursItAlwaysHad() {
        assertEquals(0xFFFF7A7D, AccentPalette.LIME.warningArgb)
        assertEquals(0xFFFFC66D, AccentPalette.LIME.cautionArgb)
    }
}
