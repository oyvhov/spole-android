package app.reelstack.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Original 24-unit Spole family: open frames, film cuts and rounded ink strokes. */
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

    // ── Handlingar ───────────────────────────────────────────────────────────
    //
    // The navigation family used to stop at five glyphs, so the most characteristic thing in the
    // app sat in the bottom bar while every other icon on screen spoke Material's filled language.
    // These are the same 24-unit grid, the same 1.8 stroke and the same open frames, covering the
    // icons that actually appear beside them.

    /** A play triangle inside an open frame, so it reads as film rather than as audio. */
    val Play = glyph("Play") {
        moveTo(9f, 7.5f); lineTo(17f, 12f); lineTo(9f, 16.5f); close()
        moveTo(3f, 7f); quadTo(3f, 4f, 6f, 4f); lineTo(18f, 4f); quadTo(21f, 4f, 21f, 7f)
        lineTo(21f, 17f); quadTo(21f, 20f, 18f, 20f); lineTo(6f, 20f); quadTo(3f, 20f, 3f, 17f); close()
    }

    val Pause = glyph("Pause") {
        moveTo(9.5f, 5f); lineTo(9.5f, 19f)
        moveTo(14.5f, 5f); lineTo(14.5f, 19f)
    }

    /** Two film cuts and a tick: something that finished, not a generic checkmark. */
    val Done = glyph("Done") {
        moveTo(4f, 12.5f); lineTo(9f, 17.5f); lineTo(20f, 6.5f)
    }

    /**
     * Favourite, as a stroked heart and a filled one.
     *
     * Two shapes rather than one shape in two colours: on a dark page an accent outline and an
     * accent fill are the same hue, and the only thing separating "is a favourite" from "could be
     * one" would be a few pixels of ink.
     */
    val Heart = glyph("Heart") {
        moveTo(12f, 20f)
        curveTo(12f, 20f, 3.5f, 14.5f, 3.5f, 9.2f)
        curveTo(3.5f, 6.3f, 5.8f, 4.3f, 8.3f, 4.3f)
        curveTo(10.1f, 4.3f, 11.4f, 5.4f, 12f, 6.4f)
        curveTo(12.6f, 5.4f, 13.9f, 4.3f, 15.7f, 4.3f)
        curveTo(18.2f, 4.3f, 20.5f, 6.3f, 20.5f, 9.2f)
        curveTo(20.5f, 14.5f, 12f, 20f, 12f, 20f)
        close()
    }

    val HeartFilled = ImageVector.Builder(
        name = "Spole.HeartFilled", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 20f)
            curveTo(12f, 20f, 3.5f, 14.5f, 3.5f, 9.2f)
            curveTo(3.5f, 6.3f, 5.8f, 4.3f, 8.3f, 4.3f)
            curveTo(10.1f, 4.3f, 11.4f, 5.4f, 12f, 6.4f)
            curveTo(12.6f, 5.4f, 13.9f, 4.3f, 15.7f, 4.3f)
            curveTo(18.2f, 4.3f, 20.5f, 6.3f, 20.5f, 9.2f)
            curveTo(20.5f, 14.5f, 12f, 20f, 12f, 20f)
            close()
        }
    }.build()

    val DoneCircle = glyph("DoneCircle") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(7.5f, 12.2f); lineTo(10.8f, 15.5f); lineTo(16.5f, 9f)
    }

    val Close = glyph("Close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }

    val Add = glyph("Add") {
        moveTo(12f, 5f); lineTo(12f, 19f)
        moveTo(5f, 12f); lineTo(19f, 12f)
    }

    /** The search lens sits inside the same open frame the Discover glyph uses. */
    val Search = glyph("Search") {
        moveTo(11f, 4f); arcTo(6.5f, 6.5f, 0f, true, true, 10.99f, 4f); close()
        moveTo(15.6f, 15.6f); lineTo(20.5f, 20.5f)
    }

    /** A reel of film seen edge-on: the app's own mark for a movie. */
    val Movie = glyph("Movie") {
        moveTo(3f, 8f); quadTo(3f, 5f, 6f, 5f); lineTo(18f, 5f); quadTo(21f, 5f, 21f, 8f)
        lineTo(21f, 16f); quadTo(21f, 19f, 18f, 19f); lineTo(6f, 19f); quadTo(3f, 19f, 3f, 16f); close()
        moveTo(3f, 9.5f); lineTo(21f, 9.5f)
        moveTo(3f, 14.5f); lineTo(21f, 14.5f)
        moveTo(8f, 5f); lineTo(8f, 9.5f)
        moveTo(16f, 14.5f); lineTo(16f, 19f)
    }

    /** A screen on a stand, drawn open like the rest of the family. */
    val Screen = glyph("Screen") {
        moveTo(3f, 6f); quadTo(3f, 4f, 5f, 4f); lineTo(19f, 4f); quadTo(21f, 4f, 21f, 6f)
        lineTo(21f, 15f); quadTo(21f, 17f, 19f, 17f); lineTo(5f, 17f); quadTo(3f, 17f, 3f, 15f); close()
        moveTo(8f, 20.5f); lineTo(16f, 20.5f)
        moveTo(12f, 17f); lineTo(12f, 20.5f)
    }

    /** A clock with the hands set high, for things that have not happened yet. */
    val Clock = glyph("Clock") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15.5f, 14f)
    }

    /** An arrow into a tray: arriving, not stored. */
    val Download = glyph("Download") {
        moveTo(12f, 4f); lineTo(12f, 14.5f)
        moveTo(7.5f, 10.5f); lineTo(12f, 15f); lineTo(16.5f, 10.5f)
        moveTo(4.5f, 18f); quadTo(4.5f, 20f, 6.5f, 20f); lineTo(17.5f, 20f); quadTo(19.5f, 20f, 19.5f, 18f)
    }

    val Bell = glyph("Bell") {
        moveTo(6f, 17f); lineTo(18f, 17f); lineTo(16.5f, 14.5f); lineTo(16.5f, 10.5f)
        quadTo(16.5f, 6f, 12f, 6f); quadTo(7.5f, 6f, 7.5f, 10.5f); lineTo(7.5f, 14.5f); close()
        moveTo(10.2f, 20f); quadTo(12f, 21.4f, 13.8f, 20f)
    }

    val Info = glyph("Info") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(12f, 11f); lineTo(12f, 16.5f)
        moveTo(12f, 7.6f); lineTo(12f, 8.2f)
    }

    /** Sliders, matching the Settings glyph already in the family. */
    val Tune = glyph("Tune") {
        moveTo(4f, 8f); lineTo(20f, 8f)
        moveTo(4f, 16f); lineTo(20f, 16f)
        moveTo(9f, 5.6f); lineTo(9f, 10.4f)
        moveTo(15f, 13.6f); lineTo(15f, 18.4f)
    }

    val ChevronDown = glyph("ChevronDown") { moveTo(6.5f, 9.5f); lineTo(12f, 15f); lineTo(17.5f, 9.5f) }
    val ChevronUp = glyph("ChevronUp") { moveTo(6.5f, 14.5f); lineTo(12f, 9f); lineTo(17.5f, 14.5f) }
    val ChevronRight = glyph("ChevronRight") { moveTo(9.5f, 5.5f); lineTo(15f, 12f); lineTo(9.5f, 18.5f) }
    val ArrowBack = glyph("ArrowBack") {
        moveTo(19f, 12f); lineTo(5f, 12f)
        moveTo(10.5f, 6.5f); lineTo(5f, 12f); lineTo(10.5f, 17.5f)
    }

    /** A list, drawn with the same short-then-long rhythm as the Activity glyph. */
    val ListLines = glyph("ListLines") {
        moveTo(4f, 6f); lineTo(5f, 6f)
        moveTo(9f, 6f); lineTo(20f, 6f)
        moveTo(4f, 12f); lineTo(5f, 12f)
        moveTo(9f, 12f); lineTo(20f, 12f)
        moveTo(4f, 18f); lineTo(5f, 18f)
        moveTo(9f, 18f); lineTo(20f, 18f)
    }
}
