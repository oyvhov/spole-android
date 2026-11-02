package app.reelstack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelstack.R

/**
 * The block that stands where a picture will be, or never came.
 *
 * It is the chosen mood's own surface. `res/drawable/media_placeholder.xml` is the id every model
 * carries as its fallback, and it is fixed to FOREST's #191C19, so under MIDNIGHT, PLUM or CINEMA it
 * was a greenish block on a blue, purple or black ground. The drawable stays what the models point
 * at (and what the Android TV launcher gets, which no theme reaches); what is drawn for it here
 * is the surface of the theme that is running.
 */
internal fun isMediaPlaceholder(@DrawableRes res: Int) = res == R.drawable.media_placeholder

/**
 * The same 100 x 150 dp rectangle as `media_placeholder.xml`, in the colour asked for. The size is
 * part of the contract: the artwork code relies on the placeholder being portrait when it decides
 * how a picture fades in over it.
 */
internal fun placeholderVector(color: Color): ImageVector =
    ImageVector.Builder(
        name = "media_placeholder",
        defaultWidth = 100.dp, defaultHeight = 150.dp,
        viewportWidth = 100f, viewportHeight = 150f,
    ).path(fill = SolidColor(color)) {
        moveTo(0f, 0f)
        horizontalLineTo(100f)
        verticalLineTo(150f)
        horizontalLineTo(0f)
        close()
    }.build()

/**
 * What to draw for a fallback drawable while a picture loads and after it fails. The placeholder
 * becomes the theme's surface; any other drawable, a demo's own picture, is the resource itself.
 */
@Composable
internal fun placeholderPainter(@DrawableRes res: Int): Painter {
    if (!isMediaPlaceholder(res)) return painterResource(res)
    val surface = MaterialTheme.colorScheme.surface
    return rememberVectorPainter(remember(surface) { placeholderVector(surface) })
}

/**
 * A card that has no picture and was given only the placeholder to show: a demo title, or a title
 * the server has no artwork for. A flat block of the theme's surface, filling the frame. It does
 * not go through the image loader, which would decode the fixed drawable and so bring the FOREST
 * colour back; it reports no picture, so nothing waits on a size or an error from it.
 */
@Composable
internal fun PlaceholderBlock(contentDescription: String?, modifier: Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.surface).then(
            if (contentDescription == null) Modifier
            else Modifier.semantics { this.contentDescription = contentDescription; role = Role.Image },
        ),
    )
}
