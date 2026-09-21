@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.offline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.media3.exoplayer.scheduler.Scheduler
import app.reelstack.R
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.HttpTransport
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.DeviceIdentity
import app.reelstack.player.MediaPlaybackClient
import app.reelstack.player.safePlaybackUrl
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/** Public request record. `data` carries hashes only; account tokens stay in ConnectionRepository. */
data class OfflineDownloadRequest(
    val profileId: String,
    val connection: ServiceConnection,
    val candidate: OfflineMediaCandidate,
    val mimeType: String? = null,
)

/**
 * Persistent Media3 manager with an app-private cache. Every HTTP request gets auth headers at
 * execution time from the currently stored account, rather than serialising a token in the Media3
 * database. A sign-out therefore makes a paused job unusable before its files are removed.
 */
object OfflineDownloadRuntime {
    @Volatile private var manager: DownloadManager? = null

    fun manager(context: Context): DownloadManager = manager ?: synchronized(this) {
        manager ?: build(context.applicationContext).also { manager = it }
    }

    private fun build(context: Context): DownloadManager {
        val database = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(File(context.filesDir, "offline-media3"), NoOpCacheEvictor(), database)
        val upstream = OkHttpDataSource.Factory(HttpTransport.sharedClient)
        val authenticated = ResolvingDataSource.Factory(upstream) { dataSpec ->
            val connection = connectionFor(context, dataSpec.uri, dataSpec.key)
                ?: throw java.io.IOException("offline media route is no longer available")
            dataSpec.withRequestHeaders(MediaPlaybackClient(deviceId = DeviceIdentity.get(context)).headers(connection))
        }
        return DownloadManager(context, database, cache, authenticated, Executors.newFixedThreadPool(2)).apply {
            maxParallelDownloads = 1
            minRetryCount = 3
            requirements = Requirements(Requirements.NETWORK)
        }
    }

    private fun connectionFor(context: Context, uri: Uri, cacheKey: String?): ServiceConnection? {
        val candidate = uri.toString()
        val repository = ConnectionRepository(context)
        val profile = repository.activeProfileId.ifBlank { "adult" }
        return repository.list().firstOrNull { connection ->
            connection.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                // A compatible second account on the same server must never lend its token to an
                // old job. The opaque cache key scopes the request before an HTTP header exists.
                cacheKey?.startsWith(offlineDownloadPrefix(profile, connection) + "-") == true &&
                connection.token.isNotBlank() && listOf(connection.baseUrl, connection.alternateUrl)
                    .filter(String::isNotBlank).any { base -> runCatching { safePlaybackUrl(base, candidate) }.isSuccess }
        }
    }
}

class OfflineDownloadRepository(private val context: Context) {
    private val appContext = context.applicationContext

    fun enqueue(request: OfflineDownloadRequest): OfflineRefusal? {
        val refusal = request.candidate.offlineRefusal()
        if (refusal != null) return refusal
        val directUrl = runCatching { safePlaybackUrl(request.connection.baseUrl, request.candidate.directDownloadUrl) }
            .recoverCatching { safePlaybackUrl(request.connection.alternateUrl, request.candidate.directDownloadUrl) }
            .getOrElse { return OfflineRefusal.MISSING_DIRECT_FILE }
        val id = downloadId(request.profileId, request.connection, request.candidate.itemId)
        val data = JSONObject().apply {
            put("version", 1)
            put("profile", offlineScopeHash(request.profileId.ifBlank { "adult" }))
            put("service", offlineServiceHash(request.connection))
            put("item", offlineScopeHash(request.candidate.itemId))
        }.toString().toByteArray(Charsets.UTF_8)
        val builder = DownloadRequest.Builder(id, Uri.parse(directUrl))
            .setCustomCacheKey(id)
            .setData(data)
        request.mimeType?.let(builder::setMimeType)
        val download = builder.build()
        DownloadService.sendAddDownload(appContext, OfflineDownloadService::class.java, download, true)
        return null
    }

    fun pauseAll() = DownloadService.sendPauseDownloads(appContext, OfflineDownloadService::class.java, false)
    fun resumeAll() = DownloadService.sendResumeDownloads(appContext, OfflineDownloadService::class.java, false)
    fun onlyWifi(enabled: Boolean) = DownloadService.sendSetRequirements(
        appContext,
        OfflineDownloadService::class.java,
        Requirements(if (enabled) Requirements.NETWORK_UNMETERED else Requirements.NETWORK),
        false,
    )
    fun remove(id: String) = DownloadService.sendRemoveDownload(appContext, OfflineDownloadService::class.java, id, false)
    fun removeAll() = DownloadService.sendRemoveAllDownloads(appContext, OfflineDownloadService::class.java, false)

