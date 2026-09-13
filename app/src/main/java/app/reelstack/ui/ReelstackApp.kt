package app.reelstack.ui
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.vector
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onKeyEvent
import app.reelstack.data.model.visibleMenu

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import app.reelstack.ui.screens.WelcomeScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.reelstack.R
import app.reelstack.ui.screens.ActivityScreen
import app.reelstack.ui.screens.DiscoverScreen
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.screens.SettingsScreen
import app.reelstack.ui.theme.Ink
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.PrimarySoft
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.SurfaceRaised

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ReelstackApp(viewModel: ReelstackViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.signingOut) {
        BackHandler { }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator()
                Text(androidx.compose.ui.res.stringResource(R.string.sign_out_all_working),
                    modifier = Modifier.padding(top = 20.dp))
            }
        }
        return
    }
    val connectionDraft by viewModel.connectionDraft.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabStates = rememberSaveableStateHolder()
    // Built once and handed to every rail that shows library cards, so a title offers the same
    // three choices wherever it appears.
    val cardActions = remember(viewModel) {
        app.reelstack.ui.screens.MediaCardActions(
            onRemoveFromResume = { viewModel.hideFromResume(it.id) },
            onFavourite = { media, favourite -> viewModel.setMediaFavourite(media.id, favourite) },
            onPlayed = { media, played -> viewModel.setMediaPlayed(media.id, played) },
        )
    }
    var pendingSearchFocus by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val tvRail = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    var tvRailFocused by remember { mutableStateOf(false) }
    var railHasFocus by remember { mutableStateOf(false) }
    var railEntryRequested by remember { mutableStateOf(false) }
    var moveIntoContent by remember { mutableStateOf(tvRail) }
    val contentFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val railFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    var contentHasFocus by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val selectTab: (AppTab) -> Unit = { tab ->
        pendingSearchFocus = false
        focusManager.clearFocus()
        keyboard?.hide()
        viewModel.selectTab(tab)
        if (tvRail) { tvRailFocused = false; moveIntoContent = true }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        var wasStopped = false
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) wasStopped = true
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START && wasStopped) {
                wasStopped = false
                viewModel.returnedToApp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycleOwner, state.selectedTab, state.activeSheet) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            if (viewModel.uiState.value.selectedTab == AppTab.HOME || viewModel.uiState.value.activeSheet is AppSheet.SessionDetails) {
                // Jellyfin will tell us when playback changes, so ask it to. While that channel is
                // open the loop below is a safety net rather than the source of truth; when it is
                // not — an older server, a proxy that strips upgrades — nothing is lost but the
                // saving, and the old cadence comes straight back.
                try {
                    viewModel.openSessionChannel()
                    while (true) {
                        if (viewModel.uiState.value.selectedTab == AppTab.HOME) viewModel.retryIncompleteHomeFeed()
                        viewModel.refreshPlayback()
                        kotlinx.coroutines.delay(
                            when {
                                viewModel.sessionChannelLive.value -> 60_000L
                                viewModel.uiState.value.sessions.isEmpty() -> 15_000L
                                else -> 5_000L
                            },
                        )
                    }
                } finally {
                    viewModel.closeSessionChannel()
                }
            }
        }
    }
    // Following a request costs a Seerr profile call, a request listing and up to twenty detail
    // lookups. Polling that every 30 seconds from every tab kept a self-hosted server busy for a
    // list nobody had on screen, so the fast cadence now belongs to the tab that shows it. The tab
    // is read inside the loop so switching tabs adapts the delay without restarting the poll.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshTrackedRequests(background = true)
                kotlinx.coroutines.delay(
                    if (viewModel.uiState.value.selectedTab == AppTab.ACTIVITY) 30_000L else 300_000L,
                )
            }
        }
    }
    BackHandler(enabled = state.activeSheet == null && !state.showOnboarding && state.selectedTab != AppTab.HOME) {
        selectTab(AppTab.HOME)
    }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }

    // A wide window moves navigation to a side rail so the bottom bar does not stretch four
    // icons across a tablet, and so the content column keeps its own width. Measured from the
    // window, not the device configuration, so split-screen is handled correctly.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Ink).onPreviewKeyEvent { event ->
        if (tvRail && event.key == androidx.compose.ui.input.key.Key.DirectionLeft) {
            if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) railEntryRequested = true
            else if (!railHasFocus) railEntryRequested = false
        }
        false
    }) {
    val windowLayout = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value)
    val wideWindow = windowLayout.useNavigationRail
    val showRail = !state.showOnboarding && wideWindow
    val personalization = app.reelstack.ui.theme.LocalPersonalization.current
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val preferences = remember(appContext) { app.reelstack.data.repository.AppPreferencesRepository(appContext) }
    LaunchedEffect(personalization.visibleMenu()) {
        if (state.selectedTab.name !in personalization.visibleMenu()) selectTab(AppTab.HOME)
    }
    LaunchedEffect(state.isRefreshing) {
        // Home replaces its header with the feature when the first library response arrives.
        // Recover only an empty focus path, never move focus away from the remote's current target.
        if (tvRail && !state.isRefreshing && !contentHasFocus && !tvRailFocused && state.activeSheet == null)
            moveIntoContent = true
    }
    LaunchedEffect(moveIntoContent, state.selectedTab, state.showOnboarding, state.libraryLoading, state.isRefreshing) {
        if (tvRail && moveIntoContent && !state.showOnboarding && state.activeSheet == null) {
            kotlinx.coroutines.delay(240)
            if (contentHasFocus || runCatching { contentFocus.requestFocus() }.getOrDefault(false)) moveIntoContent = false
        }
    }
    val expandedRail = showRail && if (tvRail) tvRailFocused else
        (personalization.sidebarExpanded ?: windowLayout.expandSidebarByDefault)
    Box(modifier = Modifier.fillMaxSize()) {
        app.reelstack.ui.components.SeasonalBackdrop(Modifier.matchParentSize())
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!state.showOnboarding && !wideWindow) ReelstackBottomBar(
                    selectedTab = state.selectedTab,
                    onSelect = selectTab,
                )
            },
        ) { paddingValues ->
            Row(Modifier.fillMaxSize().padding(paddingValues)) {
            if (showRail) {
                SidebarSlot(if (tvRail) false else expandedRail, hidden = tvRail && personalization.hideTvSidebar) {
                ReelstackNavigationRail(
                    selectedTab = state.selectedTab,
                    onSelect = selectTab,
                    expanded = expandedRail,
                    modifier = Modifier.focusRequester(railFocus).graphicsLayer {
                        translationX = if (tvRail && personalization.hideTvSidebar && !expandedRail) -200.dp.toPx() else 0f
                        alpha = if (tvRail && personalization.hideTvSidebar && !expandedRail) 0f else 1f
                    }.onPreviewKeyEvent { event ->
                        if (tvRail && event.key == androidx.compose.ui.input.key.Key.DirectionRight &&
                            event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                            contentFocus.requestFocus()
                            true
                        } else false
                    },
                    onFocusWithin = { hasFocus ->
                        railHasFocus = hasFocus
                        tvRailFocused = hasFocus && railEntryRequested
                        if (!hasFocus) railEntryRequested = false
                        else if (!railEntryRequested) moveIntoContent = true
                    },
                    shortcuts = state.libraryShortcuts,
                    libraryIcons = state.libraryIcons,
                    selectedLibraryId = state.libraryPath.firstOrNull()?.first.takeIf { state.selectedTab == AppTab.LIBRARY },
                    onLibrarySelect = { id ->
                        focusManager.clearFocus()
                        viewModel.openLibraryShortcut(id)
                        if (tvRail) { tvRailFocused = false; moveIntoContent = true }
                    },
                    onExpandedChange = { preferences.personalization = personalization.copy(sidebarExpanded = it) },
                )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize().focusRequester(contentFocus).focusRestorer()
                .onKeyEvent { event ->
                    if (tvRail && showRail && state.activeSheet == null &&
                        event.key == androidx.compose.ui.input.key.Key.DirectionLeft &&
                        event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        // Compose's default directional traversal happens after onKeyEvent.
                        // Give the row its left-hand neighbour before falling back to the rail.
                        if (!focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Left)) {
                            railEntryRequested = true
                            railFocus.requestFocus()
                        }
                        true
                    } else false
                }
                .onFocusChanged { contentHasFocus = it.hasFocus }.focusGroup()) {
            if (state.showOnboarding) {
                WelcomeScreen(
                    state = state,
                    modifier = Modifier,
                    onConnect = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                    onContinue = viewModel::completeOnboarding,
                    onCombined = viewModel::openCombinedSetup,
                )
            } else {
            SharedTransitionLayout {
            val navigation = updateTransition(state.selectedTab, label = "navigation-state")
            navigation.AnimatedContent(
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                val searchTransition = Modifier.sharedBounds(
                    rememberSharedContentState(key = "home-discover-search"),
                    animatedVisibilityScope = this,
                    boundsTransform = { _, _ -> tween(320, easing = FastOutSlowInEasing) },
                    enter = fadeIn(tween(180, delayMillis = 80)),
                    exit = fadeOut(tween(120)),
                )
                tabStates.SaveableStateProvider(tab) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        state = state,
                        contentPadding = PaddingValues(0.dp),
                        onSessionClick = { viewModel.openSheet(AppSheet.SessionDetails(it)) },
                        onPlaybackToggle = viewModel::togglePlayback,
                        onMediaClick = viewModel::openIncomingDetails,
                        onLibraryClick = viewModel::openLibraryDetails,
                        onUpcomingClick = viewModel::openUpcomingDetails,
                        onCalendarClick = { viewModel.openSheet(AppSheet.UpcomingCalendar) },
                        onRefresh = { viewModel.refreshLiveData(userInitiated = true) },
                        onAccountClick = { viewModel.selectTab(AppTab.SETTINGS) },
                        onDiscoverClick = viewModel::openRecommendationDetails,
                        onSearchClick = {
                            if (state.selectedTab == AppTab.HOME && !pendingSearchFocus) {
                                pendingSearchFocus = true
                                viewModel.selectTab(AppTab.DISCOVER)
                            }
                        },
                        searchTransitionModifier = searchTransition,
                        showSearch = windowLayout.showHomeSearch,
                        showBrand = !showRail,
                        cardActions = cardActions,
                    )
                    AppTab.DISCOVER -> DiscoverScreen(
                        state = state,
                        contentPadding = PaddingValues(0.dp),
                        onSearch = viewModel::setSearchQuery,
                        onRequest = viewModel::requestMedia,
                        onDetails = viewModel::openDiscoverDetails,
                        onAccountClick = viewModel::openSeerrAccount,
                        onLibraryDetails = viewModel::openLibraryDetails,
                        onLoadMore = viewModel::loadMoreSearchResults,
                        searchTransitionModifier = searchTransition,
                        prepareSearch = pendingSearchFocus,
                        searchReady = navigation.currentState == AppTab.DISCOVER && !navigation.isRunning,
                        onSearchFocusConsumed = { pendingSearchFocus = false },
                    )
                    AppTab.LIBRARY -> app.reelstack.ui.screens.LibraryScreen(state, viewModel::browseLibrary,
                        viewModel::openLibraryEntry, viewModel::libraryBack, viewModel::filterLibrary,
                        onShelfOpen = viewModel::openLibraryDetails, cardActions = cardActions)
                    AppTab.ACTIVITY -> ActivityScreen(state, PaddingValues(0.dp), viewModel::openActivityDetails,
                        viewModel::setFollowNotification, viewModel::refreshTrackedRequests, viewModel::openSeerrAccount,
                        viewModel::cancelTrackedRequest, viewModel::openRequestHistory,
                        viewModel::closeRequestHistory, viewModel::loadRequestHistory)
                    AppTab.SETTINGS -> SettingsScreen(
                        state = state,
                        contentPadding = PaddingValues(0.dp),
                        onConnectionClick = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                        onNotificationsChange = viewModel::setNotifications,
                        onWifiOnlyChange = viewModel::setWifiOnly,
                        onHomeSectionChange = viewModel::setHomeSectionVisible,
                        onHomeRowOrderChange = viewModel::setHomeRowOrder,
                        onSignOutAll = viewModel::signOutAll,
                        onManageLibraries = viewModel::openLibraryChoices,
                        onAccountClick = { kind ->
                            if (kind == app.reelstack.data.model.ServiceKind.SEERR) viewModel.openSeerrAccount()
                            else viewModel.openSheet(AppSheet.ConnectionEditor(kind))
                        },
                    )
                }
                }
            }
            }
            }
            }
            }
        }
    }
    }

    ReelstackSheets(
        state = state,
        connectionDraft = connectionDraft,
        onDismiss = viewModel::closeSheet,
        onPlaybackToggle = viewModel::togglePlayback,
        onConnectionNameChange = viewModel::updateConnectionName,
        onConnectionUrlChange = viewModel::updateConnectionUrl,
        onConnectionTokenChange = viewModel::updateConnectionToken,
        onConnectionUserIdChange = viewModel::updateConnectionUserId,
        onConnectionAlternateUrlChange = viewModel::updateConnectionAlternateUrl,
        onConnectionAuthModeChange = viewModel::updateConnectionAuthMode,
        onConnectionUsernameChange = viewModel::updateConnectionUsername,
        onConnectionPasswordChange = viewModel::updateConnectionPassword,
        onTestAndSaveConnection = viewModel::testAndSaveConnection,
        onRemoveConnection = viewModel::removeConnection,
        onCompanionLoginChange = viewModel::updateCompanionLogin,
        onImportSetupLink = viewModel::importSetupLink,
        onCancelConnection = viewModel::cancelConnectionSetup,
        onAddMedia = viewModel::requestMedia,
        onUpcomingClick = viewModel::openUpcomingDetails,
        onBackToCalendar = viewModel::backToCalendar,
        onSeerrAccount = viewModel::openSeerrAccount,
        onRequestSeason = viewModel::setRequestSeason,
        onRequestNotification = viewModel::setRequestNotification,
        onConfirmRequest = viewModel::confirmRequest,
        onSeasonWatch = viewModel::setSeasonWatch,
        onFavourite = viewModel::setMediaFavourite,
        onPlayed = viewModel::setMediaPlayed,
        onSeason = viewModel::selectSeason,
    )
    if (state.libraryChoicesOpen) app.reelstack.ui.screens.LibraryChoicesDialog(state,
        viewModel::closeLibraryChoices, viewModel::openLibraryChoices, viewModel::saveLibraryChoices)
    app.reelstack.update.AppUpdateHost(state.selectedTab == AppTab.HOME && state.activeSheet == null && !state.showOnboarding && !state.libraryChoicesOpen)
}

