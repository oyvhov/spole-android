package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    private val jellyfinSeries = HomeRowKey(HomeRowKind.NEW_SERIES, ServiceKind.JELLYFIN)
    private val embySeries = HomeRowKey(HomeRowKind.NEW_SERIES, ServiceKind.EMBY)
    private val jellyfinContinue = HomeRowKey(HomeRowKind.CONTINUE_WATCHING, ServiceKind.JELLYFIN)
    private val embyContinue = HomeRowKey(HomeRowKind.CONTINUE_WATCHING, ServiceKind.EMBY)

    @Test fun theDefaultMatchesTheOlderDefaultRowForRow() {
        assertEquals(HomeLayout.fromLegacy(HomeRow.entries, HomeSection.entries.toSet(), showNextUp = true), HomeLayout.DEFAULT)
        assertEquals(listOf(jellyfinContinue, embyContinue), HomeLayout.DEFAULT.order.take(2))
        assertTrue(HomeLayout.DEFAULT.hidden.isEmpty())
    }

    @Test fun oneOldSwitchBecomesARowPerServer() {
        val layout = HomeLayout.fromLegacy(HomeRow.entries,
            HomeSection.entries.toSet() - HomeSection.CONTINUE_WATCHING - HomeSection.JELLYFIN_SERIES, showNextUp = false)

        assertFalse(layout.isVisible(jellyfinContinue))
        assertFalse(layout.isVisible(embyContinue))
        assertFalse(layout.isVisible(jellyfinSeries))
        assertTrue(layout.isVisible(embySeries))
        assertFalse(layout.isVisible(HomeRowKey(HomeRowKind.NEXT_UP, ServiceKind.EMBY)))
    }

    @Test fun theOldOrderIsKept() {
        val layout = HomeLayout.fromLegacy(listOf(HomeRow.UPCOMING, HomeRow.EMBY_SERIES), HomeSection.entries.toSet(), true)
        assertEquals(listOf(HomeRowKey(HomeRowKind.UPCOMING), embySeries, jellyfinContinue), layout.order.take(3))
    }

    @Test fun seriesFromEmbyCanStayWhileJellyfinGoes() {
        val layout = HomeLayout.DEFAULT.withSourceVisible(ServiceKind.JELLYFIN, false).withVisible(embySeries, true)

        assertTrue(layout.order.filter { it.source == ServiceKind.JELLYFIN }.none(layout::isVisible))
        assertTrue(layout.isVisible(embySeries))
        assertTrue(layout.isVisible(HomeRowKey(HomeRowKind.UPCOMING)))
    }

    @Test fun movingSkipsRowsTheEditorDoesNotList() {
        val shown = HomeLayout.DEFAULT.order.filter { it.source != ServiceKind.EMBY }
        val moved = HomeLayout.DEFAULT.moved(HomeRowKey(HomeRowKind.NOW_PLAYING), -1, shown)

        // Jellyfin's row was the neighbour in the list; Emby's row, not listed, keeps its slot.
        assertEquals(listOf(HomeRowKey(HomeRowKind.NOW_PLAYING), embyContinue, jellyfinContinue), moved.order.take(3))
        assertEquals(HomeLayout.DEFAULT, HomeLayout.DEFAULT.moved(jellyfinContinue, -1, shown))
    }

    @Test fun aSavedLayoutSurvivesAndUnknownRowsAreDropped() {
        val layout = HomeLayout.DEFAULT.moved(embySeries, -1, HomeLayout.DEFAULT.order).withVisible(jellyfinSeries, false)
        assertEquals(layout, HomeLayout.decode(layout.encode()))

        val older = """{"order":["UPCOMING","SOMETHING_NEW:EMBY","NEW_SERIES"],"hidden":["NEW_MOVIES:EMBY"]}"""
        val decoded = HomeLayout.decode(older)!!
        assertEquals(HomeRowKey(HomeRowKind.UPCOMING), decoded.order.first())
        assertEquals(HomeLayout.ALL_KEYS.size, decoded.order.size)
        assertEquals(setOf(HomeRowKey(HomeRowKind.NEW_MOVIES, ServiceKind.EMBY)), decoded.hidden)
        assertNull(HomeLayout.decode("not json"))
    }

    @Test fun rowKeysRejectAServerOnAGlobalRowAndTheReverse() {
        assertEquals(embySeries, HomeRowKey.parse("NEW_SERIES:EMBY"))
        assertNull(HomeRowKey.parse("NEW_SERIES"))
        assertNull(HomeRowKey.parse("UPCOMING:EMBY"))
        assertNull(HomeRowKey.parse("NEW_SERIES:SEERR"))
    }

    @Test fun libraryChoicesArePerRowAndNewLibrariesAreIncludedByDefault() {
        val choice = HomeLibraryChoice().with(HomeRowKind.NEW_MOVIES, setOf("films"))
        assertTrue(choice.includes(HomeRowKind.NEW_MOVIES, "films"))
        assertFalse(choice.includes(HomeRowKind.NEW_MOVIES, "documentaries"))
        assertTrue(choice.includes(HomeRowKind.CONTINUE_WATCHING, "documentaries"))
        assertEquals(choice, HomeLibraryChoice.decode(choice.encode()))
        assertEquals(HomeLibraryChoice(), choice.with(HomeRowKind.NEW_MOVIES, null))
    }

    @Test fun theOldLibrarySelectionAppliesToEveryRow() {
        val choice = HomeLibraryChoice.fromLibrarySelection(setOf("films"))
        HomeRowKind.entries.filter { it.usesLibraries }.forEach { assertFalse(choice.includes(it, "kids")) }
        assertEquals(HomeLibraryChoice(), HomeLibraryChoice.fromLibrarySelection(null))
    }

    @Test fun hiddenRowsAreNotFetchedButContinueWatchingAlwaysIs() {
        val plan = HomeFetchPlan(
            HomeLayout.DEFAULT.withSourceVisible(ServiceKind.JELLYFIN, false),
            mapOf(ServiceKind.EMBY to HomeLibraryChoice().with(HomeRowKind.NEW_SERIES, setOf("tv"))),
        )
        assertFalse(plan.includes(ServiceKind.JELLYFIN, HomeRowKind.NEW_SERIES, "tv"))
        assertTrue(plan.includes(ServiceKind.JELLYFIN, HomeRowKind.CONTINUE_WATCHING, "tv"))
        assertTrue(plan.includes(ServiceKind.EMBY, HomeRowKind.NEW_SERIES, "tv"))
        assertFalse(plan.includes(ServiceKind.EMBY, HomeRowKind.NEW_SERIES, "anime"))
        // Releases read what any of the server's rows read.
        assertTrue(plan.includes(ServiceKind.EMBY, HomeRowKind.RECENT_RELEASES, "anime"))
        assertFalse(HomeFetchPlan(HomeLayout.DEFAULT.withVisible(HomeRowKey(HomeRowKind.RECENT_RELEASES), false))
            .includes(ServiceKind.EMBY, HomeRowKind.RECENT_RELEASES, "tv"))
    }

    @Test fun eachRowListsOnlyLibrariesThatCanFillIt() {
        assertTrue(HomeRowKind.NEW_MOVIES.accepts("movies"))
        assertFalse(HomeRowKind.NEW_MOVIES.accepts("tvshows"))
        assertFalse(HomeRowKind.NEXT_UP.accepts("movies"))
        assertTrue(HomeRowKind.CONTINUE_WATCHING.accepts("homevideos"))
        assertTrue(HomeRowKind.NEW_SERIES.accepts(null))
    }
}
