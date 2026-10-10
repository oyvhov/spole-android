package app.reelstack.ui

import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind

/** Jellyfin's socket only replaces Jellyfin polling. Emby always has its own clock. */
internal class PlaybackRefreshSchedule {
    private val refreshedAt = mutableMapOf<ServiceKind, Long>()

    fun due(sources: Set<ServiceKind>, jellyfinPush: Boolean, detailsOpen: Boolean, now: Long): Set<ServiceKind> =
        sources.filterTo(linkedSetOf()) { source ->
            val last = refreshedAt[source]
            last == null || now - last >= interval(source, jellyfinPush, detailsOpen)
        }

    fun refreshed(sources: Set<ServiceKind>, now: Long) {
        sources.forEach { refreshedAt[it] = now }
    }

    fun waitMillis(sources: Set<ServiceKind>, jellyfinPush: Boolean, detailsOpen: Boolean, now: Long): Long =
        sources.minOfOrNull { source ->
            val last = refreshedAt[source] ?: return@minOfOrNull 1L
            (interval(source, jellyfinPush, detailsOpen) - (now - last)).coerceAtLeast(1L)
        } ?: 60_000L

    private fun interval(source: ServiceKind, jellyfinPush: Boolean, detailsOpen: Boolean): Long = when {
        detailsOpen -> 2_000L
        source == ServiceKind.JELLYFIN && jellyfinPush -> 60_000L
        else -> 3_000L
    }
}

/** A service refresh may remove its stopped sessions without erasing another server's sessions. */
internal fun mergePlaybackSessions(
    current: List<PlaybackSession>,
    refreshed: List<PlaybackSession>,
    sources: Set<ServiceKind>,
): List<PlaybackSession> {
    val replacements = refreshed.filter { it.source in sources }.associateBy { it.key }.toMutableMap()
    return buildList {
        current.forEach { session ->
            if (session.source !in sources) add(session)
            else replacements.remove(session.key)?.let(::add)
        }
        addAll(replacements.values)
    }.distinctBy { it.key }
}
