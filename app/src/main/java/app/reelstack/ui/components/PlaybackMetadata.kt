package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val ageRating = visibleFacts.firstNotNullOfOrNull(::ageRatingLabel)
    val descriptiveFacts = visibleFacts.filterNot { ageRatingLabel(it) != null }
    val quality = if (preferences.showQuality) details.quality else emptyList()
    val descriptiveLine = descriptiveFacts.distinct().map(::formatMetadataFact)
    if (ageRating != null || descriptiveLine.isNotEmpty()) Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).testTag("playback-metadata"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ageRating?.let { rating ->
            Text(
                rating,
                color = Text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.background(SurfaceRaised, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp).testTag("age-rating"),
            )
        }
        if (descriptiveLine.isNotEmpty()) Text(
            descriptiveLine.joinToString("  ·  "),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = if (ageRating != null) Modifier.padding(start = 8.dp) else Modifier,
        )
    }
    // Codec and channel layout are useful diagnostics, but should not shout as loudly as title,
    // year and running time. They live on a quieter line below the human-readable metadata.
    if (quality.isNotEmpty()) Text(
        quality.distinct().joinToString("  ·  "),
        color = Muted.copy(alpha = .78f),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("technical-metadata"),
    )
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

/** Jellyfin and Emby return plain age numbers; render them as an actual classification. */
internal fun ageRatingLabel(fact: String): String? = fact.trim().toIntOrNull()
    ?.takeIf { it in 0..18 }
    ?.let { "$it+" }

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
