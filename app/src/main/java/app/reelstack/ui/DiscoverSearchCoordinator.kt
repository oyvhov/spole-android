package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.state.SearchSlice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns cancellable, profile-scoped search work for one search field. Discover and the global search
 * screen each have their own instance; [slice] and [withSlice] say which part of the state it owns.
 */
internal class DiscoverSearchCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
    private val slice: (ReelstackUiState) -> SearchSlice = ::discoverSlice,
    private val withSlice: (ReelstackUiState, SearchSlice) -> ReelstackUiState = ::withDiscoverSlice,
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
                withSlice(it, slice(it).copy(
                    query = value,
                    results = it.discover.filter { media -> media.title.contains(query, ignoreCase = true) },
                    libraryResults = emptyList(),
                    searching = false,
                    error = null,
                ))
            }
            return
        }
        edit { SearchSlice(query = value, searching = true) }
        searchJob = scope.launch {
            delay(DEBOUNCE_MILLIS)
            // Your own libraries and Seerr answer independently. A title you own must remain
            // discoverable when Seerr is unavailable, and a Seerr result must remain useful when
            // one media server is down.
            val libraryDeferred = async {
                if (mediaServers.isEmpty()) Result.success(emptyList()) else attempt {
                    withContext(Dispatchers.IO) {
                        container.mediaSyncRepository.searchLibraries(mediaServers, query)
                    }
                }
            }
            val discoverResult = if (seerr == null) {
                Result.success(app.reelstack.data.repository.MediaSyncRepository.SearchPage(emptyList(), 1, false))
            } else attempt {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query, page = 1) }
            }
            val libraryResult = libraryDeferred.await()
            if (!isActive || slice(readState()).query.trim() != query) return@launch
            container.preferencesRepository.rememberSearch(readState().activeProfileId, query)
            updateState {
                withSlice(it, slice(it).copy(
                    results = discoverResult.getOrNull()?.items.orEmpty(),
                    page = discoverResult.getOrNull()?.page ?: 1,
                    hasMore = discoverResult.getOrNull()?.hasMore == true,
                    libraryResults = libraryResult.getOrDefault(emptyList()),
                    searching = false,
                    error = when {
                        discoverResult.isFailure && libraryResult.isFailure -> appString(R.string.error_search_all)
                        discoverResult.isFailure && seerr != null -> appString(R.string.error_search_seerr)
                        libraryResult.isFailure -> appString(R.string.error_search_libraries)
                        else -> null
                    },
                )).copy(searchHistory = container.preferencesRepository.searchHistory(it.activeProfileId))
            }
        }
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
            current.searching ||
            loadMoreJob?.isActive == true
        ) return
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        val next = current.page + 1
        edit { it.copy(loadingMore = true) }
        loadMoreJob = scope.launch {
            val result = attempt {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query, next) }
            }
            edit { latest ->
                if (latest.query.trim() != query) return@edit latest.copy(loadingMore = false)
                val page = result.getOrNull()
                latest.copy(
                    loadingMore = false,
                    results = if (page == null) latest.results else (latest.results + page.items).distinctBy { it.id },
                    page = page?.page ?: latest.page,
                    hasMore = page?.hasMore == true,
                    error = if (result.isFailure) appString(R.string.error_search_more) else latest.error,
                )
            }
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)

    companion object {
        private const val DEBOUNCE_MILLIS = 350L
        private val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)

        /** Discover's search still lives in the flat fields its screen and tests read. */
        fun discoverSlice(state: ReelstackUiState) = SearchSlice(
            query = state.searchQuery,
            results = state.searchResults,
            libraryResults = state.librarySearchResults,
            searching = state.isSearching,
            error = state.searchError,
            page = state.searchPage,
            hasMore = state.searchHasMore,
            loadingMore = state.loadingMoreSearch,
        )

        fun withDiscoverSlice(state: ReelstackUiState, search: SearchSlice) = state.copy(
            searchQuery = search.query,
            searchResults = search.results,
            librarySearchResults = search.libraryResults,
            isSearching = search.searching,
            searchError = search.error,
            searchPage = search.page,
            searchHasMore = search.hasMore,
            loadingMoreSearch = search.loadingMore,
        )
    }
}
