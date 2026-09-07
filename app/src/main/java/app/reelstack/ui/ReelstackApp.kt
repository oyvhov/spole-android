package app.reelstack.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun ReelstackApp(viewModel: ReelstackViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionDraft by viewModel.connectionDraft.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabStates = rememberSaveableStateHolder()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    // Following a request costs a Seerr profile call, a request listing and up to twenty detail
    // lookups. Polling that every 30 seconds from every tab kept a self-hosted server busy for a
    // list nobody had on screen, so the fast cadence now belongs to the tab that shows it. The tab
    // is read inside the loop so switching tabs adapts the delay without restarting the poll.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshTrackedRequests()
                kotlinx.coroutines.delay(
                    if (viewModel.uiState.value.selectedTab == AppTab.ACTIVITY) 30_000L else 300_000L,
                )
            }
        }
    }
    BackHandler(enabled = state.activeSheet == null && !state.showOnboarding && state.selectedTab != AppTab.HOME) {
        viewModel.selectTab(AppTab.HOME)
    }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }

    // A wide window moves navigation to a side rail so the bottom bar does not stretch four
    // icons across a tablet, and so the content column keeps its own width. Measured from the
    // window, not the device configuration, so split-screen is handled correctly.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Ink)) {
    val wideWindow = maxWidth >= ReelLayout.RailBreakpoint
    val showRail = !state.showOnboarding && wideWindow
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!state.showOnboarding && !wideWindow) ReelstackBottomBar(
                    selectedTab = state.selectedTab,
                    onSelect = viewModel::selectTab,
                )
            },
        ) { paddingValues ->
            Row(Modifier.fillMaxSize().padding(paddingValues)) {
            if (showRail) {
                ReelstackNavigationRail(
                    selectedTab = state.selectedTab,
                    onSelect = viewModel::selectTab,
                )
            }
            Box(Modifier.weight(1f).fillMaxSize()) {
            if (state.showOnboarding) {
                WelcomeScreen(
                    state = state,
                    modifier = Modifier,
                    onConnect = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                    onContinue = viewModel::completeOnboarding,
                )
            } else {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "primary-navigation",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
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
                    )
                    AppTab.ACTIVITY -> ActivityScreen(state, PaddingValues(0.dp), viewModel::openActivityDetails,
                        viewModel::setFollowNotification, viewModel::refreshTrackedRequests, viewModel::openSeerrAccount,
                        viewModel::cancelTrackedRequest)
                    AppTab.SETTINGS -> SettingsScreen(
                        state = state,
                        contentPadding = PaddingValues(0.dp),
                        onConnectionClick = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                        onNotificationsChange = viewModel::setNotifications,
                        onWifiOnlyChange = viewModel::setWifiOnly,
                        onHomeSectionChange = viewModel::setHomeSectionVisible,
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
        onAddMedia = viewModel::requestMedia,
        onUpcomingClick = viewModel::openUpcomingDetails,
        onBackToCalendar = viewModel::backToCalendar,
        onSeerrAccount = viewModel::openSeerrAccount,
        onRequestSeason = viewModel::setRequestSeason,
        onRequestNotification = viewModel::setRequestNotification,
        onConfirmRequest = viewModel::confirmRequest,
    )
}

private data class TabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(AppTab.HOME, "Heim", Icons.Rounded.Home),
    TabItem(AppTab.DISCOVER, "Oppdag", Icons.Rounded.Explore),
    TabItem(AppTab.ACTIVITY, "Aktivitet", Icons.AutoMirrored.Rounded.ViewList),
    TabItem(AppTab.SETTINGS, "Innstillingar", Icons.Rounded.Settings),
)

@Composable
private fun ReelstackBottomBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    // No fixed height: at a large font scale a locked bar clips the icons and breaks the label
    // mid-word. The minimum keeps the bar at its usual size when the text is small.
    NavigationBar(
        containerColor = Ink,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.navigationBarsPadding().heightIn(min = 76.dp),
    ) {
            tabs.forEach { item ->
                NavigationBarItem(
                    selected = item.tab == selectedTab,
                    onClick = { onSelect(item.tab) },
                    icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(23.dp)) },
                    label = {
                        Text(
                            item.label,
                            fontSize = 10.sp,
                            maxLines = 1,
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
private fun ReelstackNavigationRail(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    NavigationRail(
        containerColor = Ink,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxHeight(),
    ) {
        Spacer(Modifier.weight(1f))
        tabs.forEach { item ->
            NavigationRailItem(
                selected = item.tab == selectedTab,
                onClick = { onSelect(item.tab) },
                icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(23.dp)) },
                label = { Text(item.label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = SurfaceRaised,
                    unselectedIconColor = app.reelstack.ui.theme.Muted,
                    unselectedTextColor = app.reelstack.ui.theme.Muted,
                ),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
