package app.reelstack.data.model

import androidx.annotation.StringRes
import app.reelstack.R
import app.reelstack.localization.LocalizedText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RequestSeason(
    val number: Int, val name: LocalizedText, val episodes: Int, val status: Int,
    val airDate: LocalDate? = null,
) {
    val canRequest: Boolean get() = status == 1 || status == 7
    val canWatch: Boolean get() = status in 2..4
    @get:StringRes val label: Int get() = when (status) {
        5 -> R.string.season_in_library
        4 -> R.string.season_partly_in_library
        2 -> R.string.season_awaiting_approval
        3 -> R.string.season_requested
        6 -> R.string.season_blocked
        1, 7 -> R.string.season_missing
        else -> R.string.season_unknown
    }

    /**
     * What to say about this season, as a resource plus its arguments.
     *
     * The date used to be formatted here against a hardcoded `nn-NO` locale, so an English
     * installation got Norwegian month names inside an English sentence.
     */
    fun description(today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): LocalizedText = when {
        !canRequest -> LocalizedText(label)
        airDate == null -> LocalizedText(R.string.season_premiere_unknown)
        airDate > today -> LocalizedText(R.string.season_airs_on, airDate.format(seasonDate(locale)))
        else -> LocalizedText(label)
    }
}

/** The reader's own month names, not the ones whoever wrote this code happened to speak. */
internal fun seasonDate(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("d. MMM yyyy", locale)

data class SeriesNextEpisode(val season: Int, val episode: Int, val airDate: LocalDate) {
    fun description(today: LocalDate = LocalDate.now(), locale: Locale = Locale.getDefault()): LocalizedText? =
        if (airDate < today) null else LocalizedText(
            R.string.season_next_episode,
            "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}",
            airDate.format(seasonDate(locale)),
        )
}

data class RequestDownload(val season: Int?, val status: String, val size: Double, val remaining: Double)

enum class RequestStage(@StringRes val label: Int, @StringRes val explanation: Int) {
    REQUESTED(R.string.stage_requested, R.string.stage_requested_why),
    DOWNLOADING(R.string.stage_downloading, R.string.stage_downloading_why),
    IMPORTING(R.string.stage_importing, R.string.stage_importing_why),
    AVAILABLE(R.string.stage_available, R.string.stage_available_why),
    DECLINED(R.string.stage_declined, R.string.stage_declined_why),
    FAILED(R.string.stage_failed, R.string.stage_failed_why),
    UNKNOWN(R.string.stage_unknown, R.string.stage_unknown_why),
    WATCHING(R.string.stage_watching, R.string.stage_watching_why),
    ;

    /** True for the stages that are a request in flight, as opposed to one that only follows. */
    val isRequest: Boolean get() = this != WATCHING
}

data class RequestProgress(val stage: RequestStage, val percent: Int? = null)

/** Approval is not a download, and completed bytes are not library availability. */
fun requestProgress(
    mediaStatus: Int?, seasons: List<RequestSeason>, selected: Set<Int>,
    downloads: List<RequestDownload>, requestStatus: Int? = null,
): RequestProgress {
    val ready = if (selected.isEmpty()) mediaStatus == 5
        else selected.all { number -> seasons.any { it.number == number && it.status == 5 } }
    if (ready) return RequestProgress(RequestStage.AVAILABLE)
    if (requestStatus == 3 || mediaStatus == 6) return RequestProgress(RequestStage.DECLINED)
    if (requestStatus == 4) return RequestProgress(RequestStage.FAILED)
    // An unrelated season must not make this request appear to be downloading.
    val matching = downloads.filter { selected.isEmpty() || it.season in selected }
    if (matching.any { it.status.lowercase() in setOf("failed", "warning", "error") }) return RequestProgress(RequestStage.FAILED)
    if (matching.isNotEmpty()) {
        if (matching.all { it.status.equals("completed", true) || (it.size > 0 && it.remaining <= 0) }) {
            return RequestProgress(RequestStage.IMPORTING)
        }
        val total = matching.sumOf { it.size.coerceAtLeast(0.0) }
        val left = matching.sumOf { it.remaining.coerceAtLeast(0.0) }
        return RequestProgress(RequestStage.DOWNLOADING,
            if (total > 0) ((1 - left / total) * 100).toInt().coerceIn(0, 99) else null)
    }
    return RequestProgress(RequestStage.REQUESTED)
}

data class TrackedRequest(
    val key: String,
    val mediaId: Int,
    val mediaType: String,
    val title: String,
    val artworkUrl: String?,
    val seasons: Set<Int>,
    val notify: Boolean = true,
    val stage: RequestStage = RequestStage.REQUESTED,
    val percent: Int? = null,
    val notified: Boolean = false,
    val updatedAt: Long = 0,
    val checkedAt: Long = 0,
    val is4k: Boolean = false,
    val availableSeasons: Set<Int> = emptySet(),
    /** Seerr's own request id, needed to withdraw it. Null for follows imported before 0.13. */
    val requestId: Int? = null,
    /**
     * Stages already announced. Per stage, because one `notified` flag meant a "ready" alert also
     * silenced the later "this stopped" alert for the same title.
     */
    val notifiedStages: Set<String> = emptySet(),
    /** A local availability alert is not a Seerr request and must never acquire its delete action. */
    val availabilityOnly: Boolean = false,
    /** Keep a ready-only bell's promise even if its watch later merges into an owned request. */
    val readyNotificationOnly: Boolean = false,
    val statusCheckFailed: Boolean = false,
)

/** Older imported records used these type labels before Seerr supplied a real title. */
val TrackedRequest.hasTitleMetadata: Boolean
    get() = !artworkUrl.isNullOrBlank() || title.trim().lowercase(java.util.Locale.ROOT) !in
        setOf("", "film", "serie", "movie", "series", "tv")

fun availabilityWatchProgress(
    seasons: List<RequestSeason>, selected: Set<Int>, downloads: List<RequestDownload>, mediaStatus: Int?,
): RequestProgress {
    if (mediaStatus == 6 || selected.isEmpty() || selected.any { number -> seasons.none { it.number == number } })
        return RequestProgress(RequestStage.UNKNOWN)
    val progress = requestProgress(mediaStatus, seasons, selected, downloads)
    return if (progress.stage == RequestStage.REQUESTED) RequestProgress(RequestStage.WATCHING) else progress
}

data class RequestDraft(
    val media: DiscoverMedia,
    val seasons: List<RequestSeason> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val notify: Boolean = true,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val mediaStatus: Int? = null,
    val nextEpisode: SeriesNextEpisode? = null,
    val watchedSeasons: Set<Int> = emptySet(),
    val savingWatch: Int? = null,
    val watchError: String? = null,
    val rules: RequestRules? = null,
)
