package app.reelstack.ui

import app.reelstack.data.network.ServicePayloadParser
import app.reelstack.ui.components.episodeTitle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Turning "S19 E09 · Episode 9 - Getaway Sticks" into something a person would say.
 *
 * The screen writes the season and episode itself, so whatever the server calls the episode must
 * not repeat those numbers back. The hard case is the boundary: stripping "Episode 9" must not
 * take a bite out of "Episode 90".
 */
class EpisodeLabelTest {

    @Test
    fun `the numbered prefix comes off and the real name stays`() {
        assertEquals("Getaway Sticks", episodeTitle("S19 E09 · Episode 9 - Getaway Sticks", 9))
    }

    @Test
    fun `an episode with no name of its own leaves nothing behind`() {
        assertEquals("", episodeTitle("S03 E01 · Episode 1", 1))
    }

    @Test
    fun `a real title is left exactly as the server wrote it`() {
        assertEquals(
            "Hvilken Side Er Du På? – Del 1",
            episodeTitle("S06 E11 · Hvilken Side Er Du På? – Del 1", 11),
        )
    }

    /** The reason the match is anchored on a digit boundary rather than a plain prefix. */
    @Test
    fun `episode 9 does not eat the start of episode 90`() {
        assertEquals("Episode 90 - Finale", episodeTitle("S01 E90 · Episode 90 - Finale", 9))
        assertEquals("Finale", episodeTitle("S01 E90 · Episode 90 - Finale", 90))
    }

    @Test
    fun `a padded number is still recognised`() {
        assertEquals("Pilot", episodeTitle("S01 E01 · Episode 01 – Pilot", 1))
    }

    @Test
    fun `a line with no name part produces nothing`() {
        assertEquals("", episodeTitle("S02 E04", 4))
    }

    @Test
    fun `without a number nothing is stripped`() {
        assertEquals("Episode 9 - Getaway Sticks", episodeTitle("S19 E09 · Episode 9 - Getaway Sticks", null))
    }

    /**
     * An empty SeriesName is not a name. Taking it at face value left a card on the shelf with no
     * title at all — only its episode number underneath.
     */
    @Test
    fun `a blank series name falls back to the item's own name`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"e1","Name":"Episode 9 - Getaway Sticks","SeriesName":"","Type":"Episode",
                "ParentIndexNumber":19,"IndexNumber":9}]""",
        ).single()
        assertEquals("Episode 9 - Getaway Sticks", item.title)
    }

    @Test
    fun `a real series name is still preferred over the episode's`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"e1","Name":"Getaway Sticks","SeriesName":"Taskmaster","Type":"Episode",
                "ParentIndexNumber":19,"IndexNumber":9}]""",
        ).single()
        assertEquals("Taskmaster", item.title)
        assertEquals(19, item.season)
        assertEquals(9, item.episode)
    }
}
