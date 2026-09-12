package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

private enum class TvSettingsCategory(val title: Int, val hint: Int, val icon: ImageVector) {
    APPEARANCE(R.string.personal_appearance, R.string.settings_tv_appearance_hint, app.reelstack.ui.components.SpoleIcons.Palette),
    HOME(R.string.settings_home, R.string.settings_tv_home_hint, app.reelstack.ui.components.SpoleIcons.Screen),
    MENU(R.string.settings_tv_navigation, R.string.settings_tv_navigation_hint, app.reelstack.ui.components.SpoleIcons.Tune),
    PLAYBACK(R.string.personal_playback, R.string.settings_tv_playback_hint, app.reelstack.ui.components.SpoleIcons.Play),
    ACCOUNTS(R.string.settings_services, R.string.settings_tv_accounts_hint, app.reelstack.ui.components.SpoleIcons.Server),
    UPDATES(R.string.settings_updates, R.string.settings_tv_updates_hint, app.reelstack.ui.components.SpoleIcons.Bell),
    ABOUT(R.string.settings_about, R.string.settings_tv_about_hint, app.reelstack.ui.components.SpoleIcons.Info),
}

/** Television is an input mode, not a width breakpoint: 1080p TVs are often only 960 dp wide. */
@Composable
internal fun TvSettingsScreen(state: ReelstackUiState, contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit) {
    var category by rememberSaveable { mutableStateOf(TvSettingsCategory.APPEARANCE) }
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val options = LocalPersonalization.current
    val change: (Personalization) -> Unit = { preferences.personalization = it }
    val categoryFocus = remember { TvSettingsCategory.entries.associateWith { FocusRequester() } }
    val panelFocus = remember { FocusRequester() }
    var enterPanel by remember { mutableStateOf(false) }
    var inPanel by remember { mutableStateOf(false) }
    val panes = rememberSaveableStateHolder()
    LaunchedEffect(enterPanel, category) {
        if (enterPanel) {
            withFrameNanos { }
            panelFocus.requestFocus()
            enterPanel = false
        }
    }
    BackHandler(inPanel && state.activeSheet == null && !state.libraryChoicesOpen) {
        categoryFocus.getValue(category).requestFocus()
    }
    Row(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 24.dp, vertical = 24.dp)
        .background(MaterialTheme.colorScheme.background).testTag("tv-settings"),
        horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.width(192.dp).fillMaxHeight().verticalScroll(rememberScrollState())
            .testTag("settings-categories"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            TvSettingsCategory.entries.forEach { item ->
                WideDestination(stringResource(item.title), item.icon, category == item,
                    onClick = {
                        // A click/tap need not move keyboard focus. Anchor it before replacing
                        // a focused pane, otherwise Compose can focus a different category.
                        categoryFocus.getValue(item).requestFocus()
                        category = item; enterPanel = true
                    },
                    modifier = Modifier.focusRequester(categoryFocus.getValue(item))
                        .onFocusChanged { if(it.isFocused) category = item }
                        .onPreviewKeyEvent {
                            if (it.key == Key.DirectionRight && it.type == KeyEventType.KeyDown) {
                                category = item; enterPanel = true; true
                            } else false
                        }.testTag("settings-category-${item.name}"))
            }
            Text(stringResource(R.string.settings_tv_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Text(stringResource(category.title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(category.hint), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            key(category) { panes.SaveableStateProvider(category.name) {
                Column(Modifier.weight(1f).fillMaxWidth().focusRequester(panelFocus)
                    .onPreviewKeyEvent {
                        if (it.key == Key.DirectionLeft && it.type == KeyEventType.KeyDown) {
                            categoryFocus.getValue(category).requestFocus(); true
                        } else false
                    }
                    .onFocusChanged { inPanel = it.hasFocus }.focusGroup()
                    .verticalScroll(rememberScrollState()).testTag("settings-feed"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when(category) {
                        TvSettingsCategory.APPEARANCE -> {
                            VisualThemeSettings(options, change)
                            SettingsToggleRow(stringResource(R.string.tv_show_ratings), "", options.showRatings, "show-ratings") { change(options.copy(showRatings = it)) }
                            SettingsToggleRow(stringResource(R.string.tv_show_quality), "", options.showQuality, "show-quality") { change(options.copy(showQuality = it)) }
                            LanguagePreference()
                        }
                        TvSettingsCategory.HOME -> {
                            if (state.connections.any { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() })
                                SettingsActionRow(stringResource(R.string.library_manage), stringResource(R.string.settings_tv_library_hint), "library-manage", onManageLibraries)
                            SettingsToggleRow(stringResource(R.string.tv_next_up), "", options.showNextUp, "show-next-up") { change(options.copy(showNextUp = it)) }
                            SettingsToggleRow(stringResource(R.string.tv_combine_continue), stringResource(R.string.watching_order_hint),
                                options.combineContinueWatching, "combine-continue") { change(options.copy(combineContinueWatching = it)) }
                            SettingsToggleRow(stringResource(R.string.tv_show_hero), stringResource(R.string.settings_tv_hero_hint),
                                options.showHero, "show-hero") { change(options.copy(showHero = it)) }
                            TvHomeRows(state, onHomeSectionChange)
                        }
                        TvSettingsCategory.MENU -> {
                            TvMenuSettings(options, change)
                            if (state.connections.any { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() })
                                SettingsActionRow(stringResource(R.string.library_manage), stringResource(R.string.settings_tv_library_hint), "library-manage", onManageLibraries)
                        }
                        TvSettingsCategory.PLAYBACK -> {
                            SettingsToggleRow(stringResource(R.string.personal_resume), stringResource(R.string.personal_resume_note),
                                options.autoResume, "auto-resume") { change(options.copy(autoResume = it)) }
                            NextEpisodeSettings(options, change)
                            SettingsToggleRow(stringResource(R.string.tv_slow_startup), stringResource(R.string.settings_tv_startup_hint),
                                options.slowStartup, "slow-startup") { change(options.copy(slowStartup = it)) }
                        }
                        TvSettingsCategory.ACCOUNTS -> {
                            state.connections.filter { state.canEditConnection(it.kind) }.forEach { connection ->
                                val connected = connection.token.isNotBlank()
                                val status = stringResource(when(connection.state) {
                                    ConnectionState.ERROR -> R.string.service_error
                                    ConnectionState.TESTING -> R.string.service_checking
                                    else -> if (connected) R.string.service_connected else R.string.settings_tv_connection_action
                                })
                                val summary = listOfNotNull(state.verifiedPanelAccount(connection.kind)?.displayName, status).joinToString(" · ")
                                // Who is signed in and whether it works is the row's value, so it
                                // reads across the row rather than hiding under the service name.
                                SettingsChoiceRow(connection.kind.displayName, summary,
                                    "tv-service-${connection.kind}") { onConnectionClick(connection.kind) }
                                if (connected && connection.kind == ServiceKind.SEERR)
                                    SettingsChoiceRow(stringResource(R.string.settings_tv_account_action), "Seerr", "tv-account-SEERR") { onAccountClick(ServiceKind.SEERR) }
                            }
                            PrivacyCard(state)
                        }
                        TvSettingsCategory.UPDATES -> {
                            app.reelstack.update.AppUpdateSettings()
                            SettingsToggleRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_note),
                                state.notificationsEnabled, "settings-notifications", onNotificationsChange)
                            SettingsToggleRow(stringResource(R.string.settings_wifi), stringResource(R.string.settings_wifi_note),
                                state.wifiOnly, "settings-wifi", onWifiOnlyChange)
                        }
                        TvSettingsCategory.ABOUT -> { AppIdentity(); AttributionCard(); CrashReportRow() }
                    }
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
