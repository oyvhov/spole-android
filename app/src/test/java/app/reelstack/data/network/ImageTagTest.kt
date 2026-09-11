package app.reelstack.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Jellyfin's image tag is a content hash, and leaving it out of the address is why replacing a
 * poster or a clear logo on the server never showed up in the app: the URL was identical before
 * and after, so the image cache kept serving the old picture no matter how often the feed was
 * refreshed.
 *
 * These check the tag reaches the address from each of the places Jellyfin puts it — the item's
 * own `ImageTags`, the series it belongs to, and the parent it inherits from — because a tag that
 * belongs to a different item than the address points at is worse than no tag at all.
 */
class ImageTagTest {

    private fun items(payload: String) = ServicePayloadParser.libraryItems(payload)

    @Test
    fun `an item's own primary tag reaches the address`() {
        val item = items("""[{"Id":"film-1","Name":"Arrival","Type":"Movie","ImageTags":{"Primary":"abc123"}}]""").single()
        assertEquals("film-1", item.artworkItemId)
        assertEquals("Primary", item.artworkImageType)
        assertEquals("abc123", item.artworkTag)
    }

    @Test
    fun `an episode with its own thumb carries that thumb's tag`() {
        val item = items(
            """[{"Id":"ep-1","Name":"The Signal","SeriesName":"Foundation","Type":"Episode","ImageTags":{"Thumb":"wide-1"}}]""",
        ).single()
        assertEquals("ep-1", item.artworkItemId)
        assertEquals("Thumb", item.artworkImageType)
        assertEquals("wide-1", item.artworkTag)
    }

    /** The address points at the series, so the tag has to be the series' tag, not the episode's. */
    @Test
    fun `an episode falling back to series art carries the series tag`() {
        val item = items(
            """[{"Id":"ep-2","Name":"Ep","SeriesName":"Foundation","Type":"Episode",
                "SeriesId":"series-9","SeriesThumbImageTag":"series-wide"}]""",
        ).single()
        assertEquals("series-9", item.artworkItemId)
        assertEquals("series-wide", item.artworkTag)
    }

    @Test
    fun `a clear logo carries its own tag`() {
        val item = items("""[{"Id":"film-2","Name":"Dune","Type":"Movie","ImageTags":{"Primary":"p","Logo":"logo-7"}}]""").single()
        assertEquals("film-2", item.logoItemId)
        assertEquals("logo-7", item.logoTag)
    }

    @Test
    fun `a logo inherited from the series carries the series logo tag`() {
        val item = items(
            """[{"Id":"ep-3","Name":"Ep","SeriesName":"Foundation","Type":"Episode",
                "SeriesId":"series-9","SeriesLogoImageTag":"series-logo"}]""",
        ).single()
        assertEquals("series-9", item.logoItemId)
        assertEquals("series-logo", item.logoTag)
    }

    /** Emby and Jellyfin disagree about capitalisation, and both have to work. */
    @Test
    fun `a lower case tag key is read the same`() {
        val item = items("""[{"Id":"film-3","Name":"Heat","Type":"Movie","imageTags":{"primary":"low-1"}}]""").single()
        assertEquals("low-1", item.artworkTag)
    }

    /** No tag is a valid answer; the address simply carries none rather than an empty parameter. */
    @Test
    fun `an item without tags produces no tag`() {
        val item = items("""[{"Id":"film-4","Name":"Tenet","Type":"Movie"}]""").single()
        assertEquals(null, item.artworkTag)
        assertEquals(null, item.logoTag)
    }

    @Test
    fun `an item with no logo anywhere reports none`() {
        val item = items("""[{"Id":"film-5","Name":"Tenet","Type":"Movie","ImageTags":{"Primary":"p"}}]""").single()
        assertEquals(null, item.logoItemId)
        assertEquals(null, item.logoTag)
    }

    /**
     * The whole point, stated as the thing that used to fail: two versions of the same item with
     * different art must not produce the same address.
     */
    @Test
    fun `replacing the artwork changes the tag and therefore the address`() {
        val before = items("""[{"Id":"film-6","Name":"Alien","Type":"Movie","ImageTags":{"Primary":"old"}}]""").single()
        val after = items("""[{"Id":"film-6","Name":"Alien","Type":"Movie","ImageTags":{"Primary":"new"}}]""").single()
        assertEquals(before.artworkItemId, after.artworkItemId)
        assertFalse("Taggen må endre seg", before.artworkTag == after.artworkTag)
        assertTrue(before.artworkTag == "old" && after.artworkTag == "new")
    }
}
