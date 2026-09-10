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
    if (visibleFacts.isNotEmpty() || quality.isNotEmpty()) FlowRow(
        Modifier.fillMaxWidth().padding(top = 16.dp).testTag("playback-metadata"),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (visibleFacts + quality).distinct().forEach { fact ->
            val rating = fact.startsWith("★")
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceRaised)
                .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (rating) Icon(Icons.Rounded.Star, stringResource(R.string.tv_rating),
                    tint = androidx.compose.ui.graphics.Color(0xFFFFD36D), modifier = Modifier.size(18.dp).padding(end = 3.dp))
                Text(fact.removePrefix("★").trim(), color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    val progress = details.progress?.coerceIn(0f, 1f) ?: 0f
    if (progress > 0) Column(Modifier.fillMaxWidth().padding(top = 18.dp).testTag("detail-progress")) {
        Text(if (details.remainingMinutes != null) stringResource(R.string.tv_progress, (progress * 100).toInt(), "${details.remainingMinutes} min")
            else stringResource(R.string.tv_progress_percent, (progress * 100).toInt()),
            color = Muted, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp),
            color = Primary, trackColor = SurfaceRaised)
    }
}
