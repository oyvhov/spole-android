package app.reelstack.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reelstack.ui.theme.SpoleMotion
import app.reelstack.ui.theme.motionTween
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs

/**
 * Where a horizontal television row puts the card in focus: it stays where it is while it is whole
 * inside [startInset]..(container - [endInset]), and otherwise moves to stand on [startInset].
 *
 * The least distance (see [DetailBringIntoView]) leaves the focused card flush against the far edge,
 * and every card before it wherever the last step happened to put it: on the Shield the first visible
 * card was sliced by the side menu with its caption cut mid-word (2026-11-07). Leanback aligns the
 * focused item to a fixed keyline for that reason. The row then moves in whole cards, in either
 * direction, and what lies before the keyline is out of the way or under the edge fade.
 *
 * [endInset] is the width of the end fade: a card focused inside the fade would be dimmed by it, so the
 * fade counts as outside the room cards may be focused in.
 */
internal class RowKeylineSpec(private val startInset: Float, private val endInset: Float) {
    fun scrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val end = (containerSize - endInset).coerceAtLeast(startInset)
        return if (offset >= startInset && offset + size <= end) 0f else offset - startInset
    }
}

/** Takes the framework's own bring-into-view out of a row that [rowKeylineScroll] moves instead. */
@OptIn(ExperimentalFoundationApi::class)
internal object NoBringIntoView : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

private class KeylineTarget {
    var row: LayoutCoordinates? = null
    var focused: LayoutCoordinates? = null
}

/**
 * Scrolls [state] so that the card in focus lands on the keyline of [RowKeylineSpec].
 *
 * It cannot be a `BringIntoViewSpec`: the framework stops its scroll as soon as the focused card is
 * wholly in view, however far the spec asked it to go, so the row ended up part-way to the keyline
 * with a card sliced at its start. This measures the focused card itself and scrolls by exactly the
 * distance, one move at a time. A move that is still running when the remote presses again is
 * finished first and the next is measured from where the row has come to, so the row never restarts
 * mid-motion. Pair it with [NoBringIntoView] for the row's own scrolling.
 */
@Composable
internal fun Modifier.rowKeylineScroll(state: LazyListState, startInset: Dp, endInset: Dp): Modifier {
    val density = LocalDensity.current
    val spec = remember(density, startInset, endInset) {
        RowKeylineSpec(with(density) { startInset.toPx() }, with(density) { endInset.toPx() })
    }
    val target = remember { KeylineTarget() }
    val signal = remember { Channel<Unit>(Channel.CONFLATED) }
    val motion = motionTween<Float>(SpoleMotion.ChangeMs)
    LaunchedEffect(state, spec) {
        for (changed in signal) {
            val row = target.row ?: continue
            val focused = target.focused?.takeIf { it.isAttached } ?: continue
            if (!row.isAttached) continue
            // The centre, not the corner: a focused card is scaled around its centre, which leaves the
            // centre where the layout put it.
            val width = focused.size.width.toFloat()
            val centre = row.localPositionOf(focused, Offset(width / 2f, focused.size.height / 2f)).x
            val distance = spec.scrollDistance(centre - width / 2f, width, row.size.width.toFloat())
            if (abs(distance) >= 1f) state.animateScrollBy(distance, motion)
        }
    }
    return this.onGloballyPositioned { target.row = it }
        .onFocusedBoundsChanged { target.focused = it; signal.trySend(Unit) }
}

/**
 * The room after a row's last card that lets the last card reach the keyline too.
 *
 * Scrolling stops where the content does. With the usual margin after the last card, the last few
 * cards of a row stood wherever the end happened to leave them and the first of those visible was
 * sliced at the start, however well the others were aligned. A row that fits whole keeps [base]: it
 * has nothing to scroll. Cards are taken to be of one width (a shelf's are; a strip of tabs is near
 * enough, and the last one is measured once it is in view).
 */
@Composable
internal fun rememberKeylineEndPadding(state: LazyListState, startInset: Dp, spacing: Dp, base: Dp): Dp {
    val density = LocalDensity.current
    val padding by remember(state, density, startInset, spacing, base) {
        androidx.compose.runtime.derivedStateOf {
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            val sample = items.firstOrNull { it.index == info.totalItemsCount - 1 } ?: items.firstOrNull()
            val baseSize = with(density) { base.roundToPx() }
            if (sample == null) return@derivedStateOf baseSize
            val start = with(density) { startInset.roundToPx() }
            val gap = with(density) { spacing.roundToPx() }
            val viewport = info.viewportSize.width
            val content = start + info.totalItemsCount * sample.size + (info.totalItemsCount - 1) * gap + baseSize
            if (content <= viewport) baseSize else maxOf(baseSize, viewport - start - sample.size)
        }
    }
    return with(density) { padding.toDp() }
}

/**
 * Lets a row fade out where it continues past an edge, so a card that is partly in view dissolves
 * instead of ending in a hard cut through its caption (Apple's tvOS tab bars do the same).
 *
 * The start fade is kept inside the gutter that the row's first card is indented by: it must not reach
 * the keyline, or the card in focus would be dimmed. [startClear] is the part of it that is fully
 * transparent, which is where the tail of the card before the focused one lies, and [startFade] ends
 * at the room the focused card grows into. The end fade has no focus in it, see [RowKeylineSpec].
 */
internal fun Modifier.rowEdgeFade(
    state: LazyListState,
    startClear: Dp = 0.dp,
    startFade: Dp = 0.dp,
    endFade: Dp = 40.dp,
): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        // Read here so scrolling redraws the mask without recomposing the row.
        if (startFade > 0.dp && state.canScrollBackward) {
            val clear = startClear.toPx()
            val end = startFade.toPx().coerceAtMost(size.width)
            drawRect(
                Brush.horizontalGradient(0f to Color.Transparent, (clear / end).coerceIn(0f, 1f) to Color.Transparent,
                    1f to Color.Black, startX = 0f, endX = end),
                size = Size(end, size.height), blendMode = BlendMode.DstIn)
        }
        if (endFade > 0.dp && state.canScrollForward) {
            val width = endFade.toPx().coerceAtMost(size.width)
            drawRect(
                Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent,
                    startX = size.width - width, endX = size.width),
                topLeft = Offset(size.width - width, 0f), size = Size(width, size.height), blendMode = BlendMode.DstIn)
        }
    }
