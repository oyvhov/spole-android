package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Only display metadata the server supplied; absent quality is not an invented "Auto" badge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackMetadata(details: ContentDetails, facts: List<String>) {
    val preferences = LocalPersonalization.current
    // CommunityRating is already mapped to tmdbRating when the server exposes it. Keeping the
    // old star fact here made the same score appear once unnamed and once as TMDB.
    val visibleFacts = facts.filterNot { it.startsWith("★") }
    val quality = if (preferences.showQuality) details.quality else emptyList()
    val line = (visibleFacts + quality).distinct().map { formatMetadataFact(it) }
    if (line.isNotEmpty()) Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).testTag("playback-metadata"),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            line.joinToString("  ·  "),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
    val score = if (preferences.showRatings) details.tmdbRating else null
    val critic = if (preferences.showRatings) details.criticRating else null
    val mdblist = if (preferences.showRatings) details.mdblistRating else null
    if (score != null || critic != null || mdblist != null) Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()).testTag("metadata-ratings"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        critic?.let {
            Row(Modifier.testTag("critic-rating").clearAndSetSemantics { contentDescription = "Rotten Tomatoes $it%" },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_tomato), null, Modifier.size(22.dp),
                    tint = androidx.compose.ui.graphics.Color.Unspecified)
                Text("$it%", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        score?.let {
            Row(Modifier.testTag("tmdb-rating"), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(width = 42.dp, height = 20.dp).clip(RoundedCornerShape(3.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF01B4E4)), contentAlignment = Alignment.Center) {
                    Text("TMDB", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Text("${"%.1f".format(Locale.ROOT, it / 10f)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        mdblist?.let {
            Text("MDBList ${"%.1f".format(Locale.ROOT, it / 10f)}", color = Muted,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("mdblist-rating"))
        }
    }
}

private val finishTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

private fun formatMetadataFact(fact: String): String {
    val minutes = Regex("^(\\d+) min$").matchEntire(fact)?.groupValues?.get(1)?.toIntOrNull() ?: return fact
    val hours = minutes / 60
    val remainder = minutes % 60
    val duration = when {
        hours == 0 -> "${minutes}m"
        remainder == 0 -> "${hours}t"
        else -> "${hours}t ${remainder}m"
    }
    val finish = LocalTime.now().plusMinutes(minutes.toLong()).format(finishTimeFormatter)
    return "$duration · ferdig $finish"
}
