package app.reelstack.data.network

import app.reelstack.data.model.AccentPalette
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.Season
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.VisualTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reading a series: its seasons, one season's episodes, and where Play should land.
 *
 * The awkward part is not the request but the empty answer. `Shows/{id}/Episodes` is the route
 * Jellyfin documents, and a server that replies to it with 200 and nothing inside would silently
 * present an empty season that the generic item query can list perfectly well — so an empty answer
 * has to count as "ask the next route", while a season that really is empty still ends up empty.
 */
class SeriesBrowseTest {

    private class Scripted(private val responses: List<Pair<String, String>>) : JsonHttpTransport {
        val requested = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            requested += url
            val body = responses.firstOrNull { url.contains(it.first) }?.second
            return if (body == null) HttpResponse(statusCode = 404, body = "")
            else HttpResponse(statusCode = 200, body = body)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(204, "")
        override fun delete(url: String, headers: Map<String, String>) = HttpResponse(204, "")
    }

    private val connection = ServiceConnection(
        kind = ServiceKind.JELLYFIN, name = "Jellyfin", baseUrl = "https://media.example",
        token = "t", userId = "u1", state = ConnectionState.CONNECTED,
    )

    private fun items(vararg entries: String) = """{"Items":[${entries.joinToString(",")}]}"""

    private val seasonOne = """{"Id":"s1","Name":"Season 1","Type":"Season","IndexNumber":1,"ChildCount":8}"""
    private val specials = """{"Id":"s0","Name":"Specials","Type":"Season","IndexNumber":0,"ChildCount":3}"""
    private val episode = """{"Id":"e1","Name":"Pilot","SeriesName":"Silo","Type":"Episode",
        "ParentIndexNumber":1,"IndexNumber":1,"RunTimeTicks":30000000000}"""

    @Test
    fun `seasons come from the show-scoped route`() {
        val transport = Scripted(listOf("Shows/series-1/Seasons" to items(specials, seasonOne)))
        val seasons = MediaServerClient(transport = transport, deviceId = "d").seasons(connection, "series-1")
        assertEquals(listOf("s0", "s1"), seasons.map { it.id })
    }

    /** A season's own number is IndexNumber, which the parser puts in `episode`. */
    @Test
    fun `a season carries its own number`() {
        val transport = Scripted(listOf("Shows/series-1/Seasons" to items(seasonOne)))
        val season = MediaServerClient(transport = transport, deviceId = "d").seasons(connection, "series-1").single()
        assertEquals(1, season.episode)
        assertEquals(8, season.childCount)
    }

    @Test
    fun `episodes come back with runtime and numbers`() {
        val transport = Scripted(listOf("Shows/series-1/Episodes" to items(episode)))
        val list = MediaServerClient(transport = transport, deviceId = "d").episodes(connection, "series-1", "s1")
        assertEquals(1, list.single().season)
        assertEquals(1, list.single().episode)
        assertEquals(50, list.single().runtimeMinutes)
    }

    /** The reason the fallback exists: 200 with nothing in it is not the same as "no episodes". */
    @Test
    fun `an empty show-scoped answer falls through to the item query`() {
        val transport = Scripted(listOf(
            "Shows/series-1/Episodes" to items(),
            "Items?" to items(episode),
        ))
        val list = MediaServerClient(transport = transport, deviceId = "d").episodes(connection, "series-1", "s1")
        assertEquals(listOf("e1"), list.map { it.id })
        assertTrue("Begge rutene skal ha vore prøvde", transport.requested.size >= 2)
    }

    @Test
    fun `a season that really is empty stays empty`() {
        val transport = Scripted(listOf("Shows/series-1/Episodes" to items(), "Items?" to items()))
        val list = MediaServerClient(transport = transport, deviceId = "d").episodes(connection, "series-1", "s1")
        assertTrue(list.isEmpty())
    }

    @Test
    fun `next up asks the server rather than guessing from the list`() {
        val transport = Scripted(listOf("Shows/NextUp" to items(episode)))
        val next = MediaServerClient(transport = transport, deviceId = "d").seriesNextUp(connection, "series-1")
        assertEquals("e1", next?.id)
        assertTrue(transport.requested.single().contains("seriesId=series-1"))
    }

    @Test
    fun `next up falls back to the resume list and then to nothing`() {
        val resume = Scripted(listOf("UserItems/Resume" to items(episode)))
        assertEquals("e1", MediaServerClient(transport = resume, deviceId = "d").seriesNextUp(connection, "series-1")?.id)
        val nothing = Scripted(listOf("Shows/NextUp" to items(), "UserItems/Resume" to items()))
        assertNull(MediaServerClient(transport = nothing, deviceId = "d").seriesNextUp(connection, "series-1"))
    }

    /**
     * A season is a pairing, not a background. Choosing one sets both halves; the mood is what
     * decides which season is running, so swapping only the accent keeps you in it.
     */
    @Test
    fun `a season sets mood and accent together`() {
        val christmas = Season.CHRISTMAS.applyTo(Personalization())
        assertEquals(VisualTheme.NOEL, christmas.visualTheme)
        assertEquals(AccentPalette.HOLLY, christmas.accent)
        assertEquals(Season.CHRISTMAS, Season.of(christmas))
        assertEquals(Season.CHRISTMAS, Season.of(christmas.copy(accent = AccentPalette.GOLD)))
    }

    @Test
    fun `all year puts the defaults back`() {
        val plain = Season.NONE.applyTo(Season.HALLOWEEN.applyTo(Personalization()))
        assertEquals(VisualTheme.FOREST, plain.visualTheme)
        assertEquals(AccentPalette.LIME, plain.accent)
        assertEquals(Season.NONE, Season.of(plain))
    }

    /**
     * The seasonal accents obey the same 7:1 the other eight do. Stated here as well as in
     * `AccentContrastTest` because this is the pair most likely to be nudged towards a poster red
     * or a burnt orange later, and that is exactly the change this catches.
     */
    @Test
    fun `the seasonal accents carry dark text at 7 to 1`() {
        listOf(AccentPalette.HOLLY, AccentPalette.PUMPKIN).forEach { palette ->
            val contrast = contrast(palette.argb, 0xFF101211)
            assertTrue("$palette gav berre $contrast", contrast >= 7.0)
        }
    }

    private fun contrast(first: Long, second: Long): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun luminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF) / 255.0
            return if (value <= 0.03928) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
