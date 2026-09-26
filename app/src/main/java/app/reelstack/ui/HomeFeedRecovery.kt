package app.reelstack.ui

import app.reelstack.data.model.ServiceKind

/**
 * Retry only an incomplete media feed, not a genuinely empty resume list or unrelated queue — and
 * less often each time. A server that stays down, or a warning that is really a permission (an
 * account that may not list sessions), used to bring a full refresh every minute for as long as
 * Home was open.
 */
internal fun shouldRetryHomeFeed(failed: Set<ServiceKind>, warnings: Set<ServiceKind>, refreshing: Boolean,
    elapsedMillis: Long, attempts: Int = 0): Boolean {
    if (refreshing) return false
    val mediaFailed = failed.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    val mediaWarned = warnings.any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
    if (!mediaFailed && !mediaWarned) return false
    return elapsedMillis >= homeFeedRetryDelayMillis(attempts, failedOutright = mediaFailed)
}

/** One minute after a failure and five after a partial answer, doubling up to half an hour. */
internal fun homeFeedRetryDelayMillis(attempts: Int, failedOutright: Boolean): Long {
    val first = if (failedOutright) 60_000L else 5 * 60_000L
    return (first shl attempts.coerceIn(0, 5)).coerceAtMost(30 * 60_000L)
}
