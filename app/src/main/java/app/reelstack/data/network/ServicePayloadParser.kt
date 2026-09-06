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
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
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
    val artworkItemId: String?,
    val artworkUrl: String? = null,
)

data class RemoteLibraryItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float?,
    val mediaType: String,
    val artworkItemId: String?,
    val artworkImageType: String = "Primary",
    val artworkUrl: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
)

data class RemoteLibraryView(
    val id: String,
    val name: String,
    val collectionType: String?,
)

data class RemoteQueueItem(
    val id: String,
    val title: String,
    val source: ServiceKind,
    val status: String,
    val state: IncomingState,
    val progress: Int?,
    val artworkUrl: String?,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
)

data class RemoteUpcomingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateTime: String,
    val source: ServiceKind,
    val artworkUrl: String?,
    val mediaType: String,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
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
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val seerrStatus: Int? = null,
)

data class RemoteRecommendationItem(
    val id: String,
    val remoteId: Int,
    val mediaType: String,
    val title: String,
    val metadata: String,
    val artworkUrl: String?,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
)

data class RemoteMediaDetails(
    val title: String?,
    val artworkUrl: String?,
    val tagline: String? = null,
    val overview: String? = null,
    val facts: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val cast: List<app.reelstack.data.model.CastMember> = emptyList(),
    val seerrStatus: Int? = null,
    val seasons: List<app.reelstack.data.model.RequestSeason> = emptyList(),
    val downloads: List<app.reelstack.data.model.RequestDownload> = emptyList(),
    val requestStatus: Int? = null,
    val status4k: Int? = null,
    val seasons4k: List<app.reelstack.data.model.RequestSeason> = emptyList(),
    val downloads4k: List<app.reelstack.data.model.RequestDownload> = emptyList(),
)

