package app.reelstack.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class RequestsCoordinatorTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val series = DiscoverMedia("series", "Fixture", "2026", 0, false, mediaType = "tv")
    private val seerr = ServiceConnection(ServiceKind.SEERR, "Fixture", "https://seerr.example", "fixture", "7", sessionCookie = true)
    private val owner = SessionScope("", "fixture", 1)

    private fun TestScope.feature(state: MutableStateFlow<ReelstackUiState>, writer: SessionUiState = SessionUiState(state),
        transport: JsonHttpTransport? = null, login: () -> Unit = {}): RequestsCoordinator {
        val client = transport?.let(::SeerrServiceClient) ?: SeerrServiceClient()
        val profiles = transport?.let { AccountProfileClient(transport = it) } ?: AccountProfileClient()
        return RequestsCoordinator(writer, backgroundScope, context, MediaSyncRepository(seerrServiceClient = client),
            RequestHistoryRepository(client, profiles), RequestRulesClient(), RequestTrackingRepository(context, client, profiles), login,
            ioDispatcher = StandardTestDispatcher(testScheduler))
    }

    @Test fun `real composer keeps explicit season choice and notification state`() = runTest {
        val state = MutableStateFlow(ReelstackUiState(discover = listOf(series)))
        val feature = feature(state)
        feature.requestMedia(series.id)
        assertEquals(AppSheet.RequestComposer, state.value.activeSheet)
        assertTrue(state.value.requestDraft!!.selected.isEmpty())
        feature.setRequestSeason(1, true)
        feature.setRequestSeason(99, true)
        assertEquals(setOf(1), state.value.requestDraft!!.selected)
        feature.setRequestNotification(false)
        runCurrent()
        // No unused child StateFlow may reset the actual draft after its collector starts.
        assertFalse(state.value.requestDraft!!.notify)
        assertEquals(setOf(1), state.value.requestDraft!!.selected)
        feature.setRequestSeason(1, false)
        feature.confirmRequest()
        assertNotNull(state.value.requestDraft)
        feature.setRequestSeason(1, true)
        feature.confirmRequest()
        assertNull(state.value.requestDraft)
        assertTrue(state.value.discover.single().requested)
    }

    @Test fun `sending blocks close and further season or notification changes`() = runTest {
        val draft = RequestDraft(series, loading = false, sending = true, selected = setOf(1))
        val state = MutableStateFlow(ReelstackUiState(requestDraft = draft))
        val feature = feature(state)
        assertFalse(feature.closeComposer())
        feature.setRequestSeason(1, false)
        feature.setRequestNotification(false)
        assertEquals(draft, state.value.requestDraft)
    }

    @Test fun `closing an idle composer clears only its draft`() = runTest {
        val state = MutableStateFlow(ReelstackUiState(requestDraft = RequestDraft(series), searchQuery = "kept"))
        assertTrue(feature(state).closeComposer())
        assertNull(state.value.requestDraft)
        assertEquals("kept", state.value.searchQuery)
    }

    @Test fun `unverified server account opens personal login instead of a draft`() = runTest {
        val state = MutableStateFlow(ReelstackUiState(connections = listOf(seerr.copy(sessionCookie = false)), discover = listOf(series)))
        var logins = 0
        feature(state, login = { logins++ }).requestMedia(series.id)
        assertEquals(1, logins)
        assertNull(state.value.requestDraft)
    }

    @Test fun `quota prevents confirmation without changing the selected seasons`() = runTest {
        val draft = RequestDraft(series, loading = false, selected = setOf(1, 2),
            rules = RequestRules(true, false, RequestQuota(2, 1, 7)))
        val state = MutableStateFlow(ReelstackUiState(requestDraft = draft))
        feature(state).confirmRequest()
        assertEquals(draft, state.value.requestDraft)
    }

    @Test fun `duplicate history taps perform one load and a revoked response is discarded`() = runTest {
        var historyReads = 0
        val state = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(seerr),
            accounts = mapOf(ServiceKind.SEERR to ServiceAccount(ServiceKind.SEERR, "7", "Fixture", permissions = 32))))
        val writer = SessionUiState(state)
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                if (url.endsWith("auth/me")) return HttpResponse(200, """{"id":7,"displayName":"Fixture","permissions":32}""")
                historyReads++
                writer.revoke()
                state.value = ReelstackUiState(sessionScope = owner.copy(generation = 2))
                return HttpResponse(200, """{"pageInfo":{"results":1},"results":[{"id":1,"requestedBy":{"id":7},"media":{"tmdbId":1,"mediaType":"movie","status":5,"title":"Old title","posterPath":"/p.jpg"}}]}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("read only")
        }
        val feature = feature(state, writer, transport)
        feature.openRequestHistory()
        feature.loadRequestHistory(false)
        runCurrent()
        assertEquals(1, historyReads)
        assertTrue(state.value.requestHistory.items.isEmpty())
        assertFalse(state.value.requestHistory.loading)
    }
}
