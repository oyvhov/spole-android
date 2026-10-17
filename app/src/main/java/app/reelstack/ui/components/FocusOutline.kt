package app.reelstack.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.layout.layout

/** TV artwork owns focus feedback; a Material state layer must never tint its caption. */
@Composable
internal fun mediaCardIndication(): androidx.compose.foundation.Indication? {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val television = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    return if (television) null else androidx.compose.foundation.LocalIndication.current
}

/**
 * The other half of focus on a television.
 *
 * An outline alone is one signal, and from three metres away over a bright poster it is the weakest
 * one available. Every television interface people already know — the Google TV launcher included —
 * lifts the focused card as well, because size reads at a distance where a two-pixel edge does not.
 *
 * Deliberately small: 1.035 is enough to see and small enough that a row does not shove its
 * neighbours around. Press still wins, so a card that is being clicked dips under the finger, and
 * reduced motion drops the scaling entirely and leaves the outline and shadow to do the work.
 *
 * Keep the animated state unread until graphicsLayer. Returning its value here subscribes the
 * entire calling card/button to every animation frame, even though its content never changes.
 */
@Composable
internal fun animateFocusScale(focused: Boolean, pressed: Boolean): State<Float> {
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    val target = when {
        pressed && motion -> 0.985f
        !motion -> 1f
        focused -> 1.035f
        else -> 1f
    }
    return animateFloatAsState(target, tween(if (motion) 140 else 0), label = "focus-scale")
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

/** Draw-only keyboard focus with soft accent glow and ambient drop shadow. */
@Composable
internal fun Modifier.focusOutline(source: InteractionSource, shape: Shape, glow: Boolean = true): Modifier {
    val focused by source.collectIsFocusedAsState()
    return ring(focused, shape, glow)
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
private fun Modifier.ring(focused: Boolean, shape: Shape, glow: Boolean): Modifier {
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
    val alphaState = animateFloatAsState(if (focused) 1f else 0f, tween(if (motion) 140 else 0), label = "focus-outline")
    val style = app.reelstack.ui.theme.LocalPersonalization.current.focusStyle
    val color = if (style == app.reelstack.data.model.FocusStyle.ACCENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val strokeWidthDp = if (style == app.reelstack.data.model.FocusStyle.BOLD) 4.dp else 2.dp

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
