package app.reelstack.ui.components

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/** Cached after decode; align the visible logo, not the transparent canvas supplied by the server. */
internal object ClearLogoTransformation : Transformation() {
    override val cacheKey = "spole-clearlogo-trim-v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (!input.hasAlpha()) return input
        val row = IntArray(input.width)
        var left = input.width
        var top = input.height
        var right = -1
        var bottom = -1
        for (y in 0 until input.height) {
            input.getPixels(row, 0, input.width, 0, y, input.width, 1)
            for (x in row.indices) if ((row[x] ushr 24) > 8) {
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = y
            }
        }
        if (right < left || bottom < top) return input
        return Bitmap.createBitmap(input, left, top, right - left + 1, bottom - top + 1)
    }
}
