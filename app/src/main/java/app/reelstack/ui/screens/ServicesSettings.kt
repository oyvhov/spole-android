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
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.repository.KidsPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.*
import app.reelstack.ui.kids.*

/** Connections describe services; child cards open parental controls without switching accounts. */
@Composable
internal fun ServicesSettings(state: ReelstackUiState, onConnection: (ServiceKind) -> Unit,
    onAccount: (ServiceKind) -> Unit, onSignOutAll: () -> Unit, onAddProfile: () -> Unit) {
    var editingChild by rememberSaveable { mutableStateOf<String?>(null) }
    val child = state.profiles.firstOrNull { it.id == editingChild && it.isKid }
    if (child != null) {
        androidx.activity.compose.BackHandler { editingChild = null }
        SettingsActionRow("Tilbake til tenestene dine", "", "child-settings-back", SpoleIcons.ArrowBack) { editingChild = null }
        ChildProfileSettings(child)
        return
    }
    SettingsGroup("Bibliotek og avspeling", "Kontoen din hos kvar teneste")
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
    SettingsGroup("Barneprofilar", "Trykk på eit barn for å velje utsjånad og avspeling.")
    if (children.isNotEmpty()) {
        children.forEach { profile ->
            val options = rememberKidsPreferences(profile.id)
            val services = state.allProfileConnections[profile.id].orEmpty()
                .filter { it.token.isNotBlank() }.joinToString { it.kind.displayName }
            SettingsActionRow(profile.name, "${options.world.title} · ${services.ifBlank { "Treng innlogging" }}",
                "child-settings-${profile.id}", SpoleIcons.Kids) { editingChild = profile.id }
        }
    }
    SettingsActionRow("Legg til barneprofil", "Kople til barnet sin eigen Jellyfin- eller Emby-konto", "settings-add-child", SpoleIcons.Kids, onAddProfile)
    SettingsGroup("Oppdaging og ønskeliste", "Finn og førespør nye filmar og seriar")
    state.connections.firstOrNull { it.kind == ServiceKind.SEERR }?.takeIf { state.canEditConnection(it.kind) }?.let {
        SettingsServiceRow(
            connection = it,
            account = state.verifiedPanelAccount(it.kind)?.displayName,
            avatarUrl = state.verifiedPanelAccount(it.kind)?.avatarUrl,
            warning = state.serviceWarnings[it.kind],
            tag = "tv-service-SEERR",
        ) { onConnection(it.kind) }
    }
    val advanced = state.connections.filter { it.kind in setOf(ServiceKind.RADARR, ServiceKind.SONARR) && state.canEditConnection(it.kind) }
    if (advanced.isNotEmpty()) {
        SettingsGroup("Bibliotektenester", "Tilkoplingar for komande innhald og nedlastingsstatus")
        advanced.forEach { connection ->
            SettingsServiceRow(connection, warning = state.serviceWarnings[connection.kind], tag = "tv-service-${connection.kind}") {
                onConnection(connection.kind)
            }
        }
    }
    SettingsGroup("Denne eininga")
    SignOutAllSetting(state, onSignOutAll)
    PrivacyCard(state)
}

@Composable
private fun ChildProfileSettings(profile: UserProfile) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { KidsPreferencesRepository(context) }
    val options = rememberKidsPreferences(profile.id)
    val change: (KidsPreferences) -> Unit = { repository.save(profile.id, it) }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        WorldLandscape(options.world, Modifier.size(88.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.headlineMedium)
            Text("Barneprofil · eigne val på denne eininga", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    SettingsGroup("Utsjånad", "Vel ei verd som passar barnet.")
    KidsWorldPicker(options, { change(options.copy(world = it)) })
    SettingsToggleRow("Barnet kan endre utsjånad", "Vis «Mi verd» i barneprofilen. Avspeling og kontoval er berre for vaksne.",
        options.allowAppearance, "child-allow-appearance") { change(options.copy(allowAppearance = it)) }
    SettingsToggleRow("Vis landskap og pynt", "Eit roleg bakteppe rundt historiene", options.decorations,
        "child-decorations") { change(options.copy(decorations = it)) }
    SettingsToggleRow("Rolege overgangar", "Slå av rørsle og fokusanimasjonar", options.reduceMotion,
        "child-reduce-motion") { change(options.copy(reduceMotion = it)) }
    SettingsGroup("Avspeling", "Desse vala gjeld berre ${profile.name}.")
    SettingsToggleRow("Spel neste episode automatisk", "Av som standard. Barnet kan alltid starte neste episode sjølv.",
        options.autoplay, "child-autoplay") { change(options.copy(autoplay = it)) }
    if (options.autoplay) {
        SettingsChoiceRow("Pause etter", "${options.episodeLimit} episodar", "child-episode-limit") {
            change(options.copy(episodeLimit = options.episodeLimit % 3 + 1))
        }
        Text("Etter grensa ventar neste episode på eit trykk. Dette er ikkje ei dagleg tidsgrense.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val languages = listOf(SubtitleLanguage.NORWEGIAN, SubtitleLanguage.ENGLISH, SubtitleLanguage.SERVER, SubtitleLanguage.NONE)
    val languageTitle = when (options.subtitles) {
        SubtitleLanguage.NORWEGIAN -> "Norsk"
        SubtitleLanguage.ENGLISH -> "Engelsk"
        SubtitleLanguage.NONE -> "Ingen undertekstar"
        else -> "Tenaren sitt val"
    }
    SettingsChoiceRow("Undertekstar", languageTitle, "child-subtitles") {
        change(options.copy(subtitles = languages[(languages.indexOf(options.subtitles) + 1) % languages.size]))
    }
    SettingsGroup("Innhald og tilgang")
    Text("Bibliotek og aldersgrenser blir styrte av barnet sin eigen konto i Jellyfin eller Emby. Spole viser innhaldet den kontoen har tilgang til.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("PIN-koden vernar vegen tilbake til vaksenprofilen. Endringane her påverkar ikkje kontoane eller innstillingane til andre barn.",
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
