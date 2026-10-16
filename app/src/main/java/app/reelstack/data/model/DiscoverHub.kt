package app.reelstack.data.model

/** Seerr's rows on the Discover page, in the order they are drawn. */
enum class DiscoverRowKind(val path: String) {
    TRENDING("discover/trending"),
    UPCOMING_MOVIES("discover/movies/upcoming"),
    UPCOMING_SERIES("discover/tv/upcoming"),
}

/** One row's own state; a row that fails disappears without stopping the others. */
data class DiscoverRowState(
    val items: List<DiscoverMedia> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = false,
)

data class DiscoverPerson(val id: Int, val name: String, val portraitUrl: String?, val role: String? = null)

data class DiscoverGenre(val id: Int, val name: String, val mediaType: String, val backdropUrl: String?)

/** What a row's «Alle» opens. */
sealed interface DiscoverGridTarget {
    data class Row(val kind: DiscoverRowKind) : DiscoverGridTarget
    data class Genre(val genre: DiscoverGenre) : DiscoverGridTarget
}

data class DiscoverHubState(
    val rows: Map<DiscoverRowKind, DiscoverRowState> = emptyMap(),
    val people: List<DiscoverPerson> = emptyList(),
    val genres: List<DiscoverGenre> = emptyList(),
    val grid: DiscoverGridTarget? = null,
    val gridRow: DiscoverRowState = DiscoverRowState(),
    /** The profile and server the rows were loaded for; a late answer for another is dropped. */
    val scope: String? = null,
) {
    val allMedia: List<DiscoverMedia> get() = rows.values.flatMap { it.items } + gridRow.items
    val anyFailed: Boolean get() = rows.values.any { it.failed }
}

/** The first billed people of the first titles, without repeats. */
fun peopleFrom(casts: List<List<CastMember>>, perTitle: Int = 2, limit: Int = 12): List<DiscoverPerson> =
    casts.flatMap { cast -> cast.filter { it.seerrPersonId != null }.take(perTitle) }
        .distinctBy { it.seerrPersonId }
        .take(limit)
        .map { DiscoverPerson(it.seerrPersonId!!, it.name, it.portraitUrl, it.role) }