    /**
     * A profile switch is not a failure: retain that profile's private job but stop it before a
     * different account becomes active. Its own token is selected only when that profile returns.
     */
    fun setProfileActive(profileId: String, active: Boolean) {
        val profile = offlineScopeHash(profileId.ifBlank { "adult" })
        runCatching {
            OfflineDownloadRuntime.manager(appContext).downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val download = cursor.download
                    val data = runCatching {
                        JSONObject(download.request.data?.let { bytes -> String(bytes, Charsets.UTF_8) }.orEmpty())
                    }.getOrNull() ?: continue
                    if (data.optString("profile") == profile) {
                        DownloadService.sendSetStopReason(
                            appContext,
                            OfflineDownloadService::class.java,
                            download.request.id,
                            if (active) Download.STOP_REASON_NONE else STOP_REASON_PROFILE_INACTIVE,
                            false,
                        )
                    }
                }
            }
        }
    }

    fun removeScope(profileId: String, connection: ServiceConnection? = null) {
        val profile = offlineScopeHash(profileId.ifBlank { "adult" })
        val service = connection?.let(::offlineServiceHash)
        runCatching {
            OfflineDownloadRuntime.manager(appContext).downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val download = cursor.download
                    val data = runCatching {
                        JSONObject(download.request.data?.let { bytes -> String(bytes, Charsets.UTF_8) }.orEmpty())
                    }.getOrNull() ?: continue
                    if (data.optString("profile") == profile && (service == null || data.optString("service") == service)) remove(download.request.id)
                }
            }
        }
    }

    private fun downloadId(profileId: String, connection: ServiceConnection, itemId: String): String =
        "${offlineDownloadPrefix(profileId.ifBlank { "adult" }, connection)}-${offlineScopeHash(itemId).take(24)}"

    private companion object {
        // App-defined, non-zero stop reasons are intentionally stable if the process restarts.
        const val STOP_REASON_PROFILE_INACTIVE = 7_401
    }
}

private fun offlineDownloadPrefix(profileId: String, connection: ServiceConnection): String =
    "offline-${offlineScopeHash(profileId).take(12)}-${offlineServiceHash(connection).take(12)}"

private fun offlineServiceHash(connection: ServiceConnection): String =
    offlineScopeHash("${connection.kind.name}|${connection.identity}|${connection.userId}")

private fun offlineScopeHash(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

class OfflineDownloadService : DownloadService(NOTIFICATION_ID, 1_000L, CHANNEL_ID, R.string.offline_notification_channel, 0) {
    override fun getDownloadManager(): DownloadManager = OfflineDownloadRuntime.manager(applicationContext)
    override fun getScheduler(): Scheduler? = null
    override fun getForegroundNotification(downloads: List<Download>, notMetRequirements: Int): Notification {
        channel(applicationContext)
        val knownLength = downloads.filter { it.contentLength > 0 }
        val total = knownLength.sumOf { it.contentLength.toDouble() }
        val completed = knownLength.sumOf { it.bytesDownloaded.coerceAtMost(it.contentLength).toDouble() }
        val progress = if (total > 0) ((completed * 100 / total).toInt()).coerceIn(0, 100) else 0
        val downloading = downloads.any { it.state == Download.STATE_DOWNLOADING }
        val actionLabel = applicationContext.getString(if (downloading) R.string.offline_pause else R.string.offline_resume)
        val serviceIntent = if (downloading) DownloadService.buildPauseDownloadsIntent(
            applicationContext, OfflineDownloadService::class.java, false,
        ) else DownloadService.buildResumeDownloadsIntent(
            applicationContext, OfflineDownloadService::class.java, false,
        )
        val actionIntent = PendingIntent.getService(
            applicationContext,
            if (downloading) 1 else 2,
            serviceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_spole)
            .setContentTitle(applicationContext.getString(R.string.offline_notification_title))
            .setContentText(applicationContext.getString(R.string.offline_notification_text, downloads.size))
            .setProgress(100, progress, total <= 0)
            .setOnlyAlertOnce(true)
            .addAction(0, actionLabel, actionIntent)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val NOTIFICATION_ID = 7411
        const val CHANNEL_ID = "spole-offline"
        fun channel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.offline_notification_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
