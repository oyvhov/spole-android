package app.reelstack.data.network

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfKinds
import app.reelstack.data.model.SmartShelfPresets
import app.reelstack.data.model.SmartShelfSort
import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A smart shelf asks the server: tagged titles first, the genres after them, each title once. */
class SmartShelfQueryTest {
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

    private fun movie(id: String, name: String, type: String = "Movie") = """{"Id":"$id","Name":"$name","Type":"$type"}"""
    private fun items(vararg entries: String) = """{"Items":[${entries.joinToString(",")}]}"""

    private val transport = Scripted { url ->
        when {
            "Items/Filters" in url -> """{"Genres":["Horror","Drama","horror"],"Tags":["halloween","christmas calendar","Christmas"]}"""
            "Tags=halloween" in url -> items(movie("hocus", "Hocus Pocus"), movie("scream", "Scream"))
            "Genres=Horror|Skrekk" in url -> items(movie("scream", "Scream"), movie("shining", "The Shining"))
            "Tags=christmas" in url -> items(movie("elf", "Elf"))
            else -> null
        }
    }

    @Test fun taggedTitlesComeFirstAndEachTitleOnce() {
        val shelf = MediaServerClient(transport = transport, deviceId = "d").smartShelf(jellyfin, SmartShelfPresets.halloween)
        assertEquals(listOf("hocus", "scream", "shining"), shelf.map { it.id })
        assertEquals(2, transport.requested.size)
        assertTrue(transport.requested.all { "Recursive=true" in it && "IncludeItemTypes=Movie,Series" in it && "ParentId" !in it })
    }

    @Test fun aChildsHalloweenNeverAsksForHorror() {
        val shelf = MediaServerClient(transport = transport, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.forViewer(child = true))
        assertEquals(listOf("hocus", "scream"), shelf.map { it.id })
        assertFalse(transport.requested.any { "Genres=" in it })
    }

    /** «Berre seriar» and «Berre det du ikkje har sett» are part of both questions, not a filter afterwards. */
    @Test fun kindsAndUnwatchedNarrowBothQueries() {
        MediaServerClient(transport = transport, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(kinds = SmartShelfKinds.SERIES, unwatched = true))
        assertEquals(2, transport.requested.size)
        assertTrue(transport.requested.all { "IncludeItemTypes=Series&" in it && "IsPlayed=false" in it })
    }

    /** The shelf's own order is asked of the server, and the library page's sort plays no part. */
    @Test fun theShelfsOwnOrderIsAskedOfTheServer() {
        MediaServerClient(transport = transport, deviceId = "d").smartShelf(jellyfin, SmartShelfPresets.halloween)
        assertTrue(transport.requested.all { "SortBy=SortName&SortOrder=Ascending" in it })
        transport.requested.clear()
        MediaServerClient(transport = transport, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(sort = SmartShelfSort.ADDED))
        assertTrue(transport.requested.all { "SortBy=DateCreated,SortName&SortOrder=Descending" in it })
        assertTrue(transport.requested.none { it.split("SortBy=").size > 2 })
    }

    /** «A tag or a genre» is two sorted answers; together they are sorted again, not tag-first. */
    @Test fun twoAnswersAreSortedTogether() {
        val dated = Scripted { url ->
            when {
                "Tags=halloween" in url -> items("""{"Id":"old","Name":"Old","Type":"Movie","DateCreated":"2020-01-01T00:00:00Z"}""")
                "Genres=Horror|Skrekk" in url -> items("""{"Id":"new","Name":"New","Type":"Movie","DateCreated":"2026-10-01T00:00:00Z"}""")
                else -> null
            }
        }
        val added = MediaServerClient(transport = dated, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(sort = SmartShelfSort.ADDED))
        assertEquals(listOf("new", "old"), added.map { it.id })
        val rule = MediaServerClient(transport = dated, deviceId = "d").smartShelf(jellyfin, SmartShelfPresets.halloween)
        assertEquals(listOf("old", "new"), rule.map { it.id })
    }

