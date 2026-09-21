package app.reelstack.ui.state

import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState

/** Everything the Discover tab and the global-search destination may render. */
data class DiscoverUiState(
    val connections: List<ServiceConnection> = emptyList(),
    val accounts: Map<ServiceKind, ServiceAccount> = emptyMap(),
    val accountErrors: Map<ServiceKind, String> = emptyMap(),
    val discover: List<DiscoverMedia> = emptyList(),
    val searchResults: List<DiscoverMedia> = emptyList(),
    val librarySearchResults: List<LibraryMedia> = emptyList(),
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchError: String? = null,
    val searchHasMore: Boolean = false,
    val loadingMoreSearch: Boolean = false,
    val requestingMediaIds: Set<String> = emptySet(),
) {
    val configuredCount: Int get() = connections.count { it.baseUrl.isNotBlank() }
    val visibleDiscover: List<DiscoverMedia> get() = if (searchQuery.isBlank()) discover else searchResults

    fun verifiedPanelAccount(source: ServiceKind): ServiceAccount? = accounts[source]?.takeIf {
        source !in accountErrors && it.source == source && it.displayName.isNotBlank()
    }
}

/** Transitional adapter while query ownership still belongs to the composition root. */
fun ReelstackUiState.toDiscoverUiState(): DiscoverUiState = DiscoverUiState(
    connections = connections,
    accounts = accounts,
    accountErrors = accountErrors,
    discover = discover,
    searchResults = searchResults,
    librarySearchResults = librarySearchResults,
    searchQuery = searchQuery,
    searchHistory = searchHistory,
    isSearching = isSearching,
    isRefreshing = isRefreshing,
    searchError = searchError,
    searchHasMore = searchHasMore,
    loadingMoreSearch = loadingMoreSearch,
    requestingMediaIds = requestingMediaIds,
)
