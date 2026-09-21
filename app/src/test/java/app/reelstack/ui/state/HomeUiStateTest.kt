package app.reelstack.ui.state

import app.reelstack.data.model.HomeRow
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.AppSheet
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun `adapter keeps the Home fields used by the screen`() {
        val source = ReelstackUiState(
            activeSheet = AppSheet.UpcomingCalendar,
            connections = listOf(
                ServiceConnection(ServiceKind.JELLYFIN, "Heime", "https://media.example", "token"),
                ServiceConnection(ServiceKind.SEERR, "Søk", "", ""),
            ),
            homeSections = setOf(HomeSection.CONTINUE_WATCHING),
            homeRowOrder = listOf(HomeRow.CONTINUE_WATCHING),
            searchQuery = "skal ikkje lekke til Home",
            pinError = "skal ikkje lekke til Home",
        )

        val home = source.toHomeUiState()

        assertEquals(AppSheet.UpcomingCalendar, home.activeSheet)
        assertEquals(1, home.configuredCount)
        assertEquals(setOf(HomeSection.CONTINUE_WATCHING), home.homeSections)
        assertEquals(listOf(HomeRow.CONTINUE_WATCHING), home.homeRowOrder)
        assertTrue(home.connections.first { it.kind == ServiceKind.JELLYFIN }.token.isNotBlank())
    }
}
