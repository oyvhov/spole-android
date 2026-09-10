package app.reelstack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.reelstack.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.model.isMovieRelease
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.AppFilterRow
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Stable filter identities; the visible label is a property, not the state. */
enum class CalendarFilter(val labelRes: Int) {
    ALL(R.string.calendar_all),
    MOVIES(R.string.calendar_movies),
    EPISODES(R.string.calendar_episodes),
}

@Composable
internal fun UpcomingCalendarSheet(
    items: List<UpcomingMedia>,
    onUpcomingClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0]
    val filterLabels = CalendarFilter.entries.associateWith { stringResource(it.labelRes) }
    val compactDate = remember(locale) { DateTimeFormatter.ofPattern(
        android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd"), locale) }
    val fullDate = remember(locale) { DateTimeFormatter.ofPattern(
        android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMd"), locale) }
    val agendaDate = remember(locale) { DateTimeFormatter.ofPattern(
        android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale) }
    var filter by rememberSaveable { mutableStateOf(CalendarFilter.ALL) }
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }
    val grouped = remember(items, filter, today, zone) {
        items.filter { when (filter) {
            CalendarFilter.ALL -> true
            CalendarFilter.MOVIES -> it.isMovieRelease
            CalendarFilter.EPISODES -> !it.isMovieRelease
        } }
            .sortedBy { it.airDateEpochMillis }
            .groupBy { Instant.ofEpochMilli(it.airDateEpochMillis).atZone(zone).toLocalDate() }
            .filterKeys { !it.isBefore(today) && it.isBefore(today.plusDays(28)) }
    }
    val shown = grouped.filterKeys { selectedDay == null || it.toString() == selectedDay }
    val agendaState = rememberLazyListState()
    LaunchedEffect(filter, selectedDay) { agendaState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().testTag("calendar")) {
        SheetToolbar(stringResource(R.string.calendar_title), stringResource(R.string.calendar_close), onDismiss)
        Text(stringResource(R.string.calendar_subtitle), color = Muted, fontSize = 13.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp))
        AppFilterRow(CalendarFilter.entries, filter, { filterLabels.getValue(it) }, { filter = it },
            Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Text(
            stringResource(R.string.calendar_range, today.format(compactDate), today.plusDays(27).format(compactDate)),
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("calendar-dates"),
        ) {
            items(28) { offset ->
                val date = today.plusDays(offset.toLong())
                val count = grouped[date]?.size ?: 0
                val isSelected = selectedDay == date.toString()
                val isToday = date == today
                val dateDescription = if (isToday) stringResource(R.string.calendar_today_date, date.format(fullDate)) else date.format(fullDate)
                val releaseDescription = pluralStringResource(R.plurals.calendar_releases, count, count)
                val dayDescription = stringResource(R.string.calendar_day_description, dateDescription, releaseDescription)
                // The strip crosses a month boundary, so the first day of a new month says which
                // month it belongs to instead of reading as another day of the current one.
                val newMonth = offset > 0 && date.dayOfMonth == 1
                Surface(
                    onClick = { selectedDay = if (isSelected) null else date.toString() },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Primary else SurfaceRaised,
                    contentColor = if (isSelected) Ink else MaterialTheme.colorScheme.onSurface,
                    // Today keeps a visible ring when it is not the selected day, so "now" is
                    // always locatable in the strip.
                    border = if (isToday && !isSelected) BorderStroke(1.5.dp, Primary) else null,
                    modifier = Modifier.width(IntrinsicSize.Max).widthIn(min = 52.dp).testTag("calendar-day-$offset")
                        .semantics {
                            selected = isSelected
                            contentDescription = dayDescription
                        },
                ) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp).widthIn(min = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (newMonth) date.format(DateTimeFormatter.ofPattern("MMM", locale)).removeSuffix(".")
                            else date.format(DateTimeFormatter.ofPattern("EEE", locale)).removeSuffix("."),
                            fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (newMonth) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(date.dayOfMonth.toString(), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(if (count > 0) count.toString() else "–", fontSize = 10.sp, lineHeight = 14.sp,
                            color = if (isSelected) Ink else Muted,
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (selectedDay == null) grouped.values.sumOf { it.size }.let { pluralStringResource(R.plurals.calendar_releases, it, it) }
                else shown.values.sumOf { it.size }.let { pluralStringResource(R.plurals.calendar_day_releases, it, it) },
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f),
            )
            // A reset control that looks like the label beside it is not findable, so it becomes
            // a chip once there is actually something to reset.
            if (selectedDay != null) {
                Surface(
                    onClick = { selectedDay = null },
                    shape = CircleShape,
                    color = SurfaceRaised,
                    contentColor = PrimarySoft,
                    border = BorderStroke(1.dp, ControlOutline),
                ) {
                    Text(stringResource(R.string.calendar_all_days), fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 11.dp))
                }
            }
        }
        // Only visible rows are composed, even for large calendars.
        LazyColumn(
            state = agendaState,
            modifier = Modifier.weight(1f).testTag("calendar-agenda"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            if (shown.isEmpty()) {
                item {
                    Text(stringResource(R.string.calendar_empty), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 28.dp))
                    Text(stringResource(R.string.calendar_empty_hint), color = Muted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            shown.forEach { (date, dayItems) ->
                item(key = "date-$date") {
                    Text(
                        when (date) {
                            today -> stringResource(R.string.calendar_today)
                            today.plusDays(1) -> stringResource(R.string.calendar_tomorrow)
                            else -> date.format(agendaDate)
                                .replaceFirstChar { it.titlecase(locale) }
                        },
                        fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(dayItems, key = { it.id }) { media -> CalendarEntry(media, onUpcomingClick) }
            }
        }
    }
}

@Composable
private fun CalendarEntry(media: UpcomingMedia, onOpen: (String) -> Unit) {
    val isMovie = media.isMovieRelease
    val context = LocalContext.current
    val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(media.airDateEpochMillis))
    Surface(onClick = { onOpen(media.id) }, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(88.dp), contentAlignment = Alignment.CenterStart) {
                MediaArtwork(
                    url = media.artworkUrl, fallbackRes = media.artworkRes, contentDescription = null,
                    contentScale = if (isMovie) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.size(if (isMovie) 60.dp else 88.dp, if (isMovie) 90.dp else 54.dp)
                        .clip(RoundedCornerShape(10.dp)).background(SurfaceRaised),
                )
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(media.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(media.subtitle.replace(" · TBA", ""), color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(if (isMovie) Icons.Rounded.Movie else Icons.Rounded.Tv, contentDescription = null, tint = PrimarySoft, modifier = Modifier.size(12.dp))
                    Text(
                        if (!isMovie) if (media.source == ServiceKind.SONARR) stringResource(R.string.calendar_source_time, media.source.displayName, time) else media.source.displayName
                        else if ("Fysisk utgjeving" in media.facts) stringResource(R.string.calendar_physical) else stringResource(R.string.calendar_home_release),
                        color = PrimarySoft, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }
    }
}
