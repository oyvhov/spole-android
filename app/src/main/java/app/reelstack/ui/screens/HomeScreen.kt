package app.reelstack.ui.screens

import app.reelstack.ui.components.focusOutline

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
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
import androidx.compose.ui.res.stringResource
import app.reelstack.ui.theme.LocalTabletCanvas
import app.reelstack.ui.components.TabletLibraryFeature
import app.reelstack.ui.components.tabletFeaturedTitle
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.isSeries
import app.reelstack.data.model.seerrStatusLabel
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.HomeSearchEntry
import app.reelstack.ui.components.IncomingSkeleton
import app.reelstack.ui.components.LibraryRailSkeleton
import app.reelstack.ui.components.AccountAvatarButton
import app.reelstack.ui.components.preferredHomeAccount
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.components.UpcomingSkeleton
import app.reelstack.ui.components.RecommendationSkeleton
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    onUpcomingClick: (String) -> Unit,
    onCalendarClick: () -> Unit = {},
    onRefresh: () -> Unit,
    onAccountClick: () -> Unit = {},
    onDiscoverClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    searchTransitionModifier: Modifier = Modifier,
    showSearch: Boolean = true,
    showBrand: Boolean = true,
) {
    val configuredMediaSources = state.connections
        .filter { connection ->
            connection.baseUrl.isNotBlank() &&
                (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        }
        // Each source becomes a keyed LazyColumn item, and duplicate keys crash the list.
        .map { it.kind }
        .distinct()
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
      ReelPage(media = true) {
        val tablet = LocalTabletCanvas.current
        val edge = app.reelstack.ui.theme.LocalMediaEdgeToEdge.current
        val featured = if (tablet) tabletFeaturedTitle(state.recentSeries, state.homeSections) else null
        LazyColumn(
            contentPadding = PaddingValues(
                start = ReelLayout.Gutter,
                top = if (featured != null) 16.dp else ReelLayout.PageTop,
                end = if (edge) 0.dp else ReelLayout.Gutter,
                bottom = contentPadding.calculateBottomPadding() + 22.dp,
            ),
            modifier = Modifier.fillMaxSize().testTag("home-feed"),
        ) {
            if (featured == null) item {
                Box(Modifier.padding(end = if (edge) ReelLayout.Gutter else 0.dp)) { HomeHeader(state, onAccountClick, showBrand) }
            }
            if (showSearch) item(key = "search-entry") {
                Box(Modifier.padding(top = 8.dp)) {
                    HomeSearchEntry(onSearchClick, searchTransitionModifier)
                }
            }
            if (featured != null) {
                item(key = "tablet-feature") {
                    TabletLibraryFeature(featured, onLibraryClick,
                        Modifier.padding(bottom = 4.dp, end = if (edge) ReelLayout.Gutter else 0.dp),
                        account = { HomeAccountButton(state, onAccountClick, onArtwork = true) })
                }
            }
            if (HomeSection.NOW_PLAYING in state.homeSections && state.sessions.isNotEmpty()) {
                item {
                        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp, end = mediaEndInset()), verticalAlignment = Alignment.Bottom) {
                            SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_now_playing), Modifier.weight(1f))
                            // A count is status, not an action, so it stays out of the accent colour.
                            if (state.sessions.size > 1) Text(androidx.compose.ui.res.pluralStringResource(app.reelstack.R.plurals.home_playback_count, state.sessions.size, state.sessions.size),
                                color = Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
                        }
                        NowPlayingRail(
                            sessions = state.sessions,
                            pendingSessionKey = state.pendingSessionKey,
                            onOpen = onSessionClick,
                            onPlaybackToggle = onPlaybackToggle,
                        )
                }
            }
            if (HomeSection.CONTINUE_WATCHING in state.homeSections &&
                (state.resume.isNotEmpty() || (state.isRefreshing && state.configuredCount > 0))
            ) {
                item {
                    SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_continue), Modifier.padding(top = 24.dp, bottom = 13.dp))
                    if (state.resume.isEmpty()) LibraryRailSkeleton(stringResource(R.string.home_loading_resume), wide = true, tabletArtwork = false)
                    else ResumeRail(state.resume, onLibraryClick)
                }
            }
            run {
                mediaSources.forEach { source ->
                    val section = if (source == ServiceKind.EMBY) HomeSection.EMBY_MOVIES else HomeSection.JELLYFIN_MOVIES
                    if (section !in state.homeSections) return@forEach
                    item(key = "recent-movies-${source.name}") {
                        MediaSectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_new_movies), source, Modifier.padding(top = 24.dp, bottom = 13.dp))
                        val items = state.recentMovies.filter { it.source == source }
                        if (items.isEmpty()) {
                            if (state.isRefreshing) {
                                LibraryRailSkeleton(stringResource(R.string.home_loading_movies, source.displayName))
                            } else {
                                EmptySectionLine(mediaEmptyMessage(state, source, stringResource(R.string.home_no_movies)))
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
                        MediaSectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_new_episodes), source, Modifier.padding(top = 24.dp, bottom = 13.dp))
                        val items = state.recentSeries.filter { it.source == source }
                        if (items.isEmpty()) {
                            if (state.isRefreshing) {
                                LibraryRailSkeleton(
                                    stringResource(R.string.home_loading_series, source.displayName),
                                    wide = true,
                                )
                            } else {
                                EmptySectionLine(mediaEmptyMessage(state, source, stringResource(R.string.home_no_episodes)))
                            }
                        } else {
                            LibraryRail(items, onLibraryClick, wide = true)
                        }
                    }
                }
            }
            if (HomeSection.RECOMMENDATIONS in state.homeSections) {
                item {
                    SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_recommendations), Modifier.padding(top = 28.dp, bottom = 4.dp))
                    Text(stringResource(R.string.home_recommendations_note), color = Muted, fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp))
                    if (state.recommendations.isEmpty() && state.isRefreshing) {
                        RecommendationSkeleton()
                    } else if (state.recommendations.isEmpty()) {
                        EmptySectionLine(stringResource(R.string.home_recommendations_empty))
                    } else {
                        RecommendationRail(state.recommendations.take(8), onDiscoverClick)
                    }
                }
            }
            if (HomeSection.RECENT_RELEASES in state.homeSections) {
                item {
                    Column(Modifier.padding(top = 28.dp, bottom = 13.dp)) {
                        SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_recent_releases))
                        Text(
                            stringResource(R.string.home_releases_note),
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.recentReleases.isEmpty()) {
                        if (state.isRefreshing && state.configuredCount > 0) {
                            UpcomingSkeleton()
                        } else {
                            EmptySectionLine(state.recentReleasesError ?: stringResource(R.string.home_releases_empty))
                        }
                    } else {
                        RecentReleaseRail(state.recentReleases, onUpcomingClick)
                        state.recentReleasesError?.let { EmptySectionLine(it) }
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
                        if (state.isRefreshing && state.configuredCount > 0) {
                            UpcomingSkeleton()
                        } else {
                            EmptySectionLine(state.upcomingError ?: if (!hasQueueConnection)
                                stringResource(R.string.home_calendar_disconnected)
                            else stringResource(R.string.home_upcoming_empty))
                        }
                    } else {
                        UpcomingRail(state.upcoming, onUpcomingClick)
                        state.upcomingError?.let { EmptySectionLine(it) }
                    }
                }
            }
            item { HomeFreshness(state, onRefresh) }
        }
      }
    }
}

