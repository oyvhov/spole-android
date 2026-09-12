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

    /**
     * An episode the server never matched keeps the file's name. That is not a title, and printing
     * it beside the number it already contains tells the reader nothing at all.
     */
    @Test
    fun `a release filename is not treated as a title`() {
        assertEquals("", episodeTitle("S17 E01 · 71.Grader.Nord.Kjendis.S17E01.NORWEGiAN", 1))
        assertEquals("", episodeTitle("S01 E04 · Show.Name.S01E04.1080p.WEB-DL", 4))
    }

    /** All three marks are required, so a real title with full stops in it survives. */
    @Test
    fun `a title that merely contains dots is kept`() {
        assertEquals("S.W.A.T.", episodeTitle("S01 E01 · S.W.A.T.", 1))
        assertEquals("Dr. No", episodeTitle("S01 E01 · Dr. No", 1))
        assertEquals("Part.One.S01E01", episodeTitle("S01 E01 · Part.One.S01E01", 1))
    }

    @Test
    fun `without a number nothing is stripped`() {
        assertEquals("Episode 9 - Getaway Sticks", episodeTitle("S19 E09 · Episode 9 - Getaway Sticks", null))
    }

    /**
     * An empty SeriesName is not a name. Taking it at face value left a card on the shelf with no
     * title at all — only its episode number underneath. The fallback is the episode's own name,
     * cleaned the same way the line under it is cleaned: the card already prints "Sesong 19 - Ep 9"
     * directly below, so a title that opens with "Episode 9 - " says it twice.
     */
    @Test
    fun `a blank series name falls back to the episode's name without its number`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"e1","Name":"Episode 9 - Getaway Sticks","SeriesName":"","Type":"Episode",
                "ParentIndexNumber":19,"IndexNumber":9}]""",
        ).single()
        assertEquals("Getaway Sticks", item.title)
    }

    /**
     * Cleaning must never empty a card. An episode named only for its number, and one that kept a
     * release filename, both clean away to nothing — and nothing is worse than the raw name.
     */
    @Test
    fun `a name that cleans away to nothing is kept as it was`() {
        val numbered = ServicePayloadParser.libraryItems(
            """[{"Id":"e1","Name":"Episode 2","Type":"Episode","ParentIndexNumber":22,"IndexNumber":2}]""",
        ).single()
        assertEquals("Episode 2", numbered.title)
        val filename = ServicePayloadParser.libraryItems(
            """[{"Id":"e2","Name":"71.Grader.Nord.Kjendis.S17E01.NORWEGiAN","Type":"Episode",
                "ParentIndexNumber":17,"IndexNumber":1}]""",
        ).single()
        assertEquals("71.Grader.Nord.Kjendis.S17E01.NORWEGiAN", filename.title)
    }

    /**
     * A name whose spacing is not the spacing the regex expects.
     *
     * A non-breaking space is not matched by `\s`, and on screen it is the same character — so a
     * server that writes "Episode 1" defeated the strip and the row printed "1 · Episode 1",
     * with the number in it twice. Padding and doubled spaces go the same way.
     */
    @Test
    fun `odd spacing in the number still counts as the number`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"e1","Name":"Episode 1","SeriesName":"Alone Australia","Type":"Episode",
                "ParentIndexNumber":3,"IndexNumber":1}]""",
        ).single()
        assertEquals("", episodeTitle(item.subtitle, item.episode))
        assertEquals("", episodeTitle("S03 E01 · Episode  1", 1))
        assertEquals("", episodeTitle("S03 E01 ·  Episode 01 ", 1))
        // A real name that merely starts the same way is still a name. The number is only a prefix
        // when it is the whole of the name or a separator follows it.
        assertEquals("Episode 1 and a half", episodeTitle("S03 E01 · Episode 1 and a half", 1))
        assertEquals("Getaway Sticks", episodeTitle("S19 E09 · Episode 9 - Getaway Sticks", 9))
    }

    /** A film has no episode number, so there is no prefix to take off and nothing to guess at. */
    @Test
    fun `a film keeps its title exactly`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"m1","Name":"Episode 50 First Dates","Type":"Movie","ProductionYear":2004}]""",
        ).single()
        assertEquals("Episode 50 First Dates", item.title)
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
