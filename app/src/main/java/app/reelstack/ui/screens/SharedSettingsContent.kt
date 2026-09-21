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
    HOME(R.string.settings_home_navigation, R.string.settings_tv_home_hint, app.reelstack.ui.components.SpoleIcons.Screen),
    MENU(R.string.settings_tv_navigation, R.string.settings_tv_navigation_hint, app.reelstack.ui.components.SpoleIcons.Tune),
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
    onClearLibraryCache: () -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { AppPreferencesRepository(context) }
    val options = LocalPersonalization.current
    val change: (Personalization) -> Unit = { preferences.personalization = it }
    when(category) {
        SettingsCategory.APPEARANCE -> {
            VisualThemeSettings(options, change, (state.recentMovies + state.recentSeries).take(3))
        }
        SettingsCategory.HOME -> {
            HomeExperienceSettings(options, change)
            if (!isTelevision()) TvMenuSettings(options, change)
            if (state.libraryConnection != null)
                SettingsActionRow(stringResource(R.string.library_manage), stringResource(R.string.settings_tv_library_hint), "library-manage", onManageLibraries)
            SettingsGroup(stringResource(R.string.settings_home_display_group))
            LibraryCustomizationSetting(state, options, change)
            LibraryAppearanceSettings(options, change)
            SettingsToggleRow(stringResource(R.string.tv_show_ratings), stringResource(R.string.refine_ratings_hint), options.showRatings, "show-ratings") { change(options.copy(showRatings = it)) }
            SettingsToggleRow(stringResource(R.string.tv_show_quality), stringResource(R.string.refine_quality_hint), options.showQuality, "show-quality") { change(options.copy(showQuality = it)) }
            HeroSettings(options, change)
            SettingsGroup(stringResource(R.string.refine_group_rows))
            SettingsToggleRow(stringResource(R.string.tv_next_up), stringResource(R.string.refine_next_hint), options.showNextUp, "show-next-up") { change(options.copy(showNextUp = it)) }
            SettingsToggleRow(stringResource(R.string.tv_combine_continue), stringResource(R.string.watching_order_hint),
                options.combineContinueWatching, "combine-continue") { change(options.copy(combineContinueWatching = it)) }
            HomeRowOrderSetting(state, onHomeRowOrderChange, onHomeSectionChange)
            HomeRowFormats(options, change)
        }
        SettingsCategory.MENU -> {
            TvMenuSettings(options, change)
            if (state.libraryConnection != null)
                SettingsActionRow(stringResource(R.string.library_manage), stringResource(R.string.settings_tv_library_hint), "library-manage", onManageLibraries)
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
        SettingsCategory.ACCOUNTS -> ServicesSettings(state, onConnectionClick, onAccountClick, onSignOutAll, onAddProfile)
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
