package app.reelstack.data.network

import app.reelstack.BuildConfig
import app.reelstack.data.model.ConnectionTestResult
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis

private data class ServiceProbe(
    val path: String,
    val headerName: String,
)

data class SeerrFeed(
    val discover: List<RemoteDiscoverItem>,
    val requests: List<RemoteRequest>,
)

data class MediaServerFeed(
    val sessions: List<RemotePlayback>,
    val recentMovies: List<RemoteLibraryItem>,
    val recentSeries: List<RemoteLibraryItem>,
    val warning: String? = null,
)

data class QueueServiceFeed(
    val queue: List<RemoteQueueItem>,
    val upcoming: List<RemoteUpcomingItem>,
)

data class JellyfinAuthentication(
    val accessToken: String,
    val userId: String,
)

class JellyfinAuthenticationClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val deviceId: String = "homereel-android",
) {
    fun authenticate(baseUrl: String, username: String, password: String): JellyfinAuthentication {
        val body = buildJsonObject {
            put("Username", username)
            put("Pw", password)
        }.toString()
        val authorization = "MediaBrowser Client=\"HomeReel\", Device=\"Android\", " +
            "DeviceId=\"$deviceId\", Version=\"${BuildConfig.VERSION_NAME}\""
        val response = transport.post(
            EndpointValidator.resolve(baseUrl, "Users/AuthenticateByName"),
            mapOf(
                "Authorization" to authorization,
                "X-Emby-Authorization" to authorization,
            ),
            body,
        )

        when (response.statusCode) {
            401, 403 -> error("Feil brukarnamn eller passord")
            404 -> error("Fann Jellyfin, men innlogging med brukarkonto er ikkje tilgjengeleg")
            in 200..299 -> Unit
            in 500..599 -> error("Jellyfin er utilgjengeleg no")
            else -> error("Jellyfin svara med status ${response.statusCode}")
        }

        val root = runCatching { Json.parseToJsonElement(response.body).jsonObject }
            .getOrElse { error("Jellyfin sende eit ugyldig innloggingssvar") }
        val token = root["AccessToken"]?.jsonPrimitive?.contentOrNull
            ?: root["accessToken"]?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake eit tilgangsteikn")
        val user = root["User"]?.jsonObject ?: root["user"]?.jsonObject
        val userId = user?.get("Id")?.jsonPrimitive?.contentOrNull
            ?: user?.get("id")?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake ein profil-ID")
        return JellyfinAuthentication(accessToken = token, userId = userId)
    }
}

