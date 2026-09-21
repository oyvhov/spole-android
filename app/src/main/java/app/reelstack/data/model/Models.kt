package app.reelstack.data.model

import androidx.annotation.StringRes
import app.reelstack.R

enum class ServiceKind(val displayName: String) {
    JELLYFIN("Jellyfin"),
    EMBY("Emby"),
    SEERR("Seerr"),
    RADARR("Radarr"),
    SONARR("Sonarr"),
}

enum class ConnectionState {
    DEMO,
    TESTING,
    CONNECTED,
    ERROR,
}

enum class HomeSection {
    NOW_PLAYING,
    CONTINUE_WATCHING,
    FAVOURITES,
    RECOMMENDATIONS,
    RECENT_RELEASES,
    JELLYFIN_MOVIES,
    JELLYFIN_SERIES,
    EMBY_MOVIES,
    EMBY_SERIES,
    UPCOMING,
    DOWNLOADS,
}

/** Preserve older combined switches when upgrading to per-service rows. */
fun decodeHomeSections(saved: Set<String>?): Set<HomeSection> {
    if (saved == null) return HomeSection.entries.toSet()
    return buildSet {
        saved.mapNotNullTo(this) { name -> HomeSection.entries.firstOrNull { it.name == name } }
        if ("RECENT_MOVIES" in saved || "RECENTLY_ADDED" in saved) {
            add(HomeSection.JELLYFIN_MOVIES)
            add(HomeSection.EMBY_MOVIES)
        }
        if ("RECENT_SERIES" in saved || "RECENTLY_ADDED" in saved) {
            add(HomeSection.JELLYFIN_SERIES)
            add(HomeSection.EMBY_SERIES)
        }
    }
}

data class ServiceConnection(
    val kind: ServiceKind,
    val name: String,
    /** The address in use right now. Failover swaps this with [alternateUrl]. */
    val baseUrl: String,
    val token: String = "",
    val userId: String = "",
    val state: ConnectionState = ConnectionState.DEMO,
    val latencyMs: Long? = null,
    val detail: String? = null,
    val sessionCookie: Boolean = false,
    /**
     * A second way to the same server — a LAN address at home and a proxy from outside. The token
     * belongs to the server, not the route, so switching route needs no new sign-in.
     */
    val alternateUrl: String = "",
    /**
     * Fixed at first save and never changed by failover. Local data keyed by server — followed
     * requests above all — must survive a change of route, and keying it on [baseUrl] would
     * orphan every follow the moment the app switched address.
     */
    val identityUrl: String = "",
) {
    /** Stable key for anything stored per server. */
    val identity: String get() = identityUrl.ifBlank { baseUrl }

    val hasAlternate: Boolean get() = alternateUrl.isNotBlank()
}

data class PlaybackSession(
    /** Blank when the server did not say; the screen supplies the word, not the parser. */
    val userName: String,
    val deviceName: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    /** Minutes left. 0 means the title is nearly over. */
    val remainingMinutes: Int,
    val transcoding: Boolean,
    val quality: String,
    val paused: Boolean,
    val artworkUrl: String? = null,
    val sessionId: String? = null,
    val source: ServiceKind? = null,
    /** The numbers behind [subtitle], so the card can spell them the way every shelf does. */
    val season: Int? = null,
    val episode: Int? = null,
) {
    val key: String
        get() = "${source?.name ?: "DEMO"}:${sessionId ?: "$userName:$deviceName:$title"}"
}

