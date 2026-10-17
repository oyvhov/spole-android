package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.network.readableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Owns download preparation, duplicate suppression, and progress observation for the current session. Created and retired by ReelstackFeatures. */
internal class OfflineDownloadsCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val offlineDownloads: app.reelstack.offline.OfflineDownloadRepository,
    private val isTelevision: () -> Boolean,
    private val selectTab: (AppTab) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    private var offlineRefreshJob: Job? = null
    /** In-memory only: duplicate presses must not create two jobs for the same private file. */
    private val preparingOfflineIds = mutableSetOf<String>()
    private fun startOfflineRefresh() {
        offlineRefreshJob?.cancel()
        val profileId = _uiState.value.activeProfileId
        offlineRefreshJob = operationScope.launch {
            while (isActive && _uiState.value.selectedTab == AppTab.DOWNLOADS &&
                _uiState.value.activeProfileId == profileId && !_uiState.value.isKidMode) {
                val seen = app.reelstack.offline.OfflineDownloadRuntime.changes.value
                val snapshot = withContext(ioDispatcher) { offlineDownloads.snapshot(profileId) }
                _uiState.update { current ->
                    if (current.activeProfileId == profileId && current.selectedTab == AppTab.DOWNLOADS) current.copy(offlineDownloads = snapshot)
                    else current
                }
                // Byte progress needs a tick while something transfers. Otherwise nothing changes
                // until Media3 says so, and reading the job table every second was for nothing.
                if (snapshot.hasActiveDownloads) delay(1_000L)
                else app.reelstack.offline.OfflineDownloadRuntime.changes.first { it != seen }
            }
        }
    }

    fun refreshOfflineDownloads() {
        if (_uiState.value.isKidMode || isTelevision()) return
        val profileId = _uiState.value.activeProfileId
        operationScope.launch {
            val snapshot = withContext(ioDispatcher) { offlineDownloads.snapshot(profileId) }
            _uiState.update { current -> if (current.activeProfileId == profileId) current.copy(offlineDownloads = snapshot) else current }
        }
    }

    /**
     * A detail page chooses a concrete library file, then negotiates it without opening the
     * player. The Media3 job is created only after the server confirms untouched direct playback;
     * a profile switch during that short negotiation drops the result instead of lending the old
     * account to the new profile.
     *
     * [subtitleIndex] does not change the file: every embedded subtitle is downloaded with it.
     */
    fun enqueueOfflineDownload(media: LibraryMedia, audioIndex: Int? = null,
        @Suppress("UNUSED_PARAMETER") subtitleIndex: Int? = null,
        versionId: String? = null, sourceDetailKey: String = media.id) {
        val state = _uiState.value
        if (state.isKidMode || isTelevision() || !media.mediaType.equals("Movie", true) &&
            !media.mediaType.equals("Episode", true) && !media.mediaType.equals("Video", true)) return
        val profileId = state.activeProfileId
        val detailKey = sourceDetailKey.ifBlank { media.id }
        val remoteId = media.remoteId ?: run {
            publishOfflineDownloadNotice(profileId, detailKey, appString(R.string.offline_prepare_failed))
            return
        }
        val connection = state.connections.firstOrNull { connection ->
            connection.kind == media.source && connection.baseUrl.isNotBlank() && connection.token.isNotBlank()
        } ?: run {
            publishOfflineDownloadNotice(profileId, detailKey, appString(R.string.offline_prepare_failed))
            return
        }
        val pendingKey = "$profileId|${connection.identity}|$remoteId"
        if (!preparingOfflineIds.add(pendingKey)) return
        _uiState.update { current ->
            if (current.activeProfileId == profileId) current.copy(
                offlinePreparingDetailKeys = current.offlinePreparingDetailKeys + detailKey,
                offlineDownloadNoticeDetailKey = null,
                offlineDownloadNotice = null,
            ) else current
        }
        operationScope.launch {
            try {
                val prepared = try {
                    // This is deliberately bounded. A title-detail action must never leave a reader
                    // staring at a disabled control when a server or route has stopped responding.
                    withTimeoutOrNull(30_000L) {
                    withContext(ioDispatcher) {
                        app.reelstack.offline.negotiateOfflineDownload(
                            client = app.reelstack.offline.offlineNegotiationClient(appContext),
                            profileId = profileId,
                            connection = connection,
                            remoteId = remoteId,
                            audioIndex = audioIndex,
                            versionId = versionId,
                        )
                    }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    publishOfflineDownloadNotice(profileId, detailKey,
                        error.readableMessage(appContext) ?: appString(R.string.offline_prepare_failed))
                    return@launch
                }
                if (prepared == null) {
                    publishOfflineDownloadNotice(profileId, detailKey, appString(R.string.offline_prepare_failed))
                    return@launch
                }
                // The profile is the access boundary for the encrypted catalogue and the active
                // download worker. Never enqueue a request negotiated before a profile changed.
                if (_uiState.value.activeProfileId != profileId || _uiState.value.isKidMode || isTelevision()) return@launch
                val refusal = try {
                    withContext(ioDispatcher) { offlineDownloads.enqueue(prepared) }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    publishOfflineDownloadNotice(profileId, detailKey,
                        error.readableMessage(appContext) ?: appString(R.string.offline_prepare_failed))
                    return@launch
                }
                if (refusal != null) {
                    publishOfflineDownloadNotice(profileId, detailKey, appString(if (refusal == app.reelstack.offline.OfflineRefusal.STORAGE_LIMIT) R.string.offline_storage_full else R.string.offline_direct_only))
                    return@launch
                }
                _uiState.update { current ->
                    if (current.activeProfileId == profileId) current.copy(
                        snackbar = appString(R.string.offline_queued),
                        offlineDownloadNoticeDetailKey = null,
                        offlineDownloadNotice = null,
                    ) else current
                }
                // A successful tap has a visible destination: leave the detail page only after the
                // request is persisted, then show its queued/progress state in Downloads.
                selectTab(AppTab.DOWNLOADS)
            } finally {
                preparingOfflineIds.remove(pendingKey)
                _uiState.update { current ->
                    if (current.activeProfileId == profileId) current.copy(
                        offlinePreparingDetailKeys = current.offlinePreparingDetailKeys - detailKey,
                    ) else current
                }
            }
        }
    }

    private fun publishOfflineDownloadNotice(profileId: String, detailKey: String, message: String) {
        _uiState.update { current ->
            if (current.activeProfileId == profileId) current.copy(
                offlineDownloadNoticeDetailKey = detailKey,
                offlineDownloadNotice = message,
                snackbar = message,
            ) else current
        }
    }

    fun pauseOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        offlineDownloads.pause(id)
        refreshOfflineDownloads()
    }

    fun resumeOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        offlineDownloads.resume(id)
        refreshOfflineDownloads()
    }

    fun retryOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        if (!offlineDownloads.retry(id)) return
        refreshOfflineDownloads()
    }

    fun removeOfflineDownload(id: String) {
        if (id.isBlank() || _uiState.value.isKidMode) return
        offlineDownloads.remove(_uiState.value.activeProfileId, id)
        refreshOfflineDownloads()
    }

    fun pauseOfflineDownloads() {
        if (_uiState.value.isKidMode) return
        offlineDownloads.pauseAll()
        refreshOfflineDownloads()
    }

    fun resumeOfflineDownloads() {
        if (_uiState.value.isKidMode) return
        offlineDownloads.resumeAll()
        refreshOfflineDownloads()
    }


    fun selectedTabChanged(tab: AppTab) {
        if (tab == AppTab.DOWNLOADS) startOfflineRefresh() else offlineRefreshJob?.cancel()
        if (tab == AppTab.SETTINGS) refreshOfflineDownloads()
    }

    private fun appString(@androidx.annotation.StringRes resId: Int, vararg args: Any): String =
        app.reelstack.localization.AppLanguages.wrap(appContext).getString(resId, *args)
}