private data class TabItem(
    val tab: AppTab,
    val label: Int,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(AppTab.HOME, app.reelstack.R.string.nav_home, app.reelstack.ui.components.SpoleIcons.Home),
    TabItem(AppTab.LIBRARY, R.string.nav_library, app.reelstack.ui.components.SpoleIcons.Library),
    TabItem(AppTab.DISCOVER, app.reelstack.R.string.nav_discover, app.reelstack.ui.components.SpoleIcons.Discover),
    TabItem(AppTab.ACTIVITY, app.reelstack.R.string.nav_activity, app.reelstack.ui.components.SpoleIcons.Activity),
    TabItem(AppTab.SETTINGS, app.reelstack.R.string.nav_settings, app.reelstack.ui.components.SpoleIcons.Settings),
)

@Composable
private fun ReelstackBottomBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    val visibleTabs = app.reelstack.ui.theme.LocalPersonalization.current.visibleMenu()
        .mapNotNull { name -> tabs.find { it.tab.name == name } }
    // Five full labels cannot fit at large text sizes. Keep the current page readable and
    // expose the same destinations in a labelled menu, without shrinking the user's text.
    if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.6f) {
        var expanded by remember { mutableStateOf(false) }
        val current = tabs.first { it.tab == selectedTab }
        val currentLabel = androidx.compose.ui.res.stringResource(current.label)
        val menuLabel = androidx.compose.ui.res.stringResource(R.string.settings_tv_navigation)
        Box(Modifier.fillMaxWidth().background(Ink).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("bottom-navigation")) {
            androidx.compose.material3.OutlinedButton(onClick = { expanded = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .semantics { contentDescription = "$menuLabel · $currentLabel" }) {
                Icon(current.icon, null, Modifier.size(24.dp))
                Text(currentLabel, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.labelLarge)
                Icon(app.reelstack.ui.components.SpoleIcons.Menu, null, Modifier.size(24.dp))
            }
            androidx.compose.material3.DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                visibleTabs.forEach { item ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(item.label)) },
                        leadingIcon = { Icon(item.icon, null) },
                        trailingIcon = { if (item.tab == selectedTab) Icon(app.reelstack.ui.components.SpoleIcons.Done, null) },
                        modifier = Modifier.semantics { selected = item.tab == selectedTab },
                        onClick = { expanded = false; onSelect(item.tab) })
                }
            }
        }
        return
    }
    NavigationBar(
        containerColor = Ink,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.navigationBarsPadding().heightIn(min = 76.dp).testTag("bottom-navigation"),
    ) {
            visibleTabs.forEach { item ->
                NavigationBarItem(
                    selected = item.tab == selectedTab,
                    onClick = { onSelect(item.tab) },
                    icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(23.dp)) },
                    label = {
                        Text(
                            androidx.compose.ui.res.stringResource(item.label),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        indicatorColor = SurfaceRaised,
                        unselectedIconColor = app.reelstack.ui.theme.Muted,
                        unselectedTextColor = app.reelstack.ui.theme.Muted,
                    ),
                )
            }
    }
}

