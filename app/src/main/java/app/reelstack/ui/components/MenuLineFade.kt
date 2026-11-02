package app.reelstack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A page that starts below the television's menu line fades in under it, instead of ending in a hard cut
 * that slices through a word.
 */
internal fun Modifier.softTopEdge(edge: Dp = 28.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val height = edge.toPx()
            drawRect(
                Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = height),
                size = Size(size.width, height), blendMode = BlendMode.DstIn,
            )
        }

/**
 * A page whose picture runs behind the menu line. At rest nothing is touched; as the page scrolls up, what
 * reaches the line fades out, so a title or a row of figures never shows through the tabs.
 *
 * [scrolledPx] is how far the page has moved, read while drawing, so scrolling redraws only this layer.
 */
internal fun Modifier.fadeUnderMenuLine(inset: Dp, scrolledPx: () -> Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val line = inset.toPx()
            val strength = menuLineFadeStrength(scrolledPx(), line)
            if (strength > 0f) {
                // Hidden all the way through the line, then a soft rise back to the page below it.
                val end = line * 1.4f
                val hidden = Color.Black.copy(alpha = 1f - strength)
                drawRect(
                    Brush.verticalGradient(0f to hidden, line / end to hidden, 1f to Color.Black, startY = 0f, endY = end),
                    size = Size(size.width, end), blendMode = BlendMode.DstIn,
                )
            }
        }

/** 0 while the page stands at rest, 1 once it has scrolled the full height of the line. */
internal fun menuLineFadeStrength(scrolledPx: Float, linePx: Float): Float =
    if (linePx > 0f) (scrolledPx / linePx).coerceIn(0f, 1f) else 0f
