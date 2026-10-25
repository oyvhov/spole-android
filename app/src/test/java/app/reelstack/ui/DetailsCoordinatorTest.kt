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
class DetailsCoordinatorTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://media.example", "fixture", "adult")
    private val owner = SessionScope("", "fixture", 1)
    private fun media(id: String) = LibraryMedia("jellyfin-$id", "Title $id", "", artworkRes = 0, source = ServiceKind.JELLYFIN,
        remoteId = id, mediaType = "Movie")
    private fun state() = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection),
        recentMovies = listOf(media("one"), media("two"))))

    private fun TestScope.feature(state: MutableStateFlow<ReelstackUiState>, writer: SessionUiState = SessionUiState(state),
        read: (String) -> HttpResponse): DetailsCoordinator {
        val transport = object : JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>) = read(url)
            override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("read only")
        }
        return DetailsCoordinator(writer, backgroundScope, context, MediaSyncRepository(mediaServerClient = MediaServerClient(transport)),
            isTelevision = { false }, captureSession = { owner },
            remoteDetails = { remote, id -> MediaServerClient(transport).details(remote, id) },
            ioDispatcher = StandardTestDispatcher(testScheduler))
    }

    @Test fun `opening another title cancels queued detail work before it reads the server`() = runTest {
        val state = state()
        val reads = mutableListOf<String>()
        val feature = feature(state) { url -> reads += url; HttpResponse(200, """{"Id":"two","Name":"Second title","Type":"Movie"}""") }
        feature.openLibraryDetails("jellyfin-one")
        feature.openLibraryDetails("jellyfin-two")
        runCurrent()
        assertFalse(reads.any { it.substringBefore('?').endsWith("/one") })
        assertEquals("jellyfin-two", state.value.contentDetails?.key)
        assertFalse(state.value.contentDetails!!.loading)
    }

    @Test fun `closing details stops queued reads while other feature state survives`() = runTest {
        val state = state()
        var reads = 0
        val feature = feature(state) { reads++; HttpResponse(200, "{}") }
        feature.openLibraryDetails("jellyfin-one")
        feature.closeDetails()
        state.value = state.value.copy(activeSheet = null, contentDetails = null, searchQuery = "kept")
        runCurrent()
        assertEquals(0, reads)
        assertNull(state.value.contentDetails)
        assertEquals("kept", state.value.searchQuery)
    }

    @Test fun `same title response from a retired account cannot overwrite new account details`() = runTest {
        val state = state()
        val writer = SessionUiState(state)
        val feature = feature(state, writer) {
            writer.revoke()
            state.value = state.value.copy(sessionScope = owner.copy(generation = 2),
                contentDetails = state.value.contentDetails!!.copy(title = "New account", loading = false))
            HttpResponse(200, """{"Id":"one","Name":"Old account","Type":"Movie"}""")
        }
        feature.openLibraryDetails("jellyfin-one")
        runCurrent()
        assertEquals("New account", state.value.contentDetails?.title)
    }
}
