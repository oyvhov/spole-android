package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.offline.OfflineDownloadItem
import app.reelstack.offline.OfflineDownloadState
import app.reelstack.offline.OfflineDownloadsSnapshot
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.SpoleSecondaryButton
import app.reelstack.ui.theme.ReelLayout
import app.reelstack.ui.theme.ReelPage
import kotlin.math.roundToInt

/**
 * The local collection, not a server queue. It never lists another profile's downloads and it
 * deliberately exists only in the adult touch layout; TV has neither this route nor its actions.
 *
 * [onBack] is set when the page is opened from Settings rather than from its own menu item. It
 * then reads as a settings page, with the same back arrow and heading size.
 */
@Composable
fun OfflineDownloadsScreen(
    snapshot: OfflineDownloadsSnapshot,
    wifiOnly: Boolean,
    contentPadding: PaddingValues,
    onWifiOnlyChange: (Boolean) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onPlay: (String) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var deleteCandidate by remember { mutableStateOf<OfflineDownloadItem?>(null) }
    ReelPage {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).testTag("offline-downloads"),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("heading") {
                if (onBack != null) Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("offline-back")) {
                            Icon(SpoleIcons.ArrowBack, stringResource(R.string.action_back))
                        }
                        Text(stringResource(R.string.offline_library_title), style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.offline_library_scope), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ReelLayout.Gutter))
                } else Column(Modifier.padding(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = ReelLayout.PageTop)) {
                    Text(stringResource(R.string.offline_library_title), style = MaterialTheme.typography.displaySmall)
                    Text(stringResource(R.string.offline_library_scope), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item("summary") {
                DownloadSummary(
                    snapshot = snapshot,
                    modifier = Modifier.padding(horizontal = ReelLayout.Gutter),
                    onPauseAll = onPauseAll,
                    onResumeAll = onResumeAll,
                )
            }
            item("wifi") {
                WifiPreference(
                    wifiOnly = wifiOnly,
                    modifier = Modifier.padding(horizontal = ReelLayout.Gutter),
                    onChange = onWifiOnlyChange,
                )
            }
            if (snapshot.items.isEmpty()) {
                item("empty") {
                    OfflineEmptyState(Modifier.padding(horizontal = ReelLayout.Gutter, vertical = 36.dp))
                }
            } else {
                item("items-title") {
                    Text(stringResource(R.string.offline_library_items), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = 10.dp, bottom = 2.dp))
                }
                items(snapshot.items, key = { it.id }) { item ->
                    OfflineDownloadRow(
                        item = item,
                        modifier = Modifier.padding(horizontal = ReelLayout.Gutter),
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onRetry = { onRetry(item.id) },
                        onRemove = { deleteCandidate = item },
                        onPlay = { onPlay(item.id) },
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(28.dp)) }
        }
    }
    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.offline_remove_title)) },
            text = { Text(stringResource(R.string.offline_remove_message, item.title.ifBlank { stringResource(R.string.offline_unknown_title) })) },
            confirmButton = {
                Button(onClick = { onRemove(item.id); deleteCandidate = null }, modifier = Modifier.testTag("offline-remove-confirm")) {
                    Text(stringResource(R.string.offline_remove_action))
                }
            },
            dismissButton = {
                SpoleSecondaryButton(onClick = { deleteCandidate = null }) { Text(stringResource(R.string.offline_keep)) }
            },
        )
    }
}

@Composable
private fun DownloadSummary(
    snapshot: OfflineDownloadsSnapshot,
    modifier: Modifier = Modifier,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
) {
    val activeItems = snapshot.activeItems()
    val pausedItems = snapshot.items.count { it.state == OfflineDownloadState.PAUSED }
    val progress = snapshot.overallProgress()
    val locale = LocalConfiguration.current.locales[0]
    val summary: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(SpoleIcons.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        stringResource(if (activeItems.isNotEmpty()) R.string.offline_summary_active else R.string.offline_summary_ready),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        when {
                            activeItems.isNotEmpty() -> stringResource(R.string.offline_summary_progress, activeItems.size, (progress * 100).roundToInt())
                            pausedItems > 0 -> stringResource(R.string.offline_summary_paused, pausedItems)
                            snapshot.items.isNotEmpty() -> stringResource(R.string.offline_summary_size, formatOfflineBytes(snapshot.completedBytes, locale))
                            else -> stringResource(R.string.offline_summary_empty)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (activeItems.isNotEmpty()) DownloadProgress(progress, Modifier.testTag("offline-summary-progress"))
        }
    }
    Surface(modifier = modifier.fillMaxWidth().testTag("offline-summary"), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(ReelLayout.ControlCorner)) {
        Box(Modifier.padding(16.dp)) {
            when {
                activeItems.isNotEmpty() -> ActionRow(content = summary) {
                    SpoleSecondaryButton(onClick = onPauseAll, modifier = Modifier.testTag("offline-pause-all")) {
                        Icon(SpoleIcons.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_pause))
                    }
                }
                pausedItems > 0 -> ActionRow(content = summary) {
                    SpoleSecondaryButton(onClick = onResumeAll, modifier = Modifier.testTag("offline-resume-all")) {
                        Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_resume))
                    }
                }
                else -> summary()
            }
        }
    }
}

