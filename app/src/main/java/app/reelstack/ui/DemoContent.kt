package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.*

/** Offline showroom only. No account identifiers, credentials or remote media IDs. */
private data class DemoTitle(val key: String, val title: String, val art: Int, val genre: String,
    @androidx.annotation.StringRes val description: Int, val source: ServiceKind, val series: Boolean = false, val score: String = "7,8") {
    fun media(context: Context? = null): LibraryMedia {
        val genreLabel = standardGenreResource(genre)?.let { context?.getString(it) } ?: genre
        val seasons = demoSeasons(context)
        val rating = if (context?.resources?.configuration?.locales?.get(0)?.language == "en") score.replace(',', '.') else score
        return LibraryMedia(key, title, "${demoKind(context, series)} · ${if (series) seasons else "2025"}",
            artworkRes = art, source = source, overview = demoString(context, description, "Demo story"), genres = listOf(genreLabel),
            facts = listOf("★ $rating", "2025", if (series) seasons else demoString(context, R.string.media_minutes, "108 min", 108), genreLabel),
            mediaType = if (series) "Series" else "Movie", runtimeMinutes = if (series) 48 else 108,
            childCount = if (series) 2 else null)
    }
}

private fun demoString(context: Context?, @androidx.annotation.StringRes resource: Int, fallback: String, vararg args: Any): String =
    context?.getString(resource, *args) ?: fallback
private fun demoSeasons(context: Context?): String = context?.resources?.getQuantityString(R.plurals.media_seasons, 2, 2) ?: "2 seasons"
private fun demoKind(context: Context?, series: Boolean = false): String = demoString(context,
    if (series) R.string.media_kind_series else R.string.media_kind_movie, if (series) "Series" else "Film")

private val demoTitles = listOf(
    DemoTitle("recent-odyssey", "The Odyssey", R.drawable.desert_arrival, "Adventure",
        R.string.demo_story_odyssey, ServiceKind.JELLYFIN),
    DemoTitle("demo-nordlys", "Nordlys", R.drawable.media_placeholder, "Drama",
        R.string.demo_story_nordlys, ServiceKind.JELLYFIN, score = "8,2"),
    DemoTitle("demo-desember", "Heim til desember", R.drawable.media_placeholder, "Romance",
        R.string.demo_story_desember, ServiceKind.JELLYFIN, score = "7,6"),
    DemoTitle("demo-huset", "Huset i skogen", R.drawable.media_placeholder, "Mystery",
        R.string.demo_story_huset, ServiceKind.JELLYFIN, score = "8,0"),
    DemoTitle("demo-bestilling", "Siste bestilling", R.drawable.kitchen_request, "Comedy",
        R.string.demo_story_bestilling, ServiceKind.JELLYFIN, score = "7,4"),
    DemoTitle("demo-midnatt", "Etter midnatt", R.drawable.media_placeholder, "Thriller",
        R.string.demo_story_midnatt, ServiceKind.EMBY, score = "7,9"),
    DemoTitle("demo-blaatime", "Den blå timen", R.drawable.media_placeholder, "Drama",
        R.string.demo_story_blaatime, ServiceKind.EMBY, score = "8,1"),
    DemoTitle("demo-vinterveg", "Vintervegen", R.drawable.media_placeholder, "Adventure",
        R.string.demo_story_vinterveg, ServiceKind.EMBY, score = "7,5"),
    DemoTitle("demo-signalet", "Signal frå sanden", R.drawable.desert_arrival, "Science fiction",
        R.string.demo_story_signalet, ServiceKind.EMBY, score = "8,3"),
    DemoTitle("demo-bordet", "Ein plass ved bordet", R.drawable.kitchen_request, "Drama",
        R.string.demo_story_bordet, ServiceKind.EMBY, score = "7,7"),
    DemoTitle("recent-severance", "Severance", R.drawable.session_still, "Mystery",
        R.string.demo_story_severance, ServiceKind.JELLYFIN, true, "8,7"),
    DemoTitle("demo-kystvakta", "Kystvakta", R.drawable.media_placeholder, "Crime",
        R.string.demo_story_kystvakta, ServiceKind.JELLYFIN, true, "8,2"),
    DemoTitle("demo-desemberdagar", "Desemberdagar", R.drawable.media_placeholder, "Drama",
        R.string.demo_story_desemberdagar, ServiceKind.JELLYFIN, true, "7,9"),
    DemoTitle("demo-etasjen", "Etasjen under", R.drawable.session_still, "Thriller",
        R.string.demo_story_etasjen, ServiceKind.JELLYFIN, true, "8,0"),
    DemoTitle("demo-service", "Service", R.drawable.kitchen_request, "Comedy",
        R.string.demo_story_service, ServiceKind.EMBY, true, "8,4"),
    DemoTitle("demo-spor", "Mørke spor", R.drawable.media_placeholder, "Mystery",
        R.string.demo_story_spor, ServiceKind.EMBY, true, "8,1"),
    DemoTitle("demo-leia", "Langs leia", R.drawable.media_placeholder, "Documentary",
        R.string.demo_story_leia, ServiceKind.EMBY, true, "8,5"),
    DemoTitle("demo-vinterlys", "Vinterlys", R.drawable.media_placeholder, "Drama",
        R.string.demo_story_vinterlys, ServiceKind.EMBY, true, "7,8"),
)

