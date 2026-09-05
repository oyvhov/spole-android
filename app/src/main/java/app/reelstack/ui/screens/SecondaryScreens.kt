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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import app.reelstack.ui.components.AppFilterRow
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.seerrStatusLabel
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
import androidx.compose.ui.platform.testTag
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
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SettingsAccounts
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

@Composable
private fun ScreenHeader(kicker: String, title: String, lede: String) {
    Text(title, color = TextColor, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 6.dp))
    Text(lede, color = Muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 7.dp))
}

private fun screenPadding(contentPadding: PaddingValues) = PaddingValues(
    start = ReelLayout.Gutter,
    top = ReelLayout.PageTop,
    end = ReelLayout.Gutter,
    bottom = contentPadding.calculateBottomPadding() + 24.dp,
)

@Composable
fun DiscoverScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onRequest: (String) -> Unit,
    onDetails: (String) -> Unit,
    onAccountClick: () -> Unit = {},
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
        modifier = Modifier.fillMaxSize().testTag("discover-grid"),
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
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
                AppFilterRow(listOf("Alt", "Filmar", "Seriar"), filter, { filter = it }, Modifier.padding(top = 12.dp))
                RequestIdentity(state, onSignIn = onAccountClick)
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
private fun DiscoverCard(media: DiscoverMedia, requesting: Boolean, onRequest: () -> Unit, onDetails: () -> Unit) {
    Column {
        Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onDetails)) {
            MediaArtwork(
                url = media.artworkUrl, fallbackRes = media.artworkRes, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp)),
            )
            Text(media.title, color = TextColor, fontSize = 15.sp, lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp))
            Text(media.metadata, color = Muted, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            if (media.inLibrary || media.requested || media.seerrStatus in 2..6) {
                Text(
                    seerrStatusLabel(media.seerrStatus, media.inLibrary, media.requested),
                    color = Primary, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        TextButton(
            onClick = if (!media.canRequest) onDetails else onRequest,
            enabled = !requesting,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            if (requesting) {
                CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                Icon(if (!media.canRequest) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Add,
                    contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(when {
                requesting -> "Legg til…"
                !media.canRequest -> "Vis detaljar"
                media.mediaType == "tv" -> "Vel sesongar"
                else -> "Legg til"
            }, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
fun ActivityScreen(state: ReelstackUiState, contentPadding: PaddingValues, onDetails: (String) -> Unit,
                   onNotify: (String, Boolean) -> Unit = { _, _ -> }, onRefresh: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var sourceFilter by rememberSaveable { mutableStateOf(if (state.connections.any { it.kind == ServiceKind.SEERR && it.sessionCookie }) "Mine" else "Alt") }
    val events = state.activity.filter { sourceFilter == "Alt" || it.source?.displayName == sourceFilter }
    val hasIssues = state.failedServices.isNotEmpty() || state.serviceWarnings.isNotEmpty()
    LazyColumn(contentPadding = screenPadding(contentPadding), modifier = Modifier.fillMaxSize().testTag("activity-feed")) {
        item {
            ScreenHeader("", "Aktivitet", "Følg titlane frå lagde til til klare.")
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                Box(Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape)
                    .background(if (hasIssues) Caution else Muted))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(when {
                        state.configuredCount == 0 -> "Førehandsvising med demodata"
                        state.isRefreshing -> "Oppdaterer…"
                        hasIssues -> "Noko kunne ikkje oppdaterast. Sjå Innstillingar."
                        state.lastUpdatedEpochMillis != null -> "Sist oppdatert kl. " + Instant.ofEpochMilli(state.lastUpdatedEpochMillis)
                            .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
                        else -> "Ventar på første oppdatering"
                    }, color = if (hasIssues) Caution else Muted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            AppFilterRow(listOf("Mine", "Alt", "Seerr", "Radarr", "Sonarr"), sourceFilter, { sourceFilter = it }, Modifier.padding(bottom = 12.dp))
        }
        if (sourceFilter == "Mine") {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Førespurnadene dine", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRefresh, enabled = !state.trackingLoading) { Text(if (state.trackingLoading) "Sjekkar…" else "Oppdater") }
                }
                state.trackingError?.let { Text(it, color = Caution, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)) }
                if (state.trackedRequests.any { it.notify } && (!state.notificationsEnabled || !app.reelstack.background.LibraryNotifications.allowed(context))) {
                    Text(if (!state.notificationsEnabled) "Appvarsel er av i Innstillingar. Du kan framleis følgje status her." else
                        "Android tillèt ikkje varsel no. Slå dei på for å få beskjed når innhaldet er klart.", color = Caution, fontSize = 12.sp)
                    if (state.notificationsEnabled) TextButton(onClick = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }) { Text("Opne varselinnstillingar") }
                }
                if (state.trackedRequests.isEmpty()) Text(if (state.trackingLoading) "Hentar førespurnadene dine…" else
                    "Førespurnader frå den personlege Seerr-kontoen din kjem her. Finn ein tittel i Oppdag for å starte.",
                    color = Muted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(vertical = 18.dp))
            }
            items(state.trackedRequests, key = { "follow-${it.key}" }) { request ->
                app.reelstack.ui.TrackedRequestCard(request, { onDetails(request.key) }, { onNotify(request.key, it) })
            }
        } else if (events.isEmpty() && state.isRefreshing && state.configuredCount > 0) {
            item { ActivitySkeleton(Modifier.fillMaxWidth()) }
        } else if (events.isEmpty()) {
            item {
                Column(Modifier.padding(vertical = 24.dp)) {
                    Text("Ingen hendingar her enno", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (sourceFilter == "Alt") "Nye oppdateringar dukkar opp her." else "Prøv Alt for å sjå dei andre tenestene.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            items(events, key = ActivityEvent::id) { event -> ActivityRow(event) { onDetails(event.id) } }
        }
    }
}

@Composable
private fun ActivityRow(event: ActivityEvent, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        MediaArtwork(
            url = event.artworkUrl,
            fallbackRes = event.artworkRes ?: R.drawable.media_placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 64.dp, height = 96.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(event.title, color = TextColor, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    if (event.complete) Icons.Rounded.Check else if (event.source == ServiceKind.SEERR) Icons.Rounded.CloudDone else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = if (event.complete) Success else Primary,
                    modifier = Modifier.padding(top = 1.dp).size(14.dp),
                )
                Text(event.detail, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.weight(1f).padding(start = 6.dp))
            }
            Text(
                listOfNotNull(event.time.takeIf { it.isNotBlank() }, event.source?.displayName).joinToString(" · "),
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            event.progress?.let { progress ->
                Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(6.dp).clip(CircleShape).background(app.reelstack.ui.theme.SurfaceRaised)) {
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
    onAccountClick: (ServiceKind) -> Unit = onConnectionClick,
) {
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        modifier = Modifier.fillMaxSize().testTag("settings-feed"),
    ) {
        item {
            ScreenHeader(
                kicker = "App og tenester",
                title = "Innstillingar",
                lede = "Tilkoplingar, val og personvern.",
            )
            SettingsAccounts(state, onAccountClick)
            SettingsSectionTitle("Tenestene dine")
        }
        items(state.connections, key = { it.kind }) { connection ->
            ServiceRow(connection = connection, onClick = { onConnectionClick(connection.kind) })
        }
        item {
            SettingsSectionTitle("Heimskjerm")
            Text(
                "Vel kva som skal visast på Heim.",
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
            Text("Varsel", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
            Text("Kjem seinare. Sjå oppdateringar under Aktivitet.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            PreferenceRow(
                icon = Icons.Rounded.Wifi,
                label = "Synkroniser berre på Wi-Fi",
                description = "Bakgrunnsoppdatering på Wi-Fi",
                checked = state.wifiOnly,
                onCheckedChange = onWifiOnlyChange,
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceRaised,
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(17.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Direkte og privat", color = app.reelstack.ui.theme.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("API-nøklane er krypterte på denne eininga.", color = app.reelstack.ui.theme.Muted, fontSize = 12.sp)
                    }
                }
            }
            SettingsSectionTitle("Om appen")
            AppIdentity()
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
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.padding(start = 13.dp)) {
            Text("HomeReel", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Personleg medieoversikt · v${BuildConfig.VERSION_NAME}",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(text, color = TextColor, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
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
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceRaised),
        ) {
            ServiceSymbol(connection.kind, Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(connection.kind.displayName, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
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
    HorizontalDivider(color = app.reelstack.ui.theme.Divider)
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
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).padding(vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceRaised),
        ) {
            Icon(icon, contentDescription = null, tint = PrimarySoft, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = app.reelstack.ui.theme.Ink,
                checkedTrackColor = Primary,
                uncheckedThumbColor = app.reelstack.ui.theme.Muted,
                uncheckedTrackColor = app.reelstack.ui.theme.SurfaceRaised,
            ),
        )
    }
    HorizontalDivider(color = app.reelstack.ui.theme.Divider)
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
