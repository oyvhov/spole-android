package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            RottenTomatoesRating(it, Modifier.testTag("critic-rating"))
        }
        score?.let {
            TmdbRating(it, Modifier.testTag("tmdb-rating"))
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
