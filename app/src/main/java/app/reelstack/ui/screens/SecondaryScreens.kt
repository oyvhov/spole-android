package app.reelstack.ui.screens
import app.reelstack.ui.components.SettingsServiceRow

import app.reelstack.ui.components.focusOutline
import app.reelstack.data.model.DiscoverGridTarget
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.IconButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import app.reelstack.ui.components.steadyRemoteRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.reelstack.ui.components.AppFilterRow
import app.reelstack.ui.components.ServiceSymbol
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.hasTitleMetadata
import app.reelstack.data.model.canRequestType
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import app.reelstack.ui.components.SpoleDropdownMenu as DropdownMenu
import app.reelstack.ui.components.SpoleDropdownMenuItem as DropdownMenuItem
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import app.reelstack.ui.state.DiscoverUiState
import app.reelstack.ui.state.toDiscoverUiState
import app.reelstack.ui.components.MediaArtwork
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

/**
 * How wide one cell is on a wall of covers.
 *
 * The television is the case that decides this. At 174 dp a 1080p set fits four columns, and four
 * columns of 2:3 artwork plus their captions come to more than the screen is tall — so the first
 * row of titles was cut off by the bottom edge on both Oppdag and Aktivitet, and the first row is
 * the one row every reader sees. 150 dp gives five columns, which leaves room for the captions and
 * for the top of the next row: that sliver is also how a reader on a remote learns there is more.
 *
 * Discover and Activity had drifted apart here (174 against 180, a 16 dp gap against 20) for no
 * reason anybody decided, so they share one answer now.
 */
@Composable
internal fun posterCell(): androidx.compose.ui.unit.Dp {
    val television = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val base = when {
        androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.6f -> 280.dp
        television -> 150.dp
        app.reelstack.ui.theme.LocalTabletCanvas.current -> 174.dp
        else -> 145.dp
    }
    return base * app.reelstack.ui.theme.LocalPersonalization.current.artworkSize.scale
}

private fun screenPadding(contentPadding: PaddingValues) = PaddingValues(
    start = ReelLayout.Gutter,
    top = ReelLayout.PageTop,
    end = ReelLayout.Gutter,
    bottom = contentPadding.calculateBottomPadding() + 24.dp,
)

/**
 * `searchTransitionModifier` is not this screen's modifier and must not be called `modifier`.
 *
 * A screen fills the window and takes no modifier of its own; this one is threaded down to the
 * search field alone, so that the field can keep its identity while Home and Oppdag swap places.
 * Renaming it would make the signature claim something untrue, so the lint rule is answered here
 * rather than obeyed.
 */
@Suppress("ModifierParameter")
@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onDetails: (String) -> Unit,
    hubActions: DiscoverHubActions,
    onAccountClick: () -> Unit = {},
    onLibraryDetails: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onGridMore: () -> Unit = {},
    onGridBack: () -> Unit = {},
    searchTransitionModifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val television = app.reelstack.ui.components.isTelevision()
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(searchOpen) {
        if (television && searchOpen) {
            withFrameNanos { }
            runCatching { searchFocus.requestFocus() }
            keyboard?.show()
        }
    }
    val account = state.verifiedPanelAccount(ServiceKind.SEERR)?.takeIf { it.isPersonal }
    val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }
    val accountButton: @Composable () -> Unit = {
        AccountAvatarButton(
            account = account,
            connection = connection,
            onClick = onAccountClick,
            description = account?.let { stringResource(R.string.account_change_seerr, it.displayName) }
                ?: stringResource(R.string.account_signin_seerr),
            testTag = "discover-account",
            modifier = Modifier.padding(top = 2.dp),
        )
    }
    val discoverTop: @Composable () -> Unit = {
        app.reelstack.ui.components.DiscoverHeader(heading = {
            Text(stringResource(R.string.nav_discover), color = TextColor, style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 6.dp))
        }, account = accountButton, search = {
            // The box is always on screen. On television it is a button that looks exactly like
            // the field until it is chosen, because focusing a real text field is what summons
            // the on-screen keyboard. Pressing it swaps in the field below, focused and with the
            // keyboard, which is the only moment a remote user wants either.
            if (television && !searchOpen) TvSearchBox(
                query = state.searchQuery,
                modifier = searchTransitionModifier.focusRequester(searchFocus),
                onClear = { onSearch("") },
                onOpen = { searchOpen = true },
            )
            else androidx.compose.material3.TextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                singleLine = true,
                leadingIcon = { Icon(app.reelstack.ui.components.SpoleIcons.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.isSearching) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearch("") }) {
                            Icon(app.reelstack.ui.components.SpoleIcons.Close, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.discover_search_hint), fontSize = 14.sp, lineHeight = 20.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    // Back to the button, so the keyboard goes with it and the results are visible.
                    if (television) searchOpen = false
                }),
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedIndicatorColor = Primary,
                    unfocusedIndicatorColor = app.reelstack.ui.theme.ControlOutline,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Primary,
                ),
                modifier = searchTransitionModifier.fillMaxWidth().focusRequester(searchFocus)
                    .testTag("discover-search"),
            )
        })
    }
    ReelPage(media = true) {
        val grid = state.hub.grid
        when {
            grid != null -> DiscoverGridContent(
                title = when (grid) {
                    is DiscoverGridTarget.Row -> grid.kind.title()
                    is DiscoverGridTarget.Genre -> grid.genre.name
                },
                row = state.hub.gridRow, contentPadding = contentPadding, onTitle = onDetails, onMore = onGridMore, onBack = onGridBack)
            state.searchQuery.isBlank() -> DiscoverHubContent(state, contentPadding, hubActions, header = {
                // On a television the page opens on the posters; search is an icon one press up,
                // and it opens the full search, where people are found too.
                if (television) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    IconButton(onClick = hubActions.onSearch, interactionSource = interaction,
                        modifier = Modifier.focusOutline(interaction, CircleShape).testTag("discover-search-icon")) {
                        Icon(app.reelstack.ui.components.SpoleIcons.Search, stringResource(R.string.discover_search_hint), tint = TextColor)
                    }
                    accountButton()
                } else discoverTop()
            })
            else -> DiscoverSearchGrid(state, contentPadding, discoverTop, onDetails, onLibraryDetails, onLoadMore, hubActions.onPerson)
        }
    }
}

