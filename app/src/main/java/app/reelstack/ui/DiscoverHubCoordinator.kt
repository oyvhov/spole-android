package app.reelstack.ui

import app.reelstack.data.model.CastMember
import app.reelstack.data.model.DiscoverGenre
import app.reelstack.data.model.DiscoverGridTarget
import app.reelstack.data.model.DiscoverHubState
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.DiscoverRowKind
import app.reelstack.data.model.DiscoverRowState
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.peopleFrom
import app.reelstack.data.model.withPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiscoverRowPage(val items: List<DiscoverMedia>, val page: Int, val hasMore: Boolean)

/** Everything the Discover rows need from Seerr, so the loading order can be tested without a server. */
interface DiscoverHubSource {
    suspend fun row(kind: DiscoverRowKind, page: Int): DiscoverRowPage
    suspend fun genres(): List<DiscoverGenre>
    suspend fun cast(media: DiscoverMedia): List<CastMember>
}

/** Starts every row at once and lets each land when it is ready. */
class DiscoverHubLoader(private val source: DiscoverHubSource) {
    suspend fun load(scope: String, currentScope: () -> String?, emit: ((DiscoverHubState) -> DiscoverHubState) -> Unit) = coroutineScope {
        fun land(change: (DiscoverHubState) -> DiscoverHubState) { if (currentScope() == scope) emit(change) }
        land { state ->
            state.copy(scope = scope, rows = DiscoverRowKind.entries.associateWith { kind ->
                (state.rows[kind] ?: DiscoverRowState()).copy(loading = true, failed = false)
            })
        }
        val rows = DiscoverRowKind.entries.map { kind ->
            async {
                val result = runCatching { source.row(kind, 1) }
                land { state ->
                    state.copy(rows = state.rows + (kind to result.fold(
                        onSuccess = { DiscoverRowState(it.items.distinctBy { item -> item.id }, page = it.page, hasMore = it.hasMore) },
                        onFailure = { DiscoverRowState(failed = true) },
                    )))
                }
                if (kind == DiscoverRowKind.TRENDING) result.getOrNull()?.items.orEmpty() else emptyList()
            }
        }
        val genres = async { runCatching { source.genres() }.getOrNull()?.let { list -> land { it.copy(genres = list) } } }
        // People come from the first trending titles, so they start as soon as that row has landed.
        val trending = rows.first().await()
        val casts = trending.take(6).map { media -> async { runCatching { source.cast(media) }.getOrDefault(emptyList()) } }.awaitAll()
        land { it.copy(people = peopleFrom(casts)) }
        rows.awaitAll()
        genres.await()
    }
}

/** Owns the Discover rows in the root state. */
internal class DiscoverHubCoordinator(
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val scope: CoroutineScope,
    private val readState: () -> ReelstackUiState,
    private val updateState: ((ReelstackUiState) -> ReelstackUiState) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var job: Job? = null
    private var gridJob: Job? = null

    private fun seerr(state: ReelstackUiState = readState()) =
        state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.baseUrl.isNotBlank() && it.token.isNotBlank() }

    private fun scopeKey(state: ReelstackUiState = readState()): String? =
        seerr(state)?.let { "${state.activeProfileId}:${it.baseUrl}:${it.userId}" }

    fun refresh(force: Boolean) {
        val state = readState()
        val connection = seerr(state) ?: return
        if (state.isKidMode) return
        val key = scopeKey(state) ?: return
        if (!force && state.discoverHub.scope == key && state.discoverHub.rows.values.none { it.failed }) return
        job?.cancel()
        val repository = mediaSyncRepository
        val source = object : DiscoverHubSource {
            override suspend fun row(kind: DiscoverRowKind, page: Int) = withContext(ioDispatcher) {
                repository.discoverRow(connection, kind, page).let { DiscoverRowPage(it.items, it.page, it.hasMore) }
            }
            override suspend fun genres() = withContext(ioDispatcher) { repository.discoverGenres(connection) }
            override suspend fun cast(media: DiscoverMedia) = withContext(ioDispatcher) { repository.details(connection, media).cast }
        }
        job = scope.launch {
            DiscoverHubLoader(source).load(key, { scopeKey() }) { change ->
                updateState { current -> current.copy(discoverHub = change(current.discoverHub)) }
            }
        }
    }

    fun openGrid(target: DiscoverGridTarget) {
        updateState { it.copy(discoverHub = it.discoverHub.copy(grid = target, gridRow = DiscoverRowState(loading = true))) }
        loadGrid(target, 1)
    }

    fun loadMore() {
        val hub = readState().discoverHub
        val target = hub.grid ?: return
        val row = hub.gridRow
        // After a failure the same page is asked for again; the first page if nothing came at all.
        if (row.loading || (!row.failed && !row.hasMore)) return
        updateState { it.copy(discoverHub = it.discoverHub.copy(gridRow = it.discoverHub.gridRow.copy(loading = true, failed = false))) }
        loadGrid(target, if (row.items.isEmpty()) 1 else row.page + 1)
    }

    fun closeGrid() = updateState { it.copy(discoverHub = it.discoverHub.copy(grid = null, gridRow = DiscoverRowState())) }

    private fun loadGrid(target: DiscoverGridTarget, page: Int) {
        val connection = seerr() ?: return
        gridJob?.cancel()
        gridJob = scope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    when (target) {
                        is DiscoverGridTarget.Row -> mediaSyncRepository.discoverRow(connection, target.kind, page)
                        is DiscoverGridTarget.Genre -> mediaSyncRepository.discoverGenrePage(connection, target.genre, page)
                    }
                }
            }
            updateState { state ->
                if (state.discoverHub.grid != target) return@updateState state
                val old = state.discoverHub.gridRow
                state.copy(discoverHub = state.discoverHub.copy(gridRow = result.fold(
                    onSuccess = { old.withPage(it.items, it.page, it.hasMore) },
                    onFailure = { old.copy(loading = false, failed = true) },
                )))
            }
        }
    }
}