internal fun demoRecentMovies(context: Context? = null) = demoTitles.filterNot { it.series }.map { it.media(context) }
internal fun demoRecentSeries(context: Context? = null) = demoTitles.filter { it.series }.sortedBy { if (it.key == "demo-kystvakta") 0 else 1 }.map { it.media(context) }

internal fun demoResume(context: Context? = null): List<LibraryMedia> {
    val series = demoRecentSeries(context).sortedBy { if (it.id == "recent-severance") 0 else 1 }
    val episodes = series.take(4).mapIndexed { i, media -> media.copy(
        id = if (i == 0) "resume-severance" else "resume-${media.id}",
        subtitle = "S02 E0${i + 3} · ${demoString(context, R.string.detail_minutes_left, "${listOf(20, 34, 12, 41)[i]} min left", listOf(20, 34, 12, 41)[i])}",
        progress = listOf(.58f, .29f, .75f, .15f)[i],
        mediaType = "Episode", season = 2, episode = i + 3, childCount = null, seriesId = media.id,
    ) }
    val movies = demoRecentMovies(context)
    val movie = movies.first().copy(id = "resume-odyssey", subtitle = "${demoKind(context)} · ${demoString(context, R.string.detail_minutes_left, "85 min left", 85)}", progress = .21f)
    return listOf(episodes.first(), movie) + episodes.drop(1) + movies.last().copy(id = "resume-bordet", progress = .44f)
}

internal fun demoNextUp(context: Context? = null) = demoRecentSeries(context).takeLast(4).mapIndexed { index, media -> media.copy(
    id = "next-${media.id}", subtitle = "S01 E0${index + 2} · ${demoString(context, R.string.player_next_episode, "Next episode")}", mediaType = "Episode",
    season = 1, episode = index + 2, seriesId = media.id, childCount = null,
) }

internal fun demoFavourites(context: Context? = null) = (demoRecentMovies(context).take(3) + demoRecentSeries(context).takeLast(3)).map { it.copy(favourite = true) }

internal fun demoSessions(context: Context? = null) = listOf(
    PlaybackSession("Maya", demoString(context, R.string.demo_living_room, "Living room TV"), "Severance", "S02 E03", .58f, 20, false, "4K", false,
        sessionId = "demo-living-room", source = ServiceKind.JELLYFIN, season = 2, episode = 3),
    PlaybackSession("Jonas", demoString(context, R.string.demo_tablet, "Tablet"), "Service", "S01 E02", .31f, 33, false, "1080p", true,
        sessionId = "demo-tablet", source = ServiceKind.EMBY, season = 1, episode = 2),
)

internal fun demoSessionArtwork(session: PlaybackSession): Int = when {
    session.sessionId == "demo-tablet" -> R.drawable.kitchen_request
    session.sessionId?.startsWith("demo-") == true -> R.drawable.session_still
    else -> R.drawable.media_placeholder
}

