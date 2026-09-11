package app.reelstack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke

/** TV artwork owns focus feedback; a Material state layer must never tint its caption. */
@Composable
internal fun mediaCardIndication(): androidx.compose.foundation.Indication? {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val television = configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    return if (television) null else androidx.compose.foundation.LocalIndication.current
}

/** Draw-only keyboard focus with soft accent glow and ambient drop shadow. */
@Composable
internal fun Modifier.focusOutline(source: InteractionSource, shape: Shape, glow: Boolean = true): Modifier {
    val focused by source.collectIsFocusedAsState()
    val alphaState = animateFloatAsState(if (focused) 1f else 0f, tween(140), label = "focus-outline")
    val style = app.reelstack.ui.theme.LocalPersonalization.current.focusStyle
    val color = if (style == app.reelstack.data.model.FocusStyle.ACCENT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val strokeWidthDp = if (style == app.reelstack.data.model.FocusStyle.BOLD) 4.5.dp else 2.5.dp

    return this.drawWithCache {
        val strokeWidthPx = strokeWidthDp.toPx()
        val glowStrokeWidthPx = strokeWidthPx + 3.dp.toPx()
        val shadowStrokeWidthPx = strokeWidthPx + 6.dp.toPx()
        val borderStroke = Stroke(width = strokeWidthPx)
        val glowStroke = Stroke(width = glowStrokeWidthPx)
        val shadowStroke = Stroke(width = shadowStrokeWidthPx)
        val outline = shape.createOutline(size, layoutDirection, this)

        onDrawWithContent {
            val alpha = alphaState.value
            if (glow && alpha > 0.001f) {
                drawOutline(
                    outline = outline,
                    brush = SolidColor(Color.Black.copy(alpha = 0.65f * alpha)),
                    style = shadowStroke,
                )
                drawOutline(
                    outline = outline,
                    brush = SolidColor(color.copy(alpha = 0.40f * alpha)),
                    style = glowStroke,
                )
            }
            drawContent()
            if (alpha > 0.001f) {
                drawOutline(
                    outline = outline,
                    brush = SolidColor(color.copy(alpha = alpha)),
                    style = borderStroke,
                )
            }
        }
    }
}
