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

    /** Rating. Drawn as one stroked outline so it belongs with the rest, not a filled sticker. */
    /**
     * The six library shapes, drawn in the same 1.8-unit stroke as everything else.
     *
     * These sit in the sidebar next to a pinned library, so they are as visible as the navigation
     * icons beside them — and a Material glyph among Spole's own is the one place a mixed icon set
     * is impossible to miss.
     */
    val Kids = glyph("Kids") {
        moveTo(12f, 3.5f); arcTo(4.2f, 4.2f, 0f, true, true, 11.99f, 3.5f); close()
        moveTo(11.2f, 11.9f); lineTo(12f, 14.5f); lineTo(12.8f, 11.9f)
        moveTo(12f, 14.5f); lineTo(12f, 20.5f)
        moveTo(12f, 20.5f); quadTo(10.6f, 20.8f, 11.3f, 21.6f); quadTo(12f, 22.2f, 12.7f, 21.4f)
    }

    val Documentary = glyph("Documentary") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(3.2f, 12f); lineTo(20.8f, 12f)
        moveTo(12f, 3f); quadTo(16.5f, 12f, 12f, 21f); quadTo(7.5f, 12f, 12f, 3f); close()
    }

    val Music = glyph("Music") {
        moveTo(9.2f, 17.6f); arcTo(2.6f, 2.2f, 0f, true, true, 9.19f, 17.6f); close()
        moveTo(11.8f, 17.6f); lineTo(11.8f, 4.5f); lineTo(19.5f, 6.6f); lineTo(19.5f, 15.4f)
        moveTo(16.9f, 15.4f); arcTo(2.6f, 2.2f, 0f, true, true, 16.89f, 15.4f); close()
        moveTo(11.8f, 9.2f); lineTo(19.5f, 11.3f)
    }

    val Concert = glyph("Concert") {
        moveTo(12f, 3.2f); quadTo(14.4f, 3.2f, 14.4f, 5.6f); lineTo(14.4f, 10.6f)
        quadTo(14.4f, 13f, 12f, 13f); quadTo(9.6f, 13f, 9.6f, 10.6f)
        lineTo(9.6f, 5.6f); quadTo(9.6f, 3.2f, 12f, 3.2f); close()
        moveTo(6.2f, 10.4f); quadTo(6.2f, 16.6f, 12f, 16.6f); quadTo(17.8f, 16.6f, 17.8f, 10.4f)
        moveTo(12f, 16.6f); lineTo(12f, 20.8f)
        moveTo(8.6f, 20.8f); lineTo(15.4f, 20.8f)
    }

    val Animation = glyph("Animation") {
        moveTo(9.5f, 3.2f); lineTo(11.2f, 7.8f); lineTo(15.8f, 9.5f); lineTo(11.2f, 11.2f)
        lineTo(9.5f, 15.8f); lineTo(7.8f, 11.2f); lineTo(3.2f, 9.5f); lineTo(7.8f, 7.8f); close()
        moveTo(17.5f, 13.5f); lineTo(18.4f, 16.1f); lineTo(21f, 17f); lineTo(18.4f, 17.9f)
        lineTo(17.5f, 20.5f); lineTo(16.6f, 17.9f); lineTo(14f, 17f); lineTo(16.6f, 16.1f); close()
    }

    val Sport = glyph("Sport") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(12f, 8.2f); lineTo(15.9f, 11f); lineTo(14.4f, 15.6f); lineTo(9.6f, 15.6f)
        lineTo(8.1f, 11f); close()
        moveTo(12f, 3f); lineTo(12f, 8.2f)
        moveTo(20.6f, 9.3f); lineTo(15.9f, 11f)
        moveTo(17.3f, 19.8f); lineTo(14.4f, 15.6f)
        moveTo(6.7f, 19.8f); lineTo(9.6f, 15.6f)
        moveTo(3.4f, 9.3f); lineTo(8.1f, 11f)
    }

    /**
     * Ten seconds back and ten seconds on.
     *
     * The number belongs inside the arc rather than beside it: on a remote these two sit either
     * side of the play button, and a label outside the circle would make the pair asymmetric the
     * moment one of them is focused.
     */
    val Replay10 = glyph("Replay10") {
        moveTo(4f, 12f); arcTo(8f, 8f, 0f, true, false, 6.8f, 6.1f)
        moveTo(6.6f, 2.8f); lineTo(6.6f, 6.6f); lineTo(10.4f, 6.6f)
        moveTo(9.6f, 10.6f); lineTo(10.8f, 10f); lineTo(10.8f, 15f)
        moveTo(14.2f, 11.4f); quadTo(14.2f, 10f, 15.6f, 10f); quadTo(17f, 10f, 17f, 11.4f)
        lineTo(17f, 13.6f); quadTo(17f, 15f, 15.6f, 15f); quadTo(14.2f, 15f, 14.2f, 13.6f); close()
    }

    val Forward10 = glyph("Forward10") {
        moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 17.2f, 6.1f)
        moveTo(17.4f, 2.8f); lineTo(17.4f, 6.6f); lineTo(13.6f, 6.6f)
        moveTo(6.6f, 10.6f); lineTo(7.8f, 10f); lineTo(7.8f, 15f)
        moveTo(11.2f, 11.4f); quadTo(11.2f, 10f, 12.6f, 10f); quadTo(14f, 10f, 14f, 11.4f)
        lineTo(14f, 13.6f); quadTo(14f, 15f, 12.6f, 15f); quadTo(11.2f, 15f, 11.2f, 13.6f); close()
    }

    /** Appearance. A palette with its thumb hole, not a paint tin. */
    val Palette = glyph("Palette") {
        moveTo(12f, 3.2f); quadTo(20.6f, 3.2f, 20.6f, 10.6f)
        quadTo(20.6f, 14f, 17.2f, 14f); lineTo(15.4f, 14f)
        quadTo(13.6f, 14f, 13.6f, 15.8f); quadTo(13.6f, 17.2f, 14.4f, 18f)
        quadTo(15.2f, 18.8f, 14.4f, 19.8f); quadTo(13.6f, 20.8f, 12f, 20.8f)
        quadTo(3.4f, 20.8f, 3.4f, 12f); quadTo(3.4f, 3.2f, 12f, 3.2f); close()
        moveTo(7.6f, 10.2f); arcTo(1.1f, 1.1f, 0f, true, true, 7.59f, 10.2f); close()
        moveTo(11f, 7.2f); arcTo(1.1f, 1.1f, 0f, true, true, 10.99f, 7.2f); close()
        moveTo(15.4f, 8.2f); arcTo(1.1f, 1.1f, 0f, true, true, 15.39f, 8.2f); close()
    }

    /** Your services. Two stacked server shelves with a status light on each. */
    val Server = glyph("Server") {
        moveTo(3.5f, 4.5f); lineTo(20.5f, 4.5f); lineTo(20.5f, 10.2f); lineTo(3.5f, 10.2f); close()
        moveTo(3.5f, 13.8f); lineTo(20.5f, 13.8f); lineTo(20.5f, 19.5f); lineTo(3.5f, 19.5f); close()
        moveTo(6.8f, 7.35f); lineTo(6.81f, 7.35f)
        moveTo(6.8f, 16.65f); lineTo(6.81f, 16.65f)
        moveTo(10.5f, 7.35f); lineTo(17.2f, 7.35f)
        moveTo(10.5f, 16.65f); lineTo(17.2f, 16.65f)
    }

    /** Leaves the app: a frame with a corner opened and an arrow going out of it. */
    val OpenExternal = glyph("OpenExternal") {
        moveTo(13.5f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10.5f)
        moveTo(19.5f, 4.5f); lineTo(11.5f, 12.5f)
        moveTo(16.5f, 14.5f); lineTo(16.5f, 19.5f); lineTo(4.5f, 19.5f); lineTo(4.5f, 7.5f)
        lineTo(9.5f, 7.5f)
    }

    /** Something went wrong. A circle with the same stroke as everything else, not a filled sign. */
    val Alert = glyph("Alert") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(12f, 7.2f); lineTo(12f, 13f)
        moveTo(12f, 16.4f); lineTo(12.01f, 16.4f)
    }

    /** Language. A globe with a meridian, matching [Documentary] so the pair reads as one family. */
    val Language = glyph("Language") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f); close()
        moveTo(3.2f, 12f); lineTo(20.8f, 12f)
        moveTo(12f, 3f); quadTo(16.5f, 12f, 12f, 21f); quadTo(7.5f, 12f, 12f, 3f); close()
        moveTo(5.2f, 6.8f); quadTo(12f, 9.4f, 18.8f, 6.8f)
        moveTo(5.2f, 17.2f); quadTo(12f, 14.6f, 18.8f, 17.2f)
    }

    /** In the catalogue but not in your library: a cloud with a tick inside it. */
    val CloudReady = glyph("CloudReady") {
        moveTo(7f, 18.5f); quadTo(3.2f, 18.5f, 3.2f, 14.7f)
        quadTo(3.2f, 11.3f, 6.6f, 10.9f)
        quadTo(7.4f, 6.2f, 11.8f, 6.2f); quadTo(15.8f, 6.2f, 16.8f, 10f)
        quadTo(20.8f, 10.2f, 20.8f, 14.3f); quadTo(20.8f, 18.5f, 16.6f, 18.5f); close()
        moveTo(9.4f, 14.2f); lineTo(11.3f, 16f); lineTo(14.8f, 12.2f)
    }

    /** Notifications off. The same bell with a line through it, so the pair reads as one switch. */
    val BellOff = glyph("BellOff") {
        moveTo(6.4f, 17f); quadTo(6.4f, 10.4f, 7.6f, 8.4f)
        moveTo(10.2f, 5.4f); quadTo(11f, 4.6f, 12f, 4.6f); quadTo(17.6f, 4.6f, 17.6f, 12f)
        lineTo(17.6f, 17f); lineTo(9.4f, 17f)
        moveTo(6.4f, 17f); lineTo(17.6f, 17f)
        moveTo(10.4f, 19.6f); quadTo(12f, 21.2f, 13.6f, 19.6f)
        moveTo(4.2f, 3.6f); lineTo(20f, 20.4f)
    }

    val Eye = glyph("Eye") {
        moveTo(2.6f, 12f); quadTo(6.6f, 5.6f, 12f, 5.6f); quadTo(17.4f, 5.6f, 21.4f, 12f)
        quadTo(17.4f, 18.4f, 12f, 18.4f); quadTo(6.6f, 18.4f, 2.6f, 12f); close()
        moveTo(12f, 9f); arcTo(3f, 3f, 0f, true, true, 11.99f, 9f); close()
    }

    val EyeOff = glyph("EyeOff") {
        moveTo(2.6f, 12f); quadTo(6.6f, 5.6f, 12f, 5.6f); quadTo(17.4f, 5.6f, 21.4f, 12f)
        quadTo(17.4f, 18.4f, 12f, 18.4f); quadTo(6.6f, 18.4f, 2.6f, 12f); close()
        moveTo(12f, 9f); arcTo(3f, 3f, 0f, true, true, 11.99f, 9f); close()
        moveTo(4.2f, 3.6f); lineTo(20f, 20.4f)
    }

    /** Devices: a screen and a handset, which is what "this and your phone" means here. */
    val Devices = glyph("Devices") {
        moveTo(3.2f, 5.5f); lineTo(14.2f, 5.5f); lineTo(14.2f, 14.5f); lineTo(3.2f, 14.5f); close()
        moveTo(6.4f, 18.5f); lineTo(11f, 18.5f)
        moveTo(8.7f, 14.5f); lineTo(8.7f, 18.5f)
        moveTo(16.6f, 8.5f); lineTo(20.8f, 8.5f); lineTo(20.8f, 18.5f); lineTo(16.6f, 18.5f); close()
    }

    val Wifi = glyph("Wifi") {
        moveTo(3.2f, 8.8f); quadTo(12f, 1.6f, 20.8f, 8.8f)
        moveTo(6.4f, 12.6f); quadTo(12f, 8.2f, 17.6f, 12.6f)
        moveTo(9.4f, 16.2f); quadTo(12f, 14.2f, 14.6f, 16.2f)
        moveTo(12f, 19.6f); lineTo(12.01f, 19.6f)
    }

    /** Secure: a shield with the same tick as [Done] inside it. */
    val Shield = glyph("Shield") {
        moveTo(12f, 3.2f); lineTo(19.6f, 6.2f); lineTo(19.6f, 12f)
        quadTo(19.6f, 17.6f, 12f, 20.8f); quadTo(4.4f, 17.6f, 4.4f, 12f)
        lineTo(4.4f, 6.2f); close()
        moveTo(8.8f, 11.8f); lineTo(11.2f, 14.2f); lineTo(15.4f, 9.4f)
    }

    /** An update waiting: a device with an arrow coming down into it. */
    val Update = glyph("Update") {
        moveTo(6.5f, 3.5f); lineTo(17.5f, 3.5f); lineTo(17.5f, 20.5f); lineTo(6.5f, 20.5f); close()
        moveTo(12f, 7.4f); lineTo(12f, 14.6f)
        moveTo(9f, 11.8f); lineTo(12f, 14.8f); lineTo(15f, 11.8f)
    }

    val Edit = glyph("Edit") {
        moveTo(4.2f, 19.8f); lineTo(4.9f, 15.6f); lineTo(15.9f, 4.6f)
        quadTo(17.2f, 3.3f, 18.5f, 4.6f); lineTo(19.4f, 5.5f)
        quadTo(20.7f, 6.8f, 19.4f, 8.1f); lineTo(8.4f, 19.1f); close()
        moveTo(14.4f, 6.1f); lineTo(17.9f, 9.6f)
    }

    /** Turn the phone. A frame mid-rotation with an arrow following it round. */
    val Rotate = glyph("Rotate") {
        moveTo(9.2f, 3.6f); lineTo(20.4f, 14.8f); lineTo(14.8f, 20.4f); lineTo(3.6f, 9.2f); close()
        moveTo(3.4f, 16.6f); quadTo(3.4f, 20.6f, 7.4f, 20.6f)
        moveTo(5.6f, 18.6f); lineTo(3.4f, 16.6f); lineTo(1.8f, 18.8f)
    }

    val Star = glyph("Star") {
        moveTo(12f, 3.5f); lineTo(14.6f, 9.1f); lineTo(20.5f, 9.9f)
        lineTo(16.2f, 14.1f); lineTo(17.3f, 20.2f); lineTo(12f, 17.3f)
        lineTo(6.7f, 20.2f); lineTo(7.8f, 14.1f); lineTo(3.5f, 9.9f)
        lineTo(9.4f, 9.1f); close()
    }

    val ArrowForward = glyph("ArrowForward") {
        moveTo(4f, 12f); lineTo(20f, 12f)
        moveTo(13.5f, 5.5f); lineTo(20f, 12f); lineTo(13.5f, 18.5f)
    }

    /** A loop with a head, rather than a circle that reads as "loading". */
    val Refresh = glyph("Refresh") {
        moveTo(20f, 12f); arcTo(8f, 8f, 0f, true, true, 17.2f, 6.1f)
        moveTo(17.4f, 2.8f); lineTo(17.4f, 6.6f); lineTo(13.6f, 6.6f)
    }

    val Sound = glyph("Sound") {
        moveTo(4f, 9.5f); lineTo(7.5f, 9.5f); lineTo(12f, 5.5f); lineTo(12f, 18.5f)
        lineTo(7.5f, 14.5f); lineTo(4f, 14.5f); close()
        moveTo(15.5f, 9.5f); quadTo(17.2f, 12f, 15.5f, 14.5f)
        moveTo(18.3f, 7f); quadTo(21.3f, 12f, 18.3f, 17f)
    }

    val Subtitles = glyph("Subtitles") {
        moveTo(3.5f, 5.5f); lineTo(20.5f, 5.5f); lineTo(20.5f, 18.5f); lineTo(3.5f, 18.5f); close()
        moveTo(6.5f, 12f); lineTo(11f, 12f)
        moveTo(13.5f, 12f); lineTo(17.5f, 12f)
        moveTo(6.5f, 15f); lineTo(9f, 15f)
        moveTo(11.5f, 15f); lineTo(17.5f, 15f)
    }

    /** Fill the frame; [Contract] fits it. Two arrows out, two arrows in. */
    val Expand = glyph("Expand") {
        moveTo(4f, 9f); lineTo(4f, 4f); lineTo(9f, 4f)
        moveTo(15f, 4f); lineTo(20f, 4f); lineTo(20f, 9f)
        moveTo(20f, 15f); lineTo(20f, 20f); lineTo(15f, 20f)
        moveTo(9f, 20f); lineTo(4f, 20f); lineTo(4f, 15f)
    }

    val Contract = glyph("Contract") {
        moveTo(9f, 4f); lineTo(9f, 9f); lineTo(4f, 9f)
        moveTo(20f, 9f); lineTo(15f, 9f); lineTo(15f, 4f)
        moveTo(15f, 20f); lineTo(15f, 15f); lineTo(20f, 15f)
        moveTo(4f, 15f); lineTo(9f, 15f); lineTo(9f, 20f)
    }

    val Calendar = glyph("Calendar") {
        moveTo(3.5f, 6.5f); lineTo(20.5f, 6.5f); lineTo(20.5f, 20f); lineTo(3.5f, 20f); close()
        moveTo(3.5f, 10.5f); lineTo(20.5f, 10.5f)
        moveTo(8f, 4f); lineTo(8f, 8f)
        moveTo(16f, 4f); lineTo(16f, 8f)
    }

    val Person = glyph("Person") {
        moveTo(12f, 11.5f); arcTo(3.6f, 3.6f, 0f, true, true, 11.99f, 11.5f); close()
        moveTo(4.8f, 20.5f); quadTo(5.6f, 15f, 12f, 15f); quadTo(18.4f, 15f, 19.2f, 20.5f)
    }

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
