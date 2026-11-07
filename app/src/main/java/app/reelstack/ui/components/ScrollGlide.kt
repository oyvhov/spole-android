package app.reelstack.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Remote scrolling on a no-bounce spring that keeps its speed from one press to the next, as Stova's rows do.
 *
 * Every press used to start a new tween from rest: a press while the page was still moving stopped it dead and
 * eased it out again, so stepping down Home on the Shield went in jerks (2026-11-07). A glide that is
 * interrupted hands its speed to the next one, so holding Down travels smoothly; it never overshoots, because
 * the carried speed is kept under the spring's own limit. One instance belongs to one scrolling list.
 */
internal class ScrollGlide {
    private var velocity = 0f
    private var at = 0L

    suspend fun by(state: ScrollableState, distance: Float, motion: Boolean) {
        if (abs(distance) < .5f) return
        if (!motion) {
            state.scrollBy(distance)
            return
        }
        val fresh = System.nanoTime() - at < CARRY_WINDOW_NANOS
        val carried = if (fresh && sign(velocity) == sign(distance)) velocity else 0f
        // A critically damped spring overshoots once its start speed passes omega times the distance.
        val start = carried.coerceIn(-MAX_CARRY * OMEGA * abs(distance), MAX_CARRY * OMEGA * abs(distance))
        state.scroll {
            var last = 0f
            animate(0f, distance, initialVelocity = start,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = TRAVEL_STIFFNESS)) { value, speed ->
                scrollBy(value - last)
                last = value
                velocity = speed
                at = System.nanoTime()
            }
            velocity = 0f
        }
    }

    private companion object {
        /** Stova's row travel: within 1 % of the distance after about 380 ms. */
        const val TRAVEL_STIFFNESS = 280f
        val OMEGA = sqrt(TRAVEL_STIFFNESS)
        const val MAX_CARRY = .8f
        const val CARRY_WINDOW_NANOS = 120_000_000L
    }
}
