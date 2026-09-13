package app.reelstack.ui
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.SpoleSecondaryButton

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.focusGroup
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextOverflow

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
    onImportSetupLink: (String) -> Unit = {},
    onConnectionAlternateUrlChange: (String) -> Unit = {},
    onSeasonWatch: (Int, Boolean) -> Unit = { _, _ -> },
    onFavourite: (String, Boolean) -> Unit = { _, _ -> },
    onPlayed: (String, Boolean) -> Unit = { _, _ -> },
    onSeason: (String) -> Unit = {},
    onCancelConnection: () -> Unit = {},
) {
    val sheet = state.activeSheet ?: return
    val sheetContentStates = rememberSaveableStateHolder()
    val tvDetails = sheet is AppSheet.TitleDetails &&
        (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    StableSheetDialog(dismissEnabled = state.requestDraft?.sending != true, onDismiss = onDismiss,
        fullScreen = tvDetails,
        onCloseStarted = { if (connectionDraft?.simpleSetup == true) onCancelConnection() }) { entered, closing, close ->
        val detailScroll = androidx.compose.runtime.key(sheet) { rememberScrollState() }
        Column(Modifier.fillMaxSize().testTag("sheet-viewport")) {
            if (!tvDetails && (sheet is AppSheet.TitleDetails || sheet is AppSheet.SessionDetails)) {
                SheetToolbar(
                    title = if (sheet is AppSheet.SessionDetails) stringResource(R.string.details_playback) else stringResource(R.string.details_title),
                    closeDescription = stringResource(R.string.details_close), onClose = close, enabled = !closing,
                    onBack = if (state.returnToCalendar) onBackToCalendar else null,
                    page = tvDetails,
                )
            }
            Box(
                Modifier.weight(1f).fillMaxWidth(),
            ) {
            when (sheet) {
                AppSheet.RequestComposer -> RequestComposer(state, onRequestSeason, onRequestNotification,
                    onConfirmRequest, close, { state.requestDraft?.media?.id?.let(onAddMedia) }, onSeerrAccount,
                    onSeasonWatch = onSeasonWatch, entered = entered)
                is AppSheet.SessionDetails -> SessionSheet(state, sheet.sessionKey, onPlaybackToggle, detailScroll)
                is AppSheet.TitleDetails -> state.contentDetails?.let { details ->
                    androidx.compose.runtime.key(details.key) {
                        RichTitleDetailsSheet(state = state, onAddMedia = onAddMedia,
                            onSeerrAccount = onSeerrAccount, scroll = detailScroll, entered = entered,
                            onFavourite = onFavourite, onPlayed = onPlayed, onSeason = onSeason)
                    }
                }
                AppSheet.UpcomingCalendar -> sheetContentStates.SaveableStateProvider("calendar") {
                    UpcomingCalendarSheet(state.upcoming, onUpcomingClick, close)
                }
                is AppSheet.ConnectionEditor -> connectionDraft?.let {
                    if (it.simpleSetup) CombinedSetupSheet(it, onConnectionUrlChange,
                        { url -> onCompanionLoginChange(true, url) }, onTestAndSaveConnection, close,
                        onConnectionAuthModeChange, onConnectionUsernameChange, onConnectionPasswordChange,
                        { enabled -> onCompanionLoginChange(enabled, it.companionUrl) }, onImportSetupLink)
                    else ConnectionEditorSheet(
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
                        setupLink = if (it.kind == ServiceKind.JELLYFIN) runCatching {
                            app.reelstack.data.network.SetupLink(
                                state.connections.first { connection -> connection.kind == ServiceKind.JELLYFIN }.baseUrl,
                                state.connections.firstOrNull { connection -> connection.kind == ServiceKind.SEERR }?.baseUrl.orEmpty()
                            ).encode()
                        }.getOrNull() else null,
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
private fun RichTitleDetailsSheet(state: ReelstackUiState, onAddMedia: (String) -> Unit, onSeerrAccount: () -> Unit,
    scroll: ScrollState, entered: Boolean, onFavourite: (String, Boolean) -> Unit = { _, _ -> },
    onPlayed: (String, Boolean) -> Unit = { _, _ -> }, onSeason: (String) -> Unit = {}) {
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
    val tv = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val isSeries = mediaType == "Series"
    val wideDetail = isSeries || mediaType == "Episode" || mediaType == "Season"
    // Facts, synopsis and actions share the reading column on TV so all content can be reached.
    val synopsis: @Composable () -> Unit = {
        app.reelstack.ui.components.ExpandableSynopsis(
            identity = details.key,
            title = when {
                isMovie -> stringResource(R.string.details_about_movie)
                mediaType == "Episode" -> stringResource(R.string.details_about_episode)
                mediaType == "Series" -> stringResource(R.string.details_about_series)
                else -> stringResource(R.string.details_about_title)
            },
            overview = details.overview?.takeIf { it.isNotBlank() },
            loading = details.loading,
            compact = tv,
        )
    }
    val castBlock: @Composable () -> Unit = {
        if (details.cast.isNotEmpty()) {
            Text(stringResource(R.string.details_cast), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp))
            app.reelstack.ui.components.CastRail(details.cast, details.source)
        }
    }
    val aside: @Composable () -> Unit = {
        DetailAside(details, opening, visibleFacts, tv)
    }
    val detailScope = androidx.compose.runtime.rememberCoroutineScope()
    var chosenAudio by remember(details.key) { mutableStateOf<Int?>(null) }
    var chosenSubtitle by remember(details.key) { mutableStateOf<Int?>(null) }
    var chosenVersion by remember(details.key) { mutableStateOf<String?>(null) }
    val actions: @Composable () -> Unit = {
        TitleActionRow(state, details, chosenAudio, chosenSubtitle, chosenVersion, onFavourite, onPlayed,
            modifier = Modifier.onFocusChanged { if (tv && it.hasFocus && scroll.value > 0) {
                detailScope.launch { scroll.animateScrollTo(0) }
            } }.onPreviewKeyEvent { event ->
                if (tv && event.key == androidx.compose.ui.input.key.Key.DirectionUp) {
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown)
                        detailScope.launch { scroll.animateScrollTo(0) }
                    true
                } else false
            }.focusGroup())
    }
    val tvHeading: @Composable () -> Unit = {
        opening.source?.let { Text(it.displayName, color = Muted, style = MaterialTheme.typography.labelLarge) }
        Text(opening.title, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 10.dp).testTag("detail-title"))
        if (!isSeries) Text(app.reelstack.ui.components.episodeLine(opening.season, opening.episode, opening.subtitle),
            color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp))
        if (wideDetail && ready) { aside(); actions() }
    }
    app.reelstack.ui.components.DetailReadingLayout(tv, scroll, series = wideDetail, heading = tvHeading, artwork = {
        Column {
            // The slot needs a shape before the picture arrives or the page would jump as it
            // loads, and the guess has to be the media type — but the guess is often wrong. A
            // series whose "poster" is really a landscape card left a third of the column as black
            // bars. So the guess holds only until the image reports its own proportions, and then
            // the frame becomes the picture's own; the blurred copy fills the difference in the
            // meantime, so the first paint is never a pair of black stripes either.
            //
            // Clamped, because a panorama or a bookmark-shaped image must not take the column with
            // it. Below Android 12 `blur` does nothing, and the brief letterbox that leaves is the
            // behaviour this page always had.
            var measured by remember(opening.key) { mutableStateOf<Float?>(null) }
            val slot = if (tv) { if (isSeries) 16f / 9f else if (usePoster) 2f / 3f else 16f / 9f }
                else measured ?: if (usePoster) 2f / 3f else 16f / 9f
            Box(Modifier.fillMaxWidth().aspectRatio(slot).clip(RoundedCornerShape(16.dp))) {
                if (!tv && measured == null) MediaArtwork(opening.artworkUrl, null, Modifier.matchParentSize()
                        .blur(34.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = .45f }, fallbackRes = opening.artworkRes, ContentScale.Crop, opening.source)
                MediaArtwork(opening.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = opening.artworkRes, ContentScale.Fit, opening.source, onAspectRatio = { ratio -> measured = ratio.coerceIn(0.5f, 2.0f) })
            }
            if (ready && !tv) Box(Modifier.graphicsLayer { alpha = metadataAlpha }) { aside() }
        }
    }) {
        if (!tv && usePoster) {
            MoviePosterSummary(
                title = opening.title,
                eyebrow = opening.eyebrow,
                subtitle = app.reelstack.ui.components.episodeLine(opening.season, opening.episode, opening.subtitle),
                tagline = opening.tagline,
                facts = opening.facts.filter { app.reelstack.ui.theme.LocalPersonalization.current.showRatings || !it.startsWith("★") }.take(4),
                artworkUrl = opening.artworkUrl,
                artworkRes = opening.artworkRes,
                source = opening.source,
                loading = false,
            )
        } else if (!tv) {
            CinematicTitleHero(
                title = opening.title,
                eyebrow = opening.eyebrow,
                subtitle = app.reelstack.ui.components.episodeLine(opening.season, opening.episode, opening.subtitle),
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
            return@DetailReadingLayout
        }
        // Null means "whatever the server would have picked". A choice here is carried into the
        // player, so what the page promises is what starts.
        Column(Modifier.fillMaxWidth().graphicsLayer { alpha = metadataAlpha }) {
        if (tv && !wideDetail) aside()
        if (!tv || !wideDetail) actions()
        if (tv) synopsis()
        if (isSeries) SeriesPlayNote(state.seriesBrowse, details.key)
        if (tv) TvTitleRequestAction(state, details.key, onAddMedia, onSeerrAccount)
        if (details.title != opening.title) {
            Text(details.title, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp))
        }
        // A series is not one file, so it has no tracks and no version to pick. What it has is
        // seasons, and that is the whole point of the page.
        if (!isSeries) MediaTrackChoices(details, chosenAudio, chosenSubtitle, chosenVersion,
            { chosenAudio = it }, { chosenSubtitle = it }, { chosenVersion = it })
        // An episode gets the same list, opened on the season it belongs to. The rest of the
        // season is the thing a reader on an episode page actually wants next, and it is what
        // used to leave the lower half of a television screen empty.
        SeriesEpisodes(state.seriesBrowse, details.key, onSeason)
        // TV has already shown these alongside its smaller poster.
        if (!tv) { aside(); synopsis() }
        details.statusTitle?.takeUnless { details.libraryAvailable && details.source == ServiceKind.JELLYFIN }?.let { title ->
            Row(Modifier.fillMaxWidth().padding(top = 24.dp).clip(RoundedCornerShape(14.dp))
                .background(SurfaceRaised).padding(14.dp), verticalAlignment = Alignment.Top) {
                if (details.libraryAvailable) Icon(app.reelstack.ui.components.SpoleIcons.Movie, null, tint = Primary, modifier = Modifier.padding(top = 3.dp).size(20.dp))
                else Icon(app.reelstack.ui.components.SpoleIcons.Clock, null, tint = Muted, modifier = Modifier.padding(top = 3.dp).size(20.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(if (discoverMedia != null) app.reelstack.localization.localizedSeerrStatus(discoverMedia.seerrStatus, discoverMedia.inLibrary, discoverMedia.requested) else title, color = PrimarySoft, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                    details.statusDescription?.let { Text(it, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
        details.error?.let {
            Text(it, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 12.dp))
        }
        castBlock()
        if (!tv && discoverMedia != null && discoverMedia.canRequest && (state.configuredCount == 0 ||
            state.accounts[ServiceKind.SEERR]?.let { discoverMedia.mediaType == "tv" || !it.isPersonal || it.canRequestType(discoverMedia.mediaType ?: "movie") } == true)) {
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
                    // Follows the chosen accent. These used to be fixed greens, so a coral or
                    // iris accent had a green disabled state.
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp).heightIn(min = 56.dp),
            ) {
                if (adding) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(19.dp))
                } else {
                    Icon(if (discoverMedia.mediaType == "tv") app.reelstack.ui.components.SpoleIcons.ListLines else app.reelstack.ui.components.SpoleIcons.Add,
                        contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    when {
                        adding -> stringResource(R.string.media_adding)
                        needsAccount -> stringResource(R.string.media_login_add)
                        discoverMedia.mediaType == "tv" -> stringResource(R.string.media_seasons)
                        else -> stringResource(R.string.media_add_collection)
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun TvTitleRequestAction(state: ReelstackUiState, key: String, onAddMedia: (String) -> Unit, onAccount: () -> Unit) {
    val media = (state.discover + state.searchResults + state.recommendations).firstOrNull { it.id == key } ?: return
    if (!media.canRequest || !(state.configuredCount == 0 || state.accounts[ServiceKind.SEERR]?.let {
            media.mediaType == "tv" || !it.isPersonal || it.canRequestType(media.mediaType ?: "movie") } == true)) return
    val adding = media.id in state.requestingMediaIds
    val needsAccount = state.connections.any { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() } && state.accounts[ServiceKind.SEERR]?.isPersonal != true
    val focus = remember(key) { FocusRequester() }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    LaunchedEffect(key) { androidx.compose.runtime.withFrameNanos { }; focus.requestFocus() }
    Button(onClick = { if (needsAccount) onAccount() else onAddMedia(media.id) }, enabled = !adding,
        interactionSource = interaction, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(top = 16.dp).widthIn(min = 220.dp).heightIn(min = 52.dp)
            .focusRequester(focus).focusOutline(interaction, RoundedCornerShape(16.dp))
            .testTag("tv-title-request")) {
        Icon(if (media.mediaType == "tv") app.reelstack.ui.components.SpoleIcons.ListLines else app.reelstack.ui.components.SpoleIcons.Add, null)
        Text(stringResource(when { adding -> R.string.media_adding; needsAccount -> R.string.media_login_add;
            media.mediaType == "tv" -> R.string.media_seasons; else -> R.string.media_add_collection }), Modifier.padding(start = 10.dp))
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val spacious = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useSideBySideMedia
    val posterWidth = if (spacious) 164.dp else 116.dp
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 2.dp),
    ) {
        Surface(
            color = app.reelstack.ui.theme.Ink,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(posterWidth).height(posterWidth * 1.5f).testTag("detail-artwork"),
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
        Column(Modifier.weight(1f).padding(start = if (spacious) 28.dp else 17.dp, top = 7.dp)) {
            DetailEyebrow(eyebrow, source)
            Text(
                title,
                color = Color.White,
                fontSize = if (spacious) 32.sp else 25.sp,
                lineHeight = if (spacious) 37.sp else 27.sp,
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

/** Playback requires a real Jellyfin library ID; never guess one from a Seerr title. */
/**
 * Play, watched, favourite — one line.
 *
 * These were three stacked blocks: a full-width button, two chips spelling out what they did, and
 * a separate progress bar with its own percentage caption underneath. All three say the same thing
 * a resume button already says, so the progress went inside the button and the two states became
 * icons. An icon is enough when the shape is the label: a heart is a favourite everywhere.
 */
@Composable
private fun TitleActionRow(
    state: ReelstackUiState,
    details: ContentDetails,
    audioIndex: Int?,
    subtitleIndex: Int?,
    versionId: String?,
    onFavourite: (String, Boolean) -> Unit,
    onPlayed: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val marks = details.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
        state.connections.any { it.kind == details.source && it.token.isNotBlank() } &&
        !details.mediaType.equals("Series", true) && !details.mediaType.equals("Season", true)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A phone button spans the row and the icons sit at its end; a television button is
            // as wide as its own label, so a weight there would park the icons a hand's width away
            // across empty space.
            IntegratedPlaybackButton(state, details,
                if (television) Modifier else Modifier.weight(1f), audioIndex, subtitleIndex, versionId)
            if (marks) {
                IconAction(
                    icon = app.reelstack.ui.components.SpoleIcons.DoneCircle,
                    active = details.played,
                    description = stringResource(
                        if (details.played) R.string.library_played_unmark else R.string.library_played_mark),
                    tag = "detail-played",
                ) { onPlayed(details.key, !details.played) }
                IconAction(
                    icon = if (details.favourite) app.reelstack.ui.components.SpoleIcons.HeartFilled
                    else app.reelstack.ui.components.SpoleIcons.Heart,
                    active = details.favourite,
                    description = stringResource(
                        if (details.favourite) R.string.library_favourite_remove else R.string.library_favourite_add),
                    tag = "detail-favourite",
                ) { onFavourite(details.key, !details.favourite) }
            }
        }
        state.mediaActionError?.let {
            Text(it, color = Warning, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** A round, wordless switch. The shape carries the meaning; the label is for the screen reader. */
@Composable
private fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean,
    description: String, tag: String, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.size(52.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else SurfaceRaised)
            .focusOutline(interaction, CircleShape)
            .toggleable(
                value = active,
                interactionSource = interaction,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = androidx.compose.ui.semantics.Role.Checkbox,
                onValueChange = { onClick() },
            )
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(23.dp), tint = if (active) Primary else Muted)
    }
}

@Composable
private fun IntegratedPlaybackButton(state: ReelstackUiState, details: ContentDetails,
    modifier: Modifier = Modifier, audioIndex: Int? = null, subtitleIndex: Int? = null,
    versionId: String? = null) {
    if (details.source != ServiceKind.JELLYFIN || state.connections.none { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() }) return
    val key = details.key.removePrefix("jellyfin-").takeIf { it.isNotBlank() && it != details.key } ?: return
    // A series is not playable, but the episode you are in the middle of is — and that is what
    // pressing Play on a series page has always meant. The button waits for the episode list
    // rather than handing the reader over to the player's own browser to start again from zero.
    val series = details.mediaType.equals("Series", true) || details.mediaType.equals("Season", true)
    val nextEpisode = state.seriesBrowse.takeIf { it.openedFor == details.key }?.let(::resumeTarget)
    val itemId = if (series) nextEpisode?.remoteId ?: return else key
    val context = LocalContext.current
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val playFocus = remember(details.key) { FocusRequester() }
    val inputMode = androidx.compose.ui.platform.LocalInputModeManager.current
    LaunchedEffect(details.key, television) {
        if (television) {
            inputMode.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard)
            androidx.compose.runtime.withFrameNanos { }
            playFocus.requestFocus()
        }
    }
    val shape = RoundedCornerShape(14.dp)
    val progress = if (series) nextEpisode?.progress?.coerceIn(0f, 1f) ?: 0f
        else details.progress?.coerceIn(0f, 1f) ?: 0f
    val label = when {
        // The numbers, not the episode's name: the name is right there in the list underneath,
        // and a button that grows with the title is a button that moves every time you open one.
        series -> listOfNotNull(
            stringResource(if (progress > 0) R.string.tv_resume else R.string.player_play),
            nextEpisode?.let { app.reelstack.ui.components.episodeLine(it.season, it.episode, "") },
        ).joinToString(" · ")
        progress > 0 -> listOfNotNull(
            stringResource(R.string.tv_resume),
            details.remainingMinutes?.let { stringResource(R.string.detail_minutes_left, it) },
        ).joinToString(" · ")
        else -> stringResource(R.string.player_play)
    }
    Box(modifier.clip(shape)) {
        Button(
            onClick = { app.reelstack.player.JellyfinPlayerActivity.open(context, itemId, audioIndex, subtitleIndex, versionId) },
            modifier = (if (television) Modifier.widthIn(min = 220.dp, max = 420.dp) else Modifier.fillMaxWidth())
                .heightIn(min = 52.dp).focusRequester(playFocus).testTag("play-in-spole"),
            shape = shape,
        ) {
            Icon(app.reelstack.ui.components.SpoleIcons.Play, null, Modifier.size(20.dp))
            Text(label, Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // How far in you are, on the control that acts on it. A bar of its own with a percentage
        // caption said the same thing twice, in the loudest place on the page.
        if (progress > 0) Box(
            Modifier.matchParentSize(),
            contentAlignment = Alignment.BottomStart,
        ) {
            // A track under the whole width and a darker fill up to where you are. Without the
            // track the fill reads as a shadow under the button rather than a measurement.
            Box(Modifier.fillMaxWidth().height(6.dp).background(Ink.copy(alpha = .16f)))
            Box(Modifier.fillMaxWidth(progress).height(6.dp).background(Ink.copy(alpha = .55f)))
        }
    }
}

/**
 * Which language, which subtitles, which file.
 *
 * The question a household asks before pressing play, and until now the only way to answer it was
 * to start the film and open the player's own menus. The server's own stream index travels with the
 * choice, so picking Norwegian here is the track that actually starts.
 */
@Composable
private fun MediaTrackChoices(
    details: ContentDetails,
    audio: Int?,
    subtitle: Int?,
    version: String?,
    onAudio: (Int?) -> Unit,
    onSubtitle: (Int?) -> Unit,
    onVersion: (String) -> Unit,
) {
    if (details.audioTracks.isEmpty() && details.subtitleTracks.isEmpty() && details.versions.isEmpty()) return
    val selectedAudio = audio ?: details.audioTracks.firstOrNull { it.isDefault }?.index ?: details.audioTracks.firstOrNull()?.index
    // No stored default means no subtitles, which is what the server does too.
    val selectedSubtitle = subtitle ?: details.subtitleTracks.firstOrNull { it.isDefault }?.index ?: -1
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).testTag("detail-tracks"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // One track is a fact, not a choice: a single highlighted chip looks like something to
        // press and does nothing when pressed.
        if (details.audioTracks.size == 1) TrackFact(
            stringResource(R.string.player_audio_tracks), details.audioTracks.single().label)
        else if (details.audioTracks.size > 1) TrackChooser(
            label = stringResource(R.string.player_audio_tracks),
            tag = "audio",
            options = details.audioTracks.map { it.index to it.label },
            selected = selectedAudio,
            onSelect = onAudio,
        )
        if (details.subtitleTracks.isNotEmpty()) TrackChooser(
            label = stringResource(R.string.player_subtitles),
            tag = "subtitle",
            options = listOf(-1 to stringResource(R.string.player_off)) +
                details.subtitleTracks.map { it.index to it.label },
            selected = selectedSubtitle,
            onSelect = onSubtitle,
        )
        // More than one file means a real choice, and it was listed but not offered: the page
        // named both cuts and then started whichever one the server felt like.
        if (details.versions.size > 1) VersionChooser(details.versions, version, onVersion)
    }
}

/** Same one-line shape as the track choosers, keyed on the media source id playback needs. */
@Composable
private fun VersionChooser(versions: List<app.reelstack.data.model.MediaVersion>, selected: String?, onSelect: (String) -> Unit) {
    val order = versions.map { it.id }
    val index = order.indexOf(selected).takeIf { it >= 0 } ?: 0
    TrackChooser(
        label = stringResource(R.string.detail_version),
        tag = "version",
        options = versions.mapIndexed { position, version -> position to version.name },
        selected = index,
    ) { position -> versions.getOrNull(position)?.id?.let(onSelect) }
}

/** A stated value rather than an offered one: label, then what the file actually has. */
@Composable
private fun TrackFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted,
            modifier = Modifier.padding(end = 10.dp, top = 2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/**
 * One line: what is chosen, and one way to change it.
 *
 * A well-subtitled film carries forty languages. Laying them out as chips filled two rows with
 * plates nobody reads, buried the synopsis under them and gave a remote forty stops to pass
 * through — and five plus a "39 more" chip was still two rows. The answer to "what language is
 * this in" is one value, so the line states that value and keeps the other thirty-nine behind the
 * one control that asks for them.
 */
@Composable
private fun TrackChooser(label: String, tag: String, options: List<Pair<Int, String>>, selected: Int?,
    onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected } ?: options.first()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted,
            modifier = Modifier.padding(end = 10.dp))
        Text(current.second, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).padding(end = 10.dp))
        app.reelstack.ui.screens.Chip(
            text = stringResource(R.string.detail_change),
            chosen = false,
            tag = "detail-$tag-open",
        ) { open = true }
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(label) },
        text = {
            LazyColumn {
                items(options) { (index, name) ->
                    app.reelstack.ui.components.SpoleSecondaryButton(
                        onClick = { onSelect(index); open = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("detail-$tag-$index"),
                    ) {
                        // The current one keeps a mark rather than a highlight, so a list of forty
                        // reads as a list and not as forty buttons in two colours.
                        Text(
                            if (index == current.first) "✓  $name" else name,
                            color = if (index == current.first) Primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            app.reelstack.ui.components.SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.library_cancel)) }
        },
    )
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
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(stringResource(R.string.login_example_title), color = Muted, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
        Surface(
            onClick = { onUse(example) },
            enabled = enabled,
            interactionSource = interaction,
            color = SurfaceRaised,
            contentColor = PrimarySoft,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp).focusOutline(interaction, RoundedCornerShape(12.dp)).testTag("address-example"),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(example, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.login_example_use), color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
        Text(
            stringResource(R.string.login_example_detail, kind.displayName),
            color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
internal fun CinematicTitleHero(
    title: String,
    eyebrow: String,
    subtitle: String,
    artworkUrl: String?,
    artworkRes: Int,
    source: ServiceKind?,
    portrait: Boolean,
) {
    // Frame geometry comes from media metadata and window width, never decoded image size.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacious = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useSideBySideMedia
        val artwork: @Composable () -> Unit = {
            MediaArtwork(
                url = artworkUrl, fallbackRes = artworkRes, contentDescription = null,
                contentScale = if (portrait) ContentScale.Fit else ContentScale.Crop,
                source = source, crossfadeDurationMillis = 0,
                modifier = Modifier
                    .then(if (portrait) Modifier.width(if (spacious) 142.dp else 190.dp)
                        else if (spacious) Modifier.width(304.dp) else Modifier.fillMaxWidth())
                    .aspectRatio(if (portrait) 2f / 3f else 16f / 9f)
                    .clip(RoundedCornerShape(14.dp)).background(Ink).testTag("episode-detail-artwork"),
            )
        }
        val summary: @Composable () -> Unit = {
            DetailEyebrow(eyebrow, source)
            Text(title, color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp))
            if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
        if (spacious) {
            Row(Modifier.testTag("tablet-episode-summary"), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                artwork()
                Column(Modifier.weight(1f)) { summary() }
            }
        } else {
            Column {
                artwork()
                Column(Modifier.padding(top = 16.dp)) { summary() }
            }
        }
    }
}

@Composable
private fun DetailPill(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp, lineHeight = 17.sp,
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
                    fontSize = 12.sp, lineHeight = 17.sp,
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
                    fontSize = 12.sp, lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    session.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(session.subtitle, color = app.reelstack.ui.theme.Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
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
                SessionMetric(stringResource(R.string.details_playback), session.streamMethod, Modifier.weight(1f))
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
                        Icon(if (paused) app.reelstack.ui.components.SpoleIcons.Play else app.reelstack.ui.components.SpoleIcons.Pause, contentDescription = null)
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
        Text(label.uppercase(), color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        Text(
            value,
            color = app.reelstack.ui.theme.Text,
            fontSize = 12.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
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
    setupLink: String? = null,
) {
    val shareContext = LocalContext.current
    var credentialsStep by rememberSaveable(draft.kind) { mutableStateOf(configured) }
    var advanced by rememberSaveable(draft.kind) { mutableStateOf(false) }
    var detailsExpanded by rememberSaveable(draft.kind, configured) { mutableStateOf(!configured) }
    var showPassword by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val continueInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focus = LocalFocusManager.current
    val nextStep: () -> Unit = {
        runCatching { EndpointValidator.normalizeBaseUrl(draft.url) }
            .onSuccess {
                onUrlChange(it)
                if (television && !configured && draft.kind == ServiceKind.SEERR) onAuthModeChange(ConnectionAuthMode.QUICK_CONNECT)
                credentialsStep = true; focus.clearFocus()
            }
            .onFailure { addressError = it.message }
    }
    Column(Modifier.fillMaxSize()) {
        SheetToolbar(if (configured) draft.kind.displayName else stringResource(R.string.login_service, draft.kind.displayName), stringResource(R.string.action_close), onDismiss)
    Column(
        Modifier.weight(1f).testTag("connection-scroll").verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
    ) {
        if (configured) {
            ConnectedServiceSummary(draft.kind, detailsExpanded) { detailsExpanded = !detailsExpanded }
            if (setupLink != null) {
                SpoleSecondaryButton(onClick = {
                    runCatching {
                        shareContext.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, setupLink)
                        }, "Del Spole-oppsett"))
                    }.onFailure {
                        android.widget.Toast.makeText(shareContext, "Bruk Kopier oppsettslenkje på denne eininga.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Del oppsett med ein brukar") }
                SpoleSecondaryButton(onClick = {
                    shareContext.getSystemService(android.content.ClipboardManager::class.java)
                        .setPrimaryClip(android.content.ClipData.newPlainText("Spole-oppsett", setupLink))
                    android.widget.Toast.makeText(shareContext, "Oppsettslenkja er kopiert", android.widget.Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth()) { Text("Kopier oppsettslenkje") }
                Text("Deler berre tenesteadressene. Brukaren loggar inn med sin eigen konto.", style = MaterialTheme.typography.bodySmall)
            }
        }
        AnimatedVisibility(
            visible = !configured || detailsExpanded,
            enter = expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
        ) {
        Column {
        // A step marker only helps when it says how many steps there are.
        Text(stringResource(if (configured) R.string.login_connection else if (credentialsStep) R.string.login_step_credentials else R.string.login_step_address),
            color = Muted, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Text(stringResource(if (credentialsStep) R.string.login_account_hint else R.string.login_address_hint), color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
        if (configured) {
            ConnectionTextAction(stringResource(R.string.account_sign_out), { confirmSignOut = true }, enabled = !draft.saving, warning = true)
        }
        if (confirmSignOut) {
            SignOutConfirmation(draft.kind, { confirmSignOut = false }, { confirmSignOut = false; onRemove() })
        }
        Spacer(Modifier.height(20.dp))
        if (!credentialsStep) {
            OutlinedTextField(
                value = draft.url, onValueChange = { addressError = null; onUrlChange(it) },
                label = { Text(stringResource(R.string.login_address)) }, placeholder = { Text(exampleAddress(draft.kind)) },
                supportingText = { Text(addressError ?: stringResource(R.string.login_address_detail)) },
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
            Button(onClick = nextStep, enabled = draft.url.isNotBlank(), interactionSource = continueInteraction,
                shape = RoundedCornerShape(14.dp),
                // Material's default disabled fill is 12 % of onSurface, which on this page is
                // indistinguishable from a container. An outline keeps it readable as a button
                // that is waiting for input.
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = SurfaceRaised,
                    disabledContentColor = Muted,
                ),
                border = if (draft.url.isBlank()) androidx.compose.foundation.BorderStroke(1.dp, app.reelstack.ui.theme.ControlOutline) else null,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 54.dp)
                    .focusOutline(continueInteraction, RoundedCornerShape(14.dp)).testTag("connection-continue")) {
                Text(stringResource(R.string.login_continue), fontWeight = FontWeight.Bold)
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(draft.url, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f),
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            ConnectionTextAction(stringResource(R.string.account_edit), { credentialsStep = false }, enabled = !draft.saving)
        }
        if (draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR) ||
            (draft.kind == ServiceKind.EMBY && (advanced || draft.authMode == ConnectionAuthMode.API_KEY))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                listOfNotNull(
                    (ConnectionAuthMode.QUICK_CONNECT to "Quick Connect").takeIf { draft.kind != ServiceKind.EMBY },
                    ConnectionAuthMode.ACCOUNT to stringResource(if (draft.kind == ServiceKind.SEERR) R.string.login_jellyfin_account else R.string.login_username),
                    (ConnectionAuthMode.API_KEY to stringResource(R.string.account_api_key)).takeIf { advanced || draft.authMode == ConnectionAuthMode.API_KEY },
                ).forEach { (mode, label) ->
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    FilterChip(selected = draft.authMode == mode, onClick = { onAuthModeChange(mode) },
                        interactionSource = interaction,
                        modifier = Modifier.heightIn(min = 48.dp).focusOutline(interaction, RoundedCornerShape(10.dp)),
                        enabled = !draft.saving, label = { Text(label, fontSize = 12.sp, lineHeight = 17.sp) },
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
                label = { Text(stringResource(R.string.login_username)) }, enabled = !draft.saving, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = draft.password, onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) }, supportingText = { Text(stringResource(R.string.login_empty_password)) },
                enabled = !draft.saving, singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) app.reelstack.ui.components.SpoleIcons.EyeOff else app.reelstack.ui.components.SpoleIcons.Eye,
                            stringResource(if (showPassword) R.string.login_hide_password else R.string.login_show_password))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!draft.saving && draft.username.isNotBlank()) { focus.clearFocus(); onTestAndSave() }
                }),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(if (draft.kind == ServiceKind.SEERR) stringResource(R.string.login_seerr_account_hint)
                else stringResource(R.string.login_local_account_hint, draft.kind.displayName),
                color = Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            if (supportsJellyfinLogin) {
                val otherName = if (draft.kind == ServiceKind.SEERR) "Jellyfin" else "Seerr"
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .then(Modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceRaised))
                        .then(Modifier.toggleableLogin(!draft.saving, draft.alsoConnect) { onCompanionLoginChange(it, draft.companionUrl) })
                        .padding(horizontal = 12.dp, vertical = 6.dp)) {
                    androidx.compose.material3.Checkbox(checked = draft.alsoConnect, onCheckedChange = null, enabled = !draft.saving)
                    Text(stringResource(R.string.account_also, otherName), color = Primary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(start = 10.dp))
                }
                if (draft.alsoConnect) {
                    OutlinedTextField(value = draft.companionUrl,
                        onValueChange = { onCompanionLoginChange(true, it) }, label = { Text(stringResource(R.string.account_address_for, otherName)) },
                        enabled = !draft.saving, singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                        ),
                        shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                    Text(stringResource(R.string.account_companion_consent),
                        color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp))
                    if (runCatching { EndpointValidator.isCleartext(draft.companionUrl) }.getOrDefault(false)) {
                        Text(stringResource(R.string.account_http_warning), color = Warning, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
        if (!usesAccount && !usesQuickConnect) {
            OutlinedTextField(
                value = draft.token, onValueChange = onTokenChange,
                label = { Text(stringResource(R.string.account_token)) },
                enabled = !draft.saving, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!draft.saving && draft.token.isNotBlank()) { focus.clearFocus(); onTestAndSave() }
                }),
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(stringResource(when (draft.kind) {
                ServiceKind.RADARR, ServiceKind.SONARR -> R.string.account_arr_key_hint
                ServiceKind.SEERR -> R.string.account_seerr_key_hint
                else -> R.string.account_media_key_hint
            }), color = Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp))
        }
        ConnectionTextAction(stringResource(if (advanced) R.string.account_advanced_hide else R.string.account_advanced),
            { advanced = !advanced }, enabled = !draft.saving)
        if (advanced) {
            OutlinedTextField(value = draft.name, onValueChange = onNameChange,
                label = { Text(stringResource(R.string.account_connection_name)) }, singleLine = true, enabled = !draft.saving,
                shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(), modifier = Modifier.fillMaxWidth())
            if (!usesAccount && !usesQuickConnect && (draft.kind == ServiceKind.JELLYFIN || draft.kind == ServiceKind.EMBY)) {
                OutlinedTextField(value = draft.userId, onValueChange = onUserIdChange,
                    label = { Text(stringResource(R.string.account_profile_id)) }, singleLine = true, enabled = !draft.saving,
                    supportingText = { Text(stringResource(R.string.account_profile_hint)) },
                    shape = RoundedCornerShape(14.dp), colors = connectionFieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            }
            // The same server usually has two ways in: the LAN address at home and a proxy from
            // outside. The token belongs to the server, so one sign-in covers both.
            OutlinedTextField(
                value = draft.alternateUrl, onValueChange = onAlternateUrlChange,
                label = { Text(stringResource(R.string.account_alternate)) }, singleLine = true, enabled = !draft.saving,
                placeholder = { Text("https://spole.dømet.no") },
                supportingText = {
                    Text(stringResource(R.string.account_alternate_hint))
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
        if (!television) ConnectionSubmit(draft) { focus.clearFocus(); onTestAndSave() }
        if (draft.saving) {
            ConnectionTextAction(stringResource(R.string.account_cancel), onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        }
        }
    }
    if (television && credentialsStep && (!configured || detailsExpanded)) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 24.dp, vertical = 12.dp)) {
            ConnectionSubmit(draft) { focus.clearFocus(); onTestAndSave() }
        }
    }
    }
}

@Composable
private fun ConnectionSubmit(draft: ConnectionDraft, onSubmit: () -> Unit) {
    val usesQuickConnect = draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR) && draft.authMode == ConnectionAuthMode.QUICK_CONNECT
    val usesAccount = draft.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.SEERR, ServiceKind.EMBY) && draft.authMode == ConnectionAuthMode.ACCOUNT
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        Button(
            onClick = { onSubmit() },
            interactionSource = interaction,
            enabled = !draft.saving && (usesQuickConnect || if (usesAccount) draft.username.isNotBlank() else draft.token.isNotBlank()),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).heightIn(min = 56.dp)
                .focusOutline(interaction, RoundedCornerShape(14.dp)).testTag("connection-submit"),
        ) {
            if (draft.saving) CircularProgressIndicator(color = Ink, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Text(when {
                draft.saving && usesQuickConnect && draft.quickConnectCode != null -> stringResource(R.string.quick_waiting)
                draft.saving && usesQuickConnect -> stringResource(R.string.login_creating_code)
                draft.saving -> stringResource(R.string.login_connecting)
                usesQuickConnect && draft.quickConnectCode != null -> stringResource(R.string.login_new_code)
                usesQuickConnect -> stringResource(R.string.login_start_quick)
                usesAccount && draft.alsoConnect -> stringResource(R.string.login_both)
                usesAccount -> stringResource(R.string.login_sign_in)
                else -> stringResource(R.string.login_connect)
            }, modifier = Modifier.padding(start = if (draft.saving) 10.dp else 0.dp))
        }
}

@Composable
private fun ConnectedServiceSummary(kind: ServiceKind, expanded: Boolean, onToggle: () -> Unit) {
    val isAccount = kind == ServiceKind.JELLYFIN || kind == ServiceKind.EMBY || kind == ServiceKind.SEERR
    val stateText = stringResource(if (isAccount) R.string.account_signed_in else R.string.account_connected, kind.displayName)
    val actionText = stringResource(if (isAccount) {
        if (expanded) R.string.account_hide_login else R.string.account_show_login
    } else if (expanded) R.string.account_hide_connection else R.string.account_show_connection)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(180), label = "connection-details-arrow")
    Surface(
        color = Success.copy(alpha = .09f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
            .testTag("connected-service-summary")
            .focusOutline(interaction, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current,
                onClickLabel = actionText, onClick = onToggle)
            .semantics {
                role = Role.Button
                contentDescription = "$stateText. $actionText"
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
                Icon(app.reelstack.ui.components.SpoleIcons.DoneCircle, null, tint = Success, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stateText, color = Success, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    actionText,
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                app.reelstack.ui.components.SpoleIcons.ChevronDown, null, tint = Muted,
                modifier = Modifier.size(22.dp).rotate(arrowRotation),
            )
        }
    }
}

@Composable
internal fun QuickConnectPanel(draft: ConnectionDraft) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    var copied by remember(draft.quickConnectCode) { mutableStateOf(false) }
    if (television) {
        TvQuickConnectPanel(draft)
        return
    }
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
                            app.reelstack.ui.components.SpoleIcons.Devices,
                            contentDescription = null,
                            tint = PrimarySoft,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            stringResource(R.string.quick_login_without_password),
                            color = Color.White,
                            fontSize = 15.sp, lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(if (draft.kind == ServiceKind.SEERR) R.string.quick_seerr_intro else R.string.quick_jellyfin_intro),
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
                            stringResource(if (draft.quickConnectWaiting) R.string.quick_waiting else R.string.quick_finishing),
                            color = PrimarySoft,
                            fontSize = 12.sp, lineHeight = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                    }
                    Text(
                        code.chunked(3).joinToString("  "),
                        color = Color.White,
                        fontSize = if (television) 44.sp else 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(code)); copied = true }) {
                        Text(stringResource(if (copied) R.string.quick_copied else R.string.quick_copy))
                    }
                    Text(
                        stringResource(if (television) R.string.quick_tv_instructions else R.string.quick_instructions),
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
