package app.reelstack.ui

import app.reelstack.R
import app.reelstack.data.model.*

/** Offline showroom only. No account identifiers, credentials or remote media IDs. */
private data class DemoTitle(val key: String, val title: String, val art: Int, val genre: String,
    val description: String, val source: ServiceKind, val series: Boolean = false, val score: String = "7,8") {
    fun media() = LibraryMedia(key, title, if (series) "Serie · 2 sesongar" else "Film · 2025",
        artworkRes = art, source = source, overview = description, genres = listOf(genre),
        facts = listOf("★ $score", "2025", if (series) "2 sesongar" else "1 t 48 min", genre),
        mediaType = if (series) "Series" else "Movie", runtimeMinutes = if (series) 48 else 108,
        childCount = if (series) 2 else null)
}

private val demoTitles = listOf(
    DemoTitle("recent-odyssey", "The Odyssey", R.drawable.desert_arrival, "Eventyr",
        "Ein soldat følgjer stjernene heim gjennom eit ukjent landskap. Ved enden av reisa ventar eit vanskeleg val.", ServiceKind.JELLYFIN),
    DemoTitle("demo-nordlys", "Nordlys", R.drawable.media_placeholder, "Drama",
        "Ein fyrvaktar vender heim til kysten. Eit brev frå fortida endrar alt han trudde han visste om familien.", ServiceKind.JELLYFIN, score = "8,2"),
    DemoTitle("demo-desember", "Heim til desember", R.drawable.media_placeholder, "Romantikk",
        "Eit uventa snøfall samlar gamle vener på ei hytte. Mellom juleførebuingar og stille kveldar får dei ein ny start.", ServiceKind.JELLYFIN, score = "7,6"),
    DemoTitle("demo-huset", "Huset i skogen", R.drawable.media_placeholder, "Mystikk",
        "Ei arva dagbok fører to sysken tilbake til eit forlatt hus. Kvar natt lyser eit nytt vindauge.", ServiceKind.JELLYFIN, score = "8,0"),
    DemoTitle("demo-bestilling", "Siste bestilling", R.drawable.kitchen_request, "Komedie",
        "Ein liten restaurant får ein siste sjanse når ein uventa gjest bestiller middag til heile bygda.", ServiceKind.JELLYFIN, score = "7,4"),
    DemoTitle("demo-midnatt", "Etter midnatt", R.drawable.media_placeholder, "Thriller",
        "Den siste bussen køyrer forbi same huset tre gonger. Ein passasjer bestemmer seg for å gå av.", ServiceKind.EMBY, score = "7,9"),
    DemoTitle("demo-blaatime", "Den blå timen", R.drawable.media_placeholder, "Drama",
        "Ein fotograf leitar etter det perfekte lyset langs kysten og finn eit fellesskap han ikkje visste han sakna.", ServiceKind.EMBY, score = "8,1"),
    DemoTitle("demo-vinterveg", "Vintervegen", R.drawable.media_placeholder, "Eventyr",
        "Far og dotter legg ut på ei lang vinterreise. Ei stengd fjellovergang sender dei på ein uventa omveg.", ServiceKind.EMBY, score = "7,5"),
    DemoTitle("demo-signalet", "Signal frå sanden", R.drawable.desert_arrival, "Science fiction",
        "Eit forskarlag oppdagar eit signal under sanden på ein fjern planet. Svaret kjem frå deira eiga framtid.", ServiceKind.EMBY, score = "8,3"),
    DemoTitle("demo-bordet", "Ein plass ved bordet", R.drawable.kitchen_request, "Drama",
        "Tre generasjonar møtest til søndagsmiddag. Oppskriftene er dei same, men ingen av dei kjem heim som før.", ServiceKind.EMBY, score = "7,7"),
    DemoTitle("recent-severance", "Severance", R.drawable.session_still, "Mystikk",
        "Tilsette har delt minna sine mellom arbeid og livet utanfor. Så byrjar dei to sidene å lekke over i kvarandre.", ServiceKind.JELLYFIN, true, "8,7"),
    DemoTitle("demo-kystvakta", "Kystvakta", R.drawable.media_placeholder, "Krim",
        "Eit lite etterforskingsteam følgjer spora etter eit forsvunne skip. Langs kysten kjenner alle kvarandre, men ingen veit alt.", ServiceKind.JELLYFIN, true, "8,2"),
    DemoTitle("demo-desemberdagar", "Desemberdagar", R.drawable.media_placeholder, "Drama",
        "Ein familie tek over ei fjellstove rett før jul. Kvar gjest har med seg ei historie dei ikkje hadde venta.", ServiceKind.JELLYFIN, true, "7,9"),
    DemoTitle("demo-etasjen", "Etasjen under", R.drawable.session_still, "Thriller",
        "Ein nytilsett finn ei dør som ikkje står på planteikninga. Kollegane hennar hevdar at etasjen ikkje finst.", ServiceKind.JELLYFIN, true, "8,0"),
    DemoTitle("demo-service", "Service", R.drawable.kitchen_request, "Komedie",
        "Ein kokk tek over familierestauranten. Kaoset på kjøkenet er lettare å styre enn folka rundt han.", ServiceKind.EMBY, true, "8,4"),
    DemoTitle("demo-spor", "Mørke spor", R.drawable.media_placeholder, "Mystikk",
        "Eit gamalt forsvinningsnummer dukkar opp att i arkivet. Etterforskaren finn eit mønster som strekkjer seg over fleire generasjonar.", ServiceKind.EMBY, true, "8,1"),
    DemoTitle("demo-leia", "Langs leia", R.drawable.media_placeholder, "Dokumentar",
        "Ei reise langs kysten, frå travle hamner til små samfunn der havet framleis set rytmen i kvardagen.", ServiceKind.EMBY, true, "8,5"),
    DemoTitle("demo-vinterlys", "Vinterlys", R.drawable.media_placeholder, "Drama",
        "Når mørketida kjem, opnar eit lite hotell dørene for både reisande og naboar som treng ein stad å vere.", ServiceKind.EMBY, true, "7,8"),
)

