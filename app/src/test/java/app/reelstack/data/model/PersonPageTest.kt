package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonPageTest {
    private fun media(id: Int, status: Int?, year: String) = DiscoverMedia("seerr-movie-$id", "T$id", year, 0,
        inLibrary = status == 5, remoteId = id, mediaType = "movie", seerrStatus = status)

    @Test fun ownedTitlesGoFirstAndTheRestNewestFirst() {
        val (owned, more) = splitCredits(listOf(media(1, 5, "2019"), media(2, null, "2024"), media(3, 4, "2021"), media(4, 1, "2026")))
        assertEquals(listOf(1, 3), owned.map { it.remoteId })
        assertEquals(listOf(4, 2), more.map { it.remoteId })
    }
}
