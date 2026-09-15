package app.reelstack.data.network

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.*
import java.time.LocalDate

/** Only announced future episodes; never invent a release date or a playable server id. */
internal fun parseUpcomingSeason(payload: String, tmdbId: Int, season: Int,
    today: LocalDate = LocalDate.now()): List<LibraryMedia> {
    val root = Json.parseToJsonElement(payload).jsonObject
    return (root["episodes"] as? JsonArray).orEmpty().mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        fun field(key: String) = (item[key] as? JsonPrimitive)?.contentOrNull
        val number = field("episodeNumber")?.toIntOrNull() ?: return@mapNotNull null
        val date = field("airDate")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            ?: return@mapNotNull null
        if (date.isBefore(today) || number < 1) return@mapNotNull null
        if (field("seasonNumber")?.toIntOrNull()?.let { it != season } == true) return@mapNotNull null
        val name = field("name").orEmpty()
        val still = field("stillPath")?.takeIf { it.matches(Regex("/[A-Za-z0-9_.-]+")) }
        LibraryMedia(id = "upcoming-$tmdbId-$season-$number", title = name, subtitle = name,
            artworkRes = app.reelstack.R.drawable.media_placeholder, source = ServiceKind.SEERR,
            artworkUrl = still?.let { "https://image.tmdb.org/t/p/w780$it" },
            overview = field("overview"), mediaType = "Episode", season = season, episode = number,
            available = false, premiereDate = date.toString())
    }.distinctBy { it.episode }.sortedBy { it.episode }
}

/** Match on season/episode, never translated titles; a real playable item always wins. */
internal fun mergeUpcomingEpisodes(local: List<LibraryMedia>, announced: List<LibraryMedia>): List<LibraryMedia> =
    (local + announced).distinctBy { if (it.season != null && it.episode != null) "${it.season}:${it.episode}" else it.id }
        .sortedWith(compareBy({ it.season ?: Int.MAX_VALUE }, { it.episode ?: Int.MAX_VALUE }))
