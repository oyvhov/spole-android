package app.reelstack.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import app.reelstack.ui.theme.SurfaceRaised

@Composable
fun ReelstackApp(viewModel: ReelstackViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionDraft by viewModel.connectionDraft.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val tabStates = rememberSaveableStateHolder()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshTrackedRequests()
                kotlinx.coroutines.delay(30_000)
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

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!state.showOnboarding) ReelstackBottomBar(
                    selectedTab = state.selectedTab,
                    onSelect = viewModel::selectTab,
                )
            },
        ) { paddingValues ->
            if (state.showOnboarding) {
                WelcomeScreen(
                    state = state,
                    modifier = Modifier.padding(paddingValues),
                    onConnect = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                    onContinue = viewModel::completeOnboarding,
                )
            } else {
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "primary-navigation",
                modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                    )
                    AppTab.DISCOVER -> DiscoverScreen(
                        state = state,
                        contentPadding = PaddingValues(0.dp),
                        onSearch = viewModel::setSearchQuery,
                        onRequest = viewModel::requestMedia,
                        onDetails = viewModel::openDiscoverDetails,
                        onAccountClick = viewModel::openSeerrAccount,
                    )
                    AppTab.ACTIVITY -> ActivityScreen(state, PaddingValues(0.dp), viewModel::openActivityDetails,
                        viewModel::setFollowNotification, viewModel::refreshTrackedRequests)
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

    ReelstackSheets(
        state = state,
        connectionDraft = connectionDraft,
        onDismiss = viewModel::closeSheet,
        onPlaybackToggle = viewModel::togglePlayback,
        onConnectionNameChange = viewModel::updateConnectionName,
        onConnectionUrlChange = viewModel::updateConnectionUrl,
        onConnectionTokenChange = viewModel::updateConnectionToken,
        onConnectionUserIdChange = viewModel::updateConnectionUserId,
        onConnectionAuthModeChange = viewModel::updateConnectionAuthMode,
        onConnectionUsernameChange = viewModel::updateConnectionUsername,
        onConnectionPasswordChange = viewModel::updateConnectionPassword,
        onTestAndSaveConnection = viewModel::testAndSaveConnection,
        onRemoveConnection = viewModel::removeConnection,
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
    NavigationBar(
        containerColor = Ink,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.navigationBarsPadding().height(76.dp),
    ) {
            tabs.forEach { item ->
                NavigationBarItem(
                    selected = item.tab == selectedTab,
                    onClick = { onSelect(item.tab) },
                    icon = { Icon(item.icon, contentDescription = null, modifier = Modifier.size(23.dp)) },
                    label = { Text(item.label, fontSize = 10.sp) },
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
