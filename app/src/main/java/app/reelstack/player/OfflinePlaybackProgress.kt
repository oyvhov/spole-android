package app.reelstack.player

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.LocalPlaybackStore
import app.reelstack.data.repository.PlaybackProgressOutbox
import app.reelstack.offline.OfflinePlaybackSource

/** IO-only checkpoints. The caller holds the session guard and keeps workers out with its lease. */
internal class OfflinePlaybackProgress(
    private val profileId: String,
    private val journal: LocalPlaybackStore,
    private val outbox: PlaybackProgressOutbox,
) {
    fun record(connection: ServiceConnection, source: OfflinePlaybackSource, positionMs: Long,
        durationMs: Long, completed: Boolean) {
        val scope = PlaybackProgressOutbox.scope(profileId, connection)
        val previous = outbox.pending(scope).lastOrNull { it.itemId == source.itemId }
        val baseline = previous?.baseline ?: source.progressBaseline.takeIf { source.progressScope == scope }
        val item = PlayableItem(source.itemId, source.title, type = source.mediaType, subtitle = source.subtitle)
        val journalId = journal.record(connection, item, positionMs, durationMs, completed) ?: return
        if (baseline == null || connection.userId.isBlank()) return
        outbox.put(PendingPlaybackProgress(journalId, scope, item.id,
            positionMs.coerceIn(0, durationMs), completed, baseline, System.currentTimeMillis(), journalId))
    }

    fun resumePosition(connection: ServiceConnection, source: OfflinePlaybackSource): Long {
        val scope = PlaybackProgressOutbox.scope(profileId, connection)
        val pending = outbox.latest(scope, source.itemId)
        if (pending != null) return if (pending.completed) 0 else pending.positionMs
        val baseline = source.progressBaseline.takeIf { source.progressScope == scope }
        return journal.resume(connection, PlayableItem(source.itemId, source.title, subtitle = source.subtitle,
            type = source.mediaType, resumeMs = if (baseline?.played == true) 0 else (baseline?.positionTicks ?: 0) / 10_000)).resumeMs
    }
}
