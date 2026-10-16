package app.reelstack.ui.state

import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState

/** Everything the Discover tab and the global-search destination may render. */
data class DiscoverUiState(
    val sessionScope: app.reelstack.data.repository.SessionScope? = null,
    val connections: List<ServiceConnection> = emptyList(),
    val accounts: Map<ServiceKind, ServiceAccount> = emptyMap(),
    val accountErrors: Map<ServiceKind, String> = emptyMap(),
    val discover: List<DiscoverMedia> = emptyList(),
    val searchResults: List<DiscoverMedia> = emptyList(),
    val searchPeople: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),
    val librarySearchResults: List<LibraryMedia> = emptyList(),
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchError: String? = null,
    val searchHasMore: Boolean = false,
    val loadingMoreSearch: Boolean = false,
    val requestingMediaIds: Set<String> = emptySet(),
    val hub: app.reelstack.data.model.DiscoverHubState = app.reelstack.data.model.DiscoverHubState(),
    val trackedRequests: List<app.reelstack.data.model.TrackedRequest> = emptyList(),
) {
    val configuredCount: Int get() = connections.count { it.baseUrl.isNotBlank() }
    val visibleDiscover: List<DiscoverMedia> get() = if (searchQuery.isBlank()) discover else searchResults

    fun verifiedPanelAccount(source: ServiceKind): ServiceAccount? = accounts[source]?.takeIf {
        source !in accountErrors && it.source == source && it.displayName.isNotBlank()
    }
}

/** Transitional adapter while query ownership still belongs to the composition root. */
fun ReelstackUiState.toDiscoverUiState(): DiscoverUiState = DiscoverUiState(
    sessionScope = sessionScope,
    connections = connections,
    accounts = accounts,
    accountErrors = accountErrors,
    discover = discover,
    searchResults = searchResults,
    searchPeople = searchPeople,
    librarySearchResults = librarySearchResults,
    searchQuery = searchQuery,
    searchHistory = searchHistory,
    isSearching = isSearching,
    isRefreshing = isRefreshing,
    searchError = searchError,
    searchHasMore = searchHasMore,
    loadingMoreSearch = loadingMoreSearch,
    requestingMediaIds = requestingMediaIds,
    // Without any server the preview still has a row: the demo titles, under «Populært no».
    hub = if (configuredCount == 0 && discoverHub.rows.isEmpty() && discover.isNotEmpty())
        discoverHub.copy(rows = mapOf(app.reelstack.data.model.DiscoverRowKind.TRENDING to app.reelstack.data.model.DiscoverRowState(discover)))
    else discoverHub,
    trackedRequests = trackedRequests,
)
