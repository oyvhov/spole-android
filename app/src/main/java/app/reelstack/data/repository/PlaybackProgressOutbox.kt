package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.ServiceConnection
import app.reelstack.player.PendingPlaybackProgress
import app.reelstack.player.PlaybackProgressQueue
import app.reelstack.player.PlaybackUserState
import kotlinx.serialization.json.*
import java.security.MessageDigest

/** Small durable outbox, separate from disposable metadata caches. Call only on Dispatchers.IO. */
class PlaybackProgressOutbox(context: Context, private val now: () -> Long = System::currentTimeMillis) : PlaybackProgressQueue {
    private val prefs = context.getSharedPreferences("playback_progress_outbox", Context.MODE_PRIVATE)

    fun put(entry: PendingPlaybackProgress) = synchronized(lock) {
        save((rows().filterNot { it.scope == entry.scope && it.itemId == entry.itemId } + entry).takeLast(MAX_ENTRIES))
    }
    override fun pending(scope: String): List<PendingPlaybackProgress> = synchronized(lock) {
        rows().filter { it.scope == scope && !it.blocked }
    }
    override fun current(entry: PendingPlaybackProgress): Boolean = synchronized(lock) {
        rows().any { it.id == entry.id && it.scope == entry.scope && it.itemId == entry.itemId && !it.blocked }
    }
    override fun remove(entry: PendingPlaybackProgress) = synchronized(lock) {
        save(rows().filterNot { it.id == entry.id && it.scope == entry.scope && it.itemId == entry.itemId })
    }
    override fun block(entry: PendingPlaybackProgress) = synchronized(lock) {
        save(rows().map { if (it.id == entry.id && it.scope == entry.scope && it.itemId == entry.itemId) it.copy(blocked = true) else it })
    }
    /** The newest checkpoint for an item, also one the server refused: it is still the local truth. */
    fun latest(scope: String, itemId: String): PendingPlaybackProgress? = synchronized(lock) {
        rows().lastOrNull { it.scope == scope && it.itemId == itemId }
    }
    fun forgetScopes(scopes: Collection<String>) = synchronized(lock) {
        if (scopes.isNotEmpty()) prefs.edit { putString("entries", encode(rows().filterNot { it.scope in scopes })) }
    }
    fun forget(scope: String, itemId: String) = synchronized(lock) {
        prefs.edit { putString("entries", encode(rows().filterNot { it.scope == scope && it.itemId == itemId })) }
    }
    // UI invalidations are visible synchronously; Android drains apply writes at lifecycle stops.
    fun clear() = synchronized(lock) { prefs.edit { clear() } }

    private fun rows(): List<PendingPlaybackProgress> = runCatching {
        Json.parseToJsonElement(prefs.getString("entries", "[]") ?: "[]").jsonArray
    }.getOrNull().orEmpty().mapNotNull { value ->
        val row = value as? JsonObject ?: return@mapNotNull null
        fun text(key: String) = (row[key] as? JsonPrimitive)?.contentOrNull
        val baseline = (row["baseline"] as? JsonObject)?.let(PlaybackUserState::parse) ?: return@mapNotNull null
        val position = text("positionMs")?.toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
        val at = text("recordedAt")?.toLongOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
        val completed = (row["completed"] as? JsonPrimitive)?.booleanOrNull ?: return@mapNotNull null
        PendingPlaybackProgress(text("id") ?: return@mapNotNull null, text("scope") ?: return@mapNotNull null,
            text("itemId") ?: return@mapNotNull null, position, completed, baseline, at,
            text("journalId") ?: return@mapNotNull null, (row["blocked"] as? JsonPrimitive)?.booleanOrNull ?: false)
    }.filter { now() - it.recordedAt <= RETENTION_MS }.takeLast(MAX_ENTRIES)

    private fun encode(entries: List<PendingPlaybackProgress>) = JsonArray(entries.map { entry -> buildJsonObject {
        put("id", entry.id); put("scope", entry.scope); put("itemId", entry.itemId)
        put("positionMs", entry.positionMs); put("completed", entry.completed); put("baseline", entry.baseline.toJson())
        put("recordedAt", entry.recordedAt); put("journalId", entry.journalId); put("blocked", entry.blocked)
    } }).toString()

    @android.annotation.SuppressLint("UseKtx") // KTX edit(commit = true) discards the success flag we must check.
    private fun save(entries: List<PendingPlaybackProgress>) {
        // A failed disk write must not be represented as durable, and a POST never precedes this.
        check(prefs.edit().putString("entries", encode(entries)).commit()) { "Could not save playback checkpoints" }
    }

    companion object {
        const val MAX_ENTRIES = 64
        const val RETENTION_MS = 30L * 24 * 60 * 60_000
        // Workers and UI containers can have different store instances in the same process.
        private val lock = Any()
        fun scope(profileId: String, connection: ServiceConnection): String = MessageDigest.getInstance("SHA-256")
            .digest((profileId.length.toString() + ":" + profileId + MediaSnapshotStore.fingerprint(listOf(connection)))
                .toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
