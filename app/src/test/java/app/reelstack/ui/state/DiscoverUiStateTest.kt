package app.reelstack.ui.state

import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoverUiStateTest {
    @Test
    fun `adapter keeps library hits before server search results`() {
        val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Heime", "https://media.example", "token")
        val seerr = ServiceConnection(ServiceKind.SEERR, "Søk", "https://search.example", "token")
        val result = DiscoverMedia(
            id = "seerr-film",
            title = "Søkjetreff",
            metadata = "Film",
            artworkRes = 0,
            inLibrary = false,
        )

        val state = ReelstackUiState(
            connections = listOf(jellyfin, seerr),
            accounts = mapOf(ServiceKind.SEERR to ServiceAccount(
                source = ServiceKind.SEERR, id = "reader", displayName = "Lesar",
            )),
            searchQuery = "søk",
            searchResults = listOf(result),
        ).toDiscoverUiState()

        assertEquals(2, state.configuredCount)
        assertEquals(listOf(result), state.visibleDiscover)
        assertEquals("Lesar", state.verifiedPanelAccount(ServiceKind.SEERR)?.displayName)
        assertNull(state.verifiedPanelAccount(ServiceKind.JELLYFIN))
    }
}
