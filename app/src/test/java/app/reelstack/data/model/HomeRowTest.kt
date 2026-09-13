package app.reelstack.data.model

import org.junit.Assert.*
import org.junit.Test

class HomeRowTest {
    @Test fun missingOrderPreservesExistingDefault() {
        assertEquals(HomeRow.entries, decodeHomeRowOrder(null))
        assertEquals(HomeRow.entries, decodeHomeRowOrder(""))
    }
    @Test fun unknownAndDuplicateRowsAreIgnoredAndNewRowsAppended() {
        val order = decodeHomeRowOrder("UPCOMING,OBSOLETE,FAVOURITES,UPCOMING")
        assertEquals(listOf(HomeRow.UPCOMING, HomeRow.FAVOURITES), order.take(2))
        assertEquals(HomeRow.entries.toSet(), order.toSet())
        assertEquals(HomeRow.entries.size, order.size)
        assertEquals(order, decodeHomeRowOrder(order.joinToString(",") { it.name }))
    }
    @Test fun moveBothDirectionsAndBoundaries() {
        val original = HomeRow.entries
        val moved = moveHomeRow(original, HomeRow.FAVOURITES, -1, original)
        assertEquals(HomeRow.FAVOURITES, moved[1])
        assertEquals(original, moveHomeRow(moved, HomeRow.FAVOURITES, 1, original))
        assertEquals(original, moveHomeRow(original, original.first(), -1, original))
        assertEquals(original, moveHomeRow(original, original.last(), 1, original))
    }
    @Test fun absentServiceAndCombinedNextUpKeepTheirSlots() {
        val original = HomeRow.entries
        val available = original.filter { it != HomeRow.NEXT_UP && it != HomeRow.EMBY_MOVIES }
        val moved = moveHomeRow(original, HomeRow.JELLYFIN_MOVIES, -1, available)
        assertEquals(HomeRow.JELLYFIN_MOVIES, moved[original.indexOf(HomeRow.FAVOURITES)])
        assertEquals(HomeRow.NEXT_UP, moved[original.indexOf(HomeRow.NEXT_UP)])
        assertEquals(HomeRow.EMBY_MOVIES, moved[original.indexOf(HomeRow.EMBY_MOVIES)])
        assertEquals(original.toSet(), moved.toSet())
    }
}
