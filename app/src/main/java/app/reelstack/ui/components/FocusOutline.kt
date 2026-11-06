package app.reelstack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import app.reelstack.ui.theme.SpoleMotion
import app.reelstack.ui.theme.motionTween

/** TV artwork owns focus feedback; a Material state layer must never tint its caption. */
@Composable
internal fun mediaCardIndication(): androidx.compose.foundation.Indication? {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val television = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    return if (television) null else androidx.compose.foundation.LocalIndication.current
}

/**
 * How far a focused card in the standard (LIFT) style grows.
 *
 * Measured on a Shield at 1080p, 1.06 left the focused card only 5-6 % wider than its neighbours,
 * which does not read from the sofa over a bright poster. Apple asks for the focused item to come
 * forward and Google TV scales its cards 1.025 / 1.05 / 1.1; 1.09 sits at the top of that range
 * and still fits in the gap between cards.
 */
internal const val LiftScale = 1.09f

/** Small controls in a lifted style (the player's transport row) keep the older, quieter lift. */
internal const val LiftControlScale = 1.06f

/**
 * The other half of focus on a television.
 *
 * An outline alone is one signal, and from three metres away over a bright poster it is the weakest
 * one available. Every television interface people already know — the Google TV launcher included —
 * lifts the focused card as well, because size reads at a distance where a two-pixel edge does not.
 *
 * Styles with a ring keep a small 1.035, enough to see without shoving a row's neighbours around.
 * The standard style has no ring on artwork, so a [card] there carries the focus with [LiftScale],
 * the shadow of [liftShadow] and the light of [ring]. Press still wins, so a card that is being
 * clicked dips under the finger.
 *
 * Reduced motion never animates the size. Where a ring carries the focus the card stays put; where
 * nothing else does (a lifted card) it takes its final size at once, because the size is the
 * indication and a fade between two sizes is still a movement.
 *
 * Keep the animated state unread until graphicsLayer. Returning its value here subscribes the
 * entire calling card/button to every animation frame, even though its content never changes.
 */
@Composable
internal fun animateFocusScale(focused: Boolean, pressed: Boolean, card: Boolean = true): State<Float> {
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    // A lifted card has no ring to carry it, so it grows as much as Stova's do.
    val lift = app.reelstack.ui.theme.LocalPersonalization.current.focusStyle == app.reelstack.data.model.FocusStyle.LIFT
    val target = when {
        pressed && motion -> 0.985f
        focused && lift -> if (card) LiftScale else LiftControlScale
        focused && motion -> 1.035f
        else -> 1f
    }
    return animateFloatAsState(target, if (motion) motionTween(SpoleMotion.FocusMs) else snap(), label = "focus-scale")
}

/**
 * Room for a ring around words, without moving the words. The element is measured [horizontal]
 * wider (and [vertical] taller) on each side and still takes only its old room in the layout, so a
 * text link stays on the edge of its column while its ring, and the padding the caller puts inside
 * it, stand just outside. Put it before [focusOutline]; fill the extra room with padding after the
 * clickable.
 */
internal fun Modifier.focusBleed(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp = 0.dp): Modifier =
    this.layout { measurable, constraints ->
        val h = horizontal.roundToPx()
        val v = vertical.roundToPx()
        fun grow(value: Int, by: Int) = if (value == androidx.compose.ui.unit.Constraints.Infinity || value == 0) value else value + 2 * by
        val placeable = measurable.measure(androidx.compose.ui.unit.Constraints(
            minWidth = grow(constraints.minWidth, h), maxWidth = grow(constraints.maxWidth, h),
            minHeight = grow(constraints.minHeight, v), maxHeight = grow(constraints.maxHeight, v)))
        layout((placeable.width - 2 * h).coerceAtLeast(0), (placeable.height - 2 * v).coerceAtLeast(0)) {
            placeable.place(-h, -v)
        }
    }

/**
 * Draw-only keyboard focus with soft accent glow and ambient drop shadow. On [artwork], under
 * [app.reelstack.data.model.FocusStyle.LIFT], there is no ring: the card grows, and the rest of its
 * shelf stays as it is, as in Stova (which stopped dimming them on 2026-11-01).
 */
@Composable
internal fun Modifier.focusOutline(source: InteractionSource, shape: Shape, glow: Boolean = true, artwork: Boolean = false): Modifier {
    val focused by source.collectIsFocusedAsState()
    return ring(focused, shape, glow, artwork)
}

/** White laid over a focused lifted card's artwork: light enough to leave the poster itself alone. */
private const val LiftIllumination = 0.05f

/**
 * The light on a focused lifted card. The artwork is never dimmed and its neighbours stay at full
 * brightness; the focused one gets a little lighter, which is what makes it look lit rather than
 * merely larger.
 */
private fun Modifier.illuminate(alpha: State<Float>, shape: Shape): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawWithContent {
        drawContent()
        val a = alpha.value
        if (a > 0.001f) drawOutline(outline, SolidColor(Color.White.copy(alpha = LiftIllumination * a)))
    }
}

/** One soft shadow: [spreadDp] wide, [total] opaque at the card's edge, [offsetYDp] lower than the card. */
private class GlowLayer(val color: Color, val spreadDp: Float, val offsetYDp: Float, val total: Float)

