package app.reelstack

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.RequestTrackingRepository
import org.junit.Assert.*
import org.junit.Test

class RequestStatusRecoveryTest {
    @Test fun failedMetadataKeepsConfirmedProgressAndImportsListStatusBeyondEnrichmentBatch() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val preferenceName = "status-recovery-${java.util.UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("$preferenceName-$name", mode)
        }
        var detailReads = 0
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse = when {
                url.endsWith("auth/me") -> HttpResponse(200, """{"id":7,"displayName":"Fixture","permissions":32}""")
                url.contains("/request?") -> HttpResponse(200, """{"results":[${(1..25).joinToString(",") { id ->
                    """{"id":$id,"status":2,"requestedBy":{"id":7},"media":{"tmdbId":$id,"mediaType":"movie","status":3}}"""
                }}]}""")
                else -> { detailReads++; HttpResponse(503, "{}") }
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("No writes allowed")
        }
        val connection = ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "connect.sid=fixture", "7", sessionCookie = true)
        val repository = RequestTrackingRepository(context, SeerrServiceClient(transport = transport), AccountProfileClient(transport = transport))
        val scope = repository.scope(connection, "7")
        try {
            repository.put(scope, TrackedRequest("movie:1:", 1, "movie", "Known title", null, emptySet(),
                notify = false, stage = RequestStage.DOWNLOADING, percent = 42))
            val result = repository.refresh(connection).second
            assertEquals(25, result.size)
            assertEquals(RequestStage.DOWNLOADING, result.first { it.mediaId == 1 }.stage)
            assertEquals(42, result.first { it.mediaId == 1 }.percent)
            assertTrue(result.first { it.mediaId == 1 }.statusCheckFailed)
            assertEquals(24, result.count { it.stage == RequestStage.REQUESTED })
            assertEquals(20, detailReads)
            assertTrue(result.none { it.stage == RequestStage.UNKNOWN })
            assertTrue(RequestTrackingRepository(context).list(scope).first { it.mediaId == 1 }.statusCheckFailed)
        } finally {
            listOf("request_follows", "reelstack_preferences", "reelstack_connections").forEach { base.deleteSharedPreferences("$preferenceName-$it") }
        }
    }
}
