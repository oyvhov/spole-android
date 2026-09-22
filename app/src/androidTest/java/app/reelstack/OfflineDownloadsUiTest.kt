package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.ServiceKind
import app.reelstack.offline.OfflineDownloadItem
import app.reelstack.offline.OfflineDownloadState
import app.reelstack.offline.OfflineDownloadsSnapshot
import app.reelstack.ui.screens.OfflineDownloadsScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OfflineDownloadsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun completed_file_is_playable_and_removal_needs_confirmation() {
        val removed = mutableStateOf<String?>(null)
        val snapshot = OfflineDownloadsSnapshot(
            items = listOf(
                OfflineDownloadItem(
                    id = "offline-safe-id",
                    title = "Episode 1",
                    subtitle = "Sesong 1",
                    service = ServiceKind.JELLYFIN,
                    mediaType = "Episode",
                    state = OfflineDownloadState.COMPLETE,
                    bytesDownloaded = 1_024L,
                    contentLength = 1_024L,
                    progress = 1f,
                ),
            ),
            totalBytes = 1_024L,
            completedBytes = 1_024L,
        )
        rule.setContent {
            ReelstackTheme {
                OfflineDownloadsScreen(
                    snapshot = snapshot,
                    wifiOnly = true,
                    contentPadding = PaddingValues(0.dp),
                    onWifiOnlyChange = {},
                    onPauseAll = {},
                    onResumeAll = {},
                    onPause = {},
                    onResume = {},
                    onRetry = {},
                    onRemove = { removed.value = it },
                    onPlay = {},
                )
            }
        }

        rule.onNodeWithTag("offline-downloads").assertIsDisplayed()
        rule.onNodeWithTag("offline-play-offline-safe-id").assertIsDisplayed()
        rule.onNodeWithTag("offline-remove-offline-safe-id").performClick()
        rule.onNodeWithTag("offline-remove-confirm").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("offline-safe-id", removed.value) }
    }
}
