package app.reelstack.data.repository

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.AccountProfileClient
import app.reelstack.data.network.HttpResponse
import app.reelstack.data.network.JsonHttpTransport
import app.reelstack.data.network.MediaServerClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.URI

/** Leaving the home network must not leave Home empty while the dead address times out. */
class AddressRouteTest {
    private class Server(val dead: Set<String>) : JsonHttpTransport {
        val hosts = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            val host = URI(url).host
            hosts += host
            if (host in dead) throw IOException("no route")
            return HttpResponse(200, when {
                url.endsWith("Users/Me") -> """{"Id":"me","Name":"Person","Policy":{"IsAdministrator":false}}"""
                url.contains("Views") -> """{"Items":[{"Id":"lib-movies","Name":"Filmar","CollectionType":"movies"}]}"""
                url.endsWith("Sessions") -> "[]"
                else -> """{"Items":[{"Id":"m1","Name":"Film","Type":"Movie"}]}"""
            })
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("No write expected")
    }

    private val home = ServiceConnection(
        ServiceKind.JELLYFIN, "Heime", "http://192.168.1.20:8096", "token", "me",
        alternateUrl = "https://spole.example", identityUrl = "http://192.168.1.20:8096",
    )

    private fun refresh(server: Server) = runBlocking {
        MediaSyncRepository(
            mediaServerClient = MediaServerClient(server),
            accountProfileClient = AccountProfileClient(transport = server),
            routeProbe = { connection -> URI(connection.baseUrl).host !in server.dead },
        ).refresh(listOf(home), includeRecommendations = false)
    }

    @Test fun awayFromHomeTheAccountAndTheFeedUseTheOtherAddress() {
        val server = Server(dead = setOf("192.168.1.20"))
        val snapshot = refresh(server)

        assertEquals(setOf(ServiceKind.JELLYFIN), snapshot.successfulServices)
        assertEquals(setOf(ServiceKind.JELLYFIN), snapshot.switchedToAlternate)
        assertTrue(snapshot.recentMovies.isNotEmpty())
        // Not one request, the account lookup included, waited on the dead address.
        assertTrue(server.hosts.none { it == "192.168.1.20" })
    }

    @Test fun atHomeTheStoredAddressIsKept() {
        val server = Server(dead = emptySet())
        val snapshot = refresh(server)

        assertTrue(snapshot.switchedToAlternate.isEmpty())
        assertTrue(snapshot.recentMovies.isNotEmpty())
        assertTrue(server.hosts.all { it == "192.168.1.20" })
    }

    @Test fun withNoAddressAnsweringTheStoredOneIsKept() {
        val server = Server(dead = setOf("192.168.1.20", "spole.example"))
        val snapshot = refresh(server)

        assertTrue(snapshot.switchedToAlternate.isEmpty())
        assertTrue(snapshot.recentMovies.isEmpty())
    }
}
