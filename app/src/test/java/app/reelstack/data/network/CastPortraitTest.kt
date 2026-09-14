package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test

class CastPortraitTest {
    @Test fun seasonCardsPreferEpisodeStillWhileHomeKeepsSeriesArtwork() {
        val payload = """{"Items":[{"Id":"episode","Name":"Pilot","Type":"Episode","SeriesId":"series","SeriesThumbImageTag":"show","ImageTags":{"Primary":"still"}}]}"""
        assertEquals("series", ServicePayloadParser.libraryItems(payload).single().artworkItemId)
        val still = ServicePayloadParser.libraryItems(payload, preferEpisodeStill = true).single()
        assertEquals("episode", still.artworkItemId)
        assertEquals("still", still.artworkTag)
    }
    @Test fun detailsRecoverSeriesContextMissingFromCachedHomeCards() {
        val details = ServicePayloadParser.libraryDetails("""{"Type":"Episode","SeriesId":"show","ParentIndexNumber":19,"IndexNumber":9}""")
        assertEquals("show", details.seriesId)
        assertEquals(19, details.season)
        assertEquals(9, details.episode)
    }
    @Test fun backdropUsesSeriesArtAndContentTagWithoutCredentials() {
        val details = ServicePayloadParser.libraryDetails("""{"ParentBackdropItemId":"series /1","ParentBackdropImageTags":["tag&1"]}""", "https://media.example")
        assertEquals("https://media.example/Items/series%20%2F1/Images/Backdrop/0?maxWidth=1280&quality=80&tag=tag%261", details.backdropUrl)
        assertNull(ServicePayloadParser.libraryDetails("{}").backdropUrl)
    }
    @Test fun portraitsRequireServerIdAndImageTagAndEncodePath() {
        val payload = """{"People":[{"Type":"Actor","Name":"One","Id":"a /b","PrimaryImageTag":"x&y","Role":"Lead"},{"Type":"Actor","Name":"Two","Id":"two"},{"Type":"Director","Name":"Director","Id":"d","PrimaryImageTag":"tag"}]}"""
        val cast = ServicePayloadParser.libraryDetails(payload, "https://example.com/jellyfin").cast
        assertEquals(2, cast.size)
        assertEquals("a /b", cast[0].remoteId)
        assertEquals("https://example.com/jellyfin/Items/a%20%2Fb/Images/Primary?maxWidth=184&quality=85&tag=x%26y", cast[0].portraitUrl)
        assertNull(cast[1].portraitUrl)
        assertNull(ServicePayloadParser.libraryDetails(payload).cast[0].portraitUrl)
    }
}
