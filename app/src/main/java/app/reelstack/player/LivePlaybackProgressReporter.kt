package app.reelstack.player

import app.reelstack.AppContainer
import app.reelstack.background.PlaybackProgressRuntime
import app.reelstack.background.PlaybackProgressScheduler
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.PlaybackProgressOutbox
import app.reelstack.data.repository.SessionScope
import kotlinx.coroutines.sync.withLock

internal data class PlaybackReport(
    val connection: ServiceConnection, val plan: PlaybackPlan, val event: String, val position: Long,
    val paused: Boolean, val journalId: String?, val completed: Boolean, val recordedAt: Long,
)

/** Serial IO owner. The player only snapshots Media3 state; disk and network stay off its thread. */
internal class LivePlaybackProgressReporter(
    private val container: AppContainer,
    private val owner: SessionScope,
    private val client: MediaPlaybackClient,
    private val progressClient: PlaybackProgressClient,
) {
    private var baselineItem = ""
    private var baseline: PlaybackUserState? = null
    private var retryScheduled = false

    /** Null means revoked. A final stop may close the actual old session, but cannot enqueue a retry. */
    suspend fun send(event: PlaybackReport): Boolean? = PlaybackProgressRuntime.network.withLock {
        fun current() = container.connectionRepository.isCurrent(owner)
        if (!current() && event.event != "/Stopped") return@withLock null
        if (baselineItem != event.plan.item.id) {
            baselineItem = event.plan.item.id
            baseline = event.plan.item.progressState
        }
        val checkpoint = baseline?.let { known -> event.journalId?.let { journal ->
            PendingPlaybackProgress(journal, PlaybackProgressOutbox.scope(owner.profileId, event.connection),
                event.plan.item.id, event.position, event.completed, known, event.recordedAt, journal)
        } }?.takeIf { event.connection.userId.isNotBlank() }
        val persisted = checkpoint?.takeIf {
            runCatching { container.connectionRepository.withCurrentSession(owner) {
                container.playbackProgressOutbox.put(it)
                true
            } == true }.getOrDefault(false)
        }
        // One request per reporter is enough; close/startup also schedule recovery. Do not churn
        // WorkManager's database on every ten-second tick while the live lease holds it back.
        if (persisted != null && !retryScheduled) retryScheduled =
            runCatching { PlaybackProgressScheduler.schedule(container.appContext) }.isSuccess
        if (!current() && event.event != "/Stopped") return@withLock null
        val failed = runCatching { client.report(event.connection, event.plan, event.event, event.position, event.paused) }.isFailure
        if (!failed && current()) {
            val state = runCatching { progressClient.read(event.connection, event.plan.item.id).state }.getOrNull()
            if (state != null) container.connectionRepository.withCurrentSession(owner) {
                baseline = state
                container.offlineDownloads.confirmProgress(owner.profileId, event.connection, event.plan.item.id, state)
                // Failure to remove is safe: read/compare/write will recognise this acknowledgement.
                if (persisted != null) runCatching { container.playbackProgressOutbox.remove(persisted) }
                if (event.journalId != null && (event.paused || event.event == "/Stopped" ||
                        playbackNearEnd(event.position, event.plan.item.durationMs))) {
                    container.localPlaybackStore.confirm(event.connection, event.plan.item.id, event.journalId,
                        state.positionTicks / 10_000, state.played)
                }
            }
        }
        failed
    }
}
