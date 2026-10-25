package app.reelstack.data.model

/** A person as Seerr knows them, and what they made split by whether you have it. */
data class PersonPage(
    val person: DiscoverPerson,
    val department: String? = null,
    val birthday: String? = null,
    val biography: String? = null,
    val inLibrary: List<DiscoverMedia> = emptyList(),
    val more: List<DiscoverMedia> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

/** What you have (available or partly) first; the rest newest first. */
fun splitCredits(credits: List<DiscoverMedia>): Pair<List<DiscoverMedia>, List<DiscoverMedia>> {
    val owned = credits.filter { it.inLibrary || it.seerrStatus == 4 || it.seerrStatus == 5 }
    val more = (credits - owned.toSet()).sortedByDescending { it.metadata }
    return owned to more
}
