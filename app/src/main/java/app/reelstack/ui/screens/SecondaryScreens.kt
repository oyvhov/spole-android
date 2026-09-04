package app.reelstack.ui.screens

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
import app.reelstack.BuildConfig
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.ActivitySkeleton
import app.reelstack.ui.components.DiscoverSkeleton
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@Composable
private fun ScreenHeader(kicker: String, title: String, lede: String) {
    Text(title, color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 6.dp))
    Text(lede, color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 7.dp))
}

private fun screenPadding(contentPadding: PaddingValues) = PaddingValues(
    start = 24.dp,
    top = 32.dp,
    end = 24.dp,
    bottom = contentPadding.calculateBottomPadding() + 24.dp,
)

@Composable
fun DiscoverScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onRequest: (String) -> Unit,
    onDetails: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var filter by rememberSaveable { mutableStateOf("Alt") }
    val visible = state.visibleDiscover.filter { media ->
        when (filter) {
            "Filmar" -> media.mediaType.equals("movie", true) || media.metadata.startsWith("Film")
            "Seriar" -> media.mediaType.equals("tv", true) || media.metadata.startsWith("Serie")
            else -> true
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
        contentPadding = screenPadding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenHeader("", "Oppdag", "Den neste historia di byrjar her.")
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.isSearching) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearch("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Tøm søket")
                            }
                        }
                    },
                    placeholder = { Text("Søk etter filmar og seriar", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = SurfaceRaised,
                        unfocusedContainerColor = SurfaceRaised,
                        cursorColor = Primary,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    listOf("Alt", "Filmar", "Seriar").forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option) },
                            shape = RoundedCornerShape(10.dp),
                            border = null,
                            colors = filterColors(),
                        )
                    }
                }
            }
        }
        if (state.isSearching || (state.isRefreshing && state.discover.isEmpty() && state.searchQuery.isBlank()
                && state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() })) {
            item(span = { GridItemSpan(maxLineSpan) }) { DiscoverSkeleton(Modifier.fillMaxWidth()) }
        } else if (visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    state.searchError ?: when {
                        state.searchQuery.isNotBlank() -> "Ingen treff på «${state.searchQuery.trim()}». Prøv eit anna søk eller filter."
                        filter != "Alt" -> "Ingen titlar i dette filteret enno."
                        else -> "Ingen forslag enno. Kople til Seerr i Innstillingar for å oppdage nye titlar."
                    },
                    color = Muted, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            items(visible, key = DiscoverMedia::id) { media ->
                DiscoverCard(media, media.id in state.requestingMediaIds,
                    onRequest = { onRequest(media.id) }, onDetails = { onDetails(media.id) })
            }
        }
    }
}

@Composable
private fun filterColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor = app.reelstack.ui.theme.Ink,
    containerColor = SurfaceRaised,
    labelColor = Muted,
)