/**
 * Says how old the feed is. Without it a cache from yesterday is indistinguishable from live data.
 */
@Composable
private fun HomeFreshness(state: ReelstackUiState, onRefresh: () -> Unit) {
    if (state.configuredCount == 0) return
    val text = when {
        state.isRefreshing -> stringResource(R.string.home_refreshing)
        state.lastUpdatedEpochMillis != null -> stringResource(R.string.home_updated_at, Instant
            .ofEpochMilli(state.lastUpdatedEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm")))
        else -> stringResource(R.string.home_waiting_refresh)
    }
    app.reelstack.ui.components.FeedRefreshAction(text, state.isRefreshing, onRefresh,
        Modifier.fillMaxWidth().padding(top = 28.dp, end = mediaEndInset()))
}

@Composable
private fun HomeHeader(state: ReelstackUiState, onAccountClick: () -> Unit, showBrand: Boolean) {
    var appeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "home-header-reveal",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().testTag("home-header").graphicsLayer {
            alpha = reveal
            translationY = (1f - reveal) * 24f
        },
    ) {
            if (showBrand) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.spole_mark), "Spole-logo",
                    modifier = Modifier.size(34.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Primary))
                Text("Spole", color = TextColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.7).sp, modifier = Modifier.padding(start = 8.dp))
            } else Spacer(Modifier.weight(1f))
        HomeAccountButton(state, onAccountClick)
    }
}

