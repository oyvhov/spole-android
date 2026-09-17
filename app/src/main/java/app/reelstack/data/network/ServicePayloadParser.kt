package app.reelstack.data.network

import app.reelstack.R
import app.reelstack.localization.LocalizedText
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
    /** Minutes left, so the card can spell them in the reader's language. 0 means "almost done". */
    val remainingMinutes: Int,
    /** What the server is doing with the file, as a fact rather than as a word. */
    val transcoding: Boolean,
    val quality: String,
    val paused: Boolean,
    val artworkItemId: String?,
    val artworkUrl: String? = null,
    /** Carried as numbers so the screen can write them in the reader's own language. */
    val season: Int? = null,
    val episode: Int? = null,
    val deviceId: String? = null,
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
    val facts: List<LocalizedText> = emptyList(),
    val genres: List<String> = emptyList(),
    val premiereDate: String? = null,
    val available: Boolean = true,
    val tmdbId: Int? = null,
    val isFolder: Boolean = false,
    val collectionType: String? = null,
    val seriesId: String? = null,
    val lastActivityEpochMillis: Long? = null,
    val logoItemId: String? = null,
    val logoUrl: String? = null,
    val heroImagePath: String? = null,
    val posterImagePath: String? = null,
    val heroUrl: String? = null,
    val posterUrl: String? = null,
    /**
     * Jellyfin's content hash for the chosen image. It belongs in the address: replacing a poster
     * on the server changes the tag, which changes the URL, which is what makes an image cache
     * update itself. Without it the address is identical before and after, and the old picture is
     * served for as long as the cache keeps it.
     */
    val artworkTag: String? = null,
    val logoTag: String? = null,
    /**
     * Kept as numbers rather than baked into [subtitle]. "S06 E13" is a filename, not something a
     * person reads, and only the UI knows the language it has to be written in.
     */
    val season: Int? = null,
    val episode: Int? = null,
    /** Set by the caller that asked one library for this item, never read from the payload. */
    val libraryId: String? = null,
    val favourite: Boolean = false,
    val played: Boolean = false,
    /** Minutes, for an episode list that wants to say how long each one is. */
    val runtimeMinutes: Int? = null,
    /** Episodes in a season, so a season row can say "8 episodar". */
    val childCount: Int? = null,
    val criticRating: Int? = null,
    val tmdbRating: Float? = null,
    val mdblistRating: Float? = null,
)

data class RemoteLibraryView(
    val id: String,
    val name: String,
    val collectionType: String?,
    val artworkUrl: String? = null,
)

data class RemoteQueueItem(
    val id: String,
    val title: String,
    val source: ServiceKind,
    val status: LocalizedText,
    val state: IncomingState,
    val progress: Int?,
    val artworkUrl: String?,
    val overview: String? = null,
    val facts: List<LocalizedText> = emptyList(),
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
    val facts: List<LocalizedText> = emptyList(),
    val genres: List<String> = emptyList(),
    /**
     * A disc release rather than a digital one.
     *
     * The calendar used to work this out by looking for the nynorsk words "Fysisk utgjeving" among
     * the facts — so it silently stopped being true the moment the facts were said in any other
     * language.
     */
    val physicalRelease: Boolean = false,
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
    val facts: List<LocalizedText> = emptyList(),
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
    val facts: List<LocalizedText> = emptyList(),
    val genres: List<String> = emptyList(),
)

