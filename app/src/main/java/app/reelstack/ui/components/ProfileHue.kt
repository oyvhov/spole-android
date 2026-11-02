package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.Oklch
import app.reelstack.ui.theme.SurfaceRaised
import java.util.Locale

/**
 * The colour of a letter avatar, worked out from the name.
 *
 * A profile stands for a person, and a column of identical grey circles reads as a list that has not
 * finished loading. So the circle takes one of eight hues, picked from the name: the same person gets
 * the same colour on every launch and every screen, with nothing stored anywhere.
 *
 * The eight hues are 45 degrees apart and the first is the accent's, so a household with one profile
 * sees the app's own colour. All eight have the same OKLCH lightness and chroma, which is what keeps
 * any one from being louder than the others, and the initial on them is white. They say nothing about
 * state; they only tell people apart. At this lightness and chroma the first hue is a muted, dark
 * relative of the accent, not the accent itself, so the rule that the accent is for actions holds.
 *
 * The chroma is the highest that fits the sRGB cube at this lightness for *every* hue, so no hue
 * has to be clipped and given less than the others.
 */
internal object ProfileHue {
    const val Count = 8
    private const val StepDegrees = 45.0

    /** Dark enough for a white initial to reach 5:1, light enough to read as a colour on the matte ground. */
    const val Lightness = 0.52
    const val Chroma = 0.085

    /**
     * Which of the eight, 0 to 7, for this name. FNV-1a over the characters, then a final mix so the
     * low bits that pick the hue depend on all of them. `String.hashCode()` would also be stable, but
     * an update that changed the arithmetic would recolour every profile, so
     * `ProfileHueTest` pins the answers.
     */
    fun index(name: String): Int {
        var hash = -0x7ee3623b // 2166136261, the FNV offset basis, as an Int
        for (unit in name.trim().lowercase(Locale.ROOT)) hash = (hash xor unit.code) * 16777619
        hash = hash xor (hash ushr 16)
        hash *= -0x7a143595 // 0x85EBCA6B
        hash = hash xor (hash ushr 13)
        hash *= -0x3d4d51cb // 0xC2B2AE35
        hash = hash xor (hash ushr 16)
        return Math.floorMod(hash, Count)
    }

    /** The accent's hue, turned [index] steps of 45 degrees. */
    fun hue(accentArgb: Long, index: Int): Double = (Oklch.of(accentArgb).hue + index * StepDegrees) % 360

    fun color(accentArgb: Long, index: Int): Long = Oklch.toArgb(Lightness, Chroma, hue(accentArgb, index))

    fun color(accentArgb: Long, name: String): Long = color(accentArgb, index(name))
}

/** The initial on a [profileBackground]. White, because the hues were chosen for it. */
val ProfileLetter = Color.White

/**
 * The fill behind a profile's initial. Without a name there is no person to colour, and the
 * placeholder icon stays on the raised surface it always had.
 */
@Composable
fun profileBackground(name: String?): Color {
    if (name.isNullOrBlank()) return SurfaceRaised
    val accent = LocalPersonalization.current.accent.argb
    return remember(name, accent) { Color(ProfileHue.color(accent, name)) }
}
