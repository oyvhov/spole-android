package app.reelstack

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.*
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TabletLayoutFollowupTest {
    @get:Rule val rule = createComposeRule()
    @Test fun tabletHomeOmitsSearchAndCompactWindowRestoresIt() {
        var width by mutableStateOf(1280.dp)
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width, 800.dp))) {
                ReelstackTheme { HomeScreen(ReelstackUiState(homeSections = emptySet()), PaddingValues(0.dp),
                    {}, {}, {}, {}, {}, onRefresh = {},
                    showSearch = app.reelstack.ui.layout.WindowLayoutPolicy(width.value, 800f).showHomeSearch) }
            }
        }
        rule.onNodeWithTag("home-search").assertDoesNotExist()
        rule.runOnIdle { width = 800.dp }
        rule.onNodeWithTag("home-search").assertDoesNotExist()
        rule.runOnIdle { width = 412.dp }
        rule.onNodeWithTag("home-search").assertIsDisplayed()
    }
    @Test fun wideEpisodePlacesSubtitleBesideArtAtDoubleFontSize() {
        var density = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(672.dp, 1000.dp))) {
                density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                    ReelstackTheme { CinematicTitleHero("A long series title", "Jellyfin", "S03 E02 · Episode title",
                        null, R.drawable.media_placeholder, ServiceKind.JELLYFIN, false) }
                }
            }
        }
        val art = rule.onNodeWithTag("episode-detail-artwork").fetchSemanticsNode().boundsInRoot
        val subtitle = rule.onNodeWithText("S03 E02 · Episode title").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(304f, art.width / density, 1f)
        assertTrue(subtitle.left > art.right)
        rule.onNodeWithTag("tablet-episode-summary").assertIsDisplayed()
    }
}
