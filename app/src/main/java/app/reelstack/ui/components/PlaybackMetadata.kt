package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
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
    if (line.isNotEmpty()) Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).testTag("playback-metadata"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rating = line.firstOrNull { it.startsWith("★") }
        if (rating != null) Icon(Icons.Rounded.Star, stringResource(R.string.tv_rating),
            tint = androidx.compose.ui.graphics.Color(0xFFFFD36D), modifier = Modifier.size(16.dp).padding(end = 5.dp))
        Text(
            line.joinToString("  ·  ") { it.removePrefix("★").trim() },
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}
