package app.reelstack.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps a remote's up and down presses in a lazy grid in their column. A press that outran the scroll
 * reached a row not laid out yet; the grid then laid that row out a title at a time and gave focus to
 * the first, so a quick press down from the third column landed in the first. A press now goes at once
 * when the row past focus is laid out, and otherwise waits until the scroll has brought it in. Only
 * one press waits at a time, so a held button stops when it is let go.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.steadyRemoteRows(grid: LazyGridState): Modifier {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()
    return this then remember(grid, focusManager, scope) {
        val press = WaitingPress(grid, focusManager, scope)
        Modifier
            .onPlaced { press.container = it }
            .onFocusedBoundsChanged { press.focused = it }
            .onFocusChanged { focus ->
                press.inside = focus.hasFocus
                if (!focus.hasFocus) press.direction = null
            }
            .onPreviewKeyEvent { event ->
                val direction = when (event.key) {
                    Key.DirectionDown -> FocusDirection.Down
                    Key.DirectionUp -> FocusDirection.Up
                    else -> {
                        // A press across, or anything else, overrides one still waiting.
                        if (event.type == KeyEventType.KeyDown) press.direction = null
                        return@onPreviewKeyEvent false
                    }
                }
                when {
                    event.type != KeyEventType.KeyDown -> false
                    !press.busy && press.rowIsReady(direction) -> false
                    else -> { press.wait(direction); true }
                }
            }
    }
}

private class WaitingPress(val grid: LazyGridState, val focusManager: FocusManager, val scope: CoroutineScope) {
    var container: LayoutCoordinates? = null
    var focused: LayoutCoordinates? = null
    var direction: FocusDirection? = null
    var inside = false
    private var job: Job? = null
    val busy get() = job?.isActive == true

    /**
     * Whether the row a press would move to is laid out: an item lies wholly past the focused one in
     * that direction. The grid lays out whole rows, so one item there means the row is there. With
     * nothing further to scroll to, the press is left to the grid as it is.
     */
    fun rowIsReady(direction: FocusDirection): Boolean {
        val down = direction == FocusDirection.Down
        if (if (down) !grid.canScrollForward else !grid.canScrollBackward) return true
        val box = container?.takeIf { it.isAttached } ?: return true
        val item = focused?.takeIf { it.isAttached } ?: return true
        val bounds = box.localBoundingBoxOf(item, clipBounds = false)
        val info = grid.layoutInfo
        return info.visibleItemsInfo.any {
            val top = it.offset.y - info.viewportStartOffset
            if (down) top >= bounds.bottom - 1 else top + it.size.height <= bounds.top + 1
        }
    }

    /** Holds a press until its row is laid out, or the scroll has stopped without it, then makes it. */
    fun wait(next: FocusDirection) {
        direction = next
        if (busy) return
        job = scope.launch {
            while (true) {
                val waiting = direction ?: break
                if (!rowIsReady(waiting)) {
                    // The last move asks for its scroll a frame or two after focus moved; until that
                    // begins, a grid standing still says nothing about whether the row will come.
                    repeat(2) { withFrameNanos { } }
                    snapshotFlow { rowIsReady(waiting) || !grid.isScrollInProgress }.first { it }
                }
                if (direction != waiting) continue
                direction = null
                if (inside) focusManager.moveFocus(waiting)
            }
        }
    }
}