    /** A shelf is drawn from its last answer at once; asking only the cache sends nothing. */
    @Test fun theLastAnswerIsKeptAndCanBeReadWithoutAsking() {
        val memory = object : ShelfAnswerCache {
            val entries = mutableMapOf<String, String>()
            override fun read(key: String) = entries[key]
            override fun write(key: String, body: String) { entries[key] = body }
            override fun clear() = entries.clear()
        }
        val client = MediaServerClient(transport = transport, deviceId = "d", shelfCache = memory)
        assertTrue(client.smartShelf(jellyfin, SmartShelfPresets.halloween, cachedOnly = true).isEmpty())
        assertTrue(transport.requested.isEmpty())
        val fresh = client.smartShelf(jellyfin, SmartShelfPresets.halloween)
        val asked = transport.requested.size
        assertEquals(fresh.map { it.id }, client.smartShelf(jellyfin, SmartShelfPresets.halloween, cachedOnly = true).map { it.id })
        assertEquals(asked, transport.requested.size)
        // Another server's answers are its own.
        assertTrue(client.smartShelf(jellyfin.copy(baseUrl = "https://other.example"), SmartShelfPresets.halloween, cachedOnly = true).isEmpty())
    }

    /** The server ands its filters: «a tag and a genre» is one question with both in it. */
    @Test fun aTagAndAGenreAreAskedTogether() {
        val shelf = MediaServerClient(transport = transport, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(matchAll = true))
        assertEquals(listOf("hocus", "scream"), shelf.map { it.id })
        val asked = transport.requested.single()
        assertTrue("Tags=halloween&" in asked && "Genres=Horror|Skrekk&" in asked)
    }

