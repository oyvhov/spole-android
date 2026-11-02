package app.reelstack.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * The account switch from the profile menu, as one movement.
 *
 * The picture you pick flies from its row in the menu to the profile circle in the corner and
 * lands there; the old page's rows are gone at once, and the new profile's rows come in one by
 * one once the picture rests. The switch itself happens early in the flight, while the picture
 * still moves by whole pixels per frame: a switch costs the main thread time, and near the
 * landing the picture moves by fractions of a point, where a stall would show.
 *
 * Owned by the view model, not by a feature: the switch tears the features down and builds new
 * ones, and the flight has to outlive that. The menu lives in its own window (a `Popup`), so the
 * row reports where it sits on the screen, and [rootOnScreen] turns that into the root's
 * coordinates, which is where the flight is drawn.
 *
 * Without a known circle, or with motion reduced, there is no flight: the switch runs at once
 * and the rows take their usual entrance from the session change.
 */
class ProfileSwitchMotion(
    private val scope: CoroutineScope,
    private val clock: () -> Long = SystemClock::uptimeMillis,
) {
    /** What the flying circle shows: the profile's picture, else its initial. */
    data class Picture(val avatarUrl: String?, val letter: String?)

    data class Flight(val from: Rect, val to: Rect, val picture: Picture, val startedAt: Long)

    /** The profile circle in the corner, in root coordinates. */
    var target by mutableStateOf<Rect?>(null)
        private set
    var flight by mutableStateOf<Flight?>(null)
        private set
    /** Between the pick and the moment the rows may come. */
    var holding by mutableStateOf(false)
        private set
    /** When the picture last landed: the home page's rows take this as their new epoch. */
    var landedAt by mutableStateOf<Long?>(null)
        private set
    /** Where the root composable sits on the screen; reported by the flight layer. */
    var rootOnScreen: Offset = Offset.Zero

    private var picked: Pair<Rect, Picture>? = null
    private var job: Job? = null

    /**
     * Only a circle that really sits at the top right. A tab's page reports its circle while it
     * fades in and out too, and nothing should fly into a page that is on its way out.
     */
    fun reportTarget(bounds: Rect, rootWidthPx: Float) {
        if (bounds.width <= 0f || bounds.right < rootWidthPx - TargetMarginPx) return
        target = bounds
    }

    /** The row's circle, in root coordinates, or in screen coordinates with [onScreen]. */
    fun pickedFrom(bounds: Rect, picture: Picture, onScreen: Boolean = false) {
        picked = (if (onScreen) bounds.translate(-rootOnScreen.x, -rootOnScreen.y) else bounds) to picture
    }

    /** Runs [switch] inside the movement, or at once when there is nothing to move. */
    fun switch(motionWanted: Boolean, switch: () -> Unit) {
        if (job?.isActive == true) return
        val from = picked
        picked = null
        val to = target
        if (!motionWanted || from == null || to == null) {
            switch()
            return
        }
        val startedAt = clock()
        flight = Flight(from.first, to, from.second, startedAt)
        holding = true
        job = scope.launch {
            try {
                withTimeout(HoldLimitMs) {
                    delay(SwitchAtMs)
                    switch()
                    delay(LandAtMs - SwitchAtMs)
                    flight = null
                    landedAt = clock()
                    delay(RowsAfterLandMs)
                }
            } finally {
                // Whatever ended the movement, the page is never left held or covered.
                flight = null
                if ((landedAt ?: -1L) < startedAt) landedAt = clock()
                holding = false
            }
        }
    }

    companion object {
        const val SwitchAtMs = 150L
        const val LandAtMs = 450L
        const val RowsAfterLandMs = 30L
        /** Whatever happens, the rows never wait longer than this. */
        const val HoldLimitMs = 1_500L
        /** A circle further than this from the right edge belongs to a page mid-transition. */
        const val TargetMarginPx = 400f
    }
}

/** The motion for the screens that report the corner circle or hide it while the picture flies. */
val LocalProfileSwitchMotion = staticCompositionLocalOf<ProfileSwitchMotion?> { null }
