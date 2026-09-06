package app.reelstack.data.network

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate

/** Date windows, not DateCreated or approval/download timestamps. */
internal class ReleaseWindow(clock: Clock = Clock.systemDefaultZone()) {
    val today: LocalDate = LocalDate.now(clock)
    val start: LocalDate = today.minusDays(28)
    val end: LocalDate = today.plusDays(28)
    fun date(value: String?): LocalDate? = value?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    fun recent(value: String?) = date(value)?.let { it >= start && it <= today } == true
    fun upcoming(value: String?) = date(value)?.let { it > today && it <= end } == true
}

internal fun libraryRelease(item: RemoteLibraryItem, source: ServiceKind, window: ReleaseWindow): RemoteUpcomingItem? {
    // A movie's library PremiereDate is normally cinema, not digital. Never use it here.
    if (!item.available || !window.recent(item.premiereDate) || item.mediaType != "Episode") return null
    return RemoteUpcomingItem(item.id, item.title, item.subtitle, requireNotNull(item.premiereDate), source,
        item.artworkUrl, item.mediaType, item.overview, item.facts + "I biblioteket", item.genres)
}

data class ReleaseCatalogue(
    val upcoming: List<RemoteUpcomingItem> = emptyList(),
    val recent: List<RemoteUpcomingItem> = emptyList(),
    val incomplete: Boolean = false,
)

data class LibraryReleaseCandidate(val item: RemoteLibraryItem, val source: ServiceKind)

/** Verify digital dates only for movies already present in the viewer's allowed libraries. */
class SeerrReleaseClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private data class Cache(val scope: String, val at: Long, val value: ReleaseCatalogue)
    private var cache: Cache? = null

    suspend fun feed(connection: ServiceConnection, library: List<LibraryReleaseCandidate>): ReleaseCatalogue = supervisorScope {
        require(connection.kind == ServiceKind.SEERR)
        val candidates = library.filter { it.item.available && it.item.mediaType == "Movie" && it.item.tmdbId != null }
            .distinctBy { it.item.tmdbId }.sortedByDescending { it.item.premiereDate }.take(60)
        if (candidates.isEmpty()) return@supervisorScope ReleaseCatalogue()
        val scope = MessageDigest.getInstance("SHA-256").digest(
            ("${connection.baseUrl}|${connection.userId}|${connection.sessionCookie}|${connection.token}|${LocalDate.now(clock)}|" +
                candidates.joinToString { "${it.source}:${it.item.id}:${it.item.tmdbId}" }).toByteArray()
        ).joinToString("") { "%02x".format(it) }
        synchronized(this@SeerrReleaseClient) { cache }?.takeIf {
            it.scope == scope && clock.millis() - it.at in 0 until 600_000
        }?.let { return@supervisorScope it.value }
        val window = ReleaseWindow(clock)
        val headers = if (connection.sessionCookie) seerrCookieHeaders(connection.token) else mapOf("X-Api-Key" to connection.token)
        fun get(path: String): String {
            val response = transport.get(EndpointValidator.resolve(connection.baseUrl, "api/v1/$path"), headers)
            check(response.statusCode in 200..299) { "Fekk ikkje henta utgjevingsdatoar frå Seerr (${response.statusCode})" }
            return response.body
        }
        val limit = Semaphore(4)
        val results = candidates.map { candidate -> async(Dispatchers.IO) {
            limit.withPermit { safeResult {
                val item = candidate.item
                seerrReleases(get("movie/${item.tmdbId}"), "movie", window).filter { window.recent(it.dateTime) }.map { release ->
                    release.copy(id = item.id, source = candidate.source, title = item.title, subtitle = item.subtitle,
                        artworkUrl = item.artworkUrl ?: release.artworkUrl,
                        overview = item.overview ?: release.overview, facts = release.facts + "I biblioteket")
                }
            } }
        } }.awaitAll()
        val all = results.flatMap { it.getOrDefault(emptyList()) }.distinctBy { it.id }
        val result = ReleaseCatalogue(
            upcoming = all.filter { window.upcoming(it.dateTime) }.sortedBy { it.dateTime },
            recent = all.filter { window.recent(it.dateTime) }.sortedByDescending { it.dateTime },
            incomplete = results.any { it.isFailure },
        )
        // Do not cache a failed/partial load as a valid empty catalogue. One account at a time.
        if (!result.incomplete) synchronized(this@SeerrReleaseClient) { cache = Cache(scope, clock.millis(), result) }
        result
    }
}

private inline fun <T> safeResult(block: () -> T): Result<T> = try { Result.success(block()) }
catch (e: CancellationException) { throw e }
catch (e: Exception) { Result.failure(e) }

/** Movie releaseDate is usually theatrical; only type 4 is a verified digital release. */
internal fun seerrReleases(payload: String, type: String, window: ReleaseWindow): List<RemoteUpcomingItem> {
    val root = Json.parseToJsonElement(payload).jsonObject
    fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull
    fun JsonObject.number(key: String) = (get(key) as? JsonPrimitive)?.intOrNull
    val id = root.number("id") ?: return emptyList()
    val title = root.text("title") ?: root.text("name") ?: return emptyList()
    val details = ServicePayloadParser.mediaDetails(payload)
    fun art(path: String?) = path?.takeIf { it.startsWith('/') && !it.startsWith("//") }
        ?.let { "https://image.tmdb.org/t/p/w780$it" }
    val genres = details.genres
    if (type == "movie") {
        // A restoration/reissue of an old movie is not a new film.
        val premiere = window.date(root.text("releaseDate")) ?: return emptyList()
        if (premiere < window.today.minusYears(1) || premiere > window.end) return emptyList()
        val regions = (root["releases"] as? JsonObject)?.get("results") as? JsonArray ?: return emptyList()
        val dates = regions.flatMap { region ->
            ((region as? JsonObject)?.get("release_dates") as? JsonArray).orEmpty()
        }.mapNotNull { it as? JsonObject }.filter { it.number("type") == 4 }
            .mapNotNull { window.date(it.text("release_date")) }.filter { it >= premiere }
        // Earliest home release, not a later regional re-release. No cinema-date fallback.
        val release = dates.minOrNull() ?: return emptyList()
        if (!window.recent(release.toString()) && !window.upcoming(release.toString())) return emptyList()
        return listOf(RemoteUpcomingItem("movie-$id", title, "Film · ${premiere.year}", release.toString(),
            ServiceKind.SEERR, art(root.text("posterPath")), "Movie", details.overview,
            details.facts + "Digital utgjeving", genres))
    }
    if (type != "tv") return emptyList()
    val episodes = listOfNotNull(root["lastEpisodeToAir"] as? JsonObject, root["nextEpisodeToAir"] as? JsonObject)
    return episodes.mapNotNull { episode ->
        val date = episode.text("airDate") ?: return@mapNotNull null
        if (!window.recent(date) && !window.upcoming(date)) return@mapNotNull null
        val season = episode.number("seasonNumber") ?: return@mapNotNull null
        val number = episode.number("episodeNumber") ?: return@mapNotNull null
        val label = "S${season.toString().padStart(2, '0')} E${number.toString().padStart(2, '0')}"
        RemoteUpcomingItem("tv-$id-$season-$number", title,
            listOfNotNull(label, episode.text("name")?.takeIf { it.isNotBlank() && it != "TBA" }).joinToString(" · "),
            date, ServiceKind.SEERR, art(episode.text("stillPath")) ?: art(root.text("backdropPath")) ?: details.artworkUrl,
            "Episode", episode.text("overview")?.takeIf(String::isNotBlank) ?: details.overview,
            details.facts + label, genres)
    }
}