data class RemoteRequest(
    val id: Int,
    val remoteId: Int?,
    val mediaType: String,
    val status: Int,
    val requestedBy: String,
    val createdAt: String?,
    val title: String?,
    val artworkUrl: String?,
    val ownerId: String? = null,
    val seasons: Set<Int> = emptySet(),
    val mediaStatus: Int? = null,
    val downloads: List<app.reelstack.data.model.RequestDownload> = emptyList(),
    val availableSeasons: List<app.reelstack.data.model.RequestSeason> = emptyList(),
    val is4k: Boolean = false,
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

    fun availableUserIds(payload: String): List<String> {
        val root = json.parseToJsonElement(payload)
        val users = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("Items").takeIf { it.isNotEmpty() } ?: root.array("items")
            else -> JsonArray(emptyList())
        }
        return users.mapIndexedNotNull { index, element ->
            val user = element as? JsonObject ?: return@mapIndexedNotNull null
            val policy = user.obj("Policy") ?: user.obj("policy")
            if (policy?.bool("IsDisabled") == true || policy?.bool("isDisabled") == true) return@mapIndexedNotNull null
            val id = user.string("Id") ?: user.string("id") ?: return@mapIndexedNotNull null
            val isAdministrator = policy?.bool("IsAdministrator") == true || policy?.bool("isAdministrator") == true
            val hasAllFolders = policy?.bool("EnableAllFolders") == true || policy?.bool("enableAllFolders") == true
            PreferredUser(id, if (isAdministrator) 2 else if (hasAllFolders) 1 else 0, index)
        }.sortedWith(compareByDescending<PreferredUser> { it.accessScore }.thenBy { it.originalIndex })
            .map(PreferredUser::id)
    }

    fun libraryViews(payload: String): List<RemoteLibraryView> {
        val root = json.parseToJsonElement(payload)
        val views = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("Items").takeIf { it.isNotEmpty() } ?: root.array("items")
            else -> JsonArray(emptyList())
        }
        return views.mapNotNull { element ->
            val view = element as? JsonObject ?: return@mapNotNull null
            val id = view.string("Id") ?: view.string("id") ?: return@mapNotNull null
            val name = view.string("Name") ?: view.string("name") ?: return@mapNotNull null
            val collectionType = view.string("CollectionType") ?: view.string("collectionType")
            val isFolder = view.bool("IsFolder") ?: view.bool("isFolder") ?: (collectionType != null)
            if (!isFolder) return@mapNotNull null
            RemoteLibraryView(id, name, collectionType?.lowercase())
        }
    }

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
            val artwork = libraryArtwork(item, id, mediaType)
            RemoteLibraryItem(
                id = id,
                title = series ?: name,
                subtitle = listOfNotNull(episodeLabel, name.takeIf { series != null }, year?.toString().takeIf { series == null })
                    .distinct()
                    .joinToString(" · ")
                    .ifBlank {
                        when (mediaType.lowercase()) {
                            "movie" -> "Film"
                            "series" -> "Serie"
                            "episode" -> "Episode"
                            else -> "Video"
                        }
                    },
                progress = progress,
                mediaType = mediaType,
                artworkItemId = artwork.itemId,
                artworkImageType = artwork.imageType,
                overview = item.string("Overview") ?: item.string("overview"),
                facts = libraryFacts(item, mediaType, runtime),
                genres = stringArray(item, "Genres", "genres"),
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

    fun upcoming(payload: String, source: ServiceKind, notBefore: Instant? = null): List<RemoteUpcomingItem> {
        require(source == ServiceKind.RADARR || source == ServiceKind.SONARR)
        val root = json.parseToJsonElement(payload)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("records")
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { element ->
            val item = element.jsonObject
            if (source == ServiceKind.RADARR) {
                val id = item.int("id")?.toString() ?: return@mapNotNull null
                val title = item.string("title") ?: return@mapNotNull null
                val digitalRelease = item.string("digitalRelease")
                val physicalRelease = item.string("physicalRelease")
                val release = listOfNotNull(
                    digitalRelease?.let { it to "Digital utgjeving" },
                    physicalRelease?.let { it to "Fysisk utgjeving" },
                ).firstOrNull { (date, _) ->
                    notBefore == null || calendarInstant(date)?.let { !it.isBefore(notBefore) } == true
                } ?: return@mapNotNull null
                val (dateTime, availability) = release
                val year = item.int("year")
                RemoteUpcomingItem(
                    id = id,
                    title = title,
                    subtitle = listOfNotNull("Film", year?.toString()).joinToString(" · "),
                    dateTime = dateTime,
                    source = source,
                    artworkUrl = secureArtwork(item, "poster"),
                    mediaType = "Movie",
                    overview = item.string("overview"),
                    facts = listOfNotNull("Film", year?.toString(), item.int("runtime")?.let { "$it min" }, availability),
                    genres = stringArray(item, "genres"),
                )
            } else {
                val id = item.int("id")?.toString() ?: return@mapNotNull null
                val series = item.obj("series")
                val title = series?.string("title") ?: item.string("seriesTitle") ?: return@mapNotNull null
                val season = item.int("seasonNumber")
                val episode = item.int("episodeNumber")
                val episodeTitle = item.string("title")
                val episodeNumber = if (season != null && episode != null) {
                    "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
                } else null
                RemoteUpcomingItem(
                    id = id,
                    title = title,
                    subtitle = listOfNotNull(episodeNumber, episodeTitle).joinToString(" · ").ifBlank { "Episode" },
                    dateTime = item.string("airDateUtc") ?: item.string("airDate") ?: return@mapNotNull null,
                    source = source,
                    artworkUrl = series?.let { secureArtwork(it, "fanart") }
                        ?: secureArtwork(item, "fanart"),
                    mediaType = "Episode",
                    overview = series?.string("overview") ?: item.string("overview"),
                    facts = listOfNotNull(
                        "Serie",
                        series?.int("year")?.toString(),
                        series?.int("runtime")?.let { "$it min" },
                        series?.string("network"),
                    ),
                    genres = series?.let { stringArray(it, "genres") }.orEmpty(),
                )
            }
        }
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
                metadata = "${if (mediaType == "movie") "Film" else "Serie"}${year?.let { " · $it" }.orEmpty()}",
                artworkUrl = item.string("posterPath")?.let { safeTmdbArtwork(it) },
                inLibrary = mediaStatus == 5,
                requested = mediaStatus in 2..4,
                seerrStatus = mediaStatus,
                overview = item.string("overview"),
                facts = discoverFacts(item, mediaType, year),
                genres = objectNameArray(item, "genres"),
            )
        }
    }

    fun recommendations(payload: String): List<RemoteRecommendationItem> {
        val root = json.parseToJsonElement(payload)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("items")
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val remoteId = item.int("tmdbId") ?: item.int("remoteId") ?: return@mapNotNull null
            val mediaType = item.string("mediaType")?.lowercase() ?: return@mapNotNull null
            if (mediaType !in setOf("movie", "tv")) return@mapNotNull null
            val title = item.string("title") ?: item.string("name") ?: return@mapNotNull null
            val year = item.int("year")?.toString()
                ?: (item.string("releaseDate") ?: item.string("firstAirDate"))?.take(4)
            RemoteRecommendationItem(
                id = "github-recommendation-$mediaType-$remoteId",
                remoteId = remoteId,
                mediaType = mediaType,
                title = title,
                metadata = "${if (mediaType == "movie") "Film" else "Serie"}${year?.let { " · $it" }.orEmpty()}",
                artworkUrl = item.string("posterPath")?.let(::safeTmdbArtwork)
                    ?: item.string("artworkUrl")?.takeIf { it.startsWith("https://", ignoreCase = true) },
                overview = item.string("overview"),
                facts = discoverFacts(item, mediaType, year),
                genres = stringArray(item, "genres"),
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
            val is4k = request["is4k"]?.jsonPrimitive?.booleanOrNull == true
            val statusKey = if (is4k) "status4k" else "status"
            RemoteRequest(
                id = id,
                remoteId = media?.int("tmdbId"),
                mediaType = media?.string("mediaType") ?: "movie",
                status = request.int("status") ?: 1,
                requestedBy = user?.string("displayName") ?: user?.string("username") ?: "Nokon",
                ownerId = user?.int("id")?.toString(),
                is4k = is4k,
                seasons = request.array("seasons").mapNotNull { (it as? JsonObject)?.int("seasonNumber") }.toSet(),
                mediaStatus = media?.int(statusKey),
                downloads = downloadItems(media, if (is4k) "downloadStatus4k" else "downloadStatus"),
                availableSeasons = media?.array("seasons").orEmpty().mapNotNull {
                    val season = it as? JsonObject ?: return@mapNotNull null
                    val number = season.int("seasonNumber") ?: return@mapNotNull null
                    app.reelstack.data.model.RequestSeason(number, "Sesong $number", 0, season.int(statusKey) ?: 1)
                },
                createdAt = request.string("createdAt"),
                title = media?.string("title") ?: media?.string("name"),
                artworkUrl = media?.string("posterPath")?.let(::safeTmdbArtwork),
            )
        }
    }

    fun mediaDetails(payload: String): RemoteMediaDetails {
        val item = json.parseToJsonElement(payload) as? JsonObject ?: return RemoteMediaDetails(null, null)
        return RemoteMediaDetails(
            title = item.string("title") ?: item.string("name"),
            seerrStatus = item.obj("mediaInfo")?.int("status"),
            seasons = requestSeasons(item),
            downloads = downloadItems(item.obj("mediaInfo")),
            status4k = item.obj("mediaInfo")?.int("status4k"),
            seasons4k = requestSeasons(item, "status4k"),
            downloads4k = downloadItems(item.obj("mediaInfo"), "downloadStatus4k"),
            artworkUrl = item.string("posterPath")?.let(::safeTmdbArtwork),
            tagline = item.string("tagline"),
            overview = item.string("overview")?.takeIf { it.isNotBlank() }
                ?: item.string("Overview")?.takeIf { it.isNotBlank() },
            facts = discoverFacts(
                item = item,
                mediaType = if (item.string("title") != null) "movie" else "tv",
                year = (item.string("releaseDate") ?: item.string("firstAirDate") ?: item.string("PremiereDate"))?.take(4),
            ),
            genres = objectNameArray(item, "genres").ifEmpty { stringArray(item, "Genres", "genres") },
            cast = item.obj("credits")?.array("cast").orEmpty().mapNotNull {
                val person = it as? JsonObject ?: return@mapNotNull null
                val name = person.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                app.reelstack.data.model.CastMember(name, person.string("character"),
                    person.string("profilePath")?.let(::safeTmdbArtwork))
            }.distinctBy { it.name }.take(16),
        )
    }

    private fun requestSeasons(item: JsonObject, statusKey: String = "status"): List<app.reelstack.data.model.RequestSeason> {
        val info = item.obj("mediaInfo")
        val existing = info?.array("seasons").orEmpty().mapNotNull { it as? JsonObject }
            .associateBy { it.int("seasonNumber") }
        val requests = info?.array("requests").orEmpty().mapNotNull { it as? JsonObject }
        return item.array("seasons").mapNotNull {
            val season = it as? JsonObject ?: return@mapNotNull null
            val number = season.int("seasonNumber") ?: return@mapNotNull null
            if (number < 0) return@mapNotNull null
            val pending = requests.any { request -> (request["is4k"]?.jsonPrimitive?.booleanOrNull == true) == (statusKey == "status4k") && request.int("status") in setOf(1, 2) &&
                request.array("seasons").any { (it as? JsonObject)?.int("seasonNumber") == number } }
            val recorded = existing[number]?.int(statusKey) ?: 1
            val status = if (recorded in setOf(1, 7) && pending) 2 else recorded
            app.reelstack.data.model.RequestSeason(number,
                if (number == 0) "Spesialepisodar" else "Sesong $number", season.int("episodeCount") ?: 0, status)
        }.distinctBy { it.number }.sortedBy { it.number }
    }

    private fun downloadItems(info: JsonObject?, key: String = "downloadStatus"): List<app.reelstack.data.model.RequestDownload> =
        info?.array(key).orEmpty().mapNotNull {
            val item = it as? JsonObject ?: return@mapNotNull null
            app.reelstack.data.model.RequestDownload(item.obj("episode")?.int("seasonNumber"),
                item.string("status").orEmpty(),
                item["size"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0,
                item["sizeLeft"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0)
        }

    fun libraryDetails(payload: String): RemoteMediaDetails {
        val item = json.parseToJsonElement(payload) as? JsonObject ?: return RemoteMediaDetails(null, null)
        val runtime = item.long("RunTimeTicks") ?: item.long("runTimeTicks")
        val mediaType = item.string("Type") ?: item.string("type") ?: "Video"
        return RemoteMediaDetails(
            title = item.string("Name") ?: item.string("name"),
            artworkUrl = null,
            tagline = item.string("Tagline") ?: item.string("tagline")
                ?: item.array("Taglines").firstOrNull()?.jsonPrimitive?.contentOrNull,
            overview = item.string("Overview") ?: item.string("overview"),
            facts = libraryFacts(item, mediaType, runtime),
            genres = stringArray(item, "Genres", "genres"),
            cast = item.array("People").mapNotNull {
                val person = it as? JsonObject ?: return@mapNotNull null
                if (!person.string("Type").equals("Actor", ignoreCase = true)) return@mapNotNull null
                val name = person.string("Name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                app.reelstack.data.model.CastMember(name, person.string("Role"))
            }.distinctBy { it.name }.take(16),
        )
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
            item.string("Type") ?: "Spelar no"
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
            userName = session.string("UserName") ?: "Nokon",
            deviceName = session.string("DeviceName") ?: session.string("Client") ?: "Ukjend eining",
            title = title,
            subtitle = subtitle,
            progress = if (runtime > 0L) (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f) else 0f,
            timeLeft = if (remainingMinutes > 0) "$remainingMinutes min att" else "Snart ferdig",
            streamMethod = when {
                session.obj("TranscodingInfo") != null -> "Omkoding"
                playMethod.equals("Transcode", ignoreCase = true) -> "Omkoding"
                else -> "Direkteavspeling"
            },
            quality = when {
                width >= 3_840 -> "4K"
                width >= 1_920 -> "1080p"
                width > 0 -> "${width}p"
                else -> "Auto"
            },
            paused = playState.bool("IsPaused") ?: false,
            artworkItemId = item.string("SeriesId") ?: item.string("seriesId")
                ?: item.string("PrimaryImageItemId") ?: item.string("primaryImageItemId")
                ?: item.string("Id") ?: item.string("id"),
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
            IncomingState.READY -> "Klar for import"
            IncomingState.DOWNLOADING -> progress?.let { "Lastar ned $it %" } ?: "Lastar ned"
            IncomingState.REQUESTED -> "Ventar i kø"
        }
        val artwork = media?.let(::secureArtwork)
        return RemoteQueueItem(
            id = "${source.name.lowercase()}-$id",
            title = title,
            source = source,
            status = status,
            state = state,
            progress = progress,
            artworkUrl = artwork,
            overview = media?.string("overview"),
            facts = listOfNotNull(
                if (source == ServiceKind.RADARR) "Film" else "Serie",
                media?.int("year")?.toString(),
                progress?.let { "$it %" },
            ),
            genres = media?.let { stringArray(it, "genres") }.orEmpty(),
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

    private fun stringArray(item: JsonObject, vararg keys: String): List<String> = keys.firstNotNullOfOrNull { key ->
        (item[key] as? JsonArray)?.mapNotNull { value -> value.jsonPrimitive.contentOrNull }?.takeIf(List<String>::isNotEmpty)
    }.orEmpty()

    private fun objectNameArray(item: JsonObject, key: String): List<String> = item.array(key)
        .mapNotNull { value -> (value as? JsonObject)?.string("name") }

    private fun libraryFacts(item: JsonObject, mediaType: String, runtimeTicks: Long?): List<String> = buildList {
        add(
            when (mediaType.lowercase()) {
                "movie" -> "Film"
                "series" -> "Serie"
                "episode" -> "Episode"
                else -> mediaType
            },
        )
        if (mediaType.equals("episode", ignoreCase = true)) {
            val season = item.int("ParentIndexNumber") ?: item.int("parentIndexNumber")
            val episode = item.int("IndexNumber") ?: item.int("indexNumber")
            if (season != null && episode != null) {
                add("S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}")
            }
        }
        (item.int("ProductionYear") ?: item.int("productionYear"))?.let { add(it.toString()) }
        runtimeTicks?.takeIf { it > 0 }?.let { add("${it / TICKS_PER_MINUTE} min") }
        (item.string("OfficialRating") ?: item.string("officialRating"))?.let(::add)
        (item.double("CommunityRating") ?: item.double("communityRating"))?.let { add("★ ${"%.1f".format(it)}") }
        (item.string("Status") ?: item.string("status"))?.let(::add)
        val studios = (item.array("Studios").takeIf { it.isNotEmpty() } ?: item.array("studios"))
            .mapNotNull { studio ->
                (studio as? JsonObject)?.let { it.string("Name") ?: it.string("name") }
            }
        studios.take(2).takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let(::add)
    }

    private fun discoverFacts(item: JsonObject, mediaType: String, year: String?): List<String> = buildList {
        add(if (mediaType == "movie") "Film" else "Serie")
        year?.takeIf { it.length == 4 && it.all(Char::isDigit) }?.let(::add)
        val runtime = item.int("runtime") ?: item.array("episodeRunTime").firstOrNull()?.jsonPrimitive?.intOrNull
        runtime?.takeIf { it > 0 }?.let { add("$it min") }
        item.double("voteAverage")?.takeIf { it > 0 }?.let { add("★ ${"%.1f".format(it)}") }
        item.string("status")?.trim()?.takeIf { it.isNotEmpty() }?.let { add(seerrProductionStatus(it)) }
        if (mediaType == "tv") {
            (item.int("numberOfSeasons") ?: item.int("numberOfSeason"))?.takeIf { it > 0 }?.let {
                add(if (it == 1) "1 sesong" else "$it sesongar")
            }
            item.int("numberOfEpisodes")?.takeIf { it > 0 }?.let {
                add(if (it == 1) "1 episode" else "$it episodar")
            }
            objectNameArray(item, "networks").map(String::trim).filter(String::isNotEmpty)
                .distinct().takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let(::add)
        }
    }

    private fun seerrProductionStatus(status: String): String = when (status.lowercase()) {
        "returning series" -> "Held fram"
        "ended" -> "Avslutta"
        "canceled", "cancelled" -> "Kansellert"
        "in production" -> "Under produksjon"
        "post production" -> "Etterarbeid"
        "planned" -> "Planlagd"
        "pilot" -> "Pilotepisode"
        "released" -> "Utgjeven"
        "rumored", "rumoured" -> "Ryktast"
        else -> status
    }

    private fun safeTmdbArtwork(path: String): String? = when {
        path.startsWith("/", ignoreCase = true) -> "https://image.tmdb.org/t/p/w500$path"
        path.startsWith("https://image.tmdb.org/", ignoreCase = true) -> path
        else -> null
    }

    fun calendarInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(value.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }.getOrNull()

    private fun secureArtwork(item: JsonObject, preferredType: String = "poster"): String? {
        val images = item.array("images").mapNotNull { it as? JsonObject }
        val image = images.firstOrNull { it.string("coverType").equals(preferredType, ignoreCase = true) }
            ?: images.firstOrNull { candidate ->
                candidate.string("coverType") == "poster" || candidate.string("coverType") == "fanart"
            }
        return image
            ?.let { it.string("remoteUrl") ?: it.string("url") }
            ?.takeIf { url -> url.startsWith("https://", ignoreCase = true) }
    }

    private fun libraryArtwork(item: JsonObject, id: String, mediaType: String): LibraryArtwork {
        val imageTags = item.obj("ImageTags") ?: item.obj("imageTags")
        val hasOwnThumb = imageTags?.keys?.any { it.equals("Thumb", ignoreCase = true) } == true
        val isSeriesArtwork = mediaType.equals("episode", ignoreCase = true) ||
            mediaType.equals("series", ignoreCase = true)
        if (isSeriesArtwork) {
            if (hasOwnThumb) return LibraryArtwork(id, "Thumb")
            (item.string("ParentThumbItemId") ?: item.string("parentThumbItemId"))?.let {
                return LibraryArtwork(it, "Thumb")
            }
            val seriesId = item.string("SeriesId") ?: item.string("seriesId")
            val hasSeriesThumb = item.string("SeriesThumbImageTag") != null ||
                item.string("seriesThumbImageTag") != null ||
                item.string("ParentThumbImageTag") != null ||
                item.string("parentThumbImageTag") != null
            if (seriesId != null && hasSeriesThumb) return LibraryArtwork(seriesId, "Thumb")
        }
        return LibraryArtwork(
            itemId = item.string("SeriesId") ?: item.string("seriesId")
                ?: item.string("PrimaryImageItemId") ?: item.string("primaryImageItemId")
                ?: id,
            imageType = "Primary",
        )
    }

    private const val TICKS_PER_MINUTE = 600_000_000L

    private data class PreferredUser(
        val id: String,
        val accessScore: Int,
        val originalIndex: Int,
    )

    private data class LibraryArtwork(val itemId: String, val imageType: String)
}
