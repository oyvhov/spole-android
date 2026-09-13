package app.reelstack

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

    @Test fun carouselCrossfadesWithoutMovingActionAndOpensDisplayedTitle() {
        var opened: String? = null
        val next = media.copy(id = "second", title = "Second series", overview = "Short description")
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme { TabletLibraryFeature(media, { opened = it }, candidates = listOf(media, next)) } }
        rule.mainClock.advanceTimeBy(1_000)
        val before = rule.onNodeWithTag("tablet-feature-open").fetchSemanticsNode().boundsInRoot
        rule.mainClock.advanceTimeBy(9_000)
        rule.onNodeWithText(next.title).assertIsDisplayed()
        val after = rule.onNodeWithTag("tablet-feature-open").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        rule.onNodeWithText("Jellyfin").assertIsDisplayed()
        rule.onNodeWithText("Frå Jellyfin-biblioteket ditt").assertDoesNotExist()
        rule.onNodeWithTag("tablet-feature-open").performClick()
        assertEquals(next.id, opened)
    }

    @Test fun carouselPausesWhileRemoteFocusIsOnTheAction() {
        rule.mainClock.autoAdvance = false
        rule.setContent { ReelstackTheme {
            val input = androidx.compose.ui.platform.LocalInputModeManager.current
            androidx.compose.runtime.SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
            TabletLibraryFeature(media, {}, candidates = listOf(media, media.copy(id = "second", title = "Second series")))
        } }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("tablet-feature-open").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
        rule.mainClock.advanceTimeBy(20_000)
        rule.onNodeWithText(media.title).assertIsDisplayed()
        rule.onNodeWithText("Second series").assertDoesNotExist()
    }

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
    @Test fun accountOverlaysArtworkWithoutAddingAHeaderOrOpeningTitle() {
        var accountOpened = false
        var titleOpened = false
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(760.dp, 540.dp))) {
                ReelstackTheme { TabletLibraryFeature(media, { titleOpened = true }, account = {
                    Button({ accountOpened = true }, Modifier.size(48.dp).testTag("profile")) { Text("P") }
                }) }
            }
        }
        val feature = rule.onNodeWithTag("tablet-library-feature").fetchSemanticsNode().boundsInRoot
        val overlay = rule.onNodeWithTag("feature-account-overlay", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val title = rule.onNodeWithText(media.title).fetchSemanticsNode().boundsInRoot
        assertTrue(overlay.top >= feature.top && overlay.bottom < feature.bottom)
        assertTrue(overlay.left >= title.left)
        rule.onNodeWithTag("profile").assertIsDisplayed().performClick()
        assertTrue(accountOpened)
        assertFalse(titleOpened)
    }
}
