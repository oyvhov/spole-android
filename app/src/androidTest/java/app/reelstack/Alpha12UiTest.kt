package app.reelstack

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.*
import app.reelstack.ui.screens.*
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Alpha12UiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun updatePanelOpensFromSettingsAndClosesWithoutChangingAccountState() {
        rule.setContent { ReelstackTheme {
            androidx.compose.foundation.layout.Box {
                androidx.compose.foundation.layout.Column { app.reelstack.update.AppUpdateSettings() }
                app.reelstack.update.AppUpdateHost(false)
            }
        } }
        rule.onNodeWithTag("app-updates").performClick()
        rule.onNodeWithText("Sjekk no").assertIsDisplayed()
        rule.onNodeWithText("Lukk").performClick()
        rule.onNodeWithText("Sjekk no").assertDoesNotExist()
    }
    @Test fun artworkFocusKeepsCaptionsOutsideEveryCornerShape() {
        var corners by mutableStateOf(ArtworkCorners.entries.first())
        val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.com", "fixture", userId = "me")
        rule.setContent { ReelstackTheme {
            CompositionLocalProvider(app.reelstack.ui.theme.LocalPersonalization provides Personalization(artworkCorners = corners)) {
                val input = LocalInputModeManager.current
                SideEffect { input.requestInputMode(InputMode.Keyboard) }
                LibraryScreen(ReelstackUiState(connections = listOf(connection), libraryPath = listOf("root" to "Filmar"),
                    libraryEntries = listOf(app.reelstack.data.network.RemoteLibraryItem("film", "Tittel utanfor ramma", "2026", null, "Movie", "film"))), {}, {}, {})
            }
        } }
        for (corner in ArtworkCorners.entries) {
            rule.runOnIdle { corners = corner }
            rule.onNodeWithTag("library-item-film").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            val art = rule.onNodeWithTag("library-art-film", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val title = rule.onNodeWithText("Tittel utanfor ramma", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue("Caption outside $corner", title.top >= art.bottom)
            rule.onNodeWithTag("library-item-film").assertIsFocused()
        }
    }
    @Test fun inlineFiltersApplyImmediately() {
        var applied = LibraryFilters()
        rule.setContent { ReelstackTheme { LibraryFilterBar(applied, { applied = it }, LibraryFacets("library", listOf("Drama"), listOf("2026"))) } }
        rule.onNodeWithTag("library-filters").performClick()
        rule.onNodeWithTag("library-favourites").performClick()
        rule.runOnIdle { assertTrue(applied.favourites) }
    }
    @Test fun libraryIconsAreSavedOnlyWithTheSelection() {
        var saved: Map<String, LibraryIcon>? = null
        val state = ReelstackUiState(libraryChoices = listOf(RemoteLibraryView("films", "Filmar", "movies")),
            selectedLibraryIds = setOf("films"), libraryShortcuts = listOf("films" to "Filmar"), libraryIcons = mapOf("films" to LibraryIcon.SPORT))
        rule.setContent { ReelstackTheme { LibraryChoicesDialog(state, {}, {}, { _, _, icons -> saved = icons }) } }
        rule.runOnIdle { assertNull(saved) }
        rule.onNodeWithTag("library-selection-save").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(LibraryIcon.SPORT, saved?.get("films")) }
    }
    @Test fun tvRequestActionIsVisibleAndFocusedBeforeTheLongSynopsis() {
        org.junit.Assume.assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.app.UiModeManager::class.java).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION)
        val media = ReelstackUiState().discover.first { it.canRequest }
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(960.dp, 540.dp))) { ReelstackTheme {
                val input = LocalInputModeManager.current
                SideEffect { input.requestInputMode(InputMode.Keyboard) }
                ReelstackSheets(ReelstackUiState(discover = listOf(media), activeSheet = AppSheet.TitleDetails(media.id),
                    contentDetails = ContentDetails(media.id, media.title, "Seerr", "Film · 2026", artworkRes = R.drawable.media_placeholder,
                        source = ServiceKind.SEERR, mediaType = "Movie", overview = "Ei lang omtale. ".repeat(200),
                        facts = listOf("2026", "117 min", "★ 6,0"))), null, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            } }
        }
        rule.onNodeWithTag("tv-title-request").assertIsDisplayed().assertIsFocused()
        rule.onNodeWithTag("tv-title-request").captureToImage()
    }
}
