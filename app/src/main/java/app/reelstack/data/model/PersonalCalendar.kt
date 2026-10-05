package app.reelstack.data.model

import java.time.LocalDate

/** A metadata identity, never a server playback id. */
data class CalendarTitle(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val artworkUrl: String? = null,
    val hidden: Boolean = false,
) {
    val key: String get() = "$mediaType:$tmdbId"
}

data class CalendarSelection(val titles: List<CalendarTitle> = emptyList())

val UpcomingMedia.calendarDate: LocalDate
    get() = releaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: java.time.Instant.ofEpochMilli(airDateEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

val UpcomingMedia.calendarIdentity: String
    get() = tmdbId?.let { "$mediaType:$it:${season ?: ""}:${episode ?: ""}" } ?: id

fun calendarHomeItems(items: List<UpcomingMedia>): List<UpcomingMedia> = items
    .sortedWith(compareBy<UpcomingMedia> { it.calendarDate }.thenBy { it.season }.thenBy { it.episode })
    .distinctBy { it.tmdbId?.let { id -> "${it.mediaType}:$id" } ?: it.id }

fun calendarItemsOnDate(items: List<UpcomingMedia>, date: LocalDate): List<UpcomingMedia> =
    items.filter { it.calendarDate == date }.distinctBy { it.calendarIdentity }
