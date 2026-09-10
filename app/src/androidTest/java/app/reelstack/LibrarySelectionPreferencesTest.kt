package app.reelstack

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.data.repository.AppPreferencesRepository
import org.junit.Assert.*
import org.junit.Test

class LibrarySelectionPreferencesTest {
    @Test fun selectionsPersistPerServerAndProfileIncludingEmptyAndChangeCacheIdentity() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val name = "library-selection-test-${java.util.UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(ignored: String, mode: Int) = base.getSharedPreferences(name, mode)
        }
        try {
            val connection = ServiceConnection(ServiceKind.JELLYFIN, "Test", "https://media.example", "token", userId = "me")
            val repo = AppPreferencesRepository(context)
            val before = repo.librarySelectionFingerprint(listOf(connection))
            repo.setSelectedLibraryIds(connection, setOf("kids"))
            repo.setLibraryShortcuts(connection, listOf("kids" to "Barneseriar"))
            val reopened = AppPreferencesRepository(context)
            assertEquals(listOf("kids" to "Barneseriar"), reopened.libraryShortcuts(connection))
            assertTrue(reopened.libraryShortcuts(connection.copy(userId = "other")).isEmpty())
            assertTrue(reopened.libraryShortcuts(connection.copy(baseUrl = "https://other.example")).isEmpty())
            assertTrue(reopened.includesLibrary(connection, RemoteLibraryView("kids", "Barneseriar", "tvshows")))
            assertFalse(reopened.includesLibrary(connection, RemoteLibraryView("movies", "Movies", "movies")))
            assertNull(reopened.selectedLibraryIds(connection.copy(userId = "other")))
            assertNull(reopened.selectedLibraryIds(connection.copy(baseUrl = "https://other.example")))
            assertNotEquals(before, reopened.librarySelectionFingerprint(listOf(connection)))
            reopened.setSelectedLibraryIds(connection, emptySet())
            assertTrue(reopened.libraryShortcuts(connection).isEmpty())
            assertEquals(emptySet<String>(), AppPreferencesRepository(context).selectedLibraryIds(connection))
            assertFalse(reopened.includesLibrary(connection, RemoteLibraryView("movies", "Movies", "movies")))
        } finally { base.deleteSharedPreferences(name) }
    }
}
