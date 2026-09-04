package app.reelstack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSessionClick: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    onLibraryClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val configuredMediaSources = state.connections
        .filter { connection ->
            connection.baseUrl.isNotBlank() &&
                (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        }
        .map { it.kind }
    val mediaSources = configuredMediaSources.ifEmpty {
        (state.recentMovies + state.recentSeries).map(LibraryMedia::source).distinct()
    }
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 24.dp,
                top = 54.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 22.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Header()
                Spacer(Modifier.height(28.dp))
                Text(
                    text = greeting(),
                    color = TextColor,
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            if (HomeSection.NOW_PLAYING in state.homeSections) {
                item {
                    SectionTitle("Spelar no", Modifier.padding(top = 28.dp, bottom = 15.dp))
                    if (state.sessions.isEmpty()) {
                        EmptyNowPlayingCard()
                    } else {
                        NowPlayingRail(
                            sessions = state.sessions,
                            pendingSessionKey = state.pendingSessionKey,
                            onOpen = onSessionClick,
                            onPlaybackToggle = onPlaybackToggle,
                        )
                    }
                }
            }
            if (HomeSection.RECENT_MOVIES in state.homeSections) {
                mediaSources.forEach { source ->
                    item(key = "recent-movies-${source.name}") {
                        MediaSectionTitle("Nyleg lagde til filmar", source, Modifier.padding(top = 25.dp, bottom = 13.dp))
                        val items = state.recentMovies.filter { it.source == source }
                        if (items.isEmpty()) {
                            EmptySectionLine(mediaEmptyMessage(state, source, "Ingen nyleg lagde til filmar."))
                        } else {
                            LibraryRail(items, onLibraryClick)
                        }
                    }
                }
            }
            if (HomeSection.RECENT_SERIES in state.homeSections) {
                mediaSources.forEach { source ->
                    item(key = "recent-series-${source.name}") {
                        MediaSectionTitle("Nyleg lagde til seriar", source, Modifier.padding(top = 25.dp, bottom = 13.dp))
                        val items = state.recentSeries.filter { it.source == source }
                        if (items.isEmpty()) {
                            EmptySectionLine(mediaEmptyMessage(state, source, "Ingen nyleg lagde til episodar."))
                        } else {
                            LibraryRail(items, onLibraryClick)
                        }
                    }
                }
            }
            if (HomeSection.UPCOMING in state.homeSections) {
                item {
                    SectionTitle("Kjem snart", Modifier.padding(top = 25.dp, bottom = 13.dp))
                    if (state.upcoming.isEmpty()) {
                        EmptySectionLine("Ingen overvaka utgjevingar dei neste 28 dagane.")
                    } else {
                        UpcomingRail(state.upcoming)
                    }
                }
            }
            if (HomeSection.DOWNLOADS in state.homeSections) {
                item {
                    SectionTitle("Nedlastingar", Modifier.padding(top = 26.dp, bottom = 10.dp))
                    if (state.incoming.isEmpty()) {
                        EmptySectionLine("Køane i Radarr og Sonarr er tomme.")
                    }
                }
                items(state.incoming, key = IncomingMedia::id) { media ->
                    IncomingRow(media = media, onClick = { onMediaClick(media.id) })
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "HomeReel",
                color = TextColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "God morgon"
    in 12..17 -> "God ettermiddag"
    else -> "God kveld"
}

private fun mediaEmptyMessage(state: ReelstackUiState, source: ServiceKind, emptyMessage: String): String =
    if (source in state.failedServices) {
        "Fekk ikkje oppdatert ${source.displayName}. Sjekk tilkoplinga i Innstillingar."
    } else if (source in state.serviceWarnings) {
        "${source.displayName} er tilkopla, men denne rada vart ikkje oppdatert."
    } else {
        emptyMessage
    }

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = PrimarySoft,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
private fun MediaSectionTitle(text: String, source: ServiceKind, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        ServiceLogo(
            kind = source,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "${source.displayName} · $text",
            color = PrimarySoft,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun NowPlayingRail(
    sessions: List<PlaybackSession>,
    pendingSessionKey: String?,
    onOpen: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
) {
    if (sessions.size == 1) {
        val session = sessions.single()
        NowPlayingCard(
            session = session,
            pending = pendingSessionKey == session.key,
            controlsLocked = pendingSessionKey != null,
            onOpen = { onOpen(session.key) },
            onPlaybackToggle = { onPlaybackToggle(session.key) },
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(sessions, key = PlaybackSession::key) { session ->
            NowPlayingCard(
                session = session,
                pending = pendingSessionKey == session.key,
                controlsLocked = pendingSessionKey != null,
                onOpen = { onOpen(session.key) },
                onPlaybackToggle = { onPlaybackToggle(session.key) },
                modifier = Modifier.width(316.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    session: PlaybackSession,
    pending: Boolean,
    controlsLocked: Boolean,
    onOpen: () -> Unit,
    onPlaybackToggle: () -> Unit,
    modifier: Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        animationSpec = spring(stiffness = 100f, dampingRatio = 0.82f),
        label = "playback-progress",
    )

    Box(
        modifier = modifier
            .height(306.dp)
            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 86.dp, bottomEnd = 34.dp, bottomStart = 34.dp))
            .border(
                1.dp,
                Color(0x40E2D5FF),
                RoundedCornerShape(topStart = 34.dp, topEnd = 86.dp, bottomEnd = 34.dp, bottomStart = 34.dp),
            )
            .clickable(onClick = onOpen),
    ) {
        MediaArtwork(
            url = session.artworkUrl,
            fallbackRes = if (session.sessionId?.startsWith("demo-") == true) R.drawable.session_still else R.drawable.media_placeholder,
            contentDescription = "${session.userName} ser på ${session.title}",
            contentScale = ContentScale.Crop,
            source = session.source,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x5204050A),
                    0.44f to Color(0x7704050A),
                    1f to Color(0xF5080710),
                ),
            ),
        )

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
            Text(
                "${session.userName} · ${session.deviceName}",
                color = PrimarySoft,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(session.title, color = Color.White, fontSize = 29.sp, lineHeight = 31.sp, letterSpacing = (-1.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(session.subtitle, color = Color(0xFFB2A9C1), fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = Primary,
                    trackColor = Color(0x2BCEBCEB),
                    modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                )
                Text(session.timeLeft, color = Color(0xFFD0C8DC), fontSize = 11.sp, modifier = Modifier.padding(start = 12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Badge(icon = Icons.Rounded.Tv, text = session.streamMethod)
                Spacer(Modifier.width(8.dp))
                Badge(text = session.quality)
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onPlaybackToggle,
                    enabled = !controlsLocked,
                    shape = CircleShape,
                    color = Primary,
                    contentColor = Color(0xFF110B19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.76f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (pending) {
                            CircularProgressIndicator(
                                color = Color(0xFF110B19),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(23.dp),
                            )
                        } else {
                            AnimatedContent(session.paused, label = "play-pause") { paused ->
                                Icon(
                                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = if (paused) "Hald fram avspelinga" else "Set avspelinga på pause",
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(52.dp).background(SurfaceRaised.copy(alpha = 0.92f), CircleShape),
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Avspelingsdetaljar", tint = TextColor)
                }
            }
        }
    }
}

@Composable
private fun LibraryRail(items: List<LibraryMedia>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = LibraryMedia::id) { media ->
            LibraryCard(media = media, onClick = { onClick(media.id) })
        }
    }
}

@Composable
private fun LibraryCard(media: LibraryMedia, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(146.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0x35E2D5FF), RoundedCornerShape(20.dp)),
        ) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                source = media.source,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xE0080710)),
                ),
            )
            media.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = Primary,
                    trackColor = Color(0x55FFFFFF),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp),
                )
            }
            Surface(
                color = Color(0xC4120E1B),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
            ) {
                ServiceLogo(
                    kind = media.source,
                    contentDescription = media.source.displayName,
                    modifier = Modifier.padding(6.dp).size(13.dp),
                )
            }
        }
        Text(
            media.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            media.subtitle,
            color = Muted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun UpcomingRail(items: List<UpcomingMedia>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = UpcomingMedia::id) { media -> UpcomingCard(media) }
    }
}

