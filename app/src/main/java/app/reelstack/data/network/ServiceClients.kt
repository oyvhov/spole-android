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
import java.util.Locale
import kotlin.system.measureTimeMillis

private data class ServiceProbe(
    val path: String,
    val headerName: String,
)

/** One page of Seerr results, so Discover can ask for the next one instead of truncating at 20. */
data class SeerrSearchPage(
    val items: List<RemoteDiscoverItem>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

data class SeerrFeed(
    val discover: List<RemoteDiscoverItem>,
    val requests: List<RemoteRequest>,
)

data class MediaServerFeed(
    val sessions: List<RemotePlayback>,
    val recentMovies: List<RemoteLibraryItem>,
    val recentSeries: List<RemoteLibraryItem>,
    /** Partly watched titles, newest activity first, straight from the server's own resume list. */
    val resume: List<RemoteLibraryItem> = emptyList(),
    val nextUp: List<RemoteLibraryItem> = emptyList(),
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
        val response = contacting(JELLYFIN) {
            transport.post(
                EndpointValidator.resolve(baseUrl, "Users/AuthenticateByName"),
                mapOf("Authorization" to authorization),
                body,
            )
        }

        when (response.statusCode) {
            401, 403 -> serviceError("Feil brukarnamn eller passord")
            404 -> serviceError("Fann Jellyfin, men innlogging med brukarkonto er ikkje tilgjengeleg")
            in 200..299 -> Unit
            in 500..599 -> serviceError(
                "Jellyfin klarte ikkje å opprette innloggingsøkta (tenarfeil ${response.statusCode})",
            )
            in 300..399 -> serviceError(redirectMessage(ServiceKind.JELLYFIN, response.location))
            else -> serviceError("Jellyfin svara med status ${response.statusCode}")
        }

        return parseAuthentication(response.body)
    }

    fun initiateQuickConnect(baseUrl: String): QuickConnectChallenge {
        val response = contacting(JELLYFIN) {
            transport.post(
                EndpointValidator.resolve(baseUrl, "QuickConnect/Initiate"),
                mapOf("Authorization" to jellyfinAuthorization(deviceId)),
                "{}",
            )
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            404 -> serviceError("Denne Jellyfin-tenaren støttar ikkje Quick Connect")
            401, 403 -> serviceError("Quick Connect er ikkje slått på i Jellyfin")
            in 500..599 -> serviceError("Quick Connect er ikkje slått på, eller Jellyfin klarte ikkje å lage ein kode")
            else -> serviceError("Jellyfin svara med status ${response.statusCode}")
        }
        return parseQuickConnect(response.body)
    }

    fun quickConnectState(baseUrl: String, secret: String): QuickConnectChallenge {
        val response = contacting(JELLYFIN) {
            transport.get(
                EndpointValidator.resolve(baseUrl, "QuickConnect/Connect?secret=${encode(secret)}"),
                mapOf("Authorization" to jellyfinAuthorization(deviceId)),
            )
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            404 -> serviceError("Quick Connect-koden er ikkje lenger gyldig")
            401, 403 -> serviceError("Jellyfin avviste Quick Connect-førespurnaden")
            else -> serviceError("Jellyfin svara med status ${response.statusCode}")
        }
        return parseQuickConnect(response.body)
    }

    fun authenticateWithQuickConnect(baseUrl: String, secret: String): ServiceAuthentication {
        val body = buildJsonObject { put("Secret", secret) }.toString()
        val response = contacting(JELLYFIN) {
            transport.post(
                EndpointValidator.resolve(baseUrl, "Users/AuthenticateWithQuickConnect"),
                mapOf("Authorization" to jellyfinAuthorization(deviceId)),
                body,
            )
        }
        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> serviceError("Quick Connect-koden vart ikkje godkjend")
            404 -> serviceError("Quick Connect-koden er ikkje lenger gyldig")
            else -> serviceError("Jellyfin svara med status ${response.statusCode}")
        }
        return parseAuthentication(response.body)
    }

    private fun parseAuthentication(body: String): ServiceAuthentication {
        val root = parseObject(body, "Jellyfin sende eit ugyldig innloggingssvar")
        val token = root["AccessToken"]?.jsonPrimitive?.contentOrNull
            ?: root["accessToken"]?.jsonPrimitive?.contentOrNull
            ?: serviceError("Jellyfin sende ikkje tilbake eit tilgangsteikn")
        val user = root["User"]?.jsonObject ?: root["user"]?.jsonObject
        val userId = user?.get("Id")?.jsonPrimitive?.contentOrNull
            ?: user?.get("id")?.jsonPrimitive?.contentOrNull
            ?: serviceError("Jellyfin sende ikkje tilbake ein profil-ID")
        return ServiceAuthentication(accessToken = token, userId = userId)
    }

    private fun parseQuickConnect(body: String): QuickConnectChallenge {
        val root = parseObject(body, "Jellyfin sende eit ugyldig Quick Connect-svar")
        val secret = root["Secret"]?.jsonPrimitive?.contentOrNull
            ?: root["secret"]?.jsonPrimitive?.contentOrNull
            ?: serviceError("Jellyfin sende ikkje tilbake ein Quick Connect-hemmelegheit")
        val code = root["Code"]?.jsonPrimitive?.contentOrNull
            ?: root["code"]?.jsonPrimitive?.contentOrNull
            ?: serviceError("Jellyfin sende ikkje tilbake ein Quick Connect-kode")
        val authenticated = root["Authenticated"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: root["authenticated"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: false
        return QuickConnectChallenge(secret = secret, code = code, authenticated = authenticated)
    }

    private fun parseObject(body: String, message: String) =
        runCatching { Json.parseToJsonElement(body).jsonObject }.getOrElse { serviceError(message) }

    private companion object {
        const val JELLYFIN = "Jellyfin"
    }
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
            // A test that cannot reach the server is a result the sheet can render, not a crash
            // that surfaces `UnknownHostException`'s bare hostname in the error field.
            response = contacting(connection.kind.displayName) {
                transport.get(
                    endpoint,
                    if (connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.SEERR) {
                        headers(connection, deviceId)
                    } else {
                        mapOf(probe.headerName to connection.token)
                    },
                )
            }
        }

        return when (response.statusCode) {
            in 200..299 -> ConnectionTestResult(
                success = true,
                latencyMs = elapsed,
                message = extractVersion(response.body)?.let { "Tilkopla · v$it" } ?: "Tilkopla",
            )
            401, 403 -> ConnectionTestResult(false, elapsed, if (connection.sessionCookie) "Seerr-økta er utgått. Logg inn på nytt." else "API-nøkkelen vart avvist")
            404 -> ConnectionTestResult(false, elapsed, "Fann tenesta, men API-stien var ikkje tilgjengeleg")
            // This is where a misconfigured address is actually discovered, so the redirect target
            // belongs here more than anywhere else: the user is standing in the very sheet that
            // holds the field they need to change.
            in 300..399 -> ConnectionTestResult(false, elapsed, redirectMessage(connection.kind, response.location))
            429 -> ConnectionTestResult(false, elapsed, busyMessage(connection.kind, response.retryAfterSeconds))
            in 500..599 -> ConnectionTestResult(false, elapsed, "${connection.kind.displayName} er utilgjengeleg no")
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
    private val includeLibrary: (ServiceConnection, RemoteLibraryView) -> Boolean = { _, view -> !isExcludedHomeLibrary(view.name) },
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
        if (userId == null && !allowFallback) {
            return MediaServerFeed(sessions = sessions, recentMovies = emptyList(), recentSeries = emptyList(),
                warning = "Bibliotekprofilen kunne ikkje stadfestast. Prøver igjen.")
        }

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

        val resumeResult = runCatching {
            resume(connection, requireNotNull(encodedUserId) { "Profil-ID manglar" }, viewsResult.getOrThrow())
        }
        val resume = resumeResult.getOrElse {
            warnings += "Hald fram å sjå er utilgjengeleg"
            emptyList()
        }

        val releasesResult = runCatching {
            releasedAcrossLibraries(connection, requireNotNull(encodedUserId), viewsResult.getOrThrow())
        }

        val nextUp = if (connection.kind == ServiceKind.JELLYFIN) runCatching {
            nextUp(connection, requireNotNull(encodedUserId), viewsResult.getOrThrow())
        }.getOrElse {
            warnings += "Neste episode er utilgjengeleg"
            emptyList()
        } else emptyList()

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
            resume = resume,
            nextUp = nextUp,
            warning = warnings.distinct().takeIf { it.isNotEmpty() }?.joinToString(" · "),
            recentReleases = releasesResult.getOrDefault(emptyList()).filter { it.mediaType == "Episode" }
                .mapNotNull { libraryRelease(it, connection.kind, ReleaseWindow()) },
            releasesFailed = releasesResult.isFailure,
            releaseCandidates = releasesResult.getOrDefault(emptyList()).filter { it.mediaType == "Movie" && it.available },
        )
    }

    /**
     * The server's own resume list. Jellyfin and Emby both track playback position, so a partly
     * watched title is theirs to report; guessing from a cached percentage would go stale the
     * moment the title is finished on another device.
     *
     * Scoped per library, because the resume list is otherwise unfiltered and would be the one
     * way an excluded children's library reappears on Home.
     */
    fun resume(
        connection: ServiceConnection,
        userId: String,
        views: List<RemoteLibraryView> = libraryViews(connection, userId),
    ): List<RemoteLibraryItem> {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val allowed = views.filter { includeLibrary(connection, it) }
        if (allowed.isEmpty()) return emptyList()
        val query = "Limit=$RESUME_ITEM_LIMIT&Recursive=true&MediaTypes=Video" +
            "&Fields=Overview,Genres,PrimaryImageAspectRatio&EnableImages=true&ImageTypeLimit=1" +
            "&EnableImageTypes=Primary,Thumb&EnableUserData=true"
        val groups = allowed.mapNotNull { view ->
            val scoped = "$query&ParentId=${encodePathSegment(view.id)}"
            val paths = when (connection.kind) {
                ServiceKind.JELLYFIN -> listOf("UserItems/Resume?userId=$userId&$scoped", "Users/$userId/Items/Resume?$scoped")
                else -> listOf("Users/$userId/Items/Resume?$scoped")
            }
            runCatching { getItems(connection, paths).map { it.copy(libraryId = view.id) } }.getOrNull()
        }
        check(groups.isNotEmpty()) { "Fekk ikkje henta Hald fram å sjå" }
        return interleave(groups).distinctBy(RemoteLibraryItem::id)
            .sortedByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE }.take(RESUME_ITEM_LIMIT)
    }

    /**
     * One library's own "Continue watching" and "Next up".
     *
     * [feed] asks every library at once and interleaves twelve cards out of the lot, which is right
     * for Home and wrong for a library page: open Films and you want the films you have not
     * finished, not three of them behind two episodes from a different library. Same requests,
     * one view.
     */
    fun libraryShelves(
        connection: ServiceConnection,
        view: RemoteLibraryView,
    ): Pair<List<RemoteLibraryItem>, List<RemoteLibraryItem>> {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val userId = userIdentity(connection)?.let(::encodePathSegment)
            ?: return emptyList<RemoteLibraryItem>() to emptyList()
        // A shelf is an extra, never a reason for the page itself to fail.
        val resume = runCatching { resume(connection, userId, listOf(view)) }.getOrDefault(emptyList())
        val next = if (connection.kind == ServiceKind.JELLYFIN) {
            runCatching { nextUp(connection, userId, listOf(view)) }.getOrDefault(emptyList())
        } else emptyList()
        return resume to next
    }

    /** The signed-in profile on this server, resolved the way the feed resolves it. */
    fun userIdentity(connection: ServiceConnection): String? =
        connection.userId.takeIf(String::isNotBlank)
            ?: runCatching { currentUserId(connection) }.getOrNull()

    /** Ask Jellyfin for the next unwatched episode, scoped before loading to selected libraries. */
    fun nextUp(
        connection: ServiceConnection,
        userId: String,
        views: List<RemoteLibraryView> = libraryViews(connection, userId),
    ): List<RemoteLibraryItem> {
        require(connection.kind == ServiceKind.JELLYFIN)
        val allowed = views.filter { includeLibrary(connection, it) &&
            (it.collectionType.isNullOrBlank() || it.collectionType.lowercase(java.util.Locale.ROOT) in setOf("tvshows", "mixed")) }
        if (allowed.isEmpty()) return emptyList()
        val groups = allowed.mapNotNull { view ->
            runCatching {
                getItems(connection, listOf("Shows/NextUp?UserId=$userId&ParentId=${encodePathSegment(view.id)}" +
                    "&Limit=24&EnableUserData=true&EnableResumable=false" +
                    "&Fields=Overview,Genres,PrimaryImageAspectRatio&EnableImages=true&ImageTypeLimit=1"))
                    .map { it.copy(libraryId = view.id) }
            }.getOrNull()
        }
        check(groups.isNotEmpty()) { "Fekk ikkje henta neste episode" }
        val episodes = interleave(groups).distinctBy(RemoteLibraryItem::id).take(24)
        val series = episodes.mapNotNull { it.seriesId }.distinct()
        if (series.isEmpty()) return episodes
        // An unwatched episode has no LastPlayedDate of its own. Use this profile's most
        // recently watched episode in the same series; never substitute DateCreated.
        // Bound both concurrency and elapsed time so unavailable metadata cannot hold Home.
        val pool = java.util.concurrent.Executors.newFixedThreadPool(minOf(4, series.size))
        val dates = try {
            val jobs = series.map { seriesId -> java.util.concurrent.Callable {
                seriesId to runCatching {
                    getItems(connection, listOf("Items?UserId=$userId&ParentId=${encodePathSegment(seriesId)}" +
                        "&Recursive=true&IncludeItemTypes=Episode&IsPlayed=true&SortBy=DatePlayed&SortOrder=Descending" +
                        "&Limit=1&EnableUserData=true&EnableImages=false"))
                        .firstOrNull()?.lastActivityEpochMillis
                }.getOrNull()
            } }
            pool.invokeAll(jobs, 8, java.util.concurrent.TimeUnit.SECONDS)
                .mapNotNull { runCatching { it.get() }.getOrNull() }.toMap()
        } finally { pool.shutdownNow() }
        return episodes.map { item -> item.copy(lastActivityEpochMillis =
            listOfNotNull(item.lastActivityEpochMillis, dates[item.seriesId]).maxOrNull()) }
            .sortedByDescending { it.lastActivityEpochMillis ?: Long.MIN_VALUE }
    }

    /**
     * Searches the libraries the signed-in profile can actually see. Discover only ever reached
     * Seerr, so a title already sitting in your own library was the one thing you could not find.
     */
    fun search(connection: ServiceConnection, term: String): List<RemoteLibraryItem> {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        val trimmed = term.trim()
        if (trimmed.isBlank()) return emptyList()
        val userId = ownUserId(connection)?.let(::encodePathSegment) ?: return emptyList()
        val allowed = libraryViews(connection, userId).filter { includeLibrary(connection, it) }

        if (allowed.isEmpty()) return emptyList()
        // ProviderIds is what lets the same title be recognised across two servers. Without it a
        // film held on both Jellyfin and Emby arrives as two unrelated items and is shown twice.
        val query = "searchTerm=${encode(trimmed).replace("+", "%20")}&Recursive=true" +
            "&IncludeItemTypes=Movie,Series,Episode&Limit=$SEARCH_ITEM_LIMIT" +
            "&Fields=Overview,Genres,PrimaryImageAspectRatio,ProviderIds,PremiereDate" +
            "&EnableImages=true&ImageTypeLimit=1" +
            "&EnableImageTypes=Primary,Thumb&EnableUserData=true&IsMissing=false"
        val groups = allowed.mapNotNull { view ->
            val scoped = "$query&ParentId=${encodePathSegment(view.id)}"
            val paths = when (connection.kind) {
                ServiceKind.JELLYFIN -> listOf("Items?userId=$userId&$scoped", "Users/$userId/Items?$scoped")
                else -> listOf("Users/$userId/Items?$scoped")
            }
            runCatching { getItems(connection, paths) }.getOrNull()
        }
        check(groups.isNotEmpty()) { "Fekk ikkje søkt i biblioteket" }
        return interleave(groups).distinctBy(RemoteLibraryItem::id).take(SEARCH_ITEM_LIMIT)
    }

    /** Full profile-scoped browser; Home exclusions and preview limits do not apply here. */
    fun browseLibraries(connection: ServiceConnection): List<RemoteLibraryView> {
        require(connection.kind == ServiceKind.JELLYFIN)
        val user = connection.userId.takeIf(String::isNotBlank) ?: currentUserId(connection)
        require(!user.isNullOrBlank()) { "Profil-ID manglar" }
        return libraryViews(connection, encodePathSegment(user)).map { it.copy(artworkUrl = artworkUrl(connection, it.id)) }
    }

    fun libraryFacets(connection: ServiceConnection, parentId: String): app.reelstack.data.model.LibraryFacets {
        require(connection.kind == ServiceKind.JELLYFIN && parentId.isNotBlank())
        val user = connection.userId.takeIf(String::isNotBlank) ?: currentUserId(connection)
        require(!user.isNullOrBlank())
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl,
            "Items/Filters?userId=${encodePathSegment(user)}&ParentId=${encodePathSegment(parentId)}"), headers(connection, deviceId))
        response.requireSuccess(connection.kind)
        val root = Json.parseToJsonElement(response.body).jsonObject
        fun values(key: String) = (root[key] as? kotlinx.serialization.json.JsonArray).orEmpty()
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf(String::isNotBlank) }.distinct()
        return app.reelstack.data.model.LibraryFacets(parentId, values("Genres").sorted(),
            values("Years").filter { it.toIntOrNull() in 1800..2200 }.sortedDescending())
    }

    fun browseLibrary(connection: ServiceConnection, parentId: String, offset: Int = 0, collectionType: String? = null,
        filters: app.reelstack.data.model.LibraryFilters = app.reelstack.data.model.LibraryFilters()): List<RemoteLibraryItem> {
        require(connection.kind == ServiceKind.JELLYFIN && parentId.isNotBlank() && offset >= 0)
        val user = connection.userId.takeIf(String::isNotBlank) ?: currentUserId(connection)
        require(!user.isNullOrBlank()) { "Profil-ID manglar" }
        val catalogueType = when (collectionType?.lowercase(java.util.Locale.ROOT)) {
            "movies" -> "Movie"
            "tvshows" -> "Series"
            "boxsets" -> "BoxSet"
            else -> null
        }
        // A collection is its own library in this app, so it has no business appearing between the
        // films. Jellyfin returns BoxSet children from a movie library whenever the server is set
        // to display collections there, and relying on the library reporting its own type is not
        // enough: an unknown type meant no item filter at all, and everything in the folder came
        // through. Excluding it explicitly holds either way.
        val browsingCollections = collectionType?.lowercase(java.util.Locale.ROOT) == "boxsets"
        val query = "userId=${encodePathSegment(user)}&ParentId=${encodePathSegment(parentId)}" +
            "&Recursive=${catalogueType != null}&StartIndex=$offset&Limit=60" + filters.query() +
            (catalogueType?.let { "&IncludeItemTypes=$it" } ?: "") +
            (if (browsingCollections) "" else "&ExcludeItemTypes=BoxSet") +
            "&Fields=Overview,Genres,ProviderIds&EnableUserData=true&IsMissing=false"
        return getItems(connection, listOf("Items?$query"))
    }

    private fun ownUserId(connection: ServiceConnection): String? = connection.userId.takeIf(String::isNotBlank)
        ?: runCatching { currentUserId(connection) }.getOrNull()
        ?: runCatching { preferredAvailableUserId(connection) }.getOrNull()

    private fun releasedAcrossLibraries(
        connection: ServiceConnection,
        userId: String,
        views: List<RemoteLibraryView>,
    ): List<RemoteLibraryItem> {
        val window = ReleaseWindow()
        val relevant = views.filter { includeLibrary(connection, it) && (it.supports("Movie") || it.supports("Episode")) }

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

    /**
     * Marks a title watched or unwatched on the media server.
     *
     * Spole could already *filter* on watched state but never change it, so an episode skipped by
     * accident, or a film finished on another screen, had to be tidied up in a different client.
     * The state belongs to the server, so this writes there and lets the next refresh read it back
     * rather than keeping a local guess.
     *
     * Jellyfin 10.9 moved these to `UserPlayedItems`; Emby and older Jellyfin keep the user-scoped
     * path. Both are tried, the same way the resume list already is.
     */
    fun setPlayed(connection: ServiceConnection, userId: String, itemId: String, played: Boolean) =
        setUserItemState(connection, userId, itemId, played, "PlayedItems")

    /** Same contract as [setPlayed], for the favourite flag the library filter already reads. */
    fun setFavourite(connection: ServiceConnection, userId: String, itemId: String, favourite: Boolean) =
        setUserItemState(connection, userId, itemId, favourite, "FavoriteItems")

    private fun setUserItemState(
        connection: ServiceConnection,
        userId: String,
        itemId: String,
        enabled: Boolean,
        collection: String,
    ) {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        require(userId.isNotBlank()) { "Manglar profil-ID for denne tenesta." }
        val item = encodePathSegment(itemId)
        val user = encodePathSegment(userId)
        val paths = when (connection.kind) {
            ServiceKind.JELLYFIN -> listOf("User$collection/$item", "Users/$user/$collection/$item")
            else -> listOf("Users/$user/$collection/$item")
        }
        var last: HttpResponse? = null
        paths.forEach { path ->
            val url = EndpointValidator.resolve(connection.baseUrl, path)
            val response = contacting(connection.kind.displayName) {
                if (enabled) transport.post(url, headers(connection, deviceId), "{}")
                else transport.delete(url, headers(connection, deviceId))
            }
            if (response.statusCode in 200..299) return
            // Only a missing endpoint is worth retrying on the older path. A rejected token or a
            // forbidden item means the same thing on both.
            if (response.statusCode != 404) { response.requireSuccess(connection.kind); return }
            last = response
        }
        last?.requireSuccess(connection.kind)
    }

    /**
     * Takes a title out of "Continue watching".
     *
     * The resume list is the server's, built from the saved playback position, so the only honest
     * way to remove something is to clear that position — a local "hidden" list would reappear on
     * every other client and come back on the next reinstall. Jellyfin 10.9 exposes the write as
     * user data, older Jellyfin and Emby keep the user-scoped path, and a server that knows
     * neither still accepts a stop report at position zero, which is exactly what a client that
     * played the file to the end would have sent.
     */
    fun clearResume(connection: ServiceConnection, userId: String, itemId: String) {
        require(connection.kind == ServiceKind.JELLYFIN || connection.kind == ServiceKind.EMBY)
        require(userId.isNotBlank()) { "Manglar profil-ID for denne tenesta." }
        val item = encodePathSegment(itemId)
        val user = encodePathSegment(userId)
        val cleared = buildJsonObject { put("PlaybackPositionTicks", 0L); put("PlayedPercentage", 0.0) }.toString()
        val stopped = buildJsonObject { put("ItemId", itemId); put("PositionTicks", 0L) }.toString()
        val attempts = buildList {
            if (connection.kind == ServiceKind.JELLYFIN) add("UserItems/$item/UserData" to cleared)
            add("Users/$user/Items/$item/UserData" to cleared)
            add("Sessions/Playing/Stopped" to stopped)
        }
        var last: HttpResponse? = null
        attempts.forEach { (path, payload) ->
            val response = contacting(connection.kind.displayName) {
                transport.post(EndpointValidator.resolve(connection.baseUrl, path), headers(connection, deviceId), payload)
            }
            if (response.statusCode in 200..299) return
            // Only "this server does not have that endpoint" is worth trying the next shape for.
            // A rejected token or a forbidden item means the same thing on all three.
            if (response.statusCode !in setOf(400, 404, 405)) { response.requireSuccess(connection.kind); return }
            last = response
        }
        last?.requireSuccess(connection.kind)
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
        serviceError("Fekk ikkje henta detaljar frå ${connection.kind.displayName}")
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
        val relevantViews = views.filter { it.supports(itemType) && includeLibrary(connection, it) }
        // Never fall back to an unscoped query that could reintroduce excluded libraries.
        if (relevantViews.isEmpty()) return emptyList()
        val successfulGroups = relevantViews.mapNotNull { view ->
            runCatching {
                getItems(connection, latestPaths(connection.kind, userId, itemType, groupItems, parentId = view.id))
            }.getOrNull()
        }
        if (successfulGroups.isEmpty()) serviceError("Fekk ikkje oppdatert dei valde biblioteka")
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
                            artworkUrl(connection, it, item.artworkImageType, item.artworkTag)
                        },
                        logoUrl = item.logoItemId?.let {
                            logoUrl(connection, it, item.logoTag)
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
            "&EnableImages=true&ImageTypeLimit=1&EnableImageTypes=Primary,Thumb,Logo" +
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
            ServiceKind.SEERR, ServiceKind.RADARR, ServiceKind.SONARR -> serviceError("Medietenaren er ikkje støtta")
        }
    }

    fun logoUrl(connection: ServiceConnection, itemId: String, tag: String? = null): String =
        EndpointValidator.resolve(
            connection.baseUrl,
            "Items/${encodePathSegment(itemId)}/Images/Logo?maxWidth=800&quality=90" + tagParameter(tag),
        )

    /**
     * Jellyfin's image tag is a content hash, so appending it is what makes a replaced picture
     * appear. Without it the address never changes and the cached copy is served for as long as it
     * survives eviction — which is why refreshing in the app did nothing after changing art on the
     * server.
     */
    private fun tagParameter(tag: String?): String =
        tag?.takeIf(String::isNotBlank)?.let { "&tag=" + encodePathSegment(it) }.orEmpty()

    private fun artworkUrl(
        connection: ServiceConnection,
        itemId: String,
        imageType: String = "Primary",
        tag: String? = null,
    ): String =
        EndpointValidator.resolve(
            connection.baseUrl,
            if (imageType.equals("Thumb", ignoreCase = true)) {
                "Items/${encodePathSegment(itemId)}/Images/Thumb?maxWidth=960&quality=90"
            } else {
                "Items/${encodePathSegment(itemId)}/Images/Primary?maxHeight=720&quality=90"
            } + tagParameter(tag),
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
        const val RESUME_ITEM_LIMIT = 12
        const val SEARCH_ITEM_LIMIT = 24
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

    fun search(connection: ServiceConnection, query: String, page: Int = 1): SeerrSearchPage {
        require(connection.kind == ServiceKind.SEERR)
        require(page >= 1) { "Sidetalet må vere minst 1" }
        val encodedQuery = encode(query).replace("+", "%20")
        val response = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/search?query=$encodedQuery&page=$page&language=nb"),
            headers(connection),
        )
        response.requireSuccess(connection.kind)
        return SeerrSearchPage(
            items = ServicePayloadParser.discover(response.body),
            page = page,
            totalPages = ServicePayloadParser.totalPages(response.body),
        )
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

    /** Personal history is paged only on demand; the background follow poll stays bounded. */
    fun requestHistory(connection: ServiceConnection, userId: String, offset: Int = 0): RequestHistoryPage {
        require(connection.kind == ServiceKind.SEERR && connection.sessionCookie)
        require(userId.toIntOrNull()?.let { it > 0 } == true && offset >= 0)
        val response = transport.get(EndpointValidator.resolve(connection.baseUrl,
            "api/v1/request?take=20&skip=$offset&sort=added&sortDirection=desc&requestedBy=${encode(userId)}"), headers(connection))
        response.requireSuccess(connection.kind)
        return parseRequestHistoryPage(response.body, userId, offset, 20)
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

    /**
     * Withdraws one of your own pending requests. Deleting is destructive and Seerr's own
     * permissions are the last word, but the owner is confirmed here first so the app never sends
     * a delete for a request that is not yours — the same rule the submit path already follows.
     */
    fun cancelRequest(connection: ServiceConnection, requestId: Int, expectedUserId: String = connection.userId) {
        require(connection.kind == ServiceKind.SEERR)
        require(requestId > 0) { "Førespurnaden manglar ein gyldig ID." }
        require(connection.sessionCookie && expectedUserId.isNotBlank()) {
            "Logg inn personleg i Seerr for å trekkje tilbake ein førespurnad."
        }
        val actor = AccountProfileClient(transport = transport).load(connection)
        check(actor.isPersonal && actor.id == expectedUserId) { "Seerr-kontoen er endra. Sjekk innlogginga før du held fram." }

        val requestHeaders = headers(connection)
        val existing = transport.get(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request/$requestId"),
            requestHeaders,
        )
        if (existing.statusCode == 404) serviceError("Førespurnaden finst ikkje lenger i Seerr.")
        existing.requireSuccess(connection.kind)
        val owner = ServicePayloadParser.requestOwnerId(existing.body)
        check(owner == null || owner == actor.id) { "Denne førespurnaden tilhøyrer ein annan konto." }

        val response = transport.delete(
            EndpointValidator.resolve(connection.baseUrl, "api/v1/request/$requestId"),
            requestHeaders,
        )
        if (response.statusCode == 404) serviceError("Førespurnaden var alt fjerna i Seerr.")
        response.requireSuccess(connection.kind)
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

/**
 * What Jellyfin shows in Dashboard → Devices and in the active-session list. Every Spole install
 * used to report the literal string "Android", so a phone, a tablet and a Google TV on one account
 * were three identical rows and there was no way to tell which screen was playing. The id already
 * separates them; the name is what a person reads. Quotes and control characters are stripped
 * because the value goes into a quoted header field.
 */
internal fun jellyfinDeviceName(): String =
    android.os.Build.MODEL.orEmpty().filterNot { it == '"' || it == '\\' || it.isISOControl() }
        .trim().take(48).ifBlank { "Android" }

internal fun jellyfinAuthorization(deviceId: String, token: String? = null): String = buildString {
    append("MediaBrowser Client=\"Spole\", Device=\"")
    append(jellyfinDeviceName())
    append("\", ")
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
        401 -> serviceError(if (kind == ServiceKind.SEERR) "Logg inn på Seerr på nytt i Innstillingar." else "${kind.displayName} avviste API-nøkkelen")
        403 -> serviceError(if (kind == ServiceKind.SEERR) "Seerr gav ikkje kontoen tilgang til denne handlinga." else "${kind.displayName} avviste API-nøkkelen")
        404 -> serviceError("${kind.displayName} tilbyr ikkje dette API-endepunktet")
        408 -> serviceError("${kind.displayName} er mellombels oppteken")
        429 -> serviceError(busyMessage(kind, retryAfterSeconds))
        in 300..399 -> serviceError(redirectMessage(kind, location))
        in 500..599 -> serviceError("${kind.displayName} er utilgjengeleg no")
        else -> serviceError("${kind.displayName} svara med status $statusCode")
    }
}

/**
 * A redirect is the most common way a working server still looks broken: a reverse proxy that
 * sends `http://` to `https://`, or a host that only answers on a different name. Redirects are
 * never followed automatically, so without this the user got "svara med status 301" and no idea
 * that the fix is one address change in Innstillingar.
 */
internal fun redirectMessage(kind: ServiceKind, location: String?): String {
    val target = location?.let { runCatching { java.net.URI(it) }.getOrNull() }
        ?.takeIf { it.isAbsolute && !it.host.isNullOrBlank() }
        // Only scheme, host and port. The path of a redirect is not something the user types into
        // the address field, and a query string could carry a token.
        ?.let { uri -> buildString {
            append(uri.scheme.lowercase(Locale.ROOT))
            append("://")
            append(uri.host.lowercase(Locale.ROOT))
            if (uri.port != -1) append(":${uri.port}")
        } }
    return if (target != null) {
        "${kind.displayName} sender deg vidare til $target. Bruk den adressa i Innstillingar."
    } else {
        "${kind.displayName} sender deg vidare til ei anna adresse. Sjekk kva adresse tenaren " +
            "faktisk svarar på, og bruk den i Innstillingar."
    }
}

/** Repeats the server's own `Retry-After` when it gave one, so "vent litt" has a number in it. */
internal fun busyMessage(kind: ServiceKind, retryAfterSeconds: Long?): String = when {
    retryAfterSeconds == null || retryAfterSeconds <= 0 -> "${kind.displayName} er mellombels oppteken"
    retryAfterSeconds < 60 -> "${kind.displayName} er mellombels oppteken. Prøv igjen om $retryAfterSeconds sekund."
    else -> "${kind.displayName} er mellombels oppteken. Prøv igjen om ${retryAfterSeconds / 60} minutt."
}
