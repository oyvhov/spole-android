package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.LibraryFilters
import app.reelstack.data.model.LibraryWatched
import app.reelstack.data.model.Season
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.shelf
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A season's shelf: tagged titles first, the genre after them, each title once. */
class SeasonShelfQueryTest {
    private class Scripted(private val answer: (String) -> String?) : JsonHttpTransport {
        val requested = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            requested += URLDecoder.decode(url, "UTF-8")
            return answer(requested.last())?.let { HttpResponse(200, it) } ?: HttpResponse(404, "")
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String) = HttpResponse(204, "")
        override fun delete(url: String, headers: Map<String, String>) = HttpResponse(204, "")
    }

    private val jellyfin = ServiceConnection(kind = ServiceKind.JELLYFIN, name = "Jellyfin", baseUrl = "https://media.example",
        token = "t", userId = "u1", state = ConnectionState.CONNECTED)

    private fun movie(id: String, name: String) = """{"Id":"$id","Name":"$name","Type":"Movie"}"""
    private fun items(vararg entries: String) = """{"Items":[${entries.joinToString(",")}]}"""

    private val transport = Scripted { url ->
        when {
            "Tags=halloween" in url -> items(movie("hocus", "Hocus Pocus"), movie("scream", "Scream"))
            "Genres=Horror|Skrekk" in url -> items(movie("scream", "Scream"), movie("shining", "The Shining"))
            "Tags=christmas" in url -> items(movie("elf", "Elf"))
            else -> null
        }
    }

    @Test fun taggedTitlesComeFirstAndEachTitleOnce() {
        val shelf = MediaServerClient(transport = transport, deviceId = "d")
            .seasonShelf(jellyfin, Season.HALLOWEEN.shelf(kids = false)!!)
        assertEquals(listOf("hocus", "scream", "shining"), shelf.map { it.id })
        assertEquals(2, transport.requested.size)
        assertTrue(transport.requested.all { "Recursive=true" in it && "IncludeItemTypes=Movie,Series" in it && "ParentId" !in it })
    }

    @Test fun aChildsShelfNeverAsksForTheGenre() {
        val shelf = MediaServerClient(transport = transport, deviceId = "d")
            .seasonShelf(jellyfin, Season.HALLOWEEN.shelf(kids = true)!!)
        assertEquals(listOf("hocus", "scream"), shelf.map { it.id })
        assertFalse(transport.requested.any { "Genres=" in it })
    }

    /** The library page's own filters still work on a season's shelf. */
    @Test fun theLibraryFiltersApplyToBothQueries() {
        MediaServerClient(transport = transport, deviceId = "d").seasonShelf(jellyfin,
            Season.HALLOWEEN.shelf(kids = false)!!, LibraryFilters(watched = LibraryWatched.UNWATCHED))
        assertTrue(transport.requested.all { "IsPlayed=false" in it })
    }

    @Test fun embyIsAskedOnItsUserRouteFirst() {
        val emby = jellyfin.copy(kind = ServiceKind.EMBY, name = "Emby")
        MediaServerClient(transport = transport, deviceId = "d").seasonShelf(emby, Season.CHRISTMAS.shelf(kids = false)!!)
        assertTrue(transport.requested.first().contains("/Users/u1/Items?"))
        assertTrue(transport.requested.first().contains("Tags=christmas"))
    }
}
