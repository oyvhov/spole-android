package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoverHubStateTest {
    @Test fun focusFactsKeepTheHundredMostRecent() {
        var state = DiscoverHubState()
        (1..101).forEach { state = state.withFocusFacts("t$it", listOf("$it min")) }
        assertEquals(100, state.focusFacts.size)
        assertEquals(null, state.focusFacts["t1"])
        // Looking at a title again makes it recent, so it outlives the ones after it.
        state = state.withFocusFacts("t2", listOf("2 min")).withFocusFacts("t102", emptyList())
        assertEquals(listOf("2 min"), state.focusFacts["t2"])
        assertEquals(null, state.focusFacts["t3"])
    }
}
