package app.reelstack.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.*
import app.reelstack.launcher.routeInMain
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
        val urls = mutableListOf<String>()
        override fun get(url: String, headers: Map<String, String>): HttpResponse {
            urls += url
            return if (fail) HttpResponse(503, "{}")
                else HttpResponse(200, """{"Items":[{"Id":"films","Name":"Filmar","CollectionType":"movies"},{"Id":"series","Name":"Seriar","CollectionType":"tvshows"}]}""")
        }
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

    @Test fun `parsed launcher library opens the requested view for the selected connection`() = runTest {
        val emby = connection.copy(kind = ServiceKind.EMBY, baseUrl = "https://emby.example", userId = "emby-user")
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection, emby)))
        val transport = Transport()
        val link = app.reelstack.launcher.LauncherLinks.parse("spole://library?kind=emby&user=emby-user&library=series")!!
        assertEquals(app.reelstack.launcher.LauncherAction.LIBRARY, link.action)
        val feature = feature(snapshot, transport)
        link.routeInMain(
            details = { fail("A library view must not open item details") },
            play = { fail("A library view must not start playback") },
            library = { feature.openLauncherLibrary(it.kind, it.itemId) },
        )
        runCurrent()
        assertEquals(AppTab.LIBRARY, snapshot.value.selectedTab)
        assertEquals(ServiceKind.EMBY, snapshot.value.librarySource)
        assertEquals(listOf("series" to "Seriar"), snapshot.value.libraryPath)
        assertEquals("tvshows", snapshot.value.libraryCollectionType)
        assertTrue(transport.urls.any { it.contains("emby.example") && it.contains("Users/emby-user/Views") })
        assertTrue(transport.urls.any { it.contains("emby.example") && it.contains("ParentId=series") })
        assertFalse(transport.urls.any { it.contains("media.example") })
    }

    @Test fun `launcher library survives a status refresh for the same account`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        feature(snapshot).openLauncherLibrary(ServiceKind.JELLYFIN, "films")
        snapshot.value = snapshot.value.copy(connections = listOf(connection.copy(
            state = ConnectionState.CONNECTED, latencyMs = 24, detail = "Updated status",
        )))
        runCurrent()
        assertEquals(listOf("films" to "Filmar"), snapshot.value.libraryPath)
        assertFalse(snapshot.value.libraryLoading)
    }

    @Test fun `launcher library rejects a changed account or server while loading`() = runTest {
        val changes = listOf(
            connection.copy(kind = ServiceKind.EMBY),
            connection.copy(token = "replacement"),
            connection.copy(userId = "other-user"),
            connection.copy(baseUrl = "https://other.example"),
            connection.copy(identityUrl = "https://other-identity.example"),
        )
        changes.forEach { replacement ->
            val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
            feature(snapshot).openLauncherLibrary(ServiceKind.JELLYFIN, "films")
            snapshot.value = snapshot.value.copy(connections = listOf(replacement))
            runCurrent()
            assertTrue(snapshot.value.libraryPath.isEmpty())
        }
    }

    @Test fun `child launcher library uses the requested source and child account`() = runTest {
        val emby = connection.copy(kind = ServiceKind.EMBY, baseUrl = "https://emby.example", userId = "child-user")
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, activeProfileId = "kid",
            isKidMode = true, connections = listOf(connection, emby)))
        val transport = Transport()
        val client = MediaServerClient(transport)
        val feature = KidsLibraryCoordinator(SessionUiState(snapshot), backgroundScope, client,
            MediaSyncRepository(mediaServerClient = client), StandardTestDispatcher(testScheduler))
        val link = app.reelstack.launcher.LauncherLinks.parse("spole://library?kind=emby&user=child-user&library=series")!!
        feature.openLauncherLibrary(link.kind, link.itemId)
        runCurrent()
        val destination = snapshot.value.kidsLauncherLibrary!!
        assertEquals(ServiceKind.EMBY, destination.source)
        assertEquals("series", destination.libraryId)
        assertEquals("Seriar", destination.view?.name)
        assertFalse(destination.loading)
        assertFalse(destination.error)
        assertTrue(transport.urls.any { it.contains("emby.example") && it.contains("Users/child-user/Views") })
        assertFalse(transport.urls.any { it.contains("media.example") })
    }

    @Test fun `missing launcher view never opens an arbitrary item`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val transport = Transport()
        feature(snapshot, transport).openLauncherLibrary(ServiceKind.JELLYFIN, "not-a-view")
        runCurrent()
        assertEquals(AppTab.LIBRARY, snapshot.value.selectedTab)
        assertTrue(snapshot.value.libraryPath.isEmpty())
        assertNotNull(snapshot.value.libraryError)
        assertFalse(transport.urls.any { it.contains("ParentId=not-a-view") })
    }

    @Test fun `jellyfin launcher looks up the view in the same user views endpoint as the sender`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val transport = Transport()
        feature(snapshot, transport).openLauncherLibrary(ServiceKind.JELLYFIN, "films")
        runCurrent()
        assertEquals(listOf("films" to "Filmar"), snapshot.value.libraryPath)
        assertTrue(transport.urls.any { it.contains("Users/adult/Views") })
        assertFalse(transport.urls.any { it.contains("UserViews?") })
    }

    @Test fun `a newer library request replaces an earlier request before it loads`() = runTest {
        val emby = connection.copy(kind = ServiceKind.EMBY, baseUrl = "https://emby.example", userId = "emby-user")
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection, emby)))
        val transport = Transport()
        val feature = feature(snapshot, transport)
        feature.openLauncherLibrary(ServiceKind.JELLYFIN, "films")
        feature.openLauncherLibrary(ServiceKind.EMBY, "series")
        runCurrent()
        assertEquals(ServiceKind.EMBY, snapshot.value.librarySource)
        assertEquals(listOf("series" to "Seriar"), snapshot.value.libraryPath)
        assertFalse(transport.urls.any { it.contains("media.example") })
    }

    @Test fun `retired session cannot publish a launcher library view`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = owner, connections = listOf(connection)))
        val writer = SessionUiState(snapshot)
        val feature = feature(snapshot, port = writer)
        feature.openLauncherLibrary(ServiceKind.JELLYFIN, "films")
        writer.revoke()
        snapshot.value = ReelstackUiState(sessionScope = owner.copy(generation = 2))
        runCurrent()
        assertTrue(snapshot.value.libraryPath.isEmpty())
        assertNotEquals(AppTab.LIBRARY, snapshot.value.selectedTab)
    }
}
