package app.reelstack.data.repository

import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.IncomingState
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.model.ViewerAccess
import app.reelstack.data.network.AccountProfileClient
import app.reelstack.data.network.MediaServerClient
import app.reelstack.data.network.QueueServiceFeed
import app.reelstack.data.network.QueueServiceClient
import app.reelstack.data.network.RemoteDiscoverItem
import app.reelstack.data.network.RemoteMediaDetails
import app.reelstack.data.network.MediaServerFeed
import app.reelstack.data.network.RemoteLibraryItem
import app.reelstack.data.network.RemotePlayback
import app.reelstack.data.network.RemoteQueueItem
import app.reelstack.data.network.RemoteRequest
import app.reelstack.data.network.RemoteUpcomingItem
import app.reelstack.data.network.RemoteRecommendationItem
import app.reelstack.data.network.RecommendationsClient
import app.reelstack.data.network.SeerrFeed
import app.reelstack.data.network.SeerrServiceClient
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class MediaSyncSnapshot(
    val sessions: List<PlaybackSession>,
    val recentMovies: List<LibraryMedia>,
    val recentSeries: List<LibraryMedia>,
    val upcoming: List<UpcomingMedia>,
    val recentReleases: List<UpcomingMedia> = emptyList(),
    val incoming: List<IncomingMedia>,
    val discover: List<DiscoverMedia>,
    val recommendations: List<DiscoverMedia> = emptyList(),
    val recommendationsError: String? = null,
    val activity: List<ActivityEvent>,
    val successfulServices: Set<ServiceKind>,
    val errors: Map<ServiceKind, String>,
    val refreshedAt: Instant,
    val warnings: Map<ServiceKind, String> = emptyMap(),
    val adminView: Boolean = false,
)

