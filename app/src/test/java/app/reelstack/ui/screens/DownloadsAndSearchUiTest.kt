package app.reelstack.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.offline.OfflineDownloadItem
import app.reelstack.offline.OfflineDownloadState
import app.reelstack.offline.OfflineDownloadsSnapshot
import app.reelstack.ui.AppTab
import app.reelstack.ui.ReelstackBottomBar
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.TvMenuSettings
import app.reelstack.ui.state.GlobalSearchUiState
import app.reelstack.ui.state.SearchSlice
import app.reelstack.ui.theme.LocalPersonalization
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class DownloadsAndSearchUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val downloading = OfflineDownloadsSnapshot(
        items = listOf(
            OfflineDownloadItem(
                id = "d1",
                title = "Forræder",
                subtitle = "S06 E21 · Alle vil til himmelen, men ingen vil døy",
                service = ServiceKind.JELLYFIN,
                mediaType = "Episode",
                state = OfflineDownloadState.DOWNLOADING,
                bytesDownloaded = 56_203_674L,
                contentLength = 1_932_735_283L,
                progress = .03f,
            ),
        ),
        totalBytes = 1_932_735_283L,
        completedBytes = 56_203_674L,
        hasActiveDownloads = true,
    )

    private fun showDownloads(fontScale: Float) = rule.setContent {
        ReelstackTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                Surface {
                    OfflineDownloadsScreen(downloading, wifiOnly = false, contentPadding = PaddingValues(0.dp),
                        onWifiOnlyChange = {}, onPauseAll = {}, onResumeAll = {}, onPause = {}, onResume = {},
                        onRetry = {}, onRemove = {}, onPlay = {})
                }
            }
        }
    }

    /** The screenshot: «La / st / er / ne / d» beside a button that had taken the whole row. */
    @Test fun summaryTextKeepsAReadableWidthAtDoubleFontSize() {
        showDownloads(2f)
        val card = rule.onNodeWithTag("offline-summary").fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText(rule.activity.getString(R.string.offline_summary_active)).fetchSemanticsNode().boundsInRoot
        val pause = rule.onNodeWithTag("offline-pause-all").fetchSemanticsNode().boundsInRoot
        assertTrue("title squeezed to ${title.width} of ${card.width}", title.width > card.width * .3f)
        assertTrue("pause should sit below the text when they cannot share a line", pause.top >= title.bottom)
        assertTrue(pause.right <= card.right + 1f && pause.left >= card.left - 1f)
    }

    @Test fun itemStatusAndButtonShareTheCardWithoutSqueezing() {
        showDownloads(1f)
        val card = rule.onNodeWithTag("offline-item-d1").fetchSemanticsNode().boundsInRoot
        val pause = rule.onNodeWithTag("offline-pause-d1").fetchSemanticsNode().boundsInRoot
        val status = rule.onNodeWithText("Downloading · 3 % · 53.6 MB of 1.8 GB").fetchSemanticsNode().boundsInRoot
        assertTrue("status squeezed to ${status.width} of ${card.width}", status.width > card.width * .3f)
        assertTrue(pause.right <= card.right + 1f)
        rule.onNodeWithTag("offline-progress-d1").assertIsDisplayed()
    }

    @Test fun downloadsJoinTheBottomMenuOnlyWhenChosenAndThePreferenceIsSaved() {
        val repository = AppPreferencesRepository(rule.activity)
        var options by mutableStateOf(Personalization())
        rule.setContent {
            ReelstackTheme {
                CompositionLocalProvider(LocalPersonalization provides options) {
                    Surface {
                        Column {
                            Column(Modifier.weight(1f)
                                .verticalScroll(rememberScrollState())) {
                                TvMenuSettings(options) { options = it; repository.personalization = it }
                            }
                            ReelstackBottomBar(AppTab.HOME, onSelect = {})
                        }
                    }
                }
            }
        }
        val downloads = rule.activity.getString(R.string.nav_downloads)
        val inMenu = hasContentDescription(downloads) and hasAnyAncestor(hasTestTag("bottom-navigation"))
        rule.onNode(inMenu).assertDoesNotExist()
        rule.onNodeWithTag("menu-show-downloads").performScrollTo().performClick()
        rule.onNode(inMenu).assertExists()
        rule.runOnIdle { assertTrue(repository.personalization.showDownloadsInMenu) }
    }

    @Test fun settingsOpenDownloadsFromTheirOwnRow() {
        var opened = false
        rule.setContent {
            ReelstackTheme {
                Surface {
                    MobileSettingsScreen(ReelstackUiState(), PaddingValues(0.dp), onConnectionClick = {},
                        onNotificationsChange = {}, onWifiOnlyChange = {}, onHomeSectionChange = { _, _ -> },
                        onAccountClick = {}, onManageLibraries = {}, onOpenDownloads = { opened = true })
                }
            }
        }
        rule.onNodeWithTag("settings-downloads").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(opened) }
    }

    private val owned = LibraryMedia(id = "jf-1", title = "Silo", subtitle = "Serie", artworkRes = R.drawable.media_placeholder,
        source = ServiceKind.JELLYFIN)
    private val available = DiscoverMedia(id = "seerr-tv-1", title = "Silo", metadata = "Serie", artworkRes = R.drawable.media_placeholder,
        inLibrary = true, mediaType = "tv")
    private val newTitle = DiscoverMedia(id = "seerr-tv-2", title = "Silo Stories", metadata = "Serie",
        artworkRes = R.drawable.media_placeholder, inLibrary = false, mediaType = "tv")

    @Test fun globalSearchIsItsOwnScreenWithGroupedResults() {
        var closed = false
        var library: String? = null
        var discover: String? = null
        rule.setContent {
            ReelstackTheme {
                GlobalSearchScreen(
                    state = GlobalSearchUiState(search = SearchSlice(query = "silo", libraryResults = listOf(owned),
                        results = listOf(available, newTitle))),
                    onQuery = {}, onClose = { closed = true }, onLibraryDetails = { library = it },
                    onDiscoverDetails = { discover = it }, onRequest = {}, onLoadMore = {},
                )
            }
        }
        // Not Discover in disguise: no Oppdag heading, no suggestion grid, no filters.
        rule.onNodeWithText(rule.activity.getString(R.string.nav_discover)).assertDoesNotExist()
        rule.onNodeWithTag("discover-filters").assertDoesNotExist()
        rule.onNodeWithText(rule.activity.getString(R.string.search_libraries)).assertIsDisplayed()
        // The title you own appears once, where a tap plays it.
        rule.onNodeWithTag("discover-cover-seerr-tv-1").assertDoesNotExist()
        rule.onNodeWithTag("library-hit-jf-1").performClick()
        rule.onNodeWithTag("discover-cover-seerr-tv-2").performScrollTo().performClick()
        rule.onNodeWithTag("global-search-close").performClick()
        rule.runOnIdle {
            assertEquals("jf-1", library)
            assertEquals("seerr-tv-2", discover)
            assertTrue(closed)
        }
    }

    @Test fun anEmptySearchOffersEarlierSearches() {
        var query = ""
        rule.setContent {
            ReelstackTheme {
                GlobalSearchScreen(
                    state = GlobalSearchUiState(history = listOf("Dune")),
                    onQuery = { query = it }, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {},
                    onRequest = {}, onLoadMore = {},
                )
            }
        }
        rule.onNodeWithText(rule.activity.getString(R.string.search_recent)).assertIsDisplayed()
        rule.onNodeWithText("Dune").performClick()
        rule.runOnIdle { assertEquals("Dune", query) }
    }

    @Test fun noResultsSaysSoWithoutMentioningFilters() {
        rule.setContent {
            ReelstackTheme {
                GlobalSearchScreen(
                    state = GlobalSearchUiState(search = SearchSlice(query = "zzqx")),
                    onQuery = {}, onClose = {}, onLibraryDetails = {}, onDiscoverDetails = {}, onRequest = {}, onLoadMore = {},
                )
            }
        }
        rule.onNodeWithTag("global-search-empty").assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.search_global_no_results, "zzqx")).assertIsDisplayed()
    }
}
