package app.reelstack.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.reelstack.R
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.shelfId
import app.reelstack.data.model.touchMenu
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.shelfIcon
import app.reelstack.ui.components.vector

@Composable
internal fun AppSidebarSlot(
    expanded: Boolean,
    hidden: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Commit the page width once. The rail reveals/clips above it instead of resizing every
    // poster, gradient and lazy grid on every animation frame.
    Box(Modifier.width(if (hidden) 0.dp else if (expanded) 200.dp else 80.dp).fillMaxHeight().zIndex(1f).testTag("sidebar-slot")) {
        Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) { content() }
    }
}

@Composable
internal fun AppNavigationRail(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    onFocusWithin: (Boolean) -> Unit = {},
    shortcuts: List<Pair<String, String>> = emptyList(),
    libraryIcons: Map<String, LibraryIcon> = emptyMap(),
    selectedLibraryId: String? = null,
    onLibrarySelect: (String) -> Unit = {},
    isKidMode: Boolean = false,
    modifier: Modifier = Modifier,
    compactTouch: Boolean = false,
    smartShelves: List<app.reelstack.data.model.SmartShelf> = emptyList(),
) {
    if (compactTouch) {
        AppCompactTouchNavigation(selectedTab, onSelect, isKidMode, modifier)
        return
    }
    val selectedFocus = remember { FocusRequester() }
    val width by animateDpAsState(
        if (expanded) 200.dp else 80.dp, tween(220, easing = FastOutSlowInEasing), label = "sidebar-width")
    val labelAlpha by animateFloatAsState(
        if (expanded) 1f else 0f, tween(140), label = "sidebar-labels")
    val tv = (LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val toggleLabel = stringResource(if (expanded) R.string.sidebar_collapse else R.string.sidebar_expand)
    val brandInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(modifier.width(width).fillMaxHeight().clip(RoundedCornerShape(0.dp))
        .testTag("side-navigation").background(app.reelstack.ui.theme.Surface)) {
        // Where the controls end, measured from the rail's own top, so the season's scene starts below them.
        var railTop by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        var controlsEnd by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
        app.reelstack.ui.components.SeasonalBackdrop(Modifier.matchParentSize()
            .onGloballyPositioned { railTop = it.positionInWindow().y }, menu = true, menuClearAbove = { controlsEnd - railTop })
        app.reelstack.ui.components.SeasonalIdleSpider(Modifier.matchParentSize())
        Column(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(200.dp).fillMaxHeight()
            .onFocusChanged { if (tv) onFocusWithin(it.hasFocus) }
            .focusProperties { onEnter = { if (tv) selectedFocus.requestFocus() } }.focusGroup()
            .padding(horizontal = 12.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).then(if (!tv) Modifier
                .testTag("sidebar-toggle").semantics { contentDescription = toggleLabel }
                .clip(RoundedCornerShape(16.dp))
                .focusOutline(brandInteraction, RoundedCornerShape(16.dp))
                .clickable(interactionSource = brandInteraction, indication = androidx.compose.foundation.LocalIndication.current,
                    role = Role.Button, onClick = { onExpandedChange(!expanded) }) else Modifier)
                .padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                app.reelstack.ui.components.SpoleBrandMark(Modifier.size(28.dp))
                androidx.compose.material3.Text(app.reelstack.ui.theme.LocalPersonalization.current.appLabel,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp)
                        .graphicsLayer { alpha = labelAlpha }.clearAndSetSemantics {})
            }
            // Downloads have no television or kid-mode presentation, and join the menu only on request.
            val menu = app.reelstack.ui.theme.LocalPersonalization.current.touchMenu(television = tv, kidMode = isKidMode)
            // During a season its shelf joins the library shortcuts, first among them. On a
            // television this is the way in: Home there opens on artwork, not on the greeting.
            val season = app.reelstack.ui.components.touchSeason()
            val seasonId = season.takeIf { it != app.reelstack.data.model.Season.NONE }?.shelfId()
            // Smart shelves the owner put in the menu, while their period runs; never the season's twice.
            val shelfRows = smartShelves.filter { it.pathId != seasonId }
            val pinned = shortcuts.map { it.first } + listOfNotNull(seasonId) + shelfRows.map { it.pathId }
            menu
                .filterNot { isKidMode && it == AppTab.SETTINGS.name }
                .mapNotNull { name -> tabs.find { it.tab.name == name } }.forEach { item ->
                    if (item.tab == AppTab.SETTINGS && seasonId != null) NavigationControl(
                        stringResource(if (season == app.reelstack.data.model.Season.CHRISTMAS) R.string.theme_season_christmas
                            else R.string.theme_season_halloween),
                        season.shelfIcon(), selectedLibraryId == seasonId,
                        { onLibrarySelect(seasonId) }, labelAlpha, Role.Tab, Modifier.width(width - 24.dp)
                            .then(if (selectedLibraryId == seasonId) Modifier.focusRequester(selectedFocus) else Modifier)
                            .testTag("wide-season-$season"))
                    if (item.tab == AppTab.SETTINGS) shelfRows.forEach { shelf ->
                        NavigationControl(app.reelstack.ui.components.smartShelfName(shelf), shelf.icon.vector(),
                            selectedLibraryId == shelf.pathId, { onLibrarySelect(shelf.pathId) }, labelAlpha, Role.Tab,
                            Modifier.width(width - 24.dp)
                                .then(if (selectedLibraryId == shelf.pathId) Modifier.focusRequester(selectedFocus) else Modifier)
                                .testTag("wide-shelf-${shelf.id}"))
                    }
                    if (item.tab == AppTab.SETTINGS) shortcuts.forEach { (id, name) ->
                        NavigationControl(name, (libraryIcons[id] ?: LibraryIcon.LIBRARY).vector(), selectedLibraryId == id,
                            { onLibrarySelect(id) }, labelAlpha, Role.Tab, Modifier.width(width - 24.dp)
                                .then(if (selectedLibraryId == id) Modifier.focusRequester(selectedFocus) else Modifier).testTag("wide-library-$id"))
                    }
                    NavigationControl(stringResource(item.label), item.icon, selectedTab == item.tab &&
                        (item.tab != AppTab.LIBRARY || selectedLibraryId !in pinned),
                        { onSelect(item.tab) }, labelAlpha, Role.Tab, Modifier.width(width - 24.dp)
                            .then(if (selectedTab == item.tab && (item.tab != AppTab.LIBRARY || selectedLibraryId !in pinned))
                                Modifier.focusRequester(selectedFocus) else Modifier).testTag("wide-tab-${item.tab.name}"))
                }
            Spacer(Modifier.height(0.dp).onGloballyPositioned { controlsEnd = it.positionInWindow().y })
        }
    }
}

/** Keep the same focusable nodes and icon positions in both sizes. Only labels fade and clip. */
@Composable
internal fun AppCompactTouchNavigation(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    isKidMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val menu = app.reelstack.ui.theme.LocalPersonalization.current.touchMenu(television = false, kidMode = isKidMode)
    Column(modifier.width(80.dp).fillMaxHeight().background(app.reelstack.ui.theme.Surface)
        .padding(horizontal = 8.dp, vertical = 4.dp).testTag("compact-touch-navigation")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            menu.filter { it != "SETTINGS" }.forEach { name ->
                val item = tabs.first { it.tab.name == name }
                NavigationControl(stringResource(item.label), item.icon, selectedTab == item.tab,
                    { onSelect(item.tab) }, 0f, Role.Tab, Modifier.testTag("compact-tab-$name"))
            }
        }
        if (!isKidMode) {
            val settings = tabs.first { it.tab == AppTab.SETTINGS }
            NavigationControl(stringResource(settings.label), settings.icon, selectedTab == AppTab.SETTINGS,
                { onSelect(AppTab.SETTINGS) }, 0f, Role.Tab, Modifier.testTag("compact-tab-SETTINGS"))
        }
    }
}
