package app.reelstack.data.network

import app.reelstack.BuildConfig
import app.reelstack.data.model.ConnectionTestResult
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.ViewerAccess
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.canRequestType
import app.reelstack.data.model.isExcludedHomeLibrary
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
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
    val recentReleases: List<RemoteUpcomingItem> = emptyList(),
    val releasesFailed: Boolean = false,
    val releaseCandidates: List<RemoteLibraryItem> = emptyList(),
)

data class QueueServiceFeed(
    val queue: List<RemoteQueueItem>,
    val upcoming: List<RemoteUpcomingItem>,
    val recentReleases: List<RemoteUpcomingItem> = emptyList(),
)

data class ServiceAuthentication(
    val accessToken: String,
    val userId: String,
)

data class QuickConnectChallenge(
    val secret: String,
    val code: String,
    val authenticated: Boolean,
    val cookies: String = "",
)

class JellyfinAuthenticationClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val deviceId: String = "homereel-android",
) {
    fun authenticate(baseUrl: String, username: String, password: String): ServiceAuthentication {
        val body = buildJsonObject {
            put("Username", username)
            put("Pw", password)
        }.toString()
        val authorization = jellyfinAuthorization(deviceId)
        val response = transport.post(
            EndpointValidator.resolve(baseUrl, "Users/AuthenticateByName"),
            mapOf("Authorization" to authorization),
            body,
        )

        when (response.statusCode) {
            401, 403 -> error("Feil brukarnamn eller passord")
            404 -> error("Fann Jellyfin, men innlogging med brukarkonto er ikkje tilgjengeleg")
            in 200..299 -> Unit
            in 500..599 -> error(
                "Jellyfin klarte ikkje å opprette innloggingsøkta (tenarfeil ${response.statusCode})",
            )
            else -> error("Jellyfin svara med status ${response.statusCode}")
        }

        return parseAuthentication(response.body)
    }

    fun initiateQuickConnect(baseUrl: String): QuickConnectChallenge {
        val response = transport.post(
            EndpointValidator.resolve(baseUrl, "QuickConnect/Initiate"),
            mapOf("Authorization" to jellyfinAuthorization(deviceId)),
            "{}",
        )
        when (response.statusCode) {
            in 200..299 -> Unit
            404 -> error("Denne Jellyfin-tenaren støttar ikkje Quick Connect")
            401, 403 -> error("Quick Connect er ikkje slått på i Jellyfin")
            in 500..599 -> error("Quick Connect er ikkje slått på, eller Jellyfin klarte ikkje å lage ein kode")
            else -> error("Jellyfin svara med status ${response.statusCode}")
        }
        return parseQuickConnect(response.body)
    }

    fun quickConnectState(baseUrl: String, secret: String): QuickConnectChallenge {
        val response = transport.get(
            EndpointValidator.resolve(baseUrl, "QuickConnect/Connect?secret=${encode(secret)}"),
            mapOf("Authorization" to jellyfinAuthorization(deviceId)),
        )
        when (response.statusCode) {
            in 200..299 -> Unit
            404 -> error("Quick Connect-koden er ikkje lenger gyldig")
            401, 403 -> error("Jellyfin avviste Quick Connect-førespurnaden")
            else -> error("Jellyfin svara med status ${response.statusCode}")
        }
        return parseQuickConnect(response.body)
    }

    fun authenticateWithQuickConnect(baseUrl: String, secret: String): ServiceAuthentication {
        val body = buildJsonObject { put("Secret", secret) }.toString()
        val response = transport.post(
            EndpointValidator.resolve(baseUrl, "Users/AuthenticateWithQuickConnect"),
            mapOf("Authorization" to jellyfinAuthorization(deviceId)),
            body,
        )
        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> error("Quick Connect-koden vart ikkje godkjend")
            404 -> error("Quick Connect-koden er ikkje lenger gyldig")
            else -> error("Jellyfin svara med status ${response.statusCode}")
        }
        return parseAuthentication(response.body)
    }

    private fun parseAuthentication(body: String): ServiceAuthentication {
        val root = parseObject(body, "Jellyfin sende eit ugyldig innloggingssvar")
        val token = root["AccessToken"]?.jsonPrimitive?.contentOrNull
            ?: root["accessToken"]?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake eit tilgangsteikn")
        val user = root["User"]?.jsonObject ?: root["user"]?.jsonObject
        val userId = user?.get("Id")?.jsonPrimitive?.contentOrNull
            ?: user?.get("id")?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake ein profil-ID")
        return ServiceAuthentication(accessToken = token, userId = userId)
    }

    private fun parseQuickConnect(body: String): QuickConnectChallenge {
        val root = parseObject(body, "Jellyfin sende eit ugyldig Quick Connect-svar")
        val secret = root["Secret"]?.jsonPrimitive?.contentOrNull
            ?: root["secret"]?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake ein Quick Connect-hemmelegheit")
        val code = root["Code"]?.jsonPrimitive?.contentOrNull
            ?: root["code"]?.jsonPrimitive?.contentOrNull
            ?: error("Jellyfin sende ikkje tilbake ein Quick Connect-kode")
        val authenticated = root["Authenticated"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: root["authenticated"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: false
        return QuickConnectChallenge(secret = secret, code = code, authenticated = authenticated)
    }

    private fun parseObject(body: String, message: String) =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrElse { error(message) }
}

class ServiceConnectionTester(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val deviceId: String = "homereel-android",
) {
    fun test(connection: ServiceConnection): ConnectionTestResult {
        val probe = when (connection.kind) {
            ServiceKind.JELLYFIN -> ServiceProbe("System/Info", "Authorization")
            ServiceKind.EMBY -> ServiceProbe("System/Info", "X-Emby-Token")
            ServiceKind.SEERR -> ServiceProbe("api/v1/auth/me", "X-Api-Key")
            ServiceKind.RADARR, ServiceKind.SONARR -> ServiceProbe("api/v3/system/status", "X-Api-Key")
        }
        val endpoint = EndpointValidator.resolve(connection.baseUrl, probe.path)
        lateinit var response: HttpResponse
        val elapsed = measureTimeMillis {
            response = transport.get(
                endpoint,
                if (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.SEERR) {
                    headers(connection, deviceId)
                } else {
                    mapOf(probe.headerName to connection.token)
                },
            )
        }

        return when (response.statusCode) {
            in 200..299 -> ConnectionTestResult(
                success = true,
                latencyMs = elapsed,
                message = extractVersion(response.body)?.let { "Tilkopla · v$it" } ?: "Tilkopla",
            )
            401, 403 -> ConnectionTestResult(false, elapsed, if (connection.sessionCookie) "Seerr-økta er utgått. Logg inn på nytt." else "API-nøkkelen vart avvist")
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
    private val deviceId: String = "homereel-android",
) {
    fun sessions(connection: ServiceConnection, access: ViewerAccess = localAccess(connection)): List<RemotePlayback> {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val all = access.canSeeAllSessions(connection.kind) || (access.isAdmin && access.accounts[connection.kind] == null)
        val ownId = access.ownMediaUser(connection.kind)
        if (!all && ownId == null) return emptyList()
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Sessions"),
            headers(connection, deviceId),
        )
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.playbackSessions(response.body).filter { all || it.userId == ownId }.map { item ->
            item.copy(artworkUrl = item.artworkItemId?.let { artworkUrl(connection, it) })
        }
    }

    private fun localAccess(connection: ServiceConnection) = ViewerAccess(false,
        runCatching { AccountProfileClient(deviceId, transport).load(connection) }.getOrNull()
            ?.let { mapOf(connection.kind to it) }.orEmpty())

    fun feed(connection: ServiceConnection, access: ViewerAccess = localAccess(connection)): MediaServerFeed {
        val warnings = mutableListOf<String>()
        val sessionsResult = runCatching { sessions(connection, access) }
        val sessions = sessionsResult.getOrElse {
            warnings += "Avspelingsøkter er utilgjengelege"
            emptyList()
        }
        val allowFallback = !access.seerrConfigured || access.isAdmin
        val userId = access.ownMediaUser(connection.kind) ?: if (allowFallback) connection.userId.takeIf { it.isNotBlank() }
            ?: runCatching { currentUserId(connection) }.getOrNull()
            ?: runCatching { preferredAvailableUserId(connection) }.getOrNull() else null
        if (userId == null && !allowFallback) return MediaServerFeed(sessions, emptyList(), emptyList(), null)

        val encodedUserId = userId?.let(::encodePathSegment)
        val viewsResult = runCatching { libraryViews(connection, requireNotNull(encodedUserId) { "Profil-ID manglar" }) }
        val moviesResult = runCatching {
            latestAcrossLibraries(
                connection = connection,
                userId = encodedUserId,
                itemType = "Movie",
                groupItems = false,
                views = viewsResult.getOrThrow(),
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
                groupItems = false,
                views = viewsResult.getOrThrow(),
            )
        }
        val series = seriesResult.getOrElse {
            warnings += "Nyleg lagde til seriar er utilgjengelege"
            emptyList()
        }

        val releasesResult = runCatching {
            releasedAcrossLibraries(connection, requireNotNull(encodedUserId), viewsResult.getOrThrow())
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
            recentReleases = releasesResult.getOrDefault(emptyList()).filter { it.mediaType == "Episode" }
                .mapNotNull { libraryRelease(it, connection.kind, ReleaseWindow()) },
            releasesFailed = releasesResult.isFailure,
            releaseCandidates = releasesResult.getOrDefault(emptyList()).filter { it.mediaType == "Movie" && it.available },
        )
    }

    private fun releasedAcrossLibraries(
        connection: ServiceConnection,
        userId: String,
        views: List<RemoteLibraryView>,
    ): List<RemoteLibraryItem> {
        val window = ReleaseWindow()
        val relevant = views.filter { !isExcludedHomeLibrary(it.name) && (it.supports("Movie") || it.supports("Episode")) }
            .take(MAX_LIBRARY_VIEWS)
        if (relevant.isEmpty()) return emptyList()
        val groups = relevant.flatMap { view -> listOf("Movie", "Episode").filter { view.supports(it) }.map { view to it } }.mapNotNull { (view, type) ->
            // A movie can reach digital months after cinema. Query candidates, then verify the
            // actual digital date through Seerr. Episodes use their own air date directly.
            val start = if (type == "Movie") window.today.minusYears(1) else window.start
            val query = "ParentId=${encodePathSegment(view.id)}&Recursive=true&IncludeItemTypes=$type" +
                "&SortBy=PremiereDate&SortOrder=Descending&Limit=60&IsMissing=false&IsVirtualUnaired=false" +
                "&MinPremiereDate=$start&MaxPremiereDate=${window.today}T23:59:59Z" +
                "&Fields=Overview,Genres,ProviderIds,PrimaryImageAspectRatio&EnableImages=true&EnableUserData=false"
            runCatching {
                val paths = if (connection.kind == ServiceKind.JELLYFIN) {
                    listOf("Items?UserId=$userId&$query", "Users/$userId/Items?$query")
                } else listOf("Users/$userId/Items?$query")
                getItems(connection, paths)
            }.getOrNull()
        }
        check(groups.isNotEmpty()) { "Fekk ikkje henta nye utgjevingar frå biblioteka" }
        return groups.flatten().distinctBy(RemoteLibraryItem::id).filter { it.available }
    }

    fun setPaused(connection: ServiceConnection, sessionId: String, paused: Boolean) {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val encodedSessionId = java.net.URLEncoder.encode(sessionId, Charsets.UTF_8.name())
        val command = if (paused) "Pause" else "Unpause"
        val response = transport.post(
            EndpointValidator.resolve(connection.baseUrl, "Sessions/$encodedSessionId/Playing/$command"),
            headers(connection, deviceId),
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
            val response = transport.get(
                EndpointValidator.resolve(connection.baseUrl, path),
                headers(connection, deviceId),
            )
            lastResponse = response
            if (response.statusCode in 200..299) return ServicePayloadParser.libraryDetails(response.body)
        }
        lastResponse?.requireSuccess(connection.kind)
        error("Fekk ikkje henta detaljar frå ${connection.kind.displayName}")
    }

    private fun currentUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users/Me"),
            headers(connection, deviceId),
        )
        return if (response.statusCode in 200..299) ServicePayloadParser.currentUserId(response.body) else null
    }

    private fun preferredAvailableUserId(connection: ServiceConnection): String? {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "Users"),
            headers(connection, deviceId),
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
            val response = transport.get(
                EndpointValidator.resolve(connection.baseUrl, path),
                headers(connection, deviceId),
            )
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
        if (views.isEmpty()) return emptyList()
        val relevantViews = views.filter { it.supports(itemType) && !isExcludedHomeLibrary(it.name) }.take(MAX_LIBRARY_VIEWS)
        // Never fall back to an unscoped query that could reintroduce excluded libraries.
        if (relevantViews.isEmpty()) return emptyList()
        val successfulGroups = relevantViews.mapNotNull { view ->
            runCatching {
                getItems(connection, latestPaths(connection.kind, userId, itemType, groupItems, parentId = view.id))
            }.getOrNull()
        }
        if (successfulGroups.isEmpty()) error("Fekk ikkje oppdatert dei valde biblioteka")
        return interleave(successfulGroups).distinctBy(RemoteLibraryItem::id).take(LATEST_ITEM_LIMIT)
    }

    private fun getItems(connection: ServiceConnection, paths: List<String>): List<RemoteLibraryItem> {
        require(paths.isNotEmpty()) { "Dette biblioteket krev ein profil-ID" }
        var lastResponse: HttpResponse? = null
        var authenticationFailure: HttpResponse? = null
        paths.forEach { path ->
            val response = transport.get(
                EndpointValidator.resolve(connection.baseUrl, path),
                headers(connection, deviceId),
            )
            lastResponse = response
            when (response.statusCode) {
                in 200..299 -> return ServicePayloadParser.libraryItems(response.body).map { item ->
                    item.copy(
                        artworkUrl = item.artworkItemId?.let {
                            artworkUrl(connection, it, item.artworkImageType)
                        },
                    )
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
        val query = "Limit=12&Fields=Overview,Genres,PrimaryImageAspectRatio,Studios,Taglines" +
            "&EnableImages=true&ImageTypeLimit=1&EnableImageTypes=Primary,Thumb" +
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

    private fun artworkUrl(connection: ServiceConnection, itemId: String, imageType: String = "Primary"): String =
        EndpointValidator.resolve(
            connection.baseUrl,
            if (imageType.equals("Thumb", ignoreCase = true)) {
                "Items/${encodePathSegment(itemId)}/Images/Thumb?maxWidth=960&quality=90"
            } else {
                "Items/${encodePathSegment(itemId)}/Images/Primary?maxHeight=720&quality=90"
            },
        )

    private fun verifyConnection(connection: ServiceConnection) {
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "System/Info"),
            headers(connection, deviceId),
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

    fun feed(connection: ServiceConnection, includeQueue: Boolean = true): QueueServiceFeed {
        val queue = if (includeQueue) queue(connection) else emptyList()
        // The calendar powers both "Kjem snart" and the date-based release rail. Keep a finite
        // history window so an old library title can never reappear as newly available.
        val now = Instant.now()
        val windowStart = now.minus(28, ChronoUnit.DAYS)
        val start = encode(windowStart.toString())
        val end = encode(now.plus(28, ChronoUnit.DAYS).toString())
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
        val releases = ServicePayloadParser.upcoming(response.body, connection.kind, windowStart)
        return QueueServiceFeed(
            queue = queue,
            upcoming = releases.filter { release ->
                ServicePayloadParser.calendarInstant(release.dateTime)?.let { !it.isBefore(now) } == true
            },
            recentReleases = releases.filter { release ->
                ServicePayloadParser.calendarInstant(release.dateTime)?.let { it.isBefore(now) } == true
            },
        )
    }
}

class SeerrServiceClient(
    private val transport: JsonHttpTransport = HttpTransport(),
    private val nanoTime: () -> Long = System::nanoTime,
) {
    // Cache only display metadata; request and availability status always come from fresh feed responses.
    private val requestMetadataCache = LinkedHashMap<RequestMetadataKey, CachedRequestMetadata>(16, 0.75f, true)

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

    fun details(
        connection: ServiceConnection,
        mediaType: String,
        remoteId: Int,
        language: String = "nb",
        includeOverviewFallback: Boolean = true,
    ): RemoteMediaDetails {
        require(connection.kind == ServiceKind.SEERR)
        require(mediaType == "movie" || mediaType == "tv")
        val requestedLanguage = language.trim().ifBlank { "nb" }
        val requestHeaders = headers(connection)
        fun fetch(detailLanguage: String): RemoteMediaDetails {
            val response = transport.get(
                EndpointValidator.resolve(
                    connection.baseUrl,
                    "api/v1/$mediaType/$remoteId?language=${encode(detailLanguage)}",
                ),
                requestHeaders,
            )
            response.requireSuccess(connection.kind)
            return ServicePayloadParser.mediaDetails(response.body)
        }

        val details = fetch(requestedLanguage)
        if (!includeOverviewFallback || !details.overview.isNullOrBlank() || requestedLanguage.substringBefore('-').equals("en", ignoreCase = true)) {
            return details
        }

        // Translation enrichment is optional; keep the primary metadata and availability even if it fails.
        val englishOverview = runCatching { fetch("en").overview }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        return if (englishOverview != null) details.copy(overview = englishOverview) else details
    }

    fun feed(connection: ServiceConnection, actor: ServiceAccount = AccountProfileClient(transport = transport).load(connection)): SeerrFeed {
        require(connection.kind == ServiceKind.SEERR)
        val requestHeaders = headers(connection)
        val discoverResponse = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/discover/trending?page=1&language=en"),
            requestHeaders,
        )
        discoverResponse.requireSuccess(connection.kind)

        val requestsResponse = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request?take=20&skip=0&sort=added" + if (actor.isAdmin) "" else "&requestedBy=${encode(actor.id)}"),
            requestHeaders,
        )
        requestsResponse.requireSuccess(connection.kind)
        val discover = ServicePayloadParser.discover(discoverResponse.body)
        val discoveredByMedia = discover.associateBy { it.mediaType to it.remoteId }
        val scope = RequestMetadataScope(
            endpoint = EndpointValidator.normalizeBaseUrl(connection.baseUrl),
            authFingerprint = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(connection.token.toByteArray(Charsets.UTF_8)),
            ),
            sessionCookie = connection.sessionCookie,
            userId = connection.userId,
        )
        // Failures are deduplicated only within this refresh, so the next refresh can retry.
        val detailCache = mutableMapOf<Pair<String, Int>, RequestMetadata?>()
        var detailLookups = 0
        val requests = ServicePayloadParser.requests(requestsResponse.body).filter { actor.isAdmin || it.ownerId == actor.id }.map { request ->
            val remoteId = request.remoteId ?: return@map request
            val key = request.mediaType to remoteId
            val discovered = discoveredByMedia[key]
            val enriched = request.copy(
                title = request.title?.takeIf { it.isNotBlank() } ?: discovered?.title,
                artworkUrl = request.artworkUrl ?: discovered?.artworkUrl,
            )
            if ((!enriched.title.isNullOrBlank() && enriched.artworkUrl != null) ||
                request.mediaType !in setOf("movie", "tv")
            ) return@map enriched

            if (key !in detailCache) {
                val cacheKey = RequestMetadataKey(scope, request.mediaType, remoteId)
                val cached = cachedRequestMetadata(cacheKey)
                if (cached != null) {
                    detailCache[key] = cached
                } else if (detailLookups < REQUEST_DETAIL_LIMIT) {
                    detailLookups++
                    detailCache[key] = runCatching {
                        val response = transport.get(
                            EndpointValidator.resolve(connection.baseUrl, "api/v1/${request.mediaType}/$remoteId"),
                            requestHeaders,
                        )
                        response.requireSuccess(connection.kind)
                        val details = ServicePayloadParser.mediaDetails(response.body)
                        RequestMetadata(details.title?.takeIf { it.isNotBlank() }, details.artworkUrl)
                            .takeIf { it.title != null || it.artworkUrl != null }
                            ?.also { cacheRequestMetadata(cacheKey, it) }
                    }.getOrNull()
                }
            }
            val details = detailCache[key]
            enriched.copy(
                title = enriched.title?.takeIf { it.isNotBlank() } ?: details?.title,
                artworkUrl = enriched.artworkUrl ?: details?.artworkUrl,
            )
        }
        return SeerrFeed(
            discover = discover,
            requests = requests,
        )
    }

    fun requests(connection: ServiceConnection, userId: String): List<RemoteRequest> {
        require(userId.toIntOrNull()?.let { it > 0 } == true)
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl, "api/v1/request?take=100&skip=0&sort=added&requestedBy=${encode(userId)}"), headers(connection))
        response.requireSuccess(connection.kind)
        return ServicePayloadParser.requests(response.body)
    }

    fun request(connection: ServiceConnection, mediaType: String, remoteId: Int, expectedUserId: String = connection.userId, seasons: Set<Int> = emptySet()) {
        require(connection.kind == ServiceKind.SEERR)
        require(mediaType == "movie" || mediaType == "tv")
        require(connection.sessionCookie && expectedUserId.isNotBlank()) {
            "Logg inn personleg i Seerr. Ein administratornøkkel kan ikkje sende førespurnader som deg."
        }
        val actor = AccountProfileClient(transport = transport).load(connection)
        check(actor.isPersonal && actor.id == expectedUserId) { "Seerr-kontoen er endra. Sjekk innlogginga før du sender." }
        check(actor.canRequestType(mediaType)) { "Seerr-kontoen kan ikkje leggje til denne medietypen." }
        if (mediaType == "tv") {
            require(seasons.isNotEmpty() && seasons.all { it >= 0 }) { "Vel minst éin sesong." }
            val fresh = details(connection, mediaType, remoteId)
            check(fresh.seerrStatus != 6 && seasons.all { number -> fresh.seasons.any { it.number == number && it.canRequest } }) {
                "Sesongane er endra eller alt førespurde. Opne førespurnaden på nytt."
            }
        }
        val body = buildJsonObject {
            put("mediaType", mediaType)
            put("mediaId", remoteId)
            if (mediaType == "tv") put("seasons", kotlinx.serialization.json.JsonArray(seasons.sorted().map { kotlinx.serialization.json.JsonPrimitive(it) }))
        }.toString()
        val response = transport.post(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request"),
            headers(connection),
            body,
        )
        response.requireSuccess(connection.kind)
        check(response.statusCode != 202) { "Ingen nye sesongar vart lagde til. Sjekk sesongane på nytt." }
    }

    private fun cachedRequestMetadata(key: RequestMetadataKey): RequestMetadata? = synchronized(requestMetadataCache) {
        val cached = requestMetadataCache[key] ?: return@synchronized null
        if (nanoTime() - cached.createdAtNanos >= REQUEST_METADATA_TTL_NANOS) {
            requestMetadataCache.remove(key)
            null
        } else {
            cached.metadata
        }
    }

    private fun cacheRequestMetadata(key: RequestMetadataKey, metadata: RequestMetadata) = synchronized(requestMetadataCache) {
        val now = nanoTime()
        requestMetadataCache.entries.removeAll { now - it.value.createdAtNanos >= REQUEST_METADATA_TTL_NANOS }
        requestMetadataCache[key] = CachedRequestMetadata(metadata, now)
        while (requestMetadataCache.size > REQUEST_METADATA_CACHE_LIMIT) {
            requestMetadataCache.entries.iterator().run { next(); remove() }
        }
    }

    private data class RequestMetadataScope(
        val endpoint: String,
        val authFingerprint: String,
        val sessionCookie: Boolean,
        val userId: String,
    )

    private data class RequestMetadataKey(val scope: RequestMetadataScope, val mediaType: String, val remoteId: Int)
    private data class RequestMetadata(val title: String?, val artworkUrl: String?)
    private data class CachedRequestMetadata(val metadata: RequestMetadata, val createdAtNanos: Long)

    private companion object {
        const val REQUEST_DETAIL_LIMIT = 20
        const val REQUEST_METADATA_CACHE_LIMIT = 64
        const val REQUEST_METADATA_TTL_NANOS = 600_000_000_000L
    }
}

