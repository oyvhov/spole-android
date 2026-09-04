package app.reelstack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Movie
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import app.reelstack.ui.components.IncomingSkeleton
import app.reelstack.ui.components.LibraryRailSkeleton
import app.reelstack.ui.components.NowPlayingSkeleton
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.components.UpcomingSkeleton
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSessionClick: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    onLibraryClick: (String) -> Unit,
    onUpcomingClick: (String) -> Unit,
    onCalendarClick: () -> Unit = {},
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
    val hasQueueConnection = state.connections.any {
        it.baseUrl.isNotBlank() && (it.kind == ServiceKind.RADARR || it.kind == ServiceKind.SONARR)
    }
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 24.dp,
                top = 32.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 22.dp,
            ),
            modifier = Modifier.fillMaxSize().testTag("home-feed"),
        ) {
            item {
                HomeGreeting()
            }
            if (HomeSection.NOW_PLAYING in state.homeSections) {
                item {
                    SectionTitle("Spelar no", Modifier.padding(top = 28.dp, bottom = 15.dp))
                    if (state.sessions.isEmpty()) {
                        if (state.isRefreshing && configuredMediaSources.isNotEmpty()) {
                            NowPlayingSkeleton()
                        } else {
                            EmptyNowPlayingCard()
                        }
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
            run {
                mediaSources.forEach { source ->
                    val section = if (source == ServiceKind.EMBY) HomeSection.EMBY_MOVIES else HomeSection.JELLYFIN_MOVIES
                    if (section !in state.homeSections) return@forEach
                    item(key = "recent-movies-${source.name}") {
                        MediaSectionTitle("Nyleg lagde til filmar", source, Modifier.padding(top = 25.dp, bottom = 13.dp))
                        val items = state.recentMovies.filter { it.source == source }
                        if (items.isEmpty()) {
                            if (state.isRefreshing) {
                                LibraryRailSkeleton("Lastar nyleg lagde til filmar frå ${source.displayName}")
                            } else {
                                EmptySectionLine(mediaEmptyMessage(state, source, "Ingen nyleg lagde til filmar."))
                            }
                        } else {
                            LibraryRail(items, onLibraryClick, wide = false)
                        }
                    }
                }
            }
            run {
                mediaSources.forEach { source ->
                    val section = if (source == ServiceKind.EMBY) HomeSection.EMBY_SERIES else HomeSection.JELLYFIN_SERIES
                    if (section !in state.homeSections) return@forEach
                    item(key = "recent-series-${source.name}") {
                        MediaSectionTitle("Nyleg lagde til seriar", source, Modifier.padding(top = 25.dp, bottom = 13.dp))
                        val items = state.recentSeries.filter { it.source == source }
                        if (items.isEmpty()) {
                            if (state.isRefreshing) {
                                LibraryRailSkeleton(
                                    "Lastar nyleg lagde til seriar frå ${source.displayName}",
                                    wide = true,
                                )
                            } else {
                                EmptySectionLine(mediaEmptyMessage(state, source, "Ingen nyleg lagde til episodar."))
                            }
                        } else {
                            LibraryRail(items, onLibraryClick, wide = true)
                        }
                    }
                }
            }
            if (HomeSection.UPCOMING in state.homeSections) {
                item {
                    UpcomingSectionTitle(
                        onCalendarClick = onCalendarClick,
                        modifier = Modifier.padding(top = 25.dp, bottom = 13.dp),
                    )
                    if (state.upcoming.isEmpty()) {
                        if (state.isRefreshing && hasQueueConnection) {
                            UpcomingSkeleton()
                        } else {
                            EmptySectionLine("Ingen overvaka utgjevingar dei neste 28 dagane.")
                        }
                    } else {
                        UpcomingRail(state.upcoming, onUpcomingClick)
                    }
                }
            }
            if (HomeSection.DOWNLOADS in state.homeSections) {
                item {
                    SectionTitle("Nedlastingar", Modifier.padding(top = 26.dp, bottom = 10.dp))
                    if (state.incoming.isEmpty()) {
                        if (state.isRefreshing && hasQueueConnection) {
                            IncomingSkeleton()
                        } else {
                            EmptySectionLine("Køane i Radarr og Sonarr er tomme.")
                        }
                    }
                }
                items(state.incoming, key = IncomingMedia::id) { media ->
                    IncomingRow(media = media, onClick = { onMediaClick(media.id) })
                }
            }
        }
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "God morgon"
    in 12..17 -> "God ettermiddag"
    else -> "God kveld"
}

@Composable
private fun HomeGreeting() {
    var appeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "home-greeting-reveal",
    )
    val date = remember {
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.forLanguageTag("nn-NO")),
        ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("nn-NO")) else it.toString() }
    }
    Column(
        modifier = Modifier.graphicsLayer {
            alpha = reveal
            translationY = (1f - reveal) * 24f
        },
    ) {
        Text(
            text = greeting(),
            color = TextColor,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = date,
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
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
        color = TextColor,
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
            color = TextColor,
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 420f, dampingRatio = 0.72f),
        label = "now-playing-press",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        animationSpec = spring(stiffness = 100f, dampingRatio = 0.82f),
        label = "playback-progress",
    )

    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .height(292.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onOpen,
            ),
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

        Column(modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(
                "${session.userName} · ${session.deviceName}",
                color = PrimarySoft,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(session.title, color = Color.White, fontSize = 29.sp, lineHeight = 31.sp, letterSpacing = (-1.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(session.subtitle, color = app.reelstack.ui.theme.Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = Primary,
                    trackColor = app.reelstack.ui.theme.SurfaceRaised,
                    modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape),
                )
                Text(session.timeLeft, color = app.reelstack.ui.theme.Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(session.streamMethod, color = TextColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(session.quality, color = app.reelstack.ui.theme.Muted, fontSize = 12.sp)
                }
                Surface(
                    onClick = onPlaybackToggle,
                    enabled = !controlsLocked,
                    shape = CircleShape,
                    color = Primary,
                    contentColor = app.reelstack.ui.theme.Ink,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (pending) {
                            CircularProgressIndicator(
                                color = app.reelstack.ui.theme.Ink,
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
private fun LibraryRail(items: List<LibraryMedia>, onClick: (String) -> Unit, wide: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            LibraryCard(
                media = media,
                wide = wide,
                revealDelay = (index.coerceAtMost(2) * 30),
                onClick = { onClick(media.id) },
            )
        }
    }
}

@Composable
private fun LibraryCard(media: LibraryMedia, wide: Boolean, revealDelay: Int, onClick: () -> Unit) {
    val cardWidth = if (wide) 224.dp else 146.dp
    val artworkHeight = if (wide) 126.dp else 214.dp
    val artworkShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = revealDelay),
        label = "library-card-reveal",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.7f),
        label = "library-card-press",
    )
    Column(
        modifier = Modifier
            .width(cardWidth)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 30f
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkHeight)
                .clip(artworkShape),
        ) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                source = media.source,
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                color = app.reelstack.ui.theme.SurfaceRaised,
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
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun UpcomingRail(items: List<UpcomingMedia>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            UpcomingCard(media, revealDelay = index.coerceAtMost(2) * 30) { onClick(media.id) }
        }
    }
}

