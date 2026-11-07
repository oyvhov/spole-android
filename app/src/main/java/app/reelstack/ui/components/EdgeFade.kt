package app.reelstack.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/** A strip of the content to fade out: [mask] is opaque where the content stays and clear where it goes. */
internal class FadeBand(val rect: Rect, val mask: Brush)

/**
 * Draws the content inside [plain] as it is and each [FadeBand] faded through its mask, and nothing else.
 *
 * Only a band goes through a layer of its own, as big as the band, as on Stova's Home. A fade done by
 * drawing the whole list into an offscreen layer and masking that redrew the full screen offscreen on every
 * frame of a scroll: 53 of 55 frames on the Shield did, and the GPU fell behind (2026-11-07). Content outside
 * [plain] and the bands is not drawn, which is how an edge hides what has passed it.
 */
internal fun ContentDrawScope.drawFaded(plain: Rect, bands: List<FadeBand>) {
    if (plain.width > 0f && plain.height > 0f) clipRect(plain.left, plain.top, plain.right, plain.bottom) {
        this@drawFaded.drawContent()
    }
    for (band in bands) {
        val r = band.rect
        if (r.width <= 0f || r.height <= 0f) continue
        drawIntoCanvas { canvas ->
            canvas.saveLayer(r, Paint())
            clipRect(r.left, r.top, r.right, r.bottom) { this@drawFaded.drawContent() }
            drawRect(band.mask, topLeft = r.topLeft, size = r.size, blendMode = BlendMode.DstIn)
            canvas.restore()
        }
    }
}
