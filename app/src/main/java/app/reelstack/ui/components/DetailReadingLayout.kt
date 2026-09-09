package app.reelstack.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** TV art stays anchored while the adjacent reading column scrolls independently. */
@Composable
internal fun DetailReadingLayout(tv: Boolean, scroll: ScrollState,
    artwork: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (tv) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp)) {
            val artworkWidth = minOf(230.dp, maxHeight * .62f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top) {
                Box(Modifier.width(artworkWidth).testTag("tv-detail-artwork")) { artwork() }
                Column(Modifier.weight(1f).fillMaxHeight().testTag("detail-scroll")
                    .verticalScroll(scroll).padding(bottom = 32.dp), content = content)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().testTag("detail-scroll").verticalScroll(scroll)
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp), content = content)
    }
}
