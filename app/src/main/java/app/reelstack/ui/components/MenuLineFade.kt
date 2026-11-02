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
