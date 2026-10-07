package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.repository.KidsPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.kids.*

/** Connections describe services; child cards open parental controls without switching accounts. */
@Composable
internal fun ServicesSettings(state: ReelstackUiState, onConnection: (ServiceKind) -> Unit,
    onAccount: (ServiceKind) -> Unit, onSignOutAll: () -> Unit, onAddProfile: () -> Unit,
    onRequestPinSetup: (String) -> Unit = {}, onDisablePin: () -> Unit = {}) {
    var editingChild by rememberSaveable { mutableStateOf<String?>(null) }
    val child = state.profiles.firstOrNull { it.id == editingChild && it.isKid }
    if (child != null) {
        androidx.activity.compose.BackHandler { editingChild = null }
        SettingsActionRow(stringResource(R.string.services_back), "", "child-settings-back", SpoleIcons.ArrowBack) { editingChild = null }
        ChildProfileSettings(child, state.pinConfigured, onRequestPinSetup, onDisablePin)
        return
    }
    SettingsGroup(stringResource(R.string.services_media_title), stringResource(R.string.services_media_hint))
    listOf(ServiceKind.JELLYFIN, ServiceKind.EMBY).filter { state.canEditConnection(it) }.forEach { kind ->
        val connection = state.connections.firstOrNull { it.kind == kind } ?: return@forEach
        SettingsServiceRow(
            connection = connection,
            account = state.accounts[kind]?.displayName,
            avatarUrl = state.accounts[kind]?.avatarUrl,
            warning = state.serviceWarnings[kind],
            tag = "tv-service-$kind",
        ) { onConnection(kind) }
    }
    val children = state.profiles.filter { it.isKid }
    SettingsGroup(stringResource(R.string.services_children_title), stringResource(R.string.services_children_hint))
    if (children.isNotEmpty()) {
        children.forEach { profile ->
            val options = rememberKidsPreferences(profile.id)
            val services = state.allProfileConnections[profile.id].orEmpty()
                .filter { it.token.isNotBlank() }.joinToString { it.kind.displayName }
            SettingsActionRow(profile.name, "${options.world.localizedTitle()} · ${services.ifBlank { stringResource(R.string.services_needs_login) }}",
                "child-settings-${profile.id}", SpoleIcons.Kids) { editingChild = profile.id }
        }
    }
    SettingsActionRow(stringResource(R.string.services_add_child), stringResource(R.string.services_add_child_hint), "settings-add-child", SpoleIcons.Kids, onAddProfile)
    SettingsGroup(stringResource(R.string.services_discovery_title), stringResource(R.string.services_discovery_hint))
    state.connections.firstOrNull { it.kind == ServiceKind.SEERR }?.takeIf { state.canEditConnection(it.kind) }?.let {
        SettingsServiceRow(
            connection = it,
            account = state.verifiedPanelAccount(it.kind)?.displayName,
            avatarUrl = state.verifiedPanelAccount(it.kind)?.avatarUrl,
            warning = state.serviceWarnings[it.kind],
            tag = "tv-service-SEERR",
        ) { onConnection(it.kind) }
    }
    SettingsGroup(stringResource(R.string.services_device))
    SignOutAllSetting(state, onSignOutAll)
    PrivacyCard(state)
}

