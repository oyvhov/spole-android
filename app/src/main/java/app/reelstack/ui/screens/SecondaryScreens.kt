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
import androidx.compose.material.icons.rounded.ArrowDropDown
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.reelstack.ui.components.AppFilterRow
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.canRequestType
import app.reelstack.data.model.seerrStatusLabel
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
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
import app.reelstack.data.model.activityDayGroup
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.isSeries
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.R
import app.reelstack.BuildConfig
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.ActivitySkeleton
import app.reelstack.ui.components.DiscoverSkeleton
import app.reelstack.ui.components.AccountAvatarButton
import app.reelstack.ui.components.verifiedPanelAccount
import app.reelstack.ui.components.SettingsAccounts
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor
import app.reelstack.ui.theme.Warning

/** Stable filter identities. The visible label is a property, never the state itself. */
enum class DiscoverFilter(val label: String) {
    ALL("Alt"), MOVIES("Filmar"), SERIES("Seriar")
}

enum class LibraryFilter(val label: String) {
    ALL("Alle titlar"), AVAILABLE("I biblioteket"), REQUESTABLE("Kan leggjast til")
}

enum class PersonalActivityFilter(val label: String) {
    ALL("Alle"), IN_PROGRESS("På veg"), READY("Klare")
}

enum class ActivityFilter(val label: String, val source: ServiceKind?) {
    MINE("Mine", null),
    ALL("Alt", null),
    SEERR("Seerr", ServiceKind.SEERR),
    RADARR("Radarr", ServiceKind.RADARR),
    SONARR("Sonarr", ServiceKind.SONARR),
}

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
    onLibraryDetails: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var filter by rememberSaveable { mutableStateOf(DiscoverFilter.ALL) }
    var libraryFilter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    val visible = state.visibleDiscover.filter { media ->
        val matchesType = when (filter) {
            DiscoverFilter.MOVIES -> !media.isSeries
            DiscoverFilter.SERIES -> media.isSeries
            DiscoverFilter.ALL -> true
        }
        matchesType && when (libraryFilter) {
            LibraryFilter.ALL -> true
            LibraryFilter.AVAILABLE -> media.inLibrary || media.seerrStatus == 5
            LibraryFilter.REQUESTABLE -> media.canRequest
        }
    }
    ReelPage {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
        contentPadding = screenPadding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().testTag("discover-grid"),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                val account = state.verifiedPanelAccount(ServiceKind.SEERR)?.takeIf { it.isPersonal }
                val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Oppdag", color = TextColor, style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.padding(top = 6.dp))
                        Text("Den neste historia di byrjar her.", color = Muted, fontSize = 13.sp,
                            lineHeight = 19.sp, modifier = Modifier.padding(top = 7.dp))
                    }
                    AccountAvatarButton(
                        account = account,
                        connection = connection,
                        onClick = onAccountClick,
                        description = account?.let { "Endre Seerr-kontoen til ${it.displayName}" }
                            ?: "Logg inn på Seerr",
                        testTag = "discover-account",
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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
                DiscoverFilterBar(filter, libraryFilter, { filter = it }, { libraryFilter = it },
                    Modifier.padding(top = 12.dp))
            }
        }
        if (state.librarySearchResults.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 4.dp)) {
                    Text("I biblioteka dine", color = TextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Alt her kan du sjå med ein gong.", color = Muted, fontSize = 12.sp,
                        lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                }
            }
            items(state.librarySearchResults, key = { "library-hit-${it.id}" }) { media ->
                LibraryHitCard(media) { onLibraryDetails(media.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Legg til noko nytt", color = TextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
        }
        if (state.isSearching || (state.isRefreshing && state.discover.isEmpty() && state.searchQuery.isBlank()
                && state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() })) {
            item(span = { GridItemSpan(maxLineSpan) }) { DiscoverSkeleton(Modifier.fillMaxWidth()) }
        } else if (visible.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    state.searchError ?: when {
                        state.searchQuery.isNotBlank() && state.librarySearchResults.isNotEmpty() ->
                            "Ingenting nytt å leggje til for «${state.searchQuery.trim()}»."
                        state.searchQuery.isNotBlank() -> "Ingen treff på «${state.searchQuery.trim()}». Prøv eit anna søk eller filter."
                        filter != DiscoverFilter.ALL || libraryFilter != LibraryFilter.ALL -> "Ingen titlar i dette filteret enno."
                        else -> "Ingen forslag enno. Kople til Seerr i Innstillingar for å oppdage nye titlar."
                    },
                    color = Muted, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            items(visible, key = DiscoverMedia::id) { media ->
                DiscoverCard(media, media.id in state.requestingMediaIds,
                    allowed = state.configuredCount == 0 || state.accounts[ServiceKind.SEERR]?.let { !it.isPersonal || it.canRequestType(if (media.isSeries) "tv" else "movie") } == true,
                    onRequest = { onRequest(media.id) }, onDetails = { onDetails(media.id) })
            }
            // Seerr answers 20 results at a time. Loading the next page is explicit rather than
            // automatic, so scrolling a long list never fires requests the reader did not ask for.
            if (state.searchHasMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = onLoadMore,
                        enabled = !state.loadingMoreSearch,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 52.dp)
                            .testTag("discover-load-more"),
                    ) {
                        if (state.loadingMoreSearch) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = Primary, strokeWidth = 2.dp)
                            Text("Hentar fleire…", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Text("Hent fleire treff")
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DiscoverFilterBar(
    type: DiscoverFilter,
    library: LibraryFilter,
    onType: (DiscoverFilter) -> Unit,
    onLibrary: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var statusOpen by rememberSaveable { mutableStateOf(false) }
    androidx.compose.foundation.lazy.LazyRow(
        modifier.fillMaxWidth().testTag("discover-filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(DiscoverFilter.entries, key = { "type-${it.name}" }) { option ->
            FilterChip(
                selected = option == type,
                onClick = { onType(option) },
                label = { Text(option.label, maxLines = 1) },
                shape = RoundedCornerShape(10.dp), border = null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceRaised, labelColor = Muted,
                    selectedContainerColor = Primary, selectedLabelColor = Ink,
                ),
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        }
        item(key = "library-status") {
            Box {
                FilterChip(
                    selected = library != LibraryFilter.ALL,
                    onClick = { statusOpen = true },
                    label = { Text(if (library == LibraryFilter.ALL) "Status" else library.label, maxLines = 1) },
                    trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(10.dp), border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceRaised, labelColor = Muted,
                        selectedContainerColor = Primary, selectedLabelColor = Ink,
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("discover-status-filter"),
                )
                DropdownMenu(expanded = statusOpen, onDismissRequest = { statusOpen = false },
                    containerColor = SurfaceRaised) {
                    LibraryFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            leadingIcon = if (option == library) {
                                { Icon(Icons.Rounded.Check, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                            } else null,
                            onClick = { onLibrary(option); statusOpen = false },
                        )
                    }
                }
            }
        }
    }
}

/** A title you already own: no request action, just the way into its details. */
@Composable
private fun LibraryHitCard(media: LibraryMedia, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .clickable(onClickLabel = "Vis detaljar for ${media.title}", onClick = onClick)
            .testTag("library-hit-${media.id}"),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)).background(SurfaceRaised)) {
            MediaArtwork(media.artworkUrl, media.artworkRes, null, Modifier.matchParentSize(),
                ContentScale.Crop, source = media.source)
            Row(
                Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Success, modifier = Modifier.size(12.dp))
                Text("I biblioteket", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.padding(start = 5.dp))
            }
        }
        Text(media.title, color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp))
        Text(media.subtitle, color = Muted, fontSize = 12.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DiscoverCard(media: DiscoverMedia, requesting: Boolean, onRequest: () -> Unit, onDetails: () -> Unit, allowed: Boolean = true) {
    val actionable = media.canRequest && allowed
    val statusLabel = seerrStatusLabel(media.seerrStatus, media.inLibrary, media.requested)
    Box(Modifier.fillMaxWidth().heightIn(min = 300.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceRaised)
        // The card's own action is named so a screen reader can tell it apart from the request
        // button inside it. It must not merge its descendants: that would swallow the button.
        .clickable(onClickLabel = "Vis detaljar for ${media.title}", onClick = onDetails)
        .testTag("discover-cover-${media.id}")) {
        MediaArtwork(media.artworkUrl, media.artworkRes, null, Modifier.matchParentSize(), ContentScale.Crop)
        Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to Color.Transparent, .35f to Color.Transparent, .68f to Color.Black.copy(alpha = .64f), 1f to Color.Black.copy(alpha = .96f))))
        // Badge at the top, text block anchored to the bottom. Alignment rather than a fixed
        // spacer, so the card grows with the font scale instead of clipping or leaving a gap.
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            // A type badge classifies, it does not act, so it stays off the accent colour.
            Text(if (media.isSeries) "SERIE" else "FILM", color = Color.White, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1,
                modifier = Modifier.background(Color.Black.copy(alpha = .68f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp))
            Spacer(Modifier.weight(1f))
            if (media.inLibrary || media.requested || media.seerrStatus in 2..6) {
                Icon(if (media.inLibrary || media.seerrStatus == 5) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDone,
                    // Labelled only when no status line follows below, so it is never read twice.
                    contentDescription = if (actionable) statusLabel else null,
                    tint = if (media.inLibrary || media.seerrStatus == 5) Success else Color.White,
                    modifier = Modifier.background(Color.Black.copy(alpha = .75f), CircleShape).padding(7.dp).size(19.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 58.dp)) {
            Text(media.metadata, color = Color.White.copy(alpha = .82f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(media.title, color = Color.White, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            if (actionable) {
                Button(onClick = onRequest, enabled = !requesting,
                    shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)
                        .semantics {
                            contentDescription = if (media.isSeries) "Vel sesongar av ${media.title}" else "Legg til ${media.title}"
                        }) {
                    if (requesting) CircularProgressIndicator(Modifier.size(14.dp), color = Ink, strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Add, null, Modifier.size(14.dp))
                    Text(when { requesting -> "Sender…"; media.isSeries -> "Vel sesongar"; else -> "Legg til" },
                        fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp))
                }
            } else {
                // Nothing to do here beyond opening the card, so this is a status line, not a button.
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(top = 8.dp)) {
                    Icon(if (media.inLibrary || media.seerrStatus == 5) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                        null, tint = if (media.inLibrary || media.seerrStatus == 5) Success else Muted, modifier = Modifier.size(13.dp))
                    Text(statusLabel, color = Color.White.copy(alpha = .82f), fontSize = 12.sp, maxLines = 2,
                        lineHeight = 16.sp, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
@Composable
fun ActivityScreen(state: ReelstackUiState, contentPadding: PaddingValues, onDetails: (String) -> Unit,
                   onNotify: (String, Boolean) -> Unit = { _, _ -> }, onRefresh: () -> Unit = {},
                   onAccountClick: () -> Unit = {}, onCancelRequest: (String) -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var savedSourceFilter by rememberSaveable {
        mutableStateOf(if (state.connections.any { it.kind == ServiceKind.SEERR && it.sessionCookie }) ActivityFilter.MINE else ActivityFilter.ALL)
    }
    val sourceFilter = if (state.adminView || state.configuredCount == 0) savedSourceFilter else ActivityFilter.MINE
    var personalFilter by rememberSaveable { mutableStateOf(PersonalActivityFilter.ALL) }
    val personalRequests = state.trackedRequests.filter {
        when (personalFilter) {
            PersonalActivityFilter.ALL -> true
            PersonalActivityFilter.READY -> it.stage == app.reelstack.data.model.RequestStage.AVAILABLE
            PersonalActivityFilter.IN_PROGRESS -> it.stage != app.reelstack.data.model.RequestStage.AVAILABLE
        }
    }
    val events = state.activity.filter { event ->
        sourceFilter == ActivityFilter.ALL || event.source == sourceFilter.source
    }
    val hasIssues = state.failedServices.isNotEmpty() || state.serviceWarnings.isNotEmpty()
    ReelPage {
    LazyColumn(contentPadding = screenPadding(contentPadding), modifier = Modifier.fillMaxSize().testTag("activity-feed")) {
        item {
            val account = state.verifiedPanelAccount(ServiceKind.SEERR)?.takeIf { it.isPersonal }
            val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    ScreenHeader("", "Aktivitet", "Det du har lagt til, frå førespurnad til klart.")
                }
                AccountAvatarButton(
                    account, connection, onAccountClick,
                    account?.let { "Endre Seerr-kontoen til ${it.displayName}" } ?: "Logg inn på Seerr",
                    "activity-account", Modifier.padding(top = 2.dp),
                )
            }
            if ((state.adminView || state.configuredCount == 0) && (state.isRefreshing || hasIssues || state.configuredCount == 0)) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (hasIssues) Caution else Muted))
                Text(when {
                    state.configuredCount == 0 -> "Førehandsvising med demodata"
                    state.isRefreshing -> "Oppdaterer…"
                    hasIssues -> "Noko kunne ikkje oppdaterast. Sjå Innstillingar."
                    else -> "Ventar på første oppdatering"
                }, color = if (hasIssues) Caution else Muted, fontSize = 13.sp, lineHeight = 19.sp,
                    modifier = Modifier.padding(start = 10.dp))
            }
            if (state.adminView || state.configuredCount == 0) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Vising", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    ActivityScopeMenu(sourceFilter, { savedSourceFilter = it })
                }
            }
        }
        if (sourceFilter == ActivityFilter.MINE) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Førespurnadene dine", color = TextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onRefresh, enabled = !state.trackingLoading,
                        modifier = Modifier.testTag("activity-refresh")) {
                        if (state.trackingLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                        else Icon(Icons.Rounded.Refresh, "Oppdater førespurnadene", tint = Primary)
                    }
                }
                state.trackingError?.let { Text(it, color = Caution, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)) }
                if (state.trackedRequests.isNotEmpty()) {
                    AppFilterRow(PersonalActivityFilter.entries, personalFilter, { choice ->
                        val count = when (choice) {
                            PersonalActivityFilter.ALL -> state.trackedRequests.size
                            PersonalActivityFilter.READY -> state.trackedRequests.count { it.stage == app.reelstack.data.model.RequestStage.AVAILABLE }
                            PersonalActivityFilter.IN_PROGRESS -> state.trackedRequests.count { it.stage != app.reelstack.data.model.RequestStage.AVAILABLE }
                        }
                        "${choice.label} · $count"
                    }, { personalFilter = it }, Modifier.padding(top = 4.dp, bottom = 12.dp))
                    if (personalRequests.isEmpty()) Text("Ingen førespurnader i dette filteret.", color = Muted,
                        modifier = Modifier.padding(vertical = 16.dp))
                }
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
            items(personalRequests, key = { "follow-${it.key}" }) { request ->
                app.reelstack.ui.TrackedRequestCard(
                    request,
                    { onDetails(request.key) },
                    { onNotify(request.key, it) },
                    onCancel = { onCancelRequest(request.key) },
                    cancelling = request.key in state.cancellingRequestKeys,
                )
            }
        } else if (events.isEmpty() && state.isRefreshing && state.configuredCount > 0) {
            item { ActivitySkeleton(Modifier.fillMaxWidth()) }
        } else if (events.isEmpty()) {
            item {
                Column(Modifier.padding(vertical = 24.dp)) {
                    Text("Ingen hendingar her enno", color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (sourceFilter == ActivityFilter.ALL) "Nye oppdateringar dukkar opp her." else "Prøv Alt for å sjå dei andre tenestene.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            item {
                Text("Tenesteaktivitet", color = TextColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp))
            }
            // Group by day so a long feed can be skimmed instead of read as one undifferentiated list.
            var lastGroup: String? = null
            events.forEach { event ->
                val group = activityDayGroup(event)
                if (group != lastGroup) {
                    lastGroup = group
                    item(key = "group-${event.id}") {
                        Text(group, color = Muted, fontSize = 11.sp, letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 20.dp, bottom = 2.dp).semantics { heading() })
                    }
                }
                item(key = event.id) { ActivityRow(event) { onDetails(event.id) } }
            }
        }
    }
    }
}

