package app.reelstack.ui.kids

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Focus for three metres away.
 *
 * The adult app draws a 3 dp outline and never scales. From the sofa that is easy to lose, so kids
 * mode deliberately breaks the rule: 6 dp and a 1,05x lift, as BM-13 requires.
 *
 * The lift belongs to the artwork alone. Scaling a whole card grows the poster down over its own
 * title, which reads as clipped text.
 */
internal fun Modifier.kidsFocusLift(focused: Boolean, scale: Float, accent: Color): Modifier = this
    .graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    .then(if (focused) Modifier.border(6.dp, accent, RoundedCornerShape(24.dp)) else Modifier)

/** Animated so focus glides between tiles instead of snapping a poster larger. */
@Composable
internal fun rememberKidsFocusScale(focused: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.05f else 1f,
        label = "kids-focus-scale",
    )
    return scale
}
