package app.reelstack

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.AppSheet
import app.reelstack.ui.ReelstackSheets
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Phone-only contract for the detail-page entry point to the private offline queue. */
class OfflineDetailDownloadUiTest {
    @get:Rule val rule = createComposeRule()

    private val connection = ServiceConnection(
        kind = ServiceKind.JELLYFIN,
        name = "Jellyfin",
        baseUrl = "https://media.example.test",
        token = "test-token",
    )
    private val movie = LibraryMedia(
        id = "movie-details",
        remoteId = "server-movie-id",
        title = "Film",
        subtitle = "2026",
        artworkRes = R.drawable.media_placeholder,
        source = ServiceKind.JELLYFIN,
        mediaType = "Movie",
    )

    @Test fun detail_download_dispatches_the_server_file_and_its_origin_sheet() {
        val received = mutableStateOf<Pair<String, String>?>(null)
        rule.setContent {
            Phone {
                ReelstackSheets(
                    state = movieState(),
                    connectionDraft = null,
                    onDismiss = {}, onPlaybackToggle = {},
                    onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                    onConnectionUserIdChange = {}, onConnectionAuthModeChange = {},
                    onConnectionUsernameChange = {}, onConnectionPasswordChange = {},
                    onTestAndSaveConnection = {}, onRemoveConnection = {}, onAddMedia = {}, onUpcomingClick = {},
                    onOfflineDownload = { media, _, _, _, origin -> received.value = media.remoteId.orEmpty() to origin },
                )
            }
        }

        rule.onNodeWithTag("detail-download").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals("server-movie-id" to "movie-details", received.value) }
    }

    @Test fun preparing_a_download_is_visible_and_cannot_be_submitted_twice() {
        rule.setContent {
            Phone {
                ReelstackSheets(
                    state = movieState(preparing = true),
                    connectionDraft = null,
                    onDismiss = {}, onPlaybackToggle = {},
                    onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                    onConnectionUserIdChange = {}, onConnectionAuthModeChange = {},
                    onConnectionUsernameChange = {}, onConnectionPasswordChange = {},
                    onTestAndSaveConnection = {}, onRemoveConnection = {}, onAddMedia = {}, onUpcomingClick = {},
                )
            }
        }

        rule.onNodeWithTag("detail-download").assertIsNotEnabled()
        rule.onNodeWithTag("detail-download-progress").assertIsDisplayed()
        rule.onNodeWithTag("detail-download-status").assertIsDisplayed()
    }

    @Test fun series_download_explains_when_episodes_are_still_loading() {
        val series = movie.copy(id = "series-details", remoteId = "server-series-id", mediaType = "Series")
        val state = ReelstackUiState(
            connections = listOf(connection),
            activeSheet = AppSheet.TitleDetails(series.id),
            libraryDetailMedia = series,
            contentDetails = ContentDetails(
                key = series.id,
                remoteId = series.remoteId,
                title = series.title,
                eyebrow = "Jellyfin",
                subtitle = "Serie",
                artworkRes = R.drawable.media_placeholder,
                source = ServiceKind.JELLYFIN,
                mediaType = "Series",
                libraryAvailable = true,
            ),
            seriesBrowse = SeriesBrowse(openedFor = series.id, loading = true),
        )
        rule.setContent {
            Phone {
                ReelstackSheets(
                    state = state,
                    connectionDraft = null,
                    onDismiss = {}, onPlaybackToggle = {},
                    onConnectionNameChange = {}, onConnectionUrlChange = {}, onConnectionTokenChange = {},
                    onConnectionUserIdChange = {}, onConnectionAuthModeChange = {},
                    onConnectionUsernameChange = {}, onConnectionPasswordChange = {},
                    onTestAndSaveConnection = {}, onRemoveConnection = {}, onAddMedia = {}, onUpcomingClick = {},
                )
            }
        }

        rule.onNodeWithTag("detail-download").performClick()
        rule.onNodeWithTag("detail-download-status").assertIsDisplayed()
    }

    private fun movieState(preparing: Boolean = false) = ReelstackUiState(
        connections = listOf(connection),
        activeSheet = AppSheet.TitleDetails(movie.id),
        libraryDetailMedia = movie,
        contentDetails = ContentDetails(
            key = movie.id,
            remoteId = movie.remoteId,
            title = movie.title,
            eyebrow = "Jellyfin",
            subtitle = movie.subtitle,
            artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN,
            mediaType = "Movie",
            libraryAvailable = true,
        ),
        offlinePreparingDetailKeys = if (preparing) setOf(movie.id) else emptySet(),
    )

    @androidx.compose.runtime.Composable
    private fun Phone(content: @androidx.compose.runtime.Composable () -> Unit) {
        val phoneConfiguration = Configuration().apply { uiMode = Configuration.UI_MODE_TYPE_NORMAL }
        CompositionLocalProvider(LocalConfiguration provides phoneConfiguration) {
            ReelstackTheme(content = content)
        }
    }
}
