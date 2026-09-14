package app.reelstack.data.model

import org.junit.Assert.*
import org.junit.Test

class SeasonEpisodesTest {
    private fun episode(id: String, number: Int?, season: Int = 19, played: Boolean = false) =
        LibraryMedia(id, "Show", "Episode", artworkRes = 0, source = ServiceKind.JELLYFIN,
            season = season, episode = number, played = played)
    @Test fun watchedEpisodesStayInSequenceAndSpecialsStayInTheirSeason() {
        val result = orderedSeasonEpisodes(listOf(episode("special", 221, 0), episode("9", 9),
            episode("1", 1, played = true), episode("2", 2, played = true)), 19)
        assertEquals(listOf("1", "2", "9"), result.map { it.id })
    }
    @Test fun unknownNumbersGoLastAndDuplicatesDisappear() {
        val first = episode("first", 1)
        assertEquals(listOf("first", "unknown"), orderedSeasonEpisodes(
            listOf(episode("unknown", null), first, first), 19).map { it.id })
    }
}
