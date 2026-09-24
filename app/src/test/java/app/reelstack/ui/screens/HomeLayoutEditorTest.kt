package app.reelstack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.HomeLayout
import app.reelstack.data.model.HomeLibraryChoice
import app.reelstack.data.model.HomeRowKey
import app.reelstack.data.model.HomeRowKind
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Home editor on a phone: rows per server, their switches, libraries and order. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h900dp-xxhdpi", application = app.reelstack.SheetTestApplication::class)
@OptIn(ExperimentalTestApi::class)
class HomeLayoutEditorTest {
    @get:Rule val rule = createComposeRule()

    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example", "token", "me")
    private val emby = ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example", "token", "me")
    private val libraryChanges = mutableListOf<Pair<ServiceKind, HomeLibraryChoice>>()

    private fun open(initial: ReelstackUiState, fontScale: Float = 1f): () -> ReelstackUiState {
        var state by mutableStateOf(initial.copy(homeLayout = initial.homeLayout ?: HomeLayout.DEFAULT))
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                MaterialTheme {
                    HomeLayoutSetting(state, HomeEditorActions(
                        onLayoutChange = { state = state.copy(homeLayout = it) },
                        onLibrariesChange = { kind, choice ->
                            libraryChanges += kind to choice
                            state = state.copy(homeLibraries = state.homeLibraries + (kind to choice))
                        },
                    ))
                }
            }
        }
        rule.onNodeWithTag("home-layout-open").performClick()
        return { state }
    }

    private fun row(tag: String) = rule.onNodeWithTag("home-layout-list").performScrollToNode(hasTestTag(tag)).let { rule.onNodeWithTag(tag) }

    @Test fun seriesFromEmbyStayWhileEverythingFromJellyfinGoes() {
        val state = open(ReelstackUiState(connections = listOf(jellyfin, emby)))

        rule.onNodeWithTag("home-layout-filter-JELLYFIN").performClick()
        rule.onNodeWithTag("home-layout-hide-source").performClick()
        rule.onNodeWithTag("home-layout-filter-EMBY").performClick()
        row("home-layout-row-NEW_SERIES:EMBY").assertIsDisplayed()
        rule.onNodeWithTag("home-layout-row-NEW_SERIES:JELLYFIN").assertDoesNotExist()
        row("home-layout-visible-NEW_MOVIES:EMBY").performClick()

        val layout = state().effectiveHomeLayout
        assertTrue(layout.order.filter { it.source == ServiceKind.JELLYFIN }.none(layout::isVisible))
        assertTrue(layout.isVisible(HomeRowKey(HomeRowKind.NEW_SERIES, ServiceKind.EMBY)))
        assertFalse(layout.isVisible(HomeRowKey(HomeRowKind.NEW_MOVIES, ServiceKind.EMBY)))
        assertTrue(layout.isVisible(HomeRowKey(HomeRowKind.UPCOMING)))
    }

    @Test fun aServerWithoutAnAddressHasNoRowsToEdit() {
        open(ReelstackUiState(connections = listOf(jellyfin)))

        row("home-layout-row-CONTINUE_WATCHING:JELLYFIN").assertIsDisplayed()
        rule.onNodeWithTag("home-layout-row-CONTINUE_WATCHING:EMBY").assertDoesNotExist()
        rule.onNodeWithTag("home-layout-filter-EMBY").assertDoesNotExist()
    }

    @Test fun aRowMovesToTheTopStopsThereAndResetBringsBackTheDefault() {
        val state = open(ReelstackUiState(connections = listOf(jellyfin)))
        val nextUp = HomeRowKey(HomeRowKind.NEXT_UP, ServiceKind.JELLYFIN)

        repeat(2) { row("home-layout-up-${nextUp.id}").performClick() }

        assertEquals(nextUp, editableHomeRows(state()).first())
        row("home-layout-up-${nextUp.id}").assertIsNotEnabled()
        rule.onNodeWithTag("home-layout-reset").performClick()
        assertEquals(HomeLayout.DEFAULT, state().homeLayout)
    }

    @Test fun eachRowHasItsOwnLibrariesAndOnlyThoseThatCanFillIt() {
        val views = listOf(
            RemoteLibraryView("films", "Filmar", "movies"),
            RemoteLibraryView("tv", "Seriar", "tvshows"),
            RemoteLibraryView("anime", "Anime", "tvshows"),
        )
        val state = open(ReelstackUiState(connections = listOf(emby), homeLibraryViews = mapOf(ServiceKind.EMBY to views)))

        row("home-layout-libraries-NEW_SERIES:EMBY").performClick()
        rule.onNodeWithTag("home-layout-library-films").assertDoesNotExist()
        rule.onNodeWithTag("home-layout-library-anime").performClick()

        val choice = state().homeLibraries.getValue(ServiceKind.EMBY)
        assertEquals(setOf("tv"), choice.included[HomeRowKind.NEW_SERIES])
        // The same server's other rows keep every library.
        assertNull(choice.included[HomeRowKind.CONTINUE_WATCHING])

        rule.onNodeWithTag("home-layout-library-all").performClick()
        assertNull(state().homeLibraries.getValue(ServiceKind.EMBY).included[HomeRowKind.NEW_SERIES])
        assertEquals(2, libraryChanges.size)
    }

    @Test fun ticking_every_library_again_means_all_libraries() {
        val views = listOf(RemoteLibraryView("tv", "Seriar", "tvshows"), RemoteLibraryView("anime", "Anime", "tvshows"))
        val state = open(ReelstackUiState(connections = listOf(emby), homeLibraryViews = mapOf(ServiceKind.EMBY to views)))

        row("home-layout-libraries-NEXT_UP:EMBY").performClick()
        rule.onNodeWithTag("home-layout-library-anime").performClick()
        rule.onNodeWithTag("home-layout-library-anime").performClick()

        assertNull(state().homeLibraries.getValue(ServiceKind.EMBY).included[HomeRowKind.NEXT_UP])
    }

    @Test fun largeTextStillReachesAndMovesTheLastRow() {
        val state = open(ReelstackUiState(connections = listOf(jellyfin, emby)), fontScale = 2f)

        row("home-layout-up-UPCOMING").assertIsDisplayed().performClick()

        val rows = editableHomeRows(state())
        assertEquals(HomeRowKey(HomeRowKind.UPCOMING), rows[rows.lastIndex - 1])
        rule.onNodeWithTag("home-layout-done").assertIsDisplayed()
    }
}
