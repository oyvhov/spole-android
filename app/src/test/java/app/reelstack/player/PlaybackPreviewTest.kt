package app.reelstack.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class PlaybackPreviewTest {
    private val grid = TrickplayGrid(320, 180, 10, 10, 10_000, 220, "https://example.test/Videos/film/Trickplay/320", "edition")
    @Test fun spriteBoundariesAndEndClampingSelectTheCorrectFrame() {
        assertEquals(TrickplayFrame("https://example.test/Videos/film/Trickplay/320/0.jpg?mediaSourceId=edition", 9, 9), grid.frame(999_999))
        assertEquals(0, grid.frame(1_000_000).column)
        assertEquals(0, grid.frame(1_000_000).row)
        assertTrue(grid.frame(1_000_000).url.contains("/1.jpg"))
        assertEquals(9, grid.frame(Long.MAX_VALUE).column)
        assertEquals(1, grid.frame(Long.MAX_VALUE).row)
        assertEquals(0, grid.frame(-1).column)
    }
    @Test fun absentAndMalformedServerMetadataCannotCreateRequests() {
        assertTrue(parseTrickplay(Json.parseToJsonElement("""{"Id":"film"}""").jsonObject, "https://example.test").isEmpty())
        val value = Json.parseToJsonElement("""{"Id":"film","Trickplay":{"edition":{"320":{"Width":320,"Height":180,"TileWidth":10,"TileHeight":10,"Interval":0,"ThumbnailCount":220}}}}""").jsonObject
        assertTrue(parseTrickplay(value, "https://example.test").isEmpty())
    }
    @Test fun nativeAssPacketsKeepAbsoluteTimesAndCommaRichText() {
        val result = assMatroskaDialogue("Dialogue: 0:00:00:00,0:00:02:50,0,Default,,0,0,0,,Text, with comma".toByteArray(), 8_000)!!.toString(Charsets.UTF_8)
        assertEquals("Dialogue: 0:00:08.00,0:00:10.50,0,Default,,0,0,0,,Text, with comma\n", result)
        assertNull(assTimeMs("99999:00:00.00"))
        assertNull(assMatroskaDialogue("invalid".toByteArray(), 0))
    }
    @Test fun largeSpritesHaveABoundedDecodedPixelBudget() {
        val size = previewDecodeSize(grid.copy(columns = 20, rows = 20, width = 40, height = 1024))
        assertTrue(size.width * size.height <= 1_500_000)
        assertTrue(size.width <= 4096 && size.height <= 4096)
        assertEquals(androidx.compose.ui.unit.IntSize(1600, 900), previewDecodeSize(grid))
    }
    @Test fun documentTimingHandlesUnicodeAndDeclaredFieldOrderWithoutJniStrings() {
        assertEquals(2500L, assDocumentEndMs("[Events]\nFormat: Layer, Start, End, Text\nDialogue: 0,0:00:00.00,0:00:02.50,Norsk 🎬, tekst".toByteArray()))
        assertEquals(3100L, assDocumentEndMs("[Events]\nFormat: End, Start, Text\nDialogue: 0:00:03.10,0:00:00.00,Test".toByteArray()))
        assertEquals(0L, assDocumentEndMs("invalid".toByteArray()))
    }
    @Test fun jellyfinManifestIsExplicitlyRequestedOnlyForThePlayingItem() {
        var requests = 0
        val transport = object : app.reelstack.data.network.JsonHttpTransport {
            override fun get(url: String, headers: Map<String, String>): app.reelstack.data.network.HttpResponse {
                requests++
                assertEquals("https://example.test/Users/user/Items/film?Fields=Trickplay", url)
                return app.reelstack.data.network.HttpResponse(200, """{"Id":"film","Trickplay":{"edition":{"320":{"Width":320,"Height":180,"TileWidth":10,"TileHeight":10,"Interval":10000,"ThumbnailCount":220}}}}""")
            }
            override fun post(url: String, headers: Map<String, String>, jsonBody: String) = error("A preview never writes")
        }
        val connection = app.reelstack.data.model.ServiceConnection(app.reelstack.data.model.ServiceKind.JELLYFIN, "Fixture", "https://example.test", "test-token", "user")
        val client = MediaPlaybackClient(transport, "test-device")
        assertEquals("edition", client.trickplay(connection, "user", "film").single().mediaSourceId)
        assertTrue(client.trickplay(connection.copy(kind = app.reelstack.data.model.ServiceKind.EMBY), "user", "film").isEmpty())
        assertEquals(1, requests)
    }
}
