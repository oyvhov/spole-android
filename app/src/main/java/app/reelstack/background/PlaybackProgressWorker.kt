package app.reelstack.background

import android.content.Context
import androidx.work.*
import app.reelstack.ReelstackApplication
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.DeviceIdentity
import app.reelstack.data.repository.PlaybackProgressOutbox
import app.reelstack.player.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Process-local leases keep a retry from racing the live reporter, even in another activity. */
object PlaybackProgressRuntime {
    val network = Mutex()
    private val players = mutableSetOf<String>()
    @Synchronized fun acquire(): String = UUID.randomUUID().toString().also(players::add)
    @Synchronized fun release(lease: String) { players.remove(lease) }
    @Synchronized fun isIdle(): Boolean = players.isEmpty()
}

class PlaybackProgressWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val container = (applicationContext as ReelstackApplication).container
        if (!app.reelstack.offline.offlineNetworkAllowed(applicationContext, container.preferencesRepository.wifiOnly)) return@withContext Result.retry()
        val owner = container.connectionRepository.captureSession()
        fun allowed() = !isStopped && container.connectionRepository.isCurrent(owner) && PlaybackProgressRuntime.isIdle() &&
            app.reelstack.offline.offlineNetworkAllowed(applicationContext, container.preferencesRepository.wifiOnly)
        if (!allowed()) return@withContext Result.success() // Closing the live reporter schedules another run.
        PlaybackProgressRuntime.network.withLock {
            val outbox = container.playbackProgressOutbox
            val sync = PlaybackProgressSync(outbox, PlaybackProgressClient(DeviceIdentity.get(applicationContext)))
            var retry = false
            for (connection in container.connectionRepository.list(owner.profileId)) {
                if (connection.kind !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) ||
                    connection.token.isBlank() || connection.userId.isBlank()) continue
                if (!allowed()) return@withLock Result.success()
                val scope = PlaybackProgressOutbox.scope(owner.profileId, connection)
                for (entry in outbox.pending(scope)) {
                    val result = sync.sync(connection, entry, ::allowed) { state ->
                        container.connectionRepository.withCurrentSession(owner) {
                            container.offlineDownloads.confirmProgress(owner.profileId, connection, entry.itemId, state)
                            container.localPlaybackStore.confirm(connection, entry.itemId, entry.journalId,
                                state.positionTicks / 10_000, state.played)
                        }
                    }
                    if (result == PlaybackSyncResult.RETRY) { retry = true; break }
                    if (result == PlaybackSyncResult.CANCELLED) return@withLock Result.success()
                }
            }
            if (retry) Result.retry() else Result.success()
        }
    }
}

object PlaybackProgressScheduler {
    private const val NAME = "spole-playback-progress"
    fun schedule(context: Context, afterPlayback: Boolean = false) {
        val work = OneTimeWorkRequestBuilder<PlaybackProgressWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME,
            if (afterPlayback) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, work)
    }
}
