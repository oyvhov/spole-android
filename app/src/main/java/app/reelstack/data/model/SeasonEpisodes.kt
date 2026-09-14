package app.reelstack.data.model

/** Season lists keep narrative order, independent of viewing history and aired specials. */
fun orderedSeasonEpisodes(episodes: List<LibraryMedia>, seasonNumber: Int?): List<LibraryMedia> =
    episodes.filter { seasonNumber == null || it.season == null || it.season == seasonNumber }
        .distinctBy { it.id }
        .sortedWith(compareBy({ it.episode ?: Int.MAX_VALUE }, { it.id }))
