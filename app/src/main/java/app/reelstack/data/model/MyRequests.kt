package app.reelstack.data.model

/** The tabs of «Dine førespurnader». SERVER is the administrator's view of the whole server. */
enum class MyRequestsTab { ALL, IN_PROGRESS, READY, SERVER }

private val TrackedRequest.ready: Boolean get() = stage == RequestStage.AVAILABLE

/** On the way first, oldest wait first; then what arrived most recently. */
fun myRequestsPreview(requests: List<TrackedRequest>, limit: Int): List<TrackedRequest> {
    val titled = requests.filter { it.hasTitleMetadata }
    val waiting = titled.filterNot { it.ready }.sortedBy { it.updatedAt }
    val arrived = titled.filter { it.ready }.sortedByDescending { it.updatedAt }
    return (waiting + arrived).take(limit)
}

fun myRequestsFor(tab: MyRequestsTab, requests: List<TrackedRequest>): List<TrackedRequest> = when (tab) {
    MyRequestsTab.ALL -> myRequestsPreview(requests, Int.MAX_VALUE) + requests.filterNot { it.hasTitleMetadata }
    MyRequestsTab.IN_PROGRESS -> requests.filter { !it.ready && it.hasTitleMetadata }.sortedBy { it.updatedAt }
    MyRequestsTab.READY -> requests.filter { it.ready && it.hasTitleMetadata }.sortedByDescending { it.updatedAt }
    MyRequestsTab.SERVER -> emptyList()
}
