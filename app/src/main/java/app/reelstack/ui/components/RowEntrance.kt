package app.reelstack.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import app.reelstack.ui.theme.LocalMotionEnabled
import app.reelstack.ui.theme.SpoleMotion
import app.reelstack.ui.theme.landSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A presentation belongs to the whole page, not to each lazy item. Rows first composed after
 * their window (for example by scrolling) are already settled. Only the drawing layer moves;
 * the measured shelf and its focus targets retain their positions throughout the entrance.
 * [presentSince] uses monotonic uptime, or null while a cover/profile transition holds the page.
 */
fun Modifier.rowEntrance(index: Int, presentSince: Long?): Modifier = composed {
    val motion = LocalMotionEnabled.current
    // A newly loaded section can change the indexes below it. Existing keyed rows retain their
    // original slot in this presentation rather than disappearing and starting again.
    val stagger = remember(presentSince, motion) {
        if (motion) index.coerceAtLeast(0).toLong() * SpoleMotion.RowStaggerMs else 0L
    }
    val fadeMs = if (motion) SpoleMotion.RowFadeMs else SpoleMotion.ReducedMs
    val elapsed = remember(presentSince, motion) {
        presentSince?.let { (SystemClock.uptimeMillis() - it).coerceAtLeast(0) }
    }
    val settled = elapsed != null && elapsed >= stagger + fadeMs
    val alpha = remember(presentSince, motion) { Animatable(if (settled) 1f else 0f) }
    val landing = remember(presentSince, motion) { Animatable(if (settled || !motion) 1f else 0f) }
    LaunchedEffect(presentSince, motion) {
        if (elapsed == null || settled) return@LaunchedEffect
        delay((stagger - elapsed).coerceAtLeast(0))
        coroutineScope {
            launch { alpha.animateTo(1f, tween(fadeMs, easing = LinearEasing)) }
            if (motion) launch { landing.animateTo(1f, landSpec(motion = true)) }
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        val progress = landing.value.coerceIn(0f, 1f)
        translationY = if (motion) (1f - progress) * SpoleMotion.RowOffset.toPx() else 0f
        val scale = if (motion) SpoleMotion.RowScale + (1f - SpoleMotion.RowScale) * progress else 1f
        scaleX = scale
        scaleY = scale
    }
}
