package app.reelstack.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.R
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@Composable
private fun ScreenHeader(kicker: String, title: String, lede: String) {
    Text(kicker.uppercase(), color = PrimarySoft, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
    Text(title, color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 6.dp))
    Text(lede, color = Color(0xFFBBB2CB), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 7.dp))
}

private fun screenPadding(contentPadding: PaddingValues) = PaddingValues(
    start = 24.dp,
    top = 58.dp,
    end = 24.dp,
    bottom = contentPadding.calculateBottomPadding() + 24.dp,
)

@Composable
fun DiscoverScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onRequest: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenHeader(
                kicker = "Jellyfin + Seerr",
                title = "Discover",
                lede = "Search your library and request what is missing.",
            )
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Movies, shows, people") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(21.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary.copy(alpha = 0.72f),
                    unfocusedBorderColor = Color(0x35DCCDF9),
                    focusedContainerColor = SurfaceRaised.copy(alpha = 0.95f),
                    unfocusedContainerColor = SurfaceRaised.copy(alpha = 0.9f),
                    cursorColor = Primary,
                    focusedLeadingIconColor = PrimarySoft,
                    unfocusedLeadingIconColor = PrimarySoft,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 27.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("For you") }, colors = filterColors())
                FilterChip(selected = false, onClick = {}, label = { Text("Movies") }, colors = filterColors())
                FilterChip(selected = false, onClick = {}, label = { Text("Series") }, colors = filterColors())
            }
        }

        if (state.visibleDiscover.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceRaised.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26E2D5FF)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("No matches yet. Try a broader title.", color = Muted, modifier = Modifier.padding(30.dp))
                }
            }
        } else {
            items(state.visibleDiscover, key = DiscoverMedia::id) { media ->
                DiscoverCard(
                    media = media,
                    requesting = media.id in state.requestingMediaIds,
                    onRequest = { onRequest(media.id) },
                )
            }
        }
    }
}

@Composable
private fun filterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor = Color(0xFF120B1C),
    containerColor = SurfaceRaised.copy(alpha = 0.82f),
    labelColor = Muted,
)

