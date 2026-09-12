package app.reelstack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import app.reelstack.data.model.Season
import app.reelstack.ui.theme.LocalPersonalization
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Snow in December and embers in October, drifting over the home feature.
 *
 * This is the one place in Spole that exists purely because it is nice, so it is also the one place
 * that has to be easy to switch off and impossible to get in the way. It draws nothing but soft
 * dots at low opacity, sits above the artwork and below every control, carries no semantics for a
 * screen reader, and stops entirely when the mood is not seasonal, when the ornament is switched
 * off, or when the system has animations turned down — a device with `ANIMATOR_DURATION_SCALE` at
 * zero is telling us that motion is unwelcome, and that applies to decoration first.
 *
 * The particles are generated once from a fixed seed, so the drift is the same every time the
 * screen opens rather than reshuffling under the reader.
 */
@Composable
internal fun SeasonalOrnament(modifier: Modifier = Modifier) {
    val personalization = LocalPersonalization.current
    val season = Season.of(personalization)
    if (season == Season.NONE || !personalization.seasonalOrnament) return
    if (!app.reelstack.ui.theme.LocalMotionEnabled.current) return

    val flakes = remember(season) {
        val random = Random(season.ordinal * 7919)
        List(if (season == Season.CHRISTMAS) 34 else 22) {
            Flake(
                x = random.nextFloat(),
                start = random.nextFloat(),
                radius = 1.1f + random.nextFloat() * (if (season == Season.CHRISTMAS) 2.0f else 1.4f),
                speed = 0.018f + random.nextFloat() * 0.030f,
                sway = 0.004f + random.nextFloat() * 0.016f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
            )
        }
    }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(season) {
        var previous = 0L
        while (true) {
            withFrameMillis { frame ->
                if (previous != 0L) time += (frame - previous) / 1000f
                previous = frame
            }
        }
    }

    // Snow falls; embers rise. Both are the same drift with the sign flipped, which is why one
    // component covers both seasons instead of two that would drift apart over time.
    val rising = season == Season.HALLOWEEN
    val tint = if (rising) Color(0xFFFF9A3D) else Color(0xFFE8F2FF)
    Canvas(modifier.clearAndSetSemantics { }) {
        flakes.forEach { flake ->
            val travelled = (flake.start + time * flake.speed) % 1f
            val y = if (rising) (1f - travelled) * size.height else travelled * size.height
            val drift = sin(time * 0.8f + flake.phase) * flake.sway * size.width
            // Fade in at the edge it enters from and out at the one it leaves by, so nothing ever
            // pops into existence in the middle of the picture.
            val fade = (1f - kotlin.math.abs(travelled - 0.5f) * 2f).coerceIn(0f, 1f)
            drawCircle(
                color = tint,
                radius = flake.radius * density,
                center = Offset(flake.x * size.width + drift, y),
                alpha = fade * if (rising) 0.55f else 0.42f,
            )
        }
    }
}

private data class Flake(
    val x: Float,
    val start: Float,
    val radius: Float,
    val speed: Float,
    val sway: Float,
    val phase: Float,
)
