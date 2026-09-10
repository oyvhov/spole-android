package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class LibraryBrowserTest {
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example", "token", userId = "me")
    private class Recording(val body: String, val status: Int = 200) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            assertTrue(headers.isNotEmpty())
            return HttpResponse(status, body)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
    }
    @Test fun fullBrowserIncludesChildrensAndMusicLibraries() {
        val transport = Recording("""{"Items":[{"Id":"kids","Name":"Barneseriar","CollectionType":"tvshows"},{"Id":"music","Name":"Music","CollectionType":"music"}]}""")
        assertEquals(listOf("kids", "music"), MediaServerClient(transport).browseLibraries(connection).map { it.id })
        assertTrue(transport.urls.single().contains("userId=me"))
    }
    @Test fun secondPageRemainsScopedAndDoesNotFlattenFoldersOrLimitMediaTypes() {
        val transport = Recording("""{"Items":[{"Id":"season","Name":"Season 1","SeriesName":"Show","Type":"Season","IsFolder":true}]}""")
        val entries = MediaServerClient(transport).browseLibrary(connection, "parent /?", 60)
        val url = transport.urls.single()
        assertTrue(url.contains("StartIndex=60"))
        assertTrue(url.contains("Limit=60"))
        assertTrue(url.contains("Recursive=false"))
        assertTrue(url.contains("userId=me"))
        assertFalse(url.contains("IncludeItemTypes"))
        assertFalse(url.contains("parent /?"))
        assertTrue(entries.single().isFolder)
        assertEquals("Season 1", entries.single().title)
    }
    @Test fun deniedAccessIsAnErrorAndDoesNotFallBackToUnscopedItems() {
        val transport = Recording("{}", 403)
        assertThrows(Exception::class.java) { MediaServerClient(transport).browseLibrary(connection, "private") }
        assertEquals(1, transport.urls.size)
    }
    @Test fun ordinaryMediaRemainsALeafAndKeepsPlaybackProgress() {
        val entry = ServicePayloadParser.libraryItems("""{"Items":[{"Id":"movie","Name":"Film","Type":"Movie","UserData":{"PlayedPercentage":42}}]}""").single()
        assertFalse(entry.isFolder)
        assertEquals(.42f, entry.progress!!, .001f)
    }
    @Test fun movieAndSeriesCatalogueShowsMetadataTitlesInsteadOfFilesystemFolders() {
        val transport = Recording("""{"Items":[]}""")
        val client = MediaServerClient(transport)
        client.browseLibrary(connection, "movie-root", collectionType = "movies")
        client.browseLibrary(connection, "show-root", 60, "tvshows")
        assertTrue(transport.urls[0].contains("Recursive=true"))
        assertTrue(transport.urls[0].contains("IncludeItemTypes=Movie"))
        assertTrue(transport.urls[1].contains("IncludeItemTypes=Series"))
        assertTrue(transport.urls[1].contains("ParentId=show-root"))
        assertTrue(transport.urls[1].contains("StartIndex=60"))
        client.browseLibrary(connection, "season")
        assertTrue(transport.urls[2].contains("Recursive=false"))
        assertFalse(transport.urls[2].contains("IncludeItemTypes"))
    }
    @Test fun savedSelectionControlsNewFilmsSeriesResumeReleasesAndSearchBeforeLoading() {
        val urls = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                urls += url
                return HttpResponse(200, if (url.contains("Views"))
                    """{"Items":[{"Id":"kids","Name":"Barneseriar","CollectionType":"mixed"},{"Id":"other","Name":"Other","CollectionType":"mixed"}]}"""
                else if (url.contains("ParentId=kids"))
                    """{"Items":[{"Id":"selected","Name":"Selected title","Type":"Movie"}]}""" else """{"Items":[]}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
        }
        val client = MediaServerClient(transport, includeLibrary = { _, view -> view.id == "kids" })
        val feed = client.feed(connection, app.reelstack.data.model.ViewerAccess(false, emptyMap()))
        assertEquals("selected", feed.recentMovies.single().id)
        assertEquals("selected", feed.recentSeries.single().id)
        assertEquals("selected", feed.resume.single().id)
        assertEquals("selected", client.search(connection, "title").single().id)
        assertFalse(urls.any { it.contains("ParentId=other") })
        assertTrue(urls.any { it.contains("Latest") && it.contains("ParentId=kids") })
        assertTrue(urls.any { it.contains("Resume") && it.contains("ParentId=kids") })
    }
    @Test fun selectingNoneSkipsAllLibraryContentRequests() {
        val transport = Recording("""{"Items":[{"Id":"movies","Name":"Movies","CollectionType":"movies"}]}""")
        val client = MediaServerClient(transport, includeLibrary = { _, _ -> false })
        val feed = client.feed(connection, app.reelstack.data.model.ViewerAccess(false, emptyMap()))
        assertTrue(feed.recentMovies.isEmpty())
        assertTrue(feed.recentSeries.isEmpty())
        assertTrue(feed.resume.isEmpty())
        assertTrue(feed.releaseCandidates.isEmpty())
        assertTrue(client.search(connection, "title").isEmpty())
        assertFalse(transport.urls.any { it.contains("ParentId=") || it.contains("Latest") || it.contains("Resume") || it.contains("searchTerm=") })
    }
}
