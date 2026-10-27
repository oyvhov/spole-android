package app.reelstack.ui

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the child account library and episode browser for the current session. Created and retired by ReelstackFeatures. */
internal class KidsLibraryCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val mediaServerClient: app.reelstack.data.network.MediaServerClient,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var kidsBrowseJob: Job? = null
    private var kidsLibraryJob: Job? = null
    private var launcherLibraryJob: Job? = null
    private var kidsEpisodesJob: Job? = null

    /**
     * Loads the whole of the kid's own library.
     *
     * Barnemodus does not filter: whatever the account can reach on the server, the shell shows.
     * Which libraries that is was decided in Emby or Jellyfin, not here.
     */
    fun loadKidsLibrary() {
        if (kidsLibraryJob?.isActive == true) return
        val profileId = _uiState.value.activeProfileId
        if (!_uiState.value.isKidMode) return
        val servers = _uiState.value.connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.token.isNotBlank()
        }
        if (servers.isEmpty()) {
            _uiState.update { it.copy(kidsLibraryLoading = false, kidsLibraryError = true) }
            return
        }
        _uiState.update { it.copy(kidsLibraryLoading = true, kidsLibraryError = false) }
        kidsLibraryJob = operationScope.launch {
            // One library list per server, used for both the shelves and their names; it was fetched
            // twice. Servers are independent, so they load side by side.
            val perServer = servers.map { server ->
                async(ioDispatcher) {
                    val views = runCatching { mediaServerClient.browseLibraries(server) }
                    val titles = views.mapCatching { mediaSyncRepository.accountLibrary(server, allViews = it) }
                    Triple(views.getOrDefault(emptyList()), titles.getOrDefault(emptyList()), titles.isFailure)
                }
            }.map { it.await() }
            val failed = perServer.any { it.third }
            val all = perServer.flatMap { it.second }
            val libraries = withContext(ioDispatcher) {
                perServer.flatMap { it.first }.filter { view ->
                    val type = view.collectionType?.lowercase(java.util.Locale.ROOT)
                    type in setOf("movies", "tvshows") || type.isNullOrBlank()
                }.distinctBy { it.id }
            }
            if (!isActive || _uiState.value.activeProfileId != profileId) return@launch
            _uiState.update {
                it.copy(
                    kidsLibraryLoading = false,
                    kidsLibraryError = failed,
                    kidsLibrary = all
                        .filter { media -> !media.remoteId.isNullOrBlank() }
                        .distinctBy { media -> media.source to media.remoteId }
                        .sortedBy { media -> media.title.lowercase() },
                    kidsLibraries = libraries,
                    kidsLibraryNames = libraries.map { it.id to it.name }
                )
            }
        }
    }

    /** Open only a view offered to this child by the requested server. */
    fun openLauncherLibrary(source: ServiceKind, libraryId: String) {
        if (!_uiState.value.isKidMode) return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == source && source in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) &&
                it.token.isNotBlank() && it.baseUrl.isNotBlank()
        } ?: return
        launcherLibraryJob?.cancel()
        val request = KidsLauncherLibrary(source, libraryId)
        _uiState.update { it.copy(kidsLauncherLibrary = request) }
        launcherLibraryJob = operationScope.launch {
            try {
                val result = withContext(ioDispatcher) {
                    val view = mediaServerClient.launcherLibraryView(connection, libraryId)
                    view?.let { it to mediaSyncRepository.accountLibrary(connection, allViews = listOf(it)) }
                }
                if (!isActive || !_uiState.isCurrent || _uiState.value.kidsLauncherLibrary != request) return@launch
                _uiState.update { it.copy(kidsLauncherLibrary = if (result == null) request.copy(loading = false, error = true)
                    else request.copy(view = result.first, media = result.second, loading = false)) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isActive && _uiState.isCurrent && _uiState.value.kidsLauncherLibrary == request) {
                    _uiState.update { it.copy(kidsLauncherLibrary = request.copy(loading = false, error = true)) }
                }
            }
        }
    }

    fun closeLauncherLibrary() {
        launcherLibraryJob?.cancel()
        launcherLibraryJob = null
        _uiState.update { it.copy(kidsLauncherLibrary = null) }
    }

    /**
     * Opens a series for a kid: seasons first, then the episodes of the season they are actually in.
     *
     * A movie never reaches this — it plays on the tap, which is the point of the kid shell.
     */
    fun openKidsSeries(media: LibraryMedia) {
        val target = seriesBrowseTarget(media) ?: return
        val seriesId = target.seriesId
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == media.source && it.token.isNotBlank()
        } ?: return

        kidsBrowseJob?.cancel()
        _uiState.update {
            it.copy(kidsBrowse = KidsBrowse(
                seriesId = seriesId, title = media.title, source = media.source, loading = true,
            ))
        }

        kidsBrowseJob = operationScope.launch {
            val seasons = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.seasons(connection, seriesId) }
            }.getOrDefault(emptyList())
                // Specials are season zero and almost never where a child wants to start.
                .sortedWith(compareBy({ (it.episode ?: 0) == 0 }, { it.episode ?: Int.MAX_VALUE }))
            if (!isActive || _uiState.value.kidsBrowse.seriesId != seriesId) return@launch

            // A Season link stays within that season; a global next-up could start another.
            val nextUp = if (target.initialSeasonId != null) null else runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.seriesNextUp(connection, seriesId) }
            }.getOrNull()
            val opening = nextUp?.let { next -> seasons.firstOrNull { it.episode == next.season } }
                ?: seasons.firstOrNull()

            val openingId = target.initialSeasonId ?: opening?.remoteId
            _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId) it
                else it.copy(kidsBrowse = it.kidsBrowse.copy(seasons = seasons, loading = openingId != null))
            }
            openingId?.let { selectKidsSeason(it) } ?: _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId) it else it.copy(kidsBrowse = it.kidsBrowse.copy(loading = false))
            }
        }
    }

    fun selectKidsSeason(seasonId: String) {
        val browse = _uiState.value.kidsBrowse
        if (!browse.open) return
        val connection = _uiState.value.connections.firstOrNull {
            it.kind == browse.source && it.token.isNotBlank()
        } ?: return
        val seriesId = browse.seriesId
        val profileId = _uiState.value.activeProfileId
        kidsEpisodesJob?.cancel()
        _uiState.update { it.copy(kidsBrowse = it.kidsBrowse.copy(selectedSeasonId = seasonId, episodes = emptyList(), loading = true)) }
        kidsEpisodesJob = operationScope.launch {
            val episodes = runCatching {
                withContext(ioDispatcher) { mediaSyncRepository.episodes(connection, seriesId, seasonId) }
            }.getOrDefault(emptyList())
            if (!isActive || _uiState.value.activeProfileId != profileId) return@launch
            _uiState.update {
                if (it.kidsBrowse.seriesId != seriesId || it.kidsBrowse.selectedSeasonId != seasonId) it
                else it.copy(kidsBrowse = it.kidsBrowse.copy(episodes = episodes, loading = false))
            }
        }
    }

    fun closeKidsSeries() {
        kidsBrowseJob?.cancel()
        kidsEpisodesJob?.cancel()
        _uiState.update { it.copy(kidsBrowse = KidsBrowse()) }
    }
}
