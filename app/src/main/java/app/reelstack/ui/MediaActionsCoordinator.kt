package app.reelstack.ui

import android.content.Context
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns watched, favourite, and continue-watching writes for the current session. Created and retired by ReelstackFeatures. */
internal class MediaActionsCoordinator(
    private val _uiState: SessionUiState,
    private val operationScope: CoroutineScope,
    private val appContext: Context,
    private val localPlaybackStore: app.reelstack.data.repository.LocalPlaybackStore,
    private val mediaSyncRepository: app.reelstack.data.repository.MediaSyncRepository,
    private val invalidateLibraryPages: (ServiceConnection) -> Unit,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) {
    fun hideFromResume(id: String) {
        writeMediaState(id) { connection, media -> mediaSyncRepository.clearResume(connection, media) }
    }

    fun setMediaFavourite(id: String, favourite: Boolean) {
        writeMediaState(id, favourite = favourite, removeFromResume = false) { connection, media ->
            mediaSyncRepository.setFavourite(connection, media, favourite)
        }
    }

    /** Marking something watched also takes it off the resume shelf, the way finishing it would. */
    fun setMediaPlayed(id: String, played: Boolean) {
        writeMediaState(id, played = played, removeFromResume = played) { connection, media ->
            mediaSyncRepository.setPlayed(connection, media, played)
        }
    }

    fun clearMediaActionError() {
        if (_uiState.value.mediaActionError != null) _uiState.update { it.copy(mediaActionError = null) }
    }

    private fun writeMediaState(
        id: String,
        favourite: Boolean? = null,
        played: Boolean? = null,
        removeFromResume: Boolean = true,
        write: (ServiceConnection, LibraryMedia) -> Unit,
    ) {
        val state = _uiState.value
        val media = mediaActionTarget(state, id)
        if (media == null) {
            _uiState.update { it.copy(mediaActionError = appContext.getString(R.string.library_action_failed)) }
            return
        }
        val connection = state.connections.firstOrNull {
            it.kind == media.source && it.baseUrl.isNotBlank() && it.token.isNotBlank()
        }
        if (connection == null || media.remoteId == null) {
            _uiState.update { it.copy(mediaActionError = appContext.getString(R.string.library_action_failed)) }
            return
        }
        _uiState.update { current ->
            current.copy(
                mediaActionError = null,
                contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(updating = true)
                    ?: current.contentDetails,
            )
        }
        operationScope.launch {
            val result = runCatching { withContext(ioDispatcher) { write(connection, media) } }
            if (result.isFailure) {
                _uiState.update { current ->
                    current.copy(
                        mediaActionError = appContext.getString(R.string.library_action_failed),
                        contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(updating = false)
                            ?: current.contentDetails,
                    )
                }
                return@launch
            }
            if (_uiState.value.activeProfileId != state.activeProfileId || _uiState.value.connections.none {
                it.kind == connection.kind && it.baseUrl == connection.baseUrl && it.token == connection.token && it.userId == connection.userId
            }) return@launch
            if (played != null || removeFromResume) localPlaybackStore.forget(connection, media.remoteId)
            invalidateLibraryPages(connection)
            // Flags change everywhere the title appears; only the resume shelves lose the card.
            fun update(list: List<LibraryMedia>) = list.map { entry ->
                if (entry.id != id) entry
                else entry.copy(favourite = favourite ?: entry.favourite, played = played ?: entry.played,
                    progress = if (removeFromResume) 0f else entry.progress,
                    unplayedItemCount = if (played == true) 0 else if (played == false) null else entry.unplayedItemCount)
            }
            _uiState.update { current ->
                val drop: (List<LibraryMedia>) -> List<LibraryMedia> =
                    if (removeFromResume) { list -> list.filterNot { it.id == id } } else ::update
                current.copy(
                    resume = drop(current.resume),
                    nextUp = update(current.nextUp),
                    recentMovies = update(current.recentMovies),
                    recentSeries = update(current.recentSeries),
                    favourites = update(current.favourites).filter { it.favourite },
                    librarySearchResults = update(current.librarySearchResults),
                    libraryDetailMedia = current.libraryDetailMedia?.let { update(listOf(it)).single() },
                    libraryPeeks = current.libraryPeeks.mapValues { update(it.value) },
                    libraryEntries = if (current.librarySource != media.source) current.libraryEntries else current.libraryEntries.map { entry ->
                        if (entry.id != media.remoteId) entry else entry.copy(favourite = favourite ?: entry.favourite,
                            played = played ?: entry.played, progress = if (removeFromResume) 0f else entry.progress,
                            unplayedItemCount = if (played == true) 0 else if (played == false) null else entry.unplayedItemCount)
                    },
                    globalSearch = current.globalSearch.copy(libraryResults = update(current.globalSearch.libraryResults)),
                    libraryShelves = current.libraryShelves.copy(
                        resume = drop(current.libraryShelves.resume),
                        nextUp = update(current.libraryShelves.nextUp),
                    ),
                    contentDetails = current.contentDetails?.takeIf { it.key == id }?.copy(
                        favourite = favourite ?: current.contentDetails.favourite,
                        played = played ?: current.contentDetails.played,
                        updating = false,
                    ) ?: current.contentDetails,
                )
            }
        }
    }
}
