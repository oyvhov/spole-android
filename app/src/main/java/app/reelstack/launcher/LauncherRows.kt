package app.reelstack.launcher

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.repository.AccountRows
import app.reelstack.ui.screens.railArtworkUrl

/**
 * One card for Stova: what it shows, and the account `spole://open` opens it with. The picture
 * addresses carry no token; neither does anything else here.
 */
data class LauncherRowItem(
    val row: String,
    val kind: String,
    val user: String,
    val server: String,
    val id: String,
    val type: String,
    val title: String,
    val seriesName: String?,
    val season: Int?,
    val episode: Int?,
    val year: Int?,
    val runtime: Int?,
    val progress: Float?,
    val overview: String?,
    val wide: String?,
    val poster: String?,
    val backdrop: String?,
    val logo: String?,
    val community: Double?,
    val critic: Int?,
    /** The server's rating as it gives it ("15", "PG-13"); Stova keeps its hero within the profile's age with it. */
    val official: String? = null,
)

/** The rows Stova gets, in the order it shows them. */
object LauncherRowNames {
    const val WATCHING = "continue"
    const val MOVIES = "latest_movies"
    const val SERIES = "latest_series"
}

/** The profile with this account, an own or child profile before the main one, as for `spole://profile`. */
fun launcherProfileFor(kind: ServiceKind, user: String, profiles: List<UserProfile>,
    connectionsOf: (String) -> List<ServiceConnection>): UserProfile? {
    fun has(id: String) = connectionsOf(id).any { it.kind == kind && it.userId == user && it.token.isNotBlank() }
    return profiles.filterNot { it.isMain }.sortedBy { it.isKid }.firstOrNull { has(it.id) }
        ?: profiles.firstOrNull { it.isMain }?.takeIf { has("") }
}

/** The library accounts of a profile that can be asked on its behalf. */
fun launcherLibraryAccounts(connections: List<ServiceConnection>): List<ServiceConnection> = connections.filter {
    (it.kind == ServiceKind.JELLYFIN || it.kind == ServiceKind.EMBY) && it.token.isNotBlank() && it.userId.isNotBlank()
}.sortedBy { it.kind != ServiceKind.JELLYFIN }

/** One account's rows as cards for Stova. */
fun launcherRowItems(connection: ServiceConnection, rows: AccountRows): List<LauncherRowItem> =
    rows.watching.map { it.card(LauncherRowNames.WATCHING, connection) } +
        rows.recentMovies.map { it.card(LauncherRowNames.MOVIES, connection) } +
        // New episodes of one series are one series to Stova: the same picture four times said nothing.
        rows.recentSeries.distinctBy { it.seriesId ?: it.id }.map { it.card(LauncherRowNames.SERIES, connection) }

private fun LibraryMedia.card(row: String, connection: ServiceConnection): LauncherRowItem {
    val episodeLike = mediaType.equals("Episode", true) || (season != null && episode != null)
    return LauncherRowItem(
        row = row,
        kind = connection.kind.name.lowercase(),
        user = connection.userId,
        server = connection.name,
        id = remoteId ?: id.substringAfter('-'),
        type = mediaType,
        title = title,
        // Spole's cards already carry the series in the title of an episode.
        seriesName = title.takeIf { episodeLike },
        season = season,
        episode = episode,
        year = facts.firstNotNullOfOrNull { fact -> fact.takeIf { it.matches(Regex("\\d{4}")) }?.toIntOrNull() }
            ?: premiereDate?.take(4)?.toIntOrNull(),
        runtime = runtimeMinutes,
        progress = progress,
        overview = overview,
        wide = railArtworkUrl(wide = true, heroUrl, posterUrl, artworkUrl, episodeLike),
        poster = posterUrl ?: artworkUrl,
        backdrop = backdropUrl ?: heroUrl,
        logo = logoUrl,
        community = tmdbRating?.let { it / 10.0 },
        critic = criticRating,
        official = officialRating,
    )
}

/** What an item action from Stova ended in. Fixed words: R8 renames classes in a release. */
object LauncherActionResult {
    const val OK = "ok"
    const val FAILED = "failed"
    const val UNKNOWN = "unknown"
}

/** The actions Stova may ask for on one title. */
enum class LauncherItemAction(val word: String) {
    WATCHED("watched"),
    REMOVE_RESUME("remove_resume");

    companion object {
        fun of(word: String?): LauncherItemAction? = entries.firstOrNull { it.word == word }
    }
}