private fun OfflineDownloadsSnapshot.activeItems() =
    items.filter { it.state == OfflineDownloadState.DOWNLOADING || it.state == OfflineDownloadState.QUEUED }

private fun OfflineDownloadsSnapshot.overallProgress(): Float =
    if (totalBytes > 0) (completedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    else activeItems().map { it.progress }.average().toFloat().takeIf { !it.isNaN() } ?: 0f

/** The one line under «Nedlastingar» in Settings: what is happening, or what the page is for. */
@Composable
internal fun offlineSettingsSummary(snapshot: OfflineDownloadsSnapshot): String {
    val active = snapshot.activeItems()
    val paused = snapshot.items.count { it.state == OfflineDownloadState.PAUSED }
    return when {
        active.isNotEmpty() -> stringResource(R.string.offline_summary_progress, active.size, (snapshot.overallProgress() * 100).roundToInt())
        paused > 0 -> stringResource(R.string.offline_summary_paused, paused)
        snapshot.items.isNotEmpty() -> stringResource(R.string.offline_summary_size,
            formatOfflineBytes(snapshot.completedBytes, LocalConfiguration.current.locales[0]))
        else -> stringResource(R.string.settings_downloads_hint)
    }
}

/**
 * The card is the same raised colour Material paints a track by default, so the track vanished
 * and a download read as a lone sliver of lime and a dot. The groove is the page colour instead,
 * the same measured bar Now playing uses.
 */
@Composable
private fun DownloadProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.background,
        drawStopIndicator = {},
        gapSize = 0.dp,
        modifier = modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
    )
}

/**
 * Text and its button share a line only while the text keeps a readable width. The pause button
 * used to be measured first and leave the status a column a few letters wide («La / st / er»);
 * now a long label, or font scale 2.0, moves the button under the text, aligned to the end.
 */