private fun headers(
    connection: ServiceConnection,
    jellyfinDeviceId: String = "homereel-android",
): Map<String, String> = when (connection.kind) {
    ServiceKind.JELLYFIN -> mapOf(
        "Authorization" to jellyfinAuthorization(jellyfinDeviceId, connection.token),
    )
    ServiceKind.EMBY -> mapOf("X-Emby-Token" to connection.token)
    ServiceKind.SEERR -> if (connection.sessionCookie) seerrCookieHeaders(connection.token) else mapOf("X-Api-Key" to connection.token)
    ServiceKind.RADARR, ServiceKind.SONARR -> mapOf("X-Api-Key" to connection.token)
}

internal fun jellyfinAuthorization(deviceId: String, token: String? = null): String = buildString {
    append("MediaBrowser Client=\"HomeReel\", Device=\"Android\", ")
    append("DeviceId=\"")
    append(deviceId)
    append("\", Version=\"")
    append(BuildConfig.VERSION_NAME)
    append('"')
    token?.let {
        append(", Token=\"")
        append(it)
        append('"')
    }
}

private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())

private fun encodePathSegment(value: String): String = encode(value).replace("+", "%20")

private fun HttpResponse.requireSuccess(kind: ServiceKind) {
    when (statusCode) {
        in 200..299 -> Unit
        401 -> error(if (kind == ServiceKind.SEERR) "Logg inn på Seerr på nytt i Innstillingar." else "${kind.displayName} avviste API-nøkkelen")
        403 -> error(if (kind == ServiceKind.SEERR) "Seerr gav ikkje kontoen tilgang til denne handlinga." else "${kind.displayName} avviste API-nøkkelen")
        404 -> error("${kind.displayName} tilbyr ikkje dette API-endepunktet")
        408, 429 -> error("${kind.displayName} er mellombels oppteken")
        in 500..599 -> error("${kind.displayName} er utilgjengeleg no")
        else -> error("${kind.displayName} svara med status $statusCode")
    }
}
