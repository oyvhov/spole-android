package app.reelstack.data.model

import java.net.URLEncoder

enum class LibrarySort(val api: String) { TITLE("SortName"), ADDED("DateCreated"), YEAR("ProductionYear"), RATING("CommunityRating"), RUNTIME("Runtime"), PLAYED("DatePlayed") }
enum class LibraryWatched { ALL, UNWATCHED, WATCHED, IN_PROGRESS }
enum class LibraryResolution { ALL, SD, HD, UHD }
enum class LibraryIcon { LIBRARY, MOVIES, SERIES, KIDS, DOCUMENTARY, MUSIC, CONCERT, ANIMATION, SPORT, FAVOURITES }
/**
 * How a library is drawn, as opposed to what it contains.
 *
 * Jellyfin keeps view, card size and image type per library, and people expect that: a film library
 * wants posters, a recordings library wants thumbs, and the size that suits a phone is not the size
 * that suits a television. These are display choices, so they are stored on the device and never
 * sent to the server.
 */
enum class LibraryView { GRID, LIST }

enum class LibraryCardSize(val scale: Float) { SMALL(.8f), MEDIUM(1f), LARGE(1.25f) }

/**
 * Which image a card asks the server for. `AUTO` follows the media type — posters for films and
 * series, thumbs for episodes — which is right often enough that most libraries never change it.
 */
enum class LibraryArtType(val api: String?, val ratio: Float) {
    AUTO(null, 2f / 3f),
    POSTER("Primary", 2f / 3f),
    THUMB("Thumb", 16f / 9f),
    BANNER("Banner", 1000f / 185f),
    LOGO("Logo", 16f / 9f),
}

/** Saved per library, keyed by the library's own id. */
data class LibraryDisplay(
    val view: LibraryView = LibraryView.GRID,
    val size: LibraryCardSize = LibraryCardSize.MEDIUM,
    val artType: LibraryArtType = LibraryArtType.AUTO,
    /** Titles under the artwork. Off gives a denser wall of covers, which some people prefer. */
    val showTitles: Boolean = true,
) {
    fun encode(): String = listOf(view.name, size.name, artType.name, showTitles).joinToString("|")

    companion object {
        fun decode(value: String?): LibraryDisplay {
            val parts = value?.split('|').orEmpty()
            if (parts.size < 4) return LibraryDisplay()
            return LibraryDisplay(
                view = LibraryView.entries.firstOrNull { it.name == parts[0] } ?: LibraryView.GRID,
                size = LibraryCardSize.entries.firstOrNull { it.name == parts[1] } ?: LibraryCardSize.MEDIUM,
                artType = LibraryArtType.entries.firstOrNull { it.name == parts[2] } ?: LibraryArtType.AUTO,
                showTitles = parts[3].toBooleanStrictOrNull() ?: true,
            )
        }
    }
}

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

/**
 * What a single library shows above its grid.
 *
 * Kept apart from Home's resume list on purpose: Home answers "what was I watching", a library page
 * answers "what was I watching *here*", and blending the two is what made every library page look
 * like the same wall of covers.
 */
data class LibraryShelves(
    val libraryId: String = "",
    val resume: List<LibraryMedia> = emptyList(),
    val nextUp: List<LibraryMedia> = emptyList(),
    val loading: Boolean = false,
)
