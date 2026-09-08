package app.reelstack.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared rhythm and artwork dimensions, including loading placeholders. */
object ReelLayout {
    val Gutter = 24.dp
    val PageTop = 32.dp
    val ArtworkCorner = 12.dp
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
 * Centres a page inside [ReelLayout.ContentMaxWidth]. Screens pass their scrolling container as
 * [content] so the column, not the window, decides how wide a row is allowed to grow.
 */
@Composable
fun ReelPage(modifier: Modifier = Modifier, media: Boolean = false, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val policy = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value)
        val maximum = if (media) policy.mediaMaxWidthDp.dp else ReelLayout.ContentMaxWidth
        Box(Modifier.widthIn(max = maximum).fillMaxSize()) {
            content()
        }
    }
}
