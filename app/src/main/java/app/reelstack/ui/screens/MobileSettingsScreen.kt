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

/** The phone opens one group at a time; returning preserves that group's reading position. */
@Composable
internal fun MobileSettingsScreen(state: ReelstackUiState, contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit,
    onHomeRowOrderChange: (List<HomeRow>) -> Unit = {}, onSignOutAll: () -> Unit = {}, onAddProfile: () -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    val panes = rememberSaveableStateHolder()
    LaunchedEffect(state.accountsSettingsRequest) {
        if (state.accountsSettingsRequest > 0) page = SettingsCategory.ACCOUNTS
    }
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
                        SettingsCategory.entries.forEach { destination ->
                            SettingsActionRow(stringResource(destination.title), stringResource(destination.hint),
                                "settings-category-${destination.name}", destination.icon, { page = destination })
                        }
                    } else {
                        Text(stringResource(selected.hint), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                        SharedSettingsContent(selected, state, onConnectionClick, onNotificationsChange, onWifiOnlyChange,
                            onHomeSectionChange, onAccountClick, onManageLibraries, onHomeRowOrderChange, onSignOutAll, onAddProfile)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            } }
        }
    }
}
