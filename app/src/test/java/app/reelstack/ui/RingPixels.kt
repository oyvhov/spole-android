package app.reelstack.ui

import androidx.compose.ui.geometry.Rect

/**
 * The activity's content as it is drawn. Robolectric's native renderer draws the real Compose tree
 * into a Canvas, but does not deliver the PixelCopy callback that captureToImage() waits for.
 */
internal fun drawnContent(activity: android.app.Activity): android.graphics.Bitmap {
    val content = activity.findViewById<android.view.View>(android.R.id.content)
    val bitmap = android.graphics.Bitmap.createBitmap(content.width, content.height, android.graphics.Bitmap.Config.ARGB_8888)
    content.draw(android.graphics.Canvas(bitmap))
    return bitmap
}

/** Is the white focus ring drawn at this point, [xDp] and [yDp] into [origin]? Pages and placeholders are dark. */
internal fun android.graphics.Bitmap.ringAt(xDp: Float, yDp: Float, origin: Rect, density: Float): Boolean {
    val pixel = getPixel((origin.left + xDp * density).toInt(), (origin.top + yDp * density).toInt())
    // A faint state-layer tint over nothing is white with little alpha; the ring is opaque.
    return android.graphics.Color.alpha(pixel) > 200 &&
        android.graphics.Color.red(pixel) > 200 && android.graphics.Color.green(pixel) > 200 && android.graphics.Color.blue(pixel) > 200
}
