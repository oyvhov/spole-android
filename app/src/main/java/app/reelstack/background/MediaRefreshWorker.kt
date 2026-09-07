package app.reelstack.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.reelstack.AppContainer
import java.util.concurrent.TimeUnit

class MediaRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        val connections = container.connectionRepository.list()
        val configured = connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        if (configured.isEmpty()) return Result.success()
        configured.firstOrNull { it.kind == app.reelstack.data.model.ServiceKind.SEERR && it.sessionCookie }?.let {
            runCatching { container.requestTrackingRepository.refresh(it) }
        }

        val snapshot = runCatching {
            container.mediaSyncRepository.refresh(
                connections = connections,
            )
        }.getOrElse { return Result.retry() }

        if (snapshot.successfulServices.isNotEmpty() && snapshot.errors.isEmpty()) {
            runCatching {
                container.mediaSnapshotStore.save(
                    snapshot,
                    app.reelstack.data.repository.MediaSnapshotStore.fingerprint(connections),
                )
            }
        }
        // The home-screen widget has no schedule of its own beyond Android's 30-minute floor;
        // a completed background refresh is the best moment to redraw it.
        app.reelstack.widget.NowPlayingWidget.requestUpdate(applicationContext)
        return if (snapshot.successfulServices.isEmpty() && snapshot.errors.isNotEmpty()) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}

object BackgroundRefreshScheduler {
    private const val WORK_NAME = "reelstack-media-refresh"

    fun schedule(context: Context, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<MediaRefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
