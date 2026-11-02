package app.reelstack.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * The few curves the app moves with, in one place.
 *
 * Four places with four lengths read as four apps. Everything that opens, changes or lands takes
 * its length from here and never writes a `tween` of its own; the exceptions are the player's
 * controls, which are their own world, and the slow things that are not motion at all (a season's
 * snow, a backdrop that drifts, a skeleton's shimmer).
 *
 * With motion reduced — the system scale, «Redusert rørsle» or the lightweight television —
 * every curve is the short linear fade in [ReducedMs]. A fade, never a cut: a page that snaps
 * from one state to the next is harder to follow than one that takes a tenth of a second.
 */
object SpoleMotion {
    /** Focus, a press, a small toggle, the ring. */
    const val FocusMs = 140
    /** Content that changes in place, a tab's page, a reveal. */
    const val ChangeMs = 280
    /** A sheet or panel from below; the same numbers `StableSheetDialog` has always used. */
    const val SheetEnterMs = 320
    const val SheetExitMs = 200
    /** Something that lands: the profile picture in its circle, a row coming in. */
    const val LandMs = 450
    /** Every curve, with motion reduced. */
    const val ReducedMs = 100

    /**
     * A spring that settles in about [LandMs] without overshooting (damping 0.86): the period of
     * an undamped spring is 2π/√k, so 0.45 s means k ≈ 195. A spring instead of a duration because
     * a redirect mid-flight continues the motion instead of restarting it.
     */
    const val LandDamping = 0.86f
    const val LandStiffness = 195f

    /** Rows coming in after a switch: 80 ms apart, a short fade, a small lift from 96 %. */
    const val RowStaggerMs = 80
    const val RowFadeMs = 220
    val RowOffset = 14.dp
    const val RowScale = 0.96f
}

/** A timed curve of [ms], or the reduced fade when [motion] is off. For code outside composition. */
fun <T> motionSpec(motion: Boolean, ms: Int, easing: Easing = FastOutSlowInEasing, delayMs: Int = 0): FiniteAnimationSpec<T> =
    if (motion) tween(ms, delayMs, easing) else tween(SpoleMotion.ReducedMs, 0, LinearEasing)

/** [motionSpec] with the theme's answer to whether motion is wanted. */
@Composable
fun <T> motionTween(ms: Int, easing: Easing = FastOutSlowInEasing, delayMs: Int = 0): FiniteAnimationSpec<T> =
    motionSpec(LocalMotionEnabled.current, ms, easing, delayMs)

/** The landing spring, or the reduced fade when [motion] is off. */
fun <T> landSpec(motion: Boolean): FiniteAnimationSpec<T> =
    if (motion) spring(dampingRatio = SpoleMotion.LandDamping, stiffness = SpoleMotion.LandStiffness)
    else tween(SpoleMotion.ReducedMs, 0, LinearEasing)

@Composable
fun <T> landSpring(): FiniteAnimationSpec<T> = landSpec(LocalMotionEnabled.current)