data class LibraryMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float? = null,
    val artworkRes: Int,
    val source: ServiceKind,
    val artworkUrl: String? = null,
    val remoteId: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val mediaType: String = "Video",
    val logoUrl: String? = null,
    val heroUrl: String? = null,
    val posterUrl: String? = null,
    /** Playback activity, never the date a file was added to the library. */
    val lastActivityEpochMillis: Long? = null,
    /** The server's DateCreated value, used only for library sorting. */
    val addedAtEpochMillis: Long? = null,
    /** Numbers, so the screen can write them in the reader's language. */
    val season: Int? = null,
    val episode: Int? = null,
    /** How long, and for a season how many episodes it holds. */
    val runtimeMinutes: Int? = null,
    val childCount: Int? = null,
    /**
     * Which Jellyfin library this came from. Resume and Next up are already fetched one library at
     * a time; keeping the answer means a library page can show its own shelf instead of Home's
     * mixture of every library at once.
     */
    val libraryId: String? = null,
    /**
     * The series an episode belongs to, when it is one.
     *
     * An episode page shows the rest of its season, and the season list hangs off the series — so
     * the card has to carry the parent's id or the page would have to guess it from a title.
     */
    val seriesId: String? = null,
    /** The server's own flags, so a card can offer the opposite of what is already true. */
    val favourite: Boolean = false,
    val played: Boolean = false,
    val available: Boolean = true,
    val premiereDate: String? = null,
    val tmdbId: Int? = null,
    val criticRating: Int? = null,
    val tmdbRating: Float? = null,
    val mdblistRating: Float? = null,
) {
    /**
     * A series, whatever the server called it.
     *
     * Jellyfin and Emby answer "Series", Seerr-sourced rows say "tv". Both mean the same thing to
     * a screen deciding whether a tap opens episodes or starts playing.
     */
    val isSeries: Boolean
        get() = mediaType.equals("Series", ignoreCase = true) || mediaType.equals("tv", ignoreCase = true)
}

data class UpcomingMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val airDateEpochMillis: Long,
    val artworkRes: Int,
    val source: ServiceKind,
    val artworkUrl: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val mediaType: String = "Video",
    /** A disc release rather than a digital one. See RemoteUpcomingItem for why this is a flag. */
    val physicalRelease: Boolean = false,
)

val UpcomingMedia.isMovieRelease: Boolean
    get() = mediaType.equals("Movie", true) || (mediaType == "Video" && source == ServiceKind.RADARR)

enum class IncomingState {
    DOWNLOADING,
    REQUESTED,
    READY,
}

data class IncomingMedia(
    val id: String,
    val title: String,
    val source: ServiceKind,
    val status: String,
    val state: IncomingState,
    val artworkRes: Int,
    val artworkUrl: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    /** Percent complete when the queue reports it, so Home and Activity can show the same bar. */
    val progress: Int? = null,
)

data class DiscoverMedia(
    val id: String,
    val title: String,
    val metadata: String,
    val artworkRes: Int,
    val inLibrary: Boolean,
    val requested: Boolean = false,
    val artworkUrl: String? = null,
    val remoteId: Int? = null,
    val mediaType: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val seerrStatus: Int? = null,
)

/**
 * One classification for the type filter, the type badge and the request wording. Seerr normally
 * supplies [mediaType]; the metadata line is the fallback so a missing field cannot silently
 * turn every title into a film.
 */
val DiscoverMedia.isSeries: Boolean
    get() = mediaType?.equals("tv", ignoreCase = true) ?: metadata.startsWith("Serie", ignoreCase = true)

val DiscoverMedia.canRequest: Boolean
    get() = if (isSeries) seerrStatus != 6 else !inLibrary && !requested && seerrStatus !in 2..6

/** Which sentence, not which words: the caller holds the Context and picks the language. */
@StringRes
fun seerrStatusLabel(status: Int?, inLibrary: Boolean = false, requested: Boolean = false): Int = when {
    status == 6 -> R.string.seerr_status_blocked
    status == 5 || inLibrary -> R.string.seerr_status_in_library
    status == 4 -> R.string.seerr_status_partial
    status == 3 -> R.string.seerr_status_requested
    status == 2 -> R.string.seerr_status_awaiting
    requested -> R.string.seerr_status_added
    status == 7 -> R.string.seerr_status_removed
    else -> R.string.seerr_status_can_add
}

fun resolvedMediaType(type: String?, subtitle: String): String? = when {
    type.equals("movie", true) -> "Movie"
    type.equals("episode", true) -> "Episode"
    type.equals("tv", true) || type.equals("series", true) -> "Series"
    subtitle.startsWith("Film") -> "Movie"
    subtitle.startsWith("Serie") -> "Series"
    else -> null
}

@StringRes
fun seerrStatusDescription(status: Int?, inLibrary: Boolean = false): Int = when {
    status == 6 -> R.string.seerr_desc_blocked
    status == 5 || inLibrary -> R.string.seerr_desc_in_library
    status == 4 -> R.string.seerr_desc_partial
    status == 3 -> R.string.seerr_desc_requested
    status == 2 -> R.string.seerr_desc_awaiting
    status == 7 -> R.string.seerr_desc_removed
    else -> R.string.seerr_desc_default
}

