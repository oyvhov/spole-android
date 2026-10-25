package app.reelstack.ui

import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverGridTarget
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverPerson
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.PersonPage
import app.reelstack.ui.state.toDiscoverUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoverStateFixesTest {
    private fun media(id: Int) = DiscoverMedia("seerr-movie-$id", "T$id", "2026", 0, false, remoteId = id, mediaType = "movie")

    @Test fun backClosesTheInnerPageOfDiscoverBeforeLeavingTheTab() {
        val discover = ReelstackUiState(selectedTab = AppTab.DISCOVER)
        val grid = discover.copy(discoverHub = DiscoverHubState(grid = DiscoverGridTarget.Row(DiscoverRowKind.TRENDING)))
        assertEquals(ShellBack.CLOSE_GRID, shellBack(grid, AppTab.HOME))
        assertEquals(ShellBack.CLOSE_MY_REQUESTS, shellBack(discover.copy(myRequestsOpen = true), AppTab.HOME))
        assertEquals(ShellBack.START_TAB, shellBack(discover, AppTab.HOME))
        assertNull(shellBack(ReelstackUiState(selectedTab = AppTab.HOME), AppTab.HOME))
        // Sheets and the search screen own Back while they are open.
        assertNull(shellBack(grid.copy(activeSheet = AppSheet.TitleDetails("x")), AppTab.HOME))
        assertNull(shellBack(grid.copy(globalSearchOpen = true), AppTab.HOME))
    }

    @Test fun aChangeToATitleReachesEveryListThatShowsIt() {
        val one = media(1)
        val state = ReelstackUiState(
            detailMedia = one,
            discoverHub = DiscoverHubState(rows = mapOf(DiscoverRowKind.TRENDING to DiscoverRowState(listOf(one)))),
            personPage = PersonPage(DiscoverPerson(9, "P", null), inLibrary = listOf(one), more = listOf(one)),
            contentDetails = ContentDetails("x", "X", "", "", artworkRes = 0, similar = listOf(one), recommended = listOf(one)),
        ).mapDiscoverMedia { if (it.id == one.id) it.copy(requested = true) else it }
        assertTrue(state.detailMedia!!.requested)
        assertTrue(state.discoverHub.rows.getValue(DiscoverRowKind.TRENDING).items.single().requested)
        assertTrue(state.personPage!!.inLibrary.single().requested && state.personPage!!.more.single().requested)
        assertTrue(state.contentDetails!!.similar.single().requested && state.contentDetails!!.recommended.single().requested)
    }

    @Test fun thePreviewWithoutServersStillHasARowToBrowse() {
        val demo = listOf(media(1), media(2))
        val hub = ReelstackUiState(discover = demo).toDiscoverUiState().hub
        assertEquals(demo, hub.rows.getValue(DiscoverRowKind.TRENDING).items)
    }
}
