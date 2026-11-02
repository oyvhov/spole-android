package app.reelstack.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.ui.components.AccountAvatar
import app.reelstack.ui.components.SpoleBrandMark
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.settingsSurface
import app.reelstack.ui.theme.SpoleMotion
import app.reelstack.ui.theme.motionTween
import app.reelstack.ui.components.profileFlightTarget

/** The page does not repeat the corner actions when the television's menu already carries them. */
internal val LocalTopBarCarriesAccount = compositionLocalOf { false }

/** A small, state-driven TV navigation surface; focus alone never opens a destination. */
@Composable
internal fun TvTopBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    shortcuts: List<Pair<String, String>>,
    selectedLibraryId: String?,
    onLibrarySelect: (String) -> Unit,
    discoverVisible: Boolean,
    searchOpen: Boolean,
    onSearch: () -> Unit,
    account: ServiceAccount?,
    connection: ServiceConnection?,
    onAccount: () -> Unit,
    visible: Boolean,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusWithin: (Boolean) -> Unit = {},
    onHeightChanged: (Int) -> Unit = {},
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, motionTween(SpoleMotion.ChangeMs), label = "tv-menu")
    val selectedFocus = remember { FocusRequester() }
    val pinned = shortcuts.distinctBy { it.first }
    val selectedShortcut = selectedLibraryId.takeIf { selectedTab == AppTab.LIBRARY && !searchOpen }
    val homeSelected = selectedTab == AppTab.HOME && !searchOpen
    // Settings lives in the profile menu. On that page Up still has a dependable Home anchor.
    val fallbackToHome = !searchOpen && !homeSelected && selectedShortcut !in pinned.map { it.first } &&
        !(selectedTab == AppTab.DISCOVER && discoverVisible) && !(selectedTab == AppTab.LIBRARY && pinned.isEmpty())
    Box(modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }
        .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier)) {
        Box(Modifier.fillMaxWidth().height(120.dp).background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = .12f), Color.Transparent))))
        Row(Modifier.fillMaxWidth().onSizeChanged { onHeightChanged(it.height) }.padding(horizontal = 48.dp, vertical = 20.dp)
            .onFocusChanged { onFocusWithin(it.hasFocus) }
            .focusProperties { onEnter = { if (visible) selectedFocus.requestFocus() } }
            .onPreviewKeyEvent {
                if (visible && it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                    onDown()
                    true
                } else false
            }.focusGroup().testTag("tv-top-bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SpoleBrandMark(Modifier.size(32.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopBarTab(stringResource(R.string.nav_home), homeSelected, visible, "tv-top-HOME", { onSelect(AppTab.HOME) },
                    if (homeSelected || fallbackToHome) Modifier.focusRequester(selectedFocus) else Modifier)
                if (pinned.isEmpty()) {
                    val selected = selectedTab == AppTab.LIBRARY && !searchOpen
                    TopBarTab(stringResource(R.string.nav_library), selected, visible, "tv-top-LIBRARY", { onSelect(AppTab.LIBRARY) },
                        if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                } else pinned.forEach { (id, label) -> key(id) {
                    val selected = selectedShortcut == id
                    TopBarTab(label, selected, visible, "tv-top-library-$id", { onLibrarySelect(id) },
                        if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                } }
                if (discoverVisible) {
                    val selected = selectedTab == AppTab.DISCOVER && !searchOpen
                    TopBarTab(stringResource(R.string.nav_discover), selected, visible, "tv-top-DISCOVER", { onSelect(AppTab.DISCOVER) },
                        if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                }
                TopBarTab(stringResource(R.string.tv_menu_search), searchOpen, visible, "tv-top-search", onSearch,
                    if (searchOpen) Modifier.focusRequester(selectedFocus) else Modifier)
            }
            val interaction = remember { MutableInteractionSource() }
            val accountDescription = stringResource(R.string.profile_switch_title)
            IconButton(onClick = onAccount, enabled = visible, interactionSource = interaction,
                modifier = Modifier.size(48.dp).focusOutline(interaction, androidx.compose.foundation.shape.CircleShape)
                    .profileFlightTarget().testTag("tv-top-account").semantics { contentDescription = accountDescription }) {
                AccountAvatar(account, connection, Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun TopBarTab(label: String, selected: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium,
        maxLines = 1, modifier = modifier.heightIn(min = 48.dp)
            .settingsSurface(interaction, shape, selected = selected)
            .selectable(selected, enabled = enabled, role = Role.Tab, interactionSource = interaction,
                indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp).testTag(tag))
}
