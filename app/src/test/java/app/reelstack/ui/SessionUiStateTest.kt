package app.reelstack.ui

import app.reelstack.data.repository.SessionScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionUiStateTest {
    private val adult = SessionScope("", "adult", 1)

    @Test fun `retired writer cannot publish even when the account returns`() {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val old = SessionUiState(snapshot)
        old.revoke()
        snapshot.value = ReelstackUiState(sessionScope = adult.copy(generation = 3))
        old.update { it.copy(snackbar = "stale") }
        assertNull(snapshot.value.snackbar)
        SessionUiState(snapshot).update { it.copy(snackbar = "current") }
        assertEquals("current", snapshot.value.snackbar)
    }

    @Test fun `owner change rejects a writer before explicit retirement`() {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val old = SessionUiState(snapshot)
        snapshot.value = ReelstackUiState(sessionScope = adult.copy(profileId = "child", generation = 2))
        old.update { it.copy(isKidMode = false, snackbar = "adult response") }
        assertFalse(old.isCurrent)
        assertNull(snapshot.value.snackbar)
    }

    @Test fun `revocation also protects a non cooperative finally block`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val writer = SessionUiState(snapshot)
        val job = launch {
            try { kotlinx.coroutines.awaitCancellation() }
            finally { withContext(NonCancellable) { writer.update { it.copy(snackbar = "old cleanup") } } }
        }
        runCurrent()
        writer.revoke()
        job.cancelAndJoin()
        assertNull(snapshot.value.snackbar)
    }

    @Test fun `retiring one root does not revoke another root`() {
        val first = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val second = MutableStateFlow(first.value)
        SessionUiState(first).revoke()
        SessionUiState(second).update { it.copy(snackbar = "second") }
        assertEquals("second", second.value.snackbar)
    }

    @Test fun `projection reads synchronously without a collector`() {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val owner = snapshot.project { it.sessionScope }
        snapshot.value = snapshot.value.copy(sessionScope = adult.copy(generation = 2))
        assertEquals(snapshot.value.sessionScope, owner.value)
        assertEquals(listOf(owner.value), owner.replayCache)
    }

    @Test fun `projection suppresses unrelated changes and always starts with the latest value`() = runTest {
        val snapshot = MutableStateFlow(ReelstackUiState(sessionScope = adult))
        val projected = snapshot.project { it.selectedTab }
        val seen = mutableListOf<AppTab>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projected.collect { seen += it } }
        snapshot.value = snapshot.value.copy(snackbar = "unrelated")
        snapshot.value = snapshot.value.copy(selectedTab = AppTab.LIBRARY)
        assertEquals(listOf(AppTab.HOME, AppTab.LIBRARY), seen)
        job.cancel()
        snapshot.value = snapshot.value.copy(selectedTab = AppTab.SETTINGS)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { projected.collect { seen += it } }
        assertEquals(AppTab.SETTINGS, seen.last())
    }
}