@Composable
private fun HomeAccountButton(state: ReelstackUiState, onAccountClick: () -> Unit, onArtwork: Boolean = false) {
    val account = state.preferredHomeAccount()
    val connection = state.connections.firstOrNull { it.kind == account?.source }
    AccountAvatarButton(
            account = account,
            connection = connection,
            onClick = onAccountClick,
            description = account?.let { stringResource(R.string.home_open_account, it.displayName, it.source.displayName) }
                ?: stringResource(R.string.home_account_settings),
            testTag = "home-account",
            onArtwork = onArtwork,
        )
}

@Composable
private fun mediaEmptyMessage(state: ReelstackUiState, source: ServiceKind, emptyMessage: String): String =
    if (source in state.failedServices) {
        stringResource(R.string.home_source_failed, source.displayName)
    } else if (source in state.serviceWarnings) {
        stringResource(R.string.home_source_partial, source.displayName)
    } else {
        emptyMessage
    }

@Composable
private fun mediaEndInset() = if (app.reelstack.ui.theme.LocalMediaEdgeToEdge.current) ReelLayout.Gutter else 0.dp

/** One size for every section heading on Home. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextColor,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
    )
}

@Composable
private fun MediaSectionTitle(title: String, source: ServiceKind, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth().padding(end = mediaEndInset()).clearAndSetSemantics {
        heading()
        contentDescription = "$title · ${source.displayName}"
    }) {
        Text(
            text = title,
            color = TextColor,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        // Mark and label stay centred on each other; the pair sits on the heading's baseline.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
            ServiceLogo(
                kind = source,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = source.displayName,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
private fun RecommendationRail(items: List<DiscoverMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = DiscoverMedia::id) { media ->
            RecommendationCard(media) { onClick(media.id) }
        }
    }
}

@Composable
private fun RecommendationCard(media: DiscoverMedia, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val status = if (media.seerrStatus == null && !media.inLibrary && !media.requested) stringResource(R.string.media_check_availability)
        else app.reelstack.localization.localizedSeerrStatus(media.seerrStatus, media.inLibrary, media.requested)
    Box(
        Modifier
            .width(164.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale)
            .heightIn(min = 258.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale)
            .clip(RoundedCornerShape(16.dp))
            .focusOutline(interaction, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = LocalIndication.current,
                onClickLabel = stringResource(R.string.flow_detail_named, media.title), onClick = onClick)
            .semantics { role = Role.Button }
            .testTag("recommendation-${media.id}"),
    ) {
        MediaArtwork(media.artworkUrl, media.artworkRes, media.title, Modifier.matchParentSize(), ContentScale.Crop)
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .14f),
                    .42f to Color.Transparent,
                    .72f to Color.Black.copy(alpha = .72f),
                    1f to Color.Black.copy(alpha = .96f),
                ),
            ),
        )
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (media.isSeries) stringResource(R.string.media_series) else stringResource(R.string.media_movie),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .6.sp,
                modifier = Modifier.background(Color.Black.copy(alpha = .66f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            if (media.inLibrary || media.requested || media.seerrStatus in 2..6) {
                Icon(
                    if (media.inLibrary || media.seerrStatus == 5) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDone,
                    contentDescription = null,
                    tint = app.reelstack.ui.theme.Success,
                    modifier = Modifier.background(Color.Black.copy(alpha = .72f), CircleShape).padding(6.dp).size(18.dp),
                )
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 54.dp)) {
            Text(media.metadata, color = Color.White.copy(alpha = .78f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(media.title, color = Color.White, fontSize = 17.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp))
            Text(status, color = PrimarySoft, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun NowPlayingRail(
    sessions: List<PlaybackSession>,
    pendingSessionKey: String?,
    onOpen: (String) -> Unit,
    onPlaybackToggle: (String) -> Unit,
) {
    if (LocalTabletCanvas.current) {
        LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(sessions, key = PlaybackSession::key) { session ->
                app.reelstack.ui.components.CompactSessionCard(session, pendingSessionKey == session.key,
                    controlsLocked = pendingSessionKey != null, onOpen = { onOpen(session.key) },
                    onToggle = { onPlaybackToggle(session.key) }, modifier = Modifier.width(480.dp))
            }
        }
        return
    }
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
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
        // Minimum, not fixed: a locked height drops the quality line at a large font scale.
        modifier = modifier
            .heightIn(min = 292.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .focusOutline(interactionSource, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onOpen,
            ),
    ) {
        // matchParentSize keeps artwork and scrim out of the height measurement, so the text
        // column below decides how tall the card is once the font scale grows.
        MediaArtwork(
            url = session.artworkUrl,
            fallbackRes = if (session.sessionId?.startsWith("demo-") == true) R.drawable.session_still else R.drawable.media_placeholder,
            contentDescription = "${session.userName} ser på ${session.title}",
            contentScale = ContentScale.Crop,
            source = session.source,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0x5204050A),
                    0.44f to Color(0x7704050A),
                    1f to Color(0xF5080710),
                ),
            ),
        )

        Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp)) {
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
                    drawStopIndicator = {},
                    gapSize = 0.dp,
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
    // Every card in a rail reserves the same number of title lines, so a rail where each title
    // fits on one line does not leave an empty second line under every card.
    val titleLines = if (items.any { it.title.length > if (wide) 26 else 15 }) 2 else 1
    LazyRow(modifier = Modifier.fillMaxWidth().testTag("library-rail"), contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            LibraryCard(
                media = media,
                wide = wide,
                titleLines = titleLines,
                revealDelay = (index.coerceAtMost(2) * 30),
                onClick = { onClick(media.id) },
            )
        }
    }
}

/**
 * Partly watched titles. Each card keeps the shape of its own artwork — a film stays a poster and
 * an episode stays a wide still — while a shared artwork height keeps every title on one baseline.
 */
