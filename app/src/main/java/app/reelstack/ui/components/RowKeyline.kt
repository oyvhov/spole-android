package app.reelstack.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Where a horizontal television row keeps the card in focus.
 *
 * Focus that comes into the row from above or below pins the place it lands on, if that card is whole inside
 * [startInset]..(container - [endInset]); otherwise it pins [startInset]. Left and Right then move the row one
 * card per press under a focus that stays on its pin, as Leanback and Google TV do; at either end the row cannot
 * move further and the focus walks instead.
 *
 * The least distance (see [DetailBringIntoView]) leaves the focused card flush against the far edge, and every
 * card before it wherever the last step happened to put it: on the Shield the first visible card was sliced by
 * the side menu with its caption cut mid-word (2026-11-07). Beta37 then let the focus walk to the far edge and
 * moved the row four cards at once, which read as the screen jumping; and a row that always pulled the card in
 * focus to its start slid sideways whenever focus came down onto any other card.
 */
internal class RowKeylineSpec(private val startInset: Float, private val endInset: Float) {
    fun entryPin(offset: Float, size: Float, containerSize: Float): Float {
        val end = (containerSize - endInset).coerceAtLeast(startInset)
        return if (offset >= startInset && offset + size <= end) offset else startInset
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
    /** Where the card in focus stands in the row; none while the focus is outside it. */
    var pin: Float? = null
}

/**
 * Scrolls [state] so that the card in focus stands on its pin; see [RowKeylineSpec].
 *
 * It cannot be a `BringIntoViewSpec`: the framework stops its scroll as soon as the focused card is
 * wholly in view, however far the spec asked it to go, so the row ended up part-way to the keyline
 * with a card sliced at its start. This measures the focused card itself and scrolls by exactly the
 * distance on a [ScrollGlide]. A press while the row is still moving aims the same glide at the new card,
 * from where the row has come to and with the speed it has, so the row never stops and starts again.
 * Pair it with [NoBringIntoView] for the row's own scrolling.
 */
@Composable
internal fun Modifier.rowKeylineScroll(state: LazyListState, startInset: Dp, endInset: Dp): Modifier {
    val density = LocalDensity.current
    val spec = remember(density, startInset, endInset) {
        RowKeylineSpec(with(density) { startInset.toPx() }, with(density) { endInset.toPx() })
    }
    val target = remember { KeylineTarget() }
    val signal = remember { Channel<Unit>(Channel.CONFLATED) }
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    val glide = remember(state) { ScrollGlide() }
    LaunchedEffect(state, spec, motion) {
        var move: kotlinx.coroutines.Job? = null
        var aimedAt: LayoutCoordinates? = null
        for (changed in signal) {
            val row = target.row ?: continue
            val focused = target.focused?.takeIf { it.isAttached } ?: continue
            if (!row.isAttached) continue
            // The card moves with the row while the row glides towards it; only a new card is a new aim.
            if (focused === aimedAt && move?.isActive == true) continue
            // The centre, not the corner: a focused card is scaled around its centre, which leaves the
            // centre where the layout put it.
            val width = focused.size.width.toFloat()
            val centre = row.localPositionOf(focused, Offset(width / 2f, focused.size.height / 2f)).x
            val left = centre - width / 2f
            val pin = target.pin ?: spec.entryPin(left, width, row.size.width.toFloat()).also { target.pin = it }
            val distance = left - pin
            if (abs(distance) >= 1f) {
                aimedAt = focused
                move?.cancel()
                move = launch { glide.by(state, distance, motion) }
            }
        }
    }
    return this.onGloballyPositioned { target.row = it }
        // Only focus that leaves the row lets go of the pin; between two cards the focused bounds pass through none.
        .onFocusChanged { if (!it.hasFocus) target.pin = null }
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
): Modifier = drawWithContent {
        // Read here so scrolling redraws the mask without recomposing the row.
        val start = if (startFade > 0.dp && state.canScrollBackward) startFade.toPx().coerceAtMost(size.width) else 0f
        val end = if (endFade > 0.dp && state.canScrollForward) endFade.toPx().coerceAtMost(size.width - start) else 0f
        if (start == 0f && end == 0f) {
            drawContent()
            return@drawWithContent
        }
        // The card in focus grows past the row's top and bottom into the room kept for it; that is drawn too.
        val top = -size.height
        val bottom = size.height * 2
        // Named here: inside buildList, `size` is the list's.
        val width = size.width
        val bands = buildList {
            if (start > 0f) {
                val clear = startClear.toPx()
                add(FadeBand(Rect(0f, top, start, bottom), Brush.horizontalGradient(
                    0f to Color.Transparent, (clear / start).coerceIn(0f, 1f) to Color.Transparent, 1f to Color.Black,
                    startX = 0f, endX = start)))
            }
            if (end > 0f) add(FadeBand(Rect(width - end, top, width, bottom), Brush.horizontalGradient(
                0f to Color.Black, 1f to Color.Transparent, startX = width - end, endX = width)))
        }
        drawFaded(Rect(start, top, width - end, bottom), bands)
    }
