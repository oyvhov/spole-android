package app.reelstack.data.network

import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Test

class LibraryFiltersTest {
    @Test fun filtersStayOnServerScopeAndSurvivePagination() {
        val urls = mutableListOf<String>()
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse { urls += url; return HttpResponse(200, "{\"Items\":[]}") }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("Read only")
        }
        val connection = ServiceConnection(ServiceKind.JELLYFIN, "test", "https://example.com", "fixture", userId = "me")
        MediaServerClient(transport).browseLibrary(connection, "selected", 60, "movies",
            LibraryFilters(LibrarySort.RATING, true, LibraryWatched.UNWATCHED, true, LibraryResolution.UHD, "a&ParentId=evil", "Drama", "2026"))
        val url = urls.single()
        listOf("ParentId=selected", "userId=me", "StartIndex=60", "IncludeItemTypes=Movie", "IsPlayed=false",
            "IsFavorite=true", "MinWidth=3840", "Years=2026", "Genres=Drama", "SortOrder=Descending").forEach { assertTrue(it, url.contains(it)) }
        assertFalse(url.contains("&ParentId=evil"))
    }
    @Test fun invalidYearsNeverEnterRequestsAndProgressIsDistinctFromUnwatched() {
        assertFalse(LibraryFilters(year = "hello").query().contains("Years="))
        assertFalse(LibraryFilters(year = "123").query().contains("Years="))
        val progress = LibraryFilters(watched = LibraryWatched.IN_PROGRESS).query()
        assertTrue(progress.contains("Filters=IsResumable")); assertFalse(progress.contains("IsPlayed"))
    }
    @Test fun resolutionBoundariesDoNotOverlap() {
        assertTrue(LibraryFilters(resolution = LibraryResolution.SD).query().contains("MaxWidth=1279"))
        val hd = LibraryFilters(resolution = LibraryResolution.HD).query()
        assertTrue(hd.contains("MinWidth=1280") && hd.contains("MaxWidth=3839"))
        assertFalse(LibraryFilters().query().contains("Width"))
    }
}
