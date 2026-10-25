package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.ActivityEvent
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.RequestSeason
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.asRequested
import app.reelstack.data.model.canChooseRequestOptions
import app.reelstack.data.model.canRequest
import app.reelstack.data.model.canRequest4k
import app.reelstack.data.model.canRequestType
import app.reelstack.data.model.choice
import app.reelstack.data.model.quotaExceeded
import app.reelstack.data.model.seerrStatus
import app.reelstack.data.model.visibleSeasons
import app.reelstack.data.network.readableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns request drafts, submission, history, and follow notifications for the current session. Created and retired by ReelstackFeatures. */
internal class RequestsCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val requestHistoryRepository: app.reelstack.data.repository.RequestHistoryRepository,
    private val requestRulesClient: app.reelstack.data.network.RequestRulesClient,
    private val requestTrackingRepository: app.reelstack.data.repository.RequestTrackingRepository,
    private val openSeerrAccount: () -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var trackingJob: Job? = null
    private var historyJob: Job? = null
    private var requestDraftJob: Job? = null
    fun requestMedia(id: String) {
        val state = _uiState.value
        val media = state.knownDiscoverMedia.firstOrNull { it.id == id } ?: return
        if (!media.canRequest || state.requestingMediaIds.isNotEmpty()) return
        if (media.mediaType != "tv" && state.configuredCount > 0 && state.accounts[ServiceKind.SEERR]?.isPersonal == true &&
            state.accounts[ServiceKind.SEERR]?.canRequestType(media.mediaType ?: "movie") != true) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() }
        if (state.configuredCount > 0 && (connection?.sessionCookie != true || state.accounts[ServiceKind.SEERR]?.isPersonal != true)) {
            openSeerrAccount()
            return
        }
        requestDraftJob?.cancel()
        _uiState.update { it.copy(activeSheet = AppSheet.RequestComposer, requestDraft = RequestDraft(media), returnToCalendar = false) }
        if (connection == null && state.configuredCount == 0) {
            val seasons = if (media.mediaType == "tv") listOf(RequestSeason(1, app.reelstack.localization.LocalizedText(R.string.media_season_number, 1), 8, 1)) else emptyList()
            _uiState.update { it.copy(requestDraft = RequestDraft(media, seasons, loading = false)) }
            return
        }
        requestDraftJob = operationScope.launch {
            val result = attempt { withContext(ioDispatcher) {
                val rules = async {
                    attempt { requestRulesClient.load(requireNotNull(connection),
                        requireNotNull(state.accounts[ServiceKind.SEERR]).id, media.mediaType ?: "movie") }.getOrNull()
                }
                val actor = state.accounts[ServiceKind.SEERR]
                val type = media.mediaType ?: "movie"
                // Both only for users Seerr lets choose; everyone else gets the sheet as before.
                val fourK = async {
                    actor?.canRequest4k(type) == true &&
                        attempt { mediaSyncRepository.fourKEnabled(requireNotNull(connection), type) }.getOrDefault(false)
                }
                val options = async {
                    if (actor?.canChooseRequestOptions() == true)
                        attempt { mediaSyncRepository.requestOptions(requireNotNull(connection), type, is4k = false) }.getOrNull()
                    else null
                }
                val remote = mediaSyncRepository.details(requireNotNull(connection), media)
                val watched = actor?.let {
                    media.remoteId?.let { remoteId -> requestTrackingRepository.watchedSeasons(connection, it.id, remoteId) }
                }.orEmpty()
                DraftLoad(remote, watched, rules.await(), fourK.await(), options.await())
            } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val draft = current.requestDraft?.takeIf { it.media.id == id } ?: return@update current
                if (current.activeSheet != AppSheet.RequestComposer) return@update current
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection?.token || configured?.baseUrl != connection?.baseUrl) return@update current
                current.copy(requestDraft = result.fold(onSuccess = { (remote, watched, rules, fourK, options) ->
                    // No implicit "all seasons", including future or undated returning seasons.
                    draft.copy(loading = false, seasons = remote.seasons, seasons4k = remote.seasons4k, selected = emptySet(),
                        mediaStatus = remote.seerrStatus, mediaStatus4k = remote.status4k, nextEpisode = remote.nextEpisode,
                        watchedSeasons = watched, rules = rules,
                        fourKAvailable = fourK, options = options,
                        profileId = options?.defaultProfileId?.takeIf { id -> options.profiles.any { it.id == id } } ?: options?.profiles?.firstOrNull()?.id,
                        folder = options?.defaultFolder?.takeIf { path -> options.folders.any { it.path == path } } ?: options?.folders?.firstOrNull()?.path,
                        error = when {
                            remote.seerrStatus == 6 -> appString(R.string.flow_status_blocked_admin)
                            media.mediaType == "tv" && remote.seasons.isEmpty() -> appString(R.string.flow_status_no_seasons)
                            media.mediaType != "tv" && remote.seerrStatus in 2..5 && !(fourK && remote.status4k !in 2..5) ->
                                appString(R.string.flow_status_movie_already)
                            else -> null
                        })
                }, onFailure = { draft.copy(loading = false, error = appString(R.string.error_request_seasons)) }))
            }
        }
    }

    private data class DraftLoad(val remote: app.reelstack.data.network.RemoteMediaDetails, val watched: Set<Int>,
        val rules: app.reelstack.data.model.RequestRules?, val fourK: Boolean, val options: app.reelstack.data.model.RequestOptions?)

    /** Standard or 4K. The seasons and the lists of quality and location follow the edition. */
    fun setRequestEdition(fourK: Boolean) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        if (draft.loading || draft.sending || draft.fourK == fourK || (fourK && !draft.fourKAvailable)) return
        val choose = state.accounts[ServiceKind.SEERR]?.canChooseRequestOptions() == true
        _uiState.update { it.copy(requestDraft = draft.copy(fourK = fourK, selected = emptySet(), options = null,
            profileId = null, folder = null, optionsLoading = choose,
            error = if (draft.media.mediaType != "tv" && (if (fourK) draft.mediaStatus4k else draft.mediaStatus) in 2..5)
                appString(R.string.flow_status_movie_already) else null)) }
        if (!choose) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.token.isNotBlank() } ?: return
        operationScope.launch {
            val options = attempt { withContext(ioDispatcher) {
                mediaSyncRepository.requestOptions(connection, draft.media.mediaType ?: "movie", fourK) } }.getOrNull()
            _uiState.update { current ->
                val latest = current.requestDraft?.takeIf { it.media.id == draft.media.id && it.fourK == fourK } ?: return@update current
                current.copy(requestDraft = latest.copy(optionsLoading = false, options = options,
                    profileId = options?.defaultProfileId?.takeIf { id -> options.profiles.any { it.id == id } } ?: options?.profiles?.firstOrNull()?.id,
                    folder = options?.defaultFolder?.takeIf { path -> options.folders.any { it.path == path } } ?: options?.folders?.firstOrNull()?.path))
            }
        }
    }

    fun setRequestProfile(id: Int) = _uiState.update { state ->
        val draft = state.requestDraft?.takeIf { !it.sending && it.options?.profiles?.any { p -> p.id == id } == true } ?: return@update state
        state.copy(requestDraft = draft.copy(profileId = id))
    }

    fun setRequestFolder(path: String) = _uiState.update { state ->
        val draft = state.requestDraft?.takeIf { !it.sending && it.options?.folders?.any { f -> f.path == path } == true } ?: return@update state
        state.copy(requestDraft = draft.copy(folder = path))
    }

    fun setRequestSeason(number: Int, checked: Boolean) = _uiState.update { state ->
        val draft = state.requestDraft ?: return@update state
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.mediaStatus == 6 || draft.error != null ||
            draft.visibleSeasons.none { it.number == number && it.canRequest } ||
            (state.configuredCount > 0 && state.accounts[ServiceKind.SEERR]?.canRequestType("tv") != true)) return@update state
        state.copy(requestDraft = draft.copy(selected = if (checked) draft.selected + number else draft.selected - number))
    }

    fun setRequestNotification(enabled: Boolean) = _uiState.update { state ->
        state.copy(requestDraft = state.requestDraft?.takeUnless { it.sending }?.copy(notify = enabled) ?: state.requestDraft)
    }

    fun confirmRequest() {
        val draft = _uiState.value.requestDraft ?: return
        if (draft.quotaExceeded || draft.rules?.canRequest == false) return
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.error != null || draft.optionsLoading ||
            (draft.media.mediaType == "tv" && draft.selected.isEmpty())) return
        sendRequest(draft.media.id)
    }

    fun setSeasonWatch(number: Int, enabled: Boolean) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        if (draft.loading || draft.sending || draft.savingWatch != null || draft.error != null || draft.mediaStatus == 6 ||
            draft.visibleSeasons.none { it.number == number && it.canWatch }) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() }
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal }
        if (connection == null || actor == null) return
        _uiState.update { it.copy(requestDraft = draft.copy(savingWatch = number, watchError = null)) }
        operationScope.launch {
            val result = attempt { withContext(ioDispatcher) {
                requestTrackingRepository.setSeasonWatch(connection, actor.id, draft.media, number, enabled)
                requestTrackingRepository.list(requestTrackingRepository.scope(connection, actor.id))
            } }
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl || !configured.sessionCookie ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id) return@update current
                val openDraft = current.requestDraft?.takeIf { it.media.id == draft.media.id }
                current.copy(
                    trackedRequests = result.getOrNull() ?: current.trackedRequests,
                    requestDraft = openDraft?.copy(savingWatch = null,
                        watchedSeasons = if (result.isSuccess) {
                            if (enabled) openDraft.watchedSeasons + number else openDraft.watchedSeasons - number
                        } else openDraft.watchedSeasons,
                        watchError = if (result.isFailure) appString(R.string.error_save_watch) else null,
                    ) ?: current.requestDraft,
                    snackbar = if (result.isSuccess) {
                        if (enabled) appString(R.string.flow_notify_season_on, number) else appString(R.string.flow_notify_season_off, number)
                    } else current.snackbar,
                )
            }
        }
    }

    fun setFollowNotification(key: String, enabled: Boolean) {
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR } ?: return
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal } ?: return
        // Move the switch now, store it after. This used to run on the caller's thread, which is
        // the UI one: three parses of the follow list — up to a hundred entries — and a blocking
        // commit() for a single tap on a bell.
        _uiState.update { current ->
            current.copy(
                trackedRequests = current.trackedRequests.map {
                    if (it.key == key) it.copy(notify = enabled) else it
                },
            )
        }
        operationScope.launch {
            val stored = attempt {
                withContext(ioDispatcher) {
                    val scope = requestTrackingRepository.scope(connection, actor.id)
                    // Read back what is on disk rather than what we asked for, so a write that
                    // did not happen puts the switch back instead of leaving a lie on screen.
                    runCatching { requestTrackingRepository.setNotify(scope, key, enabled) }
                    requestTrackingRepository.list(scope)
                }
            }.getOrNull()
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id
                ) {
                    return@update current
                }
                current.copy(trackedRequests = stored ?: current.trackedRequests)
            }
        }
    }

    fun cancelTrackedRequest(key: String) {
        val state = _uiState.value
        if (key in state.cancellingRequestKeys) return
        val connection = state.connections.firstOrNull {
            it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank()
        } ?: return
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal } ?: run {
            openSeerrAccount()
            return
        }
        _uiState.update { it.copy(cancellingRequestKeys = it.cancellingRequestKeys + key) }
        operationScope.launch {
            val result = attempt {
                withContext(ioDispatcher) {
                    requestTrackingRepository.cancel(connection, actor.id, key)
                    requestTrackingRepository.list(
                        requestTrackingRepository.scope(connection, actor.id),
                    )
                }
            }
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl ||
                    current.accounts[ServiceKind.SEERR]?.id != actor.id) {
                    return@update current.copy(cancellingRequestKeys = current.cancellingRequestKeys - key)
                }
                current.copy(
                    cancellingRequestKeys = current.cancellingRequestKeys - key,
                    trackedRequests = result.getOrNull() ?: current.trackedRequests,
                    snackbar = if (result.isSuccess) {
                        if (state.trackedRequests.firstOrNull { it.key == key }?.availabilityOnly == true) appString(R.string.flow_unfollowed_season)
                        else appString(R.string.flow_withdrawn_request)
                    }
                    else result.exceptionOrNull()?.readableMessage(appContext)
                        ?: appString(R.string.error_withdraw_request),
                )
            }
        }
    }

    fun openRequestHistory() {
        _uiState.update { it.copy(showRequestHistory = true) }
        if (!_uiState.value.requestHistory.loaded) loadRequestHistory(false)
    }

    fun closeRequestHistory() {
        historyJob?.cancel()
        _uiState.update { it.copy(showRequestHistory = false, requestHistory = it.requestHistory.copy(loading = false)) }
    }

    fun loadRequestHistory(more: Boolean) {
        val state = _uiState.value
        if (state.requestHistory.loading || (more && !state.requestHistory.hasMore)) return
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() }
        val actor = state.accounts[ServiceKind.SEERR]?.takeIf { it.isPersonal }
        if (connection == null || actor == null) {
            _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                error = appContext.getString(R.string.history_sign_in))) }
            return
        }
        historyJob?.cancel()
        val starting = if (more) state.requestHistory else app.reelstack.data.model.RequestHistoryState()
        _uiState.update { it.copy(requestHistory = state.requestHistory.copy(loading = true, error = null)) }
        historyJob = operationScope.launch {
            try {
                val page = withContext(ioDispatcher) {
                    requestHistoryRepository.load(connection, actor.id, starting.nextOffset) { ensureActive() }
                }
                ensureActive()
                val current = _uiState.value
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.identity != connection.identity ||
                    !configured.sessionCookie || current.accounts[ServiceKind.SEERR]?.id != actor.id) {
                    _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                        error = appContext.getString(R.string.history_sign_in))) }
                    return@launch
                }
                val updated = starting.append(page.items, page.nextOffset, page.hasMore, page.total)
                _uiState.update { it.copy(requestHistory = updated) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: app.reelstack.data.repository.HistoryIdentityChangedException) {
                _uiState.update { it.copy(requestHistory = app.reelstack.data.model.RequestHistoryState(
                    error = appContext.getString(R.string.history_sign_in))) }
            } catch (_: Exception) {
                _uiState.update { it.copy(requestHistory = state.requestHistory.copy(loading = false, retryFromStart = !more,
                    error = appContext.getString(R.string.history_failed))) }
            }
        }
    }

    /**
     * [background] is a poll nobody asked for, so it shows no spinner and, when the answer has not
     * changed, changes no state at all. A press of the refresh button is the opposite: it must show
     * that something is happening even when the result turns out identical.
     */
    fun refreshTrackedRequests(background: Boolean = false) {
        if (trackingJob?.isActive == true) return
        val state = _uiState.value
        val connection = state.connections.firstOrNull { it.kind == ServiceKind.SEERR && it.sessionCookie && it.token.isNotBlank() } ?: run {
            _uiState.update {
                if (it.trackedRequests.isEmpty() && it.trackingError == null) it
                else it.copy(trackedRequests = emptyList(), trackingError = null)
            }
            return
        }
        if (!background) _uiState.update { it.copy(trackingLoading = true) }
        trackingJob = operationScope.launch {
            val result = attempt { withContext(ioDispatcher) { requestTrackingRepository.refresh(connection) } }
            if (!isActive) return@launch
            _uiState.update { current ->
                val configured = current.connections.firstOrNull { it.kind == ServiceKind.SEERR }
                if (configured?.token != connection.token || configured.baseUrl != connection.baseUrl || configured.sessionCookie != connection.sessionCookie) current.copy(trackingLoading = false)
                else {
                    val requests = result.getOrNull()?.second ?: current.trackedRequests
                    val error = if (result.isFailure) appString(R.string.error_request_history) else null
                    // Same reasoning as the playback poll: an unchanged answer must not produce a
                    // new state object, or every screen rebuilds to show what it already showed.
                    if (!current.trackingLoading && current.trackedRequests == requests && current.trackingError == error) current
                    else current.copy(trackingLoading = false, trackedRequests = requests, trackingError = error)
                }
            }
        }
    }


    private fun sendRequest(id: String) {
        val state = _uiState.value
        val draft = state.requestDraft ?: return
        val media = draft.media
        if (!media.canRequest || id in state.requestingMediaIds) return
        val seerr = state.connections.firstOrNull { it.kind == ServiceKind.SEERR }

        if (state.configuredCount > 0 && (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null)) {
            _uiState.update { it.copy(snackbar = appString(R.string.error_send_connect_seerr)) }
            return
        }

        if (seerr == null || seerr.baseUrl.isBlank() || media.remoteId == null || media.mediaType == null) {
            _uiState.update {
                it.copy(
                    contentDetails = it.contentDetails?.let { details ->
                        if (details.key == id) details.copy(statusTitle = appString(R.string.flow_local_status_title), statusDescription = appString(R.string.flow_local_status_desc)) else details
                    },
                    snackbar = appString(R.string.flow_local_snackbar),
                    // The preview keeps the reader on the title too; its status line says «Lagd til».
                    activeSheet = if (it.contentDetails?.key == id) AppSheet.TitleDetails(id) else null,
                    requestDraft = null,
                ).mapDiscoverMedia { item -> if (item.id == id) item.copy(requested = true) else item }
            }
            return
        }

        val account = state.accounts[ServiceKind.SEERR]
        if (!seerr.sessionCookie || account?.isPersonal != true) {
            openSeerrAccount()
            _uiState.update { it.copy(snackbar = appString(R.string.flow_login_as_yourself_snackbar)) }
            return
        }
        _uiState.update { it.copy(requestingMediaIds = it.requestingMediaIds + id, requestDraft = draft.copy(sending = true)) }
        operationScope.launch {
            val result = attempt {
                withContext(ioDispatcher) {
                    // Reconfirm the exact identity shown in the UI before any write.
                    mediaSyncRepository.request(seerr, media, expectedUserId = account.id, seasons = draft.selected, choice = draft.choice)
                    requestTrackingRepository.follow(seerr, account.id, media, draft.selected, draft.notify, is4k = draft.fourK)
                }
            }
            _uiState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        discover = current.discover.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        recommendations = current.recommendations.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        searchResults = current.searchResults.map { item -> if (item.id == id) item.copy(requested = true) else item },
                        globalSearch = current.globalSearch.copy(results = current.globalSearch.results.map { item -> if (item.id == id) item.copy(requested = true) else item }),
                        contentDetails = current.contentDetails?.let { details ->
                            if (details.key == id) details.copy(statusTitle = appString(R.string.flow_sent_as_title, account.displayName), statusDescription = appString(R.string.flow_sent_as_desc)) else details
                        },
                        activity = listOf(
                            ActivityEvent(
                                id = "seerr-request-${media.id}",
                                title = media.title,
                                detail = app.reelstack.localization.LocalizedText(
                                    R.string.flow_sent_as_title, account.displayName),
                                time = app.reelstack.localization.LocalizedText(R.string.flow_just_now),
                                timeEpochMillis = System.currentTimeMillis(),
                                source = ServiceKind.SEERR,
                                artworkRes = media.artworkRes,
                                artworkUrl = media.artworkUrl,
                            ),
                        ) + current.activity,
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = appString(R.string.flow_sent_to_seerr_as, account.displayName),
                        // Back to the title it was asked from, whose status line now says «Førespurd».
                        activeSheet = if (current.contentDetails?.key == id) AppSheet.TitleDetails(id) else null,
                        requestDraft = null,
                    ).mapDiscoverMedia { item ->
                        if (item.id == id) item.asRequested(waitsForApproval = draft.rules?.approvalRequired == true) else item
                    }
                } else {
                    current.copy(
                        requestingMediaIds = current.requestingMediaIds - id,
                        snackbar = appString(R.string.error_send_as_person, account.displayName),
                        requestDraft = draft.copy(sending = false, error = appString(R.string.error_send_failed_retry)),
                    )
                }
            }
            if (result.isSuccess) refreshTrackedRequests()
        }
    }

    fun closeComposer(): Boolean {
        if (_uiState.value.requestDraft?.sending == true) return false
        requestDraftJob?.cancel()
        _uiState.update { it.copy(requestDraft = null) }
        return true
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
