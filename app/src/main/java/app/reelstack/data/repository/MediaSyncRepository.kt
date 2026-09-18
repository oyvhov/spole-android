package app.reelstack.data.repository

import app.reelstack.R
import app.reelstack.localization.LocalizedText
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
import app.reelstack.data.network.SeerrReleaseClient
import app.reelstack.data.network.localizedFailure
import app.reelstack.data.network.serviceError
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
    val resume: List<LibraryMedia> = emptyList(),
    val nextUp: List<LibraryMedia> = emptyList(),
    /** What this profile has starred. Empty is the normal state until someone stars something. */
    val favourites: List<LibraryMedia> = emptyList(),
    val upcoming: List<UpcomingMedia>,
    val recentReleases: List<UpcomingMedia> = emptyList(),
    val incoming: List<IncomingMedia>,
    val discover: List<DiscoverMedia>,
    val recommendations: List<DiscoverMedia> = emptyList(),
    val recommendationsError: LocalizedText? = null,
    val activity: List<ActivityEvent>,
    val successfulServices: Set<ServiceKind>,
    val errors: Map<ServiceKind, LocalizedText>,
    val refreshedAt: Instant,
    val warnings: Map<ServiceKind, List<LocalizedText>> = emptyMap(),
    val adminView: Boolean = false,
    /** Services that only answered on their alternate address, so the switch can be persisted. */
    val switchedToAlternate: Set<ServiceKind> = emptySet(),
    val upcomingError: LocalizedText? = null,
    val recentReleasesError: LocalizedText? = null,
)

data class LibraryFeedUpdate(
    val source: ServiceKind,
    val resume: List<LibraryMedia>,
    val nextUp: List<LibraryMedia>,
    val recentMovies: List<LibraryMedia>,
    val recentSeries: List<LibraryMedia>,
    val favourites: List<LibraryMedia>,
)

