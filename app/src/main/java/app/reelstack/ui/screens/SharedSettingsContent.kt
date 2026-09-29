package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.imageLoader
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.ui.*
import app.reelstack.ui.components.*
import app.reelstack.ui.theme.LocalPersonalization

internal enum class SettingsCategory(val title: Int, val hint: Int, val icon: ImageVector) {
    APPEARANCE(R.string.personal_appearance, R.string.settings_tv_appearance_hint, app.reelstack.ui.components.SpoleIcons.Palette),
    /** Home, Library and the menu. They were two categories on a television, and the library was in both. */
    HOME(R.string.settings_home_navigation, R.string.settings_tv_home_hint, app.reelstack.ui.components.SpoleIcons.Screen),
    PLAYBACK(R.string.personal_playback, R.string.settings_tv_playback_hint, app.reelstack.ui.components.SpoleIcons.Play),
    ACCOUNTS(R.string.settings_services, R.string.settings_tv_accounts_hint, app.reelstack.ui.components.SpoleIcons.Server),
    UPDATES(R.string.settings_updates, R.string.settings_tv_updates_hint, app.reelstack.ui.components.SpoleIcons.Bell),
    ABOUT(R.string.settings_about, R.string.settings_tv_about_hint, app.reelstack.ui.components.SpoleIcons.Info),
}


@Composable
internal fun SharedSettingsContent(category: SettingsCategory, state: ReelstackUiState,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit,
    onHomeRowOrderChange: (List<HomeRow>) -> Unit, onSignOutAll: () -> Unit, onAddProfile: () -> Unit = {},
    onClearLibraryCache: () -> Unit = {}, onRequestPinSetup: (String) -> Unit = {},
    onDisablePin: () -> Unit = {}, homeEditor: HomeEditorActions = HomeEditorActions()) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val options = LocalPersonalization.current
    val change: (Personalization) -> Unit = { preferences.personalization = it }
    when(category) {
        SettingsCategory.APPEARANCE -> {
            VisualThemeSettings(options, change, (state.recentMovies + state.recentSeries).take(3))
        }
        SettingsCategory.HOME -> {
            // One editor per page, built the same way: every row with its switch, its place and its
            // own choices. Home's rows used to be four settings; the library page's were three places,
            // two of which could hide a library, and a switch that replaced the whole page.
            SettingsGroup(stringResource(R.string.settings_pages_group))
            HomeLayoutSetting(state, homeEditor)
            LibraryCustomizationSetting(onManageLibraries)
            TvMenuSettings(options, change)
        }
        SettingsCategory.PLAYBACK -> {
            SettingsGroup(stringResource(R.string.settings_playback_start_group))
            SettingsToggleRow(stringResource(R.string.personal_resume), stringResource(R.string.personal_resume_note),
                options.autoResume, "auto-resume") { change(options.copy(autoResume = it)) }
            SettingsGroup(stringResource(R.string.settings_playback_media_group))
            SubtitleLanguageSettings(options, change)
            SubtitleAppearanceSetting(options.subtitleStyle) { change(options.copy(subtitleStyle = it)) }
            SettingsGroup(stringResource(R.string.settings_language_group))
            LanguagePreference()
            SettingsGroup(stringResource(R.string.settings_playback_next_group))
            NextEpisodeSettings(options, change)
            SettingsGroup(stringResource(R.string.settings_playback_quality_group))
            SettingsToggleRow(stringResource(R.string.tv_slow_startup), stringResource(R.string.settings_tv_startup_hint),
                options.slowStartup, "slow-startup") { change(options.copy(slowStartup = it)) }
        }
        SettingsCategory.ACCOUNTS -> ServicesSettings(state, onConnectionClick, onAccountClick, onSignOutAll, onAddProfile,
            onRequestPinSetup, onDisablePin)
        SettingsCategory.UPDATES -> {
            app.reelstack.update.AppUpdateSettings(grouped = !isTelevision())
            val isTelevision = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
            if (!isTelevision) {
                SettingsToggleRow(stringResource(R.string.settings_notifications), stringResource(R.string.settings_notifications_note),
                    state.notificationsEnabled, "settings-notifications", onNotificationsChange)
                SettingsToggleRow(stringResource(R.string.settings_wifi), stringResource(R.string.settings_wifi_note),
                    state.wifiOnly, "settings-wifi", onWifiOnlyChange)
            }
        }
        SettingsCategory.ABOUT -> {
            AppIdentitySettings(options, change)
            AppIdentity()
            LibraryCacheRow(onClearLibraryCache)
            ImageCacheRow()
            CrashReportRow()
            AttributionCard()
        }
    }
}

@Composable
private fun ImageCacheRow() {
    val context = LocalContext.current.applicationContext
    var cleared by remember { mutableStateOf(false) }
    SettingsActionRow(
        stringResource(R.string.image_cache_clear),
        stringResource(if (cleared) R.string.image_cache_cleared else R.string.image_cache_clear_hint),
        "clear-image-cache",
    ) {
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
        cleared = true
    }
}

@Composable
private fun LibraryCacheRow(onClear: () -> Unit) {
    var cleared by remember { mutableStateOf(false) }
    SettingsActionRow(
        stringResource(R.string.library_cache_clear),
        stringResource(if (cleared) R.string.library_cache_cleared else R.string.library_cache_clear_hint),
        "clear-library-cache",
    ) {
        onClear()
        cleared = true
    }
}
