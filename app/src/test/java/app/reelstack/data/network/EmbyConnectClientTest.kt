package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbyConnectClientTest {
    private class Fixture : JsonHttpTransport {
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return when {
                url.contains("/service/servers") -> HttpResponse(200, """[{"SystemId":"server-1","Name":"Heime","Url":"https://away/emby","LocalAddress":"http://tv/emby","AccessKey":"key-1"}]""")
                url.contains("Connect/Exchange") -> HttpResponse(200, """{"LocalUserId":"local-user","AccessToken":"local-token"}""")
                else -> HttpResponse(404, "")
            }
        }
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse =
            HttpResponse(200, """{"ConnectAccessToken":"connect-token","ConnectUserId":"connect-user"}""")
    }

    @Test fun authenticatesListsServersAndExchangesAccessKeyLocally() {
        val fixture = Fixture()
        val client = EmbyConnectClient(fixture)
        val session = client.authenticate("user@example.com", "password")
        val server = client.servers(session).single()
        val local = client.exchange(server, session, "https://tv.example/emby")
        assertEquals("connect-user", session.userId)
        assertEquals("server-1", server.id)
        assertEquals("local-user", local.userId)
        assertEquals("local-token", local.accessToken)
        assertTrue(fixture.urls.any { it.contains("Connect/Exchange") })
    }
}
