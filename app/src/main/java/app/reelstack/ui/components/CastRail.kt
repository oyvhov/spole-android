package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.CastMember
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.SurfaceRaised

@Composable
fun CastRail(cast: List<CastMember>, source: app.reelstack.data.model.ServiceKind? = null,
    loadTitles: suspend (CastMember, app.reelstack.data.model.ServiceKind) -> List<app.reelstack.data.model.LibraryMedia> = { _, _ -> emptyList() },
    onTitle: (app.reelstack.data.model.LibraryMedia) -> Unit = {}) {
    var selected by remember(cast) { mutableStateOf<CastMember?>(null) }
    var titles by remember(cast) { mutableStateOf(emptyList<app.reelstack.data.model.LibraryMedia>()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        titles = emptyList()
        failed = false
        val person = selected ?: return@LaunchedEffect
        val origin = source ?: return@LaunchedEffect
        loading = true
        try { titles = loadTitles(person, origin) }
        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(cast.distinctBy { it.remoteId ?: it.name }, key = { it.remoteId ?: it.name }) { person ->
            val interaction = remember { MutableInteractionSource() }
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            Column(Modifier.width(104.dp).focusOutline(interaction, shape)
                .clickable(interactionSource = interaction, indication = mediaCardIndication(),
                    enabled = person.remoteId != null && source != null, role = Role.Button) {
                    selected = if (selected == person) null else person
                }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(SurfaceRaised), contentAlignment = Alignment.Center) {
                    if (person.portraitUrl != null) {
                        MediaArtwork(person.portraitUrl, null, Modifier.matchParentSize(), fallbackRes = R.drawable.media_placeholder, source = source)
                    } else {
                        Text(person.name.split(' ').filter(String::isNotBlank).take(2).map { it.first() }.joinToString(""),
                            color = Muted, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Text(person.name, style = MaterialTheme.typography.labelMedium, maxLines = 3,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                person.role?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = Muted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
    selected?.let { person ->
        Text(person.name, style = MaterialTheme.typography.titleMedium)
        when {
            loading -> androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp))
            failed -> Text(stringResource(R.string.error_content_details), color = Muted)
            titles.isEmpty() -> Text(stringResource(R.string.person_no_titles), color = Muted)
            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(titles, key = { it.id }) { title ->
                    val interaction = remember { MutableInteractionSource() }
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    val movie = title.mediaType.equals("Movie", true)
                    Column(Modifier.width(if (movie) 132.dp else 220.dp).focusOutline(interaction, shape)
                        .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button) { onTitle(title) }
                        .padding(6.dp)) {
                        MediaArtwork(title.artworkUrl, null, Modifier.fillMaxWidth().aspectRatio(if (movie) 2f / 3f else 16f / 9f).clip(shape),
                            fallbackRes = title.artworkRes, source = title.source)
                        Text(title.title, modifier = Modifier.padding(top = 6.dp), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
    }
}
