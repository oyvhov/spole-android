package app.reelstack

import app.reelstack.data.model.trailerLink
import app.reelstack.data.network.ServicePayloadParser
import org.junit.Assert.*
import org.junit.Test

class TrailerLinkTest {
    @Test fun onlyCanonicalPublicVideoLinksLeaveTheApp() {
        val expected = "https://www.youtube.com/watch?v=abcdefghijk"
        assertEquals(expected, trailerLink("https://youtu.be/abcdefghijk?t=30&token=private"))
        assertEquals(expected, trailerLink("https://www.youtube.com/embed/abcdefghijk"))
        listOf("javascript:alert(1)", "file:///tmp/trailer.mp4", "https://youtube.com.evil.test/watch?v=abcdefghijk",
            "https://secret@youtube.com/watch?v=abcdefghijk", "https://server.test/video?api_key=secret",
            "https://youtube.com:8443/watch?v=abcdefghijk", "https://youtu.be/short").forEach { assertNull(it, trailerLink(it)) }
    }
    @Test fun libraryDetailsSkipUnsupportedTrailerAndKeepPlayableOne() {
        val details = ServicePayloadParser.libraryDetails("""{"Name":"Film","RemoteTrailers":[{"Url":"file:///secret"},{"Url":"https://youtu.be/abcdefghijk"}]}""")
        assertEquals("https://www.youtube.com/watch?v=abcdefghijk", details.trailerUrl)
        assertNull(ServicePayloadParser.libraryDetails("""{"Name":"Film"}""").trailerUrl)
    }
}
