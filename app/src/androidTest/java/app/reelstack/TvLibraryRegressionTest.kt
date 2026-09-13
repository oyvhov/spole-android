package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.ui.*
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread
import org.junit.Assert.*
import org.junit.Test

/** Real repository, HTTP and ViewModel on the isolated instrumentation profile only. */
class TvLibraryRegressionTest {
    @Test fun movieOnlyInFavouritesOpensAndLibraryTabLeavesThePinnedLibrary() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val store = ViewModelStore()
        val server = ServerSocket(0)
        val film = """{"Id":"favourite-only","Name":"Favorittfilm","Type":"Movie","Overview":"Omtale",
            "UserData":{"IsFavorite":true}}"""
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: SocketException) { break }
                thread(isDaemon = true) {
                    runCatching { socket.use {
                        it.soTimeout = 3000
                        val reader = it.getInputStream().bufferedReader()
                        val path = reader.readLine().split(' ').getOrNull(1).orEmpty()
                        while (!reader.readLine().isNullOrEmpty()) { }
                        val body = when {
                            path.contains("Users/Me") -> """{"Id":"me","Name":"Fixture","Policy":{"IsAdministrator":false}}"""
                            path.contains("UserViews") || path.contains("/Views") ->
                                """{"Items":[{"Id":"films","Name":"Filmar","CollectionType":"movies"}]}"""
                            path.contains("Filters=IsFavorite") -> """{"Items":[$film]}"""
                            path.substringBefore('?').endsWith("/favourite-only") -> film
                            path.contains("Sessions") -> "[]"
                            else -> """{"Items":[],"TotalRecordCount":0}"""
                        }
                        val bytes = body.toByteArray()
                        it.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray() + bytes)
                    } }
                }
            }
        }
        val connection = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "http://127.0.0.1:${server.localPort}", "fixture", userId = "me")
        try {
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.connectionRepository.save(connection)
            container.preferencesRepository.setLibraryShortcuts(connection, listOf("films" to "Filmar"))
            lateinit var model: ReelstackViewModel
            instrumentation.runOnMainSync { model = ReelstackViewModel(container); store.put("library", model) }
            val deadline = System.currentTimeMillis() + 15000
            while (model.uiState.value.favourites.isEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(25)
            assertEquals(1, model.uiState.value.favourites.size)
            assertTrue(model.uiState.value.recentMovies.isEmpty())
            val filmId = model.uiState.value.favourites.single().id
            instrumentation.runOnMainSync { model.openLibraryDetails(filmId) }
            assertEquals(AppSheet.TitleDetails(filmId), model.uiState.value.activeSheet)
            assertEquals("Favorittfilm", model.uiState.value.contentDetails?.title)
            instrumentation.runOnMainSync { model.closeSheet(); model.openLibraryShortcut("films") }
            assertEquals(listOf("films" to "Filmar"), model.uiState.value.libraryPath)
            instrumentation.runOnMainSync { model.selectTab(AppTab.HOME); model.selectTab(AppTab.LIBRARY) }
            assertTrue(model.uiState.value.libraryPath.isEmpty())
            assertNull(model.uiState.value.libraryCollectionType)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            server.close()
            ServiceKind.entries.forEach(container.connectionRepository::delete)
            container.mediaSnapshotStore.clear()
        }
    }
}
