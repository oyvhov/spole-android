package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Hald fram å sjå" and library search both read the signed-in profile's own libraries, so the
 * rules that keep excluded libraries off Home have to hold here too.
 */
class ResumeAndLibrarySearchTest {
    private class Recording(private val responses: (String) -> HttpResponse) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return responses(url)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("No POST expected")
    }

    private val connection = ServiceConnection(
        ServiceKind.JELLYFIN, "Heimetenar", "https://media.example", "token", userId = "me",
    )

    private val views = """
        {"Items":[
          {"Id":"lib-movies","Name":"Filmar","CollectionType":"movies"},
          {"Id":"lib-kids","Name":"Barneseriar","CollectionType":"tvshows"},
          {"Id":"lib-series","Name":"Seriar","CollectionType":"tvshows"}
        ]}
    """.trimIndent()

    private fun item(id: String, name: String, percent: Double) = """
        {"Id":"$id","Name":"$name","Type":"Movie","UserData":{"PlayedPercentage":$percent}}
    """.trimIndent()

    @Test fun resumeSkipsTheExcludedChildrensLibrary() {
        val transport = Recording { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("Resume") && url.contains("lib-movies") ->
                    HttpResponse(200, """{"Items":[${item("m1", "Halvsett film", 42.0)}]}""")
                url.contains("Resume") && url.contains("lib-series") ->
                    HttpResponse(200, """{"Items":[${item("s1", "Halvsett episode", 10.0)}]}""")
                url.contains("Resume") -> HttpResponse(200, """{"Items":[${item("k1", "Barneserie", 5.0)}]}""")
                else -> HttpResponse(404, "")
            }
        }
        val resume = MediaServerClient(transport).resume(connection, "me")

        assertEquals(listOf("Halvsett film", "Halvsett episode"), resume.map { it.title })
        assertTrue(transport.urls.any { it.contains("lib-movies") })
        // The children's library must never be queried, not merely filtered out afterwards.
        assertFalse(transport.urls.any { it.contains("lib-kids") })
    }

    @Test fun resumeKeepsThePlaybackPositionTheServerReported() {
        val transport = Recording { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("Resume") && url.contains("lib-movies") ->
                    HttpResponse(200, """{"Items":[${item("m1", "Halvsett film", 42.0)}]}""")
                else -> HttpResponse(200, """{"Items":[]}""")
            }
        }
        val progress = MediaServerClient(transport).resume(connection, "me").single().progress
        assertEquals(0.42f, progress!!, 0.001f)
    }

    @Test fun resumeAsksForUserDataSoProgressIsPresent() {
        val transport = Recording { url ->
            if (url.contains("Views") || url.contains("UserViews")) HttpResponse(200, views)
            else HttpResponse(200, """{"Items":[]}""")
        }
        MediaServerClient(transport).resume(connection, "me")
        assertTrue(transport.urls.any { it.contains("Resume") && it.contains("EnableUserData=true") })
    }

    @Test fun aTotalResumeFailureIsReportedRatherThanShownAsAnEmptyRail() {
        val transport = Recording { url ->
            if (url.contains("Views") || url.contains("UserViews")) HttpResponse(200, views)
            else HttpResponse(500, "")
        }
        val error = runCatching { MediaServerClient(transport).resume(connection, "me") }.exceptionOrNull()
        assertEquals("Fekk ikkje henta Hald fram å sjå", error?.message)
    }

    @Test fun librarySearchEscapesTheTermAndSkipsExcludedLibraries() {
        val transport = Recording { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("searchTerm") && url.contains("lib-movies") ->
                    HttpResponse(200, """{"Items":[${item("m1", "Blade Runner 2049", 0.0)}]}""")
                else -> HttpResponse(200, """{"Items":[]}""")
            }
        }
        val hits = MediaServerClient(transport).search(connection, "blade runner")

        assertEquals(listOf("Blade Runner 2049"), hits.map { it.title })
        assertTrue(transport.urls.any { it.contains("searchTerm=blade%20runner") })
        assertFalse(transport.urls.any { it.contains("lib-kids") })
    }

    @Test fun aBlankSearchNeverReachesTheServer() {
        val transport = Recording { HttpResponse(200, """{"Items":[]}""") }
        assertTrue(MediaServerClient(transport).search(connection, "   ").isEmpty())
        assertTrue(transport.urls.isEmpty())
    }

    @Test fun libraryFeedExposesResumeAlongsideTheOtherRows() {
        val transport = Recording { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("Resume") && url.contains("lib-movies") ->
                    HttpResponse(200, """{"Items":[${item("m1", "Halvsett film", 42.0)}]}""")
                else -> HttpResponse(200, """{"Items":[]}""")
            }
        }
        val feed = MediaServerClient(transport).feed(
            connection,
            app.reelstack.data.model.ViewerAccess(false, emptyMap()),
        )
        assertEquals(listOf("Halvsett film"), feed.resume.map { it.title })
    }
}