@Composable
internal fun ActionRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    action: @Composable () -> Unit,
) {
    Layout(
        contents = listOf({ Box { content() } }, { Box { action() } }),
        modifier = modifier.fillMaxWidth(),
    ) { (contentMeasurables, actionMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val body = contentMeasurables.first()
        val button = actionMeasurables.first().measure(loose)
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
            else body.maxIntrinsicWidth(Constraints.Infinity) + button.width + ActionGap.roundToPx()
        val beside = width - button.width - ActionGap.roundToPx()
        val readable = maxOf((width * .45f).roundToInt(), body.minIntrinsicWidth(Constraints.Infinity))
        if (beside >= readable) {
            val text = body.measure(loose.copy(maxWidth = beside))
            val height = maxOf(text.height, button.height)
            layout(width, height) {
                text.placeRelative(0, (height - text.height) / 2)
                button.placeRelative(width - button.width, (height - button.height) / 2)
            }
        } else {
            val text = body.measure(loose.copy(maxWidth = width))
            val gap = StackGap.roundToPx()
            layout(width, text.height + gap + button.height) {
                text.placeRelative(0, 0)
                button.placeRelative(width - button.width, text.height + gap)
            }
        }
    }
}

private val ActionGap = 12.dp
private val StackGap = 10.dp

@Composable
private fun WifiPreference(wifiOnly: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.offline_wifi)
    Surface(modifier = modifier.fillMaxWidth().testTag("offline-wifi"), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(ReelLayout.ControlCorner)) {
        Row(Modifier.fillMaxWidth().semantics { contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(SpoleIcons.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(stringResource(R.string.offline_wifi), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.offline_wifi_note), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = wifiOnly, onCheckedChange = onChange, modifier = Modifier.testTag("offline-wifi-switch"))
        }
    }
}

@Composable
private fun OfflineEmptyState(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(28.dp)) {
            Icon(SpoleIcons.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp).size(32.dp))
        }
        Text(stringResource(R.string.offline_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.offline_empty_note), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OfflineDownloadRow(
    item: OfflineDownloadItem,
    modifier: Modifier = Modifier,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onPlay: () -> Unit,
) {
    val title = item.title.ifBlank { stringResource(R.string.offline_unknown_title) }
    val locale = LocalConfiguration.current.locales[0]
    val status: @Composable (String, androidx.compose.ui.graphics.Color) -> Unit = { text, color ->
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(modifier = modifier.fillMaxWidth().testTag("offline-item-${item.id}"), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(ReelLayout.ControlCorner)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(10.dp)) {
                    Icon(if (item.state == OfflineDownloadState.COMPLETE) SpoleIcons.Done else SpoleIcons.Download, null,
                        tint = if (item.state == OfflineDownloadState.COMPLETE) MaterialTheme.colorScheme.primary else muted,
                        modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val detail = listOfNotNull(
                        item.subtitle.takeIf(String::isNotBlank),
                        item.service?.displayName,
                    ).joinToString(" · ")
                    // Two lines and an ellipsis: an episode title used to stop mid-sentence with no
                    // sign that anything was missing, and it took the service name with it.
                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).testTag("offline-remove-${item.id}")) {
                    Icon(SpoleIcons.Delete, stringResource(R.string.offline_remove, title), tint = muted)
                }
            }
            when (item.state) {
                // A finished file needs no full bar: the tick already says it is complete.
                OfflineDownloadState.COMPLETE -> ActionRow(content = {
                    status(stringResource(R.string.offline_complete, formatOfflineBytes(item.bytesDownloaded, locale)), muted)
                }) {
                    Button(onClick = onPlay, modifier = Modifier.testTag("offline-play-${item.id}")) {
                        Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_watch))
                    }
                }
                OfflineDownloadState.DOWNLOADING, OfflineDownloadState.QUEUED -> {
                    DownloadProgress(item.progress, Modifier.testTag("offline-progress-${item.id}"))
                    ActionRow(content = { status(offlineProgressText(item), muted) }) {
                        SpoleSecondaryButton(onClick = onPause, modifier = Modifier.testTag("offline-pause-${item.id}")) {
                            Icon(SpoleIcons.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_pause_one))
                        }
                    }
                }
                OfflineDownloadState.PAUSED -> {
                    DownloadProgress(item.progress)
                    ActionRow(content = { status(stringResource(R.string.offline_paused), muted) }) {
                        SpoleSecondaryButton(onClick = onResume, modifier = Modifier.testTag("offline-resume-${item.id}")) {
                            Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_resume_one))
                        }
                    }
                }
                OfflineDownloadState.FAILED -> ActionRow(content = {
                    status(stringResource(R.string.offline_failed), MaterialTheme.colorScheme.error)
                }) {
                    SpoleSecondaryButton(onClick = onRetry, modifier = Modifier.testTag("offline-retry-${item.id}")) {
                        Icon(SpoleIcons.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.action_retry))
                    }
                }
                OfflineDownloadState.REMOVING -> status(stringResource(R.string.offline_removing), muted)
            }
        }
    }
}

@Composable
private fun offlineProgressText(item: OfflineDownloadItem): String {
    val locale = LocalConfiguration.current.locales[0]
    return when {
        item.state == OfflineDownloadState.QUEUED -> stringResource(R.string.offline_queued_status)
        item.contentLength > 0 -> stringResource(R.string.offline_item_progress, (item.progress * 100).roundToInt(),
            formatOfflineBytes(item.bytesDownloaded, locale), formatOfflineBytes(item.contentLength, locale))
        else -> stringResource(R.string.offline_item_progress_unknown, (item.progress * 100).roundToInt())
    }
}

/** Sizes follow the app language: «53,6 MB» in nynorsk and bokmål, «53.6 MB» in English. */
internal fun formatOfflineBytes(bytes: Long, locale: java.util.Locale = java.util.Locale.ROOT): String {
    val value = bytes.coerceAtLeast(0)
    return when {
        value < 1_024L -> "$value B"
        value < 1_024L * 1_024L -> "${value / 1_024L} KB"
        value < 1_024L * 1_024L * 1_024L -> "${"%.1f".format(locale, value / (1_024f * 1_024f))} MB"
        else -> "${"%.1f".format(locale, value / (1_024f * 1_024f * 1_024f))} GB"
    }
}