@Composable
private fun UpcomingCard(media: UpcomingMedia) {
    Surface(
        color = SurfaceRaised.copy(alpha = 0.9f),
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x30E2D5FF)),
        modifier = Modifier.width(268.dp),
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 82.dp, height = 116.dp).clip(RoundedCornerShape(15.dp)),
            )
            Column(Modifier.weight(1f).padding(start = 13.dp, top = 5.dp)) {
                Text(media.dateLabel, color = PrimarySoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    media.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
                Text(media.subtitle, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                Text(media.source.displayName, color = PrimarySoft, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun EmptyNowPlayingCard() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF625B70)))
        Text("Ingen aktive avspelingar", color = Color(0xFF8E879A), fontSize = 12.sp, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun EmptySectionLine(text: String) {
    Text(text, color = Color(0xFF8E879A), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector? = null, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, Color(0x42E2D5FF), RoundedCornerShape(10.dp))
            .background(Color(0x5507070D), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = Color(0xFFDDD6E7), modifier = Modifier.size(15.dp)) }
        if (icon != null) Spacer(Modifier.width(5.dp))
        Text(text, color = Color(0xFFDDD6E7), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IncomingRow(media: IncomingMedia, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .semantics { role = Role.Button },
    ) {
        MediaArtwork(
            url = media.artworkUrl,
            fallbackRes = media.artworkRes,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 66.dp, height = 82.dp).clip(RoundedCornerShape(13.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(media.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(media.source.displayName, color = PrimarySoft, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Icon(
                    if (media.state == IncomingState.DOWNLOADING) Icons.Rounded.Download else Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = if (media.state == IncomingState.DOWNLOADING) Primary else Warning,
                    modifier = Modifier.size(18.dp),
                )
                Text(media.status, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = Color(0xFFB7A8CA), modifier = Modifier.size(17.dp))
    }
}
