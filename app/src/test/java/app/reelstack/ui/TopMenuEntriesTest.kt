package app.reelstack.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** The television's top menu follows the owner's «Sider og meny»: its order, and what is switched off. */
class TopMenuEntriesTest {
    private val films = "films" to "Filmar"
    private val series = "series" to "Seriar"

    private fun labels(entries: List<TopMenuEntry>) = entries.map {
        when (it) {
            is TopMenuEntry.Page -> it.tab.name
            is TopMenuEntry.Library -> it.id
            TopMenuEntry.Search -> "SEARCH"
        }
    }

    @Test fun theLibrariesTakeTheSlotOfTheLibraryPage() {
        val entries = topMenuEntries(listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS"), listOf(films, series))
        assertEquals(listOf("HOME", "films", "series", "DISCOVER", "SEARCH"), labels(entries))
    }

    @Test fun theOwnersOrderDecidesWhereTheyStand() {
        val entries = topMenuEntries(listOf("LIBRARY", "DISCOVER", "HOME", "SETTINGS"), listOf(films, series))
        assertEquals(listOf("films", "series", "DISCOVER", "HOME", "SEARCH"), labels(entries))
    }

    @Test fun aPageSwitchedOffIsNotInTheMenu() {
        val entries = topMenuEntries(listOf("HOME", "LIBRARY", "SETTINGS"), listOf(films))
        assertEquals(listOf("HOME", "films", "SEARCH"), labels(entries))
        assertEquals(listOf("HOME", "SEARCH"), labels(topMenuEntries(listOf("HOME", "SETTINGS"), listOf(films))))
    }

    @Test fun withNoLibrariesKnownTheLibraryPageStandsInTheirPlace() {
        val entries = topMenuEntries(listOf("HOME", "LIBRARY", "DISCOVER", "SETTINGS"), emptyList())
        assertEquals(listOf("HOME", "LIBRARY", "DISCOVER", "SEARCH"), labels(entries))
    }

    @Test fun settingsLiveBehindTheProfileCircleAndSearchIsAlwaysLast() {
        val entries = topMenuEntries(listOf("SETTINGS", "HOME"), emptyList())
        assertEquals(listOf("HOME", "SEARCH"), labels(entries))
    }
}
