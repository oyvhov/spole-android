package app.reelstack.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.clip
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
    /** The owner's «Sider og meny», in order. Without it: Home, Library and, when switched on, Discover. */
    menu: List<String>? = null,
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, motionTween(SpoleMotion.ChangeMs), label = "tv-menu")
    val selectedFocus = remember { FocusRequester() }
    val entries = remember(menu, discoverVisible, shortcuts) {
        topMenuEntries(menu ?: listOfNotNull("HOME", "LIBRARY", "DISCOVER".takeIf { discoverVisible }),
            shortcuts.distinctBy { it.first })
    }
    fun isSelected(entry: TopMenuEntry): Boolean = when (entry) {
        TopMenuEntry.Search -> searchOpen
        is TopMenuEntry.Library -> !searchOpen && selectedTab == AppTab.LIBRARY && selectedLibraryId == entry.id
        is TopMenuEntry.Page -> !searchOpen && selectedTab == entry.tab
    }
    // Settings lives in the profile menu. On that page, and wherever the menu holds no entry for the
    // page, Up still has a dependable Home anchor.
    val focusTarget = entries.firstOrNull { isSelected(it) } ?: TopMenuEntry.Page(AppTab.HOME)
    Box(modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }
        .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier)) {
        // The picture runs behind the line, so the words carry their own shade: a bright poster must
        // not take the white from them.
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = .75f), Color.Black.copy(alpha = .35f), Color.Transparent))))
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpoleBrandMark(Modifier.size(32.dp))
            // The tabs sit in one capsule over the picture, centred between the mark and the profile circle.
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val capsule = RoundedCornerShape(50)
                Row(Modifier.clip(capsule).background(Color.Black.copy(alpha = .42f))
                    .border(1.dp, Color.White.copy(alpha = .10f), capsule)
                    .horizontalScroll(rememberScrollState()).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    entries.forEach { entry ->
                        val entryFocus = if (entry == focusTarget) Modifier.focusRequester(selectedFocus) else Modifier
                        when (entry) {
                            is TopMenuEntry.Page -> key(entry.tab) {
                                TopBarTab(stringResource(entry.labelRes()), isSelected(entry), visible, "tv-top-${entry.tab.name}",
                                    { onSelect(entry.tab) }, entryFocus)
                            }
                            is TopMenuEntry.Library -> key(entry.id) {
                                TopBarTab(entry.label, isSelected(entry), visible, "tv-top-library-${entry.id}",
                                    { onLibrarySelect(entry.id) }, entryFocus)
                            }
                            TopMenuEntry.Search -> TopBarTab(stringResource(R.string.tv_menu_search), isSelected(entry), visible,
                                "tv-top-search", onSearch, entryFocus)
                        }
                    }
                }
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

@StringRes
private fun TopMenuEntry.Page.labelRes(): Int = when (tab) {
    AppTab.HOME -> R.string.nav_home
    AppTab.DISCOVER -> R.string.nav_discover
    else -> R.string.nav_library
}

@Composable
private fun TopBarTab(label: String, selected: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val fill by animateColorAsState(
        when {
            focused -> colors.onSurface.copy(alpha = .16f)
            selected -> colors.primary.copy(alpha = .26f)
            else -> Color.Transparent
        }, motionTween(SpoleMotion.FocusMs), label = "tv-top-tab")
    Text(label, color = if (selected || focused) colors.onSurface else colors.onSurface.copy(alpha = .78f),
        style = MaterialTheme.typography.titleMedium, maxLines = 1,
        modifier = modifier.heightIn(min = 44.dp).clip(shape).background(fill)
            .focusOutline(interaction, shape, glow = false)
            .selectable(selected, enabled = enabled, role = Role.Tab, interactionSource = interaction,
                indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp).testTag(tag))
}
