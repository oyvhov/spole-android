package app.reelstack.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalMediaEdgeToEdge = staticCompositionLocalOf { false }

val LocalTabletCanvas = staticCompositionLocalOf { false }

/** Shared rhythm and artwork dimensions, including loading placeholders. */
object ReelLayout {
    val Gutter = 24.dp
    val PageTop = 32.dp
    val ArtworkCorner: androidx.compose.ui.unit.Dp @Composable get() = LocalPersonalization.current.artworkCorners.radius.dp
    val PosterWidth = 132.dp
    val PosterHeight = 198.dp
    val EpisodeWidth = 248.dp
    val EpisodeHeight = 139.5.dp

    /**
     * One readable column on every screen size. Without this a wide window stretches each
     * `weight(1f)` header until its trailing label loses contact with the title it belongs to.
     */
    val ContentMaxWidth = 840.dp

    /** At or above this window width navigation moves from the bottom bar to a side rail. */
    val RailBreakpoint = 640.dp
}

/**
 * Media pages fill the available window. Reading pages retain their centered readable column.
 * Horizontal media rails may bleed to the end edge while headers keep their own inset.
 */
@Composable
fun ReelPage(modifier: Modifier = Modifier, media: Boolean = false, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val policy = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value)
        val maximum = if (media) policy.mediaMaxWidthDp.dp else ReelLayout.ContentMaxWidth
        val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
            android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val tablet = maxWidth >= 900.dp || (television && maxWidth >= 640.dp)
        Box(Modifier.widthIn(max = maximum).fillMaxSize()) {
            CompositionLocalProvider(LocalTabletCanvas provides tablet,
                LocalMediaEdgeToEdge provides (media && policy.useNavigationRail)) { content() }
        }
    }
}
