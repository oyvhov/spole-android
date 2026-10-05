package app.reelstack.data.repository

import android.content.Context
import app.reelstack.data.model.*
import app.reelstack.data.network.EndpointValidator
import kotlinx.serialization.json.*
import java.security.MessageDigest

/** Calendar interest is local and does not submit requests or change a server watchlist. */
class CalendarFollowStore(context: Context) {
    private val preferences = context.getSharedPreferences("calendar_follows", Context.MODE_PRIVATE)
    val revision: Long get() = preferences.getLong("revision", 0)

    fun scope(profileId: String, connection: ServiceConnection, userId: String): String {
        require(connection.kind == ServiceKind.SEERR && userId.isNotBlank())
        return MessageDigest.getInstance("SHA-256").digest(
            "$profileId|${EndpointValidator.normalizeBaseUrl(connection.identity)}|$userId".toByteArray()
        ).joinToString("") { "%02x".format(it) }
    }

    fun read(scope: String): CalendarSelection = CalendarSelection(runCatching {
        Json.parseToJsonElement(preferences.getString(scope, "[]")!!).jsonArray.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item["id"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
            val type = item["type"]?.jsonPrimitive?.content?.takeIf { it == "movie" || it == "tv" } ?: return@mapNotNull null
            CalendarTitle(id, type, item["title"]?.jsonPrimitive?.content.orEmpty(),
                item["art"]?.jsonPrimitive?.contentOrNull, item["hidden"]?.jsonPrimitive?.booleanOrNull == true)
        }
    }.getOrDefault(emptyList()))

    @Synchronized fun set(scope: String, title: CalendarTitle, followed: Boolean) {
        require(title.tmdbId > 0 && title.mediaType in setOf("movie", "tv"))
        val entries = read(scope).titles.filterNot { it.key == title.key } + title.copy(hidden = !followed)
        val encoded = JsonArray(entries.map { entry -> buildJsonObject {
            put("id", entry.tmdbId); put("type", entry.mediaType); put("title", entry.title)
            entry.artworkUrl?.let { put("art", it) }; put("hidden", entry.hidden)
        } }).toString()
        check(preferences.edit().putLong("revision", revision + 1).putString(scope, encoded).commit())
    }
}
