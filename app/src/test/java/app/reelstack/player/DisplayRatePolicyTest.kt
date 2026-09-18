package app.reelstack.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class DisplayRatePolicyTest {
    private fun mode(id: Int, hz: Float, width: Int = 3840, height: Int = 2160) =
        PlaybackDisplayMode(id, width, height, hz)

    @Test fun twentyFiveFpsCanUseFiftyHzWithoutRequiringATwentyFiveHzMode() {
        assertEquals(2, matchingDisplayMode(25f, mode(1, 60f), listOf(mode(1, 60f), mode(2, 50f)))?.id)
    }
    @Test fun compatibleCurrentMultipleAvoidsUnnecessaryDisplaySwitch() {
        assertEquals(1, matchingDisplayMode(24f, mode(1, 120f), listOf(mode(2, 24f)))?.id)
    }
    @Test fun fractionalFrameRatesAreNotRoundedToWholeNumbers() {
        assertNull(matchingDisplayMode(23.976f, mode(1, 60f), listOf(mode(2, 24f))))
        assertEquals(3, matchingDisplayMode(23.976f, mode(1, 60f), listOf(mode(2, 24f), mode(3, 23.976f)))?.id)
    }
    @Test fun frameMatchingDoesNotLowerResolutionOrGuessAnUnknownRate() {
        assertNull(matchingDisplayMode(24f, mode(1, 60f), listOf(mode(2, 24f, 1920, 1080))))
        for (fps in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(matchingDisplayMode(fps, mode(1, 60f), listOf(mode(2, 24f))))
        }
    }
    @Test fun originalSourceMetadataSuppliesMissingContainerFrameRate() {
        fun source(fields: String) = Json.parseToJsonElement("""{"MediaStreams":[{"Type":"Video",$fields}]}""").jsonObject
        assertEquals(23.976f, sourceVideoFrameRate(source("\"AverageFrameRate\":23.976")))
        assertEquals(25f, sourceVideoFrameRate(source("\"AverageFrameRate\":0,\"RealFrameRate\":25")))
        assertEquals(0f, sourceVideoFrameRate(source("\"AverageFrameRate\":9999")))
    }
}
