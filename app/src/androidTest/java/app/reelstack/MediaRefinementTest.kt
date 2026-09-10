package app.reelstack

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.screens.LibraryScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MediaRefinementTest {
    @get:Rule val rule = createComposeRule()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", userId = "me")

    @Test fun televisionFocusNeverTintsTheCaption() {
        rule.setContent { ReelstackTheme {
            val tv = Configuration(LocalConfiguration.current).apply { uiMode = Configuration.UI_MODE_TYPE_TELEVISION }
            CompositionLocalProvider(LocalConfiguration provides tv) {
                val input = LocalInputModeManager.current
                SideEffect { input.requestInputMode(InputMode.Keyboard) }
                LibraryScreen(ReelstackUiState(connections = listOf(connection), libraryPath = listOf("root" to "Filmar"),
                    libraryEntries = listOf(RemoteLibraryItem("film", "Caption", "2026", null, "Movie", "film"))), {}, {}, {})
            }
        } }
        val caption = rule.onNodeWithText("Caption", useUnmergedTree = true)
        val before = caption.captureToImage().toPixelMap()
        rule.onNodeWithTag("library-item-film").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithTag("library-item-film").assertIsFocused()
        rule.waitForIdle()
        val after = caption.captureToImage().toPixelMap()
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        for (y in 0 until before.height) for (x in 0 until before.width) assertEquals(before[x, y], after[x, y])
    }

    @Test fun phoneRailReachesWindowEdgeAndLastCardScrollsFullyIntoView() {
        verifyPhoneRail(1f)
    }

    @Test fun phoneRailRemainsScrollableAtDoubleFontScale() {
        verifyPhoneRail(2f)
    }

    private fun verifyPhoneRail(fontScale: Float) {
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))) {
          DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
            ReelstackTheme { HomeScreen(ReelstackUiState(connections = listOf(connection),
                homeSections = setOf(HomeSection.JELLYFIN_SERIES), sessions = emptyList(), resume = emptyList(),
                recentSeries = (1..3).map { LibraryMedia("item-$it", "Episode $it", "S01 E01", artworkRes = R.drawable.media_placeholder,
                    source = ServiceKind.JELLYFIN, mediaType = "Episode") }), PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) }
          }
        } }
        rule.onNodeWithTag("library-rail").performScrollTo()
        val rail = rule.onNodeWithTag("library-rail").fetchSemanticsNode().boundsInRoot
        val page = rule.onNodeWithTag("home-feed").fetchSemanticsNode().boundsInRoot
        assertEquals(page.right, rail.right, 1f)
        rule.onNodeWithTag("library-rail").performScrollToIndex(2)
        rule.onNodeWithText("Episode 3").assertIsDisplayed()
        val art = rule.onNodeWithTag("library-artwork-item-3", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(art.left >= page.left && art.right <= page.right)
    }

    @Test fun failedResumeFeedHasExplanationInsteadOfDisappearing() {
        rule.setContent { ReelstackTheme { HomeScreen(ReelstackUiState(connections = listOf(connection),
            homeSections = setOf(HomeSection.CONTINUE_WATCHING), sessions = emptyList(), resume = emptyList(),
            serviceWarnings = mapOf(ServiceKind.JELLYFIN to "Transient failure")),
            PaddingValues(0.dp), {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("Fekk ikkje lasta heile biblioteket. Prøver automatisk igjen.").assertIsDisplayed()
    }
}
