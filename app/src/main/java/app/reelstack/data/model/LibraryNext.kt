package app.reelstack.data.model

/** Older devices stored two sections. Keep their first position and combine visibility on read. */
fun libraryHubOrder(order: List<String>): List<String> = (order + DEFAULT_LIBRARY_HUB)
    .map { if (it == "CONTINUE" || it == "NEXT") "LIBRARY_NEXT" else it }
    .distinct().filter { it in DEFAULT_LIBRARY_HUB }

fun libraryHubHidden(hidden: Set<String>): Set<String> =
    (hidden - setOf("CONTINUE", "NEXT")) +
        if ("CONTINUE" in hidden && "NEXT" in hidden) setOf("LIBRARY_NEXT") else emptySet()

/** One continuation per series, scoped by server kind. A partly watched episode wins next-up. */
fun libraryNextItems(resume: List<LibraryMedia>, nextUp: List<LibraryMedia>): List<LibraryMedia> {
    fun LibraryMedia.identity() = source to (seriesId?.takeIf(String::isNotBlank) ?: id)
    val continued = resume.filterNot { it.played }.sortedByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE }
        .distinctBy { it.identity() }
    val resumed = continued.map { it.identity() }.toSet()
    return (continued + nextUp.filter { !it.played && it.identity() !in resumed }.distinctBy { it.identity() })
        .sortedByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE }
}
