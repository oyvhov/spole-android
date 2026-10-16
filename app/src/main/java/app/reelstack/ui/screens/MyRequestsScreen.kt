package app.reelstack.ui.screens

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.runtime.withFrameNanos

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.ui.focus.focusRequester

import androidx.compose.ui.focus.FocusRequester

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.IntrinsicSize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.MyRequestsTab
import app.reelstack.data.model.RequestStage
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.activityDayGroup
import app.reelstack.data.model.hasTitleMetadata
import app.reelstack.data.model.myRequestsFor
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.components.mediaCardIndication
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

/** The quiet line between list entries. */
internal val LineDivider: Color @Composable get() = TextColor.copy(alpha = .08f)

/** Requests as lines. Opening one goes to its title page, where every action on it lives. */
@Composable
fun MyRequestsScreen(
    state: ReelstackUiState,
    contentPadding: PaddingValues,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onLoadOlder: () -> Unit,
    onEvent: (String) -> Unit = {},
) {
    val tabs = MyRequestsTab.entries.filter { it != MyRequestsTab.SERVER || state.adminView }
    var tab by rememberSaveable { mutableStateOf(MyRequestsTab.ALL) }
    val older = state.requestHistory.items.filterNot { old ->
        state.trackedRequests.any { it.mediaId == old.mediaId && it.mediaType == old.mediaType && it.is4k == old.is4k }
    }
    val lines = when (tab) {
        MyRequestsTab.ALL -> myRequestsFor(tab, state.trackedRequests) + older
        else -> myRequestsFor(tab, state.trackedRequests)
    }
    val list = rememberLazyListState()
    val tv = app.reelstack.ui.components.isTelevision()
    // On a television the link that opened this page is gone; the remote starts on the chosen tab.
    val tabFocus = remember { FocusRequester() }
    LaunchedEffect(tv) {
        if (!tv) return@LaunchedEffect
        repeat(40) {
            withFrameNanos { }
            if (runCatching { tabFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val alerts = when {
        !state.notificationsEnabled -> stringResource(R.string.my_requests_alerts_off)
        !app.reelstack.background.LibraryNotifications.allowed(context) -> stringResource(R.string.my_requests_alerts_android)
        else -> null
    }
    ReelPage(media = true) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().testTag("my-requests"),
            contentPadding = PaddingValues(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item(key = "head") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val backInteraction = remember { MutableInteractionSource() }
                    IconButton(onClick = onBack, interactionSource = backInteraction, modifier = Modifier.offset(x = (-12).dp)
                        .focusOutline(backInteraction, CircleShape).testTag("my-requests-back")) {
                        Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                    }
                    Text(stringResource(R.string.my_requests_title), style = MaterialTheme.typography.headlineSmall)
                }
            }
            item(key = "tabs") {
                Column {
                    TextTabs(tabs, tab, { tab = it }, tabFocus) { choice ->
                        val count = when (choice) {
                            MyRequestsTab.SERVER -> null
                            MyRequestsTab.ALL -> state.trackedRequests.size + older.size
                            else -> myRequestsFor(choice, state.trackedRequests).size
                        }
                        stringResource(when (choice) {
                            MyRequestsTab.ALL -> R.string.my_requests_tab_all
                            MyRequestsTab.IN_PROGRESS -> R.string.my_requests_tab_in_progress
                            MyRequestsTab.READY -> R.string.my_requests_tab_ready
                            MyRequestsTab.SERVER -> R.string.my_requests_tab_server
                        }) to count
                    }
                    HorizontalDivider(color = LineDivider)
                    alerts?.let {
                        Text(it, color = Muted, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp).testTag("my-requests-alerts"))
                    }
                    state.trackingError?.let {
                        Text(it, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            if (tab == MyRequestsTab.SERVER) {
                var lastGroup: Int? = null
                state.activity.forEach { event ->
                    val group = activityDayGroup(event)
                    if (group != lastGroup) {
                        lastGroup = group
                        item(key = "day-${event.id}") {
                            Text(stringResource(group), color = Muted, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                        }
                    }
                    item(key = "event-${event.id}") { ServerEventLine(event) { onEvent(event.id) } }
                }
            } else if (lines.isEmpty()) {
                item(key = "empty") {
                    if (state.trackingLoading) app.reelstack.ui.components.ActivitySkeleton(Modifier.fillMaxWidth().padding(top = 12.dp))
                    else Text(stringResource(R.string.my_requests_empty), color = Muted, modifier = Modifier.padding(vertical = 24.dp))
                }
            } else {
                items(lines, key = { "line-${it.key}" }) { request -> RequestLine(request, { onOpen(request.key) }) }
                if (tab == MyRequestsTab.ALL && state.requestHistory.hasMore) item(key = "older") {
                    LaunchedEffect(state.requestHistory.items.size) { onLoadOlder() }
                    Text(stringResource(R.string.my_requests_older), color = Muted, modifier = Modifier.padding(vertical = 18.dp))
                }
            }
        }
    }
}

/** Text tabs with a line under the chosen one. Wraps at large text instead of clipping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextTabs(
    tabs: List<MyRequestsTab>,
    selected: MyRequestsTab,
    onSelect: (MyRequestsTab) -> Unit,
    firstFocus: FocusRequester? = null,
    label: @Composable (MyRequestsTab) -> Pair<String, Int?>,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 4.dp)) {
        tabs.forEach { choice ->
            val (text, count) = label(choice)
            val interaction = remember { MutableInteractionSource() }
            val chosen = choice == selected
            // As wide as its words: a full-width line under each tab put every tab on its own row.
            Column(
                Modifier.width(IntrinsicSize.Max).then(if (chosen && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier)
                    .focusOutline(interaction, RoundedCornerShape(8.dp))
                    .selectable(chosen, interactionSource = interaction, indication = mediaCardIndication(), role = Role.Tab) { onSelect(choice) }
                    .padding(vertical = 10.dp)
                    .testTag("my-requests-tab-${choice.name}"),
            ) {
                Text(if (count == null) text else "$text  $count", color = if (chosen) TextColor else Muted,
                    style = MaterialTheme.typography.titleSmall)
                Box(Modifier.padding(top = 6.dp).height(2.dp).fillMaxWidth()
                    .background(if (chosen) TextColor else Color.Transparent))
            }
        }
    }
}

/** One request: cover, title, status and, while on the way, a thin line of progress. */
@Composable
internal fun RequestLine(request: TrackedRequest, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(ReelLayout.ArtworkCorner)
    Column(
        modifier.fillMaxWidth()
            .focusOutline(interaction, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
            .testTag("my-request-${request.key}"),
    ) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaArtwork(request.artworkUrl, null,
                Modifier.width(48.dp).aspectRatio(2f / 3f).clip(shape).background(SurfaceRaised),
                fallbackRes = R.drawable.media_placeholder, contentScale = ContentScale.Crop, source = ServiceKind.SEERR)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (request.hasTitleMetadata) request.title
                    else stringResource(R.string.my_requests_unresolved, request.requestId ?: request.mediaId),
                    style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(requestLineStatus(request), color = Muted, style = MaterialTheme.typography.bodySmall)
                val percent = request.percent
                if (request.stage == RequestStage.DOWNLOADING && percent != null) {
                    Box(Modifier.padding(top = 2.dp).fillMaxWidth().height(2.dp).background(TextColor.copy(alpha = .12f))) {
                        Box(Modifier.fillMaxWidth(percent / 100f).height(2.dp).background(TextColor))
                    }
                }
            }
        }
        HorizontalDivider(color = LineDivider)
    }
}

/** «Sesong 3  ·  Lastar ned  ·  64 %», as much of it as is known. */
@Composable
internal fun requestLineStatus(request: TrackedRequest): String {
    val seasons = request.seasons.sorted()
    return listOfNotNull(
        seasons.takeIf { it.isNotEmpty() }?.let { pluralStringResource(R.plurals.my_requests_seasons, it.size, it.joinToString(", ")) },
        app.reelstack.localization.requestStageLabel(request.stage),
        request.percent?.takeIf { request.stage == RequestStage.DOWNLOADING }?.let { "$it %" },
    ).joinToString("  ·  ")
}

@Composable
private fun ServerEventLine(event: ActivityEvent, onClick: () -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier.fillMaxWidth()
            .focusOutline(interaction, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = mediaCardIndication(), role = Role.Button, onClick = onClick)
            .testTag("server-event-${event.id}"),
    ) {
        Text(event.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
        Text(
            listOfNotNull(event.time.text(context), event.detail.text(context), event.source?.displayName)
                .filter { it.isNotBlank() }.joinToString("  ·  "),
            color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
        )
        HorizontalDivider(color = LineDivider)
    }
}
