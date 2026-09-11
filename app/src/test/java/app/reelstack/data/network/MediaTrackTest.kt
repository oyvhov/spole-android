package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a title page can say about a file before anything is played.
 *
 * "Can we watch this in Norwegian" was previously only answerable by starting the film and opening
 * the player's own menus. The index is the part that has to survive: it is the server's own stream
 * number, and playback is asked for by number, so a label without its index would be a promise the
 * player could not keep.
 */
class MediaTrackTest {

    private fun details(payload: String) = ServicePayloadParser.libraryDetails(payload)

    private val twoLanguages = """
        {"Id":"film-1","Name":"Dune","Type":"Movie","RunTimeTicks":36000000000,
         "MediaStreams":[
           {"Type":"Video","Index":0,"Width":3840,"Height":2160,"Codec":"hevc"},
           {"Type":"Audio","Index":1,"DisplayTitle":"Norsk - AC3 5.1","Language":"nor","DisplayLanguage":"Norsk","IsDefault":true},
           {"Type":"Audio","Index":2,"DisplayTitle":"English - DTS 5.1","Language":"eng","DisplayLanguage":"English"},
           {"Type":"Subtitle","Index":3,"DisplayTitle":"Norsk","Language":"nor","DisplayLanguage":"Norsk"},
           {"Type":"Subtitle","Index":4,"DisplayTitle":"English (forced)","Language":"eng","IsForced":true}
         ]}
    """.trimIndent()

    @Test
    fun `audio streams keep the server's own index`() {
        val audio = details(twoLanguages).audioTracks
        assertEquals(listOf(1, 2), audio.map { it.index })
        assertEquals("Norsk - AC3 5.1", audio.first().label)
    }

    /** The default is what starts when nobody chooses, so the page has to know which one it is. */
    @Test
    fun `the default audio track is marked`() {
        val audio = details(twoLanguages).audioTracks
        assertTrue(audio.first().isDefault)
        assertFalse(audio.last().isDefault)
    }

    @Test
    fun `subtitles are listed with their forced flag`() {
        val subtitles = details(twoLanguages).subtitleTracks
        assertEquals(listOf(3, 4), subtitles.map { it.index })
        assertTrue(subtitles.last().forced)
    }

    /** An untagged stream still has to be nameable, or the chip would be blank. */
    @Test
    fun `a stream without a title falls back to its language and then its number`() {
        val tracks = details(
            """{"Id":"f","Name":"N","Type":"Movie","MediaStreams":[
                 {"Type":"Audio","Index":1,"DisplayLanguage":"Svensk"},
                 {"Type":"Audio","Index":2}]}""",
        ).audioTracks
        assertEquals("Svensk", tracks.first().label)
        assertEquals("Audio 3", tracks.last().label)
    }

    @Test
    fun `streams nested under a media source are read the same way`() {
        val tracks = details(
            """{"Id":"f","Name":"N","Type":"Movie","MediaSources":[{"Id":"s1","MediaStreams":[
                 {"Type":"Audio","Index":1,"DisplayTitle":"Norsk"}]}]}""",
        ).audioTracks
        assertEquals("Norsk", tracks.single().label)
    }

    /** One file is just "the file"; naming it would be a choice that is not a choice. */
    @Test
    fun `a single media source produces no version list`() {
        val single = details("""{"Id":"f","Name":"N","Type":"Movie","MediaSources":[{"Id":"s1","Name":"Bluray-1080p"}]}""")
        assertTrue(single.versions.isEmpty())
    }

    @Test
    fun `two media sources are offered as versions`() {
        val many = details(
            """{"Id":"f","Name":"N","Type":"Movie","MediaSources":[
                 {"Id":"s1","Name":"Bluray-1080p"},{"Id":"s2","Name":"Remux-2160p"}]}""",
        )
        assertEquals(listOf("Bluray-1080p", "Remux-2160p"), many.versions)
    }

    @Test
    fun `a file without streams reports none rather than inventing one`() {
        val empty = details("""{"Id":"f","Name":"N","Type":"Movie"}""")
        assertTrue(empty.audioTracks.isEmpty())
        assertTrue(empty.subtitleTracks.isEmpty())
    }

    /** The two flags the detail page turns into "Mark as watched" and "Remove from favourites". */
    @Test
    fun `the profile's own flags come back with the details`() {
        val watched = details("""{"Id":"f","Name":"N","Type":"Movie","UserData":{"Played":true,"IsFavorite":true}}""")
        assertTrue(watched.played)
        assertTrue(watched.favourite)
        val fresh = details("""{"Id":"f","Name":"N","Type":"Movie","UserData":{"Played":false}}""")
        assertFalse(fresh.played)
        assertFalse(fresh.favourite)
    }

    /** A library page filters its shelf on this, so an untagged item must not claim a library. */
    @Test
    fun `a library item carries no library until a caller assigns one`() {
        val item = ServicePayloadParser.libraryItems("""[{"Id":"i1","Name":"N","Type":"Movie"}]""").single()
        assertEquals(null, item.libraryId)
        assertEquals("lib-7", item.copy(libraryId = "lib-7").libraryId)
    }

    @Test
    fun `favourite and played reach the library item too`() {
        val item = ServicePayloadParser.libraryItems(
            """[{"Id":"i1","Name":"N","Type":"Movie","UserData":{"IsFavorite":true,"Played":true}}]""",
        ).single()
        assertTrue(item.favourite)
        assertTrue(item.played)
    }
}
