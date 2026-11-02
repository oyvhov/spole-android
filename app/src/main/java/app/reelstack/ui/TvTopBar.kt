package app.reelstack.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.ui.components.AccountAvatar
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.SpoleMotion
import app.reelstack.ui.theme.motionTween
import app.reelstack.ui.components.profileFlightTarget

/** The page does not repeat the corner actions when the television's menu already carries them. */
internal val LocalTopBarCarriesAccount = compositionLocalOf { false }

/** How far the page under the television's menu has scrolled from its own top; the menu rides up as far. */
internal class TopMenuLift {
    var pixels: () -> Int by mutableStateOf({ 0 })
}

internal val LocalTopMenuLift = staticCompositionLocalOf<TopMenuLift?> { null }

/** A page that scrolls under the television's menu lends it [list]'s distance from the top while it is shown. */
@Composable
internal fun LendTopMenuLift(list: LazyListState) {
    val lift = LocalTopMenuLift.current ?: return
    DisposableEffect(lift, list) {
        val read: () -> Int = { if (list.firstVisibleItemIndex > 0) Int.MAX_VALUE else list.firstVisibleItemScrollOffset }
        lift.pixels = read
        // Pages cross-fade: the one leaving lets go only if the one arriving has not taken over yet.
        onDispose { if (lift.pixels === read) lift.pixels = { 0 } }
    }
}

/**
 * A small, state-driven TV navigation line; focus alone never opens a destination. It lies on the picture
 * with nothing behind it, only at the top of the page: it rides up [lift] pixels with what scrolls under it,
 * and comes back while the remote is on it.
 */
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
    /** How far the page under the line has scrolled from its top, read while drawing. */
    lift: () -> Int = { 0 },
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, motionTween(SpoleMotion.ChangeMs), label = "tv-menu")
    val selectedFocus = remember { FocusRequester() }
    var focusWithin by remember { mutableStateOf(false) }
    var lineHeight by remember { mutableIntStateOf(0) }
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
    Row(modifier.fillMaxWidth()
        .graphicsLayer {
            this.alpha = alpha
            translationY = if (focusWithin) 0f else -lift().coerceIn(0, lineHeight).toFloat()
        }
        .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier)
        .onSizeChanged { lineHeight = it.height; onHeightChanged(it.height) }
        .padding(horizontal = 48.dp, vertical = 16.dp)
        .onFocusChanged { focusWithin = it.hasFocus; onFocusWithin(it.hasFocus) }
        .focusProperties { onEnter = { if (visible) selectedFocus.requestFocus() } }
        .onPreviewKeyEvent {
            if (visible && it.key == Key.DirectionDown && it.type == KeyEventType.KeyDown) {
                onDown()
                true
            } else false
        }.focusGroup().testTag("tv-top-bar"),
        verticalAlignment = Alignment.CenterVertically) {
        // Small words straight on the picture, from the left edge; the profile circle at the right.
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        val interaction = remember { MutableInteractionSource() }
        val accountDescription = stringResource(R.string.profile_switch_title)
        IconButton(onClick = onAccount, enabled = visible, interactionSource = interaction,
            modifier = Modifier.size(40.dp).focusOutline(interaction, androidx.compose.foundation.shape.CircleShape)
                .profileFlightTarget().testTag("tv-top-account").semantics { contentDescription = accountDescription }) {
            AccountAvatar(account, connection, Modifier.size(32.dp))
        }
    }
}

@StringRes
private fun TopMenuEntry.Page.labelRes(): Int = when (tab) {
    AppTab.HOME -> R.string.nav_home
    AppTab.DISCOVER -> R.string.nav_discover
    else -> R.string.nav_library
}

/** The words carry their own soft shadow instead of a band behind the line, so a bright poster keeps them white. */
private val TabShadow = Shadow(Color.Black.copy(alpha = .6f), Offset(0f, 1.5f), 8f)

@Composable
private fun TopBarTab(label: String, selected: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit,
    modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val fill by animateColorAsState(if (focused) colors.onSurface.copy(alpha = .16f) else Color.Transparent,
        motionTween(SpoleMotion.FocusMs), label = "tv-top-tab")
    Column(modifier.clip(shape).background(fill)
        .focusOutline(interaction, shape, glow = false)
        .selectable(selected, enabled = enabled, role = Role.Tab, interactionSource = interaction,
            indication = null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp).width(IntrinsicSize.Max).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected || focused) colors.onSurface else colors.onSurface.copy(alpha = .78f),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, shadow = TabShadow),
            maxLines = 1)
        // The page you are on has a short stroke in the accent under its word.
        Box(Modifier.padding(top = 3.dp).fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp))
            .background(if (selected) colors.primary else Color.Transparent))
    }
}
