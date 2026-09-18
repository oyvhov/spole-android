package app.reelstack.ui.kids

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wallpaper, not furniture.
 *
 * The space layer lives only in the decorative surface: a quiet sky behind the kid's home screen,
 * never on the artwork and never on a control. Stars are laid out once from a fixed seed so they
 * do not jump on every recomposition, and the whole thing is a single [Canvas] pass — the
 * animation is one float, not one animation per star.
 */
@Composable
fun SpaceBackdrop(
    modifier: Modifier = Modifier,
    accent: Color,
    starCount: Int = 90,
) {
    val stars = remember(starCount) {
        val random = Random(seed = 20260918)
        List(starCount) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 1.6f,
                phase = random.nextFloat() * 6.283f,
                // Most stars sit still and dim; only a few carry any twinkle at all.
                twinkle = if (random.nextFloat() < 0.35f) 0.25f + random.nextFloat() * 0.35f else 0f,
            )
        }
    }

    val phase = rememberStarPhase()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            // Decoration has nothing to announce. A screen reader should reach the content.
            .semantics { }
            .clearAndSetSemantics { },
    ) {
        val width = size.width
        val height = size.height
        stars.forEach { star ->
            val alpha = if (star.twinkle == 0f) {
                0.22f
            } else {
                0.22f + star.twinkle * abs(sin(phase.value + star.phase))
            }
            drawCircle(
                color = if (star.radius > 1.7f) accent else Color.White,
                radius = star.radius,
                center = Offset(star.x * width, star.y * height),
                alpha = alpha.coerceIn(0f, 0.6f),
            )
        }
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val twinkle: Float,
)

/**
 * One slow phase for the whole sky.
 *
 * With system animation switched off, [rememberInfiniteTransition] never advances, and the stars
 * stand still — which is exactly what the accessibility setting asks for.
 */
@Composable
private fun rememberStarPhase(): State<Float> {
    val context = LocalContext.current
    val animationsOn = remember(context) {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
    if (!animationsOn) return remember { mutableStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "kids-sky")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "kids-sky-phase",
    )
}
