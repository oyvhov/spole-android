package app.reelstack.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * «Sider og meny»: one category for Home, the library page and the menu, and one editor per page.
 * The library editor replaced «Vel bibliotek», the old tile dialog and the overview switch.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xxhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class SettingsPagesTest {
    @get:Rule val rule = createComposeRule()

    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token", "me")
    private val views = listOf(RemoteLibraryView("films", "Filmar", "movies"), RemoteLibraryView("series", "Seriar", "tvshows"),
        RemoteLibraryView("kids", "Barn", "movies"))

    private class Saved(val ids: Set<String>, val menu: List<String>, val icons: Map<String, LibraryIcon>)
    private var options by mutableStateOf(Personalization())
    private var saved: Saved? = null
    private var dismissed = false

    private fun libraryState(shortcuts: List<String> = emptyList()) = ReelstackUiState(connections = listOf(jellyfin),
        libraryChoicesOpen = true, libraryChoices = views, selectedLibraryIds = views.map { it.id }.toSet(),
        libraryShortcuts = shortcuts.map { id -> id to views.first { it.id == id }.name })

    private fun openEditor(state: ReelstackUiState, initial: Personalization = Personalization(), fontScale: Float = 1f) {
        options = initial
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                MaterialTheme {
                    LibraryEditorDialog(state, onRetry = {}, onDismiss = { dismissed = true },
                        onSave = { ids, menu, icons -> saved = Saved(ids, menu, icons) },
                        options = options, onChange = { options = it })
                }
            }
        }
    }

    private fun node(tag: String) =
        rule.onNodeWithTag("library-editor-list").performScrollToNode(hasTestTag(tag)).let { rule.onNodeWithTag(tag) }

    private fun done() = rule.onNodeWithTag("library-editor-done").performClick()

    @Test fun pagesAndMenuAreOneCategoryWithOneEntryPerPage() {
        assertFalse("MENU" in SettingsCategory.entries.map { it.name })
        var libraryEditor = false
        rule.setContent {
            MaterialTheme {
                MobileSettingsScreen(ReelstackUiState(connections = listOf(jellyfin)), PaddingValues(0.dp),
                    onConnectionClick = {}, onNotificationsChange = {}, onWifiOnlyChange = {},
                    onHomeSectionChange = { _, _ -> }, onAccountClick = {}, onManageLibraries = { libraryEditor = true })
            }
        }
        rule.onNodeWithTag("settings-category-HOME").performClick()
        rule.onNodeWithTag("home-layout-open").assertIsDisplayed()
        rule.onNodeWithTag("library-customize").assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(libraryEditor) }
        rule.onNodeWithTag("theme-choice-start-page").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("menu-order-card-HOME").performScrollTo().assertIsDisplayed()
        // The duplicate library entry and the seven switches that are now fixed behaviour.
        listOf("library-manage", "library-hub", "show-ratings", "show-quality", "hero-rotate", "hero-logo",
            "hero-compact", "show-upcoming-episodes").forEach { rule.onNodeWithTag(it).assertDoesNotExist() }
    }

    @Test fun hidingALibraryIsSavedOnceWhenTheEditorCloses() {
        openEditor(libraryState())
        node("library-visible-kids").assertIsOn().performClick().assertIsOff()
        rule.runOnIdle { assertNull(saved) }
        done()
        rule.runOnIdle {
            assertEquals(setOf("films", "series"), saved?.ids)
            assertFalse(dismissed)
        }
    }

    @Test fun closingWithoutChangesDoesNotReloadTheLibrary() {
        openEditor(libraryState(shortcuts = listOf("series", "films")))
        done()
        rule.runOnIdle { assertNull(saved); assertTrue(dismissed) }
    }

    @Test fun aLibraryHiddenOnlyFromTheOldTilesBecomesUnselected() {
        openEditor(libraryState(), Personalization(libraryHidden = setOf("kids")))
        node("library-visible-kids").assertIsOff()
        done()
        rule.runOnIdle {
            assertEquals(setOf("films", "series"), saved?.ids)
            assertEquals(emptySet<String>(), options.libraryHidden)
        }
    }

    @Test fun rowsAndLookAreSavedAtOnceAndReloadNothing() {
        openEditor(libraryState())
        node("hub-visible-FAVOURITES").performClick()
        node("hub-down-FEATURE").performClick()
        node("library-title").performClick()
        rule.runOnIdle {
            assertTrue("FAVOURITES" in options.libraryHubHidden)
            assertEquals("LIBRARY_NEXT", options.libraryHubOrder.first())
            assertTrue(options.showLibraryTitle)
        }
        done()
        rule.runOnIdle { assertNull(saved); assertTrue(dismissed) }
    }

    @Test fun phonesDoNotOfferMenuShortcutsTheyCannotShow() {
        openEditor(libraryState())
        node("library-row-films").assertIsDisplayed()
        rule.onNodeWithTag("library-pin-films").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun movingALibraryAlsoOrdersItsMenuShortcut() {
        openEditor(libraryState(shortcuts = listOf("films", "series")))
        node("library-down-films").performClick()
        rule.runOnIdle { assertEquals(listOf("series", "films", "kids"), options.libraryOrder) }
        node("library-pin-kids").performClick()
        done()
        rule.runOnIdle { assertEquals(listOf("series", "films", "kids"), saved?.menu) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun aNewShortcutJoinsAnArrangedMenuAtTheEnd() {
        openEditor(libraryState(shortcuts = listOf("series", "films")))
        node("library-pin-kids").performClick()
        done()
        rule.runOnIdle { assertEquals(listOf("series", "films", "kids"), saved?.menu) }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun aShortcutSaysWhetherItIsInTheMenu() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val inMenu = context.getString(app.reelstack.R.string.library_editor_in_menu)
        val offer = context.getString(app.reelstack.R.string.library_editor_add_to_menu)
        openEditor(libraryState(shortcuts = listOf("films")))
        node("library-pin-films").assertTextEquals(inMenu)
        node("library-pin-series").assertTextEquals(offer).performClick()
        node("library-pin-series").assertTextEquals(inMenu)
    }

    @Test fun withoutAServerTheEditorStillArrangesRowsAndLook() {
        openEditor(ReelstackUiState(libraryChoicesOpen = true))
        rule.onNodeWithTag("library-row-films").assertDoesNotExist()
        node("hub-row-FEATURE").assertIsDisplayed()
        done()
        rule.runOnIdle { assertNull(saved); assertTrue(dismissed) }
    }

    @Test fun doubleTextKeepsTheLibraryControlsInsideTheEditor() {
        openEditor(libraryState(), fontScale = 2f)
        val dialog = rule.onNodeWithTag("library-customize-dialog").fetchSemanticsNode().boundsInRoot
        listOf("library-visible-films", "library-down-films").forEach { tag ->
            val bounds = node(tag).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("$tag $bounds outside $dialog", bounds.left >= dialog.left - 1f && bounds.right <= dialog.right + 1f)
        }
        rule.onNodeWithTag("library-editor-done").assertIsDisplayed()
    }

    @Test fun homeOffersTheSearchLineOnAPhoneAndNoFeature() {
        rule.setContent { MaterialTheme { HomeLayoutSetting(ReelstackUiState(connections = listOf(jellyfin)), HomeEditorActions()) } }
        rule.onNodeWithTag("home-layout-open").performClick()
        rule.onNodeWithTag("home-layout-list").performScrollToNode(hasTestTag("home-search-bar"))
        rule.onNodeWithTag("home-search-bar").assertIsDisplayed()
        rule.onNodeWithTag("home-layout-feature").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun homeOffersItsFeatureFirstOnATablet() {
        rule.setContent { MaterialTheme { HomeLayoutSetting(ReelstackUiState(connections = listOf(jellyfin)), HomeEditorActions()) } }
        rule.onNodeWithTag("home-layout-open").performClick()
        rule.onNodeWithTag("home-layout-feature").assertIsDisplayed()
        rule.onNodeWithTag("home-layout-list").performScrollToNode(hasTestTag("home-layout-combine"))
        rule.onNodeWithTag("home-search-bar").assertDoesNotExist()
    }
}
