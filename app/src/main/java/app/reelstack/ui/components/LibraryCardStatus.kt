package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.LibraryDisplay
import app.reelstack.data.network.RemoteLibraryItem

/** Neutral status belongs in one quiet corner, separate from ratings and playback progress. */
@Composable
internal fun LibraryCardStatus(item: RemoteLibraryItem, display: LibraryDisplay, modifier: Modifier = Modifier) =
    LibraryCardStatus(item.id, item.mediaType, item.played, item.unplayedItemCount, display, modifier)

@Composable
internal fun LibraryCardStatus(id: String, mediaType: String, played: Boolean, unplayedItemCount: Int?,
    display: LibraryDisplay, modifier: Modifier = Modifier) {
    val remaining = unplayedItemCount?.takeIf { it > 0 && mediaType in setOf("Series", "Season") }
    val watched = display.showWatched && played
    val count = remaining?.takeIf { display.showUnwatchedCount && !played }
    if (!watched && count == null) return
    val description = if (watched) stringResource(R.string.library_watched_description)
        else pluralStringResource(R.plurals.library_unwatched_description, count!!, count)
    Row(modifier.testTag("library-${if (watched) "watched" else "unwatched"}-$id")
        .background(Color(0xDD151817), RoundedCornerShape(8.dp))
        .clearAndSetSemantics { contentDescription = description }
        .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(if (watched) SpoleIcons.Done else SpoleIcons.Library, null, Modifier.size(16.dp), tint = Color.White)
        count?.let { Text(it.toString(), color = Color.White, style = MaterialTheme.typography.labelMedium) }
    }
}