/** People lead the results; three lines leave the titles below them in view. */
internal const val SEARCH_PEOPLE_SHOWN = 3

/** What the box above found: people, your own titles, then everything Seerr can add. */
@Composable
private fun DiscoverSearchGrid(
    state: DiscoverUiState,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    onDetails: (String) -> Unit,
    onLibraryDetails: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPerson: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val visible = state.searchResults
    LazyVerticalGrid(
        columns = GridCells.Adaptive(posterCell()),
        state = gridState,
        contentPadding = screenPadding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize().steadyRemoteRows(gridState).testTag("discover-grid"),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        if (!state.isSearching && state.searchPeople.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.discover_people), color = Muted, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(state.searchPeople.take(SEARCH_PEOPLE_SHOWN), key = { "search-person-${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { person ->
                PersonLine(person) { onPerson(person.id) }
            }
        }
        if (state.librarySearchResults.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // Heading and note on one line. Stacked, this section spent three lines saying what
                // the row of covers under it shows anyway.
                Row(
                    Modifier.padding(top = 4.dp, bottom = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.search_libraries), color = TextColor, fontSize = 18.sp,
                        lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.search_ready), color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            items(state.librarySearchResults, key = { "library-hit-${it.id}" }) { media ->
                LibraryHitCard(media) { onLibraryDetails(media.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.search_add_new), color = TextColor, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
            }
        }
        if (state.isSearching) {
            items(8, key = { "discover-loading-$it" }) { DiscoverSkeleton(Modifier.fillMaxWidth()) }
        } else if (visible.isEmpty() && state.searchPeople.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                app.reelstack.ui.components.QuietEmptyState(
                    state.searchError ?: when {
                        state.librarySearchResults.isNotEmpty() -> stringResource(R.string.search_no_new, state.searchQuery.trim())
                        else -> stringResource(R.string.search_no_results, state.searchQuery.trim())
                    },
                    icon = if (state.searchError != null) app.reelstack.ui.components.SpoleIcons.Wifi else app.reelstack.ui.components.SpoleIcons.Search,
                    modifier = Modifier,
                )
            }
        } else {
            items(visible, key = DiscoverMedia::id) { media ->
                PosterOnlyCard(media, { onDetails(media.id) }, Modifier.fillMaxWidth())
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
                            Text(stringResource(R.string.search_loading_more), modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Text(stringResource(R.string.search_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSearchBox(query: String, modifier: Modifier, onClear: () -> Unit, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(SurfaceRaised)
            .border(1.dp, app.reelstack.ui.theme.ControlOutline, shape)
            .focusOutline(interaction, shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current,
                role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, end = 6.dp)
            .testTag("discover-search"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(app.reelstack.ui.components.SpoleIcons.Search, null, tint = Muted, modifier = Modifier.size(20.dp))
        Text(
            query.ifBlank { stringResource(R.string.home_search) },
            color = if (query.isBlank()) Muted else TextColor,
            fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        if (query.isNotBlank()) IconButton(onClick = onClear, modifier = Modifier.testTag("discover-search-clear")) {
            Icon(app.reelstack.ui.components.SpoleIcons.Close, stringResource(R.string.search_clear), tint = Muted)
        }
    }
}

/** A title you already own: no request action, just the way into its details. */
@Composable
internal fun LibraryHitCard(media: LibraryMedia, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && app.reelstack.ui.theme.LocalMotionEnabled.current) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.tween(120),
        label = "hit-card-spring",
    )
    val shape = RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner)
    Column(
        Modifier.fillMaxWidth()
            .zIndex(if (focused) 10f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = app.reelstack.ui.components.mediaCardIndication(),
                onClickLabel = stringResource(R.string.flow_detail_named, media.title), onClick = onClick)
            .testTag("library-hit-${media.id}"),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).focusOutline(interaction, shape).clip(shape).background(SurfaceRaised)
            .testTag("library-hit-artwork-${media.id}")) {
            MediaArtwork(media.artworkUrl, null, Modifier.matchParentSize(), fallbackRes = media.artworkRes, ContentScale.Crop, source = media.source)
            Row(
                Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = .72f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(app.reelstack.ui.components.SpoleIcons.DoneCircle, null, tint = Success, modifier = Modifier.size(12.dp))
                Text(stringResource(R.string.filter_available), color = Color.White, fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 5.dp))
            }
        }
        app.reelstack.ui.components.RailCardCaption(media.title, media.subtitle, titleMaxLines = 2,
            reserveTitleLines = if (app.reelstack.ui.components.isTelevision()) 2 else 1,
            subtitleMaxLines = 1,
            modifier = Modifier.padding(top = 8.dp).testTag("library-hit-caption-${media.id}"),
            titleModifier = Modifier.testTag("library-hit-title-${media.id}"),
            subtitleModifier = Modifier.testTag("library-hit-subtitle-${media.id}"))
    }
}

/** Compatibility entry point for existing Compose tests while the root state is being split. */
@Composable
fun DiscoverScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onSearch: (String) -> Unit,
    onDetails: (String) -> Unit,
    hubActions: DiscoverHubActions = DiscoverHubActions({}, {}, {}, {}, {}, {}, {}),
    onAccountClick: () -> Unit = {},
    onLibraryDetails: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    searchTransitionModifier: Modifier = Modifier,
) = DiscoverScreen(
    state = state.toDiscoverUiState(),
    contentPadding = contentPadding,
    onSearch = onSearch,
    onDetails = onDetails,
    hubActions = hubActions,
    onAccountClick = onAccountClick,
    onLibraryDetails = onLibraryDetails,
    onLoadMore = onLoadMore,
    searchTransitionModifier = searchTransitionModifier,
)

private enum class SettingsSection(val label: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ACCOUNTS(R.string.settings_services, app.reelstack.ui.components.SpoleIcons.Server),
    APPEARANCE(R.string.personal_appearance, app.reelstack.ui.components.SpoleIcons.Palette),
    PLAYBACK(R.string.personal_playback, app.reelstack.ui.components.SpoleIcons.Play),
    HOME(R.string.settings_home, app.reelstack.ui.components.SpoleIcons.Screen),
    UPDATES(R.string.settings_updates, app.reelstack.ui.components.SpoleIcons.Bell),
    ABOUT(R.string.settings_about, app.reelstack.ui.components.SpoleIcons.Info),
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
    onManageLibraries: () -> Unit = {},
    onHomeRowOrderChange: (List<app.reelstack.data.model.HomeRow>) -> Unit = {},
    onSignOutAll: () -> Unit = {},
    onAddProfile: () -> Unit = {},
    onClearLibraryCache: () -> Unit = {},
    onRequestPinSetup: (String) -> Unit = {},
    onDisablePin: () -> Unit = {},
    homeEditor: HomeEditorActions = HomeEditorActions(),
    /** Downloads have their own row in Settings whether or not they are also in the menu. */
    onOpenDownloads: () -> Unit = {},
) {
    val television = app.reelstack.ui.components.isTelevision()
    androidx.compose.runtime.CompositionLocalProvider(app.reelstack.ui.components.LocalSettingsButtonStyle provides true) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val policy = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value)
        val wide = television || policy.useSettingsPanes(androidx.compose.ui.platform.LocalDensity.current.fontScale)
        if (wide) TvSettingsScreen(state, contentPadding, onConnectionClick, onNotificationsChange,
            onWifiOnlyChange, onHomeSectionChange, onAccountClick, onManageLibraries, onHomeRowOrderChange, onSignOutAll,
            onAddProfile, onClearLibraryCache, onRequestPinSetup, onDisablePin, homeEditor, onOpenDownloads = onOpenDownloads)
        else MobileSettingsScreen(state, contentPadding, onConnectionClick, onNotificationsChange,
            onWifiOnlyChange, onHomeSectionChange, onAccountClick, onManageLibraries, onHomeRowOrderChange, onSignOutAll,
            onAddProfile, onClearLibraryCache, onRequestPinSetup, onDisablePin, homeEditor, onOpenDownloads = onOpenDownloads)
    }
    }
}

