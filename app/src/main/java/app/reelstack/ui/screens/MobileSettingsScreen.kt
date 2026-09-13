package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.*
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalPersonalization

private enum class MobileSettingsPage(val title: Int, val hint: Int) {
    APPEARANCE(R.string.personal_appearance, R.string.settings_mobile_appearance_hint),
    HOME(R.string.settings_home, R.string.settings_tv_home_hint),
    MENU(R.string.settings_tv_navigation, R.string.settings_tv_navigation_hint),
    PLAYBACK(R.string.personal_playback, R.string.settings_tv_playback_hint),
    ACCOUNTS(R.string.settings_services, R.string.settings_tv_accounts_hint),
    UPDATES(R.string.settings_updates, R.string.settings_tv_updates_hint),
    ABOUT(R.string.settings_about, R.string.settings_tv_about_hint),
}

/** The phone opens one group at a time; returning preserves that group's reading position. */
@Composable
internal fun MobileSettingsScreen(state: ReelstackUiState, contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit,
    onHomeRowOrderChange: (List<HomeRow>) -> Unit = {}, onSignOutAll: () -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf<MobileSettingsPage?>(null) }
    val panes = rememberSaveableStateHolder()
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val options = LocalPersonalization.current
    val change: (Personalization) -> Unit = { preferences.personalization = it }
    BackHandler(page != null && state.activeSheet == null && !state.libraryChoicesOpen) { page = null }
    CompositionLocalProvider(LocalSettingsButtonStyle provides true) {
        Column(Modifier.fillMaxSize().padding(contentPadding).testTag("mobile-settings")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (page != null) IconButton(onClick = { page = null }, Modifier.testTag("settings-back")) {
                    Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                }
                Text(stringResource(page?.title ?: R.string.nav_settings),
                    style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            }
            val selected = page
            key(selected) { panes.SaveableStateProvider(selected?.name ?: "INDEX") {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).testTag("settings-feed"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selected == null) {
                        MobileSettingsPage.entries.forEach { destination ->
                            SettingsActionRow(stringResource(destination.title), stringResource(destination.hint),
                                "settings-category-${destination.name}") { page = destination }
                        }
                    } else {
                        Text(stringResource(selected.hint), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        when (selected) {
                            MobileSettingsPage.APPEARANCE -> {
                                VisualThemeSettings(options, change)
                                SettingsToggleRow(stringResource(R.string.tv_show_ratings), "", options.showRatings, "show-ratings") { change(options.copy(showRatings = it)) }
                                SettingsToggleRow(stringResource(R.string.tv_show_quality), "", options.showQuality, "show-quality") { change(options.copy(showQuality = it)) }
                                LanguagePreference(compact = true)
                            }
                            MobileSettingsPage.HOME -> {
                                if (state.connections.any { it.kind == ServiceKind.JELLYFIN && it.token.isNotBlank() })
                                    SettingsActionRow(stringResource(R.string.library_manage), stringResource(R.string.settings_tv_library_hint), "library-manage", onManageLibraries)
                                SettingsToggleRow(stringResource(R.string.tv_next_up), "", options.showNextUp, "show-next-up") { change(options.copy(showNextUp = it)) }
                                SettingsToggleRow(stringResource(R.string.tv_combine_continue), stringResource(R.string.watching_order_hint),
                                    options.combineContinueWatching, "combine-continue") { change(options.copy(combineContinueWatching = it)) }
                                SettingsToggleRow(stringResource(R.string.tv_show_hero), stringResource(R.string.settings_tv_hero_hint),
                                    options.showHero, "show-hero") { change(options.copy(showHero = it)) }
                                TvHomeRows(state, onHomeSectionChange)
                                HomeRowOrderSetting(state, onHomeRowOrderChange)
                            }
                            MobileSettingsPage.MENU -> TvMenuSettings(options, change)
                            MobileSettingsPage.PLAYBACK -> {
                                SettingsToggleRow(stringResource(R.string.personal_resume), stringResource(R.string.personal_resume_note),
                                    options.autoResume, "auto-resume") { change(options.copy(autoResume = it)) }
                                NextEpisodeSettings(options, change)
                                SettingsToggleRow(stringResource(R.string.tv_slow_startup), stringResource(R.string.settings_tv_startup_hint),
                                    options.slowStartup, "slow-startup") { change(options.copy(slowStartup = it)) }
                            }
                            MobileSettingsPage.ACCOUNTS -> {
                                state.connections.filter { state.canEditConnection(it.kind) }.forEach { connection ->
                                    val connected = connection.token.isNotBlank()
                                    SettingsServiceRow(connection, state.verifiedPanelAccount(connection.kind)?.displayName,
                                        state.serviceWarnings[connection.kind], "mobile-service-${connection.kind}") { onConnectionClick(connection.kind) }
                                    if (connected && connection.kind == ServiceKind.SEERR)
                                        SettingsChoiceRow(stringResource(R.string.settings_tv_account_action), "Seerr", "mobile-account-SEERR") { onAccountClick(ServiceKind.SEERR) }
                                }
                                SignOutAllSetting(state, onSignOutAll)
                                PrivacyCard(state)
                            }
                            MobileSettingsPage.UPDATES -> {
                                app.reelstack.update.AppUpdateSettings()
                                SettingsToggleRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_note),
                                    state.notificationsEnabled, "settings-notifications", onNotificationsChange)
                                SettingsToggleRow(stringResource(R.string.settings_wifi), stringResource(R.string.settings_wifi_note),
                                    state.wifiOnly, "settings-wifi", onWifiOnlyChange)
                            }
                            MobileSettingsPage.ABOUT -> { AppIdentity(); AttributionCard(); CrashReportRow() }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            } }
        }
    }
}
