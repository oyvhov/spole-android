package app.reelstack.data.model

enum class ServiceKind(val displayName: String, val role: String) {
    JELLYFIN("Jellyfin", "Medietenar"),
    EMBY("Emby", "Medietenar"),
    SEERR("Seerr", "Legg til innhald"),
    RADARR("Radarr", "Filmar"),
    SONARR("Sonarr", "Seriar"),
}

enum class ConnectionState {
    DEMO,
    TESTING,
    CONNECTED,
    ERROR,
}

enum class HomeSection {
    NOW_PLAYING,
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
    val baseUrl: String,
    val token: String = "",
    val userId: String = "",
    val state: ConnectionState = ConnectionState.DEMO,
    val latencyMs: Long? = null,
    val detail: String? = null,
    val sessionCookie: Boolean = false,
)

data class PlaybackSession(
    val userName: String,
    val deviceName: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val timeLeft: String,
    val streamMethod: String,
    val quality: String,
    val paused: Boolean,
    val artworkUrl: String? = null,
    val sessionId: String? = null,
    val source: ServiceKind? = null,
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
)

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

fun seerrStatusLabel(status: Int?, inLibrary: Boolean = false, requested: Boolean = false): String = when {
    status == 6 -> "Blokkert i Seerr"
    status == 5 || inLibrary -> "I biblioteket ditt"
    status == 4 -> "Delvis i biblioteket"
    status == 3 -> "Førespurd"
    status == 2 -> "Ventar på godkjenning"
    requested -> "Lagd til"
    status == 7 -> "Fjerna frå biblioteket"
    else -> "Kan leggjast til"
}

fun resolvedMediaType(type: String?, subtitle: String): String? = when {
    type.equals("movie", true) -> "Movie"
    type.equals("episode", true) -> "Episode"
    type.equals("tv", true) || type.equals("series", true) -> "Series"
    subtitle.startsWith("Film") -> "Movie"
    subtitle.startsWith("Serie") -> "Series"
    else -> null
}

fun seerrStatusDescription(status: Int?, inLibrary: Boolean = false): String = when {
    status == 6 -> "Denne tittelen er blokkert av administratoren i Seerr."
    status == 5 || inLibrary -> "Klart til å sjå i mediebiblioteket ditt."
    status == 4 -> "Noko av innhaldet er tilgjengeleg, men ikkje alt."
    status == 3 -> "Seerr behandlar tittelen. Nedlastinga er ikkje nødvendigvis starta."
    status == 2 -> "Ein administrator må godkjenne tittelen i Seerr."
    status == 7 -> "Seerr melder at innhaldet er fjerna. Det kan leggjast til på nytt."
    else -> "Tilgjenge og handlingar blir styrte av Seerr-kontoen din."
}

data class CastMember(val name: String, val role: String? = null, val portraitUrl: String? = null)

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
    val cast: List<CastMember> = emptyList(),
)

data class ActivityEvent(
    val id: String,
    val title: String,
    val detail: String,
    val time: String,
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
    val message: String,
)
