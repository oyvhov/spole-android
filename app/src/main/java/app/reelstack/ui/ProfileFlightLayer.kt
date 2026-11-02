package app.reelstack.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.landSpec
import coil3.compose.AsyncImage

/**
 * The picked picture on its way from the menu to the corner circle.
 *
 * Drawn above both shells in the activity, so it survives the switch that rebuilds the page
 * underneath. Only scale and translation move; the picture is laid out once at the size it had
 * in the menu and shrinks towards the circle from its top-left corner, so at the end it covers
 * exactly the circle's bounds. The empty layer also tells the motion where the root sits on the
 * screen, which is how a pick in the menu's own window finds its place here.
 */
@Composable
fun ProfileFlightLayer(motion: ProfileSwitchMotion, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().onGloballyPositioned { motion.rootOnScreen = it.positionOnScreen() }) {
        val flight = motion.flight ?: return@Box
        val progress = remember(flight) { Animatable(0f) }
        LaunchedEffect(flight) { progress.animateTo(1f, landSpec(motion = true)) }
        val density = LocalDensity.current
        val from = flight.from
        val to = flight.to
        Box(
            Modifier
                .size(with(density) { from.width.toDp() }, with(density) { from.height.toDp() })
                .graphicsLayer {
                    val p = progress.value
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = from.left + (to.left - from.left) * p
                    translationY = from.top + (to.top - from.top) * p
                    val scale = 1f + (to.width / from.width - 1f) * p
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(SurfaceRaised)
                // The tag before the clearing, which drops every semantic declared after it.
                .testTag("profile-flight")
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            if (!flight.picture.avatarUrl.isNullOrBlank()) {
                AsyncImage(model = flight.picture.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else if (flight.picture.letter != null) {
                Text(flight.picture.letter, color = Primary, fontSize = 22.sp, lineHeight = 26.sp)
            }
        }
    }
}
