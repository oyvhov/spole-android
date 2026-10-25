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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Only display metadata the server supplied; absent quality is not an invented "Auto" badge. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackMetadata(details: ContentDetails, facts: List<String>, source: ServiceKind? = null,
    centered: Boolean = false) {
    val preferences = LocalPersonalization.current
    // CommunityRating is already mapped to tmdbRating when the server exposes it. Keeping the
    // old star fact here made the same score appear once unnamed and once as TMDB.
    val visibleFacts = facts.filterNot { it.startsWith("★") }
    val ageRating = visibleFacts.firstNotNullOfOrNull(::ageRatingLabel)
    val descriptiveFacts = visibleFacts.filterNot { ageRatingLabel(it) != null }
    val quality = if (preferences.showMediaInfo) details.quality else emptyList()
    val context = LocalContext.current
    // «ferdig 21:25» only means something for a title that can be started here.
    val playable = details.source != ServiceKind.SEERR || details.libraryAvailable
    val descriptiveLine = descriptiveFacts.distinct().map { formatMetadataFact(it, withFinish = playable).text(context) }
    if (ageRating != null || descriptiveLine.isNotEmpty() || source != null) FlowRow(
        Modifier.fillMaxWidth().testTag("playback-metadata"),
        horizontalArrangement = Arrangement.spacedBy(12.dp, if (centered) Alignment.CenterHorizontally else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ageRating?.let { rating ->
            Text(
                rating,
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("age-rating"),
            )
        }
        if (descriptiveLine.isNotEmpty()) Text(
            descriptiveLine.joinToString("  ·  "),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        )
        source?.let {
            Text(it.displayName, color = Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("detail-source"))
        }
    }
    // Codec and channel layout are useful diagnostics, but should not shout as loudly as title,
    // year and running time. They live on a quieter line below the human-readable metadata.
    if (quality.isNotEmpty()) Text(
        quality.distinct().joinToString("  ·  "),
        color = Muted.copy(alpha = .78f),
        style = MaterialTheme.typography.bodySmall,
        textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("technical-metadata"),
    )
    val score = details.tmdbRating
    val critic = details.criticRating
    val mdblist = details.mdblistRating
    if (score != null || critic != null || mdblist != null) FlowRow(
        Modifier.fillMaxWidth().padding(top = 6.dp).testTag("metadata-ratings"),
        horizontalArrangement = Arrangement.spacedBy(20.dp, if (centered) Alignment.CenterHorizontally else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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

/** A running time, with when it would end if started now; [withFinish] off for a title that cannot be played here. */
internal fun formatMetadataFact(fact: String, now: LocalTime = LocalTime.now(), withFinish: Boolean = true): app.reelstack.localization.LocalizedText {
    val minutes = Regex("^(\\d+) min$").matchEntire(fact)?.groupValues?.get(1)?.toIntOrNull()
        ?.takeIf { it > 0 } ?: return app.reelstack.localization.LocalizedText.raw(fact)
    val hours = minutes / 60
    val remainder = minutes % 60
    if (!withFinish) return when {
        hours == 0 -> app.reelstack.localization.LocalizedText(R.string.metadata_length_minutes, minutes)
        remainder == 0 -> app.reelstack.localization.LocalizedText(R.string.metadata_length_hours, hours)
        else -> app.reelstack.localization.LocalizedText(R.string.metadata_length_hours_minutes, hours, remainder)
    }
    val finish = now.plusMinutes(minutes.toLong()).format(finishTimeFormatter)
    return when {
        hours == 0 -> app.reelstack.localization.LocalizedText(R.string.metadata_runtime_minutes, minutes, finish)
        remainder == 0 -> app.reelstack.localization.LocalizedText(R.string.metadata_runtime_hours, hours, finish)
        else -> app.reelstack.localization.LocalizedText(R.string.metadata_runtime_hours_minutes, hours, remainder, finish)
    }
}
