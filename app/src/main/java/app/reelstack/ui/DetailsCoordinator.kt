package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.resolvedMediaType
import app.reelstack.data.model.seerrStatus
import app.reelstack.data.model.seerrStatusDescription
import app.reelstack.data.model.seerrStatusLabel
import app.reelstack.data.model.tmdbScore
import app.reelstack.data.network.RemoteMediaDetails
import app.reelstack.data.network.toLibraryMedia
import app.reelstack.data.repository.SessionScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val remoteItemId = Regex("[A-Za-z0-9_-]{1,128}")

/** A launcher may name a Jellyfin or Emby title by a plain server id, and nothing else. */
internal fun isRemoteDetailsTarget(kind: ServiceKind, itemId: String): Boolean =
    kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && remoteItemId.matches(itemId)

/** Owns title details, cast navigation, and season/episode loading for the current session. Created and retired by ReelstackFeatures. */
internal class DetailsCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val isTelevision: () -> Boolean,
    /** The repository's live session, as `ConnectionRepository.captureSession`; an answer for another is dropped. */
    private val captureSession: () -> SessionScope,
    /**
     * A title by its server id alone, for a launcher link to one no row has fetched yet. The loader
     * leaves the main thread itself, so a test can answer it on the test dispatcher.
     */
    private val remoteDetails: suspend (ServiceConnection, String) -> RemoteMediaDetails,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var seasonsJob: Job? = null
    private var detailsJob: Job? = null
    /** Bumped by every new title, Back and close; an answer for an older request is never shown. */
    private var detailRequest = 0L
    private var episodesJob: Job? = null
    private data class EpisodeReturn(val seriesKey: String, val connection: ServiceConnection,
        val details: ContentDetails, val browse: app.reelstack.data.model.SeriesBrowse)
    private var episodeReturn: EpisodeReturn? = null
    private val detailHistory = ArrayDeque<EpisodeReturn>()
    private var personJob: Job? = null
    private val personTrail = PersonTrail<ContentDetails>()

    /** One page per Seerr person, opened from Folk, from search or from a title's cast. */
    fun openPerson(id: Int) {
        val state = _uiState.value
        val connection = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        if (state.isKidMode) return
        val known = state.discoverHub.people.firstOrNull { it.id == id }
            ?: state.searchPeople.firstOrNull { it.id == id }
            ?: state.globalSearch.people.firstOrNull { it.id == id }
            ?: state.contentDetails?.cast?.firstOrNull { it.seerrPersonId == id }?.let {
                app.reelstack.data.model.DiscoverPerson(id, it.name, it.portraitUrl, it.role) }
            ?: app.reelstack.data.model.DiscoverPerson(id, "", null)
        personTrail.openedPerson(fromTitle = state.contentDetails?.takeIf { state.activeSheet is AppSheet.TitleDetails })
        _uiState.update { it.copy(activeSheet = AppSheet.Person(id),
            personPage = app.reelstack.data.model.PersonPage(known, loading = true)) }
        personJob?.cancel()
        personJob = operationScope.launch {
            val result = attempt { withContext(ioDispatcher) { mediaSyncRepository.person(connection, id) } }
            _uiState.update { current ->
                val page = current.personPage?.takeIf { it.person.id == id } ?: return@update current
                current.copy(personPage = result.getOrNull()?.let { loaded ->
                    // The search or the cast may have a better picture than the person record.
                    loaded.copy(person = loaded.person.copy(portraitUrl = loaded.person.portraitUrl ?: page.person.portraitUrl))
                } ?: page.copy(loading = false, failed = true))
            }
        }
    }

    /** A title on a person page; Back from it returns to the person. */
    fun openPersonTitle(id: String) {
        val personId = _uiState.value.personPage?.person?.id ?: return
        openDiscoverDetails(id)
        if (_uiState.value.activeSheet == AppSheet.TitleDetails(id)) personTrail.openedTitleFromPerson(personId)
    }

    private var focusDetailsJob: Job? = null

    /**
     * On a television the Discover header shows the picture, year and rating of the request in focus;
     * a request carries none of them itself. Each is asked about once, after the remote has rested on
     * it for a moment, and the answer is kept. A title in a row already has them.
     */
    fun focusRequestTitle(request: app.reelstack.data.model.TrackedRequest) {
        if (request.mediaId <= 0 || request.mediaType !in setOf("movie", "tv")) return
        focusDiscoverTitle(DiscoverMedia("seerr-${request.mediaType}-${request.mediaId}", request.title, "", R.drawable.media_placeholder,
            false, remoteId = request.mediaId, mediaType = request.mediaType))
    }

    private fun focusDiscoverTitle(media: DiscoverMedia) {
        val state = _uiState.value
        if (!isTelevision() || state.isKidMode || media.id in state.discoverHub.focusDetails) return
        if (media.remoteId == null || media.mediaType == null) return
        val connection = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        } ?: return
        focusDetailsJob?.cancel()
        focusDetailsJob = operationScope.launch {
            kotlinx.coroutines.delay(300)
            val remote = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.details(connection, media) }
            }.getOrNull() ?: return@launch
            val facts = (words(remote.facts) + remote.genres.take(2)).filter(String::isNotBlank)
            val details = app.reelstack.data.model.FocusDetails(facts, remote.overview?.takeIf(String::isNotBlank),
                remote.backdropUrl?.takeIf(String::isNotBlank))
            _uiState.update { it.copy(discoverHub = it.discoverHub.withFocusDetails(media.id, details)) }
        }
    }
    /** Back from a title, and the first step of closing the sheet: either way its answer is no longer wanted. */
    fun returnFromDetail(): Boolean {
        invalidateDetailRequest()
        val current = _uiState.value
        when (val step = personTrail.back(current.activeSheet is AppSheet.Person, current.personPage?.person?.id)) {
            is PersonTrail.Step.Title<*> -> {
                val details = step.details as ContentDetails
                _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(details.key), contentDetails = details) }
                return true
            }
            is PersonTrail.Step.Person -> {
                _uiState.update { it.copy(activeSheet = AppSheet.Person(step.id)) }
                return true
            }
            null -> if (current.activeSheet is AppSheet.Person) return false
        }
        val previous = detailHistory.removeLastOrNull() ?: episodeReturn
        episodeReturn = null
        if (previous != null && _uiState.value.contentDetails?.key == previous.seriesKey &&
            _uiState.value.connections.any { it.kind == previous.connection.kind && it.identity == previous.connection.identity &&
                it.userId == previous.connection.userId && it.token == previous.connection.token }) {
            seasonsJob?.cancel()
            episodesJob?.cancel()
            _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(previous.details.key),
                contentDetails = previous.details, seriesBrowse = previous.browse) }
            return true
        }
        return false
    }
    suspend fun personTitles(person: app.reelstack.data.model.CastMember, source: ServiceKind): List<LibraryMedia> {
        val connection = _uiState.value.connections.firstOrNull { it.kind == source && it.token.isNotBlank() }
            ?: return emptyList()
        val id = person.remoteId ?: return emptyList()
        return withContext(ioDispatcher) { mediaSyncRepository.personTitles(connection, id) }
    }

    fun openPersonTitle(media: LibraryMedia) {
        _uiState.update { it.copy(libraryDetailMedia = media) }
        openLibraryDetails(media.id)
    }

    fun openEpisodeDetail(media: LibraryMedia) {
        val state = _uiState.value
        val details = state.contentDetails
        val connection = state.connections.firstOrNull { it.kind == media.source && it.token.isNotBlank() }
        _uiState.update { it.copy(libraryDetailMedia = media) }
        openLibraryDetails(media.id, clearHistory = false)
        if (details != null && connection != null) {
            val ret = EpisodeReturn(media.id, connection, details, state.seriesBrowse)
            episodeReturn = ret
            detailHistory.addLast(ret)
        }
    }

    /**
     * A title from «Liknande» or «Tilrådd» on a title page. Back returns to the page it was opened
     * from, as it does from an episode, rather than closing both.
     */
    fun openRelatedDetails(id: String) {
        val state = _uiState.value
        val details = state.contentDetails
        val connection = details?.let { current -> state.connections.firstOrNull { it.kind == current.source && it.token.isNotBlank() } }
            ?: state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
        openDiscoverDetails(id)
        if (details != null && connection != null && _uiState.value.contentDetails?.key == id) {
            val ret = EpisodeReturn(id, connection, details, state.seriesBrowse)
            episodeReturn = ret
            detailHistory.addLast(ret)
        }
    }

    fun openEpisodeSeries() {
        val state = _uiState.value
        val details = state.contentDetails?.takeIf { it.mediaType.equals("Episode", true) } ?: return
        val browse = state.seriesBrowse.takeIf { it.openedFor == details.key && it.seriesId.isNotBlank() } ?: return
        val connection = state.connections.firstOrNull { it.kind == details.source && it.token.isNotBlank() } ?: return
        val candidates = state.recentSeries + state.favourites + state.libraryPeeks.values.flatten()
        val series = candidates.firstOrNull { it.source == details.source && it.remoteId == browse.seriesId && it.mediaType == "Series" }
            ?: LibraryMedia(id = "${connection.kind.name.lowercase()}-${browse.seriesId}", title = details.title,
                subtitle = "", artworkRes = R.drawable.media_placeholder, source = connection.kind,
                remoteId = browse.seriesId, mediaType = "Series")
        val selectedSeason = browse.seasons.firstOrNull { it.remoteId == browse.selectedSeasonId }?.episode ?: details.season
        _uiState.update { it.copy(libraryDetailMedia = series.copy(season = selectedSeason)) }
        openLibraryDetails(series.id, clearHistory = false)
        val ret = EpisodeReturn(series.id, connection, details, browse)
        episodeReturn = ret
        detailHistory.addLast(ret)
    }

    fun openLibraryDetails(id: String, clearHistory: Boolean = true) {
        invalidateDetailRequest()
        val request = detailRequest
        val owner = captureSession()
        if (clearHistory) {
            episodeReturn = null
            detailHistory.clear()
            personTrail.openedTitleElsewhere()
        }
        val state = _uiState.value
        val media = knownLibraryMedia(state).firstOrNull { it.id == id } ?: return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == media.source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.TitleDetails(media.id),
                contentDetails = ContentDetails(
                    key = media.id,
                    remoteId = media.remoteId,
                    tmdbId = media.tmdbId,
                    title = media.title,
                    eyebrow = "Bibliotek i ${media.source.displayName}",
                    subtitle = media.subtitle,
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    logoUrl = media.logoUrl,
                    backdropUrl = app.reelstack.data.network.heroArtworkUrl(
                        app.reelstack.data.network.mobileHeroArtworkUrl(media)),
                    source = media.source,
                    mediaType = media.mediaType,
                    loading = connection != null && media.remoteId != null,
                    statusTitle = appString(R.string.details_in_library_badge),
                    libraryAvailable = true,
                    progress = media.progress,
                    season = media.season,
                    episode = media.episode,
                    criticRating = media.criticRating,
                    tmdbRating = media.tmdbRating,
                    mdblistRating = media.mdblistRating,
                    favourite = media.favourite,
                    played = media.played,
                    statusDescription = appString(R.string.details_in_library_source, media.source.displayName),
                ),
            )
        }
        if (connection == null || media.remoteId == null) return
        // A child's title is a play panel; seasons belong to the kid shell's own series page.
        if (!owner.kidsMode) loadSeasons(connection, media)
        detailsJob = operationScope.launch {
            val started = System.nanoTime()
            val result = attempt {
                withContext(ioDispatcher) { mediaSyncRepository.details(connection, media) }
            }
            app.reelstack.data.network.PerfLog.milestone("detail loaded", started)
            if (!isActive || detailRequest != request || !isCurrent(owner)) return@launch
            result.getOrNull()?.let { remote ->
                if (!owner.kidsMode && _uiState.value.contentDetails?.key == media.id && media.seriesId == null && remote.seriesId != null) {
                    loadSeasons(connection, media.copy(seriesId = remote.seriesId, season = remote.season, episode = remote.episode))
                }
            }
            _uiState.update { current ->
                // The same id can exist on Jellyfin and Emby; only this service's answer belongs here.
                val details = current.contentDetails?.takeIf { it.key == media.id && it.source == media.source } ?: return@update current
                result.fold(
                    onSuccess = { remote ->
                        current.copy(
                            contentDetails = details.copy(
                                title = if (media.mediaType.equals("Episode", true) || media.mediaType.equals("Series", true)) {
                                    details.title
                                } else {
                                    remote.title ?: details.title
                                },
                                tagline = remote.tagline ?: details.tagline,
                                cast = remote.cast,
                                backdropUrl = remote.backdropUrl ?: details.backdropUrl,
                                logoUrl = remote.logoUrl ?: details.logoUrl,
                                trailerUrl = remote.trailerUrl,
                                season = remote.season ?: details.season,
                                episode = remote.episode ?: details.episode,
                                overview = remote.overview ?: details.overview,
                                facts = words(remote.facts),
                                criticRating = remote.criticRating ?: details.criticRating,
                                tmdbRating = remote.tmdbRating ?: details.tmdbRating,
                                mdblistRating = remote.mdblistRating ?: details.mdblistRating,
                                genres = (remote.genres + details.genres).distinct(),
                                artworkUrl = remote.artworkUrl ?: details.artworkUrl,
                                progress = remote.progress ?: details.progress,
                                remainingMinutes = remote.remainingMinutes,
                                quality = remote.quality,
                                favourite = remote.favourite,
                                played = remote.played,
                                audioTracks = remote.audioTracks,
                                subtitleTracks = remote.subtitleTracks,
                                versions = remote.versions.map { (id, name) ->
                                    app.reelstack.data.model.MediaVersion(id, name)
                                },
                                loading = false,
                            ),
                        )
                    },
                    onFailure = {
                        current.copy(contentDetails = details.copy(loading = false, error = appString(R.string.error_content_details)))
                    },
                )
            }
        }
    }

    /**
     * Opens a server id even before Home or Library has fetched a row containing it: a launcher
     * link. A title already on screen opens as itself; otherwise the page starts as a skeleton.
     */
    fun openRemoteDetails(kind: ServiceKind, itemId: String) {
        if (!isRemoteDetailsTarget(kind, itemId)) return
        val state = _uiState.value
        val cached = knownLibraryMedia(state).firstOrNull { it.source == kind && it.remoteId == itemId }
        if (cached != null) {
            openLibraryDetails(cached.id)
            return
        }
        val connection = state.connections.firstOrNull { it.kind == kind && it.token.isNotBlank() && it.baseUrl.isNotBlank() } ?: return
        invalidateDetailRequest()
        val request = detailRequest
        val owner = captureSession()
        episodeReturn = null
        detailHistory.clear()
        personTrail.openedTitleElsewhere()
        seasonsJob?.cancel(); episodesJob?.cancel()
        _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(itemId), libraryDetailMedia = null,
            seriesBrowse = app.reelstack.data.model.SeriesBrowse(),
            contentDetails = ContentDetails(key = itemId, title = "", eyebrow = "", subtitle = "",
                artworkRes = R.drawable.media_placeholder, remoteId = itemId, source = kind, loading = true,
                libraryAvailable = true, statusTitle = appString(R.string.details_in_library_badge),
                statusDescription = appString(R.string.details_in_library_source, kind.displayName))) }
        detailsJob = operationScope.launch {
            val result = attempt { remoteDetails(connection, itemId) }
            if (!isActive || detailRequest != request || !isCurrent(owner)) return@launch
            val remote = result.getOrNull()
            _uiState.update { current ->
                val details = current.contentDetails?.takeIf { it.key == itemId && it.source == kind } ?: return@update current
                if (remote == null) current.copy(contentDetails = details.copy(loading = false, error = appString(R.string.error_content_details)))
                else {
                    val loaded = details.withRemoteDetails(remote).copy(mediaType = remote.mediaType ?: "Video")
                    current.copy(contentDetails = loaded, libraryDetailMedia = LibraryMedia(
                        id = itemId, title = loaded.title, subtitle = loaded.subtitle, artworkRes = loaded.artworkRes,
                        source = kind, remoteId = itemId, artworkUrl = loaded.artworkUrl, overview = loaded.overview,
                        facts = loaded.facts, genres = loaded.genres, mediaType = loaded.mediaType ?: "Video",
                        seriesId = remote.seriesId, season = remote.season, episode = remote.episode,
                        favourite = remote.favourite, played = remote.played, progress = remote.progress,
                        logoUrl = remote.logoUrl, backdropUrl = remote.backdropUrl))
                }
            }
            if (remote != null && !owner.kidsMode) _uiState.value.libraryDetailMedia
                ?.takeIf { it.id == itemId && it.source == kind }?.let { loadSeasons(connection, it) }
        }
    }

    /** Every library title the session has on screen, including Home shelves, the kid library and the open library page. */
    private fun knownLibraryMedia(state: ReelstackUiState): Sequence<LibraryMedia> =
        sequenceOf(listOfNotNull(state.libraryDetailMedia), state.resume, state.nextUp, state.favourites, state.recentMovies,
            state.recentSeries, state.librarySearchResults, state.globalSearch.libraryResults, state.libraryPeeks.values.flatten(),
            state.libraryShelves.resume, state.libraryShelves.nextUp, state.homeShelfMedia.values.flatten(), state.kidsLibrary,
        ).flatMap { it.asSequence() } +
            state.libraryEntries.asSequence().map { it.toLibraryMedia(state.librarySource, R.drawable.media_placeholder, words(it.facts)) }

    private fun ContentDetails.withRemoteDetails(remote: RemoteMediaDetails) = copy(
        title = remote.title ?: title, tagline = remote.tagline ?: tagline, cast = remote.cast,
        backdropUrl = remote.backdropUrl ?: backdropUrl, logoUrl = remote.logoUrl ?: logoUrl,
        trailerUrl = remote.trailerUrl, season = remote.season ?: season, episode = remote.episode ?: episode,
        overview = remote.overview ?: overview, facts = words(remote.facts),
        criticRating = remote.criticRating ?: criticRating, tmdbRating = remote.tmdbRating ?: tmdbRating,
        mdblistRating = remote.mdblistRating ?: mdblistRating, genres = (remote.genres + genres).distinct(),
        artworkUrl = remote.artworkUrl ?: artworkUrl, progress = remote.progress ?: progress,
        remainingMinutes = remote.remainingMinutes, quality = remote.quality, favourite = remote.favourite,
        played = remote.played, audioTracks = remote.audioTracks, subtitleTracks = remote.subtitleTracks,
        versions = remote.versions.map { (id, name) -> app.reelstack.data.model.MediaVersion(id, name) }, loading = false)

    /** Ends the title request in flight; its answer, even one that ignored the cancel, is dropped. */
    fun invalidateDetailRequest() {
        detailRequest++
        detailsJob?.cancel()
    }

    /** Same test as `ConnectionRepository.isCurrent`, through the injected session reader. */
    private fun isCurrent(owner: SessionScope) = captureSession() == owner

    /**
     * A series' seasons, and then the first season's episodes.
     *
     * Only for a series: a film has nothing under it, and an episode's siblings belong to the
     * series page rather than to the episode. The first season is opened straight away, because a
     * list of season names with nothing under them answers nothing.
     */
    private fun loadSeasons(connection: ServiceConnection, media: LibraryMedia) {
        seasonsJob?.cancel()
        episodesJob?.cancel()
        // A series is browsed by its own id; an episode is browsed by its parent's, because the
        // useful thing on an episode page is the rest of the season it came from — without it the
        // page was a paragraph of synopsis and half a screen of black. A season is browsed by its
        // parent too, and opens on itself.
        val target = seriesBrowseTarget(media)
        if (target == null) {
            if (_uiState.value.seriesBrowse != app.reelstack.data.model.SeriesBrowse()) {
                _uiState.update { it.copy(seriesBrowse = app.reelstack.data.model.SeriesBrowse()) }
            }
            return
        }
        val seriesId = target.seriesId
        _uiState.update { it.copy(seriesBrowse = app.reelstack.data.model.SeriesBrowse(
            seriesId = seriesId, openedFor = media.id, loading = true)) }
        seasonsJob = operationScope.launch {
            // Where the reader is in the series does not depend on the season list. Asking for
            // both at once puts the episodes on screen one round trip (sometimes two) sooner.
            val nextUpLookup = async(ioDispatcher) {
                // A Season link stays within that season; a global next-up could start another.
                if (target.initialSeasonId != null) null
                else runCatching { mediaSyncRepository.seriesNextUp(connection, seriesId) }.getOrNull()
            }
            val loaded = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.seasons(connection, seriesId) }
            }
            if (!isActive || _uiState.value.seriesBrowse.seriesId != seriesId) return@launch
            // A season carries its own number in IndexNumber, which the parser maps to `episode`;
            // `season` on a season item is the series' own index and is usually absent. Specials are
            // season zero and almost never where anyone wants to start, so they go last.
            val seasons = loaded.getOrDefault(emptyList())
                .sortedWith(compareBy({ (it.episode ?: 0) == 0 }, { it.episode ?: Int.MAX_VALUE }))
            _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId) it
                else it.copy(seriesBrowse = it.seriesBrowse.copy(
                    seasons = seasons, loading = false,
                    error = if (loaded.isFailure) appContext.getString(R.string.detail_seasons_failed) else null,
                ))
            }
            // The season the reader is actually in the middle of, when the server knows one.
            val nextUp = nextUpLookup.await()
            if (isActive && nextUp != null) _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId) it else it.copy(seriesBrowse = it.seriesBrowse.copy(nextUp = nextUp))
            }
            // An episode page opens on its own season, whatever the server thinks comes next in the
            // series: the reader is looking at episode eight of season seven, not at the series.
            val opening = media.season
                ?.let { own -> seasons.firstOrNull { it.episode == own } }
                ?: nextUp?.let { next -> seasons.firstOrNull { it.episode == next.season } }
                ?: seasons.firstOrNull()
            (target.initialSeasonId ?: opening?.remoteId)?.let { selectSeason(it) }
        }
    }

    /** Opens one season. The seasons themselves stay put; only the episode list changes. */
    fun selectSeason(seasonId: String) {
        val state = _uiState.value
        val browse = state.seriesBrowse
        if (browse.seriesId.isBlank() || browse.selectedSeasonId == seasonId && browse.episodes.isNotEmpty()) return
        val connection = state.connections.firstOrNull {
            it.kind == state.contentDetails?.source && it.token.isNotBlank()
        } ?: return
        val seriesId = browse.seriesId
        episodesJob?.cancel()
        _uiState.update { it.copy(seriesBrowse = it.seriesBrowse.copy(selectedSeasonId = seasonId, episodes = emptyList(), loading = true, upcomingError = null)) }
        episodesJob = operationScope.launch {
            val loaded = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.episodes(connection, seriesId, seasonId) }
            }
            if (!isActive) return@launch
            _uiState.update {
                if (it.seriesBrowse.seriesId != seriesId || it.seriesBrowse.selectedSeasonId != seasonId) it
                else it.copy(seriesBrowse = it.seriesBrowse.copy(
                    episodes = app.reelstack.data.model.orderedSeasonEpisodes(loaded.getOrDefault(emptyList()),
                        browse.seasons.firstOrNull { season -> season.remoteId == seasonId }?.episode), loading = false,
                    error = if (loaded.isFailure) appContext.getString(R.string.detail_episodes_failed) else null,
                ))
            }
            // Publish playable episodes first. Optional catalogue dates never block this list.
            val seerr = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
            val number = browse.seasons.firstOrNull { it.remoteId == seasonId }?.episode
            if (loaded.isSuccess && seerr != null && number != null) {
                val announced = runCatching { withContext(ioDispatcher) {
                    mediaSyncRepository.upcomingSeason(connection, seerr, seriesId, number)
                } }
                if (!isActive) return@launch
                _uiState.update { current ->
                    if (current.seriesBrowse.seriesId != seriesId || current.seriesBrowse.selectedSeasonId != seasonId ||
                        current.connections.none { it.kind == seerr.kind && it.baseUrl == seerr.baseUrl && it.token == seerr.token && it.userId == seerr.userId } ||
                        current.connections.none { it.kind == connection.kind && it.baseUrl == connection.baseUrl && it.token == connection.token && it.userId == connection.userId }) current
                    else current.copy(seriesBrowse = current.seriesBrowse.copy(
                        episodes = app.reelstack.data.network.mergeUpcomingEpisodes(current.seriesBrowse.episodes, announced.getOrDefault(emptyList())),
                        upcomingError = if (announced.isFailure) appString(R.string.design_upcoming_failed) else null))
                }
            }
        }
    }

    fun openDiscoverDetails(id: String) {
        invalidateDetailRequest()
        val media = _uiState.value.knownDiscoverMedia.firstOrNull { it.id == id } ?: return
        personTrail.openedTitleElsewhere()
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        _uiState.update {
            it.copy(
                activeSheet = AppSheet.TitleDetails(media.id),
                contentDetails = ContentDetails(
                    key = media.id,
                    tmdbId = media.remoteId,
                    backdropUrl = media.backdropUrl,
                    title = media.title,
                    eyebrow = appString(if (media.inLibrary) R.string.details_eyebrow_in_library else R.string.details_eyebrow_discover),
                    // The kind, then the year: the detail sheet has no type badge to carry it.
                    subtitle = listOfNotNull(appString(mediaKindRes(media.mediaType)),
                        media.metadata.takeIf { it.isNotBlank() }).joinToString(" · "),
                    overview = media.overview,
                    facts = media.facts,
                    genres = media.genres,
                    artworkRes = media.artworkRes,
                    artworkUrl = media.artworkUrl,
                    source = ServiceKind.SEERR,
                    mediaType = resolvedMediaType(media.mediaType, media.metadata),
                    statusTitle = appString(seerrStatusLabel(media.seerrStatus, media.inLibrary, media.requested)),
                    libraryAvailable = media.inLibrary || media.seerrStatus == 5,
                    statusDescription = appString(seerrStatusDescription(media.seerrStatus, media.inLibrary)),
                    tmdbRating = media.tmdbScore(),
                    loading = connection != null && media.remoteId != null && media.mediaType != null,
                ),
            )
        }
        if (connection == null || media.remoteId == null || media.mediaType == null) return
        detailsJob = operationScope.launch {
            // Rotten Tomatoes comes from its own Seerr call; a title without a score answers 404.
            launch {
                val critic = attempt { withContext(ioDispatcher) { mediaSyncRepository.criticScore(connection, media) } }.getOrNull()
                    ?: return@launch
                _uiState.update { current ->
                    val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                    current.copy(contentDetails = details.copy(criticRating = critic))
                }
            }
            // The rows below the cast load beside the details, never in front of them.
            launch {
                val similar = attempt { withContext(ioDispatcher) { mediaSyncRepository.similar(connection, media) } }.getOrDefault(emptyList())
                val recommended = attempt { withContext(ioDispatcher) { mediaSyncRepository.recommended(connection, media) } }.getOrDefault(emptyList())
                _uiState.update { current ->
                    val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                    current.copy(contentDetails = details.copy(similar = similar.filterNot { it.id == media.id }.distinctBy { it.id }.take(20),
                        recommended = recommended.filterNot { it.id == media.id }.distinctBy { it.id }.take(20)))
                }
            }
            val result = attempt {
                withContext(ioDispatcher) { mediaSyncRepository.details(connection, media) }
            }
            _uiState.update { current ->
                val details = current.contentDetails?.takeIf { it.key == media.id } ?: return@update current
                result.fold(
                    onSuccess = { remote ->
                        current.copy(
                            contentDetails = details.copy(
                                title = remote.title ?: details.title,
                                statusTitle = remote.seerrStatus?.let { appString(seerrStatusLabel(it)) } ?: details.statusTitle,
                                libraryAvailable = remote.seerrStatus == 5,
                                statusDescription = remote.seerrStatus?.let { appString(seerrStatusDescription(it)) } ?: details.statusDescription,
                                tagline = remote.tagline ?: details.tagline,
                                cast = remote.cast,
                                overview = remote.overview ?: details.overview,
                                facts = (words(remote.facts) + details.facts).distinct(),
                                genres = (remote.genres + details.genres).distinct(),
                                artworkUrl = remote.artworkUrl ?: details.artworkUrl,
                                trailerUrl = remote.trailerUrl ?: details.trailerUrl,
                                tmdbRating = remote.tmdbRating ?: details.tmdbRating,
                                backdropUrl = details.backdropUrl ?: remote.backdropUrl,
                                loading = false,
                            ),
                        ).mapDiscoverMedia { item ->
                            if (item.id == id && remote.seerrStatus != null) item.copy(seerrStatus = remote.seerrStatus,
                                inLibrary = remote.seerrStatus == 5, requested = remote.seerrStatus in 2..4) else item
                        }
                    },
                    onFailure = {
                        current.copy(contentDetails = details.copy(loading = false, error = appString(R.string.error_content_details)))
                    },
                )
            }
        }
    }

    fun openRecommendationDetails(id: String) {
        // Opening a Home recommendation must preserve the user's separate Discover search.
        if (_uiState.value.recommendations.none { it.id == id }) return
        openDiscoverDetails(id)
    }

    fun openCalendarTitle(title: app.reelstack.data.model.CalendarTitle) {
        if (_uiState.value.isKidMode) return
        showLocalDetails(ContentDetails(key = "calendar:${title.key}", title = title.title,
            eyebrow = appString(R.string.calendar_title), subtitle = "",
            overview = appString(R.string.calendar_no_dates_hint), facts = emptyList(), genres = emptyList(),
            artworkRes = R.drawable.media_placeholder, artworkUrl = title.artworkUrl, source = ServiceKind.SEERR,
            mediaType = if (title.mediaType == "movie") "Movie" else "Series", tmdbId = title.tmdbId))
        _uiState.update { it.copy(returnToCalendar = true) }
    }

    fun openUpcomingDetails(id: String) {
        val recent = _uiState.value.recentReleases.firstOrNull { it.id == id }
        val media = _uiState.value.upcoming.firstOrNull { it.id == id } ?: recent ?: return
        val fromCalendar = _uiState.value.activeSheet == AppSheet.UpcomingCalendar
        val librarySources = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)
        val allowedCopy = media.takeIf { it.libraryRemoteId != null && it.source in librarySources } ?: _uiState.value.recentReleases.firstOrNull {
            media.tmdbId != null && it.tmdbId == media.tmdbId && it.mediaType == media.mediaType &&
                it.season == media.season && it.episode == media.episode && it.libraryRemoteId != null && it.source in librarySources
        }
        if (allowedCopy != null) {
            val library = LibraryMedia(id = allowedCopy.id, title = allowedCopy.title, subtitle = allowedCopy.subtitle,
                artworkRes = allowedCopy.artworkRes, source = allowedCopy.source, mediaType = allowedCopy.mediaType,
                remoteId = allowedCopy.libraryRemoteId, artworkUrl = allowedCopy.artworkUrl,
                overview = allowedCopy.overview, facts = allowedCopy.facts, genres = allowedCopy.genres,
                tmdbId = allowedCopy.tmdbId, season = allowedCopy.season, episode = allowedCopy.episode)
            _uiState.update { it.copy(libraryDetailMedia = library) }
            openLibraryDetails(library.id)
            _uiState.update { it.copy(returnToCalendar = fromCalendar) }
            return
        }
        showLocalDetails(
            ContentDetails(
                key = media.id,
                tmdbId = media.tmdbId,
                title = media.title,
                eyebrow = if (recent == null) appString(R.string.details_coming_soon_source, media.source.displayName)
                    else appString(R.string.details_recent_available_source, media.source.displayName),
                subtitle = media.subtitle,
                overview = media.overview ?: appString(R.string.details_overview_unavailable, media.source.displayName),
                facts = (media.facts + media.dateLabel + media.source.displayName + listOfNotNull(media.region)).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = media.mediaType,
                statusTitle = if (recent == null) appString(R.string.details_planned_release)
                    else if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)) appString(R.string.details_in_library)
                    else appString(R.string.details_home_release_title),
                statusDescription = if (recent == null) {
                    appString(R.string.details_expected_date_note, media.dateLabel)
                } else {
                    if (media.source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
                        appString(R.string.details_library_release_date_note, media.source.displayName)
                    else appString(R.string.details_external_release_date_note, media.dateLabel, media.source.displayName)
                },
            ),
        )
        _uiState.update { it.copy(returnToCalendar = fromCalendar) }
    }

    fun openIncomingDetails(id: String) {
        val media = _uiState.value.incoming.firstOrNull { it.id == id } ?: return
        showLocalDetails(
            ContentDetails(
                key = media.id,
                title = media.title,
                eyebrow = "Nedlasting · ${media.source.displayName}",
                subtitle = media.status,
                overview = media.overview ?: appString(R.string.details_tracked_fallback),
                facts = (media.facts + media.source.displayName + media.status).distinct(),
                genres = media.genres,
                artworkRes = media.artworkRes,
                artworkUrl = media.artworkUrl,
                source = media.source,
                mediaType = null,
                statusTitle = media.status,
                statusDescription = appString(R.string.queue_last_reported_state, media.source.displayName),
            ),
        )
    }

    fun openActivityDetails(id: String) {
        (_uiState.value.trackedRequests + _uiState.value.requestHistory.items).firstOrNull { it.key == id }?.let { tracked ->
            if (tracked.mediaId <= 0 || tracked.mediaType !in setOf("movie", "tv")) return
            val media = app.reelstack.data.model.DiscoverMedia(
                id = "seerr-${tracked.mediaType}-${tracked.mediaId}", title = tracked.title,
                metadata = if (tracked.mediaType == "tv") "Serie" else "Film", artworkRes = R.drawable.media_placeholder,
                artworkUrl = tracked.artworkUrl, inLibrary = tracked.stage == app.reelstack.data.model.RequestStage.AVAILABLE,
                remoteId = tracked.mediaId, mediaType = tracked.mediaType,
                // The line said «Førespurd» or «Lastar ned»; the page it opens must not offer to ask again.
                seerrStatus = tracked.stage.seerrStatus(),
                requested = tracked.stage in setOf(app.reelstack.data.model.RequestStage.REQUESTED,
                    app.reelstack.data.model.RequestStage.DOWNLOADING, app.reelstack.data.model.RequestStage.IMPORTING))
            _uiState.update { it.copy(detailMedia = media) }
            openDiscoverDetails(media.id)
            return
        }
        val event = _uiState.value.activity.firstOrNull { it.id == id } ?: return
        showLocalDetails(
            ContentDetails(
                key = event.id,
                title = event.title,
                eyebrow = appString(R.string.my_requests_tab_server) + event.source?.let { " · ${it.displayName}" }.orEmpty(),
                subtitle = event.detail.text(appContext),
                overview = appString(R.string.activity_registered, event.time.text(appContext).lowercase()),
                facts = listOfNotNull(event.source?.displayName, event.progress?.let { "$it %" },
                    event.time.text(appContext)),
                artworkRes = event.artworkRes ?: R.drawable.media_placeholder,
                artworkUrl = event.artworkUrl,
                source = event.source,
                mediaType = null,
            ),
        )
    }

    private fun showLocalDetails(details: ContentDetails) {
        _uiState.update { it.copy(activeSheet = AppSheet.TitleDetails(details.key), contentDetails = details, returnToCalendar = false) }
    }

    fun closeDetails() {
        invalidateDetailRequest()
        detailHistory.clear()
        episodeReturn = null
        personTrail.clear()
        personJob?.cancel()
        seasonsJob?.cancel()
        episodesJob?.cancel()
    }

    @androidx.annotation.StringRes
    private fun mediaKindRes(mediaType: String?): Int = when (mediaType?.lowercase()) {
        "movie" -> R.string.media_kind_movie
        "series", "tv" -> R.string.media_kind_series
        "episode" -> R.string.media_kind_episode
        else -> R.string.media_kind_video
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)

    /** A list of decisions, rendered once in the language the app is set to. */
    private fun words(items: List<app.reelstack.localization.LocalizedText>): List<String> =
        items.map { it.text(appContext) }
}