data class RemoteMediaDetails(
    val title: String?,
    val artworkUrl: String?,
    val tagline: String? = null,
    val overview: String? = null,
    val facts: List<LocalizedText> = emptyList(),
    val genres: List<String> = emptyList(),
    val cast: List<app.reelstack.data.model.CastMember> = emptyList(),
    val seerrStatus: Int? = null,
    val seasons: List<app.reelstack.data.model.RequestSeason> = emptyList(),
    val downloads: List<app.reelstack.data.model.RequestDownload> = emptyList(),
    val requestStatus: Int? = null,
    val status4k: Int? = null,
    val seasons4k: List<app.reelstack.data.model.RequestSeason> = emptyList(),
    val downloads4k: List<app.reelstack.data.model.RequestDownload> = emptyList(),
    val nextEpisode: app.reelstack.data.model.SeriesNextEpisode? = null,
    val progress: Float? = null,
    val remainingMinutes: Int? = null,
    val quality: List<String> = emptyList(),
    val favourite: Boolean = false,
    val played: Boolean = false,
    val audioTracks: List<app.reelstack.data.model.MediaTrack> = emptyList(),
    val subtitleTracks: List<app.reelstack.data.model.MediaTrack> = emptyList(),
    /** Id and name, because playback is asked for by id and people read the name. */
    val versions: List<Pair<String, String>> = emptyList(),
    val backdropUrl: String? = null,
    val logoUrl: String? = null,
    val seriesId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val trailerUrl: String? = null,
    val criticRating: Int? = null,
    val tmdbRating: Float? = null,
    val mdblistRating: Float? = null,
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

    fun libraryItems(payload: String, preferEpisodeStill: Boolean = false): List<RemoteLibraryItem> {
        val root = json.parseToJsonElement(payload)
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> root.array("Items").takeIf { it.isNotEmpty() } ?: root.array("items")
            else -> JsonArray(emptyList())
        }
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.string("Id") ?: item.string("id") ?: return@mapNotNull null
            val name = item.string("Name") ?: item.string("name") ?: return@mapNotNull null
            // Jellyfin can answer with an empty SeriesName, and an empty string is not a name:
            // taking it at face value left one card on the shelf showing nothing but its episode
            // number. Blank means absent, and absent falls back to the item's own name.
            val series = (item.string("SeriesName") ?: item.string("seriesName"))?.takeIf(String::isNotBlank)
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
            val ownStill = (item.obj("ImageTags") ?: item.obj("imageTags")).tag("Primary")
            val artwork = if (preferEpisodeStill && mediaType == "Episode" && ownStill != null)
                LibraryArtwork(id, "Primary", ownStill) else libraryArtwork(item, id, mediaType)
            RemoteLibraryItem(
                id = id,
                isFolder = item["IsFolder"]?.jsonPrimitive?.booleanOrNull == true || mediaType in setOf("Series", "Season", "BoxSet", "Folder", "CollectionFolder", "MusicAlbum", "MusicArtist"),
                // Without a series name the card has only the episode's own name to show, and
                // that name repeats the number printed directly under it. The prefix comes off for
                // the same reason it comes off the line below; if nothing survives the cleaning,
                // the raw name is still better than an empty card.
                title = when {
                    mediaType == "Season" -> name
                    series != null -> series
                    else -> app.reelstack.data.model.episodeNameOf(name, episode).ifBlank { name }
                },
                // The year belongs to a film or a series, never to an episode: "Sesong 19 · Episode
                // 9 – 2025" reads as though the year were the episode's name.
                subtitle = listOfNotNull(episodeLabel, name.takeIf { series != null },
                    year?.toString().takeIf { series == null && episodeLabel == null })
                    .distinct()
                    .joinToString(" · ")
                    .ifBlank { "" },
                progress = progress,
                seriesId = item.string("SeriesId") ?: item.string("seriesId"),
                lastActivityEpochMillis = (userData?.string("LastPlayedDate") ?: userData?.string("lastPlayedDate"))
                    ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() },
                mediaType = mediaType,
                artworkItemId = artwork.itemId,
                artworkImageType = artwork.imageType,
                artworkTag = artwork.tag,
                heroImagePath = libraryHeroPath(item, id, mediaType),
                posterImagePath = libraryPosterPath(item, id, mediaType),
                season = season,
                episode = episode,
                logoItemId = libraryLogo(item, id)?.itemId,
                logoTag = libraryLogo(item, id)?.tag,
                overview = item.string("Overview") ?: item.string("overview"),
                facts = libraryFacts(item, mediaType, runtime),
                genres = stringArray(item, "Genres", "genres"),
                premiereDate = item.string("PremiereDate") ?: item.string("premiereDate"),
                tmdbId = (item.obj("ProviderIds") ?: item.obj("providerIds"))?.let {
                    (it.string("Tmdb") ?: it.string("tmdb"))?.toIntOrNull()
                },
                available = item["IsMissing"]?.jsonPrimitive?.booleanOrNull != true &&
                    item["IsVirtualUnaired"]?.jsonPrimitive?.booleanOrNull != true &&
                    item["IsPlaceHolder"]?.jsonPrimitive?.booleanOrNull != true &&
                    !item.string("LocationType").equals("Virtual", ignoreCase = true),
                runtimeMinutes = runtime?.takeIf { it > 0 }?.let { (it / TICKS_PER_MINUTE).toInt() },
                childCount = item.int("ChildCount") ?: item.int("childCount")
                    ?: item.int("RecursiveItemCount") ?: item.int("recursiveItemCount"),
                criticRating = criticRating(item),
                tmdbRating = tmdbRating(item),
                mdblistRating = mdblistRating(item),
                favourite = userData?.get("IsFavorite")?.jsonPrimitive?.booleanOrNull == true ||
                    userData?.get("isFavorite")?.jsonPrimitive?.booleanOrNull == true,
                played = userData?.get("Played")?.jsonPrimitive?.booleanOrNull == true ||
                    userData?.get("played")?.jsonPrimitive?.booleanOrNull == true,
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
        return records.mapNotNull { record -> (record as? JsonObject)?.let { queueItem(it, source) } }
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
            val item = element as? JsonObject ?: return@mapNotNull null
            if (source == ServiceKind.RADARR) {
                val id = item.int("id")?.toString() ?: return@mapNotNull null
                val title = item.string("title") ?: return@mapNotNull null
                val digitalRelease = item.string("digitalRelease")
                val physicalRelease = item.string("physicalRelease")
                // A disc release and a digital one are different facts, so they travel as
                // different flags rather than as two sentences one of them is then compared against.
                val release = listOfNotNull(
                    digitalRelease?.let { it to false },
                    physicalRelease?.let { it to true },
                ).firstOrNull { (date, _) ->
                    notBefore == null || calendarInstant(date)?.let { !it.isBefore(notBefore) } == true
                } ?: return@mapNotNull null
                val (dateTime, isPhysical) = release
                val availability =
                    LocalizedText(if (isPhysical) R.string.release_physical else R.string.release_digital)
                val year = item.int("year")
                RemoteUpcomingItem(
                    id = id,
                    title = title,
                    subtitle = year?.toString().orEmpty(),
                    dateTime = dateTime,
                    source = source,
                    artworkUrl = secureArtwork(item, "poster"),
                    mediaType = "Movie",
                    overview = item.string("overview"),
                    facts = listOfNotNull(
                        year?.let { LocalizedText.raw(it.toString()) },
                        item.int("runtime")?.takeIf { it > 0 }?.let { LocalizedText(R.string.media_minutes, it) },
                        availability,
                    ),
                    genres = stringArray(item, "genres"),
                    physicalRelease = isPhysical,
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
                    subtitle = listOfNotNull(episodeNumber, episodeTitle).joinToString(" · "),
                    dateTime = item.string("airDateUtc") ?: item.string("airDate") ?: return@mapNotNull null,
                    source = source,
                    artworkUrl = series?.let { secureArtwork(it, "fanart") }
                        ?: secureArtwork(item, "fanart"),
                    mediaType = "Episode",
                    overview = series?.string("overview") ?: item.string("overview"),
                    facts = listOfNotNull(
                        series?.int("year")?.let { LocalizedText.raw(it.toString()) },
                        series?.int("runtime")?.takeIf { it > 0 }?.let { LocalizedText(R.string.media_minutes, it) },
                        series?.string("network")?.let { LocalizedText.raw(it) },
                    ),
                    genres = series?.let { stringArray(it, "genres") }.orEmpty(),
                )
            }
        }
    }

    fun discover(payload: String): List<RemoteDiscoverItem> {
        val results = json.parseToJsonElement(payload).jsonObject.array("results")
        return results.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
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
                metadata = year.orEmpty(),
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
                metadata = year.orEmpty(),
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
            val request = element as? JsonObject ?: return@mapNotNull null
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
                    app.reelstack.data.model.RequestSeason(number, seasonName(number), 0, season.int(statusKey) ?: 1)
                },
                createdAt = request.string("createdAt"),
                title = media?.string("title") ?: media?.string("name"),
                artworkUrl = media?.string("posterPath")?.let(::safeTmdbArtwork),
            )
        }
    }

    /** Seerr reports how many pages a search has; without it "load more" would guess. */
    fun totalPages(payload: String): Int =
        ((runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject)?.int("totalPages") ?: 1)
            .coerceAtLeast(1)

    /** The owner of a single `api/v1/request/{id}` response, used to confirm a withdrawal. */
    fun requestOwnerId(payload: String): String? =
        (runCatching { json.parseToJsonElement(payload) }.getOrNull() as? JsonObject)
            ?.obj("requestedBy")?.int("id")?.toString()

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
            nextEpisode = item.obj("nextEpisodeToAir")?.let { episode ->
                val season = episode.int("seasonNumber")?.takeIf { it > 0 } ?: return@let null
                val number = episode.int("episodeNumber")?.takeIf { it > 0 } ?: return@let null
                val date = runCatching { java.time.LocalDate.parse(episode.string("airDate")) }.getOrNull() ?: return@let null
                app.reelstack.data.model.SeriesNextEpisode(season, number, date)
            },
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
            val pending = requests.filter { request -> (request["is4k"]?.jsonPrimitive?.booleanOrNull == true) == (statusKey == "status4k") && request.int("status") in setOf(1, 2) &&
                request.array("seasons").any { (it as? JsonObject)?.int("seasonNumber") == number } }
            val recorded = existing[number]?.int(statusKey) ?: 1
            val status = if (recorded in setOf(1, 7) && pending.isNotEmpty()) {
                if (pending.any { it.int("status") == 2 }) 3 else 2
            } else recorded
            app.reelstack.data.model.RequestSeason(number,
                seasonName(number), season.int("episodeCount") ?: 0, status,
                airDate = runCatching { java.time.LocalDate.parse(season.string("airDate")) }.getOrNull())
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

    fun libraryDetails(payload: String, baseUrl: String? = null): RemoteMediaDetails {
        val item = json.parseToJsonElement(payload) as? JsonObject ?: return RemoteMediaDetails(null, null)
        val runtime = item.long("RunTimeTicks") ?: item.long("runTimeTicks")
        val mediaType = item.string("Type") ?: item.string("type") ?: "Video"
        val userData = item.obj("UserData")
        val position = userData?.long("PlaybackPositionTicks")
        val progress = if (runtime != null && runtime > 0 && position != null)
            (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f)
        else userData?.get("PlayedPercentage")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.div(100)?.coerceIn(0f, 1f)
        val streams = item.array("MediaStreams").ifEmpty {
            (item.array("MediaSources").firstOrNull() as? JsonObject)?.array("MediaStreams").orEmpty()
        }.mapNotNull { it as? JsonObject }
        val video = streams.firstOrNull { it.string("Type").equals("Video", true) }
        val audio = streams.firstOrNull { it.string("Type").equals("Audio", true) }
        val width = video?.int("Width") ?: item.int("Width") ?: 0
        val height = video?.int("Height") ?: item.int("Height") ?: 0
        val quality = buildList {
            when {
                width >= 3800 || height >= 2100 -> add("4K")
                width >= 1900 || height >= 1000 -> add("1080p")
                width >= 1200 || height >= 700 -> add("720p")
                height > 0 -> add("${height}p")
            }
            video?.string("VideoRangeType")?.takeIf { it.isNotBlank() && !it.equals("SDR", true) }?.let(::add)
            video?.string("Codec")?.takeIf(String::isNotBlank)?.uppercase(java.util.Locale.ROOT)?.let(::add)
            audio?.string("Codec")?.takeIf(String::isNotBlank)?.uppercase(java.util.Locale.ROOT)?.let { codec ->
                val channels = when (val count = audio.int("Channels")) { 8 -> "7.1"; 6 -> "5.1"; 2 -> "2.0"; 1 -> "1.0"; else -> count?.toString() }
                add(listOfNotNull(codec, channels).joinToString(" "))
            }
        }
        return RemoteMediaDetails(
            title = item.string("Name") ?: item.string("name"),
            artworkUrl = if (mediaType.equals("Episode", true) && baseUrl != null &&
                item.string("Id") != null && (item["ImageTags"] as? JsonObject)?.string("Primary") != null) {
                val id = java.net.URLEncoder.encode(item.string("Id"), "UTF-8")
                val tag = java.net.URLEncoder.encode((item["ImageTags"] as JsonObject).string("Primary"), "UTF-8")
                EndpointValidator.resolve(baseUrl, "Items/$id/Images/Primary?maxWidth=960&quality=85&tag=$tag")
            } else null,
            backdropUrl = libraryBackdrop(item, baseUrl),
            logoUrl = app.reelstack.player.playableLogoUrl(item, baseUrl),
            trailerUrl = item.array("RemoteTrailers").firstNotNullOfOrNull {
                app.reelstack.data.model.trailerLink((it as? JsonObject)?.string("Url"))
            },
            seriesId = item.string("SeriesId"),
            season = item.int("ParentIndexNumber"),
            episode = item.int("IndexNumber"),
            tagline = item.string("Tagline") ?: item.string("tagline")
                ?: item.array("Taglines").firstOrNull()?.jsonPrimitive?.contentOrNull,
            overview = item.string("Overview") ?: item.string("overview"),
            facts = libraryFacts(item, mediaType, runtime),
            progress = progress,
            criticRating = criticRating(item),
            tmdbRating = tmdbRating(item),
            mdblistRating = mdblistRating(item),
            remainingMinutes = if (runtime != null && runtime > 0 && position != null)
                kotlin.math.ceil((runtime - position).coerceAtLeast(0).toDouble() / TICKS_PER_MINUTE).toInt() else null,
            quality = quality,
            genres = stringArray(item, "Genres", "genres"),
            favourite = userData?.get("IsFavorite")?.jsonPrimitive?.booleanOrNull == true,
            played = userData?.get("Played")?.jsonPrimitive?.booleanOrNull == true,
            audioTracks = mediaTracks(streams, "Audio"),
            subtitleTracks = mediaTracks(streams, "Subtitle"),
            // Only worth naming when there is a choice to make. A single file is just "the file".
            versions = item.array("MediaSources").mapNotNull { source ->
                val media = source as? JsonObject ?: return@mapNotNull null
                val sourceId = media.string("Id")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                sourceId to (media.string("Name")?.takeIf(String::isNotBlank) ?: sourceId)
            }.distinctBy { it.first }.takeIf { it.size > 1 }.orEmpty(),
            cast = item.array("People").mapNotNull {
                val person = it as? JsonObject ?: return@mapNotNull null
                if (!person.string("Type").equals("Actor", ignoreCase = true)) return@mapNotNull null
                val name = person.string("Name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val id = person.string("Id")?.takeIf(String::isNotBlank)
                val tag = person.string("PrimaryImageTag")?.takeIf(String::isNotBlank)
                fun encodePerson(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
                val portrait = if (baseUrl != null && id != null && tag != null)
                    EndpointValidator.resolve(baseUrl, "Items/${encodePerson(id)}/Images/Primary?maxWidth=184&quality=85&tag=${encodePerson(tag)}") else null
                app.reelstack.data.model.CastMember(name, person.string("Role"), portrait, id)
            }.distinctBy { it.name }.take(16),
        )
    }

    /**
     * Streams of one kind, named the way a reader would name them.
     *
     * `DisplayTitle` is the server's own label ("Norsk - AC3 5.1") and is what Jellyfin's own
     * clients show, so it is preferred; the language and the index are only fallbacks for a file
     * whose streams were never tagged.
     */
    private fun libraryBackdrop(item: JsonObject, baseUrl: String?): String? {
        if (baseUrl == null) return null
        val ownTag = item.array("BackdropImageTags").firstOrNull()?.jsonPrimitive?.contentOrNull
        val parentTag = item.array("ParentBackdropImageTags").firstOrNull()?.jsonPrimitive?.contentOrNull
        val id = if (ownTag != null) item.string("Id") else item.string("ParentBackdropItemId")
        val tag = ownTag ?: parentTag
        if (id.isNullOrBlank() || tag.isNullOrBlank()) return null
        fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        return EndpointValidator.resolve(baseUrl, "Items/${encode(id)}/Images/Backdrop/0?maxWidth=1280&quality=80&tag=${encode(tag)}")
    }

    private fun mediaTracks(streams: List<JsonObject>, type: String): List<app.reelstack.data.model.MediaTrack> =
        streams.filter { it.string("Type").equals(type, ignoreCase = true) }.mapNotNull { stream ->
            val index = stream.int("Index") ?: return@mapNotNull null
            val language = stream.string("DisplayLanguage")?.takeIf(String::isNotBlank)
                ?: stream.string("Language")?.takeIf(String::isNotBlank)
            app.reelstack.data.model.MediaTrack(
                index = index,
                label = stream.string("DisplayTitle")?.takeIf(String::isNotBlank)
                    ?: stream.string("Title")?.takeIf(String::isNotBlank)
                    ?: language
                    ?: "$type ${index + 1}",
                language = language,
                isDefault = stream["IsDefault"]?.jsonPrimitive?.booleanOrNull == true,
                forced = stream["IsForced"]?.jsonPrimitive?.booleanOrNull == true,
                codec = stream.string("Codec")?.takeIf(String::isNotBlank),
            )
        }

    private fun playbackSession(element: JsonElement): RemotePlayback? {
        val session = element as? JsonObject ?: return null
        val item = session.obj("NowPlayingItem") ?: return null
        val playState = session.obj("PlayState") ?: JsonObject(emptyMap())
        val title = item.string("SeriesName") ?: item.string("Name") ?: return null
        val episodeName = item.string("Name").takeIf { item.string("SeriesName") != null }
        val season = item.int("ParentIndexNumber")
        val episode = item.int("IndexNumber")
        val index = if (season != null && episode != null) {
            "S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"
        } else {
            item.string("Type").orEmpty()
        }
        val subtitle = listOfNotNull(index.takeIf(String::isNotBlank), episodeName).distinct().joinToString(" · ")
        val position = playState.long("PositionTicks") ?: 0L
        val runtime = item.long("RunTimeTicks") ?: 0L
        val remainingMinutes = ((runtime - position).coerceAtLeast(0L) / TICKS_PER_MINUTE).toInt()
        val width = item.int("Width") ?: 0
        val playMethod = playState.string("PlayMethod")
        return RemotePlayback(
            sessionId = session.string("Id") ?: session.string("id") ?: return null,
            userId = session.string("UserId") ?: session.string("userId"),
            userName = session.string("UserName").orEmpty(),
            deviceName = (session.string("DeviceName") ?: session.string("Client")).orEmpty(),
            deviceId = session.string("DeviceId"),
            title = title,
            subtitle = subtitle,
            progress = if (runtime > 0L) (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f) else 0f,
            remainingMinutes = remainingMinutes,
            transcoding = session.obj("TranscodingInfo") != null ||
                playMethod.equals("Transcode", ignoreCase = true),
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
            season = season,
            episode = episode,
        )
    }

    private fun queueItem(item: JsonObject, source: ServiceKind): RemoteQueueItem? {
        val media = item.obj(if (source == ServiceKind.RADARR) "movie" else "series")
        val title = media?.string("title") ?: item.string("title") ?: return null
        // Sonarr can expose several episode rows from one season pack with the same downloadId.
        // The queue record id is the row identity; preferring downloadId produced duplicate Compose
        // keys exactly while a multi-episode download was active.
        val id = item.string("id") ?: item.string("downloadId") ?: "$title-${item.hashCode()}"
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
            IncomingState.READY -> LocalizedText(R.string.queue_ready_for_import)
            IncomingState.DOWNLOADING ->
                progress?.let { LocalizedText(R.string.queue_downloading_percent, it) }
                    ?: LocalizedText(R.string.queue_downloading)
            IncomingState.REQUESTED -> LocalizedText(R.string.queue_waiting)
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
                media?.int("year")?.let { LocalizedText.raw(it.toString()) },
                progress?.let { LocalizedText.raw("$it %") },
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

    private fun criticRating(item: JsonObject): Int? =
        (item.double("CriticRating") ?: item.double("criticRating"))
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }
            ?.let { kotlin.math.round(it).toInt() }

    private fun tmdbRating(item: JsonObject): Float? =
        (item.double("TmdbRating") ?: item.double("TMDBRating")
            ?: item.double("CommunityRating")?.times(10.0))
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }?.toFloat()

    private fun mdblistRating(item: JsonObject): Float? =
        (item.double("MdbListRating") ?: item.double("MDBListRating")
            ?: item.double("MdbList"))?.let { if (it <= 10.0) it * 10.0 else it }
            ?.takeIf { it.isFinite() && it in 0.0..100.0 }?.toFloat()

    private fun stringArray(item: JsonObject, vararg keys: String): List<String> = keys.firstNotNullOfOrNull { key ->
        (item[key] as? JsonArray)?.mapNotNull { value -> value.jsonPrimitive.contentOrNull }?.takeIf(List<String>::isNotEmpty)
    }.orEmpty()

    private fun objectNameArray(item: JsonObject, key: String): List<String> = item.array(key)
        .mapNotNull { value -> (value as? JsonObject)?.string("name") }

    /**
     * What a title is, in facts rather than in a sentence.
     *
     * The type word used to lead this list, written out in nynorsk. It is not data: every caller
     * already carries `mediaType`, so the word belongs where the language is known. What is left
     * here is either a number or something the server said, and both mean the same in any language.
     */
    private fun libraryFacts(item: JsonObject, mediaType: String, runtimeTicks: Long?): List<LocalizedText> = buildList {
        if (mediaType.equals("episode", ignoreCase = true)) {
            val season = item.int("ParentIndexNumber") ?: item.int("parentIndexNumber")
            val episode = item.int("IndexNumber") ?: item.int("indexNumber")
            if (season != null && episode != null) {
                add(LocalizedText.raw("S${season.toString().padStart(2, '0')} E${episode.toString().padStart(2, '0')}"))
            }
        }
        (item.int("ProductionYear") ?: item.int("productionYear"))?.let { add(LocalizedText.raw(it.toString())) }
        runtimeTicks?.takeIf { it > 0 }?.let { add(LocalizedText(R.string.media_minutes, (it / TICKS_PER_MINUTE).toInt())) }
        (item.string("OfficialRating") ?: item.string("officialRating"))?.let { add(LocalizedText.raw(it)) }
        (item.double("CommunityRating") ?: item.double("communityRating"))?.let {
            add(LocalizedText.raw("★ ${"%.1f".format(it)}"))
        }
    }

    /** "Sesong 3", or the name a season zero actually has. */
    private fun seasonName(number: Int): LocalizedText =
        if (number == 0) LocalizedText(R.string.media_specials) else LocalizedText(R.string.media_season_number, number)

    private fun discoverFacts(item: JsonObject, mediaType: String, year: String?): List<LocalizedText> = buildList {
        year?.takeIf { it.length == 4 && it.all(Char::isDigit) }?.let { add(LocalizedText.raw(it)) }
        val runtime = item.int("runtime") ?: item.array("episodeRunTime").firstOrNull()?.jsonPrimitive?.intOrNull
        runtime?.takeIf { it > 0 }?.let { add(LocalizedText(R.string.media_minutes, it)) }
        item.double("voteAverage")?.takeIf { it > 0 }?.let { add(LocalizedText.raw("★ ${"%.1f".format(it)}")) }
        item.string("status")?.trim()?.takeIf { it.isNotEmpty() }?.let { add(seerrProductionStatus(it)) }
        if (mediaType == "tv") {
            (item.int("numberOfSeasons") ?: item.int("numberOfSeason"))?.takeIf { it > 0 }?.let {
                add(LocalizedText.plural(R.plurals.media_seasons, it))
            }
            item.int("numberOfEpisodes")?.takeIf { it > 0 }?.let {
                add(LocalizedText.plural(R.plurals.media_episodes, it))
            }
            objectNameArray(item, "networks").map(String::trim).filter(String::isNotEmpty)
                .distinct().takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { add(LocalizedText.raw(it)) }
        }
    }

    /** Seerr answers in its API's English; the reader gets their own. An unknown value passes through. */
    private fun seerrProductionStatus(status: String): LocalizedText = when (status.lowercase()) {
        "returning series" -> LocalizedText(R.string.production_returning)
        "ended" -> LocalizedText(R.string.production_ended)
        "canceled", "cancelled" -> LocalizedText(R.string.production_canceled)
        "in production" -> LocalizedText(R.string.production_in_production)
        "post production" -> LocalizedText(R.string.production_post)
        "planned" -> LocalizedText(R.string.production_planned)
        "pilot" -> LocalizedText(R.string.production_pilot)
        "released" -> LocalizedText(R.string.production_released)
        "rumored", "rumoured" -> LocalizedText(R.string.production_rumoured)
        else -> LocalizedText.raw(status)
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

    /** Case-insensitive lookup, because Jellyfin and Emby disagree about capitalisation. */
    private fun JsonObject?.tag(type: String): String? =
        this?.keys?.firstOrNull { it.equals(type, ignoreCase = true) }?.let { this.string(it) }

    private fun libraryArtwork(item: JsonObject, id: String, mediaType: String): LibraryArtwork {
        val imageTags = item.obj("ImageTags") ?: item.obj("imageTags")
        val hasOwnThumb = imageTags?.keys?.any { it.equals("Thumb", ignoreCase = true) } == true
        val isEpisode = mediaType.equals("episode", ignoreCase = true)
        val isSeriesArtwork = isEpisode || mediaType.equals("series", ignoreCase = true)
        if (isSeriesArtwork) {
            if (hasOwnThumb) return LibraryArtwork(id, "Thumb", imageTags.tag("Thumb"))
            (item.string("ParentThumbItemId") ?: item.string("parentThumbItemId"))?.let {
                return LibraryArtwork(it, "Thumb",
                    item.string("ParentThumbImageTag") ?: item.string("parentThumbImageTag"))
            }
            val seriesId = item.string("SeriesId") ?: item.string("seriesId")
            val seriesThumbTag = item.string("SeriesThumbImageTag") ?: item.string("seriesThumbImageTag")
                ?: item.string("ParentThumbImageTag") ?: item.string("parentThumbImageTag")
            if (seriesId != null && seriesThumbTag != null) {
                return LibraryArtwork(seriesId, "Thumb", seriesThumbTag)
            }
            if (isEpisode) {
                val ownPrimaryTag = imageTags.tag("Primary")
                if (ownPrimaryTag != null) {
                    return LibraryArtwork(id, "Primary", ownPrimaryTag)
                }
            }
        }
        val seriesId = item.string("SeriesId") ?: item.string("seriesId")
        val primaryItemId = item.string("PrimaryImageItemId") ?: item.string("primaryImageItemId")
        return LibraryArtwork(
            itemId = seriesId ?: primaryItemId ?: id,
            imageType = "Primary",
            // The tag has to belong to the item the address points at, or it is worse than none.
            tag = when {
                seriesId != null -> item.string("SeriesPrimaryImageTag") ?: item.string("seriesPrimaryImageTag")
                primaryItemId != null -> item.string("PrimaryImageTag") ?: item.string("primaryImageTag")
                else -> imageTags.tag("Primary")
            },
        )
    }

    private fun imagePath(id: String?, type: String, tag: String?): String? {
        if (id.isNullOrBlank() || tag.isNullOrBlank()) return null
        fun enc(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        return "Items/${enc(id)}/Images/$type?maxWidth=${if (type == "Primary") 480 else 1920}&quality=90&tag=${enc(tag)}"
    }

    /** Hero art belongs to the movie or series, never to an individual episode. */
    private fun libraryHeroPath(item: JsonObject, id: String, type: String): String? {
        val episode = type.equals("Episode", true)
        val tags = item.obj("ImageTags") ?: item.obj("imageTags")
        if (!episode) {
            imagePath(id, "Backdrop/0", item.array("BackdropImageTags").firstOrNull()?.jsonPrimitive?.contentOrNull)?.let { return it }
            imagePath(id, "Thumb", tags.tag("Thumb"))?.let { return it }
        }
        imagePath(item.string("ParentBackdropItemId"), "Backdrop/0",
            item.array("ParentBackdropImageTags").firstOrNull()?.jsonPrimitive?.contentOrNull)?.let { return it }
        return imagePath(item.string("ParentThumbItemId") ?: item.string("SeriesId"), "Thumb",
            item.string("ParentThumbImageTag") ?: item.string("SeriesThumbImageTag"))
    }

    private fun libraryPosterPath(item: JsonObject, id: String, type: String): String? =
        if (type.equals("Episode", true)) imagePath(item.string("SeriesId"), "Primary", item.string("SeriesPrimaryImageTag"))
        else imagePath(id, "Primary", (item.obj("ImageTags") ?: item.obj("imageTags")).tag("Primary"))

    private fun libraryLogo(item: JsonObject, id: String): LibraryArtwork? {
        val imageTags = item.obj("ImageTags") ?: item.obj("imageTags")
        val hasOwnLogo = imageTags?.keys?.any { it.equals("Logo", ignoreCase = true) } == true
        if (hasOwnLogo) return LibraryArtwork(id, "Logo", imageTags.tag("Logo"))
        val seriesId = item.string("SeriesId") ?: item.string("seriesId")
        val seriesLogoTag = item.string("SeriesLogoImageTag") ?: item.string("seriesLogoImageTag")
            ?: item.string("ParentLogoImageTag") ?: item.string("parentLogoImageTag")
        if (seriesId != null && seriesLogoTag != null) return LibraryArtwork(seriesId, "Logo", seriesLogoTag)
        val parentLogoId = item.string("ParentLogoItemId") ?: item.string("parentLogoItemId")
            ?: return null
        return LibraryArtwork(parentLogoId, "Logo",
            item.string("ParentLogoImageTag") ?: item.string("parentLogoImageTag"))
    }

    private const val TICKS_PER_MINUTE = 600_000_000L

    private data class PreferredUser(
        val id: String,
        val accessScore: Int,
        val originalIndex: Int,
    )

    private data class LibraryArtwork(val itemId: String, val imageType: String, val tag: String? = null)
}
