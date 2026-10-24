package app.reelstack.ui

import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class SeriesBrowseTargetTest {
    @Test fun explicitSeasonUsesParentSeriesAndItsOwnSeasonId() {
        assertEquals(SeriesBrowseTarget("series", "season"), seriesBrowseTarget(media("Season", "season", "series")))
    }

    @Test fun seasonWithoutParentNeverTreatsItsOwnIdAsASeries() {
        assertNull(seriesBrowseTarget(media("Season", "season", null)))
    }

    @Test fun seriesUsesItsOwnId() {
        assertEquals(SeriesBrowseTarget("series"), seriesBrowseTarget(media("Series", "series", null)))
    }

    @Test fun episodeUsesItsParentSeries() {
        assertEquals(SeriesBrowseTarget("series"), seriesBrowseTarget(media("Episode", "episode", "series")))
    }

    private fun media(type: String, id: String, parent: String?) = LibraryMedia("jellyfin-$id", "Kystvakta", "",
        artworkRes = R.drawable.media_placeholder, source = ServiceKind.JELLYFIN,
        remoteId = id, seriesId = parent, mediaType = type)
}
