package app.reelstack.data.repository

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.data.network.MediaServerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Searching for a title the user owns twice used to answer twice. Jellyfin and Emby give the same
 * work different item ids, and an episode carries its series name as its title, so a search for
 * "Silo" could return the series from two servers plus several episodes — every card drawn with
 * the same poster and nothing to tell them apart.
 */
class LibrarySearchDeduplicationTest {
    private class Fake(private val responses: (String) -> HttpResponse) : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return responses(url)
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            error("No POST expected")
    }

    private val jellyfin = ServiceConnection(
        ServiceKind.JELLYFIN, "Heimetenar", "https://jf.example", "token", userId = "me",
    )
    private val emby = ServiceConnection(
        ServiceKind.EMBY, "Hyttetenar", "https://emby.example", "token", userId = "me",
    )

    private val views = """{"Items":[{"Id":"lib-series","Name":"Seriar","CollectionType":"tvshows"}]}"""

    private fun series(id: String, tmdb: Int?) = buildString {
        append("""{"Id":"$id","Name":"Silo","Type":"Series","PremiereDate":"2023-05-05T00:00:00Z"""")
        if (tmdb != null) append(""","ProviderIds":{"Tmdb":"$tmdb"}""")
        append("}")
    }

    private fun episode(id: String, number: Int) = """
        {"Id":"$id","Name":"Freedom Day","Type":"Episode","SeriesName":"Silo",
         "ParentIndexNumber":1,"IndexNumber":$number}
    """.trimIndent()

    private fun search(vararg items: String) = """{"Items":[${items.joinToString(",")}]}"""

    private fun repository(transport: JsonHttpTransport) =
        MediaSyncRepository(mediaServerClient = MediaServerClient(transport))

    @Test fun theSameSeriesOnTwoServersIsOneResult() {
        val transport = Fake { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("jf.example") -> HttpResponse(200, search(series("jf-1", 125988)))
                else -> HttpResponse(200, search(series("emby-9", 125988)))
            }
        }
        val results = runBlocking { repository(transport).searchLibraries(listOf(jellyfin, emby), "Silo") }

        assertEquals(listOf("Silo"), results.map { it.title })
    }

    @Test fun twoServersWithoutProviderIdsStillCollapseOnNameTypeAndYear() {
        val transport = Fake { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                url.contains("jf.example") -> HttpResponse(200, search(series("jf-1", null)))
                else -> HttpResponse(200, search(series("emby-9", null)))
            }
        }
        val results = runBlocking { repository(transport).searchLibraries(listOf(jellyfin, emby), "Silo") }

        assertEquals(1, results.size)
    }

    @Test fun episodesDisappearWhenTheirSeriesIsAlsoAHit() {
        val transport = Fake { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                else -> HttpResponse(200, search(series("jf-1", 125988), episode("jf-2", 3), episode("jf-3", 4)))
            }
        }
        val results = runBlocking { repository(transport).searchLibraries(listOf(jellyfin), "Silo") }

        assertEquals(listOf("Silo"), results.map { it.title })
        assertEquals(listOf("Series"), results.map { it.mediaType })
    }

    @Test fun anEpisodeSurvivesWhenItsSeriesIsNotAmongTheHits() {
        // Searching for the episode's own name is a different question, and the answer is still
        // the episode. Collapsing here would leave the search empty.
        val transport = Fake { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                else -> HttpResponse(200, search(episode("jf-2", 3)))
            }
        }
        val results = runBlocking { repository(transport).searchLibraries(listOf(jellyfin), "Freedom Day") }

        assertEquals(1, results.size)
        assertEquals("Episode", results.single().mediaType)
    }

    @Test fun differentWorksSharingATitleAreBothKept() {
        val transport = Fake { url ->
            when {
                url.contains("Views") || url.contains("UserViews") -> HttpResponse(200, views)
                else -> HttpResponse(200, search(series("jf-1", 125988), series("jf-2", 407)))
            }
        }
        val results = runBlocking { repository(transport).searchLibraries(listOf(jellyfin), "Silo") }

        assertEquals(2, results.size)
    }
}
