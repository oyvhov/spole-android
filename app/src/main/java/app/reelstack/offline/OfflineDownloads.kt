@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package app.reelstack.offline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.PlaceholderDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
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
import app.reelstack.data.security.EncryptedTokenStore
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
    /** Kept separately from Media3's request data, encrypted and scoped to this profile. */
    val title: String,
    val subtitle: String = "",
    val mediaType: String = "",
    val mimeType: String? = null,
)

/** Deliberately small view contract: no URL, endpoint, user id or token may reach the UI. */
data class OfflineDownloadItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val service: ServiceKind?,
    val mediaType: String,
    val state: OfflineDownloadState,
    val bytesDownloaded: Long,
    val contentLength: Long,
    val progress: Float,
)

enum class OfflineDownloadState { QUEUED, DOWNLOADING, PAUSED, COMPLETE, FAILED, REMOVING }

data class OfflineDownloadsSnapshot(
    val items: List<OfflineDownloadItem> = emptyList(),
    val totalBytes: Long = 0,
    val completedBytes: Long = 0,
    val hasActiveDownloads: Boolean = false,
)

/** The only data a local player needs after a title is fully cached. */
data class OfflinePlaybackSource(
    val id: String,
    val title: String,
    val subtitle: String,
    val mediaType: String,
    val service: ServiceKind,
    /** Opaque identity binding used by the local player before it records progress. */
    val serviceScope: String,
    val uri: Uri,
    val cacheKey: String,
    val itemId: String,
)

/**
 * Persistent Media3 manager with an app-private cache. Every HTTP request gets auth headers at
 * execution time from the currently stored account, rather than serialising a token in the Media3
 * database. A sign-out therefore makes a paused job unusable before its files are removed.
 */
object OfflineDownloadRuntime {
    @Volatile private var manager: DownloadManager? = null
    @Volatile private var database: StandaloneDatabaseProvider? = null
    @Volatile private var cache: SimpleCache? = null

    fun manager(context: Context): DownloadManager = manager ?: synchronized(this) {
        manager ?: build(context.applicationContext).also { manager = it }
    }

