package app.reelstack.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class DisplayRatePolicyTest {
    @Test fun originalSourceMetadataSuppliesMissingContainerFrameRate() {
        fun source(fields: String) = Json.parseToJsonElement("""{"MediaStreams":[{"Type":"Video",$fields}]}""").jsonObject
        assertEquals(23.976f, sourceVideoFrameRate(source("\"AverageFrameRate\":23.976")))
        assertEquals(25f, sourceVideoFrameRate(source("\"AverageFrameRate\":0,\"RealFrameRate\":25")))
        assertEquals(0f, sourceVideoFrameRate(source("\"AverageFrameRate\":9999")))
    }
}