@Composable
private fun DiscoverCard(media: DiscoverMedia, requesting: Boolean, onRequest: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp),
        color = SurfaceRaised.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22E2D5FF)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(13.dp)) {
            MediaArtwork(
                url = media.artworkUrl,
                fallbackRes = media.artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 116.dp, height = 158.dp).clip(RoundedCornerShape(18.dp)),
            )
            Column(modifier = Modifier.height(158.dp).weight(1f).padding(start = 17.dp)) {
                Text(
                    if (media.inLibrary) "IN YOUR LIBRARY" else "AVAILABLE TO REQUEST",
                    color = if (media.inLibrary) Success else PrimarySoft,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(media.title, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 7.dp))
                Text(media.metadata, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.weight(1f))
                if (media.inLibrary) {
                    androidx.compose.material3.TextButton(onClick = {}) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Play", modifier = Modifier.padding(start = 5.dp))
                    }
                } else {
                    Button(
                        onClick = onRequest,
                        enabled = !media.requested && !requesting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color(0xFF110B1A),
                            disabledContainerColor = Success.copy(alpha = 0.16f),
                            disabledContentColor = Success,
                        ),
                    ) {
                        if (requesting) {
                            CircularProgressIndicator(color = Color(0xFF110B1A), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(if (media.requested) Icons.Rounded.Check else Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            when {
                                requesting -> "Sending…"
                                media.requested -> "Requested"
                                else -> "Request"
                            },
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityScreen(state: ReelstackUiState, contentPadding: PaddingValues) {
    val hasIssues = state.failedServices.isNotEmpty()
    val statusColor = if (hasIssues) Warning else Success
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenHeader(
                kicker = "Across your stack",
                title = "Activity",
                lede = "Requests and downloads, translated into one clear flow.",
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (hasIssues) Color(0xB3342920) else Color(0xB31E302B),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 18.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 17.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = statusColor, modifier = Modifier.size(19.dp))
                        Text(
                            when {
                                state.isRefreshing -> "Refreshing services"
                                state.configuredCount == 0 -> "Preview activity"
                                hasIssues -> "${state.failedServices.size} service${if (state.failedServices.size == 1) "" else "s"} need attention"
                                else -> "All services online"
                            },
                            color = statusColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        if (state.configuredCount == 0) "Demo" else "${state.onlineCount}/${state.configuredCount} live",
                        color = Color(0xFFDBF9E9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (state.activity.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceRaised.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("No recent activity. Your stack is quiet.", color = Muted, modifier = Modifier.padding(22.dp))
                }
            }
        } else {
            items(state.activity, key = ActivityEvent::id) { event -> ActivityRow(event) }
        }
    }
}

@Composable
private fun ActivityRow(event: ActivityEvent) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(modifier = Modifier.size(width = 72.dp, height = 96.dp)) {
            MediaArtwork(
                url = event.artworkUrl,
                fallbackRes = event.artworkRes ?: R.drawable.session_still,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)),
            )
            Surface(
                color = if (event.complete) Success else Primary,
                contentColor = Color(0xFF130D1B),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).size(27.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (event.complete) Icons.Rounded.Check else Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(event.time, color = Color(0xFF888093), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(event.title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(event.detail, color = Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            event.progress?.let { progress ->
                Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp).clip(CircleShape).background(Color(0x24C6B1E7))) {
                    Box(Modifier.fillMaxWidth((progress / 100f).coerceIn(0f, 1f)).height(6.dp).background(Primary))
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onHomeSectionChange: (HomeSection, Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenHeader(
                kicker = "HomeReel",
                title = "Settings",
                lede = "Connections, preferences, and privacy.",
            )
            SettingsSectionTitle("Connected services")
        }
        items(state.connections, key = { it.kind }) { connection ->
            ServiceRow(connection = connection, onClick = { onConnectionClick(connection.kind) })
        }
        item {
            SettingsSectionTitle("Home screen")
            Text(
                "Every connected service contributes automatically. Choose which sections stay on Home.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HomeSectionRow(HomeSection.NOW_PLAYING, "Now playing", "Active sessions from Jellyfin and Emby", Icons.Rounded.PlayArrow, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.CONTINUE_WATCHING, "Continue watching", "Resume items from both media servers", Icons.Rounded.Tv, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.RECENTLY_ADDED, "Recently added", "New library items from Jellyfin and Emby", Icons.Rounded.Movie, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.UPCOMING, "Upcoming", "Monitored releases from Radarr and Sonarr", Icons.Rounded.Notifications, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.DOWNLOADS, "Downloads", "Current Radarr and Sonarr queues", Icons.Rounded.Download, state, onHomeSectionChange)
            SettingsSectionTitle("Preferences")
            PreferenceRow(
                icon = Icons.Rounded.Notifications,
                label = "Activity notifications",
                description = "Notify when requests and downloads change",
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationsChange,
            )
            PreferenceRow(
                icon = Icons.Rounded.Wifi,
                label = "Sync on Wi-Fi only",
                description = "Limit background refresh to unmetered networks",
                checked = state.wifiOnly,
                onCheckedChange = onWifiOnlyChange,
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xA01E302B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Success.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(17.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Direct and private", color = Color(0xFFE1F8EB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("API credentials are encrypted on this device.", color = Color(0xFFB8CFC2), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(text, color = PrimarySoft, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
}

@Composable
private fun ServiceRow(connection: ServiceConnection, onClick: () -> Unit) {
    val connected = connection.state == ConnectionState.CONNECTED
    val hasError = connection.state == ConnectionState.ERROR
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Primary.copy(alpha = 0.17f)),
        ) {
            Icon(
                when (connection.kind) {
                    ServiceKind.JELLYFIN, ServiceKind.EMBY -> Icons.Rounded.Tv
                    ServiceKind.SEERR -> Icons.Rounded.CloudDone
                    ServiceKind.RADARR, ServiceKind.SONARR -> Icons.Rounded.Movie
                },
                contentDescription = null,
                tint = PrimarySoft,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(connection.kind.displayName, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when (connection.state) {
                    ConnectionState.CONNECTED -> connection.detail ?: "Connected"
                    ConnectionState.TESTING -> "Checking connection…"
                    ConnectionState.ERROR -> connection.detail ?: "Needs attention · Tap to fix"
                    ConnectionState.DEMO -> "Demo data · Tap to connect"
                },
                color = when {
                    connected -> Success
                    hasError -> Warning
                    else -> Muted
                },
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            if (connected) Icons.Rounded.CheckCircle else Icons.Rounded.Dns,
            contentDescription = null,
            tint = when {
                connected -> Success
                hasError -> Warning
                else -> PrimarySoft
            },
            modifier = Modifier.size(22.dp),
        )
    }
    HorizontalDivider(color = Color(0x20E2D5FF))
}

@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Primary.copy(alpha = 0.17f)),
        ) {
            Icon(icon, contentDescription = null, tint = PrimarySoft, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF150E1E),
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color(0xFFBBB4C5),
                uncheckedTrackColor = Color(0xFF3A3445),
            ),
        )
    }
    HorizontalDivider(color = Color(0x20E2D5FF))
}

@Composable
private fun HomeSectionRow(
    section: HomeSection,
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    state: ReelstackUiState,
    onChange: (HomeSection, Boolean) -> Unit,
) {
    PreferenceRow(
        icon = icon,
        label = label,
        description = description,
        checked = section in state.homeSections,
        onCheckedChange = { onChange(section, it) },
    )
}
