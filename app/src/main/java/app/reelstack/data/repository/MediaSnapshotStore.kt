package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

data class CachedMediaSnapshot(
    val sessions: List<PlaybackSession>,
    val continueWatching: List<LibraryMedia>,
    val recentlyAdded: List<LibraryMedia>,
    val upcoming: List<UpcomingMedia>,
    val incoming: List<IncomingMedia>,
    val discover: List<DiscoverMedia>,
    val activity: List<ActivityEvent>,
    val refreshedAtEpochMillis: Long,
)

class MediaSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_media_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun save(snapshot: MediaSyncSnapshot) {
        val payload = buildJsonObject {
            put("refreshedAt", snapshot.refreshedAt.toEpochMilli())
            put("sessions", buildJsonArray { snapshot.sessions.forEach { add(sessionJson(it)) } })
            put("continueWatching", libraryJson(snapshot.continueWatching))
            put("recentlyAdded", libraryJson(snapshot.recentlyAdded))
            put("upcoming", upcomingJson(snapshot.upcoming))
            put("incoming", incomingJson(snapshot.incoming))
            put("discover", discoverJson(snapshot.discover))
            put("activity", activityJson(snapshot.activity))
        }.toString()
        preferences.edit { putString(CACHE_KEY, payload) }
    }

    fun read(): CachedMediaSnapshot? = runCatching {
        val payload = preferences.getString(CACHE_KEY, null) ?: return null
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return null
        CachedMediaSnapshot(
            sessions = root.array("sessions").mapNotNull { (it as? JsonObject)?.let(::session) }
                .ifEmpty { listOfNotNull(root.obj("session")?.let(::session)) },
            continueWatching = root.array("continueWatching").mapIndexedNotNull(::libraryItem),
            recentlyAdded = root.array("recentlyAdded").mapIndexedNotNull(::libraryItem),
            upcoming = root.array("upcoming").mapIndexedNotNull(::upcomingItem),
            incoming = root.array("incoming").mapNotNull(::incomingItem),
            discover = root.array("discover").mapNotNull(::discoverItem),
            activity = root.array("activity").mapNotNull(::activityItem),
            refreshedAtEpochMillis = root.long("refreshedAt") ?: return null,
        )
    }.getOrNull()

    fun clear() {
        preferences.edit { remove(CACHE_KEY) }
    }

    private fun sessionJson(item: PlaybackSession) = buildJsonObject {
        put("userName", item.userName)
        put("deviceName", item.deviceName)
        put("title", item.title)
        put("subtitle", item.subtitle)
        put("progress", item.progress)
        put("timeLeft", item.timeLeft)
        put("streamMethod", item.streamMethod)
        put("quality", item.quality)
        put("paused", item.paused)
        item.sessionId?.let { put("sessionId", it) }
        item.source?.let { put("source", it.name) }
    }

    private fun session(item: JsonObject): PlaybackSession? {
        val userName = item.string("userName") ?: return null
        val title = item.string("title") ?: return null
        return PlaybackSession(
            userName = userName,
            deviceName = item.string("deviceName") ?: "Unknown device",
            title = title,
            subtitle = item.string("subtitle").orEmpty(),
            progress = item.float("progress") ?: 0f,
            timeLeft = item.string("timeLeft") ?: "",
            streamMethod = item.string("streamMethod") ?: "Auto",
            quality = item.string("quality") ?: "Auto",
            paused = item.bool("paused") ?: false,
            sessionId = item.string("sessionId"),
            source = item.enumValue<ServiceKind>("source"),
        )
    }

    private fun libraryJson(items: List<LibraryMedia>) = buildJsonArray {
        items.take(CACHE_ITEM_LIMIT).forEach { item ->
            add(buildJsonObject {
                put("id", item.id)
                put("title", item.title)
                put("subtitle", item.subtitle)
                item.progress?.let { put("progress", it) }
                put("source", item.source.name)
            })
        }
    }

    private fun libraryItem(index: Int, element: kotlinx.serialization.json.JsonElement): LibraryMedia? {
        val item = element as? JsonObject ?: return null
        return LibraryMedia(
            id = item.string("id") ?: return null,
            title = item.string("title") ?: return null,
            subtitle = item.string("subtitle").orEmpty(),
            progress = item.float("progress"),
            artworkRes = if (index % 2 == 0) R.drawable.session_still else R.drawable.kitchen_request,
            source = item.enumValue<ServiceKind>("source") ?: return null,
        )
    }

    private fun upcomingJson(items: List<UpcomingMedia>) = buildJsonArray {
        items.take(CACHE_ITEM_LIMIT).forEach { item ->
            add(buildJsonObject {
                put("id", item.id)
                put("title", item.title)
                put("subtitle", item.subtitle)
                put("dateLabel", item.dateLabel)
                put("airDate", item.airDateEpochMillis)
                put("source", item.source.name)
                item.artworkUrl?.let { put("artworkUrl", it) }
            })
        }
    }

    private fun upcomingItem(index: Int, element: kotlinx.serialization.json.JsonElement): UpcomingMedia? {
        val item = element as? JsonObject ?: return null
        val source = item.enumValue<ServiceKind>("source") ?: return null
        return UpcomingMedia(
            id = item.string("id") ?: return null,
            title = item.string("title") ?: return null,
            subtitle = item.string("subtitle").orEmpty(),
            dateLabel = item.string("dateLabel") ?: "Upcoming",
            airDateEpochMillis = item.long("airDate") ?: return null,
            artworkRes = if (source == ServiceKind.RADARR || index % 2 == 0) R.drawable.desert_arrival else R.drawable.kitchen_request,
            source = source,
            artworkUrl = item.string("artworkUrl"),
        )
    }

    private fun incomingJson(items: List<IncomingMedia>) = buildJsonArray {
        items.take(CACHE_ITEM_LIMIT).forEach { item ->
            add(buildJsonObject {
                put("id", item.id)
                put("title", item.title)
                put("source", item.source.name)
                put("status", item.status)
                put("state", item.state.name)
                item.artworkUrl?.let { put("artworkUrl", it) }
            })
        }
    }

    private fun incomingItem(element: kotlinx.serialization.json.JsonElement): IncomingMedia? {
        val item = element as? JsonObject ?: return null
        val source = item.enumValue<ServiceKind>("source") ?: return null
        return IncomingMedia(
            id = item.string("id") ?: return null,
            title = item.string("title") ?: return null,
            source = source,
            status = item.string("status") ?: "Queued",
            state = item.enumValue<IncomingState>("state") ?: IncomingState.REQUESTED,
            artworkRes = if (source == ServiceKind.RADARR) R.drawable.desert_arrival else R.drawable.kitchen_request,
            artworkUrl = item.string("artworkUrl"),
        )
    }

    private fun discoverJson(items: List<DiscoverMedia>) = buildJsonArray {
        items.take(CACHE_ITEM_LIMIT).forEach { item ->
            add(buildJsonObject {
                put("id", item.id)
                put("title", item.title)
                put("metadata", item.metadata)
                put("inLibrary", item.inLibrary)
                put("requested", item.requested)
                item.artworkUrl?.let { put("artworkUrl", it) }
                item.remoteId?.let { put("remoteId", it) }
                item.mediaType?.let { put("mediaType", it) }
            })
        }
    }

    private fun discoverItem(element: kotlinx.serialization.json.JsonElement): DiscoverMedia? {
        val item = element as? JsonObject ?: return null
        val mediaType = item.string("mediaType")
        return DiscoverMedia(
            id = item.string("id") ?: return null,
            title = item.string("title") ?: return null,
            metadata = item.string("metadata").orEmpty(),
            artworkRes = if (mediaType == "tv") R.drawable.kitchen_request else R.drawable.desert_arrival,
            inLibrary = item.bool("inLibrary") ?: false,
            requested = item.bool("requested") ?: false,
            artworkUrl = item.string("artworkUrl"),
            remoteId = item.int("remoteId"),
            mediaType = mediaType,
        )
    }

    private fun activityJson(items: List<ActivityEvent>) = buildJsonArray {
        items.take(CACHE_ITEM_LIMIT).forEach { item ->
            add(buildJsonObject {
                put("id", item.id)
                put("title", item.title)
                put("detail", item.detail)
                put("time", item.time)
                item.progress?.let { put("progress", it) }
                put("complete", item.complete)
                item.source?.let { put("source", it.name) }
                item.artworkUrl?.let { put("artworkUrl", it) }
            })
        }
    }

    private fun activityItem(element: kotlinx.serialization.json.JsonElement): ActivityEvent? {
        val item = element as? JsonObject ?: return null
        return ActivityEvent(
            id = item.string("id") ?: return null,
            title = item.string("title") ?: return null,
            detail = item.string("detail").orEmpty(),
            time = item.string("time") ?: "Recently",
            progress = item.int("progress"),
            complete = item.bool("complete") ?: false,
            source = item.enumValue<ServiceKind>("source"),
            artworkRes = when (item.enumValue<ServiceKind>("source")) {
                ServiceKind.RADARR -> R.drawable.desert_arrival
                ServiceKind.SONARR, ServiceKind.SEERR -> R.drawable.kitchen_request
                else -> R.drawable.session_still
            },
            artworkUrl = item.string("artworkUrl"),
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    private fun JsonObject.float(key: String): Float? = (this[key] as? JsonPrimitive)?.floatOrNull
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

    private inline fun <reified T : Enum<T>> JsonObject.enumValue(key: String): T? =
        string(key)?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    private companion object {
        const val CACHE_KEY = "dashboard_snapshot_v1"
        const val CACHE_ITEM_LIMIT = 30
    }
}
