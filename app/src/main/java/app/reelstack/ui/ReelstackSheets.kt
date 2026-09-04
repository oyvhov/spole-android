package app.reelstack.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import app.reelstack.data.network.EndpointValidator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Warning
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.DetailTextSkeleton
import app.reelstack.ui.components.ServiceLogo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelstackSheets(
    state: ReelstackUiState,
    connectionDraft: ConnectionDraft?,
    onDismiss: () -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onConnectionNameChange: (String) -> Unit,
    onConnectionUrlChange: (String) -> Unit,
    onConnectionTokenChange: (String) -> Unit,
    onConnectionUserIdChange: (String) -> Unit,
    onConnectionAuthModeChange: (ConnectionAuthMode) -> Unit,
    onConnectionUsernameChange: (String) -> Unit,
    onConnectionPasswordChange: (String) -> Unit,
    onTestAndSaveConnection: () -> Unit,
    onRemoveConnection: (ServiceKind) -> Unit,
    onAddMedia: (String) -> Unit,
    onUpcomingClick: (String) -> Unit,
) {
    val sheet = state.activeSheet ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = app.reelstack.ui.theme.Surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color(0xB8040308),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 5.dp).size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF81788D), CircleShape),
            )
        },
    ) {
        when (sheet) {
            is AppSheet.SessionDetails -> SessionSheet(state, sheet.sessionKey, onPlaybackToggle)
            is AppSheet.MediaDetails -> MediaDetailsSheet(state, sheet.mediaId)
            is AppSheet.LibraryDetails -> LibraryDetailsSheet(state, sheet.mediaId)
            is AppSheet.TitleDetails -> state.contentDetails?.let {
                RichTitleDetailsSheet(state = state, onAddMedia = onAddMedia)
            }
            AppSheet.UpcomingCalendar -> UpcomingCalendarSheet(state.upcoming, onUpcomingClick)
            is AppSheet.ConnectionEditor -> connectionDraft?.let {
                ConnectionEditorSheet(
                    draft = it,
                    configured = state.connections.firstOrNull { item -> item.kind == it.kind }?.baseUrl?.isNotBlank() == true,
                    onDismiss = onDismiss,
                    onNameChange = onConnectionNameChange,
                    onUrlChange = onConnectionUrlChange,
                    onTokenChange = onConnectionTokenChange,
                    onUserIdChange = onConnectionUserIdChange,
                    onAuthModeChange = onConnectionAuthModeChange,
                    onUsernameChange = onConnectionUsernameChange,
                    onPasswordChange = onConnectionPasswordChange,
                    onTestAndSave = onTestAndSaveConnection,
                    onRemove = { onRemoveConnection(it.kind) },
                )
            }
        }
    }
}

