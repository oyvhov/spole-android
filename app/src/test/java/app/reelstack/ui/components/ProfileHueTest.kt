package app.reelstack.ui.components

import app.reelstack.data.model.AccentPalette
import app.reelstack.ui.theme.Oklch
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A profile is a person, and eight grey circles in a column look like a list that has not finished
 * loading. So each name gets a colour: the same one every time, on every launch, with nothing stored.
 */
class ProfileHueTest {
    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 255).toDouble() / 255
            return if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
        }
        return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
    }
    private fun contrastWithWhite(argb: Long) = 1.05 / (luminance(argb) + .05)

    @Test fun theSameNameGetsTheSameHue() {
        for (name in listOf("Øyvind", "Kari", "Heimetenar", "A", "æøå", "😀")) {
            assertEquals(name, ProfileHue.index(name), ProfileHue.index(name))
        }
    }

    @Test fun caseAndStrayWhitespaceDoNotMakeANewPerson() {
        assertEquals(ProfileHue.index("Øyvind"), ProfileHue.index("  øyvind "))
        assertEquals(ProfileHue.index("Kari"), ProfileHue.index("KARI"))
    }

    /**
     * The numbers below came from a separate implementation of the same arithmetic, not from this
     * one. They are here so that nobody swaps the hash for `String.hashCode()` on a whim: that would
     * be stable too, but it would recolour everyone's profile on the next update.
     */
    @Test fun theHueIsStableAcrossRunsAndVersions() {
        val expected = mapOf("Øyvind" to 2, "Kari" to 2, "Ola" to 2, "Nora" to 3,
            "Lars" to 5, "Ingrid" to 0, "Emil" to 1, "Sofie" to 7)
        for ((name, index) in expected) assertEquals(name, index, ProfileHue.index(name))
    }

    @Test fun aHouseholdOfEightIsToldApart() {
        val household = listOf("Øyvind", "Kari", "Ola", "Nora", "Lars", "Ingrid", "Emil", "Sofie")
        val distinct = household.map(ProfileHue::index).toSet().size
        // A hash cannot promise eight different colours for eight names; it can promise not to lump them.
        assertTrue("only $distinct different hues for eight names", distinct >= 6)
    }

    @Test fun manyNamesSpreadEvenlyOverTheEightHues() {
        val counts = IntArray(ProfileHue.Count)
        repeat(1000) { counts[ProfileHue.index("Profil $it")]++ }
        // 125 each if it were perfect.
        for ((hue, count) in counts.withIndex()) assertTrue("hue $hue got $count of 1000", count in 100..150)
    }

    @Test fun theFirstHueIsTheAccentsAndTheRestAreFortyFiveDegreesApart() {
        for (palette in AccentPalette.entries) {
            val accent = Oklch.of(palette.argb).hue
            assertEquals(palette.name, accent, ProfileHue.hue(palette.argb, 0), 1e-9)
            for (step in 1 until ProfileHue.Count) {
                val apart = Oklch.hueDistance(ProfileHue.hue(palette.argb, step - 1), ProfileHue.hue(palette.argb, step))
                assertEquals("${palette.name} step $step", 45.0, apart, 1e-9)
            }
        }
    }

    @Test fun allEightHaveTheSameLightnessAndChromaSoNoneIsLouder() {
        for (palette in AccentPalette.entries) {
            val colours = (0 until ProfileHue.Count).map { Oklch.of(ProfileHue.color(palette.argb, it)) }
            for ((i, lch) in colours.withIndex()) {
                assertEquals("${palette.name} $i lightness", ProfileHue.Lightness, lch.lightness, .004)
                assertEquals("${palette.name} $i chroma", ProfileHue.Chroma, lch.chroma, .004)
            }
        }
    }

    @Test fun anInitialInWhiteIsReadableOnEveryHue() {
        for (palette in AccentPalette.entries) {
            for (index in 0 until ProfileHue.Count) {
                val contrast = contrastWithWhite(ProfileHue.color(palette.argb, index))
                assertTrue("${palette.name} $index: $contrast", contrast >= 4.5)
            }
        }
    }

    @Test fun theEightColoursAreAllDifferent() {
        val colours = (0 until ProfileHue.Count).map { ProfileHue.color(AccentPalette.LIME.argb, it) }
        assertEquals(ProfileHue.Count, colours.toSet().size)
        assertNotEquals(colours[0], colours[1])
    }
}
