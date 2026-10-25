package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SmartShelf
import app.reelstack.data.model.SmartShelfPresets
import app.reelstack.data.model.mergedSmartShelves
import app.reelstack.data.model.smartShelfAccountKey
import app.reelstack.data.model.withShelf
import app.reelstack.data.repository.MediaSnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns smart shelf editing, previews, and Home shelf loads for the current session. Created and retired by ReelstackFeatures. */
internal class SmartShelfCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val mediaServerClient: app.reelstack.data.network.MediaServerClient,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val preferencesRepository: app.reelstack.data.repository.AppPreferencesRepository,
    private val selectLibrarySource: (ServiceKind) -> Unit,
    private val browseLibrary: () -> Unit,
    private val invalidateLibraryPages: () -> Unit,
    private val setHomeLayout: (app.reelstack.data.model.HomeLayout) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var smartPeekJob: Job? = null
    private var smartPreviewJob: Job? = null
    private var smartFacetsJob: Job? = null
    private var smartLibrariesJob: Job? = null
    private var catalogueFacets: Pair<String, app.reelstack.data.model.CatalogueFacets>? = null
    /**
     * What each Home row of a smart shelf was last loaded with, so a row is only asked again when it
     * changed or went stale. Declared here, above the init block: the first refresh runs from init,
     * and a property declared after it is still null by then.
     */
    private val homeShelfLoads = mutableMapOf<String, Pair<String, Long>>()
    // Two handles, not one. Loading the seasons ends by opening the first of them, and a single
    // handle meant that call cancelled the very coroutine it was running inside — correct today
    // only because nothing suspends between the cancel and the relaunch, which is not a property
    // anybody should have to verify to change this code.
    fun openSeasonShelf(season: app.reelstack.data.model.Season) {
        SmartShelfPresets.of(season)?.let { openSmartShelf(it.id) }
    }

    /** A shelf of this profile's, or a preset as it comes. */
    private fun smartShelf(id: String): SmartShelf? = mergedSmartShelves(_uiState.value.smartShelves).firstOrNull { it.id == id }

    fun smartShelfOfPath(pathId: String?): SmartShelf? = SmartShelf.idOfPath(pathId)?.let(::smartShelf)

    private fun smartShelfTitle(shelf: SmartShelf): String =
        shelf.name.ifBlank { app.reelstack.ui.components.smartShelfPresetName(shelf.id)?.let(::appString).orEmpty() }

    /**
     * A smart shelf is a library page like any other, so details, Back and the remote's focus all
     * come with it; only the listing is the shelf's own question to the server.
     */
    fun openSmartShelf(id: String) {
        val shelf = smartShelf(id) ?: return
        val state = _uiState.value
        val connection = state.smartShelfConnection(shelf) ?: return
        if (!sameSmartShelfAccount(state.libraryConnection, connection)) selectLibrarySource(connection.kind)
        _uiState.update { it.copy(selectedTab = AppTab.LIBRARY, activeSheet = null, libraryPath = listOf(shelf.pathId to smartShelfTitle(shelf)),
            libraryCollectionType = null, libraryFilters = app.reelstack.data.model.LibraryFilters(),
            libraryFacets = app.reelstack.data.model.LibraryFacets(), libraryEntries = emptyList(),
            libraryOffset = 0, libraryHasMore = false) }
        browseLibrary()
    }

    /**
     * A few titles for each shelf's tile on the library page, all asked for together. A shelf that
     * found nothing keeps an empty answer, so it is not asked again until it changes.
     */
    fun loadSmartShelfPeeks() {
        val state = _uiState.value
        val connection = state.libraryConnection ?: return
        val missing = app.reelstack.data.model.libraryShelves(state.smartShelves, java.time.LocalDate.now())
            .filter { it.hasRule && it.appliesTo(connection) && it.id !in state.smartShelfPeeks }
        if (missing.isEmpty()) return
        smartPeekJob?.cancel()
        smartPeekJob = operationScope.launch {
            // What the tiles held last time first, so their posters are there before the server answers.
            val remembered = withContext(ioDispatcher) {
                missing.associate { shelf -> shelf.id to runCatching {
                    mediaServerClient.smartShelf(connection, shelf.forViewer(state.isKidMode), limit = 4, cachedOnly = true)
                }.getOrDefault(emptyList()) }.filterValues { it.isNotEmpty() }
            }
            fun ownsPeeks(current: ReelstackUiState) = isActive && current.activeProfileId == state.activeProfileId &&
                current.sessionScope == state.sessionScope && sameSmartShelfAccount(current.libraryConnection, connection)
            if (remembered.isNotEmpty() && ownsPeeks(_uiState.value)) {
                _uiState.update { current ->
                    val valid = remembered.filterKeys { id ->
                        val old = missing.firstOrNull { it.id == id }
                        val latest = mergedSmartShelves(current.smartShelves).firstOrNull { it.id == id }
                        old != null && latest != null && sameSmartShelfQuery(old, latest)
                    }
                    if (ownsPeeks(current)) current.copy(smartShelfPeeks = valid + current.smartShelfPeeks) else current
                }
            }
            kotlinx.coroutines.coroutineScope {
                missing.forEach { shelf -> launch {
                    val items = withContext(ioDispatcher) {
                        runCatching { mediaServerClient.smartShelf(connection, shelf.forViewer(state.isKidMode), limit = 4) }
                            .getOrNull()
                    } ?: return@launch
                    if (ownsPeeks(_uiState.value)) {
                        _uiState.update { current ->
                            val latest = mergedSmartShelves(current.smartShelves).firstOrNull { it.id == shelf.id }
                            if (ownsPeeks(current) && latest != null && sameSmartShelfQuery(latest, shelf))
                                current.copy(smartShelfPeeks = current.smartShelfPeeks + (shelf.id to items)) else current
                        }
                    }
                } }
            }
        }
    }

    /**
     * Fills the Home rows smart shelves have. Every row asks for itself and appears when its own
     * answer arrives; a hidden row asks for nothing, and a row that fails keeps what it showed.
     */
    fun loadHomeShelves(force: Boolean = false) {
        val state = _uiState.value
        val layout = state.effectiveHomeLayout
        val rows = state.homeShelfRows
        val shelves = app.reelstack.data.model.homeShelves(state.smartShelves, java.time.LocalDate.now()).associateBy { it.id }
        val rowIds = rows.mapTo(mutableSetOf()) { it.id }
        if (state.homeShelfMedia.keys.any { it !in rowIds }) {
            _uiState.update { it.copy(homeShelfMedia = it.homeShelfMedia.filterKeys { key -> key in rowIds }) }
        }
        val now = android.os.SystemClock.elapsedRealtime()
        rows.filter(layout::isVisible).forEach { key ->
            val shelf = shelves[key.shelf] ?: return@forEach
            val connection = state.connections.firstOrNull {
                it.kind == key.source && it.baseUrl.isNotBlank() && it.token.isNotBlank() && shelf.appliesTo(it)
            } ?: return@forEach
            val signature = shelf.encode().toString() + "|" + MediaSnapshotStore.fingerprint(listOf(connection)) + "|" + state.isKidMode
            val last = homeShelfLoads[key.id]
            if (!force && last != null && last.first == signature && now - last.second < HOME_SHELF_STALE_MILLIS) return@forEach
            homeShelfLoads[key.id] = signature to now
            val viewer = shelf.forViewer(state.isKidMode)
            operationScope.launch {
                var loaded = false
                fun ownsRow(current: ReelstackUiState): Boolean {
                    val latest = mergedSmartShelves(current.smartShelves).firstOrNull { it.id == shelf.id }
                    return isActive && current.activeProfileId == state.activeProfileId && current.sessionScope == state.sessionScope &&
                        current.homeShelfRows.contains(key) && latest != null && sameSmartShelfQuery(latest, shelf) &&
                        sameSmartShelfAccount(current.connections.firstOrNull { it.kind == key.source }, connection)
                }
                try {
                    // The row as it was last time, at once; the server's answer replaces it below.
                    if (key.id !in _uiState.value.homeShelfMedia) {
                        val remembered = withContext(ioDispatcher) {
                            runCatching { mediaSyncRepository.smartShelfMedia(connection, viewer, HOME_SHELF_LIMIT, cachedOnly = true) }
                                .getOrDefault(emptyList())
                        }
                        if (remembered.isNotEmpty() && ownsRow(_uiState.value)) {
                            _uiState.update { if (ownsRow(it)) it.copy(homeShelfMedia = mapOf(key.id to remembered) + it.homeShelfMedia) else it }
                        }
                    }
                    val items = withContext(ioDispatcher) {
                        runCatching { mediaSyncRepository.smartShelfMedia(connection, viewer, HOME_SHELF_LIMIT) }.getOrNull()
                    } ?: return@launch
                    if (ownsRow(_uiState.value)) {
                        _uiState.update { if (ownsRow(it)) it.copy(homeShelfMedia = it.homeShelfMedia + (key.id to items)) else it }
                        loaded = true
                    }
                } finally {
                    // A load that failed or was cancelled half-way, by a profile check or a new refresh,
                    // must not count as done, or the row would wait out the whole ten minutes empty.
                    if (!loaded && homeShelfLoads[key.id]?.first == signature) homeShelfLoads.remove(key.id)
                }
            }
        }
    }

    /**
     * The shelf builder. [id] edits a shelf; [template] starts a new one from a preset's rule; neither
     * starts empty. A new shelf from a template is the owner's own and keeps nothing of the preset
     * but its rule, icon and period.
     */
    fun openSmartShelfEditor(id: String? = null, template: String? = null) {
        closeSmartShelfEditor()
        val state = _uiState.value
        val existing = id?.let(::smartShelf)
        val initialScope = state.newSmartShelfScope()
        if (existing == null && initialScope == null) return
        // A shelf someone makes is one they want to see: on Home as well as in the library, and from
        // today. A template lends its rule; its season stays with the built-in shelf.
        val draft = existing ?: template?.let { name -> SmartShelfPresets.all.firstOrNull { it.id == name } }
            ?.let { it.copy(id = newSmartShelfId(), name = smartShelfTitle(it), preset = false,
                period = app.reelstack.data.model.SmartShelfPeriod.ALWAYS, onHome = true, scope = initialScope) }
            ?: SmartShelf(id = newSmartShelfId(), onHome = true, scope = initialScope)
        val sources = state.connections.filter {
            it.kind in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) && it.baseUrl.isNotBlank() &&
                it.token.isNotBlank() && it.userId.isNotBlank()
        }.map { app.reelstack.data.model.SmartShelfSourceOption(smartShelfAccountKey(it), it.kind, it.kind.displayName) }
        _uiState.update { it.copy(smartShelfEditor = app.reelstack.data.model.SmartShelfEditor(draft,
            isNew = existing == null, sources = sources)) }
        loadSmartShelfLibraries()
        loadCatalogueFacets()
        previewSmartShelf()
    }

    private fun newSmartShelfId() = "u-" + java.util.UUID.randomUUID().toString().take(8)

    /** Every change asks the server again, a moment after the last one, what the shelf would hold. */
    fun updateSmartShelfDraft(draft: SmartShelf) {
        val editor = _uiState.value.smartShelfEditor ?: return
        if (editor.draft == draft) return
        val scopeChanged = editor.draft.scope != draft.scope
        val accountChanged = !sameSmartShelfSource(editor.draft, draft)
        val ruleChanged = !sameSmartShelfQuery(editor.draft, draft)
        _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.let { current ->
            if (scopeChanged) current.copy(draft = draft,
                libraries = if (accountChanged) emptyList() else current.libraries,
                librariesFailed = if (accountChanged) false else current.librariesFailed,
                facets = app.reelstack.data.model.CatalogueFacets(), facetsFailed = false, facetsError = null,
                previewMovies = null, previewSeries = null, previewCapped = false)
            else current.copy(draft = draft)
        }) }
        if (scopeChanged) {
            if (accountChanged) loadSmartShelfLibraries()
            loadCatalogueFacets()
        }
        if (ruleChanged) previewSmartShelf(delayMillis = 350)
    }

    /** Starts the rule over from a preset, keeping a name the owner has already typed. */
    fun applySmartShelfTemplate(template: String) {
        val editor = _uiState.value.smartShelfEditor ?: return
        val preset = SmartShelfPresets.all.firstOrNull { it.id == template } ?: return
        updateSmartShelfDraft(editor.draft.copy(name = editor.draft.name.ifBlank { smartShelfTitle(preset) },
            icon = preset.icon, tags = preset.tags, genres = preset.genres, kinds = preset.kinds,
            matchAll = preset.matchAll))
    }

    fun closeSmartShelfEditor() {
        smartPreviewJob?.cancel()
        smartFacetsJob?.cancel()
        smartLibrariesJob?.cancel()
        _uiState.update { it.copy(smartShelfEditor = null) }
    }

    fun saveSmartShelf() {
        val state = _uiState.value
        val editor = state.smartShelfEditor ?: return
        val trimmed = editor.draft.copy(name = editor.draft.name.trim().take(40))
        if (!trimmed.hasRule || trimmed.scope?.libraryIds?.isEmpty() == true) return
        val shelf = if (!trimmed.preset && trimmed.name.isBlank()) trimmed.copy(name = appString(R.string.smart_shelf_untitled)) else trimmed
        val stored = state.smartShelves.withShelf(shelf)
        preferencesRepository.setSmartShelves(state.activeProfileId, stored)
        invalidateLibraryPages()
        closeSmartShelfEditor()
        _uiState.update { it.copy(smartShelves = stored, smartShelfPeeks = it.smartShelfPeeks - shelf.id,
            homeShelfMedia = it.homeShelfMedia.filterKeys { key -> app.reelstack.data.model.HomeRowKey.parse(key)?.shelf != shelf.id }) }
        loadHomeShelves()
        openSmartShelf(shelf.id)
    }

    /**
     * Changes a shelf's order from its own page. It is saved with the shelf, so the page, its tile
     * and its Home row agree, and asked of the server again, which does the sorting.
     */
    fun setSmartShelfSort(id: String, sort: app.reelstack.data.model.SmartShelfSort) {
        val state = _uiState.value
        val shelf = smartShelf(id)?.takeIf { it.sort != sort } ?: return
        val stored = state.smartShelves.withShelf(shelf.copy(sort = sort))
        preferencesRepository.setSmartShelves(state.activeProfileId, stored)
        invalidateLibraryPages()
        _uiState.update { it.copy(smartShelves = stored, smartShelfPeeks = it.smartShelfPeeks - id) }
        if (SmartShelf.idOfPath(state.libraryPath.lastOrNull()?.first) == id) browseLibrary()
        loadHomeShelves()
    }

    /** The owner's own shelf goes; a preset goes back to how it came. */
    fun deleteSmartShelf(id: String) {
        val state = _uiState.value
        val shelf = smartShelf(id) ?: return
        val stored = state.smartShelves.filterNot { it.id == id }
        preferencesRepository.setSmartShelves(state.activeProfileId, stored)
        invalidateLibraryPages()
        closeSmartShelfEditor()
        _uiState.update { it.copy(smartShelves = stored, smartShelfPeeks = it.smartShelfPeeks - id,
            homeShelfMedia = it.homeShelfMedia.filterKeys { key -> app.reelstack.data.model.HomeRowKey.parse(key)?.shelf != id }) }
        // A deleted shelf takes its Home rows out of the saved layout; a preset only goes back to how it came.
        if (!shelf.preset) _uiState.value.homeLayout?.takeIf { layout -> layout.order.any { it.shelf == id } }
            ?.let { setHomeLayout(it.withoutShelf(id)) }
        val showing = SmartShelf.idOfPath(state.libraryPath.lastOrNull()?.first) == id
        if (showing && shelf.preset) openSmartShelf(id)
        else if (showing) {
            _uiState.update { it.copy(libraryPath = emptyList(), libraryEntries = emptyList(), libraryOffset = 0, libraryHasMore = false) }
            browseLibrary()
        }
    }

    fun retryCatalogueFacets() {
        catalogueFacets = null
        loadSmartShelfLibraries()
        loadCatalogueFacets()
    }

    private fun loadSmartShelfLibraries() {
        smartLibrariesJob?.cancel()
        val state = _uiState.value
        val draft = state.smartShelfEditor?.draft ?: return
        val connection = state.smartShelfConnection(draft)
        if (draft.scope == null || connection == null) {
            _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(libraries = emptyList(),
                librariesLoading = false, librariesFailed = draft.scope != null)) }
            return
        }
        _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(librariesLoading = true, librariesFailed = false)) }
        smartLibrariesJob = operationScope.launch {
            val outcome = withContext(ioDispatcher) { runCatching { mediaServerClient.browseLibraries(connection) } }
            if (!isActive) return@launch
            _uiState.update { current ->
                if (!current.ownsSmartShelfEditorResult(state, draft, connection, matchLibraries = false)) current
                else current.copy(smartShelfEditor = current.smartShelfEditor?.copy(librariesLoading = false,
                    librariesFailed = outcome.isFailure, libraries = outcome.getOrNull().orEmpty()
                        .map { app.reelstack.data.model.SmartShelfLibraryOption(it.id, it.name) }))
            }
        }
    }

    private fun loadCatalogueFacets() {
        smartFacetsJob?.cancel()
        val state = _uiState.value
        val draft = state.smartShelfEditor?.draft ?: return
        val connection = state.smartShelfConnection(draft)
        if (connection == null) {
            _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(
                facets = app.reelstack.data.model.CatalogueFacets(), facetsLoading = false, facetsFailed = true, facetsError = null)) }
            return
        }
        val key = state.activeProfileId + "|" + MediaSnapshotStore.fingerprint(listOf(connection)) + "|" + draft.scope?.encode()
        catalogueFacets?.takeIf { it.first == key }?.let { (_, facets) ->
            _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(facets = facets,
                facetsLoading = false, facetsFailed = false, facetsError = null)) }
            return
        }
        _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(facetsLoading = true, facetsFailed = false, facetsError = null)) }
        smartFacetsJob = operationScope.launch {
            val outcome = withContext(ioDispatcher) {
                runCatching { mediaServerClient.catalogueFacets(connection, draft.scope?.libraryIds) }
            }
            val facets = outcome.getOrNull()
            if (!isActive) return@launch
            if (!_uiState.value.ownsSmartShelfEditorResult(state, draft, connection)) return@launch
            if (facets != null) catalogueFacets = key to facets
            _uiState.update { current ->
                if (!current.ownsSmartShelfEditorResult(state, draft, connection)) current
                else current.copy(smartShelfEditor = current.smartShelfEditor?.copy(
                    facets = facets ?: app.reelstack.data.model.CatalogueFacets(), facetsLoading = false, facetsFailed = facets == null,
                    facetsError = outcome.exceptionOrNull()?.let { (it.message ?: it::class.java.simpleName).take(140) }))
            }
        }
    }

    private fun previewSmartShelf(delayMillis: Long = 0) {
        val state = _uiState.value
        val draft = state.smartShelfEditor?.draft ?: return
        val connection = state.smartShelfConnection(draft)
        smartPreviewJob?.cancel()
        if (connection == null || !draft.hasRule || draft.scope?.libraryIds?.isEmpty() == true) {
            _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(previewMovies = null, previewSeries = null,
                previewCapped = false, previewLoading = false)) }
            return
        }
        _uiState.update { it.copy(smartShelfEditor = it.smartShelfEditor?.copy(previewLoading = true)) }
        smartPreviewJob = operationScope.launch {
            kotlinx.coroutines.delay(delayMillis)
            val items = withContext(ioDispatcher) {
                runCatching { mediaServerClient.smartShelf(connection, draft.forViewer(state.isKidMode)) }.getOrNull()
            }
            if (!isActive) return@launch
            _uiState.update { current ->
                val editor = current.smartShelfEditor ?: return@update current
                if (!current.ownsSmartShelfEditorResult(state, draft, connection) || !sameSmartShelfQuery(editor.draft, draft))
                    return@update current
                current.copy(smartShelfEditor = editor.copy(previewLoading = false,
                    previewSeries = items?.count { it.mediaType.equals("Series", ignoreCase = true) },
                    previewMovies = items?.count { !it.mediaType.equals("Series", ignoreCase = true) },
                    previewCapped = (items?.size ?: 0) >= 60))
            }
        }
    }

    fun cancelPeeks() { smartPeekJob?.cancel() }
    private companion object {
        const val HOME_SHELF_LIMIT = 24
        const val HOME_SHELF_STALE_MILLIS = 10 * 60_000L
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