@Composable
private fun ChildProfileSettings(
    profile: UserProfile,
    pinConfigured: Boolean,
    onRequestPinSetup: (String) -> Unit,
    onDisablePin: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { KidsPreferencesRepository(context) }
    val options = rememberKidsPreferences(profile.id)
    val change: (KidsPreferences) -> Unit = { repository.save(profile.id, it) }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WorldLandscape(options.world, Modifier.size(88.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.child_options_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    SettingsGroup(stringResource(R.string.child_appearance_title), stringResource(R.string.child_appearance_hint))
    KidsWorldPicker(options, { change(options.copy(world = it)) })
    SettingsToggleRow(stringResource(R.string.child_allow_appearance), stringResource(R.string.child_allow_appearance_hint),
        options.allowAppearance, "child-allow-appearance") { change(options.copy(allowAppearance = it)) }
    SettingsToggleRow(stringResource(R.string.child_decorations), stringResource(R.string.child_decorations_hint), options.decorations,
        "child-decorations") { change(options.copy(decorations = it)) }
    SettingsToggleRow(stringResource(R.string.child_library_titles), stringResource(R.string.child_library_titles_hint),
        options.libraryTitlesBelow, "child-library-titles-below") { change(options.copy(libraryTitlesBelow = it)) }
    SettingsToggleRow(stringResource(R.string.child_reduce_motion), stringResource(R.string.child_reduce_motion_hint), options.reduceMotion,
        "child-reduce-motion") { change(options.copy(reduceMotion = it)) }
    SettingsGroup(stringResource(R.string.child_playback_title), stringResource(R.string.child_playback_hint, profile.name))
    SettingsToggleRow(stringResource(R.string.child_autoplay), stringResource(R.string.child_autoplay_hint),
        options.autoplay, "child-autoplay") { change(options.copy(autoplay = it)) }
    if (options.autoplay) {
        SettingsChoiceRow(stringResource(R.string.child_pause_after), androidx.compose.ui.res.pluralStringResource(R.plurals.child_pause_episodes, options.episodeLimit, options.episodeLimit), "child-episode-limit") {
            change(options.copy(episodeLimit = options.episodeLimit % 3 + 1))
        }
        Text(stringResource(R.string.child_pause_note),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SettingsToggleRow(
        stringResource(R.string.kids_bedtime_enabled),
        stringResource(R.string.kids_bedtime_enabled_hint),
        options.bedtime.enabled,
        "child-bedtime-enabled",
    ) { enabled -> change(options.copy(bedtime = options.bedtime.copy(enabled = enabled))) }
    if (options.bedtime.enabled) {
        SettingsChoiceRow(
            stringResource(R.string.kids_bedtime_time_title),
            stringResource(R.string.kids_bedtime_time, options.bedtime.hour, options.bedtime.minute),
            "child-bedtime-time",
        ) { change(options.copy(bedtime = options.bedtime.nextHalfHour())) }
        Text(
            stringResource(R.string.kids_bedtime_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    val languages = listOf(SubtitleLanguage.NORWEGIAN, SubtitleLanguage.ENGLISH, SubtitleLanguage.SERVER, SubtitleLanguage.NONE)
    val languageTitle = when (options.subtitles) {
        SubtitleLanguage.NORWEGIAN -> stringResource(R.string.child_subtitle_norwegian)
        SubtitleLanguage.ENGLISH -> stringResource(R.string.child_subtitle_english)
        SubtitleLanguage.NONE -> stringResource(R.string.child_subtitle_none)
        else -> stringResource(R.string.child_subtitle_server)
    }
    SettingsChoiceRow(stringResource(R.string.child_subtitles), languageTitle, "child-subtitles") {
        change(options.copy(subtitles = languages[(languages.indexOf(options.subtitles) + 1) % languages.size]))
    }
    SettingsGroup(stringResource(R.string.child_access_title))
    var confirmDisablePin by rememberSaveable { mutableStateOf(false) }
    SettingsToggleRow(
        stringResource(R.string.kids_pin_required_title),
        stringResource(R.string.kids_pin_required_hint),
        pinConfigured,
        "child-require-pin",
    ) { enabled ->
        if (enabled) onRequestPinSetup(profile.id) else confirmDisablePin = true
    }
    if (confirmDisablePin) {
        AlertDialog(
            onDismissRequest = { confirmDisablePin = false },
            title = { Text(stringResource(R.string.kids_pin_disable_title)) },
            text = { Text(stringResource(R.string.kids_pin_disable_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDisablePin = false; onDisablePin() }) {
                    Text(stringResource(R.string.kids_pin_disable_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisablePin = false }) {
                    Text(stringResource(R.string.account_cancel))
                }
            },
        )
    }
    Text(stringResource(R.string.child_access_note),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(stringResource(R.string.child_pin_note),
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