@Composable
private fun ActivityScopeMenu(selected: ActivityFilter, onSelect: (ActivityFilter) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("activity-scope")) {
            Text(selected.label, color = PrimarySoft)
            Icon(Icons.Rounded.ArrowDropDown, null, tint = PrimarySoft, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = SurfaceRaised) {
            ActivityFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Rounded.Check, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(event: ActivityEvent, onClick: () -> Unit) {
    // Episodes carry 16:9 stills and films carry 2:3 posters. A constant frame width keeps every
    // title on the same left edge while each format keeps its own shape and stays uncropped.
    val wide = event.mediaType?.equals("Episode", ignoreCase = true) == true
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    ) {
        MediaArtwork(
            url = event.artworkUrl,
            fallbackRes = event.artworkRes ?: R.drawable.media_placeholder,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(96.dp).aspectRatio(if (wide) 16f / 9f else 2f / 3f)
                .clip(RoundedCornerShape(10.dp)),
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
            // The detail line already names the service, so the meta line carries only the time.
            event.time.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 5.dp))
            }
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
    var homeExpanded by rememberSaveable { mutableStateOf(false) }
    // A visibility switch for a service that is not connected controls content that cannot exist,
    // so the row only appears once that service has an address.
    val connected = { kind: ServiceKind ->
        state.configuredCount == 0 || state.connections.any { it.kind == kind && it.baseUrl.isNotBlank() }
    }
    ReelPage {
    LazyColumn(
        contentPadding = screenPadding(contentPadding),
        modifier = Modifier.fillMaxSize().testTag("settings-feed"),
    ) {
        item {
            ScreenHeader(
                kicker = "App og tenester",
                title = "Innstillingar",
                lede = "Gjer Spole til ditt.",
            )
            SettingsAccounts(state, onAccountClick)
            SettingsSectionTitle("Tenestene dine")
            Text(
                "Tenaradresse og innlogging for kvar teneste.",
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        item {
            Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    state.connections.filter { state.canEditConnection(it.kind) }.forEach { connection ->
                        ServiceRow(connection = connection, onClick = { onConnectionClick(connection.kind) })
                    }
                }
            }
        }
        item {
            SettingsSectionTitle("Heimskjerm")
            Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp)) {
              Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth().clickable { homeExpanded = !homeExpanded }
                    .semantics { stateDescription = if (homeExpanded) "Utvida" else "Felt saman" }
                    .padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tv, null, tint = PrimarySoft, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Tilpass framsida", color = TextColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Vel rader frå kvart bibliotek", color = Muted, fontSize = 12.sp)
                    }
                    Icon(if (homeExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        null, tint = Muted)
                }
                androidx.compose.animation.AnimatedVisibility(visible = homeExpanded) {
                  Column {
            HomeSectionRow(HomeSection.NOW_PLAYING, "Spelar no", "Aktive avspelingar frå Jellyfin og Emby", Icons.Rounded.PlayArrow, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.CONTINUE_WATCHING, "Hald fram å sjå", "Halvsette filmar og episodar", Icons.Rounded.History, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.RECOMMENDATIONS, "Anbefalingar", "Felles liste frå GitHub", Icons.Rounded.Explore, state, onHomeSectionChange)
            HomeSectionRow(HomeSection.RECENT_RELEASES, "Nyleg tilgjengeleg", "Siste 28 dagar etter release-dato", Icons.Rounded.Schedule, state, onHomeSectionChange)
            if (connected(ServiceKind.JELLYFIN)) {
                HomeSectionRow(HomeSection.JELLYFIN_MOVIES, "Jellyfin · Filmar", "Nyleg lagde til filmar", Icons.Rounded.Movie, state, onHomeSectionChange)
                HomeSectionRow(HomeSection.JELLYFIN_SERIES, "Jellyfin · Seriar", "Nyleg lagde til episodar", Icons.Rounded.Tv, state, onHomeSectionChange)
            }
            if (connected(ServiceKind.EMBY)) {
                HomeSectionRow(HomeSection.EMBY_MOVIES, "Emby · Filmar", "Nyleg lagde til filmar", Icons.Rounded.Movie, state, onHomeSectionChange)
                HomeSectionRow(HomeSection.EMBY_SERIES, "Emby · Seriar", "Nyleg lagde til episodar", Icons.Rounded.Tv, state, onHomeSectionChange)
            }
            HomeSectionRow(HomeSection.UPCOMING, "Kjem snart", "Overvaka utgjevingar frå Radarr og Sonarr", Icons.Rounded.CalendarMonth, state, onHomeSectionChange)
                  }
                }
              }
            }
            SettingsSectionTitle("Varsel og oppdatering")
            Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp)) {
              Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            PreferenceRow(Icons.Rounded.Notifications, "Bibliotekvarsel", "For førespurnader du har valt å følgje",
                state.notificationsEnabled, onNotificationsChange)
            PreferenceRow(
                icon = Icons.Rounded.Wifi,
                label = "Synkroniser berre på Wi-Fi",
                description = "Bakgrunnsoppdatering på Wi-Fi",
                checked = state.wifiOnly,
                onCheckedChange = onWifiOnlyChange,
            )
              }
            }
            PrivacyCard(state)
            SettingsSectionTitle("Om appen")
            AppIdentity()
        }
    }
    }
}

