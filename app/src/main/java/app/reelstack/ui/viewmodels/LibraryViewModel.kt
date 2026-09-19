package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelstack.R
import app.reelstack.data.model.LibraryFacets
import app.reelstack.data.model.LibraryFilters
import app.reelstack.data.model.LibraryIcon
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.LibraryShelves
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.network.MediaServerClient
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.data.network.RemoteLibraryView
import app.reelstack.data.network.readableMessage
import app.reelstack.data.repository.AppPreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryUiState(
    val libraryPath: List<Pair<String, String>> = emptyList(),
    val libraryEntries: List<RemoteLibraryItem> = emptyList(),
    val libraryLoading: Boolean = false,
    val libraryError: String? = null,
    val libraryOffset: Int = 0,
    val libraryHasMore: Boolean = false,
    val libraryFacets: LibraryFacets = LibraryFacets(),
    val libraryFilters: LibraryFilters = LibraryFilters(),
    val selectedLibraryIds: Set<String> = emptySet(),
    val libraryShortcuts: List<Pair<String, String>> = emptyList(),
    val libraryIcons: Map<String, LibraryIcon> = emptyMap(),
    val libraryChoicesOpen: Boolean = false,
    val libraryChoicesLoading: Boolean = false,
    val libraryChoicesError: String? = null,
    val libraryChoices: List<RemoteLibraryView> = emptyList(),
    val libraryShelves: LibraryShelves = LibraryShelves(),
    val libraryPeeks: Map<String, List<LibraryMedia>> = emptyMap(),
    val libraryPeeksLoading: Boolean = false,
    val libraryDetailMedia: LibraryMedia? = null,
)

class LibraryViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val mediaServerClient: MediaServerClient,
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var libraryJob: Job? = null
    private var libraryChoicesJob: Job? = null

    fun openLibraryChoices(connection: ServiceConnection?) {
        if (connection == null) return
        libraryChoicesJob?.cancel()
        _uiState.update {
            it.copy(
                libraryChoicesOpen = true,
                libraryChoicesLoading = true,
                libraryChoicesError = null,
                libraryChoices = emptyList(),
            )
        }
        libraryChoicesJob = viewModelScope.launch {
            try {
                val views = withContext(ioDispatcher) {
                    mediaServerClient.browseLibraries(connection)
                }
                _uiState.update {
                    it.copy(
                        libraryChoicesLoading = false,
                        libraryChoices = views,
                        selectedLibraryIds = preferencesRepository.selectedLibraryIds(connection).orEmpty(),
                        libraryShortcuts = preferencesRepository.libraryShortcuts(connection),
                        libraryIcons = preferencesRepository.libraryIcons(connection),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        libraryChoicesLoading = false,
                        libraryChoicesError = e.readableMessage(appContext)
                            ?: appContext.getString(R.string.library_failed),
                    )
                }
            }
        }
    }

    fun closeLibraryChoices() {
        libraryChoicesJob?.cancel()
        _uiState.update { it.copy(libraryChoicesOpen = false) }
    }

    fun saveLibraryChoices(
        connection: ServiceConnection,
        ids: Set<String>,
        shortcuts: List<String>,
        icons: Map<String, LibraryIcon>,
        onAfterSave: () -> Unit,
    ) {
        val state = _uiState.value
        val validIds = ids.intersect(state.libraryChoices.map { it.id }.toSet())
        preferencesRepository.setSelectedLibraryIds(connection, validIds)

        val pinned = shortcuts.distinct().filter { it in ids }
            .mapNotNull { id -> state.libraryChoices.firstOrNull { it.id == id }?.let { id to it.name } }
        preferencesRepository.setLibraryShortcuts(connection, pinned)

        val savedIcons = icons.filterKeys { key -> state.libraryChoices.any { it.id == key } }
        preferencesRepository.setLibraryIcons(connection, savedIcons)

        _uiState.update {
            it.copy(
                libraryChoicesOpen = false,
                selectedLibraryIds = ids,
                libraryShortcuts = pinned,
                libraryIcons = savedIcons,
                libraryPath = emptyList(),
                libraryEntries = emptyList(),
                libraryDetailMedia = null,
                libraryShelves = LibraryShelves(),
                libraryPeeks = emptyMap(),
                libraryPeeksLoading = false,
            )
        }
        onAfterSave()
    }

    fun browseLibrary(
        connection: ServiceConnection?,
        collectionType: String? = null,
        more: Boolean = false,
    ) {
        if (connection == null) return
        if (more && (_uiState.value.libraryLoading || !_uiState.value.libraryHasMore)) return
        libraryJob?.cancel()

        val state = _uiState.value
        val path = state.libraryPath
        val offset = if (more) state.libraryOffset else 0

        _uiState.update {
            it.copy(
                libraryLoading = true,
                libraryError = null,
                libraryEntries = if (more) it.libraryEntries else emptyList(),
            )
        }

        libraryJob = viewModelScope.launch {
            try {
                val facetJob = async(ioDispatcher) {
                    if (path.isEmpty()) LibraryFacets()
                    else if (state.libraryFacets.parentId == path.last().first) state.libraryFacets
                    else runCatching { mediaServerClient.libraryFacets(connection, path.last().first) }
                        .getOrDefault(LibraryFacets())
                }

                val entries = withContext(ioDispatcher) {
                    if (path.isEmpty()) {
                        mediaServerClient.browseLibraries(connection)
                            .filter { preferencesRepository.includesLibrary(connection, it) }
                            .map { view ->
                                RemoteLibraryItem(
                                    id = view.id,
                                    title = view.name,
                                    subtitle = "",
                                    progress = null,
                                    mediaType = "CollectionFolder",
                                    artworkItemId = view.id,
                                    artworkUrl = view.artworkUrl,
                                    isFolder = true,
                                    collectionType = view.collectionType,
                                )
                            }
                    } else {
                        val resolvedCollectionType = if (path.size == 1) {
                            collectionType ?: mediaServerClient.browseLibraries(connection)
                                .firstOrNull { it.id == path.first().first }?.collectionType
                        } else null
                        mediaServerClient.browseLibrary(connection, path.last().first, offset, resolvedCollectionType, state.libraryFilters)
                    }
                }

                val facets = facetJob.await()
                if (!isActive) return@launch

                _uiState.update {
                    it.copy(
                        libraryLoading = false,
                        libraryFacets = facets,
                        libraryEntries = ((if (more) it.libraryEntries else emptyList()) + entries).distinctBy { entry -> entry.id },
                        libraryOffset = offset + entries.size,
                        libraryHasMore = path.isNotEmpty() && entries.size == 60,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (isActive) {
                    _uiState.update {
                        it.copy(
                            libraryLoading = false,
                            libraryError = error.readableMessage(appContext)
                                ?: appContext.getString(R.string.library_failed),
                        )
                    }
                }
            }
        }
    }

    fun libraryBack(connection: ServiceConnection?, collectionType: String? = null) {
        _uiState.update {
            it.copy(
                libraryPath = it.libraryPath.dropLast(1),
                libraryFilters = LibraryFilters(),
            )
        }
        browseLibrary(connection, collectionType)
    }

    fun updateLibraryFilters(filters: LibraryFilters) {
        _uiState.update { it.copy(libraryFilters = filters) }
    }
}