internal fun demoRecentMovies() = demoTitles.filterNot { it.series }.map { it.media() }
internal fun demoRecentSeries() = demoTitles.filter { it.series }.sortedBy { if (it.key == "demo-kystvakta") 0 else 1 }.map { it.media() }

internal fun demoResume(): List<LibraryMedia> {
    val series = demoRecentSeries().sortedBy { if (it.id == "recent-severance") 0 else 1 }
    val episodes = series.take(4).mapIndexed { i, media -> media.copy(
        id = if (i == 0) "resume-severance" else "resume-${media.id}",
        subtitle = "S02 E0${i + 3} · ${listOf(20, 34, 12, 41)[i]} min att", progress = listOf(.58f, .29f, .75f, .15f)[i],
        mediaType = "Episode", season = 2, episode = i + 3, childCount = null, seriesId = media.id,
    ) }
    val movie = demoRecentMovies().first().copy(id = "resume-odyssey", subtitle = "Film · 85 min att", progress = .21f)
    return listOf(episodes.first(), movie) + episodes.drop(1) + demoRecentMovies().last().copy(id = "resume-bordet", progress = .44f)
}

internal fun demoNextUp() = demoRecentSeries().takeLast(4).mapIndexed { index, media -> media.copy(
    id = "next-${media.id}", subtitle = "S01 E0${index + 2} · Neste episode", mediaType = "Episode",
    season = 1, episode = index + 2, seriesId = media.id, childCount = null,
) }

internal fun demoFavourites() = (demoRecentMovies().take(3) + demoRecentSeries().takeLast(3)).map { it.copy(favourite = true) }

internal fun demoSessions() = listOf(
    PlaybackSession("Maya", "TV i stova", "Severance", "S02 E03", .58f, 20, false, "4K", false,
        sessionId = "demo-living-room", source = ServiceKind.JELLYFIN, season = 2, episode = 3),
    PlaybackSession("Jonas", "Nettbrett", "Service", "S01 E02", .31f, 33, false, "1080p", true,
        sessionId = "demo-tablet", source = ServiceKind.EMBY, season = 1, episode = 2),
)

