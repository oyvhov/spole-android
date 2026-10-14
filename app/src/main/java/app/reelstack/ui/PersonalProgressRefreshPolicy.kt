package app.reelstack.ui

import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind

/** Refresh shelves when a session starts, pauses, stops or changes episode, not on each time tick. */
internal fun personalProgressChanged(
    before: List<PlaybackSession>, after: List<PlaybackSession>, sources: Set<ServiceKind>,
): Boolean {
    fun identity(sessions: List<PlaybackSession>) = sessions.filter { it.source in sources }.associate { session ->
        session.key to listOf(session.title, session.subtitle, session.season, session.episode, session.paused)
    }
    return identity(before) != identity(after)
}
