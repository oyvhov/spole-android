package app.reelstack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.RequestHistoryState
import app.reelstack.ui.TrackedRequestCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RequestHistoryScreen(state: RequestHistoryState, onDetails: (String) -> Unit,
    onBack: () -> Unit, onLoad: (Boolean) -> Unit) {
    BackHandler(onBack = onBack)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    LazyVerticalGrid(columns = GridCells.Adaptive(if (LocalDensity.current.fontScale >= 1.6f) 280.dp else 160.dp),
        contentPadding = PaddingValues(24.dp), horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize().testTag("request-history")) {
        item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("history-back")) { Text(stringResource(R.string.history_back)) }
                Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.history_description), style = MaterialTheme.typography.bodyMedium)
                Text(if (state.total != null) pluralStringResource(R.plurals.history_count_total, state.total, state.items.size, state.total)
                    else pluralStringResource(R.plurals.history_count, state.items.size, state.items.size), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onLoad(false) }, enabled = !state.loading,
                    modifier = Modifier.testTag("history-refresh")) { Text(stringResource(R.string.history_refresh)) }
            }
        }
        items(state.items, key = { it.key }) { item ->
            Column {
                TrackedRequestCard(item.copy(title = item.title.ifBlank { stringResource(R.string.history_untitled, item.requestId ?: 0) }),
                    { onDetails(item.key) }, {}, showActions = false,
                    detailsEnabled = item.mediaId > 0 && item.mediaType in setOf("tv", "movie"))
                if (item.updatedAt > 0) Text(dateFormat.format(Instant.ofEpochMilli(item.updatedAt).atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
        item(key = "paging", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("history-loading"))
                    Text(stringResource(R.string.history_loading))
                } else if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { onLoad(!state.retryFromStart && state.loaded && state.hasMore) }, modifier = Modifier.testTag("history-retry")) {
                        Text(stringResource(R.string.history_retry))
                    }
                } else if (state.loaded && state.items.isEmpty()) Text(stringResource(R.string.history_empty))
                else if (state.loaded && !state.hasMore) Text(stringResource(R.string.history_end))
                if (state.hasMore && !state.loading && state.error == null) {
                    Button(onClick = { onLoad(true) }, modifier = Modifier.testTag("history-more")) {
                        Text(stringResource(R.string.history_more))
                    }
                }
            }
        }
    }
}
