package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.*
import app.reelstack.data.model.*
import app.reelstack.ui.components.TabletLibraryFeature
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TabletFeatureTest {
    @get:Rule val rule = createComposeRule()
    private val media = LibraryMedia("fixture-episode", "A long series title", "S01 E02 · An episode",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
        overview = "A detailed description. ".repeat(30))

    @Test fun featureOpensExactLibraryItemWithoutPlayback() {
        var opened: String? = null
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1100.dp, 800.dp))) {
                ReelstackTheme { TabletLibraryFeature(media, { opened = it }) }
            }
        }
        rule.onNodeWithText(media.title).assertIsDisplayed()
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed().performClick()
        assertEquals(media.id, opened)
    }
    @Test fun largeTextGrowsTheFeatureAndKeepsActionVisible() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(1000.dp, 1200.dp))) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    ReelstackTheme { TabletLibraryFeature(media, {}) }
                }
            }
        }
        rule.onNodeWithTag("tablet-feature-open").assertIsDisplayed()
        rule.onNodeWithText(media.title).assertIsDisplayed()
    }
}
