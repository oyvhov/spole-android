package app.reelstack.player

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import java.time.Instant

/** Compare server values, never the clocks of two devices. Missing data is not position zero. */
data class PlaybackUserState(
    val positionTicks: Long,
    val played: Boolean,
    val playCount: Int? = null,
    val lastPlayed: String? = null,
) {
    fun toJson() = buildJsonObject {
        put("PlaybackPositionTicks", positionTicks); put("Played", played)
        playCount?.let { put("PlayCount", it) }; lastPlayed?.let { put("LastPlayedDate", it) }
    }
    companion object {
        fun parse(data: JsonObject): PlaybackUserState? {
            val position = (data["PlaybackPositionTicks"] as? JsonPrimitive)?.longOrNull ?: return null
            val played = (data["Played"] as? JsonPrimitive)?.booleanOrNull ?: return null
            if (position < 0) return null
            val date = (data["LastPlayedDate"] as? JsonPrimitive)?.contentOrNull
            return PlaybackUserState(position, played, (data["PlayCount"] as? JsonPrimitive)?.intOrNull,
                date?.let { runCatching { Instant.parse(it).toString() }.getOrDefault(it) })
        }
    }
}

data class PendingPlaybackProgress(
    val id: String,
    val scope: String,
    val itemId: String,
    val positionMs: Long,
    /** Only an actual end event marks an item watched; server resume thresholds are unknown. */
    val completed: Boolean,
    val baseline: PlaybackUserState,
    val recordedAt: Long,
    val journalId: String,
    val blocked: Boolean = false,
) {
    val positionTicks: Long get() = if (completed) 0 else positionMs.coerceIn(0, Long.MAX_VALUE / 10_000) * 10_000
    fun matches(state: PlaybackUserState) = state.positionTicks == positionTicks &&
        state.played == (completed || baseline.played)
}

interface PlaybackProgressQueue {
    fun pending(scope: String): List<PendingPlaybackProgress>
    fun current(entry: PendingPlaybackProgress): Boolean
    fun remove(entry: PendingPlaybackProgress)
    fun block(entry: PendingPlaybackProgress)
}

data class RemotePlaybackProgress(val state: PlaybackUserState, val data: JsonObject)

interface PlaybackProgressRemote {
    fun read(connection: ServiceConnection, itemId: String): RemotePlaybackProgress
    /** False means this server cannot accept the write. Never fall back to a session report. */
    fun write(connection: ServiceConnection, entry: PendingPlaybackProgress, remote: RemotePlaybackProgress): Boolean
}

private class PlaybackProgressUnavailable : java.io.IOException()

/** No saved tokens, session IDs, stream URLs or invented playback sessions in the retry path. */
class PlaybackProgressClient(
    private val deviceId: String,
    private val transport: JsonHttpTransport = HttpTransport(),
) : PlaybackProgressRemote {
    private fun headers(c: ServiceConnection) = MediaPlaybackClient(deviceId = deviceId).headers(c)
    override fun read(connection: ServiceConnection, itemId: String): RemotePlaybackProgress {
        require(connection.userId.isNotBlank())
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl,
            "Users/${enc(connection.userId)}/Items/${enc(itemId)}"), headers(connection))
        if (response.statusCode in setOf(401, 403, 404)) throw PlaybackProgressUnavailable()
        response.requireSuccess(connection.kind)
        val data = Json.parseToJsonElement(response.body).jsonObject["UserData"] as? JsonObject
            ?: throw java.io.IOException("Missing playback state")
        return RemotePlaybackProgress(PlaybackUserState.parse(data)
            ?: throw java.io.IOException("Incomplete playback state"), data)
    }

    override fun write(connection: ServiceConnection, entry: PendingPlaybackProgress, remote: RemotePlaybackProgress): Boolean {
        require(connection.userId.isNotBlank())
        val path = when (connection.kind) {
            ServiceKind.JELLYFIN -> "UserItems/${enc(entry.itemId)}/UserData?userId=${enc(connection.userId)}"
            ServiceKind.EMBY -> "Users/${enc(connection.userId)}/Items/${enc(entry.itemId)}/UserData"
            else -> return false
        }
        // Jellyfin's update DTO is a patch. Emby accepts a whole UserItemDataDto: retain the
        // fresh favourite, rating and counter fields instead of resetting them to defaults.
        if (connection.kind == ServiceKind.EMBY &&
            ((remote.data["IsFavorite"] as? JsonPrimitive)?.booleanOrNull == null || remote.state.playCount == null)) return false
        val payload = buildJsonObject {
            if (connection.kind == ServiceKind.EMBY) remote.data.forEach { (key, value) ->
                if (key !in setOf("PlayedPercentage", "UnplayedItemCount")) put(key, value)
            }
            put("PlaybackPositionTicks", entry.positionTicks)
            put("Played", entry.completed || remote.state.played)
            put("LastPlayedDate", Instant.ofEpochMilli(entry.recordedAt).toString())
        }
        val response = transport.post(EndpointValidator.resolve(connection.baseUrl, path), headers(connection), payload.toString())
        // Older servers/forbidden accounts retain the local checkpoint, without a retry storm.
        if (response.statusCode in setOf(400, 401, 403, 404, 405)) return false
        response.requireSuccess(connection.kind)
        return true
    }
}

enum class PlaybackSyncResult { SAVED, CONFLICT, BLOCKED, RETRY, CANCELLED }

/** A conservative read/compare/write. The server APIs do not offer atomic compare-and-swap. */
class PlaybackProgressSync(private val queue: PlaybackProgressQueue, private val remote: PlaybackProgressRemote) {
    fun sync(
        connection: ServiceConnection,
        entry: PendingPlaybackProgress,
        allowed: () -> Boolean,
        confirm: (PlaybackUserState) -> Unit,
    ): PlaybackSyncResult {
        fun current() = allowed() && queue.current(entry)
        fun finish(state: PlaybackUserState, result: PlaybackSyncResult): PlaybackSyncResult {
            if (!current()) return PlaybackSyncResult.CANCELLED
            confirm(state)
            if (!current()) return PlaybackSyncResult.CANCELLED
            queue.remove(entry)
            return result
        }
        if (!current()) return PlaybackSyncResult.CANCELLED
        return try {
            val before = remote.read(connection, entry.itemId)
            if (!current()) return PlaybackSyncResult.CANCELLED
            // Also handles a POST whose response was lost: no counter increments or double writes.
            if (entry.matches(before.state)) return finish(before.state, PlaybackSyncResult.SAVED)
            if (before.state != entry.baseline) return finish(before.state, PlaybackSyncResult.CONFLICT)
            if (!remote.write(connection, entry, before)) {
                if (!current()) return PlaybackSyncResult.CANCELLED
                queue.block(entry)
                return PlaybackSyncResult.BLOCKED
            }
            if (!current()) return PlaybackSyncResult.CANCELLED
            val after = remote.read(connection, entry.itemId)
            // The canonical answer also covers server normalisation and another device's update.
            finish(after.state, if (entry.matches(after.state)) PlaybackSyncResult.SAVED else PlaybackSyncResult.CONFLICT)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: PlaybackProgressUnavailable) {
            if (!current()) PlaybackSyncResult.CANCELLED
            else { queue.block(entry); PlaybackSyncResult.BLOCKED }
        }
        catch (_: Exception) { PlaybackSyncResult.RETRY }
    }
}