    /**
     * A completed download is read from the same private cache the service populated. The
     * placeholder upstream is intentional: an offline player must fail cleanly rather than quietly reaching
     * a server with a stale request after the user chose to watch without a network.
     */
    fun offlineDataSourceFactory(context: Context): CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache(context.applicationContext))
        .setCacheReadDataSourceFactory(FileDataSource.Factory())
        .setUpstreamDataSourceFactory(PlaceholderDataSource.FACTORY)
        .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)

    private fun database(context: Context): StandaloneDatabaseProvider = database ?: synchronized(this) {
        database ?: StandaloneDatabaseProvider(context.applicationContext).also { database = it }
    }

    private fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(File(context.filesDir, "offline-media3"), NoOpCacheEvictor(), database(context)).also { cache = it }
    }

    private fun build(context: Context): DownloadManager {
        val upstream = OkHttpDataSource.Factory(HttpTransport.sharedClient)
        val authenticated = ResolvingDataSource.Factory(upstream) { dataSpec ->
            val connection = connectionFor(context, dataSpec.uri, dataSpec.key)
                ?: throw java.io.IOException("offline media route is no longer available")
            dataSpec.withRequestHeaders(MediaPlaybackClient(deviceId = DeviceIdentity.get(context)).headers(connection))
        }
        return DownloadManager(context, database(context), cache(context), authenticated, Executors.newFixedThreadPool(2)).apply {
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
    private val catalog = OfflineDownloadCatalog(appContext)

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
        // This remains in a keystore-encrypted local catalogue. The Media3 database carries only
        // hashes, so neither a database inspection nor a diagnostic export reveals watch history.
        catalog.put(request.profileId, OfflineCatalogEntry(
            id = id,
            itemId = request.candidate.itemId,
            title = request.title.cleanOfflineLabel(),
            subtitle = request.subtitle.cleanOfflineLabel(),
            service = request.connection.kind,
            serviceScope = offlineServiceHash(request.connection),
            mediaType = request.mediaType.cleanOfflineLabel(),
        ))
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
    fun pause(id: String) = DownloadService.sendSetStopReason(
        appContext, OfflineDownloadService::class.java, id, STOP_REASON_USER_PAUSED, false,
    )
    fun resume(id: String) = DownloadService.sendSetStopReason(
        appContext, OfflineDownloadService::class.java, id, Download.STOP_REASON_NONE, false,
    )
    fun retry(id: String): Boolean {
        val request = downloads().firstOrNull { it.request.id == id }?.request ?: return false
        DownloadService.sendAddDownload(appContext, OfflineDownloadService::class.java, request, false)
        return true
    }
    fun remove(profileId: String, id: String) {
        catalog.remove(profileId, id)
        DownloadService.sendRemoveDownload(appContext, OfflineDownloadService::class.java, id, false)
    }
    fun removeAll() {
        catalog.clearAll()
        DownloadService.sendRemoveAllDownloads(appContext, OfflineDownloadService::class.java, false)
    }

    /** Reading the index is local and never wakes a media server. */
    fun snapshot(profileId: String): OfflineDownloadsSnapshot {
        val entries = catalog.entries(profileId).associateBy { it.id }
        val items = downloads().mapNotNull { download ->
            val scope = download.scope() ?: return@mapNotNull null
            if (scope.profile != offlineScopeHash(profileId.ifBlank { "adult" })) return@mapNotNull null
            // The encrypted catalogue is the ownership record. If it was removed first, hide the
            // Media3 job immediately while the service finishes deleting its cache bytes.
            val saved = entries[download.request.id] ?: return@mapNotNull null
            val contentLength = download.contentLength.coerceAtLeast(0)
            val bytes = download.bytesDownloaded.coerceAtLeast(0)
            val progress = when {
                contentLength > 0 -> (bytes.toFloat() / contentLength).coerceIn(0f, 1f)
                download.percentDownloaded >= 0f -> (download.percentDownloaded / 100f).coerceIn(0f, 1f)
                download.state == Download.STATE_COMPLETED -> 1f
                else -> 0f
            }
            OfflineDownloadItem(
                id = download.request.id,
                title = saved.title,
                subtitle = saved.subtitle,
                service = saved.service,
                mediaType = saved.mediaType,
                state = download.toOfflineState(),
                bytesDownloaded = bytes,
                contentLength = contentLength,
                progress = progress,
            )
        }.sortedWith(compareBy<OfflineDownloadItem> { it.state == OfflineDownloadState.COMPLETE }.thenBy { it.title })
        return OfflineDownloadsSnapshot(
            items = items,
            totalBytes = items.sumOf { it.contentLength },
            completedBytes = items.sumOf { it.bytesDownloaded },
            hasActiveDownloads = items.any { it.state in setOf(OfflineDownloadState.QUEUED, OfflineDownloadState.DOWNLOADING) },
        )
    }

    fun playbackSource(profileId: String, id: String): OfflinePlaybackSource? {
        val normalizedProfile = profileId.ifBlank { "adult" }
        val connections = ConnectionRepository(appContext)
        // A cached byte stream must not survive an account replacement. This also makes old
        // catalogue records created by an earlier app version fail closed.
        if (connections.activeProfileId.ifBlank { "adult" } != normalizedProfile) return null
        val item = snapshot(profileId).items.firstOrNull { it.id == id && it.state == OfflineDownloadState.COMPLETE } ?: return null
        val entry = catalog.entries(profileId).firstOrNull { it.id == id } ?: return null
        val download = downloads().firstOrNull { it.request.id == id } ?: return null
        val service = entry.service.takeIf { it in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) } ?: return null
        if (entry.serviceScope.isBlank() || connections.list().none { connection ->
                connection.kind == service && connection.token.isNotBlank() &&
                    offlineServiceHash(connection) == entry.serviceScope
            }
        ) return null
        return OfflinePlaybackSource(
            id = item.id,
            title = entry.title,
            subtitle = entry.subtitle,
            mediaType = entry.mediaType,
            service = service,
            serviceScope = entry.serviceScope,
            uri = download.request.uri,
            cacheKey = download.request.customCacheKey ?: download.request.id,
            itemId = entry.itemId,
        )
    }

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
                        JSONObject(String(download.request.data, Charsets.UTF_8))
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
                        JSONObject(String(download.request.data, Charsets.UTF_8))
                    }.getOrNull() ?: continue
                    if (data.optString("profile") == profile && (service == null || data.optString("service") == service)) {
                        remove(profileId, download.request.id)
                    }
                }
            }
        }
        if (connection == null) catalog.clear(profileId) else catalog.clearService(profileId, connection)
    }

    private fun downloadId(profileId: String, connection: ServiceConnection, itemId: String): String =
        "${offlineDownloadPrefix(profileId.ifBlank { "adult" }, connection)}-${offlineScopeHash(itemId).take(24)}"

    private fun downloads(): List<Download> = runCatching {
        OfflineDownloadRuntime.manager(appContext).downloadIndex.getDownloads().use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.download) }
        }
    }.getOrDefault(emptyList())

    private companion object {
        // App-defined, non-zero stop reasons are intentionally stable if the process restarts.
        const val STOP_REASON_PROFILE_INACTIVE = 7_401
        const val STOP_REASON_USER_PAUSED = 7_402
    }
}

private data class OfflineScope(val profile: String, val service: String)

private fun Download.scope(): OfflineScope? = runCatching {
    val payload = JSONObject(String(request.data, Charsets.UTF_8))
    OfflineScope(payload.getString("profile"), payload.getString("service"))
}.getOrNull()