class MediaSyncRepository(
    private val mediaServerClient: MediaServerClient = MediaServerClient(),
    private val queueServiceClient: QueueServiceClient = QueueServiceClient(),
    private val seerrServiceClient: SeerrServiceClient = SeerrServiceClient(),
    private val recommendationsClient: RecommendationsClient = RecommendationsClient(),
    private val accountProfileClient: AccountProfileClient = AccountProfileClient(),
) {
    suspend fun refresh(
        connections: List<ServiceConnection>,
    ): MediaSyncSnapshot = supervisorScope {
        val configured = connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        val identities = configured.filter { it.kind in setOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY) }
            .map { connection -> async { runCatching { accountProfileClient.load(connection) }.getOrNull()?.let { connection.kind to it } } }
            .mapNotNull { it.await() }.toMap()
        val access = ViewerAccess(configured.any { it.kind == ServiceKind.SEERR }, identities)
        val deferred = configured.map { connection ->
            async { connection.kind to runCatching { fetch(connection, access) } }
        }
        val results = deferred.map { it.await() }
        val successful = results.filter { it.second.isSuccess }.mapTo(mutableSetOf()) { it.first }
        val errors = results.mapNotNull { (kind, result) ->
            result.exceptionOrNull()?.let { kind to friendlyError(kind, it) }
        }.toMap()

        val payloads = results.mapNotNull { it.second.getOrNull() }
        val mediaPayloads = payloads.filterIsInstance<ServicePayload.Media>()
        val warnings = mediaPayloads.mapNotNull { payload ->
            payload.feed.warning?.let { payload.kind to it }
        }.toMap()
        val queuePayloads = payloads.filterIsInstance<ServicePayload.Queue>()
        val queue = queuePayloads.flatMap { it.feed.queue }
        val upcoming = queuePayloads.flatMap { payload ->
            payload.feed.upcoming.mapNotNull(::upcomingMedia)
        }.sortedBy(UpcomingMedia::airDateEpochMillis)
        val recentReleases = queuePayloads.flatMap { payload ->
            payload.feed.recentReleases.mapNotNull(::upcomingMedia)
        }.sortedByDescending(UpcomingMedia::airDateEpochMillis)
        val seerr = payloads.filterIsInstance<ServicePayload.Seerr>().firstOrNull()?.feed
        val discover = seerr?.discover.orEmpty().map(::discoverMedia)
        val recommendationResult = runCatching {
            recommendationsClient.feed().map(::recommendationMedia)
        }
        val recommendationSeed = recommendationResult.getOrDefault(emptyList())
        // The public recommendation feed deliberately contains no private Seerr state. Resolve
        // that state during refresh instead of waiting for a tap on the card. This means Home can
        // truthfully label a title as available/requested before the detail sheet opens.
        val seerrConnection = configured.firstOrNull { it.kind == ServiceKind.SEERR }
        val recommendations = if (seerrConnection != null) {
            recommendationSeed.mapIndexed { index, recommendation ->
                async {
                    if (index >= RECOMMENDATION_STATUS_LOOKUP_LIMIT) {
                        return@async recommendation
                    }
                    val remoteId = recommendation.remoteId ?: return@async recommendation
                    val mediaType = recommendation.mediaType ?: return@async recommendation
                    if (mediaType != "movie" && mediaType != "tv") return@async recommendation
                    runCatching {
                        seerrServiceClient.details(
                            connection = seerrConnection,
                            mediaType = mediaType,
                            remoteId = remoteId,
                            includeOverviewFallback = false,
                        )
                    }.fold(
                        onSuccess = { remote ->
                            val status = remote.seerrStatus
                            recommendation.copy(
                                inLibrary = status?.let { it == 5 } ?: recommendation.inLibrary,
                                requested = status?.let { it in 2..4 } ?: recommendation.requested,
                                seerrStatus = status ?: recommendation.seerrStatus,
                                artworkUrl = remote.artworkUrl ?: recommendation.artworkUrl,
                                overview = remote.overview ?: recommendation.overview,
                                facts = (remote.facts + recommendation.facts).distinct(),
                                genres = (remote.genres + recommendation.genres).distinct(),
                            )
                        },
                        onFailure = { recommendation },
                    )
                }
            }.awaitAll()
        } else {
            recommendationSeed
        }
        val titleLookup = discover.associateBy { it.remoteId }
        val activity = buildList {
            seerr?.requests.orEmpty().forEach { add(requestActivity(it, titleLookup[it.remoteId])) }
            if (access.isAdmin) queue.forEach { add(queueActivity(it)) }
        }.take(30)

        MediaSyncSnapshot(
            sessions = mediaPayloads.flatMap { payload ->
                payload.feed.sessions.map { playbackSession(it, payload.kind) }
            },
            recentMovies = interleave(mediaPayloads.map { payload ->
                payload.feed.recentMovies.map { item -> libraryMedia(item, payload.kind) }
            }).take(24),
            recentSeries = interleave(mediaPayloads.map { payload ->
                payload.feed.recentSeries.map { item -> libraryMedia(item, payload.kind) }
            }).take(24),
            upcoming = upcoming.take(30),
            recentReleases = recentReleases.take(30),
            incoming = if (access.isAdmin) queue.map(::incomingMedia) else emptyList(),
            discover = discover,
            recommendations = recommendations,
            recommendationsError = recommendationResult.exceptionOrNull()?.message,
            activity = activity,
            successfulServices = successful,
            errors = errors,
            refreshedAt = Instant.now(),
            warnings = warnings,
            adminView = access.isAdmin,
        )
    }

    fun request(connection: ServiceConnection, media: DiscoverMedia, expectedUserId: String = connection.userId, seasons: Set<Int> = emptySet()) {
        val remoteId = requireNotNull(media.remoteId) { "Tittelen manglar medie-ID frå Seerr" }
        val mediaType = requireNotNull(media.mediaType) { "Tittelen manglar medietype frå Seerr" }
        seerrServiceClient.request(connection, mediaType, remoteId, expectedUserId, seasons)
    }

    fun search(connection: ServiceConnection, query: String): List<DiscoverMedia> =
        seerrServiceClient.search(connection, query).map(::discoverMedia)

    fun details(connection: ServiceConnection, media: DiscoverMedia): RemoteMediaDetails =
        seerrServiceClient.details(
            connection,
            requireNotNull(media.mediaType) { "Medietypen manglar" },
            requireNotNull(media.remoteId) { "Medie-ID-en manglar" },
        )

    fun details(connection: ServiceConnection, media: LibraryMedia): RemoteMediaDetails =
        mediaServerClient.details(
            connection,
            requireNotNull(media.remoteId) { "Medie-ID-en manglar" },
        )

    fun setPlaybackPaused(
        connections: List<ServiceConnection>,
        session: PlaybackSession,
        paused: Boolean,
    ) {
        val source = requireNotNull(session.source) { "Avspelingskjelda er ikkje tilgjengeleg" }
        val sessionId = requireNotNull(session.sessionId) { "Avspelingsøkta er ikkje tilgjengeleg" }
        val connection = connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() }
            ?: error("Tilkoplinga til ${source.displayName} er ikkje tilgjengeleg")
        val accounts = connections.filter { it.kind == source || it.kind == ServiceKind.SEERR }
            .mapNotNull { c -> runCatching { accountProfileClient.load(c) }.getOrNull()?.let { c.kind to it } }.toMap()
        val access = ViewerAccess(connections.any { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }, accounts)
        check(mediaServerClient.sessions(connection, access).any { it.sessionId == sessionId }) { "Avspelingsøkta er ikkje tilgjengeleg" }
        mediaServerClient.setPaused(connection, sessionId, paused)
    }

    private fun fetch(connection: ServiceConnection, access: ViewerAccess): ServicePayload = when (connection.kind) {
        ServiceKind.JELLYFIN, ServiceKind.EMBY -> ServicePayload.Media(
            connection.kind,
            mediaServerClient.feed(connection, access),
        )
        ServiceKind.RADARR, ServiceKind.SONARR -> ServicePayload.Queue(
            connection.kind,
            queueServiceClient.feed(connection, includeQueue = access.isAdmin),
        )
        ServiceKind.SEERR -> ServicePayload.Seerr(connection.kind, seerrServiceClient.feed(connection,
            requireNotNull(access.accounts[ServiceKind.SEERR]) { "Fekk ikkje stadfesta Seerr-kontoen" }))
    }

    private fun playbackSession(item: RemotePlayback, source: ServiceKind) = PlaybackSession(
        userName = item.userName,
        deviceName = item.deviceName,
        title = item.title,
        subtitle = item.subtitle,
        progress = item.progress,
        timeLeft = item.timeLeft,
        streamMethod = item.streamMethod,
        quality = item.quality,
        paused = item.paused,
        artworkUrl = item.artworkUrl,
        sessionId = item.sessionId,
        source = source,
    )

    private fun libraryMedia(item: RemoteLibraryItem, source: ServiceKind) = LibraryMedia(
        id = "${source.name.lowercase()}-${item.id}",
        title = item.title,
        subtitle = item.subtitle,
        progress = item.progress,
        artworkRes = R.drawable.media_placeholder,
        source = source,
        artworkUrl = item.artworkUrl,
        remoteId = item.id,
        overview = item.overview,
        facts = item.facts,
        genres = item.genres,
        mediaType = item.mediaType,
    )

    private fun incomingMedia(item: RemoteQueueItem) = IncomingMedia(
        id = item.id,
        title = item.title,
        source = item.source,
        status = item.status,
        state = item.state,
        artworkRes = R.drawable.media_placeholder,
        artworkUrl = item.artworkUrl,
        overview = item.overview,
        facts = item.facts,
        genres = item.genres,
        progress = item.progress,
    )

    private fun upcomingMedia(item: RemoteUpcomingItem): UpcomingMedia? {
        val instant = parseCalendarInstant(item.dateTime) ?: return null
        return UpcomingMedia(
            id = "${item.source.name.lowercase()}-${item.id}",
            title = item.title,
            subtitle = item.subtitle,
            dateLabel = calendarLabel(instant, item.source),
            airDateEpochMillis = instant.toEpochMilli(),
            artworkRes = R.drawable.media_placeholder,
            source = item.source,
            artworkUrl = item.artworkUrl,
            overview = item.overview,
            facts = item.facts,
            genres = item.genres,
            mediaType = item.mediaType,
        )
    }

    private fun discoverMedia(item: RemoteDiscoverItem) = DiscoverMedia(
        id = item.id,
        title = item.title,
        metadata = item.metadata,
        artworkRes = R.drawable.media_placeholder,
        inLibrary = item.inLibrary,
        requested = item.requested,
        seerrStatus = item.seerrStatus,
        artworkUrl = item.artworkUrl,
        remoteId = item.remoteId,
        mediaType = item.mediaType,
        overview = item.overview,
        facts = item.facts,
        genres = item.genres,
    )

    private fun recommendationMedia(item: RemoteRecommendationItem) = DiscoverMedia(
        id = item.id,
        title = item.title,
        metadata = item.metadata,
        artworkRes = R.drawable.media_placeholder,
        inLibrary = false,
        requested = false,
        artworkUrl = item.artworkUrl,
        remoteId = item.remoteId,
        mediaType = item.mediaType,
        overview = item.overview,
        facts = item.facts,
        genres = item.genres,
    )

    private fun queueActivity(item: RemoteQueueItem) = ActivityEvent(
        id = "activity-${item.id}",
        title = item.title,
        detail = "${item.source.displayName} · ${item.status}",
        time = "No",
        progress = item.progress,
        complete = item.state == IncomingState.READY,
        source = item.source,
        artworkRes = R.drawable.media_placeholder,
        artworkUrl = item.artworkUrl,
        mediaType = if (item.source == ServiceKind.SONARR) "Episode" else "Movie",
    )

    private fun requestActivity(request: RemoteRequest, discovered: DiscoverMedia?) = ActivityEvent(
        id = "seerr-request-${request.id}",
        title = discovered?.title ?: request.title ?: if (request.mediaType == "movie") "Ny film" else "Ny serie",
        detail = app.reelstack.data.model.requestProgress(request.mediaStatus, request.availableSeasons, request.seasons,
            request.downloads, request.status).stage.label + " · ${request.requestedBy}",
        time = relativeTime(request.createdAt),
        complete = app.reelstack.data.model.requestProgress(request.mediaStatus, request.availableSeasons, request.seasons,
            request.downloads, request.status).stage == app.reelstack.data.model.RequestStage.AVAILABLE,
        source = ServiceKind.SEERR,
        artworkRes = R.drawable.media_placeholder,
        artworkUrl = discovered?.artworkUrl ?: request.artworkUrl,
        // Seerr requests are shown with their poster, the same as everywhere else in the app.
        mediaType = "Movie",
    )

    private fun parseCalendarInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(value.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }.getOrNull()

    private fun calendarLabel(instant: Instant, source: ServiceKind): String {
        val zone = ZoneId.systemDefault()
        val dateTime = instant.atZone(zone)
        val today = LocalDate.now(zone)
        val date = dateTime.toLocalDate()
        val day = when (date) {
            today -> "I dag"
            today.plusDays(1) -> "I morgon"
            else -> date.format(DateTimeFormatter.ofPattern("EEE d. MMM", Locale.forLanguageTag("nn-NO")))
        }
        return if (source == ServiceKind.SONARR) "$day · ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}" else day
    }

    private fun relativeTime(createdAt: String?): String {
        val instant = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return "Nyleg"
        val duration = Duration.between(instant, Instant.now()).coerceAtLeast(Duration.ZERO)
        return when {
            duration.toMinutes() < 1 -> "Akkurat no"
            duration.toHours() < 1 -> "For ${duration.toMinutes()} min sidan"
            duration.toDays() < 1 -> "For ${duration.toHours()} t sidan"
            duration.toDays() == 1L -> "I går"
            else -> "For ${duration.toDays()} dagar sidan"
        }
    }

    private fun friendlyError(kind: ServiceKind, error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("avviste", ignoreCase = true) || message.contains("rejected", ignoreCase = true) ->
                "API-nøkkelen vart avvist"
            message.contains("endepunkt", ignoreCase = true) || message.contains("endpoint", ignoreCase = true) ->
                "API-endepunktet er utilgjengeleg"
            message.contains("profil", ignoreCase = true) || message.contains("profile", ignoreCase = true) ->
                "Fann ingen medieprofil — legg til profil-ID"
            message.contains("for stort", ignoreCase = true) || message.contains("too large", ignoreCase = true) ->
                "Svaret var større enn tryggleiksgrensa"
            else -> "Fekk ikkje oppdatert ${kind.displayName}"
        }
    }

    private sealed interface ServicePayload {
        val kind: ServiceKind

        data class Media(override val kind: ServiceKind, val feed: MediaServerFeed) : ServicePayload
        data class Queue(override val kind: ServiceKind, val feed: QueueServiceFeed) : ServicePayload
        data class Seerr(override val kind: ServiceKind, val feed: SeerrFeed) : ServicePayload
    }

    private companion object {
        // Keep refresh quick and predictable if the shared catalogue grows. The Home rail only
        // renders a small number of cards, while the remaining entries can still be checked when
        // the user opens them from the catalogue in a later step.
        const val RECOMMENDATION_STATUS_LOOKUP_LIMIT = 12
    }
}

private fun <T> interleave(groups: List<List<T>>): List<T> = buildList {
    val maxSize = groups.maxOfOrNull(List<T>::size) ?: 0
    repeat(maxSize) { index ->
        groups.forEach { group -> group.getOrNull(index)?.let(::add) }
    }
}
