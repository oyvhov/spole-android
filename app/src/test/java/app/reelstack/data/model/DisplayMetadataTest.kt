package app.reelstack.data.model

import org.junit.Assert.*
import org.junit.Test

class DisplayMetadataTest {
    private val connection = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "https://media.example", "fixture")
    @Test fun ratingAcceptsBothDecimalFormatsButNeverInventsMissingScores() {
        assertEquals("7,8", communityRatingLabel(listOf("2026", "12", "★ 7,8")))
        assertEquals("8.1", communityRatingLabel(listOf("★ 8.1")))
        assertNull(communityRatingLabel(listOf("2026", "12", "118 min")))
        listOf("★ NaN", "★ Infinity", "★ -1", "★ 11", "★ unknown").forEach {
            assertNull(communityRatingLabel(listOf(it)))
        }
    }
    @Test fun actualServerRatingSurvivesLibraryParsing() {
        val item = app.reelstack.data.network.ServicePayloadParser.libraryItems(
            """{"Items":[{"Id":"movie","Name":"Film","Type":"Movie","CommunityRating":7.8}]}""").single()
        assertEquals(7.8, communityRatingLabel(item.facts)!!.replace(',', '.').toDouble(), 0.01)
    }
    @Test fun storedTokenDoesNotMeanTheServiceIsHealthy() {
        assertEquals(ServiceHealth.UNCHECKED, connection.health())
        assertEquals(ServiceHealth.UNCONFIGURED, connection.copy(token = "").health())
        assertEquals(ServiceHealth.OK, connection.copy(state = ConnectionState.CONNECTED).health())
        assertEquals(ServiceHealth.CHECKING, connection.copy(state = ConnectionState.TESTING).health())
        assertEquals(ServiceHealth.ERROR, connection.copy(state = ConnectionState.ERROR).health())
    }
    @Test fun partialResponseIsAWarningNotAnOkCheckmark() {
        val connected = connection.copy(state = ConnectionState.CONNECTED)
        assertEquals(ServiceHealth.WARNING, connected.health("Could not fetch recent movies"))
        assertEquals(ServiceHealth.WARNING, connected.copy(detail = "Tilkopla · Delvis svar").health())
        assertEquals(ServiceHealth.ERROR, connected.copy(state = ConnectionState.ERROR).health("Delvis svar"))
    }
}
