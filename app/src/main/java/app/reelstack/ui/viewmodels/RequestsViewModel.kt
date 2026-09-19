package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reelstack.R
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.IncomingMedia
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.TrackedRequest
import app.reelstack.data.model.UpcomingMedia
import app.reelstack.data.network.MediaServerClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RequestsUiState(
    val requestDraft: RequestDraft? = null,
    val requestingMediaIds: Set<String> = emptySet(),
    val trackedRequests: List<TrackedRequest> = emptyList(),
    val incoming: List<IncomingMedia> = emptyList(),
    val upcoming: List<UpcomingMedia> = emptyList(),
    val recentReleases: List<UpcomingMedia> = emptyList(),
)

class RequestsViewModel(
    private val mediaServerClient: MediaServerClient,
    private val appContext: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState: StateFlow<RequestsUiState> = _uiState.asStateFlow()

    private var requestDraftJob: Job? = null

    fun openRequestComposer(media: DiscoverMedia, connection: ServiceConnection?) {
        requestDraftJob?.cancel()
        _uiState.update {
            it.copy(
                requestDraft = RequestDraft(media),
            )
        }
    }

    fun toggleSeason(number: Int, checked: Boolean) {
        _uiState.update { state ->
            val draft = state.requestDraft ?: return@update state
            state.copy(
                requestDraft = draft.copy(
                    selected = if (checked) draft.selected + number else draft.selected - number,
                ),
            )
        }
    }

    fun setNotify(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                requestDraft = state.requestDraft?.takeUnless { it.sending }?.copy(notify = enabled)
                    ?: state.requestDraft,
            )
        }
    }

    fun closeRequestComposer() {
        if (_uiState.value.requestDraft?.sending == true) return
        requestDraftJob?.cancel()
        _uiState.update { it.copy(requestDraft = null) }
    }
}