internal fun demoSessionArtwork(session: PlaybackSession): Int = when {
    session.sessionId == "demo-tablet" -> R.drawable.kitchen_request
    session.sessionId?.startsWith("demo-") == true -> R.drawable.session_still
    else -> R.drawable.media_placeholder
}

internal fun demoDiscover(): List<DiscoverMedia> {
    val lead = DiscoverMedia("last-horizon", "The Last Horizon", "Film · 2026", R.drawable.desert_arrival, false,
        mediaType = "movie", overview = "Eit mannskap følgjer eit signal til kanten av det kjende rommet.",
        facts = listOf("★ 8,1", "2026", "1 t 58 min"), genres = listOf("Science fiction"))
    val service = DiscoverMedia("service", "Service", "Serie · 2 sesongar", R.drawable.kitchen_request, true,
        mediaType = "tv", overview = demoTitles.first { it.key == "demo-service" }.description, facts = listOf("★ 8,4", "2 sesongar"), genres = listOf("Komedie"))
    return listOf(lead, service) + demoTitles.filter { it.key != "demo-service" }.take(12).mapIndexed { i, title ->
        val media = title.media()
        DiscoverMedia("discover-${title.key}", title.title, media.subtitle, title.art, i % 4 == 0,
            requested = i % 4 == 1, mediaType = if (title.series) "tv" else "movie", overview = title.description,
            facts = media.facts, genres = media.genres, seerrStatus = when (i % 4) { 0 -> 5; 1 -> 2; else -> null })
    }
}

internal fun demoRecommendations() = demoDiscover().filterNot { it.inLibrary || it.requested }.take(8)

private fun demoRelease(recent: Boolean): List<UpcomingMedia> {
    val today = java.time.LocalDate.now()
    return (demoTitles.filterNot { it.series }.take(4) + demoTitles.filter { it.series }.take(4)).mapIndexed { index, title ->
        val offset = if (recent) -index.toLong() else index.toLong()
        UpcomingMedia("${if (recent) "released" else "upcoming"}-${title.key}", title.title, title.media().subtitle,
            when (offset) { 0L -> "I dag · 20:00"; 1L -> "I morgon · 20:00"; -1L -> "I går · 20:00"
                else -> today.plusDays(offset).format(java.time.format.DateTimeFormatter.ofPattern("d. MMM", java.util.Locale.forLanguageTag("nn"))) },
            today.plusDays(offset).atTime(20, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            title.art, if (title.series) ServiceKind.SONARR else ServiceKind.RADARR, overview = title.description, facts = title.media().facts,
            genres = listOf(title.genre), mediaType = if (title.series) "Episode" else "Movie")
    }
}
internal fun demoUpcoming() = demoRelease(false)
internal fun demoRecentReleases() = demoRelease(true)

internal fun demoIncoming() = demoTitles.take(5).mapIndexed { index, title ->
    val state = listOf(IncomingState.DOWNLOADING, IncomingState.REQUESTED, IncomingState.READY)[index % 3]
    IncomingMedia("incoming-${title.key}", title.title, ServiceKind.RADARR,
        when (state) { IncomingState.DOWNLOADING -> "Lastar ned 68 %"; IncomingState.REQUESTED -> "Førespurd"; IncomingState.READY -> "I biblioteket" },
        state, title.art, overview = title.description, progress = if (state == IncomingState.DOWNLOADING) 68 else null)
}

internal fun demoActivity() = demoTitles.take(8).mapIndexed { index, title ->
    ActivityEvent("activity-${title.key}", title.title,
        app.reelstack.localization.LocalizedText(
            when (index % 3) { 0 -> R.string.stage_requested; 1 -> R.string.stage_downloading; else -> R.string.stage_available }),
        if (index < 4) app.reelstack.localization.LocalizedText.plural(R.plurals.time_minutes_ago, 2 + index * 8)
        else app.reelstack.localization.LocalizedText(R.string.time_yesterday),
        complete = index % 3 == 2,
        progress = if (index % 3 == 1) 42 else null, source = if (index % 3 == 0) ServiceKind.SEERR else ServiceKind.RADARR,
        artworkRes = title.art, mediaType = "Movie")
}