/**
 * The attributions Spole is obliged to show once it is distributed to anyone but its author: TMDB
 * requires acknowledgement for the artwork it serves, and five separate projects lend their names
 * and marks to an app none of them endorses.
 */
@Composable
internal fun AttributionCard() {
    Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.attribution_services),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
            )
            Text(
                stringResource(R.string.attribution_tmdb),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                stringResource(R.string.attribution_libraries),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * Only appears when there is something to send. A crash report row on an app that has never
 * crashed is a permanent reminder of a problem the user does not have.
 */
@Composable
internal fun CrashReportRow() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        report = withContext(Dispatchers.IO) { app.reelstack.diagnostics.CrashReporter.report(context) }
    }
    val current = report ?: return
    val shareLabel = stringResource(R.string.crash_share)
    val shareSubject = stringResource(R.string.crash_subject)
    Surface(color = app.reelstack.ui.theme.Surface, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.crash_title), color = TextColor, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.crash_note),
                color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.padding(top = 12.dp)) {
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = {
                    context.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject)
                                .putExtra(android.content.Intent.EXTRA_TEXT, current),
                            shareLabel,
                        ),
                    )
                }) { Text(shareLabel, color = Primary, fontSize = 13.sp, lineHeight = 18.sp) }
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = {
                    app.reelstack.diagnostics.CrashReporter.clear(context)
                    report = null
                }) { Text(stringResource(R.string.action_delete), color = Muted, fontSize = 13.sp, lineHeight = 18.sp) }
            }
        }
    }
}

