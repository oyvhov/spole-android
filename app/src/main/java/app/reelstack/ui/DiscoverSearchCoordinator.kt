package app.reelstack.ui

import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.mergeLibrarySearch
import app.reelstack.ui.state.SearchSlice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns cancellable, profile-scoped search work for one search field. Discover and the global search
 * screen each have their own instance; [slice] and [withSlice] say which part of the state it owns.
 */
internal class DiscoverSearchCoordinator(
    private val appContext: android.content.Context,
    mediaSyncRepository: MediaSyncRepository,
    private val preferencesRepository: app.reelstack.data.repository.AppPreferencesRepository,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
    private val slice: (ReelstackUiState) -> SearchSlice = ::discoverSlice,
    private val withSlice: (ReelstackUiState, SearchSlice) -> ReelstackUiState = ::withDiscoverSlice,
    ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    private val searchLibrary: suspend (ServiceConnection, String) -> List<LibraryMedia> =
        { server, query -> withContext(ioDispatcher) { mediaSyncRepository.searchLibrary(server, query) } },
    private val searchDiscover: suspend (ServiceConnection, String, Int) -> MediaSyncRepository.SearchPage =
        { server, query, page -> withContext(ioDispatcher) { mediaSyncRepository.search(server, query, page) } },
) {
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    fun cancel() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = null
        loadMoreJob = null
    }

    private fun edit(transform: (SearchSlice) -> SearchSlice) =
        updateState { state -> withSlice(state, transform(slice(state))) }

    fun setQuery(value: String) {
        cancel()
        val query = value.trim()
        val state = readState()
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val mediaServers = state.connections.filter {
            it.kind in MEDIA_SERVERS && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        if (query.length < 2) {
            edit { SearchSlice(query = value) }
            return
        }
        if (seerr == null && mediaServers.isEmpty()) {
            updateState {
                withSlice(it, SearchSlice(
                    query = value,
                    results = it.discover.filter { media -> media.title.contains(query, ignoreCase = true) },
                ))
            }
            return
        }
        // Keep the last answer while typing and fetching. Pagination belongs to the new query,
        // so the old page cannot be appended while those cards are still visible.
        edit { previous -> SearchSlice(
            query = value, searching = true,
            results = if (seerr != null) previous.results else emptyList(),
            people = if (seerr != null) previous.people else emptyList(),
            libraryResults = if (mediaServers.isNotEmpty()) previous.libraryResults else emptyList(),
        ) }
        fun publish(transform: (SearchSlice) -> SearchSlice) =
            edit { if (it.query == value) transform(it) else it }

        searchJob = scope.launch {
            delay(DEBOUNCE_MILLIS)
            val failed = mutableSetOf<ServiceKind>()
            val groups = MutableList<List<LibraryMedia>>(mediaServers.size) { emptyList() }
            coroutineScope {
                mediaServers.forEachIndexed { index, server ->
                    launch {
                        val answer = attempt { searchLibrary(server, query) }
                        ensureActive()
                        if (answer.isFailure) failed += server.kind
                        groups[index] = answer.getOrDefault(emptyList())
                        val hits = mergeLibrarySearch(groups)
                        publish { it.copy(libraryResults = hits) }
                    }
                }
                if (seerr != null) {
                    val answer = attempt { searchDiscover(seerr, query, 1) }
                    ensureActive()
                    if (answer.isFailure) failed += seerr.kind
                    val page = answer.getOrNull()
                    publish { it.copy(results = page?.items.orEmpty(), people = page?.people.orEmpty(),
                        page = page?.page ?: 1, hasMore = page?.hasMore == true) }
                }
            }
            val error = if (failed.isEmpty()) null else appString(R.string.error_search_sources,
                failed.sortedBy { it.ordinal }.joinToString(", ") { it.displayName })
            publish { it.copy(searching = false, error = error) }
        }
    }

    /**
     * The reader has moved from typing to the results: that is the one moment a term is worth
     * remembering. The screen calls this when a result gets focus or is opened (or, with a touch
     * screen, when the keyboard's search action is pressed). Terms typed on the way never get here, so
     * the history reads as what was looked for. It is the typed term, trimmed, and not the answer
     * that is on screen: the last answer may still be on its way.
     */
    fun commitSearchToHistory() {
        val state = readState()
        val query = slice(state).query.trim()
        if (query.length < MIN_REMEMBERED) return
        // Already first: nothing to write, and no new state to redraw the page for.
        if (state.searchHistory.firstOrNull() == query) return
        preferencesRepository.rememberSearch(state.activeProfileId, query)
        val history = preferencesRepository.searchHistory(state.activeProfileId)
        updateState { it.copy(searchHistory = history) }
    }

    fun clearSearchHistory() {
        val profile = readState().activeProfileId
        preferencesRepository.clearSearchHistory(profile)
        updateState { if (it.activeProfileId == profile) it.copy(searchHistory = emptyList()) else it }
    }

    /** Appends one verified Seerr page; a changed query discards a late response. */
    fun loadMore() {
        val state = readState()
        val current = slice(state)
        val query = current.query.trim()
        if (
            query.isBlank() ||
            !current.hasMore ||
            current.loadingMore ||
            loadMoreJob?.isActive == true
        ) return
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        val next = current.page + 1
        edit { it.copy(loadingMore = true, moreFailed = false) }
        loadMoreJob = scope.launch {
            val result = attempt { searchDiscover(seerr, query, next) }
            ensureActive()
            edit { latest ->
                if (latest.query.trim() != query) return@edit latest
                val page = result.getOrNull()
                latest.copy(
                    loadingMore = false,
                    results = if (page == null) latest.results else (latest.results + page.items).distinctBy { it.id },
                    page = page?.page ?: latest.page,
                    hasMore = page?.hasMore ?: latest.hasMore,
                    moreFailed = result.isFailure,
                )
            }
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)

    companion object {
        private const val DEBOUNCE_MILLIS = 350L
        private const val MIN_REMEMBERED = 2
        private val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)

        /** Discover's search still lives in the flat fields its screen and tests read. */
        fun discoverSlice(state: ReelstackUiState) = SearchSlice(
            query = state.searchQuery,
            results = state.searchResults,
            libraryResults = state.librarySearchResults,
            people = state.searchPeople,
            searching = state.isSearching,
            error = state.searchError,
            page = state.searchPage,
            hasMore = state.searchHasMore,
            loadingMore = state.loadingMoreSearch,
            moreFailed = state.searchMoreFailed,
        )

        fun withDiscoverSlice(state: ReelstackUiState, search: SearchSlice) = state.copy(
            searchQuery = search.query,
            searchResults = search.results,
            librarySearchResults = search.libraryResults,
            searchPeople = search.people,
            isSearching = search.searching,
            searchError = search.error,
            searchPage = search.page,
            searchHasMore = search.hasMore,
            loadingMoreSearch = search.loadingMore,
            searchMoreFailed = search.moreFailed,
        )
    }
}
