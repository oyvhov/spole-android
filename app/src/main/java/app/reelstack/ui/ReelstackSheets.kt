package app.reelstack.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import app.reelstack.data.model.ContentDetails
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.data.model.resolvedMediaType
import app.reelstack.data.model.canRequest
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import app.reelstack.data.network.EndpointValidator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.model.canRequestType
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.Success
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Warning
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.DetailTextSkeleton
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.ServiceLogo
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.components.StableSheetDialog
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints

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
    onBackToCalendar: () -> Unit = {},
    onSeerrAccount: () -> Unit = {},
    onRequestSeason: (Int, Boolean) -> Unit = { _, _ -> },
    onRequestNotification: (Boolean) -> Unit = {},
    onConfirmRequest: () -> Unit = {},
    onCompanionLoginChange: (Boolean, String) -> Unit = { _, _ -> },
    onConnectionAlternateUrlChange: (String) -> Unit = {},
) {
    val sheet = state.activeSheet ?: return
    val sheetContentStates = rememberSaveableStateHolder()
    StableSheetDialog(dismissEnabled = state.requestDraft?.sending != true, onDismiss = onDismiss) { entered, closing, close ->
        val detailScroll = androidx.compose.runtime.key(sheet) { rememberScrollState() }
        Column(Modifier.fillMaxSize().testTag("sheet-viewport")) {
            if (sheet is AppSheet.TitleDetails || sheet is AppSheet.SessionDetails) {
                SheetToolbar(
                    title = if (sheet is AppSheet.SessionDetails) "Avspeling" else "Detaljar",
                    closeDescription = "Lukk detaljane", onClose = close, enabled = !closing,
                    onBack = if (state.returnToCalendar) onBackToCalendar else null,
                )
            }
            Box(
                Modifier.weight(1f).fillMaxWidth(),
            ) {
            when (sheet) {
                AppSheet.RequestComposer -> RequestComposer(state, onRequestSeason, onRequestNotification,
                    onConfirmRequest, close, { state.requestDraft?.media?.id?.let(onAddMedia) }, onSeerrAccount)
                is AppSheet.SessionDetails -> SessionSheet(state, sheet.sessionKey, onPlaybackToggle, detailScroll)
                is AppSheet.TitleDetails -> state.contentDetails?.let { details ->
                    androidx.compose.runtime.key(details.key) {
                        RichTitleDetailsSheet(state = state, onAddMedia = onAddMedia,
                            onSeerrAccount = onSeerrAccount, scroll = detailScroll, entered = entered)
                    }
                }
                AppSheet.UpcomingCalendar -> sheetContentStates.SaveableStateProvider("calendar") {
                    UpcomingCalendarSheet(state.upcoming, onUpcomingClick, close)
                }
                is AppSheet.ConnectionEditor -> connectionDraft?.let {
                    ConnectionEditorSheet(
                        draft = it,
                        configured = state.connections.firstOrNull { item -> item.kind == it.kind }?.baseUrl?.isNotBlank() == true,
                        onDismiss = close,
                        onNameChange = onConnectionNameChange,
                        onUrlChange = onConnectionUrlChange,
                        onTokenChange = onConnectionTokenChange,
                        onUserIdChange = onConnectionUserIdChange,
                        onAlternateUrlChange = onConnectionAlternateUrlChange,
                        onAuthModeChange = onConnectionAuthModeChange,
                        onCompanionLoginChange = onCompanionLoginChange,
                        onUsernameChange = onConnectionUsernameChange,
                        onPasswordChange = onConnectionPasswordChange,
                        onTestAndSave = onTestAndSaveConnection,
                        onRemove = { onRemoveConnection(it.kind) },
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun DetailSheetSkeleton() {
    // One quiet, viewport-sized loading state. Do not expose partial metadata and then
    // move the same paragraphs repeatedly as the detail response completes.
    Column(Modifier.fillMaxSize().testTag("detail-loading")
        .padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(17.dp)) {
            Box(Modifier.width(116.dp).height(174.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceRaised))
            Column(Modifier.weight(1f).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.width(70.dp).height(9.dp).clip(CircleShape).background(SurfaceRaised))
                Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceRaised))
                Box(Modifier.fillMaxWidth(0.65f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceRaised))
            }
        }
        DetailTextSkeleton(Modifier.fillMaxWidth())
        DetailTextSkeleton(Modifier.fillMaxWidth())
    }
}

@Composable
private fun RichTitleDetailsSheet(state: ReelstackUiState, onAddMedia: (String) -> Unit, onSeerrAccount: () -> Unit, scroll: ScrollState, entered: Boolean) {
    val details = state.contentDetails ?: return
    // Freeze the opening artwork and title. Late metadata must not replace or resize the hero.
    val opening = remember(details.key) { details }
    val ready = entered && !details.loading
    val metadataAlpha by animateFloatAsState(if (ready) 1f else 0f, tween(180), label = "metadata-reveal")
    val discoverMedia = (state.discover + state.searchResults + state.recommendations).firstOrNull { it.id == details.key }
    val mediaType = resolvedMediaType(opening.mediaType, opening.subtitle)
    val isMovie = mediaType == "Movie"
    val usePoster = isMovie || mediaType == "Series" || opening.source == ServiceKind.SEERR
    val visibleFacts = details.facts.filterNot { it == details.source?.displayName }.distinct()
    Column(
        Modifier
            .fillMaxSize()
            .testTag("detail-scroll")
            .verticalScroll(scroll)
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
    ) {
        if (usePoster) {
            MoviePosterSummary(
                title = opening.title,
                eyebrow = opening.eyebrow,
                subtitle = opening.subtitle,
                tagline = opening.tagline,
                facts = opening.facts.take(4),
                artworkUrl = opening.artworkUrl,
                artworkRes = opening.artworkRes,
                source = opening.source,
                loading = false,
            )
        } else {
            CinematicTitleHero(
                title = opening.title,
                eyebrow = opening.eyebrow,
                subtitle = opening.subtitle,
                artworkUrl = opening.artworkUrl,
                artworkRes = opening.artworkRes,
                source = opening.source,
                portrait = mediaType == "Series",
            )
        }
        if (!ready) {
            Column(Modifier.fillMaxWidth().testTag("detail-loading").padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DetailTextSkeleton(Modifier.fillMaxWidth())
                DetailTextSkeleton(Modifier.fillMaxWidth())
            }
            return@Column
        }
        Column(Modifier.fillMaxWidth().graphicsLayer { alpha = metadataAlpha }) {
        if (details.title != opening.title) {
            Text(details.title, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp))
        }
        val remainingFacts = if (usePoster) visibleFacts.filterNot { it in opening.facts.take(4) } else visibleFacts
        // Keep metadata on one predictable rail. A wrapping FlowRow changes the scroll content
        // height when the remote detail response adds a second row of facts.
        Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(top = 16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            ) {
                if (remainingFacts.isNotEmpty()) {
                    remainingFacts.distinct().take(8).forEach { fact -> DetailPill(fact) }
                } else if (details.loading) {
                    repeat(2) { index ->
                        Box(
                            Modifier
                                .width(if (index == 0) 62.dp else 86.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(SurfaceRaised),
                        )
                    }
                }
            }
        }
        // Reserve the two metadata slots before the network response arrives. The placeholders
        // are intentionally quiet; the fixed geometry is what makes the opening feel instant.
        Box(Modifier.fillMaxWidth().heightIn(min = 31.dp).padding(start = 6.dp, top = 15.dp, end = 6.dp)) {
            if (details.genres.isNotEmpty()) {
                Text(
                    details.genres.take(4).joinToString(" · "),
                    color = PrimarySoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            } else if (details.loading) {
                Box(
                    Modifier.width(132.dp).height(8.dp).clip(CircleShape).background(SurfaceRaised),
                )
            }
        }
        if (!usePoster) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 6.dp, top = 10.dp, end = 6.dp)) {
                val tagline = details.tagline?.takeIf(String::isNotBlank)
                if (tagline != null) {
                    Text(
                        tagline,
                        color = app.reelstack.ui.theme.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                } else if (details.loading) {
                    Box(Modifier.fillMaxWidth(0.72f).height(9.dp).clip(CircleShape).background(SurfaceRaised))
                }
            }
        }
        // Reserve room for a synopsis, but let large text and explicit expansion grow inside the
        // scroll view. Only the outer sheet has a fixed height; text must never be clipped by it.
        val overview = details.overview?.takeIf { it.isNotBlank() }
        var expandedOverview by rememberSaveable(details.key) { mutableStateOf(false) }
        val textMeasurer = rememberTextMeasurer()
        Column(
            Modifier.fillMaxWidth().heightIn(min = 174.dp).padding(start = 6.dp, top = 19.dp, end = 6.dp),
        ) {
            Text(
                when {
                    isMovie -> "Om filmen"
                    mediaType == "Episode" -> "Om episoden"
                    mediaType == "Series" -> "Om serien"
                    else -> "Om tittelen"
                },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (overview != null) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val overviewStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 25.sp)
                // Measure before placement: onTextLayout + mutableState inserts the button
                // one frame later and moves everything below it on every first composition.
                val overviewOverflows = textMeasurer.measure(
                    overview, style = overviewStyle, maxLines = 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = constraints.maxWidth),
                ).hasVisualOverflow
                Column {
                Text(
                    overview,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = overviewStyle,
                    maxLines = if (expandedOverview) Int.MAX_VALUE else 4,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (overviewOverflows || expandedOverview) {
                    TextButton(onClick = { expandedOverview = !expandedOverview },
                        modifier = Modifier.testTag("overview-expand")) {
                        Text(if (expandedOverview) "Vis mindre" else "Les heile omtalen")
                    }
                }
                }
                }
            } else if (details.loading) {
                DetailTextSkeleton(Modifier.fillMaxWidth().padding(top = 14.dp))
            } else {
                Text(
                    "Ingen omtale tilgjengeleg.",
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        details.statusTitle?.let { title ->
            Row(Modifier.fillMaxWidth().padding(top = 24.dp).clip(RoundedCornerShape(14.dp))
                .background(SurfaceRaised).padding(14.dp), verticalAlignment = Alignment.Top) {
                if (details.libraryAvailable) Icon(Icons.Rounded.VideoLibrary, null, tint = Primary, modifier = Modifier.padding(top = 3.dp).size(20.dp))
                else Icon(Icons.Rounded.Schedule, null, tint = Muted, modifier = Modifier.padding(top = 3.dp).size(20.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(title, color = PrimarySoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    details.statusDescription?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
        details.error?.let {
            Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        }
        if (details.cast.isNotEmpty()) {
            Text("Medverkande", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 14.dp))
            app.reelstack.ui.components.CastRail(details.cast)
        }
        OpenInServerButton(state, details)
        if (discoverMedia != null && discoverMedia.canRequest && (state.configuredCount == 0 ||
            state.accounts[ServiceKind.SEERR]?.let { !it.isPersonal || it.canRequestType(discoverMedia.mediaType ?: "movie") } == true)) {
            val adding = discoverMedia.id in state.requestingMediaIds
            val connectedSeerr = state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() }
            val needsAccount = connectedSeerr && state.accounts[ServiceKind.SEERR]?.isPersonal != true
            RequestIdentity(state, onSeerrAccount)
            Button(
                onClick = { if (needsAccount) onSeerrAccount() else onAddMedia(discoverMedia.id) },
                enabled = !adding,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Ink,
                    disabledContainerColor = Color(0xFF2A473B),
                    disabledContentColor = Color(0xFFC9F4DB),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).heightIn(min = 56.dp),
            ) {
                if (adding) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(19.dp))
                } else {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    when {
                        adding -> "Legg til…"
                        needsAccount -> "Logg inn for å leggje til"
                        discoverMedia.mediaType == "tv" -> "Vel sesongar"
                        else -> "Legg til i mediesamlinga"
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
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
    loading: Boolean,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
    ) {
        Surface(
            color = app.reelstack.ui.theme.Ink,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(116.dp).height(174.dp).testTag("detail-artwork"),
        ) {
            MediaArtwork(
                url = artworkUrl,
                fallbackRes = artworkRes,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                source = source,
                crossfadeDurationMillis = 0,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f).padding(start = 17.dp, top = 7.dp)) {
            DetailEyebrow(eyebrow, source)
            Text(
                title,
                color = Color.White,
                fontSize = 25.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 7.dp),
            )
            // Type/year already live in the facts: avoid saying them twice.
            val supportingText = tagline?.takeIf(String::isNotBlank) ?: subtitle.takeIf { facts.isEmpty() }.orEmpty()
            Box(Modifier.fillMaxWidth().heightIn(min = 43.dp).padding(top = 8.dp)) {
                if (supportingText.isNotBlank()) {
                Text(
                    supportingText,
                    color = app.reelstack.ui.theme.Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 39.dp).padding(top = 11.dp)) {
                if (facts.isNotEmpty()) {
                    Text(
                        facts.take(4).joinToString(" · "),
                        color = PrimarySoft,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                } else if (loading) {
                    Box(Modifier.width(112.dp).height(8.dp).clip(CircleShape).background(SurfaceRaised))
                }
            }
        }
    }
}


@Composable
private fun SourceMark(kind: ServiceKind, modifier: Modifier = Modifier) {
    ServiceSymbol(kind, modifier)
}

/**
 * Says where this title came from and what it is right now — "Nyleg tilgjengeleg · Radarr",
 * "Bibliotek i Jellyfin", "I biblioteket ditt". The screens build that line for every sheet, and
 * both headers used to drop it and print the bare service name instead, so a title opened from
 * Kjem snart looked the same as one opened from Nedlastingar.
 */
@Composable
private fun DetailEyebrow(eyebrow: String, source: ServiceKind?, modifier: Modifier = Modifier) {
    val label = eyebrow.takeIf(String::isNotBlank) ?: source?.displayName ?: return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        source?.let {
            SourceMark(it, Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/**
 * Hands a library title over to the server's own client, which is the one place that can actually
 * play it. Library ids are stored as "<source>-<serverId>", so the server id is recoverable.
 */
@Composable
private fun OpenInServerButton(state: ReelstackUiState, details: ContentDetails) {
    val source = details.source ?: return
    if (source != ServiceKind.JELLYFIN && source != ServiceKind.EMBY) return
    val itemId = details.key.removePrefix("${source.name.lowercase()}-").takeIf { it.isNotBlank() && it != details.key }
        ?: return
    val baseUrl = state.connections.firstOrNull { it.kind == source }?.baseUrl?.trimEnd('/')?.takeIf { it.isNotBlank() }
        ?: return
    val path = if (source == ServiceKind.JELLYFIN) "details" else "item"
    val context = LocalContext.current
    val url = "$baseUrl/web/index.html#!/$path?id=$itemId"
    // Resolved once per sheet, so the button can name the app it is actually going to open.
    val target = androidx.compose.runtime.remember(url) {
        app.reelstack.ui.components.NativeClientLauncher.resolve(context, url)
    }
    var failed by androidx.compose.runtime.saveable.rememberSaveable(url) { mutableStateOf(false) }
    OutlinedButton(
        onClick = {
            failed = !app.reelstack.ui.components.NativeClientLauncher.open(context, url, target.packageName)
        },
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, app.reelstack.ui.theme.ControlOutline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimarySoft),
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 52.dp).testTag("open-in-server"),
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            if (target.packageName != null) target.label else "Opne i ${source.displayName}",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    if (failed) {
        Text(
            "Fann ingen app som kan opne denne lenkja.",
            color = Warning, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** Default port per service, matching the addresses documented in the README. */
private fun exampleAddress(kind: ServiceKind): String = when (kind) {
    ServiceKind.JELLYFIN, ServiceKind.EMBY -> "http://192.168.1.20:8096"
    ServiceKind.SEERR -> "http://192.168.1.20:5055"
    ServiceKind.RADARR -> "http://192.168.1.20:7878"
    ServiceKind.SONARR -> "http://192.168.1.20:8989"
}

/**
 * The address step is where people who did not set up the stack themselves get stuck, so it
 * shows a working example and says where to find the real one.
 */
@Composable
private fun AddressExamples(kind: ServiceKind, enabled: Boolean, onUse: (String) -> Unit) {
    val example = exampleAddress(kind)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("Slik ser ei adresse ut", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Surface(
            onClick = { onUse(example) },
            enabled = enabled,
            color = SurfaceRaised,
            contentColor = PrimarySoft,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp).testTag("address-example"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(example, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Bruk", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Text(
            "Det er den same adressa du opnar ${kind.displayName} med i nettlesaren, på same nett som tenaren. " +
                "Utanfrå treng du ei HTTPS-adresse gjennom din eigen proxy.",
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
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
    portrait: Boolean,
) {
    // Pick the frame from metadata, not from the first decoded frame. Measuring the image and
    // then changing between a wide and portrait layout while the sheet is entering is visible as
    // a jump on slower devices. The image itself can still be fit or cropped inside this stable
    // frame, so the sheet has one geometry from the first frame onward.
    Column {
        MediaArtwork(
            url = artworkUrl, fallbackRes = artworkRes, contentDescription = null,
            contentScale = if (portrait) ContentScale.Fit else ContentScale.Crop,
            source = source,
            crossfadeDurationMillis = 0,
            modifier = Modifier
                .then(if (portrait) Modifier.width(190.dp) else Modifier.fillMaxWidth())
                .aspectRatio(if (portrait) 2f / 3f else 16f / 9f)
                .clip(RoundedCornerShape(14.dp)).background(Ink),
        )
        DetailEyebrow(eyebrow, source, Modifier.padding(top = 16.dp))
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
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}

@Composable
private fun SessionSheet(state: ReelstackUiState, sessionKey: String, onPlaybackToggle: (String) -> Unit, scroll: ScrollState) {
    val session = state.sessions.firstOrNull { it.key == sessionKey } ?: return
    Column(
        Modifier.testTag("session-scroll").verticalScroll(scroll).padding(start = 18.dp, end = 18.dp, bottom = 34.dp),
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
                    .clip(CircleShape).background(app.reelstack.ui.theme.SurfaceRaised)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            ) {
                Box(Modifier.size(7.dp).background(Primary, CircleShape))
                Text(
                    if (session.paused) "På pause" else "Spelar no",
                    color = Color.White,
                    fontSize = 12.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    session.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(session.subtitle, color = app.reelstack.ui.theme.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                LinearProgressIndicator(
                    progress = { session.progress.coerceIn(0f, 1f) },
                    color = Primary,
                    trackColor = Color(0x45FFFFFF),
                    // Material draws a dot at the far end by default, which reads as a second
                    // position marker on a bar that already shows where playback is.
                    drawStopIndicator = {},
                    gapSize = 0.dp,
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
                SessionMetric("Avspeling", session.streamMethod, Modifier.weight(1f))
                SessionMetric("Kvalitet", session.quality, Modifier.weight(1f))
                // The value already ends in "att"; repeating it in the label read as "att att".
                SessionMetric("Tid igjen", session.timeLeft.removeSuffix(" att"), Modifier.weight(1f))
            }
        }
        Button(
            onClick = { onPlaybackToggle(session.key) },
            enabled = state.pendingSessionKey == null,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = app.reelstack.ui.theme.Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 58.dp),
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
        Text(label.uppercase(), color = Muted, fontSize = 11.sp)
        Text(
            value,
            color = app.reelstack.ui.theme.Text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}


@Composable
internal fun ConnectionEditorSheet(
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
    onCompanionLoginChange: (Boolean, String) -> Unit = { _, _ -> },
    onAlternateUrlChange: (String) -> Unit = {},
) {
    var credentialsStep by rememberSaveable(draft.kind) { mutableStateOf(configured) }
    var advanced by rememberSaveable(draft.kind) { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(draft.kind, configured) { mutableStateOf(!configured) }
    var showPassword by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val nextStep: () -> Unit = {
        runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .onSuccess { onUrlChange(it); credentialsStep = true; focus.clearFocus() }
            .onFailure { addressError = it.message }
    }
    Column(Modifier.fillMaxSize()) {
        SheetToolbar(if (configured) draft.kind.displayName else "Logg inn på ${draft.kind.displayName}", "Lukk", onDismiss)
    Column(
        Modifier.weight(1f).testTag("connection-scroll").verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        if (configured) {
            ConnectedServiceSummary(draft.kind, detailsExpanded) { detailsExpanded = !detailsExpanded }
        }
        AnimatedVisibility(
            visible = !configured || detailsExpanded,
            enter = expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
        ) {
        Column {
        // A step marker only helps when it says how many steps there are.
        Text(if (configured) "TILKOPLING" else if (credentialsStep) "STEG 2 AV 2 · LOGG INN" else "STEG 1 AV 2 · FINN TENAREN",
            color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Text(if (credentialsStep) "Bruk kontoen din. Vi tek vare på resten."
            else "Bruk adressa du vanlegvis opnar i nettlesaren.", color = Muted, fontSize = 14.sp)
        if (configured) {
            TextButton(onClick = { confirmSignOut = true }, enabled = !draft.saving) { Text("Logg ut", color = Warning) }
        }
        if (confirmSignOut) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmSignOut = false },
                title = { Text("Logg ut av ${draft.kind.displayName}?") },
                text = { Text("Innlogginga blir fjerna frå Spole på denne eininga. Dei andre tenestene er framleis innlogga. Vi hugsar tenaradressa til neste gong.") },
                confirmButton = { TextButton(onClick = { confirmSignOut = false; onRemove() }) { Text("Logg ut", color = Warning) } },
                dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Avbryt") } },
                containerColor = SurfaceRaised, shape = RoundedCornerShape(28.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        if (!credentialsStep) {
            OutlinedTextField(
                value = draft.url, onValueChange = { addressError = null; onUrlChange(it) },
                label = { Text("Tenaradresse") }, placeholder = { Text(exampleAddress(draft.kind)) },
                supportingText = { Text(addressError ?: "Ta med port eller undermappe dersom tenaren din brukar det.") },
                isError = addressError != null,
                singleLine = true, shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                keyboardOptions = KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { nextStep() }),
                modifier = Modifier.fillMaxWidth().testTag("connection-url"),
            )
            // Material hides the placeholder until the field has focus, so the example that
            // people actually need has to live outside the field. Tapping it fills the field.
            AddressExamples(draft.kind, enabled = !draft.saving) { addressError = null; onUrlChange(it) }
            Button(onClick = nextStep, enabled = draft.url.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                // Material's default disabled fill is 12 % of onSurface, which on this page is
                // indistinguishable from a container. An outline keeps it readable as a button
                // that is waiting for input.
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = SurfaceRaised,
                    disabledContentColor = Muted,
                ),
                border = if (draft.url.isBlank()) androidx.compose.foundation.BorderStroke(1.dp, app.reelstack.ui.theme.ControlOutline) else null,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 54.dp)) {
                Text("Hald fram", fontWeight = FontWeight.Bold)
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(draft.url, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f),
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            TextButton(onClick = { credentialsStep = false }, enabled = !draft.saving) { Text("Endre") }
        }
        if (draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR) ||
            (draft.kind == ServiceKind.EMBY && (advanced || draft.authMode == ConnectionAuthMode.API_KEY))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOfNotNull(
                    (ConnectionAuthMode.QUICK_CONNECT to "Quick Connect").takeIf { draft.kind != ServiceKind.EMBY },
                    ConnectionAuthMode.ACCOUNT to if (draft.kind == ServiceKind.SEERR) "Jellyfin-konto" else "Brukarnamn",
                    (ConnectionAuthMode.API_KEY to "API-nøkkel").takeIf { advanced || draft.authMode == ConnectionAuthMode.API_KEY },
                ).forEach { (mode, label) ->
                    FilterChip(selected = draft.authMode == mode, onClick = { onAuthModeChange(mode) },
                        enabled = !draft.saving, label = { Text(label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp), border = null, colors = connectionChipColors())
                }
            }
        }
        val supportsJellyfinLogin = draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.SEERR
        val usesAccount = (supportsJellyfinLogin || draft.kind == ServiceKind.EMBY) && draft.authMode == ConnectionAuthMode.ACCOUNT
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
                else "Bruk den lokale ${draft.kind.displayName}-kontoen din. Passordet blir aldri lagra.",
                color = Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            if (supportsJellyfinLogin) {
                val otherName = if (draft.kind == ServiceKind.SEERR) "Jellyfin" else "Seerr"
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .then(Modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceRaised))
                        .then(Modifier.toggleableLogin(!draft.saving, draft.alsoConnect) { onCompanionLoginChange(it, draft.companionUrl) })
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    androidx.compose.material3.Checkbox(checked = draft.alsoConnect, onCheckedChange = null, enabled = !draft.saving)
                    Text("Logg inn på $otherName òg", color = Primary, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                }
                if (draft.alsoConnect) {
                    OutlinedTextField(value = draft.companionUrl,
                        onValueChange = { onCompanionLoginChange(true, it) }, label = { Text("Adresse til $otherName") },
                        enabled = !draft.saving, singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                        ),
                        shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Text("Same brukarnamn og passord blir sende til begge adressene du har valt. Seerr må vere knytt til denne Jellyfin-tenaren. Eksisterande kontoar på desse to tenestene blir bytte ut.",
                        color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                    if (runCatching { EndpointValidator.isCleartext(draft.companionUrl) }.getOrDefault(false)) {
                        Text("Denne adressa brukar HTTP utan kryptering. Bruk helst HTTPS.", color = Warning, fontSize = 12.sp)
                    }
                }
            }
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
            }, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp))
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
            // The same server usually has two ways in: the LAN address at home and a proxy from
            // outside. The token belongs to the server, so one sign-in covers both.
            OutlinedTextField(
                value = draft.alternateUrl, onValueChange = onAlternateUrlChange,
                label = { Text("Andre adresse (valfri)") }, singleLine = true, enabled = !draft.saving,
                placeholder = { Text("https://spole.dømet.no") },
                supportingText = {
                    Text("Brukt automatisk når den vanlege adressa ikkje svarar, til dømes når du ikkje er heime.")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    autoCorrectEnabled = false, keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("connection-alternate-url"),
            )
        }
        draft.warning?.let { MessageCard(it, warning = true) }
        draft.error?.let { MessageCard(it, warning = false) }
        Button(
            onClick = { focus.clearFocus(); onTestAndSave() },
            enabled = !draft.saving && (usesQuickConnect || if (usesAccount) draft.username.isNotBlank() else draft.token.isNotBlank()),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 56.dp),
        ) {
            if (draft.saving) CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Text(when {
                draft.saving && usesQuickConnect && draft.quickConnectCode != null -> "Ventar på godkjenning…"
                draft.saving && usesQuickConnect -> "Lagar kode…"
                draft.saving -> "Koplar til…"
                usesQuickConnect && draft.quickConnectCode != null -> "Lag ny kode"
                usesQuickConnect -> "Start Quick Connect"
                usesAccount && draft.alsoConnect -> "Logg inn på begge"
                usesAccount -> "Logg inn"
                else -> "Kople til"
            }, modifier = Modifier.padding(start = if (draft.saving) 10.dp else 0.dp))
        }
        if (draft.saving) {
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Avbryt") }
        }
        }
        }
    }
    }
}

@Composable
private fun ConnectedServiceSummary(kind: ServiceKind, expanded: Boolean, onToggle: () -> Unit) {
    val isAccount = kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY || kind == ServiceKind.SEERR
    val stateText = if (isAccount) "${kind.displayName} er innlogga" else "${kind.displayName} er tilkopla"
    val subject = if (isAccount) "innlogginga" else "tilkoplinga"
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "connection-details-arrow")
    Surface(
        color = Success.copy(alpha = .09f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            .testTag("connected-service-summary")
            .clickable(onClickLabel = if (expanded) "Skjul $subject" else "Vis $subject", onClick = onToggle)
            .semantics {
                role = Role.Button
                contentDescription = "$stateText. ${if (expanded) "Skjul" else "Vis"} $subject"
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(Success.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Success, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stateText, color = Success, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (expanded) "Skjul $subject" else "Vis $subject og kontoval",
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown, null, tint = Muted,
                modifier = Modifier.size(22.dp).rotate(arrowRotation),
            )
        }
    }
}

@Composable
internal fun QuickConnectPanel(draft: ConnectionDraft) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember(draft.quickConnectCode) { mutableStateOf(false) }
    AnimatedContent(
        targetState = draft.quickConnectCode,
        transitionSpec = { (fadeIn() togetherWith fadeOut()).using(SizeTransform(clip = false)) },
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
                            fontSize = 12.sp,
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
                            fontSize = 12.sp,
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
                    TextButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(code)); copied = true }) {
                        Text(if (copied) "Kopiert" else "Kopier kode")
                    }
                    Text(
                        "Opne Jellyfin der du er innlogga. Gå til Innstillingar → Quick Connect og lim inn koden. Du kan bruke nettlesaren på denne mobilen òg.",
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

private fun Modifier.toggleableLogin(enabled: Boolean, checked: Boolean, onChange: (Boolean) -> Unit): Modifier =
    toggleable(value = checked, enabled = enabled, role = androidx.compose.ui.semantics.Role.Checkbox, onValueChange = onChange)

@Composable
private fun connectionChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Primary,
    selectedLabelColor = Ink,
    containerColor = SurfaceRaised,
    labelColor = app.reelstack.ui.theme.Text,
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
        Text(text, color = accent, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
// A transparent border leaves the field at 1.24:1 against the page, which is not enough to
// identify it as a control. ControlOutline is 3.5:1 against Ink and satisfies WCAG 1.4.11.
private fun connectionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary.copy(alpha = 0.72f),
    unfocusedBorderColor = app.reelstack.ui.theme.ControlOutline,
    disabledBorderColor = app.reelstack.ui.theme.ControlOutline.copy(alpha = 0.6f),
    focusedContainerColor = SurfaceRaised,
    unfocusedContainerColor = SurfaceRaised,
    disabledContainerColor = SurfaceRaised,
    cursorColor = Primary,
)
