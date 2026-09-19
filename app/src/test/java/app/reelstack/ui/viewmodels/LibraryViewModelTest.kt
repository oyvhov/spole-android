package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.LibraryFilters
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.MediaServerClient
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.data.repository.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LibraryViewModelTest {

    private lateinit var context: Context
    private lateinit var prefsRepo: AppPreferencesRepository
    private lateinit var viewModel: LibraryViewModel
    private val testConnection = ServiceConnection(
        kind = ServiceKind.JELLYFIN,
        name = "TestServer",
        baseUrl = "http://127.0.0.1:8096",
        token = "token123",
        userId = "user123",
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefsRepo = AppPreferencesRepository(context)
        viewModel = LibraryViewModel(
            preferencesRepository = prefsRepo,
            mediaServerClient = MediaServerClient(),
            appContext = context,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `closeLibraryChoices sets open state to false`() {
        viewModel.closeLibraryChoices()
        assertFalse(viewModel.uiState.value.libraryChoicesOpen)
    }

    @Test
    fun `updateLibraryFilters updates filters in state`() {
        val filters = LibraryFilters(genre = "Action", sort = app.reelstack.data.model.LibrarySort.RATING, favourites = true)
        viewModel.updateLibraryFilters(filters)

        assertEquals("Action", viewModel.uiState.value.libraryFilters.genre)
        assertEquals(app.reelstack.data.model.LibrarySort.RATING, viewModel.uiState.value.libraryFilters.sort)
        assertTrue(viewModel.uiState.value.libraryFilters.favourites)
    }

    @Test
    fun `saveLibraryChoices stores selection and triggers callback`() {
        var callbackCalled = false
        val ids = setOf("lib1", "lib2")
        val shortcuts = listOf("lib1")
        val icons = mapOf("lib1" to LibraryIcon.MOVIES)

        viewModel.saveLibraryChoices(
            connection = testConnection,
            ids = ids,
            shortcuts = shortcuts,
            icons = icons,
            onAfterSave = { callbackCalled = true },
        )

        assertTrue(callbackCalled)
        assertFalse(viewModel.uiState.value.libraryChoicesOpen)
        assertEquals(ids, viewModel.uiState.value.selectedLibraryIds)
    }

    @Test
    fun `libraryBack resets filters`() {
        viewModel.updateLibraryFilters(LibraryFilters(genre = "Comedy"))
        viewModel.libraryBack(testConnection)

        assertEquals("", viewModel.uiState.value.libraryFilters.genre)
        assertEquals(emptyList<Pair<String, String>>(), viewModel.uiState.value.libraryPath)
    }
}
