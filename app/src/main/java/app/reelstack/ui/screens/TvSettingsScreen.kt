package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.togetherWith
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalPersonalization

/** Television is an input mode, not a width breakpoint: 1080p TVs are often only 960 dp wide. */
@Composable
internal fun TvSettingsScreen(state: ReelstackUiState, contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit,
    onHomeRowOrderChange: (List<HomeRow>) -> Unit = {}, onSignOutAll: () -> Unit = {}, onAddProfile: () -> Unit = {},
    onClearLibraryCache: () -> Unit = {}) {
    val television = isTelevision()
    var category by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }
    var focusedCategory by rememberSaveable { mutableStateOf(category) }
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val options = LocalPersonalization.current
    val change: (Personalization) -> Unit = { preferences.personalization = it }
    val categoryFocus = remember { SettingsCategory.entries.associateWith { FocusRequester() } }
    val panelFocus = remember { FocusRequester() }
    var enterPanel by remember { mutableStateOf(false) }
    var inPanel by remember { mutableStateOf(false) }
    val panes = rememberSaveableStateHolder()

    LaunchedEffect(state.accountsSettingsRequest) {
        if (state.accountsSettingsRequest > 0) {
            focusedCategory = SettingsCategory.ACCOUNTS
            category = SettingsCategory.ACCOUNTS
            if (television) categoryFocus.getValue(SettingsCategory.ACCOUNTS).requestFocus()
        }
    }

    LaunchedEffect(focusedCategory, television) {
        if (television) {
            kotlinx.coroutines.delay(130)
        }
        category = focusedCategory
    }

    LaunchedEffect(enterPanel, category) {
        if (enterPanel) {
            withFrameNanos { }
            panelFocus.requestFocus()
            enterPanel = false
        }
    }
    BackHandler(television && inPanel && state.activeSheet == null && !state.libraryChoicesOpen) {
        categoryFocus.getValue(category).requestFocus()
    }
    Row(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 24.dp, vertical = 24.dp)
        .testTag("tv-settings"),
        horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.width(192.dp).fillMaxHeight().verticalScroll(rememberScrollState())
            .testTag("settings-categories"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            SettingsCategory.entries.forEach { item ->
                WideDestination(stringResource(item.title), item.icon, focusedCategory == item,
                    onClick = {
                        // A click/tap need not move keyboard focus. Anchor it before replacing
                        // a focused pane, otherwise Compose can focus a different category.
                        categoryFocus.getValue(item).requestFocus()
                        focusedCategory = item; category = item; enterPanel = television
                    },
                    modifier = Modifier.focusRequester(categoryFocus.getValue(item))
                        .onFocusChanged { if (television && it.isFocused) focusedCategory = item }
                        .onPreviewKeyEvent {
                            if (it.key == Key.DirectionRight && it.type == KeyEventType.KeyDown) {
                                focusedCategory = item; category = item; enterPanel = true; true
                            } else false
                        }.testTag("settings-category-${item.name}"))
            }
            if (television) Text(stringResource(R.string.settings_tv_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(stringResource(category.title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(category.hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            androidx.compose.animation.AnimatedContent(
                targetState = category,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90))
                },
                label = "settings-pane-crossfade",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { activeCategory ->
                panes.SaveableStateProvider(activeCategory.name) {
                    Column(Modifier.fillMaxSize().focusRequester(panelFocus)
                        .focusProperties {
                            onExit = {
                                if (television && requestedFocusDirection == FocusDirection.Left)
                                    categoryFocus.getValue(category).requestFocus()
                            }
                        }
                        .onFocusChanged { inPanel = it.hasFocus }.focusGroup()
                        .verticalScroll(rememberScrollState()).testTag("settings-feed"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SharedSettingsContent(activeCategory, state, onConnectionClick, onNotificationsChange, onWifiOnlyChange,
                            onHomeSectionChange, onAccountClick, onManageLibraries, onHomeRowOrderChange, onSignOutAll,
                            onAddProfile, onClearLibraryCache)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun TvHomeRows(state: ReelstackUiState, onChange: (HomeSection, Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.settings_tv_rows), stringResource(R.string.settings_tv_rows_hint), "tv-home-rows") { open = true }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(stringResource(R.string.settings_tv_rows)) },
        confirmButton = { app.reelstack.ui.components.SpoleSecondaryButton(onClick = { open = false }) { Text(stringResource(R.string.action_close)) } },
        text = { Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HomeSection.entries.filter { it != HomeSection.DOWNLOADS }.filter { section ->
                val service = when(section) { HomeSection.JELLYFIN_MOVIES, HomeSection.JELLYFIN_SERIES -> ServiceKind.JELLYFIN
                    HomeSection.EMBY_MOVIES, HomeSection.EMBY_SERIES -> ServiceKind.EMBY; else -> null }
                service == null || state.configuredCount == 0 || state.connections.any { it.kind == service && it.baseUrl.isNotBlank() }
            }.forEach { section ->
                val title = when(section) {
                    HomeSection.NOW_PLAYING -> stringResource(R.string.home_now_playing)
                    HomeSection.CONTINUE_WATCHING -> stringResource(R.string.home_continue)
                    HomeSection.FAVOURITES -> stringResource(R.string.home_favourites)
                    HomeSection.RECOMMENDATIONS -> stringResource(R.string.home_recommendations)
                    HomeSection.RECENT_RELEASES -> stringResource(R.string.home_recent_releases)
                    HomeSection.JELLYFIN_MOVIES -> stringResource(R.string.settings_movies, "Jellyfin")
                    HomeSection.JELLYFIN_SERIES -> stringResource(R.string.settings_series, "Jellyfin")
                    HomeSection.EMBY_MOVIES -> stringResource(R.string.settings_movies, "Emby")
                    HomeSection.EMBY_SERIES -> stringResource(R.string.settings_series, "Emby")
                    else -> stringResource(R.string.home_upcoming)
                }
                SettingsToggleRow(title, "", section in state.homeSections, "home-row-$section") { onChange(section, it) }
            }
        } })
}
