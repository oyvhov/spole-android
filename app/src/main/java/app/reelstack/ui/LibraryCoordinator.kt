package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfScope
import app.reelstack.data.model.choice
import app.reelstack.data.model.mergedSmartShelves
import app.reelstack.data.network.readableMessage
import app.reelstack.data.network.toLibraryMedia
import app.reelstack.data.repository.MediaSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns library pages, filters, choices, and the page cache for the current session. Created and retired by ReelstackFeatures. */
internal class LibraryCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val mediaServerClient: app.reelstack.data.network.MediaServerClient,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val preferencesRepository: app.reelstack.data.repository.AppPreferencesRepository,
    private val closeSmartShelfEditor: () -> Unit,
    private val cancelSmartPeeks: () -> Unit,
    private val loadSmartShelfPeeks: () -> Unit,
    private val smartShelfOfPath: (String?) -> SmartShelf?,
    private val openSmartShelf: (String) -> Unit,
    private val cancelSearch: () -> Unit,
    private val setSearchQuery: (String) -> Unit,
    private val openLibraryDetails: (String) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private data class LibraryCacheKey(
        val accountFingerprint: String,
        val source: ServiceKind,
        val path: String,
        val collectionType: String?,
        val filters: app.reelstack.data.model.LibraryFilters,
        val shelfScope: SmartShelfScope?,
    )

    private data class CachedLibraryPage(
        val entries: List<app.reelstack.data.network.RemoteLibraryItem>,
        val facets: app.reelstack.data.model.LibraryFacets,
        val hasMore: Boolean,
    )

    /**
     * Keeps visited library pages warm while the app is alive. The server remains authoritative,
     * but returning to Films or Series should show the last page immediately while a quiet refresh
     * checks for new titles in the background.
     */
    private val libraryPageCache = object : LinkedHashMap<LibraryCacheKey, CachedLibraryPage>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LibraryCacheKey, CachedLibraryPage>?): Boolean = size > 8
    }

    private fun libraryCacheKey(connection: ServiceConnection, state: ReelstackUiState): LibraryCacheKey =
        LibraryCacheKey(
            accountFingerprint = MediaSnapshotStore.fingerprint(listOf(connection)),
            source = connection.kind,
            path = state.libraryPath.joinToString("/") { it.first },
            collectionType = state.libraryCollectionType,
            filters = state.libraryFilters,
            shelfScope = SmartShelf.idOfPath(state.libraryPath.lastOrNull()?.first)?.let { id ->
                mergedSmartShelves(state.smartShelves).firstOrNull { it.id == id }?.scope
            },
        )

    private var libraryChoicesJob: Job? = null
    private var libraryJob: Job? = null
    private var shelfJob: Job? = null
    private var peekJob: Job? = null

    /** Forget catalog pages without touching artwork, playback history, or server credentials. */
    fun clearLibraryCache() {
        libraryJob?.cancel()
        shelfJob?.cancel()
        peekJob?.cancel()
        cancelSmartPeeks()
        libraryPageCache.clear()
        _uiState.update {
            it.copy(
                libraryEntries = emptyList(),
                libraryFacets = app.reelstack.data.model.LibraryFacets(),
                libraryShelves = app.reelstack.data.model.LibraryShelves(),
                libraryPeeks = emptyMap(), smartShelfPeeks = emptyMap(),
                libraryPeeksLoading = false,
                libraryOffset = 0,
                libraryHasMore = false,
                libraryLoading = false,
                libraryError = null,
            )
        }
        if (_uiState.value.selectedTab == AppTab.LIBRARY) browseLibrary(false)
    }

    fun selectLibrarySource(source: ServiceKind) {
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == source && source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        preferencesRepository.preferredLibrarySource = source
        libraryJob?.cancel()
        peekJob?.cancel()
        shelfJob?.cancel()
        libraryChoicesJob?.cancel()
        cancelSmartPeeks()
        closeSmartShelfEditor()
        libraryPageCache.clear()
        _uiState.update { it.copy(selectedLibrarySource = source,
            libraryPath = emptyList(), libraryCollectionType = null, libraryEntries = emptyList(),
            libraryPeeks = emptyMap(), smartShelfPeeks = emptyMap(), libraryPeeksLoading = false,
            libraryShelves = app.reelstack.data.model.LibraryShelves(),
            libraryFilters = app.reelstack.data.model.LibraryFilters(),
            libraryFacets = app.reelstack.data.model.LibraryFacets(),
            libraryLoading = false, libraryError = null, libraryOffset = 0, libraryHasMore = false,
            libraryChoicesOpen = false, libraryChoices = emptyList(), selectedLibraryIds = emptySet(),
            libraryShortcuts = preferencesRepository.libraryShortcuts(connection),
            libraryIcons = preferencesRepository.libraryIcons(connection)) }
        browseLibrary()
    }

    /** Opens «Tilpass biblioteksida». Its library list needs a server; its rows and look do not. */
    fun openLibraryChoices() {
        libraryChoicesJob?.cancel()
        val connection = _uiState.value.libraryConnection ?: run {
            _uiState.update { it.copy(libraryChoicesOpen = true, libraryChoicesLoading = false, libraryChoicesError = null,
                libraryChoices = emptyList()) }
            return
        }
        _uiState.update { it.copy(libraryChoicesOpen = true, libraryChoicesLoading = true, libraryChoicesError = null, libraryChoices = emptyList()) }
        libraryChoicesJob = operationScope.launch {
            try {
                val choices = withContext(ioDispatcher) { mediaServerClient.browseLibraries(connection) }
                if (!isActive) return@launch
                _uiState.update { it.copy(libraryChoicesLoading = false, libraryChoices = choices,
                    selectedLibraryIds = choices.filter { view -> preferencesRepository.includesLibrary(connection, view) }.mapTo(mutableSetOf()) { view -> view.id }) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(libraryChoicesLoading = false,
                    libraryChoicesError = error.readableMessage(appContext) ?: appString(R.string.library_failed)) }
            }
        }
    }

    fun closeLibraryChoices() {
        libraryChoicesJob?.cancel()
        _uiState.update { it.copy(libraryChoicesOpen = false) }
    }

    /** [shortcuts] is ordered: the menu shows them in exactly the order it is given. */
    fun saveLibraryChoices(ids: Set<String>, shortcuts: List<String> = _uiState.value.libraryShortcuts.map { it.first },
        icons: Map<String, app.reelstack.data.model.LibraryIcon> = _uiState.value.libraryIcons) {
        val state = _uiState.value
        if (!state.libraryChoicesOpen || state.libraryChoicesLoading || state.libraryChoicesError != null) return
        val connection = state.libraryConnection ?: return
        preferencesRepository.setSelectedLibraryIds(connection, ids.intersect(state.libraryChoices.map { it.id }.toSet()))
        // The chosen order, not the server's. A menu the reader arranged has to stay arranged.
        val pinned = shortcuts.distinct().filter { it in ids }
            .mapNotNull { id -> state.libraryChoices.firstOrNull { it.id == id }?.let { id to it.name } }
        preferencesRepository.setLibraryShortcuts(connection, pinned)
        val savedIcons = icons.filterKeys { key -> state.libraryChoices.any { it.id == key } }
        preferencesRepository.setLibraryIcons(connection, savedIcons)
        libraryChoicesJob?.cancel()
        libraryJob?.cancel()
        cancelSearch()
        libraryPageCache.clear()
        // Home has its own library choice (docs/HOME_LAYOUT.md). This one used to empty every Home
        // row and refetch the whole feed although nothing on Home depends on it any more.
        _uiState.update { it.copy(libraryChoicesOpen = false, selectedLibraryIds = ids, libraryShortcuts = pinned, libraryIcons = savedIcons,
            libraryPath = emptyList(), libraryEntries = emptyList(), libraryDetailMedia = null,
            libraryShelves = app.reelstack.data.model.LibraryShelves(),
            libraryPeeks = emptyMap(), libraryPeeksLoading = false,
            librarySearchResults = emptyList(), isSearching = false,
            globalSearch = app.reelstack.ui.state.SearchSlice()) }
        // Library filters do not invalidate playback progress or unsent checkpoints.
        browseLibrary()
        if (state.searchQuery.isNotBlank()) setSearchQuery(state.searchQuery)
    }

    fun browseLibrary(more: Boolean = false) {
        if (!more) loadLibraryShelves()
        if (more && (_uiState.value.libraryLoading || !_uiState.value.libraryHasMore)) return
        libraryJob?.cancel()
        val state = _uiState.value
        val connection = state.libraryConnection ?: return
        val path = state.libraryPath
        val offset = if (more) state.libraryOffset else 0
        val cacheKey = libraryCacheKey(connection, state)
        val cached = libraryPageCache[cacheKey]
        _uiState.update { it.copy(libraryError = null,
            // Stale-while-revalidate: a visited page is useful immediately, while the request
            // below quietly confirms it against Jellyfin/Emby. New pages still show loading.
            libraryLoading = more || cached == null,
            libraryEntries = if (more) it.libraryEntries else cached?.entries ?: it.libraryEntries,
            libraryFacets = cached?.facets ?: it.libraryFacets,
            libraryOffset = if (more) it.libraryOffset else cached?.entries?.size ?: it.libraryOffset,
            libraryHasMore = if (more) it.libraryHasMore else cached?.hasMore ?: it.libraryHasMore) }
        libraryJob = operationScope.launch {
            try {
                fun currentCatalogue() = isActive && _uiState.value.librarySource == connection.kind &&
                    _uiState.value.libraryPath == path && _uiState.value.libraryFilters == state.libraryFilters &&
                    libraryCacheKey(connection, _uiState.value) == cacheKey &&
                    _uiState.value.activeProfileId == state.activeProfileId &&
                    _uiState.value.sessionScope == state.sessionScope &&
                    _uiState.value.libraryConnection?.let {
                        it.baseUrl == connection.baseUrl && it.token == connection.token && it.userId == connection.userId
                    } == true
                val seasonShelf = smartShelfOfPath(path.lastOrNull()?.first)
                    ?.forViewer(state.isKidMode)
                // A shelf page not seen in this session starts from what it held last time.
                if (seasonShelf != null && cached == null && !more) {
                    val remembered = withContext(ioDispatcher) {
                        runCatching { mediaServerClient.smartShelf(connection, seasonShelf, cachedOnly = true) }
                            .getOrDefault(emptyList())
                    }
                    if (remembered.isNotEmpty() && currentCatalogue() && _uiState.value.libraryEntries.isEmpty()) {
                        _uiState.update { it.copy(libraryEntries = remembered) }
                    }
                }
                loadLibraryPage(ioDispatcher, readFacets = {
                    if (path.isEmpty() || seasonShelf != null) app.reelstack.data.model.LibraryFacets()
                    else if (state.libraryFacets.parentId == path.last().first) state.libraryFacets
                    else runCatching { mediaServerClient.libraryFacets(connection, path.last().first) }
                        .getOrDefault(app.reelstack.data.model.LibraryFacets())
                }, readItems = {
                    if (seasonShelf != null) mediaServerClient.smartShelf(connection, seasonShelf)
                    else if (path.isEmpty()) mediaServerClient.browseLibraries(connection).filter { preferencesRepository.includesLibrary(connection, it) }.map { view ->
                        app.reelstack.data.network.RemoteLibraryItem(view.id, view.name, "", null, "CollectionFolder", view.id,
                            artworkUrl = view.artworkUrl, isFolder = true, collectionType = view.collectionType)
                    } else {
                        val catalogueType = if (path.size == 1) state.libraryCollectionType ?:
                            mediaServerClient.browseLibraries(connection).firstOrNull { it.id == path.first().first }?.collectionType else null
                        mediaServerClient.browseLibrary(connection, path.last().first, offset, catalogueType, state.libraryFilters)
                    }
                }, onItems = { entries ->
                  if (currentCatalogue()) _uiState.update { current ->
                    val merged = if (more) {
                        (current.libraryEntries + entries).distinctBy { entry -> entry.id }
                    } else {
                        // Keep already fetched pages after the freshly confirmed first page so a
                        // return to a scrolled library does not throw away the user's position.
                        (entries + (cached?.entries.orEmpty().drop(entries.size))).distinctBy { entry -> entry.id }
                    }
                    val hasMore = path.isNotEmpty() && seasonShelf == null && (entries.size == 60 || (cached?.hasMore == true && !more))
                    libraryPageCache[cacheKey] = CachedLibraryPage(merged, current.libraryFacets, hasMore)
                    current.copy(libraryLoading = false,
                        libraryEntries = merged,
                        libraryOffset = merged.size,
                        libraryHasMore = hasMore)
                  }
                // The libraries this listing actually found — not the pinned shortcuts, which are a
                // menu choice and can be empty while the page still shows every library there is.
                if (currentCatalogue() && path.isEmpty()) loadLibraryPeeks(entries.map {
                    app.reelstack.data.network.RemoteLibraryView(it.id, it.title, it.collectionType, it.artworkUrl)
                })
                if (currentCatalogue() && path.isEmpty()) loadSmartShelfPeeks()
                }, onFacets = { facets ->
                    if (currentCatalogue()) {
                        libraryPageCache[cacheKey]?.let { libraryPageCache[cacheKey] = it.copy(facets = facets) }
                        _uiState.update { it.copy(libraryFacets = facets) }
                    }
                })
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (isActive) _uiState.update { it.copy(libraryLoading = false, libraryError = error.readableMessage(appContext) ?: appContext.getString(R.string.library_failed)) }
            }
        }
    }

    fun libraryBack() {
        _uiState.update { it.copy(libraryPath = it.libraryPath.dropLast(1), libraryFilters = app.reelstack.data.model.LibraryFilters(),
            libraryEntries = emptyList(), libraryOffset = 0, libraryHasMore = false) }
        browseLibrary()
    }

    /**
     * What this library was left half-watched at.
     *
     * Only for the root of one library: inside a series the grid is already the episode list, and a
     * shelf above it would repeat it. Failures are silent by design — a shelf is an extra, and a
     * library that cannot produce one should still open.
     */
    /**
     * The peek into every library, for the page that lists them.
     *
     * Asked once per sign-in and kept: the landing page is arrowed through, not scrolled, and a
     * request per keypress would make the page feel worse than the four tiles it replaces. Failure
     * is silent — the tiles are still there, they just have nothing under them.
     */
    private fun loadLibraryPeeks(views: List<app.reelstack.data.network.RemoteLibraryView>) {
        val state = _uiState.value
        // A library with nothing in it never lands in the map, so "already asked" cannot be read off
        // the keys. The loading flag covers the request in flight; the map covers the answer.
        if (views.isEmpty() || state.libraryPeeksLoading || state.libraryPeeks.isNotEmpty()) return
        val connection = state.libraryConnection ?: return
        peekJob?.cancel()
        _uiState.update { it.copy(libraryPeeks = emptyMap(), libraryPeeksLoading = true) }
        peekJob = operationScope.launch {
            // One request per library, all in flight together, and each row appears the moment its
            // own answer arrives. Waiting for the slowest library before drawing any of them is how
            // a page that is mostly ready still feels like it is loading.
            kotlinx.coroutines.coroutineScope {
                views.forEach { view -> launch {
                    val items = withContext(ioDispatcher) {
                        runCatching { mediaSyncRepository.libraryPeek(connection, view) }.getOrDefault(emptyList())
                    }
                    if (isActive && items.isNotEmpty() && _uiState.value.activeProfileId == state.activeProfileId &&
                        _uiState.value.libraryConnection == connection) {
                        _uiState.update { it.copy(libraryPeeks = it.libraryPeeks + (view.id to items)) }
                    }
                } }
            }
            if (isActive) _uiState.update { it.copy(libraryPeeksLoading = false) }
        }
    }

    private fun loadLibraryShelves() {
        val state = _uiState.value
        val path = state.libraryPath
        // A season's shelf spans every library, so there is no single library to be half-way through.
        val library = path.singleOrNull()?.takeIf { app.reelstack.data.model.SmartShelf.idOfPath(it.first) == null }
        if (library == null) {
            shelfJob?.cancel()
            if (state.libraryShelves != app.reelstack.data.model.LibraryShelves()) {
                _uiState.update { it.copy(libraryShelves = app.reelstack.data.model.LibraryShelves()) }
            }
            return
        }
        if (state.libraryShelves.libraryId == library.first && !state.libraryShelves.loading) return
        val connection = state.libraryConnection ?: return
        shelfJob?.cancel()
        _uiState.update { it.copy(libraryShelves = app.reelstack.data.model.LibraryShelves(libraryId = library.first, loading = true)) }
        shelfJob = operationScope.launch {
            val view = app.reelstack.data.network.RemoteLibraryView(library.first, library.second, _uiState.value.libraryCollectionType)
            val loaded = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.libraryShelves(connection, view) }
            }.getOrNull()
            if (!isActive) return@launch
            _uiState.update { current ->
                if (current.libraryShelves.libraryId != library.first) return@update current
                current.copy(libraryShelves = current.libraryShelves.copy(
                    resume = loaded?.first.orEmpty(), nextUp = loaded?.second.orEmpty(), loading = false))
            }
        }
    }
    fun filterLibrary(filters: app.reelstack.data.model.LibraryFilters) {
        _uiState.update { it.copy(libraryFilters = filters) }
        browseLibrary()
    }

    fun openLibraryEntry(id: String) {
        val entry = _uiState.value.libraryEntries.firstOrNull { it.id == id } ?: return
        // A series is a folder to the server and a *title* to a person. Its detail page carries the
        // poster, the synopsis, the cast and every episode with its own still and resume point,
        // which is more than a grid of season posters says after two more presses. Every other kind
        // of folder — collections, music, the libraries themselves — still opens as a folder.
        if (entry.isFolder && !entry.mediaType.equals("Series", ignoreCase = true)) {
            _uiState.update { it.copy(libraryPath = it.libraryPath + (entry.id to entry.title),
                libraryCollectionType = if (it.libraryPath.isEmpty()) entry.collectionType else it.libraryCollectionType,
                libraryEntries = emptyList(), libraryOffset = 0, libraryHasMore = false,
                libraryFilters = app.reelstack.data.model.LibraryFilters()) }
            browseLibrary()
        } else {
            val source = _uiState.value.librarySource
            val media = entry.toLibraryMedia(source, R.drawable.media_placeholder, words(entry.facts))
            _uiState.update { it.copy(libraryDetailMedia = media) }
            openLibraryDetails(media.id)
        }
    }
    fun openLibraryShortcut(id: String) {
        SmartShelf.idOfPath(id)?.let { openSmartShelf(it); return }
        val shortcut = _uiState.value.libraryShortcuts.firstOrNull { it.first == id } ?: return
        _uiState.update { it.copy(selectedTab = AppTab.LIBRARY, activeSheet = null, libraryPath = listOf(shortcut), libraryCollectionType = null,
            libraryFilters = app.reelstack.data.model.LibraryFilters(), libraryFacets = app.reelstack.data.model.LibraryFacets()) }
        browseLibrary()
    }

    fun invalidatePages(connection: ServiceConnection? = null) {
        if (connection == null) libraryPageCache.clear()
        else {
            val fingerprint = MediaSnapshotStore.fingerprint(listOf(connection))
            libraryPageCache.keys.filter { it.accountFingerprint == fingerprint }.toList().forEach(libraryPageCache::remove)
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)

    /** A list of decisions, rendered once in the language the app is set to. */
    private fun words(items: List<app.reelstack.localization.LocalizedText>): List<String> =
        items.map { it.text(appContext) }
}
