package app.reelstack.data.network

import app.reelstack.data.model.ConnectionTestResult
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import kotlinx.serialization.json.buildJsonObject
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
    val continueWatching: List<RemoteLibraryItem>,
    val recentlyAdded: List<RemoteLibraryItem>,
    val warning: String? = null,
)

data class QueueServiceFeed(
    val queue: List<RemoteQueueItem>,
    val upcoming: List<RemoteUpcomingItem>,
)

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
                message = extractVersion(response.body)?.let { "Connected · v$it" } ?: "Connected",
            )
            401, 403 -> ConnectionTestResult(false, elapsed, "The API key was rejected")
            404 -> ConnectionTestResult(false, elapsed, "Service found, but its API path was not available")
            else -> ConnectionTestResult(false, elapsed, "Server returned ${response.statusCode}")
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
            warnings += "Playback sessions unavailable"
            emptyList()
        }
        val userId = connection.userId.takeIf { it.isNotBlank() }
            ?: runCatching { currentUserId(connection) }.getOrNull()
            ?: sessions.firstOrNull()?.userId
            ?: runCatching { firstAvailableUserId(connection) }.getOrNull()

        val resumeResult = userId?.let { resolvedUserId ->
            runCatching { getItems(connection, resumePaths(connection.kind, encodePathSegment(resolvedUserId))) }
        }
        val resume = resumeResult?.getOrElse {
            warnings += "Continue watching could not refresh"
            emptyList()
        }.orEmpty()

        val latestResult = runCatching {
            getItems(connection, latestPaths(connection.kind, userId?.let(::encodePathSegment)))
        }
        val latest = latestResult.getOrElse {
            warnings += "Recently added could not refresh"
            emptyList()
        }

        if (userId == null) warnings += "Add a Profile ID for personal media"
        val anyFeedCallSucceeded = sessionsResult.isSuccess || resumeResult?.isSuccess == true || latestResult.isSuccess
        if (!anyFeedCallSucceeded) {
            verifyConnection(connection)
            warnings += "Media sections unavailable"
        }
        return MediaServerFeed(
            sessions = sessions,
            continueWatching = resume,
            recentlyAdded = latest,
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

    private fun currentUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users/Me"),
            headers(connection),
        )
        return if (response.statusCode in 200..299) ServicePayloadParser.currentUserId(response.body) else null
    }

    private fun firstAvailableUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users"),
            headers(connection),
        )
        return if (response.statusCode in 200..299) {
            ServicePayloadParser.availableUserIds(response.body).firstOrNull()
        } else null
    }

    private fun getItems(connection: ServiceConnection, paths: List<String>): List<RemoteLibraryItem> {
        require(paths.isNotEmpty()) { "A Profile ID is required for this library" }
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

    private fun resumePaths(kind: ServiceKind, userId: String): List<String> = when (kind) {
        ServiceKind.JELLYFIN -> listOf(
            "UserItems/Resume?userId=$userId&limit=12&fields=ProductionYear,SeriesName,RunTimeTicks&mediaTypes=Video&enableUserData=true",
            "Users/$userId/Items/Resume?Limit=12&Fields=ProductionYear,SeriesName,RunTimeTicks&MediaTypes=Video&EnableUserData=true",
        )
        ServiceKind.EMBY -> listOf(
            "Users/$userId/Items/Resume?Limit=12&Fields=ProductionYear,SeriesName,RunTimeTicks&MediaTypes=Video&EnableUserData=true",
        )
        ServiceKind.SEERR, ServiceKind.RADARR, ServiceKind.SONARR -> error("Unsupported media server")
    }

    private fun latestPaths(kind: ServiceKind, userId: String?): List<String> = when (kind) {
        ServiceKind.JELLYFIN -> buildList {
            userId?.let {
                add("Items/Latest?userId=$it&limit=12&fields=ProductionYear,SeriesName,RunTimeTicks&enableUserData=true&groupItems=true")
                add("Users/$it/Items/Latest?Limit=12&Fields=ProductionYear,SeriesName,RunTimeTicks&EnableUserData=true&GroupItems=true")
            }
            add("Items/Latest?limit=12&fields=ProductionYear,SeriesName,RunTimeTicks&groupItems=true")
        }
        ServiceKind.EMBY -> userId?.let {
            listOf("Users/$it/Items/Latest?Limit=12&Fields=ProductionYear,SeriesName,RunTimeTicks&EnableUserData=true&GroupItems=true")
        }.orEmpty()
        ServiceKind.SEERR, ServiceKind.RADARR, ServiceKind.SONARR -> error("Unsupported media server")
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
        401, 403 -> error("${kind.displayName} rejected its API key")
        404 -> error("${kind.displayName} does not provide this API endpoint")
        408, 429 -> error("${kind.displayName} is temporarily busy")
        in 500..599 -> error("${kind.displayName} is currently unavailable")
        else -> error("${kind.displayName} returned status $statusCode")
    }
}