class ServiceConnectionTester(
    private val transport: JsonHttpTransport = HttpTransport(),
) {
    fun test(connection: ServiceConnection): ConnectionTestResult {
        val probe = when (connection.kind) {
            ServiceKind.JELLYFIN, ServiceKind.EMBY -> ServiceProbe("System/Info", "X-Emby-Token")
            ServiceKind.SEERR -> ServiceProbe("api/v1/request?take=1&skip=0", "X-Api-Key")
            ServiceKind.RADARR, ServiceKind.SONARR -> ServiceProbe("api/v3/system/status", "X-Api-Key")
        }
        val endpoint = EndpointValidator.resolve(connection.baseUrl, probe.path)
        lateinit var response: HttpResponse
        val elapsed = measureTimeMillis {
            response = transport.get(endpoint, mapOf(probe.headerName to connection.token))
        }

        return when (response.statusCode) {
            in 200..299 -> ConnectionTestResult(
                success = true,
                latencyMs = elapsed,
                message = extractVersion(response.body)?.let { "Tilkopla · v$it" } ?: "Tilkopla",
            )
            401, 403 -> ConnectionTestResult(false, elapsed, "API-nøkkelen vart avvist")
            404 -> ConnectionTestResult(false, elapsed, "Fann tenesta, men API-stien var ikkje tilgjengeleg")
            else -> ConnectionTestResult(false, elapsed, "Tenaren svara med status ${response.statusCode}")
        }
    }

    private fun extractVersion(json: String): String? {
        val pattern = Regex("\\\"(?:Version|version)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        return pattern.find(json)?.groupValues?.getOrNull(1)
    }
}

class MediaServerClient(
    private val transport: JsonHttpTransport = HttpTransport(),
) {
    fun sessions(connection: ServiceConnection): List<RemotePlayback> {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Sessions"),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.playbackSessions(response.body).map { item ->
            item.copy(artworkUrl = item.artworkItemId?.let { artworkUrl(connection, it) })
        }
    }

    fun feed(connection: ServiceConnection): MediaServerFeed {
        val warnings = mutableListOf<String>()
        val sessionsResult = runCatching { sessions(connection) }
        val sessions = sessionsResult.getOrElse {
            warnings += "Avspelingsøkter er utilgjengelege"
            emptyList()
        }
        val userId = connection.userId.takeIf { it.isNotBlank() }
            ?: runCatching { currentUserId(connection) }.getOrNull()
            ?: runCatching { preferredAvailableUserId(connection) }.getOrNull()

        val encodedUserId = userId?.let(::encodePathSegment)
        val views = encodedUserId?.let { encodedId ->
            runCatching { libraryViews(connection, encodedId) }.getOrDefault(emptyList())
        }.orEmpty()
        val moviesResult = runCatching {
            latestAcrossLibraries(
                connection = connection,
                userId = encodedUserId,
                itemType = "Movie",
                groupItems = false,
                views = views,
            )
        }
        val movies = moviesResult.getOrElse {
            warnings += "Nyleg lagde til filmar er utilgjengelege"
            emptyList()
        }
        val seriesResult = runCatching {
            latestAcrossLibraries(
                connection = connection,
                userId = encodedUserId,
                itemType = "Episode",
                groupItems = true,
                views = views,
            )
        }
        val series = seriesResult.getOrElse {
            warnings += "Nyleg lagde til seriar er utilgjengelege"
            emptyList()
        }

        if (userId == null && connection.kind == ServiceKind.EMBY) {
            warnings += "Legg til profil-ID for bibliotekradene frå Emby"
        }
        val anyFeedCallSucceeded = sessionsResult.isSuccess || moviesResult.isSuccess || seriesResult.isSuccess
        if (!anyFeedCallSucceeded) {
            verifyConnection(connection)
            warnings += "Mediedelane er utilgjengelege"
        }
        return MediaServerFeed(
            sessions = sessions,
            recentMovies = movies,
            recentSeries = series,
            warning = warnings.distinct().takeIf { it.isNotEmpty() }?.joinToString(" · "),
        )
    }

    fun setPaused(connection: ServiceConnection, sessionId: String, paused: Boolean) {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val encodedSessionId = java.net.URLEncoder.encode(sessionId, Charsets.UTF_8.name())
        val command = if (paused) "Pause" else "Unpause"
        val response = transport.post(
            EndpointValidator.resolve(connection.baseUrl, "Sessions/$encodedSessionId/Playing/$command"),
            headers(connection),
            "{}",
        )
        response.requireSuccess(connection.kind)
    }

    fun details(connection: ServiceConnection, itemId: String): RemoteMediaDetails {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val encodedItemId = encodePathSegment(itemId)
        val userId = connection.userId.takeIf(String::isNotBlank)
            ?: runCatching { currentUserId(connection) }.getOrNull()
            ?: runCatching { preferredAvailableUserId(connection) }.getOrNull()
        val paths = buildList {
            userId?.let { add("Users/${encodePathSegment(it)}/Items/$encodedItemId") }
            add("Items/$encodedItemId")
        }
        var lastResponse: HttpResponse? = null
        paths.forEach { path ->
            val response = transport.get(EndpointValidator.resolve(connection.baseUrl, path), headers(connection))
            lastResponse = response
            if (response.statusCode in 200..299) return ServicePayloadParser.libraryDetails(response.body)
        }
        lastResponse?.requireSuccess(connection.kind)
        error("Fekk ikkje henta detaljar frå ${connection.kind.displayName}")
    }

    private fun currentUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users/Me"),
            headers(connection),
        )
        return if (response.statusCode in 200..299) ServicePayloadParser.currentUserId(response.body) else null
    }

    private fun preferredAvailableUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users"),
            headers(connection),
        )
        return if (response.statusCode in 200..299) {
            ServicePayloadParser.availableUserIds(response.body).firstOrNull()
        } else null
    }

    private fun libraryViews(connection: ServiceConnection, userId: String): List<RemoteLibraryView> {
        val paths = when (connection.kind) {
            ServiceKind.JELLYFIN -> listOf(
                "UserViews?userId=$userId&includeExternalContent=false&includeHidden=false",
                "Users/$userId/Views?IncludeExternalContent=false",
            )
            ServiceKind.EMBY -> listOf("Users/$userId/Views?IncludeExternalContent=false")
            else -> emptyList()
        }
        var lastResponse: HttpResponse? = null
        paths.forEach { path ->
            val response = transport.get(EndpointValidator.resolve(connection.baseUrl, path), headers(connection))
            lastResponse = response
            if (response.statusCode in 200..299) return ServicePayloadParser.libraryViews(response.body)
        }
        lastResponse?.requireSuccess(connection.kind)
        return emptyList()
    }

    private fun latestAcrossLibraries(
        connection: ServiceConnection,
        userId: String?,
        itemType: String,
        groupItems: Boolean,
        views: List<RemoteLibraryView>,
    ): List<RemoteLibraryItem> {
        val relevantViews = views.filter { it.supports(itemType) }.take(MAX_LIBRARY_VIEWS)
        if (relevantViews.isNotEmpty()) {
            val successfulGroups = relevantViews.mapNotNull { view ->
                runCatching {
                    getItems(
                        connection,
                        latestPaths(connection.kind, userId, itemType, groupItems, parentId = view.id),
                    )
                }.getOrNull()
            }
            if (successfulGroups.isNotEmpty()) {
                return interleave(successfulGroups).distinctBy(RemoteLibraryItem::id).take(LATEST_ITEM_LIMIT)
            }
        }
        return getItems(connection, latestPaths(connection.kind, userId, itemType, groupItems))
            .distinctBy(RemoteLibraryItem::id)
            .take(LATEST_ITEM_LIMIT)
    }

    private fun getItems(connection: ServiceConnection, paths: List<String>): List<RemoteLibraryItem> {
        require(paths.isNotEmpty()) { "Dette biblioteket krev ein profil-ID" }
        var lastResponse: HttpResponse? = null
        var authenticationFailure: HttpResponse? = null
        paths.forEach { path ->
            val response = transport.get(EndpointValidator.resolve(connection.baseUrl, path), headers(connection))
            lastResponse = response
            when (response.statusCode) {
                in 200..299 -> return ServicePayloadParser.libraryItems(response.body).map { item ->
                    item.copy(artworkUrl = item.artworkItemId?.let { artworkUrl(connection, it) })
                }
                401, 403 -> authenticationFailure = authenticationFailure ?: response
                else -> Unit // Try another route when this server version or profile needs one.
            }
        }
        (authenticationFailure ?: lastResponse)?.requireSuccess(connection.kind)
        return emptyList()
    }

    private fun latestPaths(
        kind: ServiceKind,
        userId: String?,
        itemType: String,
        groupItems: Boolean,
        parentId: String? = null,
    ): List<String> {
        val query = "Limit=12&Fields=ProductionYear,SeriesName,RunTimeTicks,Overview,Genres,CommunityRating,OfficialRating" +
            "&IncludeItemTypes=$itemType&GroupItems=$groupItems" +
            parentId?.let { "&ParentId=${encodePathSegment(it)}" }.orEmpty()
        return when (kind) {
            ServiceKind.JELLYFIN -> buildList {
                userId?.let {
                    add("Items/Latest?UserId=$it&$query&EnableUserData=true")
                    add("Users/$it/Items/Latest?$query&EnableUserData=true")
                }
                add("Items/Latest?$query")
            }
            ServiceKind.EMBY -> userId?.let {
                listOf("Users/$it/Items/Latest?$query&EnableUserData=true")
            }.orEmpty()
            ServiceKind.SEERR, ServiceKind.RADARR, ServiceKind.SONARR -> error("Medietenaren er ikkje støtta")
        }
    }

    private fun artworkUrl(connection: ServiceConnection, itemId: String): String =
        EndpointValidator.resolve(
            connection.baseUrl,
            "Items/${encodePathSegment(itemId)}/Images/Primary?maxHeight=720&quality=90",
        )

    private fun verifyConnection(connection: ServiceConnection) {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "System/Info"),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
    }

    private fun RemoteLibraryView.supports(itemType: String): Boolean = when (collectionType) {
        null, "", "mixed" -> true
        "movies" -> itemType == "Movie"
        "tvshows", "tv" -> itemType == "Episode"
        else -> false
    }

    private fun <T> interleave(groups: List<List<T>>): List<T> = buildList {
        val largestGroup = groups.maxOfOrNull(List<T>::size) ?: 0
        repeat(largestGroup) { index ->
            groups.forEach { group -> group.getOrNull(index)?.let(::add) }
        }
    }

    private companion object {
        const val LATEST_ITEM_LIMIT = 12
        const val MAX_LIBRARY_VIEWS = 12
    }
}

