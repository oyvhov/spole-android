package app.reelstack

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@androidx.compose.runtime.Composable
internal fun RoadmapTestTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    app.reelstack.ui.theme.ReelstackTheme {
        androidx.compose.material3.Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

/** Synthetic fixture images for local layout review, never the production account. */
internal fun SemanticsNodeInteraction.saveRoadmapImage(name: String) {
    val folder = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "roadmap-review").apply { mkdirs() }
    val bitmap = captureToImage().asAndroidBitmap()
    File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
}
