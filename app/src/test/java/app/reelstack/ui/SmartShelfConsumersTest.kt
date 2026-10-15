package app.reelstack.ui

import app.reelstack.data.model.*
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.data.repository.SessionScope
import org.junit.Assert.*
import org.junit.Test

class SmartShelfConsumersTest {
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://jellyfin.example",
        token = "jellyfin-token", userId = "adult")
    private val emby = ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example",
        token = "emby-token", userId = "adult")
    private val shelf = SmartShelf("mine", tags = listOf("family"), onHome = true,
        scope = SmartShelfScope.of(jellyfin, setOf("films")))
    private val state = ReelstackUiState(connections = listOf(jellyfin, emby),
        selectedLibrarySource = ServiceKind.JELLYFIN, activeProfileId = "adult",
        sessionScope = SessionScope("adult", "account", 1), smartShelves = listOf(shelf),
        smartShelfEditor = SmartShelfEditor(shelf, isNew = false))

    @Test fun scopedHomeRowUsesOnlyItsOwnerWhileLegacyRowsRemainPerServer() {
        val legacy = shelf.copy(id = "legacy", scope = null)
        val rows = state.copy(smartShelves = listOf(shelf, legacy)).homeShelfRows
        assertEquals(listOf(ServiceKind.JELLYFIN), rows.filter { it.shelf == shelf.id }.map { it.source })
        assertEquals(setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY), rows.filter { it.shelf == legacy.id }.map { it.source }.toSet())
        assertTrue(state.copy(connections = listOf(jellyfin.copy(userId = "child"), emby))
            .homeShelfRows.none { it.shelf == shelf.id })
    }

    @Test fun savedRowsForAnotherAccountAreNotRenderedOrRemovedFromStoredLayout() {
        val wrongRow = HomeRowKey.shelfRow(ServiceKind.EMBY, shelf.id)
        val saved = HomeLayout(order = listOf(wrongRow))
        val current = state.copy(homeLayout = saved)
        assertFalse(current.effectiveHomeLayout.order.contains(wrongRow))
        assertTrue(current.homeLayout!!.order.contains(wrongRow))
        assertTrue(current.effectiveHomeLayout.order.contains(HomeRowKey.shelfRow(ServiceKind.JELLYFIN, shelf.id)))
    }

    @Test fun savingLayoutOutsideTheSeasonPreservesGlobalAndOwnedSeasonRowPositions() {
        val inactivePeriod = SmartShelfPeriod.entries.first { !it.isActive(java.time.LocalDate.now()) }
        val global = shelf.copy(id = "global-season", period = inactivePeriod, scope = null)
        val owned = shelf.copy(id = "owned-season", period = inactivePeriod)
        val saved = HomeLayout(order = listOf(HomeRowKey(HomeRowKind.NOW_PLAYING),
            HomeRowKey.shelfRow(ServiceKind.EMBY, global.id), HomeRowKey(HomeRowKind.UPCOMING),
            HomeRowKey.shelfRow(ServiceKind.JELLYFIN, owned.id)))
        val current = state.copy(homeLayout = saved, smartShelves = listOf(global, owned) +
            SmartShelfPresets.all.map { it.copy(onHome = false) })
        assertTrue(current.homeShelfRows.isEmpty())
        assertEquals(saved.order, current.effectiveHomeLayout.order)
        val savedAgain = current.copy(homeLayout = current.effectiveHomeLayout)
        assertEquals(saved.order, savedAgain.homeLayout!!.order)
    }

    @Test fun openingScopedShelfChoosesItsOwnerWithoutFallingBackWhenUnavailable() {
        val ownedByEmby = shelf.copy(scope = SmartShelfScope.of(emby))
        assertEquals(emby, state.smartShelfConnection(ownedByEmby))
        assertNull(state.copy(connections = listOf(jellyfin)).smartShelfConnection(ownedByEmby))
        assertNull(state.copy(connections = listOf(jellyfin.copy(userId = "child"), emby)).smartShelfConnection(shelf))
        assertEquals(jellyfin, state.smartShelfConnection(shelf.copy(scope = null)))
    }

    @Test fun newDraftUsesTheVerifiedRootLibraryAndNeverTheNestedSeriesId() {
        val browsing = state.copy(libraryShortcuts = listOf("films" to "Films"),
            libraryPath = listOf("films" to "Films", "series-id" to "A series"))
        assertEquals(setOf("films"), browsing.newSmartShelfScope()!!.libraryIds)
        assertEquals(smartShelfAccountKey(jellyfin), browsing.newSmartShelfScope()!!.accountKey)
        assertEquals(emptySet<String>(), browsing.copy(libraryPath = listOf("series-id" to "A series")).newSmartShelfScope()!!.libraryIds)
        assertEquals(emptySet<String>(), state.copy(libraryPath = listOf(shelf.pathId to "Shelf")).newSmartShelfScope()!!.libraryIds)
        assertNull(state.copy(connections = listOf(jellyfin.copy(userId = ""))).newSmartShelfScope())
        assertEquals(setOf("other"), state.copy(libraryChoices = listOf(RemoteLibraryView("other", "Other", "movies")),
            libraryPath = listOf("other" to "Other")).newSmartShelfScope()!!.libraryIds)
    }

    @Test fun lateEditorResultsAreRejectedAfterProfileGenerationAccountOrLibraryChanges() {
        assertTrue(state.ownsSmartShelfEditorResult(state, shelf, jellyfin))
        val renamed = state.copy(smartShelfEditor = state.smartShelfEditor!!.copy(draft = shelf.copy(name = "Renamed")))
        assertTrue(renamed.ownsSmartShelfEditorResult(state, shelf, jellyfin))
        assertFalse(state.copy(activeProfileId = "child").ownsSmartShelfEditorResult(state, shelf, jellyfin))
        assertFalse(state.copy(sessionScope = SessionScope("adult", "account", 3)).ownsSmartShelfEditorResult(state, shelf, jellyfin))
        assertFalse(state.copy(connections = listOf(jellyfin.copy(userId = "child"), emby))
            .ownsSmartShelfEditorResult(state, shelf, jellyfin))
        assertFalse(state.copy(smartShelfEditor = null).ownsSmartShelfEditorResult(state, shelf, jellyfin))
        for (scope in listOf(SmartShelfScope.of(emby), SmartShelfScope.of(jellyfin, setOf("series")),
            SmartShelfScope.of(jellyfin, emptySet()), SmartShelfScope.of(jellyfin))) {
            val changed = state.copy(smartShelfEditor = state.smartShelfEditor!!.copy(draft = shelf.copy(scope = scope)))
            assertFalse(changed.ownsSmartShelfEditorResult(state, shelf, jellyfin))
        }
    }

    @Test fun previewQueryIncludesScopeAndAllServerResultFiltersButIgnoresTheDisplayName() {
        assertTrue(sameSmartShelfQuery(shelf, shelf.copy(name = "A new name", inMenu = true)))
        for (changed in listOf(shelf.copy(scope = null), shelf.copy(scope = SmartShelfScope.of(jellyfin, emptySet())),
            shelf.copy(tags = listOf("crime")), shelf.copy(genres = listOf("Comedy")),
            shelf.copy(kinds = SmartShelfKinds.SERIES), shelf.copy(unwatched = true), shelf.copy(sort = SmartShelfSort.TITLE))) {
            assertFalse(sameSmartShelfQuery(shelf, changed))
        }
        val combined = shelf.copy(genres = listOf("Comedy"))
        assertFalse(sameSmartShelfQuery(combined, combined.copy(matchAll = true)))
    }

    @Test fun changingLibrarySelectionKeepsAnInFlightLibraryListButRejectsItsOldFacetsAndPreview() {
        val changedShelf = shelf.copy(scope = SmartShelfScope.of(jellyfin, setOf("series")))
        val changed = state.copy(smartShelfEditor = state.smartShelfEditor!!.copy(draft = changedShelf))
        assertTrue(sameSmartShelfSource(shelf, changedShelf))
        assertTrue(changed.ownsSmartShelfEditorResult(state, shelf, jellyfin, matchLibraries = false))
        assertFalse(changed.ownsSmartShelfEditorResult(state, shelf, jellyfin))
        val otherAccount = changed.copy(smartShelfEditor = state.smartShelfEditor!!.copy(
            draft = shelf.copy(scope = SmartShelfScope.of(emby))))
        assertFalse(otherAccount.ownsSmartShelfEditorResult(state, shelf, jellyfin, matchLibraries = false))
        assertFalse(sameSmartShelfSource(shelf, shelf.copy(scope = null)))
    }
}
