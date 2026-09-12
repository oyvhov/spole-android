package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.theme.*

/** Only display metadata the server supplied; absent quality is not an invented "Auto" badge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackMetadata(details: ContentDetails, facts: List<String>) {
    val preferences = LocalPersonalization.current
    val visibleFacts = facts.filter { preferences.showRatings || !it.startsWith("★") }
    val quality = if (preferences.showQuality) details.quality else emptyList()
    val line = (visibleFacts + quality).distinct()
    // One quiet line, not a wall of plates. Seven raised chips reading "2026 · 51 min · 720p"
    // gave the running time the same weight as the Play button, and how long a film is has never
    // been the reason anyone opened the page.
    val rating = line.firstOrNull { it.startsWith("★") }
    // The rating leads the line, because the star in front of it is what says the number is a
    // rating. Left in place, the star sat before "2025" and pointed at the year; and when the line
    // wrapped, centring floated it between the two lines with nothing beside it at all.
    val ordered = listOfNotNull(rating) + line.filterNot { it == rating }
    if (line.isNotEmpty()) Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).testTag("playback-metadata"),
        verticalAlignment = Alignment.Top,
    ) {
        if (rating != null) Icon(app.reelstack.ui.components.SpoleIcons.Star, stringResource(R.string.tv_rating),
            tint = androidx.compose.ui.graphics.Color(0xFFFFD36D),
            modifier = Modifier.padding(top = 3.dp, end = 5.dp).size(16.dp))
        Text(
            ordered.joinToString("  ·  ") { it.removePrefix("★").trim() },
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