data class CastMember(val name: String, val role: String? = null, val portraitUrl: String? = null, val remoteId: String? = null)

data class ContentDetails(
    val key: String,
    val title: String,
    val eyebrow: String,
    val subtitle: String,
    val tagline: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val artworkRes: Int,
    val artworkUrl: String? = null,
    val source: ServiceKind? = null,
    val mediaType: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val statusTitle: String? = null,
    val statusDescription: String? = null,
    val libraryAvailable: Boolean = false,
    val progress: Float? = null,
    val remainingMinutes: Int? = null,
    val quality: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    /** Numbers, so the page can write them in the reader's language rather than as "S19 E09". */
    val season: Int? = null,
    val episode: Int? = null,
    /** The server's flags for this profile, so the page can offer the opposite of what is true. */
    val favourite: Boolean = false,
    val played: Boolean = false,
    /** Set while a write is in flight, so the two controls cannot be pressed into a race. */
    val updating: Boolean = false,
    /**
     * What the file actually contains. A detail page that lists the running time and the codec but
     * not the languages leaves the one question a household with subtitles actually asks — can we
     * watch this in Norwegian — unanswered until playback has already started.
     */
    val audioTracks: List<MediaTrack> = emptyList(),
    val subtitleTracks: List<MediaTrack> = emptyList(),
    /** More than one file for the same title: a 4K and a 1080p cut, a director's edition. */
    val versions: List<MediaVersion> = emptyList(),
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val logoUrl: String? = null,
    val criticRating: Int? = null,
    /** Ratings are kept separate so a server's generic critic score is never mislabeled. */
    val tmdbRating: Float? = null,
    val mdblistRating: Float? = null,
)

/** One playable file behind a title. The id is what playback has to be asked for. */
data class MediaVersion(val id: String, val name: String)

/**
 * The seasons of a series, and the episodes of whichever one is open.
 *
 * A series is the one thing in a library that is not a single object, and a detail page that only
 * offers "choose an episode somewhere else" says nothing about what the series contains. This is
 * what fills the room under the artwork.
 */
data class SeriesBrowse(
    val seriesId: String = "",
    /**
     * The detail page this was loaded for.
     *
     * Usually the series itself, but an episode page shows its own season too, and there the key is
     * the episode's while [seriesId] is the series it belongs to. Screens compare against this, so
     * neither has to work the other out from an id.
     */
    val openedFor: String = "",
    val seasons: List<LibraryMedia> = emptyList(),
    /** What the server says comes next in this series, which is where Play should land. */
    val nextUp: LibraryMedia? = null,
    val selectedSeasonId: String = "",
    val upcomingError: String? = null,
    val episodes: List<LibraryMedia> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * One audio or subtitle stream, named the way the server names it.
 *
 * [index] is the server's stream index, which is what playback has to be asked for, so a choice
 * made on the detail page survives the trip into the player unchanged.
 */
data class MediaTrack(
    val index: Int,
    val label: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val forced: Boolean = false,
    val codec: String? = null,
)

data class ActivityEvent(
    val id: String,
    val title: String,
    /** The status line, as a resource and its arguments — the words are chosen where a Context is. */
    val detail: app.reelstack.localization.LocalizedText,
    val time: app.reelstack.localization.LocalizedText,
    /**
     * Which stage a Seerr request is at, or null for anything that is not one.
     *
     * A request in flight and a title you only follow looked identical on the activity timeline:
     * same card, same grey line. This is what lets them be told apart.
     */
    val stage: RequestStage? = null,
    /**
     * When the event actually happened. [time] is a display string ("For 5 dagar sidan"), and two
     * such strings can share a prefix without sharing a day, so day grouping reads this instead.
     */
    val timeEpochMillis: Long? = null,
    val progress: Int? = null,
    val complete: Boolean = false,
    val source: ServiceKind? = null,
    val artworkRes: Int? = null,
    val artworkUrl: String? = null,
    /** "Episode" or "Movie" when known, so the row can pick the right artwork frame. */
    val mediaType: String? = null,
)

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    /** Which sentence, not the sentence: the words are chosen where a Context exists. */
    val message: app.reelstack.localization.LocalizedText,
    val detectedKind: ServiceKind? = null,
)
