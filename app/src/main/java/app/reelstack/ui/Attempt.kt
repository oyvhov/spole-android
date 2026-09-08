package app.reelstack.ui

import kotlinx.coroutines.CancellationException

/**
 * `runCatching`, minus the one case it gets wrong inside a coroutine.
 *
 * `runCatching` catches `Throwable`, and cancellation *is* a `Throwable`. So closing a sheet in
 * the middle of a call, switching tabs, or `refreshJob?.cancel()` all landed in the failure
 * branch — and the user was shown an error for the very thing they had just chosen to abandon.
 * Worse, the cancelled coroutine went on to update state it no longer owned.
 *
 * Every suspending call in the ViewModel goes through here so the distinction cannot be forgotten
 * one call site at a time. A `runCatching` around purely synchronous work is still fine; it has no
 * cancellation to mistake.
 */
internal suspend inline fun <T> attempt(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
