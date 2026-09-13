package app.reelstack.data.network

import org.junit.Assert.*
import org.junit.Test

class CastPortraitTest {
    @Test fun portraitsRequireServerIdAndImageTagAndEncodePath() {
        val payload = """{"People":[{"Type":"Actor","Name":"One","Id":"a /b","PrimaryImageTag":"x&y","Role":"Lead"},{"Type":"Actor","Name":"Two","Id":"two"},{"Type":"Director","Name":"Director","Id":"d","PrimaryImageTag":"tag"}]}"""
        val cast = ServicePayloadParser.libraryDetails(payload, "https://example.com/jellyfin").cast
        assertEquals(2, cast.size)
        assertEquals("https://example.com/jellyfin/Items/a%20%2Fb/Images/Primary?maxWidth=184&quality=85&tag=x%26y", cast[0].portraitUrl)
        assertNull(cast[1].portraitUrl)
        assertNull(ServicePayloadParser.libraryDetails(payload).cast[0].portraitUrl)
    }
}