internal fun demoDiscover(context: Context? = null): List<DiscoverMedia> {
    val rating = if (context?.resources?.configuration?.locales?.get(0)?.language == "en") "8.1" else "8,1"
    val lead = DiscoverMedia("last-horizon", "The Last Horizon", "${demoKind(context)} · 2026", R.drawable.desert_arrival, false,
        mediaType = "movie", overview = demoString(context, R.string.demo_story_horizon, "A crew follows a signal to the edge of known space."),
        facts = listOf("★ $rating", "2026", demoString(context, R.string.media_minutes, "118 min", 118)),
        genres = listOf(context?.getString(R.string.genre_science_fiction) ?: "Science fiction"))
    val serviceMedia = demoTitles.first { it.key == "demo-service" }.media(context)
    val service = DiscoverMedia("service", "Service", serviceMedia.subtitle, R.drawable.kitchen_request, true,
        mediaType = "tv", overview = serviceMedia.overview, facts = serviceMedia.facts, genres = serviceMedia.genres)
    return listOf(lead, service) + demoTitles.filter { it.key != "demo-service" }.take(12).mapIndexed { i, title ->
        val media = title.media(context)
        DiscoverMedia("discover-${title.key}", title.title, media.subtitle, title.art, i % 4 == 0,
            requested = i % 4 == 1, mediaType = if (title.series) "tv" else "movie", overview = media.overview,
            facts = media.facts, genres = media.genres, seerrStatus = when (i % 4) { 0 -> 5; 1 -> 2; else -> null })
    }
}

internal fun demoRecommendations(context: Context? = null) = demoDiscover(context).filterNot { it.inLibrary || it.requested }.take(8)

private fun demoRelease(recent: Boolean, context: Context?): List<UpcomingMedia> {
    val today = java.time.LocalDate.now()
    val locale = context?.resources?.configuration?.locales?.get(0) ?: java.util.Locale.forLanguageTag("nn")
    return (demoTitles.filterNot { it.series }.take(4) + demoTitles.filter { it.series }.take(4)).mapIndexed { index, title ->
        val offset = if (recent) -index.toLong() else index.toLong()
        val media = title.media(context)
        UpcomingMedia("${if (recent) "released" else "upcoming"}-${title.key}", title.title, media.subtitle,
            when (offset) { 0L -> demoString(context, R.string.cal_today, "Today")
                1L -> demoString(context, R.string.cal_tomorrow, "Tomorrow")
                -1L -> demoString(context, R.string.time_yesterday, "Yesterday")
                else -> today.plusDays(offset).format(java.time.format.DateTimeFormatter.ofPattern("d. MMM", locale)) },
            today.plusDays(offset).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            title.art, ServiceKind.SEERR, overview = media.overview, facts = media.facts,
            genres = media.genres, mediaType = if (title.series) "Episode" else "Movie")
    }
}
internal fun demoUpcoming(context: Context? = null) = demoRelease(false, context)
internal fun demoRecentReleases(context: Context? = null) = demoRelease(true, context)

internal fun demoIncoming(context: Context? = null) = demoTitles.take(5).mapIndexed { index, title ->
    val state = listOf(IncomingState.DOWNLOADING, IncomingState.REQUESTED, IncomingState.READY)[index % 3]
    IncomingMedia("incoming-${title.key}", title.title, ServiceKind.SEERR,
        when (state) { IncomingState.DOWNLOADING -> demoString(context, R.string.queue_downloading_percent, "Downloading · 68 %", 68)
            IncomingState.REQUESTED -> demoString(context, R.string.stage_requested, "Requested")
            IncomingState.READY -> demoString(context, R.string.stage_available, "In library") },
        state, title.art, overview = title.media(context).overview, progress = if (state == IncomingState.DOWNLOADING) 68 else null)
}

internal fun demoActivity() = demoTitles.take(8).mapIndexed { index, title ->
    ActivityEvent("activity-${title.key}", title.title,
        app.reelstack.localization.LocalizedText(
            when (index % 3) { 0 -> R.string.stage_requested; 1 -> R.string.stage_downloading; else -> R.string.stage_available }),
        if (index < 4) app.reelstack.localization.LocalizedText.plural(R.plurals.time_minutes_ago, 2 + index * 8)
        else app.reelstack.localization.LocalizedText(R.string.time_yesterday),
        complete = index % 3 == 2,
        progress = if (index % 3 == 1) 42 else null, source = if (index % 3 == 0) ServiceKind.SEERR else ServiceKind.SEERR,
        artworkRes = title.art, mediaType = "Movie")
}
