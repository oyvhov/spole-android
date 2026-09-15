package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class LibraryBrowserTest {
    @Test fun embyBrowserUsesStoredProfileAndKeepsPaginationScoped() {
        val transport = Recording("""{"Items":[]}""")
        val client = MediaServerClient(transport)
        val emby = connection.copy(kind = ServiceKind.EMBY)
        client.browseLibraries(emby)
        client.browseLibrary(emby, "films", 60, "movies")
        assertTrue(transport.urls[0].contains("Users/me/Views"))
        assertTrue(transport.urls[1].contains("Users/me/Items?"))
        assertTrue(transport.urls[1].contains("ParentId=films"))
        assertTrue(transport.urls[1].contains("StartIndex=60"))
        assertTrue(transport.urls[1].contains("IncludeItemTypes=Movie"))
        assertFalse(transport.urls.any { it.contains("Users/Me") })
    }

    @Test fun embyBrowserRejectsMissingProfileWithoutGuessingAnotherUser() {
        val transport = Recording("""{"Items":[]}""")
        val client = MediaServerClient(transport)
        assertThrows(IllegalArgumentException::class.java) {
            client.browseLibraries(connection.copy(kind = ServiceKind.EMBY, userId = ""))
        }
        assertTrue(transport.urls.isEmpty())
    }

    @Test fun embyFacetsRemainScopedToTheSelectedLibrary() {
        val transport = Recording("""{"Genres":["Drama"],"Years":[2024,2020]}""")
        val facets = MediaServerClient(transport).libraryFacets(connection.copy(kind = ServiceKind.EMBY), "films")
        assertEquals(listOf("Drama"), facets.genres)
        assertTrue(transport.urls.single().contains("ParentId=films"))
        assertTrue(transport.urls.single().contains("userId=me"))
    }
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
        assertTrue(url.contains("CollapseBoxSetItems=false"))
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
    @Test fun movieCatalogueOverridesServerGroupingWhileCollectionsRemainBrowsable() {
        val urls = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                urls += url
                // A server with grouping enabled substitutes a BoxSet unless explicitly disabled.
                val collection = url.contains("IncludeItemTypes=BoxSet") || !url.contains("CollapseBoxSetItems=false")
                return HttpResponse(200, if (collection)
                    """{"Items":[{"Id":"set","Name":"Collection","Type":"BoxSet"}]}"""
                else """{"Items":[{"Id":"film","Name":"Film","Type":"Movie"}]}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Unexpected write")
        }
        val client = MediaServerClient(transport)
        assertEquals("Movie", client.browseLibrary(connection, "films", collectionType = "movies").single().mediaType)
        assertEquals("BoxSet", client.browseLibrary(connection, "collections", collectionType = "boxsets").single().mediaType)
        assertEquals("Movie", client.browseLibrary(connection, "set").single().mediaType)
        assertTrue(urls[0].contains("ExcludeItemTypes=BoxSet"))
        assertFalse(urls[1].contains("ExcludeItemTypes=BoxSet"))
        assertTrue(urls.all { it.contains("userId=me") })
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