@Composable
internal fun SidebarSlot(expanded: Boolean, hidden: Boolean = false, content: @Composable () -> Unit) {
    // Commit the page width once. The rail reveals/clips above it instead of resizing every
    // poster, gradient and lazy grid on every animation frame.
    Box(Modifier.width(if (hidden) 0.dp else if (expanded) 200.dp else 80.dp).fillMaxHeight().zIndex(1f).testTag("sidebar-slot")) {
        Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) { content() }
    }
}

@Composable
internal fun ReelstackNavigationRail(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    onFocusWithin: (Boolean) -> Unit = {},
    shortcuts: List<Pair<String, String>> = emptyList(),
    libraryIcons: Map<String, app.reelstack.data.model.LibraryIcon> = emptyMap(),
    selectedLibraryId: String? = null,
    onLibrarySelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val selectedFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val width by androidx.compose.animation.core.animateDpAsState(
        if (expanded) 200.dp else 80.dp, tween(220, easing = FastOutSlowInEasing), label = "sidebar-width")
    val labelAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) 1f else 0f, tween(140), label = "sidebar-labels")
    val tv = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val toggleLabel = androidx.compose.ui.res.stringResource(if (expanded) R.string.sidebar_collapse else R.string.sidebar_expand)
    val brandInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(modifier.width(width).fillMaxHeight().clip(RoundedCornerShape(0.dp))
        .testTag("side-navigation").background(app.reelstack.ui.theme.Surface)) {
    app.reelstack.ui.components.SeasonalBackdrop(Modifier.matchParentSize(), menu = true)
    Column(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(200.dp).fillMaxHeight()
        .onFocusChanged { if (tv) onFocusWithin(it.hasFocus) }
        .focusProperties { onEnter = { if (tv) selectedFocus.requestFocus() } }.focusGroup()
        .padding(horizontal = 12.dp, vertical = 24.dp)
        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).then(if (!tv) Modifier
            .testTag("sidebar-toggle").semantics { contentDescription = toggleLabel }
            .clip(RoundedCornerShape(16.dp))
            .focusOutline(brandInteraction, RoundedCornerShape(16.dp))
            .clickable(interactionSource = brandInteraction, indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.Button, onClick = { onExpandedChange(!expanded) }) else Modifier)
            .padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            app.reelstack.ui.components.SpoleBrandMark(Modifier.size(28.dp))
            Text("Spole", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp)
                    .graphicsLayer { alpha = labelAlpha }.clearAndSetSemantics {})
        }
        app.reelstack.ui.theme.LocalPersonalization.current.visibleMenu().mapNotNull { name -> tabs.find { it.tab.name == name } }.forEach { item ->
            if (item.tab == AppTab.SETTINGS) shortcuts.forEach { (id, name) ->
                SidebarControl(name, (libraryIcons[id] ?: app.reelstack.data.model.LibraryIcon.LIBRARY).vector(), selectedLibraryId == id,
                    { onLibrarySelect(id) }, labelAlpha, Role.Tab, Modifier.width(width - 24.dp)
                        .then(if (selectedLibraryId == id) Modifier.focusRequester(selectedFocus) else Modifier).testTag("wide-library-$id"))
            }
            SidebarControl(androidx.compose.ui.res.stringResource(item.label), item.icon, selectedTab == item.tab &&
                (item.tab != AppTab.LIBRARY || shortcuts.none { it.first == selectedLibraryId }),
                { onSelect(item.tab) }, labelAlpha, Role.Tab, Modifier.width(width - 24.dp)
                    .then(if (selectedTab == item.tab && (item.tab != AppTab.LIBRARY || shortcuts.none { it.first == selectedLibraryId }))
                        Modifier.focusRequester(selectedFocus) else Modifier).testTag("wide-tab-${item.tab.name}"))
        }
    }
}

}

/** Keep the same focusable nodes and icon positions in both sizes. Only labels fade and clip. */
@Composable
private fun SidebarControl(label: String, icon: ImageVector, selected: Boolean,
    onClick: () -> Unit, labelAlpha: Float, role: Role, modifier: Modifier) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Row(modifier.fillMaxWidth().heightIn(min = 58.dp).clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .border(if (focused) 2.dp else 0.dp, if (focused) Primary else Color.Transparent, shape)
        .semantics { contentDescription = label }
        .selectable(selected, role = role, interactionSource = interaction,
            indication = androidx.compose.foundation.LocalIndication.current, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected || focused) Primary else app.reelstack.ui.theme.Muted, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected || focused) MaterialTheme.colorScheme.onSurface else app.reelstack.ui.theme.Muted,
            style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 14.dp).wrapContentWidth(Alignment.Start, unbounded = true)
                .requiredWidth(106.dp).graphicsLayer { alpha = labelAlpha }
                .clearAndSetSemantics {})
    }
}