    @Test fun aChildsHalloweenWithBothStillAsksForTheTagOnly() {
        MediaServerClient(transport = transport, deviceId = "d")
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(matchAll = true).forViewer(child = true))
        assertTrue(transport.requested.single().let { "Tags=halloween" in it && "Genres=" !in it })
    }

    @Test fun aShelfWithoutARuleAsksNothing() {
        assertTrue(MediaServerClient(transport = transport, deviceId = "d").smartShelf(jellyfin, SmartShelf("empty")).isEmpty())
        assertTrue(transport.requested.isEmpty())
    }

    @Test fun aPeekAsksForAHandful() {
        MediaServerClient(transport = transport, deviceId = "d").smartShelf(jellyfin, SmartShelfPresets.christmas, limit = 4)
        assertTrue(transport.requested.single().contains("Limit=4&"))
    }

    @Test fun embyIsAskedOnItsUserRouteFirst() {
        val emby = jellyfin.copy(kind = ServiceKind.EMBY, name = "Emby")
        MediaServerClient(transport = transport, deviceId = "d").smartShelf(emby, SmartShelfPresets.christmas)
        assertTrue(transport.requested.first().contains("/Users/u1/Items?"))
        assertTrue(transport.requested.first().contains("Tags=christmas"))
    }

    /**
     * The builder offers the server's own genres and tags, each once and in alphabetical order.
     * They are asked per library: the same question about the whole server reads every title.
     */
    @Test fun theBuilderGetsEveryGenreAndTagAcrossTheLibraries() {
        val libraries = Scripted { url ->
            when {
                "UserViews" in url -> """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"},
                    {"Id":"shows","Name":"Seriar","CollectionType":"tvshows"},{"Id":"music","Name":"Musikk","CollectionType":"music"}]}"""
                "ParentId=movies" in url -> """{"Genres":["Horror","Drama"],"Tags":["halloween","christmas calendar"]}"""
                "ParentId=shows" in url -> """{"Genres":["drama","Comedy"],"Tags":["Christmas"]}"""
                else -> null
            }
        }
        val facets = MediaServerClient(transport = libraries, deviceId = "d").catalogueFacets(jellyfin)
        assertEquals(listOf("Comedy", "Drama", "Horror"), facets.genres)
        assertEquals(listOf("Christmas", "christmas calendar", "halloween"), facets.tags)
        val filters = libraries.requested.filter { "Items/Filters" in it }
        assertEquals(2, filters.size)
        assertTrue(filters.all { "userId=u1" in it && "IncludeItemTypes=Movie,Series" in it && "Recursive=true" in it })
        assertTrue(libraries.requested.none { "ParentId=music" in it })
    }

    /** One library that does not answer leaves the others' genres; none answering is an error, not an empty list. */
    @Test fun aSlowLibraryIsLeftOutAndNoAnswerAtAllIsAnError() {
        val partial = Scripted { url ->
            when {
                "UserViews" in url -> """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"},
                    {"Id":"shows","Name":"Seriar","CollectionType":"tvshows"}]}"""
                "ParentId=movies" in url -> """{"Genres":["Horror"],"Tags":[]}"""
                else -> null
            }
        }
        assertEquals(listOf("Horror"), MediaServerClient(transport = partial, deviceId = "d").catalogueFacets(jellyfin).genres)
        val none = Scripted { url -> if ("UserViews" in url) """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"}]}""" else null }
        val failed = runCatching { MediaServerClient(transport = none, deviceId = "d").catalogueFacets(jellyfin) }
        assertTrue(failed.isFailure)
    }

    /** A server without the legacy filters list answers the newer one, or at least the plain genre list. */
    @Test fun aServerWithoutTheLegacyFiltersIsAskedInTheOtherWays() {
        fun views(url: String) = if ("UserViews" in url) """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"}]}""" else null
        val newer = Scripted { url -> views(url) ?: if ("Items/Filters2" in url)
            """{"Genres":[{"Name":"Horror","Id":"1"},{"Name":"Drama","Id":"2"}],"Tags":["halloween"]}""" else null }
        val viaNewer = MediaServerClient(transport = newer, deviceId = "d").catalogueFacets(jellyfin)
        assertEquals(listOf("Drama", "Horror"), viaNewer.genres)
        assertEquals(listOf("halloween"), viaNewer.tags)

        val plain = Scripted { url -> views(url) ?: if (url.contains("Genres?")) """{"Items":[{"Name":"Comedy"},{"Name":"Action"}]}""" else null }
        val viaGenres = MediaServerClient(transport = plain, deviceId = "d").catalogueFacets(jellyfin)
        assertEquals(listOf("Action", "Comedy"), viaGenres.genres)
        assertTrue(viaGenres.tags.isEmpty())
    }

    /** Emby gives its genres through the newer filters and its tags through its own tag list. */
    @Test fun embyTagsComeFromItsTagList() {
        val emby = Scripted { url ->
            when {
                "UserViews" in url -> """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"}]}"""
                "Items/Filters2" in url -> """{"Genres":[{"Name":"Horror","Id":"1"}],"Tags":[]}"""
                "/Tags?" in url -> """{"Items":[{"Name":"halloween"},{"Name":"witch"}]}"""
                else -> null
            }
        }
        val facets = MediaServerClient(transport = emby, deviceId = "d").catalogueFacets(jellyfin)
        assertEquals(listOf("Horror"), facets.genres)
        assertEquals(listOf("halloween", "witch"), facets.tags)
    }

    /** The library filter on Emby gets its genres the same way, so «Sjanger» is offered there too. */
    @Test fun theLibraryFilterFindsEmbyGenres() {
        val emby = Scripted { url -> if ("Items/Filters2" in url) """{"Genres":[{"Name":"Drama","Id":"2"}],"Years":[2024]}""" else null }
        val facets = MediaServerClient(transport = emby, deviceId = "d").libraryFacets(jellyfin.copy(kind = ServiceKind.EMBY), "films")
        assertEquals(listOf("Drama"), facets.genres)
        assertEquals(listOf("2024"), facets.years)
        assertTrue(emby.requested.none { "/Tags?" in it })
    }

    /** What went wrong travels with the failure, so the builder can show more than «it did not work». */
    @Test fun aFailureSaysWhichQuestionWasRefused() {
        val refused = Scripted { url -> if ("UserViews" in url) """{"Items":[{"Id":"movies","Name":"Filmar","CollectionType":"movies"}]}""" else null }
        val message = runCatching { MediaServerClient(transport = refused, deviceId = "d").catalogueFacets(jellyfin) }
            .exceptionOrNull()?.message.orEmpty()
        assertTrue(message, "HTTP 404" in message)
    }

    /** The shelf's big questions go through the patient transport, so a slow catalogue is not read as an empty one. */
    @Test fun shelfQuestionsUseThePatientTransport() {
        val patient = Scripted { url -> if ("Tags=halloween" in url) items(movie("hocus", "Hocus Pocus")) else null }
        val quick = Scripted { null }
        val shelf = MediaServerClient(transport = quick, deviceId = "d", patientTransport = patient)
            .smartShelf(jellyfin, SmartShelfPresets.halloween.copy(genres = emptyList()))
        assertEquals(listOf("hocus"), shelf.map { it.id })
        assertTrue(quick.requested.isEmpty())
    }
}
