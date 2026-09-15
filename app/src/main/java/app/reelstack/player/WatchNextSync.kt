package app.reelstack.player

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.media.tv.TvContract.WatchNextPrograms as Programs
import android.net.Uri
import app.reelstack.AppContainer
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.MediaSnapshotStore
import coil3.imageLoader
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** No network is needed for the list. Artwork is a small, opt-in, revocable local copy. */
class WatchNextSync(private val container: AppContainer) {
    private val context = container.appContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val enabled = MutableStateFlow(false)
    private var stopObserving: (() -> Unit)? = null
    private val mutex = Mutex()
    private var cleanedWhileInactive = false

    fun start() {
        if (!context.packageManager.hasSystemFeature("android.software.leanback") || stopObserving != null) return
        stopObserving = container.preferencesRepository.observePersonalization { enabled.value = it.watchNextEnabled }
        scope.launch {
            combine(enabled, container.localPlaybackStore.changes, container.connectionRepository.changes, container.mediaSnapshotStore.changes) { _, _, _, _ -> Unit }
                .collect { sync() }
        }
    }

    suspend fun sync(): Boolean = mutex.withLock { try {
        val connection = container.connectionRepository.get(ServiceKind.JELLYFIN)
        val fingerprint = MediaSnapshotStore.fingerprint(listOf(connection))
        val active = container.preferencesRepository.personalization.watchNextEnabled && connection.token.isNotBlank()
        if (!active && cleanedWhileInactive) return@withLock true
        if (active) cleanedWhileInactive = false
        val cached = if (active) container.mediaSnapshotStore.read(container.mediaFingerprint(container.connectionRepository.list()))?.resume.orEmpty() else emptyList()
        val rows = if (active) container.localPlaybackStore.merge(connection, cached).filter {
            it.source == ServiceKind.JELLYFIN && it.mediaType in listOf("Movie", "Episode") &&
                it.remoteId?.matches(Regex("[A-Za-z0-9_-]{1,128}")) == true && !it.played &&
                (it.progress ?: 0f) > 0f && (it.progress ?: 0f) < .95f &&
                (it.runtimeMinutes ?: 0) * 60_000L * (it.progress ?: 0f) >= 60_000
        }.take(12) else emptyList()
        val desired = rows.associateBy { "$fingerprint-${it.remoteId}" }
        val existing = mutableMapOf<String, Pair<Long, Boolean>>()
        val resolver = context.contentResolver
        resolver.query(Programs.CONTENT_URI, arrayOf("_id", Programs.COLUMN_INTERNAL_PROVIDER_ID, Programs.COLUMN_PACKAGE_NAME, Programs.COLUMN_BROWSABLE), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(2) != context.packageName) continue
                val id = cursor.getLong(0)
                val key = cursor.getString(1).orEmpty()
                if (key !in desired) resolver.delete(ContentUris.withAppendedId(Programs.CONTENT_URI, id), null, null)
                else existing[key] = id to (cursor.getInt(3) == 1)
            }
        }
        val directory = context.cacheDir.resolve("watch-next-art").apply { mkdirs() }
        directory.listFiles()?.filter { it.name.removeSuffix(".jpg") !in desired }?.forEach { it.delete() }
        for ((key, media) in desired) {
            if (existing[key]?.second == false) continue // Respect removal in the launcher.
            if (MediaSnapshotStore.fingerprint(listOf(container.connectionRepository.get(ServiceKind.JELLYFIN))) != fingerprint ||
                !container.preferencesRepository.personalization.watchNextEnabled) break
            val file = directory.resolve("$key.jpg")
            if (!file.exists() && media.artworkUrl != null) {
                runCatching {
                    withTimeout(3_000) {
                        val request = ImageRequest.Builder(context).data(media.artworkUrl).size(480, 270).allowHardware(false)
                        app.reelstack.ui.components.MediaAuthHeaders.forUrl(context, media.source, media.artworkUrl)?.let(request::httpHeaders)
                        val image = (context.imageLoader.execute(request.build()) as? coil3.request.SuccessResult)?.image as? coil3.BitmapImage
                        image?.bitmap?.let { bitmap -> file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) } }
                    }
                }
            }
            val uri = Uri.Builder().scheme("spole").authority("watch-next").appendPath(fingerprint).appendPath(media.remoteId).build()
            if (MediaSnapshotStore.fingerprint(listOf(container.connectionRepository.get(ServiceKind.JELLYFIN))) != fingerprint ||
                !container.preferencesRepository.personalization.watchNextEnabled) break
            val intent = Intent(context, WatchNextActivity::class.java).setAction(Intent.ACTION_VIEW).setData(uri)
            val duration = (media.runtimeMinutes ?: 0) * 60_000L
            val values = ContentValues().apply {
                put(Programs.COLUMN_INTERNAL_PROVIDER_ID, key)
                put(Programs.COLUMN_TITLE, media.title)
                put(Programs.COLUMN_SHORT_DESCRIPTION, media.subtitle)
                put(Programs.COLUMN_TYPE, if (media.mediaType == "Movie") Programs.TYPE_MOVIE else Programs.TYPE_TV_EPISODE)
                put(Programs.COLUMN_WATCH_NEXT_TYPE, Programs.WATCH_NEXT_TYPE_CONTINUE)
                put(Programs.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, media.lastActivityEpochMillis ?: System.currentTimeMillis())
                put(Programs.COLUMN_DURATION_MILLIS, duration)
                put(Programs.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, (duration * (media.progress ?: 0f)).toLong())
                put(Programs.COLUMN_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME))
                put(Programs.COLUMN_POSTER_ART_URI, if (file.exists()) "content://${context.packageName}.watchart/$key" else "android.resource://${context.packageName}/${app.reelstack.R.drawable.media_placeholder}")
            }
            val previous = existing[key]
            if (previous == null) resolver.insert(Programs.CONTENT_URI, values)
            else resolver.update(ContentUris.withAppendedId(Programs.CONTENT_URI, previous.first), values, null, null)
        }
        if (!active) cleanedWhileInactive = true
        true
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { false } } // An unsupported launcher must never break playback.
}

class WatchNextActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as app.reelstack.ReelstackApplication).container
        val connection = container.connectionRepository.get(ServiceKind.JELLYFIN)
        val uri = intent.data
        val parts = uri?.pathSegments.orEmpty()
        if (container.preferencesRepository.personalization.watchNextEnabled && connection.token.isNotBlank() &&
            uri?.scheme == "spole" && uri.host == "watch-next" && parts.size == 2 &&
            parts[0] == MediaSnapshotStore.fingerprint(listOf(connection)) && parts[1].matches(Regex("[A-Za-z0-9_-]{1,128}"))) {
            JellyfinPlayerActivity.open(this, parts[1])
        }
        finish()
    }
}

/** Exposes only opted-in artwork, never account files or credential-bearing server URLs. */
class WatchNextArtworkProvider : android.content.ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/jpeg"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): android.database.Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun openFile(uri: Uri, mode: String): android.os.ParcelFileDescriptor {
        val context = requireNotNull(context)
        val container = (context.applicationContext as app.reelstack.ReelstackApplication).container
        val key = uri.lastPathSegment.orEmpty()
        val connection = container.connectionRepository.get(ServiceKind.JELLYFIN)
        if (mode != "r" || uri.pathSegments.size != 1 || !key.matches(Regex("[a-f0-9]{64}-[A-Za-z0-9_-]{1,128}")) ||
            !container.preferencesRepository.personalization.watchNextEnabled || connection.token.isBlank() ||
            !key.startsWith(MediaSnapshotStore.fingerprint(listOf(connection)) + "-")) throw java.io.FileNotFoundException()
        return android.os.ParcelFileDescriptor.open(context.cacheDir.resolve("watch-next-art/$key.jpg"), android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