internal fun offlineDownloadState(state: Int): OfflineDownloadState = when (state) {
    Download.STATE_DOWNLOADING -> OfflineDownloadState.DOWNLOADING
    Download.STATE_COMPLETED -> OfflineDownloadState.COMPLETE
    Download.STATE_FAILED -> OfflineDownloadState.FAILED
    Download.STATE_REMOVING -> OfflineDownloadState.REMOVING
    Download.STATE_STOPPED -> OfflineDownloadState.PAUSED
    else -> OfflineDownloadState.QUEUED
}

internal fun Download.toOfflineState(): OfflineDownloadState = offlineDownloadState(state)

private data class OfflineCatalogEntry(
    val id: String,
    val itemId: String,
    val title: String,
    val subtitle: String,
    val service: ServiceKind,
    /** One-way hash of the exact server/account identity; never rendered or logged. */
    val serviceScope: String,
    val mediaType: String,
)

/**
 * Human-readable media metadata is private watch history. It lives in the Android Keystore-backed
 * store, indexed by a profile hash, while the download index itself stays opaque.
 */
private class OfflineDownloadCatalog(context: Context) {
    private val store = EncryptedTokenStore(context.applicationContext)

    fun entries(profileId: String): List<OfflineCatalogEntry> = runCatching {
        val payload = JSONObject(store.get(key(profileId)).orEmpty())
        payload.optJSONArray("items")?.let { items ->
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val source = ServiceKind.entries.firstOrNull { it.name == item.optString("service") } ?: continue
                    if (id.isBlank()) continue
                    add(OfflineCatalogEntry(
                        id = id,
                        itemId = item.optString("item"),
                        title = item.optString("title"),
                        subtitle = item.optString("subtitle"),
                        service = source,
                        serviceScope = item.optString("serviceScope"),
                        mediaType = item.optString("type"),
                    ))
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    fun put(profileId: String, entry: OfflineCatalogEntry) = write(profileId, entries(profileId).filterNot { it.id == entry.id } + entry)

    fun remove(profileId: String, id: String) = write(profileId, entries(profileId).filterNot { it.id == id })

    fun clear(profileId: String) {
        store.remove(key(profileId))
        writeIndex(index() - profileHash(profileId))
    }

    fun clearService(profileId: String, connection: ServiceConnection) {
        val serviceScope = offlineServiceHash(connection)
        write(profileId, entries(profileId).filterNot { it.serviceScope == serviceScope })
    }

    fun clearAll() {
        index().forEach { profile -> store.remove(keyForHash(profile)) }
        store.remove(INDEX_KEY)
    }

    private fun write(profileId: String, entries: List<OfflineCatalogEntry>) {
        val hash = profileHash(profileId)
        if (entries.isEmpty()) {
            store.remove(keyForHash(hash))
            writeIndex(index() - hash)
            return
        }
        val payload = JSONObject().put("items", org.json.JSONArray().apply {
            entries.forEach { entry -> put(JSONObject().apply {
                put("id", entry.id); put("item", entry.itemId); put("title", entry.title)
                put("subtitle", entry.subtitle); put("service", entry.service.name)
                put("serviceScope", entry.serviceScope); put("type", entry.mediaType)
            }) }
        })
        store.put(keyForHash(hash), payload.toString())
        writeIndex(index() + hash)
    }

    private fun index(): Set<String> = runCatching {
        val items = org.json.JSONArray(store.get(INDEX_KEY).orEmpty())
        buildSet { for (index in 0 until items.length()) items.optString(index).takeIf(String::isNotBlank)?.let(::add) }
    }.getOrDefault(emptySet())

    private fun writeIndex(value: Set<String>) {
        if (value.isEmpty()) store.remove(INDEX_KEY)
        else store.put(INDEX_KEY, org.json.JSONArray(value.sorted()).toString())
    }

    private fun key(profileId: String) = keyForHash(profileHash(profileId))
    private fun keyForHash(profileHash: String) = "$KEY_PREFIX$profileHash"
    private fun profileHash(profileId: String) = offlineScopeHash(profileId.ifBlank { "adult" })

    private companion object {
        const val KEY_PREFIX = "offline.catalog."
        const val INDEX_KEY = "offline.catalog.index"
    }
}

private fun String.cleanOfflineLabel(): String = trim().replace(Regex("\\s+"), " ").take(240)

private fun offlineDownloadPrefix(profileId: String, connection: ServiceConnection): String =
    "offline-${offlineScopeHash(profileId).take(12)}-${offlineServiceHash(connection).take(12)}"

internal fun offlineServiceHash(connection: ServiceConnection): String =
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
        val openDownloads = PendingIntent.getActivity(
            applicationContext,
            3,
            Intent(applicationContext, app.reelstack.MainActivity::class.java)
                .putExtra("open_downloads", true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_spole)
            .setContentTitle(applicationContext.getString(R.string.offline_notification_title))
            .setContentText(applicationContext.getString(R.string.offline_notification_text, downloads.size))
            .setProgress(100, progress, total <= 0)
            .setOnlyAlertOnce(true)
            .setContentIntent(openDownloads)
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
