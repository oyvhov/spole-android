package app.reelstack.ui.layout

import app.reelstack.data.model.AccentPalette
import kotlin.math.pow
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
}
