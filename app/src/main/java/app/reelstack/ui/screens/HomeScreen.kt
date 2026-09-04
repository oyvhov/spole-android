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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.TvOff
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
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ConnectionState
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onServerClick: () -> Unit,
    onSessionClick: () -> Unit,
    onPlaybackToggle: () -> Unit,
    onMediaClick: (String) -> Unit,
    onLibraryClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
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
                Header(
                    serverName = state.selectedConnection?.name ?: "Home server",
                    serverOnline = state.selectedConnection?.state == ConnectionState.CONNECTED,
                    onServerClick = onServerClick,
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Good evening",
                    color = TextColor,
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = state.syncSummary,
                    color = Color(0xFFBBB2CB),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                SectionTitle("Now playing", Modifier.padding(top = 28.dp, bottom = 15.dp))
                if (state.session != null) {
                    NowPlayingCard(
                        state = state,
                        onOpen = onSessionClick,
                        onPlaybackToggle = onPlaybackToggle,
                    )
                } else {
                    EmptyNowPlayingCard()
                }
                SectionTitle("Coming in", Modifier.padding(top = 26.dp, bottom = 10.dp))
                if (state.incoming.isEmpty()) {
                    EmptyIncomingCard()
                }
            }
            items(state.incoming, key = IncomingMedia::id) { media ->
                IncomingRow(media = media, onClick = { onMediaClick(media.id) })
            }
            if (state.continueWatching.isNotEmpty()) {
                item {
                    SectionTitle("Continue watching", Modifier.padding(top = 25.dp, bottom = 13.dp))
                    LibraryRail(state.continueWatching, onLibraryClick)
                }
            }
            if (state.recentlyAdded.isNotEmpty()) {
                item {
                    SectionTitle("Recently added", Modifier.padding(top = 25.dp, bottom = 13.dp))
                    LibraryRail(state.recentlyAdded, onLibraryClick)
                }
            }
        }
    }
}

@Composable
private fun Header(serverName: String, serverOnline: Boolean, onServerClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.MovieFilter,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "Reelstack",
                color = TextColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Surface(
            onClick = onServerClick,
            color = Color(0xDD14111D),
            contentColor = TextColor,
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x47E0D5FF)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            ) {
                Text(serverName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (serverOnline) Success else Primary))
                Spacer(Modifier.width(5.dp))
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Choose server", modifier = Modifier.size(17.dp))
            }
        }
    }
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
private fun NowPlayingCard(
    state: ReelstackUiState,
    onOpen: () -> Unit,
    onPlaybackToggle: () -> Unit,
) {
    val session = state.session ?: return
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        animationSpec = spring(stiffness = 100f, dampingRatio = 0.82f),
        label = "playback-progress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
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
            fallbackRes = R.drawable.session_still,
            contentDescription = "${session.userName} watching ${session.title}",
            contentScale = ContentScale.Crop,
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
            Text("${session.userName} is watching", color = PrimarySoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(session.title, color = Color.White, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1.5).sp)
            Text(session.subtitle, color = Color(0xFFB2A9C1), fontSize = 17.sp, modifier = Modifier.padding(top = 2.dp))
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
                    shape = CircleShape,
                    color = Primary,
                    contentColor = Color(0xFF110B19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.76f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.playbackControlPending) {
                            CircularProgressIndicator(
                                color = Color(0xFF110B19),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(23.dp),
                            )
                        } else {
                            AnimatedContent(session.paused, label = "play-pause") { paused ->
                                Icon(
                                    if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = if (paused) "Resume playback" else "Pause playback",
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
                    Icon(Icons.Rounded.Tune, contentDescription = "Playback details", tint = TextColor)
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
                url = null,
                fallbackRes = media.artworkRes,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
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
                Text(
                    media.source.displayName.take(1),
                    color = PrimarySoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
private fun EmptyNowPlayingCard() {
    Surface(
        color = SurfaceRaised.copy(alpha = 0.88f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x32E2D5FF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(22.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(50.dp).clip(CircleShape).background(Primary.copy(alpha = 0.15f)),
            ) {
                Icon(Icons.Rounded.TvOff, contentDescription = null, tint = PrimarySoft)
            }
            Column(Modifier.padding(start = 15.dp)) {
                Text("Nothing playing right now", color = TextColor, fontWeight = FontWeight.SemiBold)
                Text("A live session will appear here automatically.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun EmptyIncomingCard() {
    Surface(
        color = SurfaceRaised.copy(alpha = 0.78f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24E2D5FF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Your Radarr and Sonarr queues are clear.", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(20.dp))
    }
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
