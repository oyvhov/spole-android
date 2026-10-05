package app.reelstack.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class CalendarFilter {
    ALL, MOVIES, EPISODES;

    val labelRes: Int get() = when (this) {
        ALL -> R.string.calendar_all
        MOVIES -> R.string.calendar_movies
        EPISODES -> R.string.calendar_episodes
    }
}

@Composable
internal fun UpcomingCalendarSheet(
    items: List<UpcomingMedia>, onUpcomingClick: (String) -> Unit, onDismiss: () -> Unit,
    undated: List<CalendarTitle> = emptyList(), error: String? = null, loading: Boolean = false,
    onTitleClick: (CalendarTitle) -> Unit = {},
) {
    val today = LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    val density = LocalDensity.current
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE d. MMMM", locale) }
    val shortFormat = remember(locale) { DateTimeFormatter.ofPattern("d. MMM", locale) }
    val weekFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    var filter by rememberSaveable { mutableStateOf(CalendarFilter.ALL) }
    var selectedDay by rememberSaveable { mutableStateOf<String?>(null) }
    var lastOpened by rememberSaveable { mutableStateOf<String?>(null) }
    val television = LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val open: (String) -> Unit = { id -> lastOpened = id; onUpcomingClick(id) }
    val agendaState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val resetScroll: () -> Unit = { scope.launch { agendaState.scrollToItem(0) }; Unit }
    val labels = CalendarFilter.entries.associateWith { stringResource(it.labelRes) }
    val grouped = remember(items, filter, today) {
        items.filter { it.calendarDate in today..today.plusDays(28) && when (filter) {
            CalendarFilter.ALL -> true
            CalendarFilter.MOVIES -> it.isMovieRelease
            CalendarFilter.EPISODES -> !it.isMovieRelease
        } }.distinctBy { it.calendarIdentity }.sortedWith(compareBy<UpcomingMedia> { it.calendarDate }
            .thenBy { it.title }.thenBy { it.season }.thenBy { it.episode }).groupBy { it.calendarDate }
    }
    val shown = grouped.filterKeys { selectedDay == null || it.toString() == selectedDay }
    val pending = undated.filter { when (filter) {
        CalendarFilter.ALL -> true
        CalendarFilter.MOVIES -> it.mediaType == "movie"
        CalendarFilter.EPISODES -> it.mediaType == "tv"
    } }.distinctBy { it.key }
    Column(Modifier.fillMaxSize().testTag("calendar")) {
        SheetToolbar(stringResource(R.string.calendar_title), stringResource(R.string.calendar_close), onDismiss)
        LazyColumn(state = agendaState, modifier = Modifier.weight(1f).testTag("calendar-agenda"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "calendar-controls") {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.calendar_personal), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            Text(stringResource(R.string.calendar_range, today.format(shortFormat), today.plusDays(28).format(shortFormat)),
                fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            AppFilterRow(CalendarFilter.entries, filter, { labels.getValue(it) }, { filter = it; lastOpened = null; resetScroll() },
                Modifier.padding(vertical = 10.dp))
        }
        }
        item(key = "calendar-dates") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("calendar-dates")) {
            items(29) { offset ->
                val date = today.plusDays(offset.toLong())
                val count = grouped[date]?.size ?: 0
                val chosen = selectedDay == date.toString()
                val description = stringResource(R.string.calendar_day_description, date.format(dateFormat),
                    pluralStringResource(R.plurals.calendar_releases, count, count))
                val interaction = remember { MutableInteractionSource() }
                val shape = RoundedCornerShape(16.dp)
                Surface(onClick = { selectedDay = if (chosen) null else date.toString(); lastOpened = null; resetScroll() },
                    interactionSource = interaction, shape = shape, color = if (chosen) MaterialTheme.colorScheme.surfaceContainerHigh else SurfaceRaised,
                    contentColor = app.reelstack.ui.theme.Text,
                    border = if (chosen || offset == 0) BorderStroke(1.dp, ControlOutline) else null,
                    modifier = Modifier.width(with(density) { 44.sp.toDp() }.coerceAtLeast(60.dp) + 24.dp)
                        .focusOutline(interaction, shape, glow = false)
                        .testTag("calendar-day-$offset").semantics { selected = chosen; contentDescription = description }) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp).widthIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (date.dayOfMonth == 1) date.format(DateTimeFormatter.ofPattern("MMM", locale)).removeSuffix(".")
                            else date.format(weekFormat).removeSuffix("."), color = Muted, fontSize = 12.sp, lineHeight = 20.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                                .heightIn(min = with(density) { 20.sp.toDp() } + 2.dp))
                        Text(date.dayOfMonth.toString(), fontSize = 22.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                                .heightIn(min = with(density) { 33.sp.toDp() } + 2.dp))
                        Box(Modifier.padding(top = 7.dp).height(3.dp).width(18.dp)
                            .background(if (chosen) Primary else if (count > 0) Muted else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }
        }
        item(key = "calendar-count") {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val count = shown.values.sumOf { it.size }
            Text(pluralStringResource(R.plurals.calendar_releases, count, count), color = Muted, fontSize = 12.sp,
                lineHeight = 18.sp, modifier = Modifier.weight(1f))
            if (selectedDay != null) TextButton(onClick = { selectedDay = null; resetScroll() }) {
                Text(stringResource(R.string.calendar_all_days))
            }
            if (loading) CircularProgressIndicator(Modifier.padding(start = 10.dp).size(16.dp), color = Muted, strokeWidth = 2.dp)
        }
        }
            error?.let { message -> item(key = "calendar-error") { Text(message, color = Muted, fontSize = 13.sp, lineHeight = 19.sp) } }
            if (shown.isEmpty()) item(key = "calendar-empty") {
                Column(Modifier.padding(vertical = 20.dp)) {
                    Text(stringResource(R.string.calendar_empty), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.calendar_empty_hint), color = Muted, fontSize = 13.sp, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
            shown.forEach { (date, events) ->
                item(key = "date-$date") {
                    Text(when (date) {
                        today -> stringResource(R.string.calendar_today)
                        today.plusDays(1) -> stringResource(R.string.calendar_tomorrow)
                        else -> date.format(dateFormat).replaceFirstChar { it.titlecase(locale) }
                    }, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
                }
                val batches = events.groupBy { it.tmdbId?.let { id -> "${it.mediaType}:$id:${it.season}" } ?: it.id }.values.toList()
                items(batches, key = { "$date:${it.first().calendarIdentity}" }) { batch -> CalendarEntry(batch, open,
                    restoreFocus = television && batch.any { it.id == lastOpened }) }
            }
            if (pending.isNotEmpty()) {
                item(key = "calendar-pending-heading") {
                    Text(stringResource(R.string.calendar_no_dates), fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp))
                    Text(stringResource(R.string.calendar_no_dates_hint), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
                items(pending, key = { "pending:${it.key}" }) { title ->
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(16.dp)
                    Surface(onClick = { onTitleClick(title) }, interactionSource = interaction, shape = shape, color = SurfaceRaised,
                        contentColor = app.reelstack.ui.theme.Text,
                        modifier = Modifier.fillMaxWidth().focusOutline(interaction, shape, false).testTag("calendar-following-${title.key}")) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(SpoleIcons.Calendar, null, Modifier.size(22.dp), tint = Muted)
                            Text(title.title, modifier = Modifier.padding(start = 14.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarEntry(batch: List<UpcomingMedia>, onOpen: (String) -> Unit, restoreFocus: Boolean = false) {
    val media = batch.first()
    var expanded by rememberSaveable(media.calendarIdentity) { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    val focus = remember { FocusRequester() }
    LaunchedEffect(restoreFocus) { if (restoreFocus) { withFrameNanos { }; focus.requestFocus() } }
    Column {
        Surface(onClick = { onOpen(media.id) }, interactionSource = interaction, color = SurfaceRaised, shape = shape,
            contentColor = app.reelstack.ui.theme.Text,
            modifier = Modifier.fillMaxWidth().focusRequester(focus).focusOutline(interaction, shape, false).testTag("calendar-entry-${media.id}")) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(url = media.artworkUrl, fallbackRes = media.artworkRes, contentDescription = null,
                    modifier = Modifier.width(if (media.isMovieRelease) 58.dp else 90.dp)
                        .aspectRatio(if (media.isMovieRelease) 2f / 3f else 16f / 9f).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(media.title, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (batch.size > 1) pluralStringResource(R.plurals.calendar_batch, batch.size, batch.size) else media.subtitle,
                        color = Muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
                    Text(stringResource(if (media.isMovieRelease) R.string.calendar_home_release else R.string.calendar_episode_premiere),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        if (batch.size > 1) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("calendar-batch-${media.id}")) {
                Text(stringResource(if (expanded) R.string.calendar_hide_episodes else R.string.calendar_show_episodes))
            }
            if (expanded) batch.forEach { episode ->
                TextButton(onClick = { onOpen(episode.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(episode.subtitle, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