@Composable
private fun DiscoverCard(media: DiscoverMedia, requesting: Boolean, onRequest: () -> Unit, onDetails: () -> Unit) {
    Column {
        Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onDetails)) {
            Box {
                MediaArtwork(
                    url = media.artworkUrl, fallbackRes = media.artworkRes, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
                )
                if (media.inLibrary || media.requested) {
                    Surface(
                        color = app.reelstack.ui.theme.Ink,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    ) {
                        Text(if (media.inLibrary) "I biblioteket" else "Lagd til",
                            color = Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }
            Text(media.title, color = TextColor, fontSize = 15.sp, lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp).heightIn(min = 38.dp))
            Text(media.metadata, color = Muted, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
        }
        TextButton(
            onClick = if (media.inLibrary || media.requested) onDetails else onRequest,
            enabled = !requesting,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            if (requesting) {
                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(if (media.inLibrary || media.requested) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Add,
                    contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(when {
                requesting -> "Legg til…"
                media.inLibrary || media.requested -> "Vis detaljar"
                else -> "Legg til"
            }, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
fun ActivityScreen(state: ReelstackUiState, contentPadding: PaddingValues, onDetails: (String) -> Unit) {
    val hasIssues = state.failedServices.isNotEmpty()
    val statusColor = if (hasIssues) Warning else Success
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenHeader(
                kicker = "I heile mediestakken",
                title = "Aktivitet",
                lede = "Nye titlar og nedlastingar samla i éi oversikt.",
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
                                state.isRefreshing -> "Oppdaterer tenestene"
                                state.configuredCount == 0 -> "Førehandsvising"
                                hasIssues -> "${state.failedServices.size} teneste${if (state.failedServices.size == 1) "" else "r"} må sjekkast"
                                else -> "Alle tenestene er på nett"
                            },
                            color = statusColor,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        if (state.configuredCount == 0) "Demo" else "${state.onlineCount}/${state.configuredCount} aktive",
                        color = Color(0xFFDBF9E9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (state.activity.isEmpty() && state.isRefreshing && state.connections.any {
                it.baseUrl.isNotBlank() && (it.kind == ServiceKind.SEERR || it.kind == ServiceKind.RADARR || it.kind == ServiceKind.SONARR)
            }) {
            item { ActivitySkeleton(Modifier.fillMaxWidth()) }
        } else if (state.activity.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceRaised.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ingen ny aktivitet. Alt er roleg.", color = Muted, modifier = Modifier.padding(22.dp))
                }
            }
        } else {
            items(state.activity, key = ActivityEvent::id) { event -> ActivityRow(event) { onDetails(event.id) } }
        }
    }
}

@Composable
private fun ActivityRow(event: ActivityEvent, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
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
            AppIdentity()
            ScreenHeader(
                kicker = "App og tenester",
                title = "Innstillingar",
                lede = "Tilkoplingar, val og personvern.",
            )
            SettingsSectionTitle("Tilkopla tenester")
        }
        items(state.connections, key = { it.kind }) { connection ->
            ServiceRow(connection = connection, onClick = { onConnectionClick(connection.kind) })
        }
        item {
            SettingsSectionTitle("Heimskjerm")
            Text(
                "Kvar teneste får sine eigne delar. Vel kva som skal visast på Heim.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HomeSectionRow(HomeSection.NOW_PLAYING, "Spelar no", "Aktive avspelingar frå Jellyfin og Emby", Icons.Rounded.PlayArrow, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.JELLYFIN_MOVIES, "Jellyfin · Filmar", "Nyleg lagde til filmar", Icons.Rounded.Movie, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.JELLYFIN_SERIES, "Jellyfin · Seriar", "Nyleg lagde til episodar", Icons.Rounded.Tv, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.EMBY_MOVIES, "Emby · Filmar", "Nyleg lagde til filmar", Icons.Rounded.Movie, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.EMBY_SERIES, "Emby · Seriar", "Nyleg lagde til episodar", Icons.Rounded.Tv, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.UPCOMING, "Kjem snart", "Overvaka utgjevingar frå Radarr og Sonarr", Icons.Rounded.Notifications, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.DOWNLOADS, "Nedlastingar", "Aktive køar i Radarr og Sonarr", Icons.Rounded.Download, state, onHomeSectionChange)
            SettingsSectionTitle("Val")
            PreferenceRow(
                icon = Icons.Rounded.Notifications,
                label = "Aktivitetsvarsel",
                description = "Varsle når nye titlar og nedlastingar endrar seg",
                checked = state.notificationsEnabled,
                onCheckedChange = onNotificationsChange,
            )
            PreferenceRow(
                icon = Icons.Rounded.Wifi,
                label = "Synkroniser berre på Wi-Fi",
                description = "Avgrens bakgrunnsoppdatering til nett utan datakostnad",
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
                        Text("Direkte og privat", color = Color(0xFFE1F8EB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("API-nøklane er krypterte på denne eininga.", color = Color(0xFFB8CFC2), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIdentity() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "HomeReel-logo",
            modifier = Modifier.size(48.dp),
        )
        Column(Modifier.padding(start = 13.dp)) {
            Text("HomeReel", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Personleg medieoversikt · v${BuildConfig.VERSION_NAME}",
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
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
    val hasWarning = connected && connection.detail?.startsWith("Tilkopla ·") == true
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Primary.copy(alpha = 0.17f)),
        ) {
            if (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY) {
                ServiceLogo(
                    kind = connection.kind,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    if (connection.kind == ServiceKind.SEERR) Icons.Rounded.CloudDone else Icons.Rounded.Movie,
                    contentDescription = null,
                    tint = PrimarySoft,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(connection.kind.displayName, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                when (connection.state) {
                    ConnectionState.CONNECTED -> connection.detail ?: "Tilkopla"
                    ConnectionState.TESTING -> "Sjekkar tilkoplinga…"
                    ConnectionState.ERROR -> connection.detail ?: "Må sjekkast · Trykk for å rette"
                    ConnectionState.DEMO -> "Kople til ${connection.kind.displayName}"
                },
                color = when {
                    hasWarning -> Caution
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
                hasWarning -> Caution
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
