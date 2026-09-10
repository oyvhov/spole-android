package app.reelstack.ui

import app.reelstack.data.model.ServiceKind

/** Retry only an incomplete media feed, not a genuinely empty resume list or unrelated queue. */
internal fun shouldRetryHomeFeed(failed: Set<ServiceKind>, warnings: Set<ServiceKind>, refreshing: Boolean,
    elapsedMillis: Long): Boolean = !refreshing && elapsedMillis >= 60_000L &&
    (failed + warnings).any { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
