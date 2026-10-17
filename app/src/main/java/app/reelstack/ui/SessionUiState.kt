package app.reelstack.ui

import app.reelstack.data.repository.SessionScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A revocable writer for one session's features. The root keeps a single atomic screen snapshot,
 * so a request can update its card and detail sheet together without competing StateFlow mirrors.
 * Retired features cannot publish, including from finally blocks or non-cooperative callbacks.
 */
internal class SessionUiState(
    private val state: MutableStateFlow<ReelstackUiState>,
    private val owner: SessionScope? = state.value.sessionScope,
) {
    @Volatile private var revoked = false
    val value: ReelstackUiState get() = state.value
    val isCurrent: Boolean get() = !revoked && state.value.sessionScope == owner

    fun update(transform: (ReelstackUiState) -> ReelstackUiState) {
        state.update { current ->
            if (!revoked && current.sessionScope == owner) transform(current) else current
        }
    }

    fun revoke() { revoked = true }
}

/** A read-only projection with a synchronous value; it stores no second copy of screen state. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal fun <T, R> StateFlow<T>.project(transform: (T) -> R): StateFlow<R> =
    object : StateFlow<R> {
        override val value: R get() = transform(this@project.value)
        override val replayCache: List<R> get() = listOf(value)

        @OptIn(InternalCoroutinesApi::class)
        override suspend fun collect(collector: FlowCollector<R>): Nothing {
            this@project.map(transform).distinctUntilChanged().collect(collector)
            error("StateFlow collection completed")
        }
    }