@Composable
private fun RichTitleDetailsSheet(state: ReelstackUiState, onAddMedia: (String) -> Unit) {
    val details = state.contentDetails ?: return
    val discoverMedia = (state.discover + state.searchResults).firstOrNull { it.id == details.key }
    val isMovie = details.mediaType.equals("movie", ignoreCase = true) ||
        details.facts.any { it.equals("Film", ignoreCase = true) }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .animateContentSize(animationSpec = spring())
            .padding(start = 18.dp, end = 18.dp, bottom = 40.dp),
    ) {
        if (isMovie) {
            MoviePosterSummary(
                title = details.title,
                eyebrow = details.eyebrow,
                subtitle = details.subtitle,
                tagline = details.tagline,
                facts = details.facts,
                artworkUrl = details.artworkUrl,
                artworkRes = details.artworkRes,
                source = details.source,
            )
        } else {
            CinematicTitleHero(
                title = details.title,
                eyebrow = details.eyebrow,
                subtitle = details.subtitle,
                artworkUrl = details.artworkUrl,
                artworkRes = details.artworkRes,
                source = details.source,
            )
        }
        val remainingFacts = if (isMovie) details.facts.drop(4) else details.facts
        if (remainingFacts.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 16.dp),
            ) {
                remainingFacts.take(5).forEach { fact -> DetailPill(fact) }
            }
        }
        if (details.genres.isNotEmpty()) {
            Text(
                details.genres.take(4).joinToString(" · "),
                color = PrimarySoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, top = 15.dp, end = 6.dp),
            )
        }
        details.tagline?.takeIf { !isMovie && it.isNotBlank() }?.let { tagline ->
            Text(
                tagline,
                color = Color(0xFFD8CDE2),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(start = 6.dp, top = 17.dp, end = 6.dp),
            )
        }
        Text(
            when {
                isMovie -> "Om filmen"
                details.mediaType.equals("Episode", ignoreCase = true) -> "Om episoden"
                else -> "Om serien"
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp, top = 19.dp, end = 6.dp),
        )
        Text(
            details.overview ?: details.subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp),
        )
        if (details.loading) {
            DetailTextSkeleton(Modifier.fillMaxWidth().padding(top = 14.dp))
        }
        details.error?.let {
            Text(it, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        }
        if (discoverMedia != null && !discoverMedia.inLibrary) {
            val adding = discoverMedia.id in state.requestingMediaIds
            Button(
                onClick = { onAddMedia(discoverMedia.id) },
                enabled = !discoverMedia.requested && !adding,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Ink,
                    disabledContainerColor = Color(0xFF2A473B),
                    disabledContentColor = Color(0xFFC9F4DB),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(56.dp),
            ) {
                if (adding) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(19.dp))
                } else {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    when {
                        adding -> "Legg til…"
                        discoverMedia.requested -> "Lagd til"
                        else -> "Legg til i mediesamlinga"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MoviePosterSummary(
    title: String,
    eyebrow: String,
    subtitle: String,
    tagline: String?,
    facts: List<String>,
    artworkUrl: String?,
    artworkRes: Int,
    source: ServiceKind?,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
    ) {
        Surface(
            color = Color(0xFF0B0810),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 34.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x38E2D5FF)),
            shadowElevation = 12.dp,
            modifier = Modifier.width(130.dp).height(195.dp),
        ) {
            MediaArtwork(
                url = artworkUrl,
                fallbackRes = artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                source = source,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f).padding(start = 17.dp, top = 7.dp)) {
            Text(
                eyebrow.uppercase(),
                color = PrimarySoft,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                title,
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 7.dp),
            )
            val supportingText = tagline?.takeIf(String::isNotBlank) ?: subtitle
            if (supportingText.isNotBlank()) {
                Text(
                    supportingText,
                    color = Color(0xFFC8BECE),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (facts.isNotEmpty()) {
                Text(
                    facts.take(4).joinToString(" · "),
                    color = PrimarySoft,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 11.dp),
                )
            }
            source?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    SourceMark(kind = it, modifier = Modifier.size(13.dp))
                    Text(it.displayName, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun UpcomingCalendarSheet(items: List<UpcomingMedia>, onUpcomingClick: (String) -> Unit) {
    val zone = ZoneId.systemDefault()
    val grouped = items
        .sortedBy(UpcomingMedia::airDateEpochMillis)
        .groupBy { Instant.ofEpochMilli(it.airDateEpochMillis).atZone(zone).toLocalDate() }
    val weekday = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("nn-NO"))
    val month = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("nn-NO"))
    Column(
        Modifier
            .heightIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(color = Primary.copy(alpha = 0.2f), shape = CircleShape) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = PrimarySoft,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                )
            }
            Column(Modifier.padding(start = 13.dp)) {
                Text("Kalender", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("Komande 28 dagar · heimeutgjevingar og nye episodar", color = Muted, fontSize = 11.sp)
            }
        }
        if (grouped.isEmpty()) {
            Text(
                "Ingen digitale filmutgjevingar eller nye episodar er planlagde enno.",
                color = Muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
            )
        } else {
            grouped.forEach { (date, dayItems) ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                        Text(
                            date.format(weekday).removeSuffix(".").uppercase(),
                            color = PrimarySoft,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            color = Primary.copy(alpha = 0.18f),
                            shape = CircleShape,
                            modifier = Modifier.padding(top = 6.dp).size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(date.format(month).take(3), color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f).padding(start = 13.dp),
                    ) {
                        dayItems.forEach { media -> UpcomingCalendarRow(media, onUpcomingClick) }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingCalendarRow(media: UpcomingMedia, onUpcomingClick: (String) -> Unit) {
    val isMovie = media.mediaType.equals("Movie", ignoreCase = true)
    Surface(
        onClick = { onUpcomingClick(media.id) },
        color = SurfaceRaised.copy(alpha = 0.82f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24E2D5FF)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(9.dp)) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = if (isMovie) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .size(width = if (isMovie) 56.dp else 86.dp, height = 78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B0810)),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceMark(kind = media.source, modifier = Modifier.size(11.dp))
                    Text(
                        when {
                            media.source == ServiceKind.SONARR -> "NY EPISODE"
                            media.facts.any { it == "Fysisk utgjeving" } -> "FYSISK UTGJEVING"
                            else -> "DIGITAL UTGJEVING"
                        },
                        color = PrimarySoft,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
                Text(
                    media.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(
                    media.subtitle,
                    color = Muted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(media.dateLabel, color = Color(0xFFD9D0E2), fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun SourceMark(kind: ServiceKind, modifier: Modifier = Modifier) {
    when (kind) {
        ServiceKind.JELLYFIN, ServiceKind.EMBY -> ServiceLogo(
            kind = kind,
            contentDescription = null,
            modifier = modifier,
        )
        ServiceKind.SONARR -> Icon(Icons.Rounded.Tv, contentDescription = null, tint = PrimarySoft, modifier = modifier)
        ServiceKind.RADARR, ServiceKind.SEERR -> Icon(
            Icons.Rounded.Movie,
            contentDescription = null,
            tint = PrimarySoft,
            modifier = modifier,
        )
    }
}

@Composable
private fun CinematicTitleHero(
    title: String,
    eyebrow: String,
    subtitle: String,
    artworkUrl: String?,
    artworkRes: Int,
    source: ServiceKind?,
) {
    Column {
        MediaArtwork(
            url = artworkUrl, fallbackRes = artworkRes, contentDescription = null,
            contentScale = ContentScale.Fit, source = source,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)).background(Ink),
        )
        Text(eyebrow.uppercase(), color = PrimarySoft, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 20.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp))
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun DetailPill(text: String) {
    Text(
        text = text,
        color = Color(0xFFE8E0EF),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceRaised)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun SheetHeader(title: String, description: String, onDismiss: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(description, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
        }
        onDismiss?.let {
            IconButton(onClick = it) { Icon(Icons.Rounded.Close, contentDescription = "Lukk") }
        }
    }
}

@Composable
private fun SessionSheet(state: ReelstackUiState, sessionKey: String, onPlaybackToggle: (String) -> Unit) {
    val session = state.sessions.firstOrNull { it.key == sessionKey } ?: return
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 34.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(286.dp).clip(RoundedCornerShape(28.dp)),
        ) {
            MediaArtwork(
                url = session.artworkUrl,
                fallbackRes = if (session.sessionId?.startsWith("demo-") == true) {
                    R.drawable.session_still
                } else {
                    R.drawable.media_placeholder
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                source = session.source,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x26090711),
                        0.48f to Color(0x18090711),
                        1f to Color(0xF20A0711),
                    ),
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                    .clip(CircleShape).background(Color(0xB5120E1B))
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            ) {
                Box(Modifier.size(7.dp).background(Primary, CircleShape))
                Text(
                    if (session.paused) "På pause" else "Spelar no",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    "${session.source?.displayName ?: "Medietenar"} · ${session.userName} · ${session.deviceName}".uppercase(),
                    color = PrimarySoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    session.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(session.subtitle, color = Color(0xFFD1C8D8), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                LinearProgressIndicator(
                    progress = { session.progress.coerceIn(0f, 1f) },
                    color = Primary,
                    trackColor = Color(0x45FFFFFF),
                    modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(4.dp).clip(CircleShape),
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceRaised,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            ) {
                SessionMetric("Straum", session.streamMethod, Modifier.weight(1f))
                SessionMetric("Kvalitet", session.quality, Modifier.weight(1f))
                SessionMetric("Att", session.timeLeft, Modifier.weight(1f))
            }
        }
        Button(
            onClick = { onPlaybackToggle(session.key) },
            enabled = state.pendingSessionKey == null,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color(0xFF160D20)),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(58.dp),
        ) {
            if (state.pendingSessionKey == session.key) {
                CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Text("Sender kommando…", modifier = Modifier.padding(start = 8.dp))
            } else {
                AnimatedContent(
                    targetState = session.paused,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "playback-action",
                ) { paused ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                        Text(if (paused) "Hald fram" else "Set på pause", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), color = Muted, fontSize = 9.sp)
        Text(
            value,
            color = Color(0xFFF1EBF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun MediaDetailsSheet(state: ReelstackUiState, mediaId: String) {
    val media = state.incoming.firstOrNull { it.id == mediaId } ?: return
    val downloading = media.state == IncomingState.DOWNLOADING
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "${if (downloading) "Film" else "Serie"} · ${media.source.displayName}")
        Row(modifier = Modifier.padding(top = 18.dp)) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 116.dp, height = 164.dp).clip(RoundedCornerShape(20.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape)
                        .background((if (downloading) Primary else Warning).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Icon(
                        if (downloading) Icons.Rounded.Download else Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (downloading) PrimarySoft else Warning,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(media.status, color = if (downloading) PrimarySoft else Warning, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
                Text(
                    if (downloading) "Ferdig om om lag 24 minutt" else "Ventar på godkjenning",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    if (downloading) "Radarr fann ei 4K-utgjeving og sende henne til nedlastingsklienten." else "Seerr varslar deg når tittelen er godkjend.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryDetailsSheet(state: ReelstackUiState, mediaId: String) {
    val media = (state.recentMovies + state.recentSeries).firstOrNull { it.id == mediaId } ?: return
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 40.dp)) {
        SheetHeader(media.title, "Bibliotek i ${media.source.displayName}")
        MediaArtwork(
            url = media.artworkUrl,
            fallbackRes = media.artworkRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            source = media.source,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(190.dp).clip(RoundedCornerShape(24.dp)),
        )
        Text(media.subtitle, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 15.dp))
        media.progress?.let { progress ->
            Text("${(progress * 100).toInt()} % sett", color = PrimarySoft, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = Primary,
                trackColor = Color(0x2BCEBCEB),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).clip(CircleShape),
            )
        }
        Text(
            "Opne ${media.source.displayName} for å spele av tittelen.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

@Composable
private fun ConnectionEditorSheet(
    draft: ConnectionDraft,
    configured: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onUserIdChange: (String) -> Unit,
    onAuthModeChange: (ConnectionAuthMode) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTestAndSave: () -> Unit,
    onRemove: () -> Unit,
) {
    var credentialsStep by rememberSaveable(draft.kind) { mutableStateOf(configured) }
    var advanced by rememberSaveable(draft.kind) { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val nextStep: () -> Unit = {
        runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .onSuccess { onUrlChange(it); credentialsStep = true; focus.clearFocus() }
            .onFailure { addressError = it.message }
    }
    Column(
        Modifier.imePadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        Text(if (configured) "TILKOPLING" else if (credentialsStep) "02 / LOGG INN" else "01 / FINN TENAREN",
            color = Primary, fontSize = 10.sp, letterSpacing = 1.6.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        SheetHeader("Kople til ${draft.kind.displayName}",
            if (credentialsStep) "Vel korleis du vil logge inn." else "Bruk adressa du vanlegvis opnar i nettlesaren.", onDismiss)
        Spacer(Modifier.height(20.dp))
        if (!credentialsStep) {
            OutlinedTextField(
                value = draft.url, onValueChange = { addressError = null; onUrlChange(it) },
                label = { Text("Tenaradresse") }, placeholder = { Text("https://media.example.com") },
                supportingText = { Text(addressError ?: "Ta med port eller undermappe dersom tenaren din brukar det.") },
                isError = addressError != null,
                singleLine = true, shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { nextStep() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = nextStep, enabled = draft.url.isNotBlank(),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(54.dp)) {
                Text("Hald fram", fontWeight = FontWeight.Bold)
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(draft.url, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f),
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            TextButton(onClick = { credentialsStep = false }, enabled = !draft.saving) { Text("Endre") }
        }
        if (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOf(
                    ConnectionAuthMode.QUICK_CONNECT to "Quick Connect",
                    ConnectionAuthMode.ACCOUNT to if (draft.kind == ServiceKind.SEERR) "Jellyfin-konto" else "Brukarnamn",
                    ConnectionAuthMode.API_KEY to if (draft.kind == ServiceKind.SEERR) "API-nøkkel" else "Tilgangsteikn",
                ).forEach { (mode, label) ->
                    FilterChip(selected = draft.authMode == mode, onClick = { onAuthModeChange(mode) },
                        enabled = !draft.saving, label = { Text(label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp), border = null, colors = connectionChipColors())
                }
            }
        }
        val supportsJellyfinLogin = draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR
        val usesAccount = supportsJellyfinLogin && draft.authMode == ConnectionAuthMode.ACCOUNT
        val usesQuickConnect = supportsJellyfinLogin && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
        if (usesQuickConnect) QuickConnectPanel(draft)
        if (usesAccount) {
            OutlinedTextField(
                value = draft.username, onValueChange = onUsernameChange,
                label = { Text("Brukarnamn") }, enabled = !draft.saving, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = draft.password, onValueChange = onPasswordChange,
                label = { Text("Passord") }, supportingText = { Text("Kan stå tomt for ein konto utan passord.") },
                enabled = !draft.saving, singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (showPassword) "Skjul passord" else "Vis passord")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!draft.saving && draft.username.isNotBlank()) { focus.clearFocus(); onTestAndSave() }
                }),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(if (draft.kind == ServiceKind.SEERR) "Bruk Jellyfin-kontoen din. Seerr sjekkar innlogginga og brukar dine vanlege rettar. Passordet blir aldri lagra."
                else "Passordet blir sendt direkte til Jellyfin og blir aldri lagra.",
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (!usesAccount && !usesQuickConnect) {
            OutlinedTextField(
                value = draft.token, onValueChange = onTokenChange,
                label = { Text("API-nøkkel eller tilgangsteikn") },
                enabled = !draft.saving, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!draft.saving && draft.token.isNotBlank()) { focus.clearFocus(); onTestAndSave() }
                }),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(when (draft.kind) {
                ServiceKind.RADARR, ServiceKind.SONARR -> "Du finn API-nøkkelen under Settings → General → Security på tenaren."
                ServiceKind.SEERR -> "Du finn API-nøkkelen under Settings → General i Seerr."
                else -> "Du finn API-nøkkelen i kontrollpanelet til tenaren."
            }, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp))
        }
        TextButton(onClick = { advanced = !advanced }, enabled = !draft.saving) {
            Text(if (advanced) "Skjul avanserte val" else "Avanserte val", fontSize = 12.sp)
        }
        if (advanced) {
            OutlinedTextField(value = draft.name, onValueChange = onNameChange,
                label = { Text("Namn på tilkoplinga") }, singleLine = true, enabled = !draft.saving,
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(), modifier = Modifier.fillMaxWidth())
            if (!usesAccount && !usesQuickConnect && (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.EMBY)) {
                OutlinedTextField(value = draft.userId, onValueChange = onUserIdChange,
                    label = { Text("Profil-ID (valfri)") }, singleLine = true, enabled = !draft.saving,
                    supportingText = { Text("Tomt felt vel automatisk ein profil med alle bibliotek.") },
                    shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
        }
        draft.warning?.let { MessageCard(it, warning = true) }
        draft.error?.let { MessageCard(it, warning = false) }
        Button(
            onClick = { focus.clearFocus(); onTestAndSave() },
            enabled = !draft.saving && (usesQuickConnect || if (usesAccount) draft.username.isNotBlank() else draft.token.isNotBlank()),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp),
        ) {
            if (draft.saving) CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Text(when {
                draft.saving && usesQuickConnect && draft.quickConnectCode != null -> "Ventar på godkjenning…"
                draft.saving && usesQuickConnect -> "Lagar kode…"
                draft.saving -> "Koplar til…"
                usesQuickConnect && draft.quickConnectCode != null -> "Lag ny kode"
                usesQuickConnect -> "Start Quick Connect"
                usesAccount -> "Logg inn"
                else -> "Kople til"
            }, modifier = Modifier.padding(start = if (draft.saving) 10.dp else 0.dp))
        }
        if (draft.saving) {
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Avbryt") }
        } else if (configured) {
            TextButton(onClick = onRemove, colors = ButtonDefaults.textButtonColors(contentColor = Warning),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                Text("Fjern tilkoplinga", modifier = Modifier.padding(start = 7.dp))
            }
        }
    }
}

@Composable
private fun QuickConnectPanel(draft: ConnectionDraft) {
    AnimatedContent(
        targetState = draft.quickConnectCode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "quick-connect-code",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { code ->
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceRaised,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (code == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Devices,
                            contentDescription = null,
                            tint = PrimarySoft,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            "Logg inn utan passord",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (draft.kind == ServiceKind.SEERR) "Godkjenn koden i Jellyfin for å logge inn på Seerr. Krev ein Seerr-versjon med Quick Connect."
                            else "Godkjenn koden i ein Jellyfin-app der du allereie er innlogga.",
                            color = Muted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Primary, CircleShape))
                        Text(
                            if (draft.quickConnectWaiting) "Ventar på godkjenning" else "Fullfører innlogginga",
                            color = PrimarySoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                    Text(
                        code.chunked(3).joinToString("  "),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        "Opne Jellyfin på ei anna eining, gå til Innstillingar → Quick Connect, og skriv inn koden.",
                        color = Muted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun connectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor = Ink,
    containerColor = SurfaceRaised.copy(alpha = 0.78f),
    labelColor = Muted,
)

@Composable
private fun MessageCard(text: String, warning: Boolean) {
    val accent = if (warning) Color(0xFFFFBE69) else Warning
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
    ) {
        Text(text, color = accent, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun connectionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary.copy(alpha = 0.72f),
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = SurfaceRaised,
    unfocusedContainerColor = SurfaceRaised,
    cursorColor = Primary,
)
