package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.localization.LocalizedText
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*
import java.time.Clock
import java.time.LocalDate
import java.security.MessageDigest

data class PersonalCalendarFeed(
    val upcoming: List<RemoteUpcomingItem> = emptyList(),
    val undated: List<CalendarTitle> = emptyList(),
    val incomplete: Boolean = false,
)

class CalendarAccessException : java.io.IOException("Calendar session is no longer authorised")

/** Reads Seerr metadata through the verified personal session; no acquisition-service calls. */
class PersonalCalendarClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private data class Cached(val at: Long, val payload: String)
    private val cache = linkedMapOf<String, Cached>()
    private var currentScope = ""

    suspend fun feed(connection: ServiceConnection, userId: String, candidates: List<CalendarTitle>,
        scopeSalt: String = "", force: Boolean = false): PersonalCalendarFeed = supervisorScope {
        require(connection.kind == ServiceKind.SEERR && userId.isNotBlank())
        val scope = MessageDigest.getInstance("SHA-256").digest(
            "${connection.identity}|$userId|${connection.token}|$scopeSalt".toByteArray()
        ).joinToString("") { "%02x".format(it) }
        synchronized(cache) { if (currentScope != scope) { cache.clear(); currentScope = scope } }
        val headers = if (connection.sessionCookie) seerrCookieHeaders(connection.token) else mapOf("X-Api-Key" to connection.token)
        val today = LocalDate.now(clock)
        val incomplete = java.util.concurrent.atomic.AtomicBoolean(false)
        fun get(path: String): JsonObject {
            val key = "$scope:$path"
            val cached = synchronized(cache) { cache[key] }?.takeIf { !force && clock.millis() - it.at in 0 until 6 * 60 * 60_000 }
            val body = cached?.payload ?: transport.get(EndpointValidator.resolve(connection.baseUrl, "api/v1/$path"), headers).let { response ->
                if (response.statusCode in setOf(401, 403)) {
                    synchronized(cache) { cache.clear(); currentScope = "" }; throw CalendarAccessException()
                }
                check(response.statusCode in 200..299) { "Calendar metadata unavailable" }
                Json.parseToJsonElement(response.body).jsonObject
                synchronized(cache) {
                    if (currentScope == scope) {
                        cache[key] = Cached(clock.millis(), response.body)
                        while (cache.size > 2000) cache.remove(cache.keys.first())
                    }
                }
                response.body
            }
            return Json.parseToJsonElement(body).jsonObject
        }
        val candidatesByKey = candidates.filter { it.tmdbId > 0 && it.mediaType in setOf("movie", "tv") }.groupBy { it.key }
        val selected = candidatesByKey.values.mapNotNull { entries -> entries.firstOrNull()?.takeUnless { entries.any(CalendarTitle::hidden) } }
        val permits = Semaphore(if (transport.supportsConcurrentCalls) 4 else 1)
        val results = selected.map { title -> async(Dispatchers.IO) {
            permits.withPermit {
                try {
                    val root = get("${title.mediaType}/${title.tmdbId}")
                    check(root.number("id") == title.tmdbId) { "Calendar metadata identity does not match" }
                    // Blocked metadata cannot leak back through an earlier personal follow.
                    if (((root["mediaInfo"] as? JsonObject)?.get("status") as? JsonPrimitive)?.intOrNull == 6) return@withPermit emptyList<RemoteUpcomingItem>() to null
                    val display = title.copy(title = root.text("name") ?: root.text("title") ?: title.title,
                        artworkUrl = art(root.text("posterPath")) ?: title.artworkUrl)
                    val items = if (title.mediaType == "movie") {
                        seerrReleases(root.toString(), "movie", ReleaseWindow(clock)).filter { it.dateTime >= today.toString() }
                            .map { it.copy(tmdbId = title.tmdbId, region = digitalRegion(root, it.dateTime)) }
                    } else {
                        val next = root["nextEpisodeToAir"] as? JsonObject
                        val knownSeasons = (root["seasons"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
                        val latest = knownSeasons.mapNotNull { it.number("seasonNumber") }.filter { it > 0 }.maxOrNull()
                        val latestAired = knownSeasons.filter { it.date("airDate")?.let { date -> date <= today } == true }
                            .mapNotNull { it.number("seasonNumber") }.filter { it > 0 }.maxOrNull()
                        val seasons = knownSeasons
                            .filter { (it.number("seasonNumber") ?: 0) > 0 &&
                                (it.date("airDate")?.let { date -> date in today..today.plusDays(28) } == true ||
                                    it.number("seasonNumber") == next?.number("seasonNumber") ||
                                    it.number("seasonNumber") == latest || it.number("seasonNumber") == latestAired) }
                            .mapNotNull { it.number("seasonNumber") }.distinct()
                        val episodes = mutableListOf<JsonObject>()
                        next?.let(episodes::add)
                        for (season in seasons) {
                            try { episodes += (get("tv/${title.tmdbId}/season/$season")["episodes"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject } }
                            catch (e: CancellationException) { throw e }
                            catch (e: CalendarAccessException) { throw e }
                            catch (e: Exception) { incomplete.set(true) }
                        }
                        episodes.mapNotNull { episode -> calendarEpisode(root, episode, title.tmdbId, today) }
                    }
                    val pending = display.takeIf { items.isEmpty() && !title.hidden }
                    items to pending
                } catch (e: CancellationException) { throw e }
                catch (e: CalendarAccessException) { throw e }
                catch (e: Exception) { incomplete.set(true); emptyList<RemoteUpcomingItem>() to title }
            }
        } }.awaitAll()
        PersonalCalendarFeed(results.flatMap { it.first }.distinctBy { it.id }.sortedBy { it.dateTime },
            results.mapNotNull { it.second }, incomplete.get())
    }

}

private fun JsonObject.text(name: String) = (get(name) as? JsonPrimitive)?.contentOrNull
private fun JsonObject.number(name: String) = (get(name) as? JsonPrimitive)?.intOrNull
private fun JsonObject.date(name: String) = text(name)?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
private fun art(path: String?) = path?.takeIf { it.startsWith('/') && !it.startsWith("//") }?.let { "https://image.tmdb.org/t/p/w780$it" }

internal fun calendarEpisode(root: JsonObject, episode: JsonObject, tmdbId: Int, today: LocalDate): RemoteUpcomingItem? {
    val date = episode.date("airDate")?.takeIf { it in today..today.plusDays(28) } ?: return null
    val season = episode.number("seasonNumber")?.takeIf { it > 0 } ?: return null
    val number = episode.number("episodeNumber")?.takeIf { it > 0 } ?: return null
    val title = root.text("name") ?: return null
    val label = "S${season.toString().padStart(2, '0')} E${number.toString().padStart(2, '0')}"
    return RemoteUpcomingItem("tv-$tmdbId-$season-$number", title,
        listOfNotNull(label, episode.text("name")?.takeIf { it.isNotBlank() && it != "TBA" }).joinToString(" · "),
        date.toString(), ServiceKind.SEERR, art(episode.text("stillPath")) ?: art(root.text("backdropPath")) ?: art(root.text("posterPath")),
        "Episode", episode.text("overview")?.takeIf { it.isNotBlank() } ?: root.text("overview"),
        listOf(LocalizedText.raw(label)), tmdbId = tmdbId, season = season, episode = number)
}

private fun digitalRegion(root: JsonObject, date: String): String? =
    ((root["releases"] as? JsonObject)?.get("results") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        .firstOrNull { region -> (region["release_dates"] as? JsonArray).orEmpty().any { entry ->
            (entry as? JsonObject)?.let { it.number("type") == 4 && it.text("release_date")?.take(10) == date } == true
        } }?.text("iso_3166_1")
