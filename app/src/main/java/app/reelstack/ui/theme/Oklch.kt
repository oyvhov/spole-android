package app.reelstack.ui.theme

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * OKLCH: lightness, chroma and hue in a space where equal steps look equal.
 *
 * The app uses it for two things that both come down to "how different do these look". The accent
 * palettes keep their error and caution colours at least 40 degrees of hue away from the accent,
 * and the profile colours are eight hues 45 degrees apart at one lightness and one chroma. HSL
 * would get both wrong: its hue steps are uneven (it puts lime and amber 38 degrees apart where
 * the eye, and OKLCH, see 44), and its "same lightness" is not.
 *
 * Pure arithmetic with no Android types, so it runs in a plain JVM test. The matrices are Björn
 * Ottosson's published OKLab ones.
 */
internal object Oklch {
    class Lch(val lightness: Double, val chroma: Double, val hue: Double)

    fun of(argb: Long): Lch {
        val r = linear(((argb shr 16) and 255) / 255.0)
        val g = linear(((argb shr 8) and 255) / 255.0)
        val b = linear((argb and 255) / 255.0)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        val hue = Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
        return Lch(lightness, hypot(a, bb), hue)
    }

    /** The shortest way round the hue circle, 0 to 180 degrees. */
    fun hueDistance(first: Double, second: Double): Double {
        val apart = abs(first - second) % 360
        return min(apart, 360 - apart)
    }

    /**
     * Opaque ARGB for the colour. When the sRGB cube has no such colour (most hues cannot reach a
     * high chroma at a given lightness) the chroma is lowered until it fits, so lightness and hue,
     * which is what a row of look-alikes needs to hold equal, are never the thing that gives.
     */
    fun toArgb(lightness: Double, chroma: Double, hue: Double): Long {
        var c = chroma
        while (c > 0) {
            val rgb = linearRgb(lightness, c, hue)
            if (rgb.all { it in -GAMUT_SLACK..1 + GAMUT_SLACK }) break
            c -= 0.002
        }
        val rgb = linearRgb(lightness, c.coerceAtLeast(0.0), hue)
        val (r, g, b) = rgb.map { (encode(it.coerceIn(0.0, 1.0)) * 255).roundToLong() }
        return 0xFF000000L or (r shl 16) or (g shl 8) or b
    }

    private const val GAMUT_SLACK = 1e-4

    private fun linearRgb(lightness: Double, chroma: Double, hue: Double): List<Double> {
        val a = chroma * cos(Math.toRadians(hue))
        val b = chroma * sin(Math.toRadians(hue))
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)
        return listOf(
            4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private fun linear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun encode(c: Double) = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
}
