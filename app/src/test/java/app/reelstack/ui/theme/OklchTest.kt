package app.reelstack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OKLCH is how the app measures "do these two colours look alike": equal steps in hue are equal
 * steps to the eye, which HSL's are not (HSL calls lime and amber 38 degrees apart, OKLCH 44).
 * The reference values are the ones CSS Color 4 publishes for the sRGB primaries.
 */
class OklchTest {
    @Test fun srgbPrimariesLandWhereCssColor4PutsThem() {
        val red = Oklch.of(0xFFFF0000)
        assertEquals(.6280, red.lightness, .002)
        assertEquals(.2577, red.chroma, .002)
        assertEquals(29.23, red.hue, .3)
        val green = Oklch.of(0xFF00FF00)
        assertEquals(.8664, green.lightness, .002)
        assertEquals(.2948, green.chroma, .002)
        assertEquals(142.5, green.hue, .3)
        val blue = Oklch.of(0xFF0000FF)
        assertEquals(.4520, blue.lightness, .002)
        assertEquals(.3132, blue.chroma, .002)
        assertEquals(264.05, blue.hue, .3)
    }

    @Test fun whiteAndBlackHaveNoChroma() {
        assertEquals(1.0, Oklch.of(0xFFFFFFFF).lightness, .002)
        assertEquals(0.0, Oklch.of(0xFFFFFFFF).chroma, .002)
        assertEquals(0.0, Oklch.of(0xFF000000).lightness, .002)
    }

    @Test fun aColourSurvivesTheRoundTripToWithinOneStepPerChannel() {
        for (argb in listOf(0xFFD5F478, 0xFF96D5FF, 0xFFF6806C, 0xFFFFC66D, 0xFF2E332E)) {
            val lch = Oklch.of(argb)
            val back = Oklch.toArgb(lch.lightness, lch.chroma, lch.hue)
            assertEquals("alpha of ${argb.toString(16)}", 0xFF, (back shr 24) and 255)
            for (shift in listOf(16, 8, 0)) {
                assertEquals("channel $shift of ${argb.toString(16)}",
                    ((argb shr shift) and 255).toDouble(), ((back shr shift) and 255).toDouble(), 1.0)
            }
        }
    }

    @Test fun hueDistanceGoesTheShortWayRoundTheCircle() {
        assertEquals(20.0, Oklch.hueDistance(350.0, 10.0), 1e-9)
        assertEquals(20.0, Oklch.hueDistance(10.0, 350.0), 1e-9)
        assertEquals(180.0, Oklch.hueDistance(0.0, 180.0), 1e-9)
        assertEquals(0.0, Oklch.hueDistance(77.0, 77.0), 1e-9)
    }

    @Test fun aColourOutsideTheGamutKeepsItsLightnessAndHueAndLosesChroma() {
        // Nothing at this lightness has a chroma of 0.4.
        val fitted = Oklch.of(Oklch.toArgb(.7, .4, 140.0))
        assertEquals(.7, fitted.lightness, .005)
        assertEquals(140.0, fitted.hue, 1.5)
        assertTrue("chroma ${fitted.chroma}", fitted.chroma < .4)
    }
}