class MediaSyncRepository(
    /**
     * Only for the month and weekday names a date carries. Everything else this class says to a
     * person travels as a [LocalizedText] — a resource and its arguments — so the repository stays
     * free of a Context and its unit tests keep running on the JVM.
     */
    private val locale: Locale = Locale.getDefault(),
    /**
     * The device's own clock setting, which is not something a locale can answer: an English
     * reader in Norway may well have the 24-hour clock on, and Android keeps that as a separate
     * system switch. It arrives as a plain flag so this class still needs no Context.
     */
    private val use24HourClock: Boolean = true,
    /**
     * Turns a decision into words. The sync layer knows a title is a film released in 2024; only
     * whoever holds a Context knows whether that reads "Film · 2024" or "Movie · 2024".
     *
     * The default is for tests: it keeps whatever came from a server and drops the rest, so a test
     * can assert on the decisions without standing up Android resources.
     */
    private val words: (LocalizedText) -> String = { it.literal.orEmpty() },
    private val mediaServerClient: MediaServerClient = MediaServerClient(),
    private val queueServiceClient: QueueServiceClient = QueueServiceClient(),
    private val seerrServiceClient: SeerrServiceClient = SeerrServiceClient(),
    private val recommendationsClient: RecommendationsClient = RecommendationsClient(),
    private val accountProfileClient: AccountProfileClient = AccountProfileClient(),
    private val seerrReleaseClient: SeerrReleaseClient = SeerrReleaseClient(),
) {
    /** Refresh only playback, retaining the same server-verified visibility rules as a full sync. */
    suspend fun refreshPlayback(connections: List<ServiceConnection>): List<PlaybackSession> = supervisorScope {
        val configured = connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        val identities = configured.filter { it.kind in setOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY) }
            .map { connection -> async { runCatching { accountProfileClient.load(connection) }.getOrNull()?.let { connection.kind to it } } }
            .mapNotNull { it.await() }.toMap()
        val access = ViewerAccess(configured.any { it.kind == ServiceKind.SEERR }, identities)
        configured.filter { it.kind == ServiceKind.JELLYFIN || it.kind == ServiceKind.EMBY }
            .map { connection -> async {
                runCatching { mediaServerClient.sessions(connection, access).map { playbackSession(it, connection.kind) } }
                    .getOrDefault(emptyList())
            } }.awaitAll().flatten().distinctBy { it.key }
    }

    suspend fun refresh(
        connections: List<ServiceConnection>,
        /**
         * Whether the shared recommendation list should be fetched at all. It is the one call
         * Spole makes to a host that is not the user's own server, so switching the row off in
         * Innstillingar has to stop the request, not merely hide the result. Otherwise the privacy
         * note would be describing something the app does not do.
         */
        includeRecommendations: Boolean = true,
        onLibraryReady: (LibraryFeedUpdate) -> Unit = {},
    ): MediaSyncSnapshot = supervisorScope {
        val configured = connections.filter { it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        val profileJobs = configured.filter { it.kind in setOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY) }
            .associate { connection -> connection.kind to async(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { accountProfileClient.load(connection) }.getOrNull()
            } }
        val mediaJobs = configured.filter { it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) }
            .associate { connection -> connection.kind to async(kotlinx.coroutines.Dispatchers.IO) {
                val profile = profileJobs[connection.kind]?.await()
                val seerr = profileJobs[ServiceKind.SEERR]?.takeIf { it.isCompleted }?.await()
                val ownAccess = ViewerAccess(true, buildMap {
                    profile?.let { put(connection.kind, it) }
                    seerr?.let { put(ServiceKind.SEERR, it) }
                })
                val result = fetchWithFailover(connection, ownAccess)
                val feed = (result.first.getOrNull() as? ServicePayload.Media)?.feed
                if (profile != null && feed != null) {
                    fun mapped(items: List<RemoteLibraryItem>) = items.map { libraryMedia(it, connection.kind) }
                    onLibraryReady(LibraryFeedUpdate(connection.kind, mapped(feed.resume), mapped(feed.nextUp), mapped(feed.recentMovies),
                        mapped(feed.recentSeries), mapped(feed.favourites)))
                }
                result
            } }
        val identities = profileJobs.mapNotNull { (kind, job) -> job.await()?.let { kind to it } }.toMap()
        val access = ViewerAccess(configured.any { it.kind == ServiceKind.SEERR }, identities)
        val deferred = configured.map { connection ->
            async {
                val mediaJob = mediaJobs[connection.kind]
                val result = if (mediaJob == null) fetchWithFailover(connection, access) else {
                    val early = mediaJob.await()
                    if (access.ownMediaUser(connection.kind) == null) fetchWithFailover(connection, access)
                    else if (access.canSeeAllSessions(connection.kind)) {
                        val payload = early.first.getOrNull() as? ServicePayload.Media
                        if (payload == null) early else Result.success<ServicePayload>(payload.copy(feed = payload.feed.copy(
                            sessions = runCatching { mediaServerClient.sessions(connection, access) }.getOrDefault(payload.feed.sessions)))) to early.second
                    } else early
                }
                connection.kind to result
            }
        }
        val awaited = deferred.map { it.await() }
        val switchedToAlternate = awaited.filter { it.second.second }.mapTo(mutableSetOf()) { it.first }
        val results = awaited.map { it.first to it.second.first }
        val successful = results.filter { it.second.isSuccess }.mapTo(mutableSetOf()) { it.first }
        val errors = results.mapNotNull { (kind, result) ->
            result.exceptionOrNull()?.let { kind to friendlyError(kind, it) }
        }.toMap()

        val payloads = results.mapNotNull { it.second.getOrNull() }
        val mediaPayloads = payloads.filterIsInstance<ServicePayload.Media>()
        val warnings = mediaPayloads.mapNotNull { payload ->
            payload.feed.warnings.takeIf { it.isNotEmpty() }?.let { payload.kind to it }
        }.toMap()
        val queuePayloads = payloads.filterIsInstance<ServicePayload.Queue>()
        val queue = queuePayloads.flatMap { it.feed.queue }
        val catalogueResult = configured.firstOrNull { it.kind == ServiceKind.SEERR && it.kind in successful }?.let { connection ->
            try { Result.success(seerrReleaseClient.feed(connection, mediaPayloads.flatMap { payload ->
                payload.feed.releaseCandidates.map { app.reelstack.data.network.LibraryReleaseCandidate(it, payload.kind) }
            })) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
        }
        val catalogue = catalogueResult?.getOrNull()
        val upcoming = mergeReleaseItems(queuePayloads.flatMap { payload ->
            payload.feed.upcoming.mapNotNull(::upcomingMedia)
        }).sortedBy(UpcomingMedia::airDateEpochMillis)
        val recentReleases = mergeReleaseItems(mediaPayloads.flatMap { payload ->
            payload.feed.recentReleases.mapNotNull(::upcomingMedia)
        } + catalogue?.recent.orEmpty().mapNotNull(::upcomingMedia)).sortedByDescending(UpcomingMedia::airDateEpochMillis)
            // One latest episode per series keeps imported season batches from hiding new movies.
            .distinctBy { "${it.mediaType.lowercase(Locale.ROOT)}:${it.title.lowercase(Locale.ROOT).trim()}" }
        val seerr = payloads.filterIsInstance<ServicePayload.Seerr>().firstOrNull()?.feed
        val discover = seerr?.discover.orEmpty().map(::discoverMedia)
        val recommendationResult = if (!includeRecommendations) Result.success(emptyList()) else runCatching {
            recommendationsClient.feed().map(::recommendationMedia)
        }
        val recommendationSeed = recommendationResult.getOrDefault(emptyList())
        // The public recommendation feed deliberately contains no private Seerr state. Resolve
        // that state during refresh instead of waiting for a tap on the card. This means Home can
        // truthfully label a title as available/requested before the detail sheet opens.
        val seerrConnection = configured.firstOrNull { it.kind == ServiceKind.SEERR && it.kind in successful }
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
                                facts = (remote.facts.map(words) + recommendation.facts).distinct(),
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
        }.distinctBy { it.id }.take(30)

        MediaSyncSnapshot(
            sessions = mediaPayloads.flatMap { payload ->
                payload.feed.sessions.map { playbackSession(it, payload.kind) }
            },
            resume = interleave(mediaPayloads.map { payload ->
                payload.feed.resume.map { item -> libraryMedia(item, payload.kind) }
            }).take(12),
            recentMovies = interleave(mediaPayloads.map { payload ->
                payload.feed.recentMovies.map { item -> libraryMedia(item, payload.kind) }
            }).take(24),
            nextUp = interleave(mediaPayloads.map { payload ->
                payload.feed.nextUp.map { item -> libraryMedia(item, payload.kind) }
            }).distinctBy { it.id }.take(24),
            favourites = interleave(mediaPayloads.map { payload ->
                payload.feed.favourites.map { item -> libraryMedia(item, payload.kind) }
            }).distinctBy { it.id }.take(24),
            recentSeries = interleave(mediaPayloads.map { payload ->
                payload.feed.recentSeries.map { item -> libraryMedia(item, payload.kind) }
            }).take(24),
            upcoming = upcoming.take(30),
            recentReleases = recentReleases.take(30),
            incoming = if (access.isAdmin) queue.map(::incomingMedia).distinctBy { it.id } else emptyList(),
            discover = discover,
            recommendations = recommendations,
            recommendationsError = recommendationResult.exceptionOrNull()?.let { friendlyError(ServiceKind.SEERR, it) },
            activity = activity,
            successfulServices = successful,
            errors = errors,
            refreshedAt = Instant.now(),
            warnings = warnings,
            adminView = access.isAdmin,
            switchedToAlternate = switchedToAlternate,
            upcomingError = if (errors.keys.any { it in setOf(ServiceKind.RADARR, ServiceKind.SONARR) })
                LocalizedText(R.string.err_nokre_utgjevingsdatoar) else null,
            recentReleasesError = if (catalogueResult?.isFailure == true || catalogue?.incomplete == true ||
                mediaPayloads.any { it.feed.releasesFailed } || errors.keys.any { it != ServiceKind.SEERR })
                LocalizedText(R.string.err_nokre_nye_utgjevingar) else null,
        )
    }

    /**
     * Everything one account may open, library by library, already mapped for the screen.
     *
     * The kid shell shows what the server grants and nothing else: the boundary is the account,
     * not a list the client curates. `browseLibraries` returns exactly the views this token can
     * see, so a library the parent did not grant never appears here in the first place.
     *
     * Paged, with a ceiling, because a shelf of a thousand posters is a scroll no child finishes
     * and a request no home server should have to answer at once.
     */
    fun accountLibrary(connection: ServiceConnection, perLibraryLimit: Int = 300): List<LibraryMedia> {
        val views = runCatching { mediaServerClient.browseLibraries(connection) }.getOrDefault(emptyList())
        val out = mutableListOf<LibraryMedia>()
        for (view in views) {
            var offset = 0
            while (offset < perLibraryLimit) {
                val page = runCatching {
                    mediaServerClient.browseLibrary(connection, view.id, offset, view.collectionType)
                }.getOrDefault(emptyList())
                out += page.map { libraryMedia(it, connection.kind) }
                if (page.size < 60) break
                offset += page.size
            }
        }
        return out
    }

    /** The newest titles in one library, already mapped for the screen. */
    fun libraryPeek(connection: ServiceConnection, view: app.reelstack.data.network.RemoteLibraryView):
        List<LibraryMedia> =
        mediaServerClient.libraryPeek(connection, view).map { libraryMedia(it, connection.kind) }

    /** One library's own shelves, already mapped for the screen. */
    fun libraryShelves(connection: ServiceConnection, view: app.reelstack.data.network.RemoteLibraryView):
        Pair<List<LibraryMedia>, List<LibraryMedia>> {
        val (resume, nextUp) = mediaServerClient.libraryShelves(connection, view)
        return resume.map { libraryMedia(it, connection.kind) } to nextUp.map { libraryMedia(it, connection.kind) }
    }

    /**
     * The three writes a card offers.
     *
     * All of them belong to the server: a local "hidden" or "favourite" list would disagree with
     * every other client and be lost on reinstall. The screen updates itself at once and the next
     * refresh reads back what the server actually stored.
     */
    fun clearResume(connection: ServiceConnection, media: LibraryMedia) =
        mediaServerClient.clearResume(connection, mediaUser(connection), requireNotNull(media.remoteId) { "title has no remote id" })

    fun setFavourite(connection: ServiceConnection, media: LibraryMedia, favourite: Boolean) =
        mediaServerClient.setFavourite(connection, mediaUser(connection), requireNotNull(media.remoteId) { "title has no remote id" }, favourite)

    fun setPlayed(connection: ServiceConnection, media: LibraryMedia, played: Boolean) =
        mediaServerClient.setPlayed(connection, mediaUser(connection), requireNotNull(media.remoteId) { "Tittelen manglar ID" }, played)

    private fun mediaUser(connection: ServiceConnection): String =
        mediaServerClient.userIdentity(connection)
            ?: serviceError(R.string.err_fann_ikkje_profilen, connection.kind.displayName)

    fun request(connection: ServiceConnection, media: DiscoverMedia, expectedUserId: String = connection.userId, seasons: Set<Int> = emptySet()) {
        val remoteId = requireNotNull(media.remoteId) { "title has no Seerr media id" }
        val mediaType = requireNotNull(media.mediaType) { "title has no Seerr media type" }
        seerrServiceClient.request(connection, mediaType, remoteId, expectedUserId, seasons)
    }

    /** One page of Seerr results plus whether another page exists. */
    data class SearchPage(val items: List<DiscoverMedia>, val page: Int, val hasMore: Boolean)

    fun search(connection: ServiceConnection, query: String, page: Int = 1): SearchPage {
        val result = seerrServiceClient.search(connection, query, page)
        return SearchPage(result.items.map(::discoverMedia), result.page, result.hasMore)
    }

    /** Searches every connected media server, so a title you already own is findable by name. */
    suspend fun searchLibraries(
        connections: List<ServiceConnection>,
        query: String,
    ): List<LibraryMedia> = supervisorScope {
        val servers = connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        val groups = servers.map { connection ->
            async {
                runCatching { mediaServerClient.search(connection, query) }.getOrNull()
                    ?.map { it to connection.kind }
            }
        }.awaitAll().filterNotNull()
        // Deduplicated by the work, not by the copy. A series held on both Jellyfin and Emby has a
        // different item id on each, so the old id-based pass could not see that they were the
        // same thing — and the user got two cards with the same TMDB poster and nothing to tell
        // them apart. Interleaving still decides which server's copy wins, so neither server's
        // whole library outranks the other's.
        val unique = interleave(groups).distinctBy { (item, _) -> workKey(item) }

        // An episode carries its *series* name as its title, so searching for a series returns the
        // series and every episode that matched — each drawn with the same poster whenever the
        // episode has no still of its own. When the series itself is among the hits, its episodes
        // are the same answer repeated; the series card is the one that leads somewhere.
        val seriesTitles = unique.filter { (item, _) -> item.mediaType.equals("Series", ignoreCase = true) }
            .mapTo(mutableSetOf()) { (item, _) -> item.title.trim().lowercase(java.util.Locale.ROOT) }
        unique.filterNot { (item, _) ->
            item.mediaType.equals("Episode", ignoreCase = true) &&
                item.title.trim().lowercase(java.util.Locale.ROOT) in seriesTitles
        }
            .map { (item, kind) -> libraryMedia(item, kind) }
            .take(24)
    }

    /**
     * Identifies the title itself across servers. TMDB's id is the reliable answer when the server
     * knows it; otherwise name, type and release year together are specific enough to catch the
     * ordinary case without merging, say, two different films that share a name.
     */
    private fun workKey(item: RemoteLibraryItem): String = item.tmdbId?.let { "tmdb:$it" }
        ?: listOf(
            item.title.trim().lowercase(java.util.Locale.ROOT),
            item.mediaType.lowercase(java.util.Locale.ROOT),
            item.premiereDate?.take(4).orEmpty(),
        ).joinToString("|")

    fun details(connection: ServiceConnection, media: DiscoverMedia): RemoteMediaDetails =
        seerrServiceClient.details(
            connection,
            requireNotNull(media.mediaType) { "media type missing" },
            requireNotNull(media.remoteId) { "media id missing" },
        )

    fun personTitles(connection: ServiceConnection, personId: String): List<LibraryMedia> =
        mediaServerClient.personTitles(connection, personId).map { libraryMedia(it, connection.kind) }

    fun details(connection: ServiceConnection, media: LibraryMedia): RemoteMediaDetails =
        mediaServerClient.details(
            connection,
            requireNotNull(media.remoteId) { "media id missing" },
        )

    fun setPlaybackPaused(
        connections: List<ServiceConnection>,
        session: PlaybackSession,
        paused: Boolean,
    ) {
        val source = session.source ?: serviceError(R.string.err_avspelingskjelda_ikkje_tilgjengeleg)
        val sessionId = session.sessionId ?: serviceError(R.string.err_avspelingsokta_ikkje_tilgjengeleg)
        val connection = connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() }
            ?: serviceError(R.string.err_tilkoplinga_ikkje_tilgjengeleg, source.displayName)
        val accounts = connections.filter { it.kind == source || it.kind == ServiceKind.SEERR }
            .mapNotNull { c -> runCatching { accountProfileClient.load(c) }.getOrNull()?.let { c.kind to it } }.toMap()
        val access = ViewerAccess(connections.any { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }, accounts)
        if (mediaServerClient.sessions(connection, access).none { it.sessionId == sessionId }) serviceError(R.string.err_avspelingsokta_ikkje_tilgjengeleg)
        mediaServerClient.setPaused(connection, sessionId, paused)
    }

    /**
     * Tries the address in use, then the alternate. Only a failure pays for the second attempt, so
     * the ordinary case at home costs exactly what it did before. Returns whether the alternate is
     * the one that answered.
     */
    private fun fetchWithFailover(
        connection: ServiceConnection,
        access: ViewerAccess,
    ): Pair<Result<ServicePayload>, Boolean> {
        val first = runCatching { fetch(connection, access) }
        if (first.isSuccess || !connection.hasAlternate) return first to false
        val swapped = connection.copy(baseUrl = connection.alternateUrl, alternateUrl = connection.baseUrl)
        val second = runCatching { fetch(swapped, access) }
        // Keep the original failure when neither route works: it describes the address the user set.
        return if (second.isSuccess) second to true else first to false
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
            requireNotNull(access.accounts[ServiceKind.SEERR]) { "Seerr account not confirmed" }))
    }

    private fun playbackSession(item: RemotePlayback, source: ServiceKind) = PlaybackSession(
        userName = item.userName,
        deviceName = item.deviceName,
        title = item.title,
        subtitle = item.subtitle,
        progress = item.progress,
        remainingMinutes = item.remainingMinutes,
        transcoding = item.transcoding,
        quality = item.quality,
        paused = item.paused,
        artworkUrl = item.artworkUrl,
        sessionId = item.sessionId,
        source = source,
        season = item.season,
        episode = item.episode,
    )

    private fun libraryMedia(item: RemoteLibraryItem, source: ServiceKind) = LibraryMedia(
        id = "${source.name.lowercase()}-${item.id}",
        title = item.title,
        subtitle = item.subtitle,
        season = item.season,
        episode = item.episode,
        progress = item.progress,
        artworkRes = R.drawable.media_placeholder,
        source = source,
        artworkUrl = item.artworkUrl,
        logoUrl = item.logoUrl,
        heroUrl = item.heroUrl,
        posterUrl = item.posterUrl,
        remoteId = item.id,
        overview = item.overview,
        facts = listOf(words(mediaKind(item.mediaType))) + item.facts.map(words),
        genres = item.genres,
        mediaType = item.mediaType,
        lastActivityEpochMillis = item.lastActivityEpochMillis,
        libraryId = item.libraryId,
        seriesId = item.seriesId,
        favourite = item.favourite,
        played = item.played,
        runtimeMinutes = item.runtimeMinutes,
        childCount = item.childCount,
        available = item.available,
        premiereDate = item.premiereDate,
        tmdbId = item.tmdbId,
        criticRating = item.criticRating,
        tmdbRating = item.tmdbRating,
        mdblistRating = item.mdblistRating,
    )

    /** One series' seasons, or one season's episodes, mapped for the screen. */
    fun seasons(connection: ServiceConnection, seriesId: String): List<LibraryMedia> =
        mediaServerClient.seasons(connection, seriesId).map { libraryMedia(it, connection.kind) }

    fun episodes(connection: ServiceConnection, seriesId: String, seasonId: String): List<LibraryMedia> =
        mediaServerClient.episodes(connection, seriesId, seasonId).map { libraryMedia(it, connection.kind) }

    fun upcomingSeason(connection: ServiceConnection, seerr: ServiceConnection, seriesId: String, season: Int): List<LibraryMedia> {
        val tmdbId = mediaServerClient.seriesTmdbId(connection, seriesId) ?: return emptyList()
        return seerrServiceClient.upcomingSeason(seerr, tmdbId, season)
    }

    /** The one episode this series should resume on, as the server sees it. */
    fun seriesNextUp(connection: ServiceConnection, seriesId: String): LibraryMedia? =
        mediaServerClient.seriesNextUp(connection, seriesId)?.let { libraryMedia(it, connection.kind) }

    private fun incomingMedia(item: RemoteQueueItem) = IncomingMedia(
        id = item.id,
        title = item.title,
        source = item.source,
        status = words(item.status),
        state = item.state,
        artworkRes = R.drawable.media_placeholder,
        artworkUrl = item.artworkUrl,
        overview = item.overview,
        facts = listOf(words(mediaKind(item.source.name))) + item.facts.map(words),
        genres = item.genres,
        progress = item.progress,
    )

    private fun upcomingMedia(item: RemoteUpcomingItem): UpcomingMedia? {
        val instant = parseCalendarInstant(item.dateTime) ?: return null
        return UpcomingMedia(
            id = "${item.source.name.lowercase()}-${item.id}",
            title = item.title,
            subtitle = mediaLine(item.mediaType, item.subtitle),
            dateLabel = calendarLabel(instant, item.source),
            airDateEpochMillis = instant.toEpochMilli(),
            artworkRes = R.drawable.media_placeholder,
            source = item.source,
            artworkUrl = item.artworkUrl,
            overview = item.overview,
            facts = listOf(words(mediaKind(item.mediaType))) + item.facts.map(words),
            genres = item.genres,
            mediaType = item.mediaType,
            physicalRelease = item.physicalRelease,
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
        facts = listOf(words(mediaKind(item.mediaType))) + item.facts.map(words),
        genres = item.genres,
    )

    private fun recommendationMedia(item: RemoteRecommendationItem) = DiscoverMedia(
        id = item.id,
        title = item.title,
        metadata = mediaLine(item.mediaType, item.metadata),
        artworkRes = R.drawable.media_placeholder,
        inLibrary = false,
        requested = false,
        artworkUrl = item.artworkUrl,
        remoteId = item.remoteId,
        mediaType = item.mediaType,
        overview = item.overview,
        facts = listOf(words(mediaKind(item.mediaType))) + item.facts.map(words),
        genres = item.genres,
    )

    /**
     * What kind of thing this is, as a word.
     *
     * Every caller already carries `mediaType`, so the word is derived rather than stored — which
     * is what makes it possible to say it in a different language without re-fetching anything.
     */
    private fun mediaKind(mediaType: String?): LocalizedText = when (mediaType?.lowercase()) {
        "movie", "radarr" -> LocalizedText(R.string.media_kind_movie)
        "series", "tv", "sonarr" -> LocalizedText(R.string.media_kind_series)
        "episode" -> LocalizedText(R.string.media_kind_episode)
        else -> LocalizedText(R.string.media_kind_video)
    }

    /** The kind, then whatever else is worth saying about it. */
    private fun mediaLine(mediaType: String?, rest: String?): String =
        listOfNotNull(words(mediaKind(mediaType)), rest?.takeIf { it.isNotBlank() }).joinToString(" · ")

    private fun queueActivity(item: RemoteQueueItem) = ActivityEvent(
        id = "activity-${item.id}",
        title = item.title,
        detail = LocalizedText(R.string.activity_detail_stage_raw, item.source.displayName, item.status),
        time = LocalizedText(R.string.time_now),
        timeEpochMillis = Instant.now().toEpochMilli(),
        progress = item.progress,
        complete = item.state == IncomingState.READY,
        source = item.source,
        artworkRes = R.drawable.media_placeholder,
        artworkUrl = item.artworkUrl,
        mediaType = if (item.source == ServiceKind.SONARR) "Episode" else "Movie",
    )

    private fun requestActivity(request: RemoteRequest, discovered: DiscoverMedia?): ActivityEvent {
        val progress = app.reelstack.data.model.requestProgress(
            request.mediaStatus, request.availableSeasons, request.seasons, request.downloads, request.status,
        )
        val created = createdInstant(request.createdAt)
        return ActivityEvent(
            id = "seerr-request-${request.id}",
            title = discovered?.title ?: request.title ?: words(LocalizedText(if (request.mediaType == "movie") R.string.release_new_title_movie else R.string.release_new_title_series)),
            detail = LocalizedText(R.string.activity_detail_stage, progress.stage.label, request.requestedBy),
            stage = progress.stage,
            time = relativeTime(created),
            timeEpochMillis = created?.toEpochMilli(),
            complete = progress.stage == app.reelstack.data.model.RequestStage.AVAILABLE,
            source = ServiceKind.SEERR,
            artworkRes = R.drawable.media_placeholder,
            artworkUrl = discovered?.artworkUrl ?: request.artworkUrl,
            // Seerr requests are shown with their poster, the same as everywhere else in the app.
            mediaType = "Movie",
        )
    }

    private fun parseCalendarInstant(value: String): Instant? =
        runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(value.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }.getOrNull()

    /**
     * The date itself, and nothing that expires.
     *
     * This used to bake "I dag" / "I morgon" into the label — and the label is cached, so a row
     * stored today still said "I dag" tomorrow. Today and tomorrow are a rendering decision that
     * belongs where the clock is read; the cache keeps the date. The month and weekday names now
     * follow the reader's language rather than a hardcoded nn-NO, and an episode's airtime follows
     * the device's 12/24-hour setting rather than always being written as 20:00.
     */
    private fun calendarLabel(instant: Instant, source: ServiceKind): String {
        val zone = ZoneId.systemDefault()
        val dateTime = instant.atZone(zone)
        val day = dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("EEE d. MMM", locale))
        return if (source == ServiceKind.SONARR) {
            "$day · ${dateTime.format(timeFormatter)}"
        } else {
            day
        }
    }

    private val timeFormatter: DateTimeFormatter =
        if (use24HourClock) DateTimeFormatter.ofPattern("HH:mm", locale)
        else DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale)

    private fun createdInstant(createdAt: String?): Instant? =
        createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }

    /**
     * Plural forms come from the resources, so "1 minutt" and "5 minutt" can differ per language —
     * and an English reader gets "1 minute ago" rather than "For 1 min sidan".
     */
    private fun relativeTime(instant: Instant?): LocalizedText {
        if (instant == null) return LocalizedText(R.string.time_recently)
        val duration = Duration.between(instant, Instant.now()).coerceAtLeast(Duration.ZERO)
        return when {
            duration.toMinutes() < 1 -> LocalizedText(R.string.time_just_now)
            duration.toHours() < 1 -> LocalizedText.plural(R.plurals.time_minutes_ago, duration.toMinutes().toInt())
            duration.toDays() < 1 -> LocalizedText.plural(R.plurals.time_hours_ago, duration.toHours().toInt())
            duration.toDays() == 1L -> LocalizedText(R.string.time_yesterday)
            else -> LocalizedText.plural(R.plurals.time_days_ago, duration.toDays().toInt())
        }
    }

    /**
     * What to put beside a service that did not answer.
     *
     * This used to guess, by searching the exception message for the nynorsk word "profil" or
     * "avviste" — which meant it only ever worked in one language, and stopped working entirely
     * once those messages became resource ids. A failure that was written for a person already
     * carries its own sentence; anything else is a defect, and the generic line is the honest
     * answer for it.
     */
    private fun friendlyError(kind: ServiceKind, error: Throwable): LocalizedText =
        error.localizedFailure() ?: LocalizedText(R.string.err_sync_generic, kind.displayName)

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

/** Prefer a verified library copy; retain distinct episodes but avoid repeated source cards. */
internal fun mergeReleaseItems(items: List<UpcomingMedia>): List<UpcomingMedia> = items.distinctBy {
    val episode = if (it.mediaType.equals("Episode", true)) Regex("S\\d+ E\\d+").find(it.subtitle)?.value ?: it.id else "movie"
    "${it.title.lowercase(Locale.ROOT).trim()}|$episode"
}

private fun <T> interleave(groups: List<List<T>>): List<T> = buildList {
    val maxSize = groups.maxOfOrNull(List<T>::size) ?: 0
    repeat(maxSize) { index ->
        groups.forEach { group -> group.getOrNull(index)?.let(::add) }
    }
}
