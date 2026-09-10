package app.reelstack.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Original 24-unit Spole navigation family: open frames, film cuts and rounded ink strokes. */
object SpoleIcons {
    private fun glyph(name: String, draw: PathBuilder.() -> Unit) = ImageVector.Builder(
        name = "Spole.$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
    }.build()

    val Home = glyph("Home") {
        moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f)
        moveTo(5f, 9f); lineTo(5f, 19f); quadTo(5f, 21f, 7f, 21f)
        lineTo(17f, 21f); quadTo(19f, 21f, 19f, 19f); lineTo(19f, 13f)
        moveTo(10f, 11f); lineTo(15f, 14f); lineTo(10f, 17f); close()
    }
    val Discover = glyph("Discover") {
        moveTo(8f, 3f); lineTo(5f, 3f); quadTo(3f, 3f, 3f, 5f); lineTo(3f, 8f)
        moveTo(16f, 3f); lineTo(19f, 3f); quadTo(21f, 3f, 21f, 5f); lineTo(21f, 8f)
        moveTo(21f, 16f); lineTo(21f, 19f); quadTo(21f, 21f, 19f, 21f); lineTo(16f, 21f)
        moveTo(8f, 21f); lineTo(5f, 21f); quadTo(3f, 21f, 3f, 19f); lineTo(3f, 16f)
        moveTo(15.5f, 8.5f); lineTo(13f, 13f); lineTo(8.5f, 15.5f)
        lineTo(11f, 11f); close()
    }
    val Library = glyph("Library") {
        moveTo(4f, 4f); lineTo(4f, 20f); lineTo(9f, 20f); lineTo(9f, 4f); close()
        moveTo(13f, 4f); lineTo(18f, 3f); lineTo(21f, 19f); lineTo(16f, 20f); close()
    }
    val Activity = glyph("Activity") {
        moveTo(4f, 6f); lineTo(5f, 6f)
        moveTo(9f, 6f); lineTo(20f, 6f)
        moveTo(4f, 12f); lineTo(5f, 12f)
        moveTo(9f, 12f); lineTo(20f, 12f)
        moveTo(4f, 18f); lineTo(5f, 18f)
        moveTo(9f, 18f); lineTo(16f, 18f)
    }
    val Settings = glyph("Settings") {
        moveTo(5f, 3f); lineTo(5f, 6f); moveTo(5f, 12f); lineTo(5f, 21f)
        moveTo(12f, 3f); lineTo(12f, 13f); moveTo(12f, 19f); lineTo(12f, 21f)
        moveTo(19f, 3f); lineTo(19f, 6f); moveTo(19f, 12f); lineTo(19f, 21f)
        moveTo(3f, 6f); lineTo(7f, 6f); lineTo(7f, 12f); lineTo(3f, 12f); close()
        moveTo(10f, 13f); lineTo(14f, 13f); lineTo(14f, 19f); lineTo(10f, 19f); close()
        moveTo(17f, 6f); lineTo(21f, 6f); lineTo(21f, 12f); lineTo(17f, 12f); close()
    }
    val Menu = glyph("Menu") {
        moveTo(4f, 5f); lineTo(4f, 19f)
        moveTo(9f, 6f); lineTo(21f, 6f)
        moveTo(9f, 12f); lineTo(18f, 12f)
        moveTo(9f, 18f); lineTo(21f, 18f)
    }
    val MenuClose = glyph("MenuClose") {
        moveTo(4f, 5f); lineTo(4f, 19f)
        moveTo(9f, 6f); lineTo(21f, 6f)
        moveTo(9f, 18f); lineTo(21f, 18f)
        moveTo(14f, 9f); lineTo(11f, 12f); lineTo(14f, 15f)
        moveTo(12f, 12f); lineTo(21f, 12f)
    }
}
