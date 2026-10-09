package app.reelstack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.reelstack.data.model.CatalogueFacets
import app.reelstack.data.model.ConnectionState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.onNodeWithContentDescription
import app.reelstack.data.model.HomeLayout
import app.reelstack.data.model.HomeRowKey
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfEditor
import app.reelstack.data.model.SmartShelfKinds
import app.reelstack.data.model.SmartShelfPresets
import app.reelstack.data.model.libraryShelves
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.components.LocalSmartShelfActions
import app.reelstack.ui.components.SmartShelfActions
import app.reelstack.ui.components.SmartShelfRow
import app.reelstack.ui.screens.LibraryEditorDialog
import app.reelstack.ui.screens.LibraryHub
import app.reelstack.ui.screens.HomeScreen
import app.reelstack.ui.screens.LibraryScreen
import app.reelstack.ui.screens.SmartShelfEditorDialog
import app.reelstack.ui.theme.ReelstackTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Smart shelves: films and series apart on smaller covers, and a builder that never needs typing. */
class SmartShelvesTest {
    @get:Rule val rule = createComposeRule()

    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "t",
        userId = "u1", state = ConnectionState.CONNECTED)

    private fun item(id: String, type: String) = RemoteLibraryItem(id, "Title $id", "2024", null, type, id)

    private class Recorder : SmartShelfActions {
        val drafts = mutableListOf<SmartShelf>()
        var saved = 0
        var opened: String? = null
        var editing: Pair<String?, String?>? = null
        override fun open(id: String) { opened = id }
        override fun edit(id: String?, template: String?) { editing = id to template }
        override fun update(draft: SmartShelf) { drafts += draft }
        override fun applyTemplate(template: String) {}
        override fun save() { saved++ }
        override fun delete(id: String) {}
        override fun close() {}
        var retried = 0
        override fun retryFacets() { retried++ }
    }

    @Test fun aShelfShowsFilmsAndSeriesApart() {
        val state = ReelstackUiState(connections = listOf(connection), libraryPath = listOf(SmartShelfPresets.halloween.pathId to "Halloween"),
            libraryEntries = listOf(item("m1", "Movie"), item("s1", "Series"), item("m2", "Movie")))
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides Recorder()) {
            LibraryScreen(state, {}, {}, {})
        } } }
        rule.onNodeWithTag("smart-shelf-page").assertExists()
        rule.onNodeWithTag("smart-shelf-movies").assertIsDisplayed()
        rule.onNodeWithTag("smart-shelf-series").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("smart-shelf-counts").assertTextContains("2", substring = true)
        rule.onNodeWithTag("smart-item-s1").performScrollTo().assertIsDisplayed()
    }

    /** The covers are a size down from the library grid: more of a shelf on screen at once. */
    @Test fun shelfCoversAreSmallerThanTheLibraryGrid() {
        var density = 1f
        val state = ReelstackUiState(connections = listOf(connection), libraryPath = listOf(SmartShelfPresets.halloween.pathId to "Halloween"),
            libraryEntries = List(12) { item("m$it", "Movie") })
        val tv = InstrumentationTvCheck.isTelevision()
        rule.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(if (tv) DpSize(960.dp, 540.dp) else DpSize(412.dp, 900.dp))) {
            density = LocalDensity.current.density
            ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides Recorder()) { LibraryScreen(state, {}, {}, {}) } }
        } }
        val width = rule.onNodeWithTag("smart-item-m0").fetchSemanticsNode().boundsInRoot.width / density
        // The library grid's covers start at 155 dp on a television and 130 dp on a phone.
        val library = if (tv) 155f else 130f
        assertTrue("A shelf cover is $width dp; the library grid's smallest is $library dp", width < library)
    }

    @Test fun theBuilderAsksForARuleBeforeSaving() {
        val actions = Recorder()
        var editor by mutableStateOf(SmartShelfEditor(SmartShelf("u-1"), isNew = true,
            facets = CatalogueFacets(genres = listOf("Horror", "Family"), tags = listOf("halloween", "dog"))))
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(editor, actions) } }
        rule.onNodeWithTag("smart-shelf-save").performClick()
        assertEquals(0, actions.saved)
        rule.onNodeWithTag("smart-shelf-preview").assertTextContains("sjanger", substring = true)
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-genre-Horror"))
        rule.onNodeWithTag("smart-genre-Horror").performClick()
        rule.runOnIdle { assertEquals(listOf("Horror"), actions.drafts.last().genres) }
        editor = editor.copy(draft = actions.drafts.last(), previewMovies = 38, previewSeries = 4)
        rule.onNodeWithTag("smart-shelf-preview").assertTextContains("38 filmar og 4 seriar", substring = true)
        rule.onNodeWithTag("smart-shelf-save").performClick()
        assertEquals(1, actions.saved)
    }

    @Test fun suggestedTagsAreTheOnesTheServerHas() {
        val actions = Recorder()
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(SmartShelfEditor(SmartShelf("u-1"), isNew = false,
            facets = CatalogueFacets(tags = listOf("halloween", "dog", "obscure keyword"))), actions) } }
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-tag-dog"))
        rule.onNodeWithTag("smart-tag-dog").performClick()
        rule.runOnIdle { assertEquals(listOf("dog"), actions.drafts.last().tags) }
        rule.onNodeWithTag("smart-tag-obscure keyword").assertDoesNotExist()
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-kinds-SERIES"))
        rule.onNodeWithTag("smart-kinds-SERIES").performClick()
        rule.runOnIdle { assertEquals(SmartShelfKinds.SERIES, actions.drafts.last().kinds) }
    }

    @Test fun theLibraryRowOpensShelvesAndMakesNewOnes() {
        val actions = Recorder()
        rule.setContent { ReelstackTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            SmartShelfRow(listOf(SmartShelf("u-1", name = "Hundefilmar", tags = listOf("dog")), SmartShelfPresets.halloween),
                emptyMap(), ServiceKind.JELLYFIN, onOpen = actions::open, onNew = { actions.edit() })
        } } }
        rule.onNodeWithText("Hundefilmar").assertIsDisplayed()
        rule.onNodeWithTag("smart-shelf-halloween").performClick()
        assertEquals("halloween", actions.opened)
        rule.onNodeWithTag("smart-shelf-new").performScrollTo().performClick()
        assertEquals(null to null, actions.editing)
    }

    /** A shelf on Home is a row per server, like «Nye filmar», with a way into the whole shelf. */
    @Test fun aShelfOnHomeGetsARowPerServerAndAWayIn() {
        val actions = Recorder()
        val emby = connection.copy(kind = ServiceKind.EMBY, name = "Emby")
        val shelf = SmartShelf("u-1", name = "Hundefilmar", tags = listOf("dog"), onHome = true)
        val onJellyfin = HomeRowKey.shelfRow(ServiceKind.JELLYFIN, "u-1")
        val onEmby = HomeRowKey.shelfRow(ServiceKind.EMBY, "u-1")
        fun film(id: String, source: ServiceKind) = LibraryMedia(id = id, title = "Hund $id", subtitle = "Film",
            artworkRes = R.drawable.media_placeholder, source = source)
        val state = ReelstackUiState(connections = listOf(connection, emby), smartShelves = listOf(shelf),
            homeShelfMedia = mapOf(onJellyfin.id to listOf(film("j1", ServiceKind.JELLYFIN)),
                onEmby.id to listOf(film("e1", ServiceKind.EMBY))))
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides actions) {
            HomeScreen(state = state, contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {}, onRefresh = {})
        } } }
        // Home starts with the demo's own rows, so a shelf is some way down the feed.
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag("home-shelf-${onJellyfin.id}"))
        rule.onNodeWithContentDescription("Hundefilmar · Jellyfin").assertIsDisplayed()
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag("home-shelf-all-${onEmby.id}"))
        rule.onNodeWithContentDescription("Hundefilmar · Emby").assertExists()
        rule.onNodeWithTag("home-shelf-all-${onEmby.id}").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("u-1", actions.opened) }
    }

    /** A shelf's row is the reader's to place: hidden in the layout it is not drawn, and a row with no titles is not either. */
    @Test fun aHiddenOrEmptyShelfRowIsNotDrawn() {
        val emby = connection.copy(kind = ServiceKind.EMBY, name = "Emby")
        val shelf = SmartShelf("u-1", name = "Hundefilmar", tags = listOf("dog"), onHome = true)
        val hidden = HomeRowKey.shelfRow(ServiceKind.JELLYFIN, "u-1")
        val shown = HomeRowKey.shelfRow(ServiceKind.EMBY, "u-1")
        fun film(id: String, source: ServiceKind) = LibraryMedia(id = id, title = "Hund $id", subtitle = "Film",
            artworkRes = R.drawable.media_placeholder, source = source)
        val layout = HomeLayout.DEFAULT.withShelfRows(listOf(hidden, shown)).withVisible(hidden, false)
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides Recorder()) {
            HomeScreen(state = ReelstackUiState(connections = listOf(connection, emby), smartShelves = listOf(shelf),
                homeLayout = layout, homeShelfMedia = mapOf(hidden.id to listOf(film("j1", ServiceKind.JELLYFIN)),
                    shown.id to listOf(film("e1", ServiceKind.EMBY)))),
                contentPadding = PaddingValues(0.dp), onSessionClick = {}, onPlaybackToggle = {},
                onMediaClick = {}, onLibraryClick = {}, onUpcomingClick = {}, onRefresh = {})
        } } }
        // The visible row is the control: it is found, so the hidden one is missing because it is hidden.
        rule.onNodeWithTag("home-feed").performScrollToNode(hasTestTag("home-shelf-${shown.id}"))
        rule.onNodeWithTag("home-shelf-${shown.id}").assertExists()
        rule.onNodeWithTag("home-shelf-${hidden.id}").assertDoesNotExist()
    }

    /** Library, Home and menu are three choices of their own, and a shelf shown nowhere says so. */
    @Test fun aShelfIsPlacedPlaceByPlace() {
        val actions = Recorder()
        var editor by mutableStateOf(SmartShelfEditor(SmartShelf("u-1", tags = listOf("dog")), isNew = false))
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(editor, actions) } }
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-shelf-on-home"))
        rule.onNodeWithTag("smart-shelf-on-home").performClick()
        rule.runOnIdle { assertTrue(actions.drafts.last().onHome) }
        rule.onNodeWithTag("smart-shelf-place-none").assertDoesNotExist()
        editor = editor.copy(draft = SmartShelf("u-1", tags = listOf("dog"), inLibrary = false))
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-shelf-place-none"))
        rule.onNodeWithTag("smart-shelf-place-none").assertIsDisplayed()
    }

    /** A server that did not answer is not one with no genres: the builder says so and can ask again. */
    @Test fun aServerThatDidNotAnswerCanBeAskedAgain() {
        val actions = Recorder()
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(
            SmartShelfEditor(SmartShelf("u-1", tags = listOf("dog")), isNew = false, facetsFailed = true), actions) } }
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-facets-retry"))
        rule.onNodeWithTag("smart-facets-retry").performClick()
        rule.runOnIdle { assertEquals(1, actions.retried) }
    }

    /** The library page keeps no empty frame for shelves, and a new one is a quiet plus, not a tile. */
    @Test fun theLibraryPageShowsShelvesOnlyWhenThereAreSome() {
        val libraries = listOf(RemoteLibraryItem("movies", "Filmar", "", null, "CollectionFolder", null,
            isFolder = true, collectionType = "movies"))
        var state by mutableStateOf(ReelstackUiState(connections = listOf(connection), libraryEntries = libraries))
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides Recorder()) {
            LibraryHub(state, {}, {}, null, {})
        } } }
        // A season's template fills the row by itself while it runs; outside one the row is gone.
        if (libraryShelves(emptyList(), LocalDate.now()).isEmpty()) rule.onNodeWithTag("smart-shelf-row").assertDoesNotExist()
        state = state.copy(smartShelves = listOf(SmartShelf("u-1", name = "Hundefilmar", tags = listOf("dog"))))
        rule.onNodeWithTag("library-hub").performScrollToNode(hasTestTag("smart-shelf-row"))
        rule.onNodeWithText("Hundefilmar").assertIsDisplayed()
        val tile = rule.onNodeWithTag("smart-shelf-u-1").fetchSemanticsNode().boundsInRoot
        val plus = rule.onNodeWithTag("smart-shelf-new").fetchSemanticsNode().boundsInRoot
        assertTrue("The plus is ${plus.height} px high beside a ${tile.height} px tile", plus.height < tile.height / 2)
    }

    /** With no row on the page, the first shelf is made from «Tilpass biblioteksida». */
    @Test fun aFirstShelfStartsFromTheLibraryEditor() {
        val actions = Recorder()
        rule.setContent { ReelstackTheme { CompositionLocalProvider(LocalSmartShelfActions provides actions) {
            LibraryEditorDialog(ReelstackUiState(connections = listOf(connection)), {}, {}, { _, _, _ -> })
        } } }
        rule.onNodeWithTag("library-editor-list").performScrollToNode(hasTestTag("library-editor-new-shelf"))
        rule.onNodeWithTag("library-editor-new-shelf").performClick()
        rule.runOnIdle { assertEquals(null to null, actions.editing) }
    }

    /** «Or» and «and» are offered once a shelf has both tags and genres, and the rule says which. */
    @Test fun bothTagsAndGenresOfferOrAndAnd() {
        val actions = Recorder()
        var editor by mutableStateOf(SmartShelfEditor(SmartShelf("u-1", tags = listOf("halloween")), isNew = false))
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(editor, actions) } }
        rule.onNodeWithTag("smart-match-all").assertDoesNotExist()
        editor = editor.copy(draft = editor.draft.copy(genres = listOf("Horror")))
        rule.onNodeWithTag("smart-shelf-editor-list").performScrollToNode(hasTestTag("smart-match-all"))
        rule.onNodeWithTag("smart-match-any").assertIsSelected()
        rule.onNodeWithTag("smart-match-all").performClick()
        rule.runOnIdle { assertTrue(actions.drafts.last().matchAll) }
    }

    /** A template names a shelf that has none yet; what the owner types goes to the draft. */
    @Test fun theNameFieldFollowsATemplateAndTheOwner() {
        val actions = Recorder()
        var editor by mutableStateOf(SmartShelfEditor(SmartShelf("u-1"), isNew = true))
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(editor, actions) } }
        editor = editor.copy(draft = SmartShelf("u-1", name = "Halloween", tags = listOf("halloween")))
        rule.onNodeWithTag("smart-shelf-name").assertTextContains("Halloween")
        rule.runOnIdle { assertTrue(actions.drafts.isEmpty()) }
        rule.onNodeWithTag("smart-shelf-name").performTextClearance()
        rule.onNodeWithTag("smart-shelf-name").performTextInput("Hundefilmar")
        rule.runOnIdle { assertEquals("Hundefilmar", actions.drafts.last().name) }
    }

    /** The remote walks past a text field: down leaves it again, without OK opening anything on the way. */
    @OptIn(ExperimentalTestApi::class)
    @Test fun theRemoteWalksPastTheNameField() {
        if (!InstrumentationTvCheck.isTelevision()) return
        // A new shelf: templates above the name, icons below it.
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(SmartShelfEditor(SmartShelf("u-1", name = "Hund"), isNew = true), Recorder()) } }
        val name = rule.onNodeWithTag("smart-shelf-name")
        name.performSemanticsAction(SemanticsActions.RequestFocus)
        name.assertIsFocused()
        name.performKeyInput { pressKey(Key.DirectionDown) }
        name.assertIsNotFocused()
        name.performSemanticsAction(SemanticsActions.RequestFocus)
        name.performKeyInput { pressKey(Key.DirectionUp) }
        name.assertIsNotFocused()
    }

    /** On a television the builder starts on a choice, never in a text field. */
    @Test fun aRemoteStartsOnTheFirstTemplate() {
        if (!InstrumentationTvCheck.isTelevision()) return
        rule.setContent { ReelstackTheme { SmartShelfEditorDialog(SmartShelfEditor(SmartShelf("u-1"), isNew = true), Recorder()) } }
        rule.waitForIdle()
        rule.onNodeWithTag("smart-template-halloween").assertIsFocused()
    }
}

private object InstrumentationTvCheck {
    fun isTelevision(): Boolean {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        return (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    }
}
