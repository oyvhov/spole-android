package app.reelstack.ui

import app.reelstack.AppContainer
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns cancellable, profile-scoped global-search work. */
internal class DiscoverSearchCoordinator(
    private val container: AppContainer,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
) {
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    fun cancel() {
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = null
        loadMoreJob = null
    }

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
            updateState {
                it.copy(
                    searchQuery = value,
                    searchResults = emptyList(),
                    librarySearchResults = emptyList(),
                    isSearching = false,
                    searchError = null,
                    searchPage = 1,
                    searchHasMore = false,
                )
            }
            return
        }
        if (seerr == null && mediaServers.isEmpty()) {
            updateState {
                it.copy(
                    searchQuery = value,
                    searchResults = it.discover.filter { media -> media.title.contains(query, ignoreCase = true) },
                    librarySearchResults = emptyList(),
                    isSearching = false,
                    searchError = null,
                )
            }
            return
        }
        updateState {
            it.copy(
                searchQuery = value,
                searchResults = emptyList(),
                librarySearchResults = emptyList(),
                isSearching = true,
                searchError = null,
                searchPage = 1,
                searchHasMore = false,
            )
        }
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
            if (!isActive || readState().searchQuery.trim() != query) return@launch
            container.preferencesRepository.rememberSearch(readState().activeProfileId, query)
            updateState {
                it.copy(
                    searchResults = discoverResult.getOrNull()?.items.orEmpty(),
                    searchPage = discoverResult.getOrNull()?.page ?: 1,
                    searchHasMore = discoverResult.getOrNull()?.hasMore == true,
                    librarySearchResults = libraryResult.getOrDefault(emptyList()),
                    isSearching = false,
                    searchError = when {
                        discoverResult.isFailure && libraryResult.isFailure -> appString(R.string.error_search_all)
                        discoverResult.isFailure && seerr != null -> appString(R.string.error_search_seerr)
                        libraryResult.isFailure -> appString(R.string.error_search_libraries)
                        else -> null
                    },
                    searchHistory = container.preferencesRepository.searchHistory(it.activeProfileId),
                )
            }
        }
    }

    /** Appends one verified Seerr page; a changed query discards a late response. */
    fun loadMore() {
        val state = readState()
        val query = state.searchQuery.trim()
        if (
            query.isBlank() ||
            !state.searchHasMore ||
            state.loadingMoreSearch ||
            state.isSearching ||
            loadMoreJob?.isActive == true
        ) return
        val seerr = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        val next = state.searchPage + 1
        updateState { it.copy(loadingMoreSearch = true) }
        loadMoreJob = scope.launch {
            val result = attempt {
                withContext(Dispatchers.IO) { container.mediaSyncRepository.search(seerr, query, next) }
            }
            updateState { current ->
                if (current.searchQuery.trim() != query) return@updateState current.copy(loadingMoreSearch = false)
                val page = result.getOrNull()
                current.copy(
                    loadingMoreSearch = false,
                    searchResults = if (page == null) current.searchResults
                    else (current.searchResults + page.items).distinctBy { it.id },
                    searchPage = page?.page ?: current.searchPage,
                    searchHasMore = page?.hasMore == true,
                    searchError = if (result.isFailure) appString(R.string.error_search_more) else current.searchError,
                )
            }
        }
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(container.appContext).getString(resId, *args)

    private companion object {
        const val DEBOUNCE_MILLIS = 350L
        val MEDIA_SERVERS = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
    }
}
