package app.reelstack.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MyRequestsTest {
    private fun request(key: String, stage: RequestStage, updated: Long, title: String = key) =
        TrackedRequest(key, mediaId = key.hashCode().and(0xffff), mediaType = "movie", title = title,
            artworkUrl = "https://image.tmdb.org/t/p/w500/$key.jpg", seasons = emptySet(), stage = stage, updatedAt = updated)

    @Test fun inFlightComesFirstThenTheLatestReady() {
        val list = listOf(request("a", RequestStage.AVAILABLE, 10), request("b", RequestStage.DOWNLOADING, 1),
            request("c", RequestStage.AVAILABLE, 30), request("d", RequestStage.REQUESTED, 2))
        assertEquals(listOf("b", "d", "c"), myRequestsPreview(list, 3).map { it.key })
    }

    @Test fun tabsSplitByStage() {
        val list = listOf(request("a", RequestStage.AVAILABLE, 1), request("b", RequestStage.IMPORTING, 2))
        assertEquals(listOf("b"), myRequestsFor(MyRequestsTab.IN_PROGRESS, list).map { it.key })
        assertEquals(listOf("a"), myRequestsFor(MyRequestsTab.READY, list).map { it.key })
        assertEquals(2, myRequestsFor(MyRequestsTab.ALL, list).size)
    }

    @Test fun untitledImportsStayInAllButNotInThePreview() {
        val untitled = TrackedRequest("u", 9, "movie", "film", null, emptySet())
        assertEquals(emptyList<String>(), myRequestsPreview(listOf(untitled), 3).map { it.key })
        assertEquals(listOf("u"), myRequestsFor(MyRequestsTab.ALL, listOf(untitled)).map { it.key })
    }
}
