package app.reelstack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.PersonPage
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.focusBleed
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.isTelevision
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.screens.PosterOnlyCard
import app.reelstack.ui.screens.departmentLabel
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

/** One page per person: picture, a little about them, what you have and what else they made. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PersonSheet(page: PersonPage?, onTitle: (String) -> Unit) {
    page ?: return
    val tv = isTelevision()
    var expanded by remember(page.person.id) { mutableStateOf(false) }
    // On a television the heading of the row in focus names the poster the remote is on.
    var focusedTitle by remember(page.person.id) { mutableStateOf<Pair<Int, String>?>(null) }
    val width = (if (tv) 120.dp else 104.dp) * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val firstPoster = remember { FocusRequester() }
    val firstId = (page.inLibrary.firstOrNull() ?: page.more.firstOrNull())?.id
    // Without titles the remote starts on «Les meir»; a page with nothing to focus would ignore it.
    val startOnBiography = firstId == null && page.biography != null && !expanded
    val list = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(tv, firstId, startOnBiography) {
        if (!tv || (firstId == null && !startOnBiography)) return@LaunchedEffect
        repeat(40) {
            withFrameNanos { }
            if (runCatching { firstPoster.requestFocus() }.isSuccess) {
                // Bringing the poster into view scrolled the picture and the name off the top.
                list.scrollToItem(0)
                return@LaunchedEffect
            }
        }
    }
    val born = page.birthday?.let { birthday(it) }
    // A television centres the focused row by default, which scrolled the picture and the name off
    // the top. This page only scrolls as far as the focused poster needs, as the title page does.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides
        app.reelstack.ui.components.DetailBringIntoView) {
    LazyColumn(Modifier.fillMaxSize().testTag("person-sheet"), state = list, verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(vertical = if (tv) 40.dp else 16.dp)) {
        item(key = "head") {
            Row(Modifier.padding(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (tv) 168.dp else 104.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                    page.person.portraitUrl?.let {
                        MediaArtwork(it, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(page.person.name, color = TextColor,
                        style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall)
                    val line = listOfNotNull(departmentLabel(page.department), born?.let { stringResource(R.string.person_born, it) })
                    if (line.isNotEmpty()) Text(line.joinToString("  ·  "), color = Muted, style = MaterialTheme.typography.bodyMedium)
                    page.biography?.let { bio ->
                        Text(bio, color = Muted, style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                        // A toggle, as the title page's synopsis is: a button that went away with the press left
                        // the remote on nothing.
                        run {
                            val interaction = remember { MutableInteractionSource() }
                            Text(stringResource(if (expanded) R.string.details_less else R.string.person_read_more), color = TextColor, style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.heightIn(min = 44.dp).then(if (startOnBiography) Modifier.focusRequester(firstPoster) else Modifier)
                                    .focusBleed(14.dp).focusOutline(interaction, CircleShape)
                                    .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button) { expanded = !expanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp).testTag("person-read-more"))
                        }
                    }
                }
            }
        }
        if (page.loading && page.inLibrary.isEmpty() && page.more.isEmpty()) item(key = "loading") {
            LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false) {
                items(6) {
                    Box(Modifier.width(width).aspectRatio(2f / 3f).clip(RoundedCornerShape(ReelLayout.ArtworkCorner)).background(SurfaceRaised))
                }
            }
        }
        if (page.failed) item(key = "failed") {
            Text(stringResource(R.string.person_failed), color = Muted, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = ReelLayout.Gutter).testTag("person-failed"))
        }
        listOf(R.string.person_in_library to page.inLibrary, R.string.person_more_with to page.more).forEach { (label, titles) ->
            if (titles.isEmpty()) return@forEach
            item(key = "row-$label") {
                Column(Modifier.testTag(if (label == R.string.person_in_library) "person-in-library" else "person-more"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.padding(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (label == R.string.person_more_with) stringResource(label, page.person.name) else stringResource(label),
                            color = TextColor, style = MaterialTheme.typography.titleLarge)
                        if (tv) focusedTitle?.takeIf { it.first == label }?.let {
                            Text(it.second, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    LazyRow(contentPadding = PaddingValues(horizontal = ReelLayout.Gutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(titles, key = { "$label-${it.id}" }) { media ->
                            PosterOnlyCard(media, { onTitle(media.id) },
                                Modifier.width(width).then(if (media.id == firstId) Modifier.focusRequester(firstPoster) else Modifier),
                                onFocused = {
                                    focusedTitle = label to listOf(media.title, media.metadata).filter(String::isNotBlank).joinToString("  ·  ")
                                })
                        }
                    }
                }
            }
        }
    }
    }
}

/** «23. oktober 1994» in the reader's language; the raw text if Seerr sent something else. */
@Composable
private fun birthday(raw: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return runCatching {
        java.time.LocalDate.parse(raw).format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
            .withLocale(locale))
    }.getOrDefault(raw)
}