@Composable
private fun ResumeRail(items: List<LibraryMedia>, onClick: (String) -> Unit) {
    val titleLines = if (items.any { it.title.length > 20 }) 2 else 1
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> "resume-${media.id}" }) { index, media ->
            ResumeCard(media, titleLines, revealDelay = index.coerceAtMost(2) * 30) { onClick(media.id) }
        }
    }
}

@Composable
private fun ResumeCard(media: LibraryMedia, titleLines: Int, revealDelay: Int, onClick: () -> Unit) {
    val wide = !media.mediaType.equals("Movie", ignoreCase = true)
    val artworkHeight = ReelLayout.EpisodeHeight * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val cardWidth = if (wide) artworkHeight * 16f / 9f else artworkHeight * 2f / 3f
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var appeared by rememberSaveable(media.id) { mutableStateOf(false) }
    LaunchedEffect(media.id) { appeared = true }
    val reveal by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 240, delayMillis = revealDelay),
        label = "resume-card-reveal",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.7f),
        label = "resume-card-press",
    )
    val percent = ((media.progress ?: 0f).coerceIn(0f, 1f) * 100).toInt()
    val resumeLabel = stringResource(R.string.home_resume_description, media.title, percent)
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
                onClickLabel = resumeLabel,
            )
            .semantics { role = Role.Button }
            .testTag("resume-card-${media.id}"),
    ) {
        Box(Modifier.fillMaxWidth().height(artworkHeight).clip(RoundedCornerShape(ReelLayout.ArtworkCorner))
            .focusOutline(interactionSource, RoundedCornerShape(ReelLayout.ArtworkCorner))) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                source = media.source,
                modifier = Modifier.fillMaxSize(),
            )
            // How far in you are is the whole point of this rail, so it sits on the artwork
            // rather than competing with the title for a line of its own.
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp)
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Box(Modifier.fillMaxWidth((media.progress ?: 0f).coerceIn(0f, 1f)).fillMaxHeight().background(Primary))
            }
        }
        Text(
            media.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = titleLines,
            minLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            media.subtitle,
            color = Muted,
            fontSize = 12.sp,
            maxLines = 2,
            lineHeight = 17.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LibraryCard(media: LibraryMedia, wide: Boolean, titleLines: Int, revealDelay: Int, onClick: () -> Unit) {
    val tablet = LocalTabletCanvas.current
    val cardWidth = (if (wide) { if (tablet) 292.dp else ReelLayout.EpisodeWidth } else { if (tablet) 158.dp else ReelLayout.PosterWidth }) * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
    val artworkHeight = if (wide) cardWidth * 9f / 16f else cardWidth * 1.5f
    val artworkShape = RoundedCornerShape(ReelLayout.ArtworkCorner)
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
                .testTag("library-artwork-${media.id}")
                .clip(artworkShape).focusOutline(interactionSource, artworkShape),
        ) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                source = media.source,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            media.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis,
            // A long neighbouring title must not reserve a blank line in this card.
            minLines = 1,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            media.subtitle,
            color = Muted,
            fontSize = 12.sp,
            maxLines = if (wide) 2 else 1,
            lineHeight = 17.sp,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun UpcomingRail(items: List<UpcomingMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            UpcomingCard(media, revealDelay = index.coerceAtMost(2) * 30) { onClick(media.id) }
        }
    }
}

