package app.reelstack.data.model

/** Stable identifiers; order is independent of visibility and temporary empty server responses. */
enum class HomeRow(val section: HomeSection?) {
    CONTINUE_WATCHING(HomeSection.CONTINUE_WATCHING),
    // Resuming your own title is the normal first action. Remote sessions are status and belong
    // just below it, not before the library the person opened the app to watch.
    NOW_PLAYING(HomeSection.NOW_PLAYING),
    NEXT_UP(null),
    FAVOURITES(HomeSection.FAVOURITES),
    JELLYFIN_MOVIES(HomeSection.JELLYFIN_MOVIES),
    EMBY_MOVIES(HomeSection.EMBY_MOVIES),
    JELLYFIN_SERIES(HomeSection.JELLYFIN_SERIES),
    EMBY_SERIES(HomeSection.EMBY_SERIES),
    RECOMMENDATIONS(HomeSection.RECOMMENDATIONS),
    RECENT_RELEASES(HomeSection.RECENT_RELEASES),
    UPCOMING(HomeSection.UPCOMING),
}

fun decodeHomeRowOrder(saved: String?): List<HomeRow> =
    (saved.orEmpty().split(',').mapNotNull { name -> HomeRow.entries.firstOrNull { it.name == name } } + HomeRow.entries).distinct()

/** Move across the rows shown in the editor, preserving absent services' saved positions. */
fun moveHomeRow(order: List<HomeRow>, row: HomeRow, direction: Int, available: List<HomeRow>): List<HomeRow> {
    val normalized = decodeHomeRowOrder(order.joinToString(",") { it.name })
    if (direction != -1 && direction != 1) return normalized
    val shown = normalized.filter { it in available }
    val position = shown.indexOf(row)
    if (position < 0) return normalized
    val neighbour = shown.getOrNull(position + direction) ?: return normalized
    return normalized.toMutableList().apply {
        val from = indexOf(row)
        val to = indexOf(neighbour)
        this[from] = neighbour
        this[to] = row
    }
}
