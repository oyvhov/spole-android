package app.reelstack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SettingsActionRow
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.LocalPersonalization

@Composable
internal fun HomeRowOrderSetting(state: ReelstackUiState, onChange: (List<HomeRow>) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    SettingsActionRow(stringResource(R.string.home_order_title), stringResource(R.string.home_order_hint),
        "home-order-open") { open = true }
    if (open) HomeRowOrderDialog(state, onChange) { open = false }
}

@Composable
internal fun HomeRowOrderDialog(state: ReelstackUiState, onChange: (List<HomeRow>) -> Unit, onDismiss: () -> Unit) {
    val options = LocalPersonalization.current
    val combined = options.showNextUp && options.combineContinueWatching && HomeSection.CONTINUE_WATCHING in state.homeSections
    val order = decodeHomeRowOrder(state.homeRowOrder.joinToString(",") { it.name })
    val sources = state.connections.filter { it.token.isNotBlank() }.map { it.kind }
    val available = order.filter { row ->
        when (row) {
            HomeRow.NEXT_UP -> !combined
            HomeRow.JELLYFIN_MOVIES, HomeRow.JELLYFIN_SERIES -> ServiceKind.JELLYFIN in sources || state.configuredCount == 0
            HomeRow.EMBY_MOVIES, HomeRow.EMBY_SERIES -> ServiceKind.EMBY in sources || state.configuredCount == 0
            else -> true
        }
    }
    val focus = remember { HomeRow.entries.associateWith { listOf(FocusRequester(), FocusRequester()) } }
    val listState = rememberLazyListState()
    val resetFocus = remember { FocusRequester() }
    var resetVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(resetVersion) {
        if (resetVersion > 0) {
            // Wait until the reordered keyed items have been laid out before scrolling.
            withFrameNanos { }
            listState.scrollToItem(0)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 600.dp).fillMaxWidth(.94f).fillMaxHeight(.9f).testTag("home-order-dialog"),
            shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_order_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.home_order_saved), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (combined) Text(stringResource(R.string.home_order_combined), style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f).testTag("home-order-list"), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(available, key = { _, row -> row.name }) { index, row ->
                        val title = homeRowTitle(row, combined)
                        val hidden = if (row == HomeRow.NEXT_UP) !options.showNextUp else row.section !in state.homeSections
                        Row(Modifier.fillMaxWidth().testTag("home-order-row-$row").padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                                if (hidden) Text(stringResource(R.string.home_order_hidden), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            listOf(-1, 1).forEachIndexed { button, direction ->
                                val interaction = remember { MutableInteractionSource() }
                                IconButton(onClick = {
                                    // Move focus before disabling the arrow at a boundary. A
                                    // deferred request races Compose's automatic focus recovery.
                                    val destination = index + direction
                                    val nextButton = if (destination == 0) 1 else if (destination == available.lastIndex) 0 else button
                                    focus.getValue(row)[nextButton].requestFocus()
                                    onChange(moveHomeRow(order, row, direction, available))
                                }, enabled = if (direction < 0) index > 0 else index < available.lastIndex,
                                    interactionSource = interaction,
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        .focusOutline(interaction, CircleShape)
                                        .focusRequester(focus.getValue(row)[button]).testTag("home-order-$row-$button")) {
                                    Icon(if (direction < 0) SpoleIcons.ChevronUp else SpoleIcons.ChevronDown,
                                        stringResource(if (direction < 0) R.string.home_order_up else R.string.home_order_down, title))
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    resetFocus.requestFocus()
                    onChange(HomeRow.entries)
                    resetVersion++
                }, modifier = Modifier.focusRequester(resetFocus).testTag("home-order-reset")) {
                    Text(stringResource(R.string.home_order_reset))
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("home-order-done")) {
                    Text(stringResource(R.string.home_order_done))
                }
            }
        }
    }
}

@Composable
private fun homeRowTitle(row: HomeRow, combined: Boolean): String = when (row) {
    HomeRow.NOW_PLAYING -> stringResource(R.string.home_now_playing)
    HomeRow.CONTINUE_WATCHING -> stringResource(if (combined) R.string.tv_continue_combined else R.string.home_continue)
    HomeRow.FAVOURITES -> stringResource(R.string.home_favourites)
    HomeRow.NEXT_UP -> stringResource(R.string.tv_next_up)
    HomeRow.JELLYFIN_MOVIES -> stringResource(R.string.settings_movies, "Jellyfin")
    HomeRow.EMBY_MOVIES -> stringResource(R.string.settings_movies, "Emby")
    HomeRow.JELLYFIN_SERIES -> stringResource(R.string.settings_series, "Jellyfin")
    HomeRow.EMBY_SERIES -> stringResource(R.string.settings_series, "Emby")
    HomeRow.RECOMMENDATIONS -> stringResource(R.string.home_recommendations)
    HomeRow.RECENT_RELEASES -> stringResource(R.string.home_recent_releases)
    HomeRow.UPCOMING -> stringResource(R.string.home_upcoming)
}
