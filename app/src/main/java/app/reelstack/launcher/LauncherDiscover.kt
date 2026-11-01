package app.reelstack.launcher

import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.tmdbScore
import app.reelstack.data.network.RemoteMediaDetails
import app.reelstack.localization.LocalizedText

/**
 * What a person may do with Seerr through Stova, as the `seerr` extra of `discover` says it. Fixed
 * words: R8 renames classes in a release.
 */
enum class LauncherSeerrAccess(val wire: String) {
    /** No Seerr, or a child: Stova shows nothing and asks nothing. */
    NONE("none"),
    /** An API key: Spole may look on its behalf, but nothing it asks for would be in the person's name. */
    BROWSE("browse"),
    /** A personal session: the same Seerr account Spole asks with, so asking is allowed. */
    PERSONAL("personal"),
}

/**
 * Children have no Discover anywhere, and a Seerr that is not set up (no address, or no key or
 * session) has nothing to give. A session cookie means the person signed in as themselves; a bare
 * API key is the server's, so it may only look.
 */
fun launcherSeerrAccess(profileIsChild: Boolean, seerr: ServiceConnection?): LauncherSeerrAccess = when {
    profileIsChild || seerr == null || seerr.baseUrl.isBlank() || seerr.token.isBlank() -> LauncherSeerrAccess.NONE
    seerr.sessionCookie -> LauncherSeerrAccess.PERSONAL
    else -> LauncherSeerrAccess.BROWSE
}

/** The columns of each Discover answer, in the order Stova reads them. */
object LauncherDiscoverColumns {
    val ITEM = arrayOf("row", "type", "tmdb", "title", "year", "overview", "poster", "backdrop", "status", "community", "genres")
    val TITLE = arrayOf("type", "tmdb", "title", "year", "tagline", "overview", "poster", "backdrop", "runtime", "genres",
        "community", "critic", "status", "can_request", "request_id", "request_status", "trailer")
    val SEASON = arrayOf("number", "name", "episodes", "status", "air_date")
    val REQUEST = arrayOf("type", "tmdb", "title", "poster", "backdrop", "stage", "percent", "seasons", "request_id", "is4k")
}

/** The rows Stova gets from Discover. Fixed words: R8 renames classes in a release. */
object LauncherDiscoverRows {
    const val TRENDING = "trending"
    const val UPCOMING_MOVIES = "upcoming_movies"
    const val UPCOMING_SERIES = "upcoming_series"
    const val SIMILAR = "similar"
    const val RECOMMENDED = "recommended"
    const val SEARCH = "search"
}

/** What a request from Stova ended in. Fixed words: R8 renames classes in a release. */
enum class LauncherDiscoverResult(val wire: String) {
    OK("ok"),
    ALREADY("already"),
    NOT_PERSONAL("not_personal"),
    FORBIDDEN("forbidden"),
    FAILED("failed"),
    UNKNOWN("unknown"),
}

/** Four digits that are not part of a longer number: «2024 · Film» has a year, «Episode 12345» has none. */
private val YEAR = Regex("(?<!\\d)\\d{4}(?!\\d)")

private fun yearIn(text: String): Int? = YEAR.find(text)?.value?.toIntOrNull()

/**
 * Spole keeps a TMDB score out of 100, as Jellyfin's community rating is kept; every score Stova
 * gets from Spole is out of ten (see `community` of the library rows), and Stova draws nothing
 * outside 0 to 10. The score is one decimal at most, so rounding only removes the float noise of
 * 7.1 × 10.
 */
private fun outOfTen(score: Float?): Double? = score?.let { Math.round(it) / 10.0 }

/**
 * The bare card a title Stova names is asked about by: its type and TMDB id, with Spole's own id for
 * a Seerr title. It knows no status, so whether it can be asked for is judged by what Seerr says.
 */
fun discoverMinimal(type: String, tmdb: Int): DiscoverMedia =
    DiscoverMedia("seerr-$type-$tmdb", "", "", 0, false, remoteId = tmdb, mediaType = type)

/** One title of a Discover row as a card: [row] is one of [LauncherDiscoverRows], the columns [LauncherDiscoverColumns.ITEM]. */
fun discoverItemRow(row: String, media: DiscoverMedia): Array<Any?> = arrayOf(
    row,
    media.mediaType,
    media.remoteId,
    media.title,
    yearIn(media.metadata),
    media.overview,
    media.artworkUrl,
    media.backdropUrl,
    // 1 is Seerr's «unknown», the status of a title it has never heard of.
    media.seerrStatus ?: 1,
    outOfTen(media.tmdbScore()),
    media.genres.joinToString(", "),
)

/**
 * The title page: the card the person came from, completed by what Seerr says about the title
 * itself ([details], [critic] for Rotten Tomatoes, [tracked] for the request they follow). The page
 * may open from a bare card that only knows the type and the id, so everything [details] has wins.
 *
 * `can_request` is 1 only for a personal Seerr session and a title Seerr's own rules let be asked
 * for, judged with the status [details] gave: a bare card knows no status, and every title would
 * look requestable, the ones already in the library too. `runtime` is not sent yet.
 */
fun discoverTitleRow(media: DiscoverMedia, details: RemoteMediaDetails, critic: Int?, access: LauncherSeerrAccess,
    tracked: TrackedRequest?): Array<Any?> {
    val status = details.seerrStatus ?: media.seerrStatus
    val requestable = access == LauncherSeerrAccess.PERSONAL && media.copy(seerrStatus = status).canRequest
    return arrayOf(
        media.mediaType,
        media.remoteId,
        details.title?.takeIf { it.isNotBlank() } ?: media.title,
        yearIn(media.metadata)
            ?: details.facts.firstNotNullOfOrNull { fact -> fact.literal?.takeIf { it.length == 4 && it.all(Char::isDigit) }?.toIntOrNull() },
        details.tagline?.takeIf { it.isNotBlank() },
        details.overview ?: media.overview,
        details.artworkUrl ?: media.artworkUrl,
        details.backdropUrl ?: media.backdropUrl,
        null,
        details.genres.ifEmpty { media.genres }.joinToString(", "),
        outOfTen(details.tmdbRating ?: media.tmdbScore()),
        critic,
        status ?: 1,
        if (requestable) 1 else 0,
        tracked?.requestId,
        tracked?.stage?.name?.lowercase(),
        details.trailerUrl,
    )
}

/**
 * The seasons of a series, one row each, for the season choice. Seerr's season names are
 * [LocalizedText], decided by the data layer without a language; [name] says them, and the provider
 * holds the Context that can (`{ it.text(context) }`).
 */
fun discoverSeasonRows(details: RemoteMediaDetails, name: (LocalizedText) -> String): List<Array<Any?>> =
    details.seasons.map { season ->
        arrayOf<Any?>(season.number, name(season.name), season.episodes, season.status, season.airDate?.toString())
    }

/**
 * A request the person follows. `request_id` is Seerr's own and is missing for follows from before
 * Spole kept it, which cannot be withdrawn from Stova. Spole keeps no backdrop for a followed
 * request, so there is none.
 */
fun discoverRequestRow(request: TrackedRequest): Array<Any?> = arrayOf(
    request.mediaType,
    request.mediaId,
    request.title,
    request.artworkUrl,
    null,
    request.stage.name.lowercase(),
    request.percent,
    request.seasons.sorted().joinToString(","),
    request.requestId,
    if (request.is4k) 1 else 0,
)
