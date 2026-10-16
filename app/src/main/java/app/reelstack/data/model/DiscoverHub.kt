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
    /** Length, genres and the like for titles that had focus on a television, newest last. */
    val focusFacts: Map<String, List<String>> = emptyMap(),
) {
    val allMedia: List<DiscoverMedia> get() = rows.values.flatMap { it.items } + gridRow.items
    val anyFailed: Boolean get() = rows.values.any { it.failed }

    /** Remembers [facts] for [id]; only the [limit] most recently looked at are kept. */
    fun withFocusFacts(id: String, facts: List<String>, limit: Int = 100): DiscoverHubState =
        copy(focusFacts = (focusFacts - id + (id to facts)).entries.toList().takeLast(limit).associate { it.toPair() })
}

/** The first billed people of the first titles, without repeats. */
fun peopleFrom(casts: List<List<CastMember>>, perTitle: Int = 2, limit: Int = 12): List<DiscoverPerson> =
    casts.flatMap { cast -> cast.filter { it.seerrPersonId != null }.take(perTitle) }
        .distinctBy { it.seerrPersonId }
        .take(limit)
        .map { DiscoverPerson(it.seerrPersonId!!, it.name, it.portraitUrl, it.role) }
