package app.reelstack.offline

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import java.io.File
import java.security.MessageDigest

/** Only a server-confirmed, complete direct file may enter offline storage. */
data class OfflineMediaCandidate(
    val itemId: String,
    val service: ServiceKind,
    val isLive: Boolean = false,
    val isDiscImage: Boolean = false,
    val isDrmProtected: Boolean = false,
    val requiresTranscode: Boolean = false,
    val completeFile: Boolean = true,
    val directDownloadUrl: String = "",
)

enum class OfflineRefusal {
    LIVE_TV, DISC_IMAGE, DRM, TRANSCODE_REQUIRED, INCOMPLETE_FILE, UNSUPPORTED_SERVICE, MISSING_DIRECT_FILE,
}

fun OfflineMediaCandidate.offlineRefusal(): OfflineRefusal? = when {
    service !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) -> OfflineRefusal.UNSUPPORTED_SERVICE
    isLive -> OfflineRefusal.LIVE_TV
    isDiscImage -> OfflineRefusal.DISC_IMAGE
    isDrmProtected -> OfflineRefusal.DRM
    requiresTranscode -> OfflineRefusal.TRANSCODE_REQUIRED
    !completeFile -> OfflineRefusal.INCOMPLETE_FILE
    directDownloadUrl.isBlank() -> OfflineRefusal.MISSING_DIRECT_FILE
    else -> null
}

data class OfflineRecord(
    val profileKey: String,
    val serviceKey: String,
    val itemId: String,
    val fileName: String,
    val bytes: Long,
)

/**
 * Owns the private directory shape and nothing about network playback. Account identity is hashed
 * before it reaches a path; neither a token nor a clear server address is written to disk here.
 */
class OfflineStorage(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "offline")
    private val preferences = appContext.getSharedPreferences("reelstack_offline", Context.MODE_PRIVATE)

    fun destination(profileId: String, connection: ServiceConnection, itemId: String, extension: String): File {
        require(itemId.isNotBlank() && itemId.length <= 512)
        require(connection.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
        val profile = digest(profileId.ifBlank { "adult" })
        val service = digest("${connection.kind.name}|${connection.identity}|${connection.userId}")
        val safeExtension = extension.lowercase().removePrefix(".").filter(Char::isLetterOrDigit).take(8)
        val directory = File(File(root, profile), service)
        return File(directory, "${digest(itemId)}${safeExtension.takeIf(String::isNotBlank)?.let { ".$it" }.orEmpty()}")
    }

    fun register(profileId: String, connection: ServiceConnection, itemId: String, file: File, bytes: Long) {
        val expected = destination(profileId, connection, itemId, file.extension)
        require(samePath(expected, file)) { "offline file escaped private scope" }
        preferences.edit { putString(recordKey(profileId, connection, itemId), "${file.name}|${bytes.coerceAtLeast(0)}") }
    }

    fun remove(profileId: String, connection: ServiceConnection, itemId: String) {
        val file = destination(profileId, connection, itemId, "")
        file.parentFile?.listFiles()?.filter { it.nameWithoutExtension == file.name }
            ?.forEach(::deleteInsideRoot)
        preferences.edit { remove(recordKey(profileId, connection, itemId)) }
    }

    fun clearForProfile(profileId: String) {
        deleteInsideRoot(File(root, digest(profileId.ifBlank { "adult" })))
        preferences.edit { all.keys.filter { it.startsWith("offline.${digest(profileId.ifBlank { "adult" })}.") }.forEach(::remove) }
    }

    fun clearForConnection(profileId: String, connection: ServiceConnection) {
        val profile = digest(profileId.ifBlank { "adult" })
        val service = digest("${connection.kind.name}|${connection.identity}|${connection.userId}")
        deleteInsideRoot(File(File(root, profile), service))
        preferences.edit { all.keys.filter { it.startsWith("offline.$profile.$service.") }.forEach(::remove) }
    }

    fun clearAll() {
        deleteInsideRoot(root)
        preferences.edit { clear() }
    }

    private fun recordKey(profileId: String, connection: ServiceConnection, itemId: String): String =
        "offline.${digest(profileId.ifBlank { "adult" })}.${digest("${connection.kind.name}|${connection.identity}|${connection.userId}")}.${digest(itemId)}"

    private fun deleteInsideRoot(target: File) {
        if (!target.exists()) return
        check(target.canonicalPath.startsWith(root.canonicalPath + File.separator) || target.canonicalPath == root.canonicalPath) {
            "refused to delete outside offline storage"
        }
        target.deleteRecursively()
    }

    private fun samePath(first: File, second: File): Boolean = first.canonicalPath == second.canonicalPath
    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
