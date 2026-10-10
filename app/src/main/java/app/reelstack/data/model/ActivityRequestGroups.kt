package app.reelstack.data.model

/** A presentation group only. Each request keeps its own status, notification and cancel action. */
data class ActivityRequestGroup(val key: String, val requests: List<TrackedRequest>)

/** Called after the current account and Activity filter have selected their visible requests. */
fun groupActivityRequests(requests: List<TrackedRequest>): List<ActivityRequestGroup> =
    requests.groupBy { request ->
        if (request.mediaId > 0 && request.mediaType.lowercase() in setOf("tv", "series") && request.seasons.isNotEmpty())
            "series:${request.mediaId}:${request.is4k}:${request.availabilityOnly}"
        else "request:${request.key}"
    }.map { (key, items) -> ActivityRequestGroup(key, items) }
