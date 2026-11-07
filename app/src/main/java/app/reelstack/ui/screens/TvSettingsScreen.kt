package app.reelstack.ui.screens

import app.reelstack.ui.components.safeBottomFade
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
import app.reelstack.ui.theme.SpoleMotion

/** Television is an input mode, not a width breakpoint: 1080p TVs are often only 960 dp wide. */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun TvSettingsScreen(state: ReelstackUiState, contentPadding: PaddingValues,
    onConnectionClick: (ServiceKind) -> Unit, onNotificationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit, onHomeSectionChange: (HomeSection, Boolean) -> Unit,
    onAccountClick: (ServiceKind) -> Unit, onManageLibraries: () -> Unit,
    onHomeRowOrderChange: (List<HomeRow>) -> Unit = {}, onSignOutAll: () -> Unit = {}, onAddProfile: () -> Unit = {},
    onClearLibraryCache: () -> Unit = {}, onRequestPinSetup: (String) -> Unit = {}, onDisablePin: () -> Unit = {},
    homeEditor: HomeEditorActions = HomeEditorActions(), onOpenDownloads: () -> Unit = {},
    onOwnProfile: (ServiceKind, String, String?) -> Unit = { _, _, _ -> }) {
    val television = isTelevision()
    val motion = app.reelstack.ui.theme.LocalMotionEnabled.current
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
    // The bottom inset is kept inside the lists, not round them: a list that stopped short of the screen's edge cut
    // «Legg til barneprofil» through the middle of its line 72 dp above it (Loftet, 2026-11-07). The settings run on
    // to the edge and fade out there, as a television detail page does.
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val bottomInset = contentPadding.calculateBottomPadding()
    Row(Modifier.fillMaxSize()
        .padding(start = contentPadding.calculateStartPadding(layoutDirection), top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection))
        .padding(start = 24.dp, end = 24.dp, top = 24.dp)
        .testTag("tv-settings"),
        horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.width(192.dp).fillMaxHeight().padding(bottom = bottomInset + 24.dp).verticalScroll(rememberScrollState())
            .testTag("settings-categories"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.nav_settings), style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            SettingsCategory.entries.forEach { item ->
                SettingsCategoryDestination(item, focusedCategory == item,
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
                // Downloads open as their own page rather than a pane: the list refreshes itself
                // while it is on screen. Television has no downloads at all.
                if (item == SettingsCategory.UPDATES && !television && !state.isKidMode)
                    WideDestination(stringResource(R.string.nav_downloads), SpoleIcons.Download, selected = false,
                        onClick = onOpenDownloads, modifier = Modifier.testTag("settings-downloads"))
            }
            if (television) Text(stringResource(R.string.settings_tv_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            SettingsCategoryIntro(category)
            Spacer(Modifier.height(8.dp))
            androidx.compose.animation.AnimatedContent(
                targetState = category,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(app.reelstack.ui.theme.motionSpec(motion, SpoleMotion.ChangeMs)) togetherWith
                        androidx.compose.animation.fadeOut(app.reelstack.ui.theme.motionSpec(motion, SpoleMotion.FocusMs))
                },
                label = "settings-pane-crossfade",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { activeCategory ->
                panes.SaveableStateProvider(activeCategory.name) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides
                            app.reelstack.ui.components.rememberSafeDetailBringIntoView()) {
                    Column(Modifier.fillMaxSize().safeBottomFade().focusRequester(panelFocus)
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
                            onAddProfile, onClearLibraryCache, onRequestPinSetup, onDisablePin, homeEditor, onOwnProfile)
                        // Room for the last row to come up out of the fade and the safe area below it.
                        Spacer(Modifier.height(16.dp + app.reelstack.ui.components.DetailSafeBottom + bottomInset))
                    }
                    }
                }
            }
        }
    }
}