private const val GlowSteps = 10

/**
 * The elevation of a focused lifted card: a soft shadow that follows [shape] and gives the card
 * something to stand in front of.
 *
 * Two layers, because no single colour reads on both grounds Spole puts cards on. A black drop
 * shadow, a little lower than the card, shows on a bright poster row or a backdrop but vanishes on
 * the near-black page; a faint halo in the text colour shows on the page and costs nothing on a
 * bright ground. Both are stacks of outlines, not a blur, so they look the same on every device and
 * are cheap: [GlowSteps] outlines per layer, drawn only while focused.
 *
 * It draws outside the card, so put it before `clip`, which would cut it off at the card's edge.
 * It does nothing in any style but LIFT. Reduced motion keeps it; only the fade is shortened.
 */
@Composable
internal fun Modifier.liftShadow(source: InteractionSource, shape: Shape): Modifier {
    if (app.reelstack.ui.theme.LocalPersonalization.current.focusStyle != app.reelstack.data.model.FocusStyle.LIFT) return this
    val focused by source.collectIsFocusedAsState()
    val alphaState = animateFloatAsState(if (focused) 1f else 0f, motionTween(SpoleMotion.FocusMs), label = "lift-shadow")
    val halo = MaterialTheme.colorScheme.onSurface
    return this.drawWithCache {
        val layers = listOf(
            GlowLayer(Color.Black, spreadDp = 16f, offsetYDp = 6f, total = 0.60f),
            GlowLayer(halo, spreadDp = 12f, offsetYDp = 0f, total = 0.16f),
        )
        // The outlines depend only on the size, so they are made once per size, not once per frame.
        val rings = layers.map { layer ->
            val spread = layer.spreadDp.dp.toPx()
            List(GlowSteps) { i ->
                val grow = spread * (i + 1) / GlowSteps
                grow to shape.createOutline(androidx.compose.ui.geometry.Size(size.width + 2 * grow, size.height + 2 * grow), layoutDirection, this)
            }
        }
        onDrawWithContent {
            val alpha = alphaState.value
            if (alpha > 0.001f) layers.forEachIndexed { index, layer ->
                val stepAlpha = layer.total * alpha / GlowSteps
                val dy = layer.offsetYDp.dp.toPx()
                rings[index].forEach { (grow, outline) ->
                    translate(-grow, -grow + dy) {
                        drawOutline(outline, SolidColor(layer.color.copy(alpha = stepAlpha)))
                    }
                }
            }
            drawContent()
        }
    }
}

/**
 * The same ring for a Material button, which keeps its interaction source to itself: it follows
 * focus anywhere inside the element. Give it the button's own [shape], and make the button at least
 * 48 dp tall ([androidx.compose.foundation.layout.heightIn]) so the ring is not drawn around the
 * invisible touch margin of a 40 dp button.
 */
@Composable
internal fun Modifier.focusRing(shape: Shape, glow: Boolean = true): Modifier {
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    return this.onFocusChanged { focused = it.hasFocus }.ring(focused, shape, glow)
}

@Composable
private fun Modifier.ring(focused: Boolean, shape: Shape, glow: Boolean, artwork: Boolean = false): Modifier {
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    val alphaState = animateFloatAsState(if (focused) 1f else 0f, motionTween(SpoleMotion.FocusMs), label = "focus-outline")
    val style = app.reelstack.ui.theme.LocalPersonalization.current.focusStyle
    val lift = style == app.reelstack.data.model.FocusStyle.LIFT
    if (lift && artwork) return this.illuminate(alphaState, shape)
    val color = if (style == app.reelstack.data.model.FocusStyle.ACCENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val strokeWidthDp = if (style == app.reelstack.data.model.FocusStyle.BOLD || (lift && isTelevision())) 4.dp else 2.dp

    return this.drawWithCache {
        val strokeWidthPx = strokeWidthDp.toPx()
        val glowStrokeWidthPx = strokeWidthPx + 1.dp.toPx()
        val shadowStrokeWidthPx = strokeWidthPx + 4.dp.toPx()
        val borderStroke = Stroke(width = strokeWidthPx)
        val glowStroke = Stroke(width = glowStrokeWidthPx)
        val shadowStroke = Stroke(width = shadowStrokeWidthPx)
        val edge = strokeWidthPx / 2
        val outline = shape.createOutline(androidx.compose.ui.geometry.Size(
            (size.width - strokeWidthPx).coerceAtLeast(0f),
            (size.height - strokeWidthPx).coerceAtLeast(0f)), layoutDirection, this)

        onDrawWithContent {
            val alpha = alphaState.value
            if (glow && motion && alpha > 0.001f) {
                inset(edge) {
                drawOutline(
                    outline = outline,
                    brush = SolidColor(Color.Black.copy(alpha = 0.65f * alpha)),
                    style = shadowStroke,
                )
                drawOutline(
                    outline = outline,
                    brush = SolidColor(color.copy(alpha = 0.18f * alpha)),
                    style = glowStroke,
                )
                }
            }
            drawContent()
            if (alpha > 0.001f) {
                inset(edge) {
                drawOutline(
                    outline = outline,
                    brush = SolidColor(color.copy(alpha = alpha)),
                    style = borderStroke,
                )
                }
            }
        }
    }
}
