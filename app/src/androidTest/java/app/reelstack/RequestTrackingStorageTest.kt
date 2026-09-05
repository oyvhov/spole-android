package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.RequestTrackingRepository
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RequestTrackingStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = RequestTrackingRepository(context)
    private val connection = ServiceConnection(ServiceKind.SEERR, "Test", "https://seerr.example", "session-one", "7", sessionCookie = true)

    @Test fun scopeUsesServerAndUserNotSessionCookie() {
        assertEquals(repository.scope(connection, "7"), repository.scope(connection.copy(token = "session-two"), "7"))
        assertNotEquals(repository.scope(connection, "7"), repository.scope(connection, "8"))
        assertNotEquals(repository.scope(connection, "7"), repository.scope(connection.copy(baseUrl = "https://other.example"), "7"))
    }

    @Test fun followStateSurvivesReloadAndStaysIsolated() {
        val scope = "test-${UUID.randomUUID()}"
        val item = TrackedRequest("tv:42:2", 42, "tv", "Testserie", null, setOf(2), notify = true,
            stage = RequestStage.DOWNLOADING, percent = 35, availableSeasons = setOf(2), is4k = true)
        try {
            repository.put(scope, item)
            assertEquals(item, RequestTrackingRepository(context).list(scope).single())
            assertTrue(repository.list("$scope-other-user").isEmpty())
            repository.setNotify(scope, item.key, false)
            assertFalse(RequestTrackingRepository(context).list(scope).single().notify)
        } finally {
            context.getSharedPreferences("request_follows", 0).edit().remove(scope).commit()
        }
    }
}
