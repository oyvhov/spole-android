package app.reelstack.data.network

import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.roundToInt

data class RemotePlayback(
    val sessionId: String,
    val userId: String?,
    val userName: String,
    val deviceName: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val timeLeft: String,
    val streamMethod: String,
    val quality: String,
    val paused: Boolean,
)

data class RemoteLibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float?,
    val mediaType: String,
)

data class RemoteQueueItem(
    val id: String,
    val title: String,
    val source: ServiceKind,
    val status: String,
    val state: IncomingState,
    val progress: Int?,
    val artworkUrl: String?,
)

data class RemoteDiscoverItem(
    val id: String,
    val remoteId: Int,
    val mediaType: String,
    val title: String,
    val metadata: String,
    val artworkUrl: String?,
    val inLibrary: Boolean,
    val requested: Boolean,
)

data class RemoteRequest(
    val id: Int,
    val remoteId: Int?,
    val mediaType: String,
    val status: Int,
    val requestedBy: String,
    val createdAt: String?,
)

object ServicePayloadParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
    }

    fun playbackSessions(payload: String): List<RemotePlayback> =
        json.parseToJsonElement(payload).asArray().mapNotNull(::playbackSession)

    fun currentUserId(payload: String): String? =
        (json.parseToJsonElement(payload) as? JsonObject)?.string("Id")
            ?: (json.parseToJsonElement(payload) as? JsonObject)?.string("id")

    fun libraryItems(payload: String): List<RemoteLibraryItem> {
        val root = json.parseToJsonElement(payload)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("Items").takeIf { it.isNotEmpty() } ?: root.array("items")
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { element ->
            val item = element.jsonObject
            val id = item.string("Id") ?: item.string("id") ?: return@mapNotNull null
            val name = item.string("Name") ?: item.string("name") ?: return@mapNotNull null
            val series = item.string("SeriesName") ?: item.string("seriesName")
            val season = item.int("ParentIndexNumber") ?: item.int("parentIndexNumber")
            val episode = item.int("IndexNumber") ?: item.int("indexNumber")
            val mediaType = item.string("Type") ?: item.string("type") ?: "Video"
            val userData = item.obj("UserData") ?: item.obj("userData")
            val playedPercent = userData?.double("PlayedPercentage") ?: userData?.double("playedPercentage")
            val position = userData?.long("PlaybackPositionTicks") ?: userData?.long("playbackPositionTicks")
            val runtime = item.long("RunTimeTicks") ?: item.long("runTimeTicks")
            val progress = when {
                playedPercent != null -> (playedPercent / 100.0).toFloat().coerceIn(0f, 1f)
                position != null && runtime != null && runtime > 0 -> (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f)
                else -> null
            }
            val episodeLabel = if (season != null && episode != null) {
                "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
            } else null
            val year = item.int("ProductionYear") ?: item.int("productionYear")
            RemoteLibraryItem(
                id = id,
                title = series ?: name,
                subtitle = listOfNotNull(episodeLabel, name.takeIf { series != null }, year?.toString().takeIf { series == null })
                    .distinct()
                    .joinToString(" · ")
                    .ifBlank { mediaType },
                progress = progress,
                mediaType = mediaType,
            )
        }
    }

    fun queue(payload: String, source: ServiceKind): List<RemoteQueueItem> {
        require(source == ServiceKind.RADARR || source == ServiceKind.SONARR)
        val root = json.parseToJsonElement(payload)
        val records = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("records")
            else -> JsonArray(emptyList())
        }
        return records.mapNotNull { queueItem(it.jsonObject, source) }
    }

    fun discover(payload: String): List<RemoteDiscoverItem> {
        val results = json.parseToJsonElement(payload).jsonObject.array("results")
        return results.mapNotNull { element ->
            val item = element.jsonObject
            val remoteId = item.int("id") ?: return@mapNotNull null
            val mediaType = item.string("mediaType") ?: if (item.string("title") != null) "movie" else "tv"
            if (mediaType != "movie" && mediaType != "tv") return@mapNotNull null
            val title = item.string("title") ?: item.string("name") ?: return@mapNotNull null
            val date = item.string("releaseDate") ?: item.string("firstAirDate")
            val year = date?.take(4)?.takeIf { it.all(Char::isDigit) }
            val mediaStatus = item.obj("mediaInfo")?.int("status") ?: 1
            RemoteDiscoverItem(
                id = "seerr-$mediaType-$remoteId",
                remoteId = remoteId,
                mediaType = mediaType,
                title = title,
                metadata = "${if (mediaType == "movie") "Movie" else "Series"}${year?.let { " · $it" }.orEmpty()}",
                artworkUrl = item.string("posterPath")?.let { safeTmdbArtwork(it) },
                inLibrary = mediaStatus == 5,
                requested = mediaStatus in 2..4,
            )
        }
    }

    fun requests(payload: String): List<RemoteRequest> {
        val results = json.parseToJsonElement(payload).jsonObject.array("results")
        return results.mapNotNull { element ->
            val request = element.jsonObject
            val id = request.int("id") ?: return@mapNotNull null
            val media = request.obj("media")
            val user = request.obj("requestedBy")
            RemoteRequest(
                id = id,
                remoteId = media?.int("tmdbId"),
                mediaType = media?.string("mediaType") ?: "movie",
                status = request.int("status") ?: 1,
                requestedBy = user?.string("displayName") ?: user?.string("username") ?: "Someone",
                createdAt = request.string("createdAt"),
            )
        }
    }

    private fun playbackSession(element: JsonElement): RemotePlayback? {
        val session = element.jsonObject
        val item = session.obj("NowPlayingItem") ?: return null
        val playState = session.obj("PlayState") ?: JsonObject(emptyMap())
        val title = item.string("SeriesName") ?: item.string("Name") ?: return null
        val episodeName = item.string("Name").takeIf { item.string("SeriesName") != null }
        val season = item.int("ParentIndexNumber")
        val episode = item.int("IndexNumber")
        val index = if (season != null && episode != null) {
            "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
        } else {
            item.string("Type") ?: "Now playing"
        }
        val subtitle = listOfNotNull(index, episodeName).distinct().joinToString(" · ")
        val position = playState.long("PositionTicks") ?: 0L
        val runtime = item.long("RunTimeTicks") ?: 0L
        val remainingMinutes = ((runtime - position).coerceAtLeast(0L) / TICKS_PER_MINUTE).toInt()
        val width = item.int("Width") ?: 0
        val playMethod = playState.string("PlayMethod")
        return RemotePlayback(
            sessionId = session.string("Id") ?: session.string("id") ?: return null,
            userId = session.string("UserId") ?: session.string("userId"),
            userName = session.string("UserName") ?: "Someone",
            deviceName = session.string("DeviceName") ?: session.string("Client") ?: "Unknown device",
            title = title,
            subtitle = subtitle,
            progress = if (runtime > 0L) (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f) else 0f,
            timeLeft = if (remainingMinutes > 0) "$remainingMinutes min left" else "Ending soon",
            streamMethod = when {
                session.obj("TranscodingInfo") != null -> "Transcoding"
                playMethod.equals("Transcode", ignoreCase = true) -> "Transcoding"
                else -> "Direct play"
            },
            quality = when {
                width >= 3_840 -> "4K"
                width >= 1_920 -> "1080p"
                width > 0 -> "${width}p"
                else -> "Auto"
            },
            paused = playState.bool("IsPaused") ?: false,
        )
    }

    private fun queueItem(item: JsonObject, source: ServiceKind): RemoteQueueItem? {
        val media = item.obj(if (source == ServiceKind.RADARR) "movie" else "series")
        val title = media?.string("title") ?: item.string("title") ?: return null
        val id = item.string("downloadId") ?: item.int("id")?.toString() ?: "$title-${item.hashCode()}"
        val size = item.long("size") ?: 0L
        val sizeLeft = item.long("sizeleft") ?: item.long("sizeLeft") ?: size
        val progress = if (size > 0L) (((size - sizeLeft).coerceAtLeast(0L).toDouble() / size) * 100).roundToInt().coerceIn(0, 100) else null
        val rawStatus = item.string("status").orEmpty().lowercase()
        val trackedState = item.string("trackedDownloadState").orEmpty().lowercase()
        val state = when {
            rawStatus == "completed" || trackedState == "importpending" -> IncomingState.READY
            rawStatus == "downloading" || rawStatus == "queued" -> IncomingState.DOWNLOADING
            else -> IncomingState.REQUESTED
        }
        val status = when (state) {
            IncomingState.READY -> "Ready to import"
            IncomingState.DOWNLOADING -> progress?.let { "Downloading $it%" } ?: "Downloading"
            IncomingState.REQUESTED -> rawStatus.replaceFirstChar(Char::uppercase).ifBlank { "Queued" }
        }
        val artwork = media?.array("images")
            ?.mapNotNull { it as? JsonObject }
            ?.firstOrNull { it.string("coverType") == "poster" }
            ?.let { it.string("remoteUrl") ?: it.string("url") }
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
        return RemoteQueueItem(
            id = "${source.name.lowercase()}-$id",
            title = title,
            source = source,
            status = status,
            state = state,
            progress = progress,
            artworkUrl = artwork,
        )
    }

    private fun JsonElement.asArray(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())
    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
    private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
    private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
    private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

    private fun safeTmdbArtwork(path: String): String? = when {
        path.startsWith("/", ignoreCase = true) -> "https://image.tmdb.org/t/p/w500$path"
        path.startsWith("https://image.tmdb.org/", ignoreCase = true) -> path
        else -> null
    }

    private const val TICKS_PER_MINUTE = 600_000_000L
}