@Composable
private fun RecentReleaseRail(items: List<UpcomingMedia>, onClick: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(end = mediaEndInset()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(items, key = { _, media -> media.id }) { index, media ->
            UpcomingCard(media, revealDelay = index.coerceAtMost(2) * 30, recent = true) { onClick(media.id) }
        }
    }
}

@Composable
private fun UpcomingCard(media: UpcomingMedia, revealDelay: Int, recent: Boolean = false, onClick: () -> Unit) {
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
    Box(Modifier.width(280.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).heightIn(min = 226.dp * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale).graphicsLayer {
        alpha = reveal; translationY = (1f - reveal) * 18f; scaleX = scale; scaleY = scale
    }.clip(shape).background(SurfaceRaised).focusOutline(interactionSource, shape)
        .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
        .semantics { role = Role.Button }.testTag("upcoming-cover-${media.id}")) {
        MediaArtwork(media.artworkUrl, media.artworkRes, null, Modifier.matchParentSize(), ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .12f), .35f to Color.Transparent,
            .7f to Color.Black.copy(alpha = .62f), 1f to Color.Black.copy(alpha = .94f))))
        Row(Modifier.align(Alignment.TopStart).padding(14.dp).background(Color.Black.copy(alpha = .76f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = Primary, modifier = Modifier.size(14.dp))
            Text(media.dateLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 70.dp)) {
            Text(if (recent) {
                if (media.mediaType.equals("Movie", true)) stringResource(R.string.media_new_movie) else stringResource(R.string.media_new_episode)
            } else if (media.mediaType.equals("Movie", true)) stringResource(R.string.media_home_release) else stringResource(R.string.media_new_episode),
                color = Color.White.copy(alpha = .8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(media.title, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Text(media.subtitle.replace(" · TBA", ""), color = Color.White.copy(alpha = .85f),
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

/**
 * Title and calendar shortcut sit on one line; the explanatory line runs full width underneath so
 * it is not squeezed into a half-width column that wraps at the default font size.
 */
@Composable
private fun UpcomingSectionTitle(onCalendarClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(end = mediaEndInset())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SectionTitle(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_upcoming), Modifier.weight(1f).padding(end = 12.dp))
            Surface(
                onClick = onCalendarClick,
                color = Primary.copy(alpha = 0.18f),
                contentColor = PrimarySoft,
                shape = CircleShape,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(androidx.compose.ui.res.stringResource(app.reelstack.R.string.home_calendar), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Text(
            stringResource(R.string.home_upcoming_note),
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptySectionLine(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = mediaEndInset()))
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
            // Source is provenance, not an action, so it stays neutral.
            Text(media.source.displayName, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Icon(
                    if (media.state == IncomingState.DOWNLOADING) Icons.Rounded.Download else Icons.Rounded.Schedule,
                    contentDescription = null,
                    // Waiting in a queue is a normal state, not a problem the user must act on.
                    tint = if (media.state == IncomingState.DOWNLOADING) Primary else Muted,
                    modifier = Modifier.size(18.dp),
                )
                Text(media.status, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
            }
            media.progress?.let { progress ->
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp).clip(CircleShape).background(SurfaceRaised)) {
                    Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0f, 1f)).height(4.dp).background(Primary))
                }
            }
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = app.reelstack.ui.theme.Muted, modifier = Modifier.size(17.dp))
    }
}
