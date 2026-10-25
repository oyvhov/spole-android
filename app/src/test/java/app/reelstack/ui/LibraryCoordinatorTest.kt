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
class LibraryCoordinatorTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://media.example", "fixture", "adult")
    private val owner = SessionScope("", "fixture", 1)
    private val views = listOf(RemoteLibraryView("films", "Filmar", "movies"), RemoteLibraryView("series", "Seriar", "tvshows"))
    private class Transport : JsonHttpTransport {
        var fail = false
        override fun get(url: String, headers: Map<String, String>): HttpResponse = if (fail) HttpResponse(503, "{}")
            else HttpResponse(200, """{"Items":[{"Id":"films","Name":"Filmar","CollectionType":"movies"},{"Id":"series","Name":"Seriar","CollectionType":"tvshows"}]}""")
        override fun post(url: String, headers: Map<String, String>, jsonBody: String): HttpResponse = error("read only")
    }
    private fun TestScope.feature(snapshot: MutableStateFlow<ReelstackUiState>, transport: Transport = Transport(),
        port: SessionUiState = SessionUiState(snapshot), cancelSearch: () -> Unit = {}): LibraryCoordinator {
        val client = MediaServerClient(transport)
        return LibraryCoordinator(port, backgroundScope, context, client, MediaSyncRepository(mediaServerClient = client),
            AppPreferencesRepository(context), {}, {}, {}, { null }, {}, cancelSearch, {}, {},
            ioDispatcher = StandardTestDispatcher(testScheduler))
    }

    @Test fun `choices load and persist only offered library ids and the chosen order`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        var searchesCancelled = false
        val feature = feature(snapshot, cancelSearch = { searchesCancelled = true })
        feature.openLibraryChoices()
        runCurrent()
        assertFalse(snapshot.value.libraryChoicesLoading)
        assertEquals(listOf("films", "series"), snapshot.value.libraryChoices.map { it.id })
        feature.saveLibraryChoices(setOf("films", "series"), listOf("series", "films", "series"), mapOf("series" to LibraryIcon.SERIES))
        assertTrue(searchesCancelled)
        assertFalse(snapshot.value.libraryChoicesOpen)
        assertEquals(listOf("series", "films"), AppPreferencesRepository(context).libraryShortcuts(connection).map { it.first })
    }

    @Test fun `loading or failed choices cannot overwrite saved selection`() = runTest {
        val preferences = AppPreferencesRepository(context)
        preferences.setSelectedLibraryIds(connection, setOf("films"))
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val feature = feature(snapshot, Transport().apply { fail = true })
        feature.openLibraryChoices()
        feature.saveLibraryChoices(setOf("series"))
        runCurrent()
        assertNotNull(snapshot.value.libraryChoicesError)
        feature.saveLibraryChoices(setOf("series"))
        assertTrue(preferences.includesLibrary(connection, views.first()))
        assertFalse(preferences.includesLibrary(connection, views.last()))
    }

    @Test fun `closing choices cancels a queued load`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val feature = feature(snapshot)
        feature.openLibraryChoices()
        feature.closeLibraryChoices()
        runCurrent()
        assertFalse(snapshot.value.libraryChoicesOpen)
        assertTrue(snapshot.value.libraryChoices.isEmpty())
    }

    @Test fun `retired library result cannot populate a fresh session`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val writer = SessionUiState(snapshot)
        val feature = feature(snapshot, port = writer)
        feature.openLibraryChoices()
        writer.revoke()
        snapshot.value = ReelstackUiState(sessionScope = owner.copy(generation = 2))
        runCurrent()
        assertTrue(snapshot.value.libraryChoices.isEmpty())
        assertFalse(snapshot.value.libraryChoicesOpen)
    }

    @Test fun `back drops one folder and clears its filters while preserving other features`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner,
            libraryPath = listOf("library" to "Bibliotek", "folder" to "Mappe"),
            libraryFilters = LibraryFilters(genre = "Comedy"), searchQuery = "kept"))
        feature(snapshot).libraryBack()
        assertEquals(listOf("library" to "Bibliotek"), snapshot.value.libraryPath)
        assertEquals(LibraryFilters(), snapshot.value.libraryFilters)
        assertEquals("kept", snapshot.value.searchQuery)
    }
}
