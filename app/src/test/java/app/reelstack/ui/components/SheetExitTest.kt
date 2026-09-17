package app.reelstack.ui.components

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sheet has to be leavable. That is the whole of it.
 *
 * On 16 September 2026 Spole was found on the TV home screen behind a dialog window that was
 * focused, drew nothing, and ignored Back; the only way out was force-stopping the app. The cause
 * was not layout and not focus order — it was that the dismissal was the statement *after* an
 * animation, so an animation that never finished never dismissed, while the `closing` latch
 * swallowed every later press.
 *
 * That is a coroutine-cancellation bug, and a virtual clock is the right instrument for it: these
 * run on the JVM in milliseconds, with no emulator and no frame clock to go missing. The device
 * tests in `SheetEscapeTest` cover the same promise through a real window; these cover the rule
 * itself, including the two endings a device test can only produce by accident.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SheetExitTest {

    // --- which gestures may take the sheet away ---

    @Test fun backLeavesEvenWhileTheSheetIsCommittingSomething() {
        // Back is unguarded. A Seerr request that never returns must not strand the reader in a
        // full-screen modal; the request finishes on its own either way.
        assertTrue(sheetShouldLeave(guarded = false, canDismiss = false, closing = false))
        assertTrue(sheetShouldLeave(guarded = false, canDismiss = true, closing = false))
    }

    @Test fun anOutsideTapRespectsASheetThatIsCommittingSomething() {
        assertFalse(sheetShouldLeave(guarded = true, canDismiss = false, closing = false))
        assertTrue(sheetShouldLeave(guarded = true, canDismiss = true, closing = false))
    }

    @Test fun aSecondRequestDoesNotStartASecondExit() {
        // The latch is deliberate — two exit animations over each other would be a mess. It is also
        // exactly what made the freeze permanent, which is what [runSheetExit] below answers for.
        for (guarded in listOf(true, false)) {
            for (canDismiss in listOf(true, false)) {
                assertFalse(
                    "closing latches for guarded=$guarded canDismiss=$canDismiss",
                    sheetShouldLeave(guarded, canDismiss, closing = true),
                )
            }
        }
    }

    // --- and the promise that the latch is always cleared ---

    @Test fun anExitThatFinishesDismisses() = runTest {
        var dismissed = 0
        runSheetExit(dismiss = { dismissed++ }) { delay(200) }

        assertEquals(1, dismissed)
    }

    @Test fun anExitThatIsCancelledStillDismisses() = runTest {
        // The original bug, reproduced. The scope goes away mid-animation — a composition leaving,
        // a host that stopped — and the dismissal has to happen anyway.
        var dismissed = 0
        val started = CompletableDeferred<Unit>()
        val job = launch {
            runSheetExit(dismiss = { dismissed++ }) {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        assertEquals("nothing has been dismissed yet", 0, dismissed)

        job.cancel()
        advanceUntilIdle()

        assertEquals("a cancelled exit must still dismiss", 1, dismissed)
    }

    @Test fun anExitThatThrowsStillDismisses() = runTest {
        var dismissed = 0
        val failure = runCatching {
            runSheetExit(dismiss = { dismissed++ }) { error("frame clock went away") }
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(1, dismissed)
    }

    @Test fun anExitThatHangsIsDismissedAtTheBound() = runTest {
        // An animation waiting on a frame clock that is not running is the shape of the original
        // failure: it does not throw, it simply never returns.
        var dismissed = 0
        val job = launch { runSheetExit(timeoutMs = 600, dismiss = { dismissed++ }) { awaitCancellation() } }

        advanceTimeBy(599)
        assertEquals("still inside the bound", 0, dismissed)

        advanceTimeBy(2)
        assertEquals("past the bound the sheet leaves regardless", 1, dismissed)
        job.join()
    }

    @Test fun theBoundIsNotPaidWhenTheExitIsQuick() = runTest {
        // A timeout that always cost its full duration would make every close feel slow.
        var dismissed = 0
        val job = launch { runSheetExit(timeoutMs = 600, dismiss = { dismissed++ }) { delay(200) } }

        advanceTimeBy(201)

        assertEquals(1, dismissed)
        job.join()
    }

    @Test fun theSheetIsNeverDismissedTwiceForOneRequest() = runTest {
        var dismissed = 0
        runSheetExit(timeoutMs = 600, dismiss = { dismissed++ }) { delay(100) }
        advanceUntilIdle()

        assertEquals(1, dismissed)
    }

    @Test fun cancellationIsStillReportedUpwards() = runTest {
        // `finally` must not swallow the cancellation itself — a scope that was cancelled has to
        // stay cancelled, or the caller would carry on inside a composition that is gone.
        var dismissed = 0
        val started = CompletableDeferred<Unit>()
        val job = launch {
            try {
                runSheetExit(dismiss = { dismissed++ }) {
                    started.complete(Unit)
                    awaitCancellation()
                }
            } catch (cancelled: CancellationException) {
                dismissed += 10
                throw cancelled
            }
        }
        // Cancel while the exit is still running. Letting the clock idle first would trip the
        // timeout instead, which is the other ending and already has its own test.
        started.await()
        job.cancel()
        job.join()

        assertEquals("dismissed once, and the cancellation was seen", 11, dismissed)
        assertTrue(job.isCancelled)
    }
}
