package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The peek behind the page that lists your libraries.
 *
 * It is an extra, not a page: every failure mode here has to end with an empty list and a library
 * that still opens, because the alternative is a landing page that refuses to draw because one
 * folder on the server is misconfigured.
 */
class LibraryPeekTest {
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example", "token", userId = "me")
    private val emby = ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example", "token", userId = "me")
    private val films = RemoteLibraryView("lib /1", "Filmar", "movies")

    private class Scripted(private val answers: MutableList<HttpResponse>) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return if (answers.isEmpty()) HttpResponse(500, "") else answers.removeAt(0)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("A peek only reads")
    }

    private fun ok(body: String) = mutableListOf(HttpResponse(200, body))

    @Test
    fun asksTheLatestEndpointAndTagsEveryItemWithItsLibrary() {
        val transport = Scripted(ok("""[{"Id":"m1","Name":"Moana","Type":"Movie","ProductionYear":2026}]"""))

        val items = MediaServerClient(transport).libraryPeek(jellyfin, films)

        assertEquals(listOf("m1"), items.map { it.id })
        assertEquals("lib /1", items.single().libraryId)
        val url = transport.urls.single()
        assertTrue(url, url.contains("UserItems/Latest"))
        assertTrue(url, url.contains("userId=me"))
        // The id has a space and a slash in it; neither may reach the query as itself.
        assertTrue(url, url.contains("ParentId=lib%20%2F1"))
        assertTrue(url, url.contains("Limit=14"))
    }

    /** Emby has never had the newer spelling, so the user-scoped route has to be tried too. */
    @Test
    fun embyFallsStraightToTheUserScopedRoute() {
        val transport = Scripted(ok("""[{"Id":"m1","Name":"Moana","Type":"Movie"}]"""))

        val items = MediaServerClient(transport).libraryPeek(emby, films)

        assertEquals(1, items.size)
        assertEquals(1, transport.urls.size)
        assertTrue(transport.urls.single(), transport.urls.single().contains("Users/me/Items/Latest"))
    }

    /** A build with neither Latest route still has the ordinary item query. */
    @Test
    fun aServerWithoutLatestIsAskedTheOrdinaryQuery() {
        val transport = Scripted(mutableListOf(
            HttpResponse(404, ""),
            HttpResponse(404, ""),
            HttpResponse(200, """{"Items":[{"Id":"m2","Name":"Green Zone","Type":"Movie"}]}"""),
        ))

        val items = MediaServerClient(transport).libraryPeek(jellyfin, films)

        assertEquals(listOf("m2"), items.map { it.id })
        assertEquals(3, transport.urls.size)
        val last = transport.urls.last()
        assertTrue(last, last.contains("SortBy=DateCreated"))
        // Seasons and episodes are not what "newest in this library" means to a reader standing in
        // front of a shelf: they want the film or the series, not episode 3 of something.
        assertTrue(last, last.contains("ExcludeItemTypes=Season,Episode,BoxSet"))
    }

    @Test
    fun aLibraryThatFailsEverywhereAnswersWithNothingRatherThanThrowing() {
        val transport = Scripted(mutableListOf())

        assertEquals(emptyList<RemoteLibraryItem>(), MediaServerClient(transport).libraryPeek(jellyfin, films))
    }

    /** No profile, no peek — and still no exception for the page that called it. */
    @Test
    fun aConnectionWithoutAProfileAsksNothingAtAll() {
        val transport = Scripted(mutableListOf())
        val anonymous = jellyfin.copy(userId = "")

        assertEquals(emptyList<RemoteLibraryItem>(), MediaServerClient(transport).libraryPeek(anonymous, films))
    }

    @Test
    fun theLimitIsTheCallersToChoose() {
        val transport = Scripted(ok("[]"))

        MediaServerClient(transport).libraryPeek(jellyfin, films, limit = 4)

        assertTrue(transport.urls.first(), transport.urls.first().contains("Limit=4"))
    }
}
