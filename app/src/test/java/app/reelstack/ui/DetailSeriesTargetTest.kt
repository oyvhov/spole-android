package app.reelstack.ui

import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.SeriesBrowse
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailSeriesTargetTest {
    private fun episode(id: String, played: Boolean = false, progress: Float? = null) = LibraryMedia(
        id = id, remoteId = id, title = id, subtitle = id, artworkRes = 0,
        source = ServiceKind.JELLYFIN, mediaType = "Episode", played = played, progress = progress,
    )

    @Test fun `season marks the next available episode rather than the first card`() {
        val first = episode("one", played = true)
        val next = episode("two")
        val browse = SeriesBrowse(episodes = listOf(first, next))

        assertEquals("two", nextEpisodeTarget(browse)?.id)
        assertEquals("two", resumeTarget(browse)?.id)
    }

    @Test fun `a completed season has no false next marker but can still restart from its first episode`() {
        val first = episode("one", played = true)
        val last = episode("two", played = true)
        val browse = SeriesBrowse(episodes = listOf(first, last))

        assertNull(nextEpisodeTarget(browse))
        assertEquals("one", resumeTarget(browse)?.id)
    }
}
