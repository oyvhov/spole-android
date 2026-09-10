package app.reelstack.data.model

data class RequestHistoryState(
    val items: List<TrackedRequest> = emptyList(),
    val nextOffset: Int = 0,
    val hasMore: Boolean = false,
    val total: Int? = null,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val retryFromStart: Boolean = false,
) {
    fun append(page: List<TrackedRequest>, offset: Int, more: Boolean, count: Int?): RequestHistoryState {
        require(offset > nextOffset || !more) { "History page did not advance" }
        // Seerr uses offsets: new requests may move a boundary while older pages are loaded.
        val merged = (items + page).distinctBy { it.requestId }
        check(!more || page.isEmpty() || merged.size > items.size) { "History page repeated" }
        return copy(items = merged, nextOffset = offset, hasMore = more, total = count,
            loaded = true, loading = false, error = null)
    }
}

/** Never evict an enabled alert or unfinished follow when newer history arrives. */
internal fun retainTrackedRequests(items: List<TrackedRequest>): List<TrackedRequest> {
    val sorted = items.sortedByDescending { it.updatedAt }
    val protected = sorted.filter { it.notify || it.availabilityOnly || it.stage !in
        setOf(RequestStage.AVAILABLE, RequestStage.DECLINED, RequestStage.FAILED) }
    val protectedKeys = protected.mapTo(mutableSetOf()) { it.key }
    return (protected + sorted.filterNot { it.key in protectedKeys }.take(100)).sortedByDescending { it.updatedAt }
}
