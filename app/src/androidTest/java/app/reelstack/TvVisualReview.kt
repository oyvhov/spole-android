package app.reelstack

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry

/** Optional, local-only evidence from synthetic fixtures. Never used on real-account AVDs. */
internal fun saveTvReview(name: String, image: ImageBitmap) {
    if (InstrumentationRegistry.getArguments().getString("reviewScreenshots") != "true") return
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    java.io.File(context.getExternalFilesDir(null), "review-$name.png").outputStream().use {
        image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
    }
}