/**
 * Says what is actually true about this device's connections instead of only asserting that
 * secrets are encrypted.
 */
@Composable
private fun PrivacyCard(state: ReelstackUiState) {
    val configured = state.connections.filter { it.baseUrl.isNotBlank() }
    val cleartext = configured.filter { it.baseUrl.startsWith("http://", ignoreCase = true) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceRaised,
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(17.dp)) {
            Icon(Icons.Rounded.Security, contentDescription = null, tint = Success,
                modifier = Modifier.padding(top = 1.dp).size(24.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text("Direkte og privat", color = TextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Tilgangsteikn er krypterte på denne eininga og blir aldri sende vidare.",
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                if (configured.isNotEmpty()) Text(
                    when {
                        cleartext.isEmpty() && configured.size == 1 -> "Tilkoplinga går over HTTPS."
                        cleartext.isEmpty() -> "Alle ${configured.size} tilkoplingane går over HTTPS."
                        else -> {
                            val scope = if (configured.size == 1) "Tilkoplinga" else "${cleartext.size} av ${configured.size} tilkoplingar"
                            "$scope går over HTTP: " + cleartext.joinToString(", ") { it.kind.displayName } +
                                ". Det er greitt på eige nett."
                        }
                    },
                    color = if (cleartext.isEmpty()) Muted else Caution,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp),
                )
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
            painter = painterResource(R.drawable.spole_mark),
            contentDescription = "Spole-logo",
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.padding(start = 13.dp)) {
            Text("Spole", color = TextColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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
    Text(text, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp, start = 4.dp).semantics { heading() })
}

@Composable
private fun ServiceRow(connection: ServiceConnection, onClick: () -> Unit) {
    val connected = connection.state == ConnectionState.CONNECTED
    val hasError = connection.state == ConnectionState.ERROR
    val hasWarning = connected && connection.detail?.startsWith("Tilkopla ·") == true
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
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
                    ConnectionState.CONNECTED -> if (hasWarning) connection.detail else "Tilkopla"
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
            if (connected && !hasWarning) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
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
        // Top-aligned: centring an icon against a block that can wrap to three lines leaves the
        // icon floating in the middle of the text instead of next to its label.
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).padding(vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceRaised),
        ) {
            Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(label, color = TextColor, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)
            description?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp)) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            // Lime marks the thumb, not the whole track: a list of switches should not read as
            // the loudest surface in the app when none of them is an action.
            colors = SwitchDefaults.colors(
                checkedThumbColor = Primary,
                checkedTrackColor = app.reelstack.ui.theme.SwitchTrackOn,
                checkedBorderColor = app.reelstack.ui.theme.SwitchTrackOn,
                uncheckedThumbColor = app.reelstack.ui.theme.Muted,
                uncheckedTrackColor = app.reelstack.ui.theme.SurfaceRaised,
                uncheckedBorderColor = app.reelstack.ui.theme.ControlOutline,
            ),
        )
    }
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
