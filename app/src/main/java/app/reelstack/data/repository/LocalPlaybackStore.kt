package app.reelstack.data.repository

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.*
import app.reelstack.player.PlayableItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*

/** A small account-scoped journal. Server latency must not erase progress from this device. */
class LocalPlaybackStore(context: Context) {
    /** Do not turn the first play event at position zero into a visible local resume item. */
    private val minimumResumePositionMs = 10_000L
    private val prefs = context.getSharedPreferences("local_playback", Context.MODE_PRIVATE)
    private val revision = MutableStateFlow(0L)
    val changes = revision.asStateFlow()
    private fun key(c: ServiceConnection) = MediaSnapshotStore.fingerprint(listOf(c))

    @Synchronized fun record(c: ServiceConnection, item: PlayableItem, position: Long, duration: Long, completed: Boolean) {
        if (c.token.isBlank() || item.id.isBlank() || duration <= 0) return
        if (!completed && position < minimumResumePositionMs) return
        val now = System.currentTimeMillis()
        val entry = buildJsonObject {
            put("id", item.id); put("title", item.title); put("subtitle", item.subtitle); put("type", item.type)
            put("art", item.artworkUrl); put("logo", item.logoUrl); put("series", item.seriesId)
            put("season", item.season); put("episode", item.episode)
            put("position", position.coerceIn(0, duration)); put("duration", duration)
            put("updated", now); put("completed", completed)
        }
        val rows = (listOf(entry) + entries(c).filter { it.text("id") != item.id }).take(20)
        // apply updates the in-process value immediately and serialises disk writes in order.
        prefs.edit().putString(key(c), JsonArray(rows).toString()).apply()
        revision.value++
    }

    @Synchronized private fun entries(c: ServiceConnection): List<JsonObject> {
        if (c.token.isBlank()) return emptyList()
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60_000L
        return runCatching { Json.parseToJsonElement(prefs.getString(key(c), "[]")!!).jsonArray
            .mapNotNull { it as? JsonObject }.filter { it.number("updated") >= cutoff } }.getOrDefault(emptyList())
    }

    fun resume(c: ServiceConnection, item: PlayableItem): PlayableItem {
        val row = entries(c).firstOrNull { it.text("id") == item.id } ?: return item
        if ((item.lastPlayedEpochMillis ?: 0) > row.number("updated")) return item
        return item.copy(resumeMs = if (row.done()) 0 else row.number("position"), played = row.done())
    }

    fun merge(c: ServiceConnection, server: List<LibraryMedia>): List<LibraryMedia> {
        val result = server.toMutableList()
        for (row in entries(c)) {
            val id = "${c.kind.name.lowercase(java.util.Locale.ROOT)}-${row.text("id")}"
            val existing = result.firstOrNull { it.source == c.kind && it.id == id }
            if ((existing?.lastActivityEpochMillis ?: 0) > row.number("updated")) continue
            result.removeAll { it.source == c.kind && it.id == id }
            if (row.done()) continue
            val media = existing ?: LibraryMedia(id, row.text("title").orEmpty(), row.text("subtitle").orEmpty(),
                artworkRes = R.drawable.media_placeholder, source = c.kind, remoteId = row.text("id"),
                artworkUrl = row.text("art"), logoUrl = row.text("logo"), mediaType = row.text("type").orEmpty(),
                seriesId = row.text("series"), season = row.text("season")?.toIntOrNull(), episode = row.text("episode")?.toIntOrNull())
            result += media.copy(progress = (row.number("position").toFloat() / row.number("duration").coerceAtLeast(1)).coerceIn(0f, 1f),
                runtimeMinutes = (row.number("duration") / 60_000).toInt(), played = false,
                lastActivityEpochMillis = row.number("updated"))
        }
        return result.sortedByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE }
    }

    fun nextUp(c: ServiceConnection, server: List<LibraryMedia>): List<LibraryMedia> {
        val recent = entries(c).associateBy { "${c.kind.name.lowercase(java.util.Locale.ROOT)}-${it.text("id")}" }
        return server.filter { item ->
            val row = recent[item.id]
            item.source != c.kind || row == null || (item.lastActivityEpochMillis ?: 0) > row.number("updated")
        }
    }

    @Synchronized fun clear() { prefs.edit().clear().apply(); revision.value++ }
    @Synchronized fun forget(c: ServiceConnection, id: String) {
        prefs.edit().putString(key(c), JsonArray(entries(c).filter { it.text("id") != id }).toString()).apply()
        revision.value++
    }
    private fun JsonObject.text(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.number(key: String) = text(key)?.toLongOrNull() ?: 0L
    private fun JsonObject.done() = text("completed") == "true"
}
