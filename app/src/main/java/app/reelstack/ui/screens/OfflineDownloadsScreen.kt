package app.reelstack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
) {
    var deleteCandidate by remember { mutableStateOf<OfflineDownloadItem?>(null) }
    ReelPage {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).testTag("offline-downloads"),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("heading") {
                Column(Modifier.padding(start = ReelLayout.Gutter, end = ReelLayout.Gutter, top = ReelLayout.PageTop)) {
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
    val activeItems = snapshot.items.filter { it.state in setOf(OfflineDownloadState.DOWNLOADING, OfflineDownloadState.QUEUED) }
    val pausedItems = snapshot.items.count { it.state == OfflineDownloadState.PAUSED }
    val progress = if (snapshot.totalBytes > 0) (snapshot.completedBytes.toFloat() / snapshot.totalBytes).coerceIn(0f, 1f)
        else activeItems.map { it.progress }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
    Surface(modifier = modifier.fillMaxWidth().testTag("offline-summary"), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(ReelLayout.ControlCorner)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            snapshot.items.isNotEmpty() -> stringResource(R.string.offline_summary_size, formatOfflineBytes(snapshot.completedBytes))
                            else -> stringResource(R.string.offline_summary_empty)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                when {
                    activeItems.isNotEmpty() -> SpoleSecondaryButton(onClick = onPauseAll, modifier = Modifier.testTag("offline-pause-all")) {
                        Icon(SpoleIcons.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_pause))
                    }
                    pausedItems > 0 -> SpoleSecondaryButton(onClick = onResumeAll, modifier = Modifier.testTag("offline-resume-all")) {
                        Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_resume))
                    }
                }
            }
            if (activeItems.isNotEmpty()) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().testTag("offline-summary-progress"))
            }
        }
    }
}

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
    Surface(modifier = modifier.fillMaxWidth().testTag("offline-item-${item.id}"), color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(ReelLayout.ControlCorner)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, shape = RoundedCornerShape(10.dp)) {
                    Icon(if (item.state == OfflineDownloadState.COMPLETE) SpoleIcons.Done else SpoleIcons.Download, null,
                        tint = if (item.state == OfflineDownloadState.COMPLETE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    val detail = listOfNotNull(
                        item.subtitle.takeIf(String::isNotBlank),
                        item.service?.displayName,
                    ).joinToString(" · ")
                    if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp).testTag("offline-remove-${item.id}")) {
                    Icon(SpoleIcons.Delete, stringResource(R.string.offline_remove, title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            when (item.state) {
                OfflineDownloadState.COMPLETE -> {
                    LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.offline_complete, formatOfflineBytes(item.bytesDownloaded)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Button(onClick = onPlay, modifier = Modifier.testTag("offline-play-${item.id}")) {
                            Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_watch))
                        }
                    }
                }
                OfflineDownloadState.DOWNLOADING, OfflineDownloadState.QUEUED -> {
                    LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(offlineProgressText(item), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        SpoleSecondaryButton(onClick = onPause, modifier = Modifier.testTag("offline-pause-${item.id}")) {
                            Icon(SpoleIcons.Pause, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_pause_one))
                        }
                    }
                }
                OfflineDownloadState.PAUSED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.offline_paused), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    SpoleSecondaryButton(onClick = onResume, modifier = Modifier.testTag("offline-resume-${item.id}")) {
                        Icon(SpoleIcons.PlaySimple, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.offline_resume_one))
                    }
                }
                OfflineDownloadState.FAILED -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.offline_failed), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    SpoleSecondaryButton(onClick = onRetry, modifier = Modifier.testTag("offline-retry-${item.id}")) {
                        Icon(SpoleIcons.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.action_retry))
                    }
                }
                OfflineDownloadState.REMOVING -> Text(stringResource(R.string.offline_removing), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun offlineProgressText(item: OfflineDownloadItem): String = when {
    item.state == OfflineDownloadState.QUEUED -> stringResource(R.string.offline_queued_status)
    item.contentLength > 0 -> stringResource(R.string.offline_item_progress, (item.progress * 100).roundToInt(),
        formatOfflineBytes(item.bytesDownloaded), formatOfflineBytes(item.contentLength))
    else -> stringResource(R.string.offline_item_progress_unknown, (item.progress * 100).roundToInt())
}

internal fun formatOfflineBytes(bytes: Long): String {
    val value = bytes.coerceAtLeast(0)
    return when {
        value < 1_024L -> "$value B"
        value < 1_024L * 1_024L -> "${value / 1_024L} KB"
        value < 1_024L * 1_024L * 1_024L -> "${(value / (1_024f * 1_024f)).let { "%.1f".format(java.util.Locale.ROOT, it) }} MB"
        else -> "${(value / (1_024f * 1_024f * 1_024f)).let { "%.1f".format(java.util.Locale.ROOT, it) }} GB"
    }
}
