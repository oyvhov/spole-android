package app.reelstack.ui

import app.reelstack.R
import app.reelstack.data.model.ContentDetails
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaActionTargetTest {

    @Test
    fun `restored episode detail remains a watched action target after series navigation`() {
        val episode = ContentDetails(
            key = "jellyfin-episode-7",
            remoteId = "episode-7",
            title = "Episode 7",
            eyebrow = "Bibliotek i Jellyfin",
            subtitle = "Sesong 1 · Episode 7",
            artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN,
            mediaType = "Episode",
            libraryAvailable = true,
            played = true,
        )
        val series = LibraryMedia(
            id = "jellyfin-series-1",
            title = "Serien",
            subtitle = "",
            artworkRes = R.drawable.media_placeholder,
            source = ServiceKind.JELLYFIN,
            remoteId = "series-1",
            mediaType = "Series",
        )

        val action = mediaActionTarget(ReelstackUiState(contentDetails = episode, libraryDetailMedia = series), episode.key)

        assertNotNull(action)
        assertEquals("episode-7", action?.remoteId)
        assertEquals(ServiceKind.JELLYFIN, action?.source)
        assertEquals(true, action?.played)
    }
}
