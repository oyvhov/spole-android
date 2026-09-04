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

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearSnackbar()
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink)) {
        Image(
            painter = painterResource(R.drawable.cinematic_sky),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.72f,
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x5A080610),
                    0.45f to Color(0xA80A0812),
                    1f to Ink,
                ),
            ),
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ReelstackBottomBar(
                    selectedTab = state.selectedTab,
                    onSelect = viewModel::selectTab,
                )
            },
        ) { paddingValues ->
            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "primary-navigation",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        state = state,
                        contentPadding = paddingValues,
                        onServerClick = { viewModel.openSheet(AppSheet.ServerPicker) },
                        onSessionClick = { viewModel.openSheet(AppSheet.SessionDetails) },
                        onPlaybackToggle = viewModel::togglePlayback,
                        onMediaClick = { viewModel.openSheet(AppSheet.MediaDetails(it)) },
                        onLibraryClick = { viewModel.openSheet(AppSheet.LibraryDetails(it)) },
                        onRefresh = { viewModel.refreshLiveData(userInitiated = true) },
                    )
                    AppTab.DISCOVER -> DiscoverScreen(
                        state = state,
                        contentPadding = paddingValues,
                        onSearch = viewModel::setSearchQuery,
                        onRequest = viewModel::requestMedia,
                    )
                    AppTab.ACTIVITY -> ActivityScreen(state, paddingValues)
                    AppTab.SETTINGS -> SettingsScreen(
                        state = state,
                        contentPadding = paddingValues,
                        onConnectionClick = { viewModel.openSheet(AppSheet.ConnectionEditor(it)) },
                        onNotificationsChange = viewModel::setNotifications,
                        onWifiOnlyChange = viewModel::setWifiOnly,
                    )
                }
            }
        }
    }

    ReelstackSheets(
        state = state,
        connectionDraft = connectionDraft,
        onDismiss = viewModel::closeSheet,
        onSelectServer = viewModel::selectServer,
        onPlaybackToggle = viewModel::togglePlayback,
        onConnectionNameChange = viewModel::updateConnectionName,
        onConnectionUrlChange = viewModel::updateConnectionUrl,
        onConnectionTokenChange = viewModel::updateConnectionToken,
        onConnectionUserIdChange = viewModel::updateConnectionUserId,
        onTestAndSaveConnection = viewModel::testAndSaveConnection,
        onRemoveConnection = viewModel::removeConnection,
    )
}

private data class TabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(AppTab.HOME, "Home", Icons.Rounded.Home),
    TabItem(AppTab.DISCOVER, "Discover", Icons.Rounded.Explore),
    TabItem(AppTab.ACTIVITY, "Activity", Icons.AutoMirrored.Rounded.ViewList),
    TabItem(AppTab.SETTINGS, "Settings", Icons.Rounded.Settings),
)

@Composable
private fun ReelstackBottomBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    Surface(
        color = SurfaceRaised.copy(alpha = 0.97f),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 18.dp,
        tonalElevation = 4.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp)) {
            tabs.forEach { item ->
                BottomBarItem(
                    item = item,
                    selected = item.tab == selectedTab,
                    onClick = { onSelect(item.tab) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    item: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (selected) PrimarySoft else Color(0xFFC6BDCF),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 3.dp),
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 58.dp, height = 36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Primary.copy(alpha = 0.28f) else Color.Transparent),
            ) {
                Icon(item.icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Text(
                text = item.label,
                fontSize = 11.sp,
                color = LocalContentColor.current,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
