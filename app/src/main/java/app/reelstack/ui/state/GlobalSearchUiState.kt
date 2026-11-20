package app.reelstack.ui.state

import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.canRequestType
import app.reelstack.data.model.isSeries
import app.reelstack.data.model.resolvedMediaType
import app.reelstack.ui.ReelstackUiState

/**
 * One search: what was typed and what came back. Discover's field and the global search screen
 * each own one, so a search opened from Home neither borrows nor overwrites Discover's words.
 */
data class SearchSlice(
    val query: String = "",
    val results: List<DiscoverMedia> = emptyList(),
    val libraryResults: List<LibraryMedia> = emptyList(),
    val people: List<app.reelstack.data.model.DiscoverPerson> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val moreFailed: Boolean = false,
)

/** What the global search screen needs, and nothing that makes it redraw for unrelated reasons. */
data class GlobalSearchUiState(
    val search: SearchSlice = SearchSlice(),
    val history: List<String> = emptyList(),
    val requestingMediaIds: Set<String> = emptySet(),
    val seerrAccount: ServiceAccount? = null,
    val configuredCount: Int = 0,
) {
    /** The same rule Discover applies before it offers a request button. */
    fun canRequest(media: DiscoverMedia): Boolean =
        configuredCount == 0 || seerrAccount?.let { media.isSeries || !it.isPersonal || it.canRequestType("movie") } == true

    /**
     * Titles you can request, minus those Seerr already reports as available and that the library
     * group shows anyway: one title, one card, in the group where a tap plays it.
     */
    val requestable = discoverSearchTitles(search.results, search.libraryResults)
}

internal fun discoverSearchTitles(titles: List<DiscoverMedia>, library: List<LibraryMedia>): List<DiscoverMedia> =
    titles.filterNot { hit -> hit.inLibrary && library.any { it.matchesSearchHit(hit) } }

private fun LibraryMedia.matchesSearchHit(hit: DiscoverMedia): Boolean {
    val kind = resolvedMediaType(mediaType, subtitle)
    val hitKind = resolvedMediaType(hit.mediaType, hit.metadata)
    if (kind != null && hitKind != null && kind != hitKind) return false
    if (tmdbId != null && hit.remoteId != null) {
        if (tmdbId != hit.remoteId) return false
        if (kind != null && kind == hitKind) return true
    }
    val year = premiereDate?.take(4)?.toIntOrNull()
    val hitYear = hit.metadata.toIntOrNull()
    if (year != null && hitYear != null && year != hitYear) return false
    val name = searchTitleKey(title)
    return name.isNotEmpty() && name == searchTitleKey(hit.title)
}

internal fun searchTitleKey(title: String): String = title.lowercase().filter(Char::isLetterOrDigit)

fun ReelstackUiState.toGlobalSearchUiState(): GlobalSearchUiState = GlobalSearchUiState(
    search = globalSearch,
    history = searchHistory,
    requestingMediaIds = requestingMediaIds,
    seerrAccount = accounts[ServiceKind.SEERR],
    configuredCount = configuredCount,
)
