package app.reelstack.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPageLoadingTest {
    @Test fun `catalogue is published while filter service remains slow`() = runTest {
        val events = mutableListOf<String>()
        launch { loadLibraryPage(StandardTestDispatcher(testScheduler),
            readItems = { delay(100); "covers" }, readFacets = { delay(60_000); "genres" },
            onItems = { events += it }, onFacets = { events += it }) }
        advanceTimeBy(101)
        assertEquals(listOf("covers"), events)
        advanceUntilIdle()
        assertEquals(listOf("covers", "genres"), events)
    }

    @Test fun `leaving a library cancels late filter publication`() = runTest {
        val events = mutableListOf<String>()
        val job = launch { loadLibraryPage(StandardTestDispatcher(testScheduler),
            readItems = { delay(100); "covers" }, readFacets = { delay(60_000); "genres" },
            onItems = { events += it }, onFacets = { events += it }) }
        advanceTimeBy(101)
        job.cancel()
        advanceUntilIdle()
        assertEquals(listOf("covers"), events)
    }
}
