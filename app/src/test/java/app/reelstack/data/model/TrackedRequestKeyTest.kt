package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackedRequestKeyTest {
    @Test fun aFourKRequestIsItsOwnTrackedRequest() {
        assertEquals("tv:11:1,3", trackedRequestKey("tv", 11, setOf(3, 1), is4k = false))
        assertEquals("movie:11::4k", trackedRequestKey("movie", 11, emptySet(), is4k = true))
    }
}
