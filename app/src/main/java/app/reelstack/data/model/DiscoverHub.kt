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
    /** Length, genres and synopsis for titles that had focus on a television, newest last. */
    val focusDetails: Map<String, FocusDetails> = emptyMap(),
    /** Where the remote left the rows for «Alle», a genre or your requests, to come back to. */
    val returnTo: DiscoverReturn? = null,
) {
    val allMedia: List<DiscoverMedia> get() = rows.values.flatMap { it.items } + gridRow.items
    val anyFailed: Boolean get() = rows.values.any { it.failed }

    /** Remembers [details] for [id]; only the [limit] most recently looked at are kept. */
    fun withFocusDetails(id: String, details: FocusDetails, limit: Int = 100): DiscoverHubState =
        copy(focusDetails = (focusDetails - id + (id to details)).entries.toList().takeLast(limit).associate { it.toPair() })
}

/** What the TV header adds to a row's own data once a title has had focus. */
data class FocusDetails(val facts: List<String>, val overview: String?, val backdropUrl: String? = null)

/** The next page of a paged list. TMDB lists shift while they are read, so repeats are dropped. */
fun DiscoverRowState.withPage(more: List<DiscoverMedia>, page: Int, hasMore: Boolean): DiscoverRowState =
    DiscoverRowState(((if (page == 1) emptyList() else items) + more).distinctBy { it.id }, page = page, hasMore = hasMore)

/** A title just asked for: «Førespurd», or «Ventar på godkjenning» when Seerr needs an approval first. */
fun DiscoverMedia.asRequested(waitsForApproval: Boolean): DiscoverMedia = copy(
    requested = true,
    seerrStatus = seerrStatus?.takeIf { it in 4..6 } ?: if (waitsForApproval) 2 else 3,
)

/** The Seerr status a request line stands for, so its title page opens with the same words. */
/** The tile the remote left the rows from ([key]) and how far the rows were scrolled. */
data class DiscoverReturn(val key: String, val index: Int, val offset: Int)

/** The TMDB score a row title already shows as «★ 7.1», out of 100 as Spole keeps them. */
fun DiscoverMedia.tmdbScore(): Float? = facts.firstNotNullOfOrNull { fact ->
    fact.removePrefix("★").takeIf { it != fact }?.trim()?.replace(',', '.')?.toFloatOrNull()
}?.takeIf { it > 0f && it <= 10f }?.times(10f)

fun RequestStage.seerrStatus(): Int? = when (this) {
    RequestStage.AVAILABLE -> 5
    RequestStage.REQUESTED, RequestStage.DOWNLOADING, RequestStage.IMPORTING -> 3
    else -> null
}

/** The first billed people of the first titles, without repeats. */
fun peopleFrom(casts: List<List<CastMember>>, perTitle: Int = 2, limit: Int = 12): List<DiscoverPerson> =
    casts.flatMap { cast -> cast.filter { it.seerrPersonId != null }.take(perTitle) }
        .distinctBy { it.seerrPersonId }
        .take(limit)
        .map { DiscoverPerson(it.seerrPersonId!!, it.name, it.portraitUrl, it.role) }
