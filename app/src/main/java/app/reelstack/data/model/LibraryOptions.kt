package app.reelstack.data.model

import java.net.URLEncoder

enum class LibrarySort(val api: String) { TITLE("SortName"), ADDED("DateCreated"), YEAR("ProductionYear"), RATING("CommunityRating"), RUNTIME("Runtime"), PLAYED("DatePlayed") }
enum class LibraryWatched { ALL, UNWATCHED, WATCHED, IN_PROGRESS }
enum class LibraryResolution { ALL, SD, HD, UHD }
enum class LibraryIcon { LIBRARY, MOVIES, SERIES, KIDS, DOCUMENTARY, MUSIC, CONCERT, ANIMATION, SPORT, FAVOURITES }
data class LibraryFacets(val parentId: String = "", val genres: List<String> = emptyList(), val years: List<String> = emptyList())
data class LibraryFilters(val sort: LibrarySort = LibrarySort.TITLE, val descending: Boolean = false,
    val watched: LibraryWatched = LibraryWatched.ALL, val favourites: Boolean = false,
    val resolution: LibraryResolution = LibraryResolution.ALL, val search: String = "", val genre: String = "", val year: String = "") {
    val activeCount: Int get() = listOf(watched != LibraryWatched.ALL, favourites, resolution != LibraryResolution.ALL,
        search.isNotBlank(), genre.isNotBlank(), year.isNotBlank()).count { it }
    fun query(): String = buildString {
        fun parameter(name: String, value: String) { append('&').append(name).append('=').append(URLEncoder.encode(value, "UTF-8")) }
        parameter("SortBy", sort.api + if (sort != LibrarySort.TITLE) ",SortName" else "")
        parameter("SortOrder", if (descending) "Descending" else "Ascending")
        when(watched) {
            LibraryWatched.WATCHED -> parameter("IsPlayed", "true")
            LibraryWatched.UNWATCHED -> parameter("IsPlayed", "false")
            LibraryWatched.IN_PROGRESS -> parameter("Filters", "IsResumable")
            else -> Unit
        }
        if (favourites) parameter("IsFavorite", "true")
        when(resolution) {
            LibraryResolution.SD -> parameter("MaxWidth", "1279")
            LibraryResolution.HD -> { parameter("MinWidth", "1280"); parameter("MaxWidth", "3839") }
            LibraryResolution.UHD -> parameter("MinWidth", "3840")
            else -> Unit
        }
        search.trim().take(150).takeIf(String::isNotBlank)?.let { parameter("SearchTerm", it) }
        genre.trim().take(100).takeIf(String::isNotBlank)?.let { parameter("Genres", it) }
        year.toIntOrNull()?.takeIf { it in 1800..2200 }?.let { parameter("Years", it.toString()) }
    }
}