/**
 * Says what is actually true about this device's connections instead of only asserting that
 * secrets are encrypted.
 */
@Composable
internal fun PrivacyCard(state: ReelstackUiState) {
    val configured = state.connections.filter { it.baseUrl.isNotBlank() }
    val cleartext = configured.filter { it.baseUrl.startsWith("http://", ignoreCase = true) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceRaised,
        modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(17.dp)) {
            Icon(app.reelstack.ui.components.SpoleIcons.Shield, contentDescription = null, tint = Success,
                modifier = Modifier.padding(top = 1.dp).size(24.dp))
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.privacy_title), color = TextColor, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.privacy_tokens),
                    color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp))
                if (configured.isNotEmpty()) Text(
                    when {
                        cleartext.isEmpty() && configured.size == 1 -> stringResource(R.string.privacy_https)
                        cleartext.isEmpty() -> androidx.compose.ui.res.pluralStringResource(R.plurals.privacy_all_https, configured.size, configured.size)
                        else -> androidx.compose.ui.res.pluralStringResource(R.plurals.privacy_http, configured.size, cleartext.size, configured.size,
                            cleartext.joinToString(", ") { it.kind.displayName })
                    },
                    color = if (cleartext.isEmpty()) Muted else Caution,
                    fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun AppIdentity() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 26.dp),
    ) {
        app.reelstack.ui.components.SpoleBrandMark(Modifier.size(40.dp), "Spole-logo")
        Column(Modifier.padding(start = 13.dp)) {
            Text("Spole", color = TextColor, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                color = Muted,
                fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    // No start inset. Every card, paragraph and heading on this page starts at the content edge,
    // and 4 dp was enough to see: «Kontoen din» began at x=76 and «Tenestene dine» at x=85.
    Text(text, color = Muted, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 26.dp, bottom = 10.dp).semantics { heading() })
}


@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) = app.reelstack.ui.components.SettingsPreferenceRow(icon, label, description, checked, null, onCheckedChange)

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
