package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class PersonalRequestTest {
    private val connection = ServiceConnection(ServiceKind.SEERR, "Seerr", "https://seerr.example", "connect.sid=personal", "7", sessionCookie = true)
    private class Transport(private val actor: HttpResponse = HttpResponse(200, """{"id":7,"displayName":"Maya","permissions":32}""")) : JsonHttpTransport {
        var writes = 0
        var body = ""
        var headers = emptyMap<String, String>()
        override fun get(url: String, headers: Map<String, String>) = actor
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse {
            writes++; body = jsonBody; this.headers = headers
            return HttpResponse(201, "{}")
        }
    }
    @Test fun ownSessionSendsWithoutAnImpersonationField() {
        val transport = Transport()
        SeerrServiceClient(transport).request(connection, "movie", 99, "7")
        assertEquals(1, transport.writes)
        assertEquals("connect.sid=personal", transport.headers["Cookie"])
        assertFalse(transport.body.contains("requestedBy"))
        assertFalse(transport.headers.containsKey("X-Api-Key"))
    }
    @Test fun administratorKeyCannotSilentlyCreateAPersonalRequest() {
        val transport = Transport()
        assertTrue(runCatching { SeerrServiceClient(transport).request(connection.copy(sessionCookie = false), "movie", 99, "7") }.isFailure)
        assertEquals(0, transport.writes)
    }
    @Test fun differentServerIdentityNeverPosts() {
        val transport = Transport(HttpResponse(200, """{"id":8,"displayName":"Someone else"}"""))
        assertTrue(runCatching { SeerrServiceClient(transport).request(connection, "movie", 99, "7") }.isFailure)
        assertEquals(0, transport.writes)
    }
    @Test fun expiredSessionNeverPosts() {
        val transport = Transport(HttpResponse(401, "{}"))
        assertTrue(runCatching { SeerrServiceClient(transport).request(connection, "movie", 99, "7") }.isFailure)
        assertEquals(0, transport.writes)
    }
}
