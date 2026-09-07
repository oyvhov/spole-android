package app.reelstack.data.model

data class RequestSeason(val number: Int, val name: String, val episodes: Int, val status: Int) {
    val canRequest: Boolean get() = status == 1 || status == 7
    val label: String get() = when (status) {
        5 -> "I biblioteket"
        4 -> "Delvis i biblioteket"
        2 -> "Ventar på godkjenning"
        3 -> "Førespurd"
        6 -> "Blokkert"
        else -> "Manglar"
    }
}

data class RequestDownload(val season: Int?, val status: String, val size: Double, val remaining: Double)

enum class RequestStage(val label: String, val explanation: String) {
    REQUESTED("Førespurd", "Ventar på godkjenning eller at ei utgåve blir funnen."),
    DOWNLOADING("Lastar ned", "Ei nedlasting er funnen. Innhaldet er ikkje i biblioteket enno."),
    IMPORTING("Blir lagt i biblioteket", "Nedlastinga er ferdig. Ventar på at Seerr registrerer innhaldet."),
    AVAILABLE("I biblioteket", "Ferdig · klart til å sjå i mediebiblioteket ditt."),
    DECLINED("Avvist", "Førespurnaden vart avvist i Seerr."),
    FAILED("Treng tilsyn", "Seerr eller nedlastingsklienten melder om ein feil."),
    UNKNOWN("Status ukjend", "Fekk ikkje oppdatert status. Prøver igjen seinare."),
}

data class RequestProgress(val stage: RequestStage, val percent: Int? = null)

/** Approval is not a download, and completed bytes are not library availability. */
fun requestProgress(
    mediaStatus: Int?, seasons: List<RequestSeason>, selected: Set<Int>,
    downloads: List<RequestDownload>, requestStatus: Int? = null,
): RequestProgress {
    val ready = if (selected.isEmpty()) mediaStatus == 5
        else selected.all { number -> seasons.any { it.number == number && it.status == 5 } }
    if (ready) return RequestProgress(RequestStage.AVAILABLE)
    if (requestStatus == 3 || mediaStatus == 6) return RequestProgress(RequestStage.DECLINED)
    if (requestStatus == 4) return RequestProgress(RequestStage.FAILED)
    // An unrelated season must not make this request appear to be downloading.
    val matching = downloads.filter { selected.isEmpty() || it.season in selected }
    if (matching.any { it.status.lowercase() in setOf("failed", "warning", "error") }) return RequestProgress(RequestStage.FAILED)
    if (matching.isNotEmpty()) {
        if (matching.all { it.status.equals("completed", true) || (it.size > 0 && it.remaining <= 0) }) {
            return RequestProgress(RequestStage.IMPORTING)
        }
        val total = matching.sumOf { it.size.coerceAtLeast(0.0) }
        val left = matching.sumOf { it.remaining.coerceAtLeast(0.0) }
        return RequestProgress(RequestStage.DOWNLOADING,
            if (total > 0) ((1 - left / total) * 100).toInt().coerceIn(0, 99) else null)
    }
    return RequestProgress(RequestStage.REQUESTED)
}

data class TrackedRequest(
    val key: String,
    val mediaId: Int,
    val mediaType: String,
    val title: String,
    val artworkUrl: String?,
    val seasons: Set<Int>,
    val notify: Boolean = true,
    val stage: RequestStage = RequestStage.REQUESTED,
    val percent: Int? = null,
    val notified: Boolean = false,
    val updatedAt: Long = 0,
    val checkedAt: Long = 0,
    val is4k: Boolean = false,
    val availableSeasons: Set<Int> = emptySet(),
    /** Seerr's own request id, needed to withdraw it. Null for follows imported before 0.13. */
    val requestId: Int? = null,
    /**
     * Stages already announced. Per stage, because one `notified` flag meant a "ready" alert also
     * silenced the later "this stopped" alert for the same title.
     */
    val notifiedStages: Set<String> = emptySet(),
)

data class RequestDraft(
    val media: DiscoverMedia,
    val seasons: List<RequestSeason> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val notify: Boolean = true,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val mediaStatus: Int? = null,
)
