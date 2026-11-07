package app.reelstack.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

/**
 * A background that is only drawn, for the parts of a card a page repeats many times over.
 *
 * In Compose Foundation 1.11 `Modifier.background` is also a node in the semantics tree, with its shape, whatever it
 * paints. Any accessibility service makes Compose sort every such node by position about every 100 ms while a page
 * moves; on the Shield, with Button Mapper on, that took a fifth of the main thread between frames and inside them
 * (2026-11-07). A track, a scrim or a chip on a card tells a screen reader nothing the card's label does not.
 */
internal fun Modifier.drawnBackground(color: Color, shape: Shape = RectangleShape): Modifier =
    if (shape === RectangleShape) drawBehind { drawRect(color) }
    else drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, color) }
    }

/** [drawnBackground] with a brush, such as the gradient that keeps words legible on artwork. */
internal fun Modifier.drawnBackground(brush: Brush, shape: Shape = RectangleShape): Modifier =
    if (shape === RectangleShape) drawBehind { drawRect(brush) }
    else drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, brush) }
    }