@Composable
private fun UpcomingCard(media: UpcomingMedia, revealDelay: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = revealDelay),
        label = "upcoming-card-reveal",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.7f),
        label = "upcoming-card-press",
    )
    Column(
        modifier = Modifier
            .width(178.dp)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 30f
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .clip(shape),
        ) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = media.title,
                contentScale = if (media.mediaType.equals("Movie", true)) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(app.reelstack.ui.theme.Ink),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x1808060E),
                        0.5f to Color.Transparent,
                        1f to Color(0xF3090710),
                    ),
                ),
            )
            Surface(
                color = app.reelstack.ui.theme.Ink,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = PrimarySoft, modifier = Modifier.size(13.dp))
                    Text(
                        media.dateLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
            Surface(
                color = app.reelstack.ui.theme.Ink,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(
                    if (media.source == ServiceKind.RADARR) Icons.Rounded.Movie else Icons.Rounded.Tv,
                    contentDescription = media.source.displayName,
                    tint = PrimarySoft,
                    modifier = Modifier.padding(7.dp).size(13.dp),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(15.dp)) {
                Text(
                    media.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    media.subtitle,
                    color = app.reelstack.ui.theme.Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun UpcomingSectionTitle(onCalendarClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Kjem snart", color = TextColor, style = MaterialTheme.typography.titleMedium)
            Text("Heimeutgjevingar og nye episodar", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Surface(
            onClick = onCalendarClick,
            color = Primary.copy(alpha = 0.18f),
            contentColor = PrimarySoft,
            shape = CircleShape,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Kalender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
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
        Box(Modifier.size(7.dp).clip(CircleShape).background(app.reelstack.ui.theme.Muted))
        Text("Ingen aktive avspelingar", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun EmptySectionLine(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
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
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = app.reelstack.ui.theme.Muted, modifier = Modifier.size(17.dp))
    }
}