class QueueServiceClient(
    private val transport: JsonHttpTransport = HttpTransport(),
) {
    fun queue(connection: ServiceConnection): List<RemoteQueueItem> {
        require(connection.kind == ServiceKind.RADARR || connection.kind == ServiceKind.SONARR)
        val response = transport.get(
            EndpointValidator.resolve(
                connection.baseUrl,
                "api/v3/queue?page=1&pageSize=20&sortDirection=descending&includeUnknownMovieItems=true&includeUnknownSeriesItems=true",
            ),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.queue(response.body, connection.kind)
    }

    fun feed(connection: ServiceConnection): QueueServiceFeed {
        val queue = queue(connection)
        val start = encode(Instant.now().minus(1, ChronoUnit.DAYS).toString())
        val end = encode(Instant.now().plus(28, ChronoUnit.DAYS).toString())
        val options = if (connection.kind == ServiceKind.SONARR) {
            "&includeSeries=true&includeEpisodeImages=true"
        } else {
            ""
        }
        val response = transport.get(
            EndpointValidator.resolve(
                connection.baseUrl,
                "api/v3/calendar?start=$start&end=$end&unmonitored=false$options",
            ),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return QueueServiceFeed(
            queue = queue,
            upcoming = ServicePayloadParser.upcoming(response.body, connection.kind),
        )
    }
}

class SeerrServiceClient(
    private val transport: JsonHttpTransport = HttpTransport(),
) {
    fun search(connection: ServiceConnection, query: String): List<RemoteDiscoverItem> {
        require(connection.kind == ServiceKind.SEERR)
        val encodedQuery = encode(query).replace("+", "%20")
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/search?query=$encodedQuery&page=1&language=nb"),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.discover(response.body)
    }

    fun details(connection: ServiceConnection, mediaType: String, remoteId: Int): RemoteMediaDetails {
        require(connection.kind == ServiceKind.SEERR)
        require(mediaType == "movie" || mediaType == "tv")
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/$mediaType/$remoteId"),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.mediaDetails(response.body)
    }

    fun feed(connection: ServiceConnection): SeerrFeed {
        require(connection.kind == ServiceKind.SEERR)
        val requestHeaders = headers(connection)
        val discoverResponse = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/discover/trending?page=1&language=en"),
            requestHeaders,
        )
        discoverResponse.requireSuccess(connection.kind)

        val requestsResponse = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request?take=20&skip=0&sort=added"),
            requestHeaders,
        )
        requestsResponse.requireSuccess(connection.kind)
        val requests = ServicePayloadParser.requests(requestsResponse.body).mapIndexed { index, request ->
            if (index >= REQUEST_DETAIL_LIMIT || request.remoteId == null ||
                (request.title != null && request.artworkUrl != null)
            ) {
                request
            } else {
                val detailsResponse = transport.get(
                    EndpointValidator.resolve(connection.baseUrl, "api/v1/${request.mediaType}/${request.remoteId}"),
                    requestHeaders,
                )
                if (detailsResponse.statusCode in 200..299) {
                    val details = ServicePayloadParser.mediaDetails(detailsResponse.body)
                    request.copy(
                        title = request.title ?: details.title,
                        artworkUrl = request.artworkUrl ?: details.artworkUrl,
                    )
                } else {
                    request
                }
            }
        }
        return SeerrFeed(
            discover = ServicePayloadParser.discover(discoverResponse.body),
            requests = requests,
        )
    }

    fun request(connection: ServiceConnection, mediaType: String, remoteId: Int) {
        require(connection.kind == ServiceKind.SEERR)
        require(mediaType == "movie" || mediaType == "tv")
        val body = buildJsonObject {
            put("mediaType", mediaType)
            put("mediaId", remoteId)
            if (mediaType == "tv") put("seasons", "all")
        }.toString()
        val response = transport.post(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request"),
            headers(connection),
            body,
        )
        response.requireSuccess(connection.kind)
    }

    private companion object {
        const val REQUEST_DETAIL_LIMIT = 4
    }
}

private fun headers(connection: ServiceConnection): Map<String, String> = when (connection.kind) {
    ServiceKind.JELLYFIN, ServiceKind.EMBY -> mapOf("X-Emby-Token" to connection.token)
    ServiceKind.SEERR, ServiceKind.RADARR, ServiceKind.SONARR -> mapOf("X-Api-Key" to connection.token)
}

private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

private fun encodePathSegment(value: String): String = encode(value).replace("+", "%20")

private fun HttpResponse.requireSuccess(kind: ServiceKind) {
    when (statusCode) {
        in 200..299 -> Unit
        401, 403 -> error("${kind.displayName} avviste API-nøkkelen")
        404 -> error("${kind.displayName} tilbyr ikkje dette API-endepunktet")
        408, 429 -> error("${kind.displayName} er mellombels oppteken")
        in 500..599 -> error("${kind.displayName} er utilgjengeleg no")
        else -> error("${kind.displayName} svara med status $statusCode")
    }
}
