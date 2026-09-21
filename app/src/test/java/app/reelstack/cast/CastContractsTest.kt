package app.reelstack.cast

import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.PlayableItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastContractsTest {
    private val connection = ServiceConnection(
        kind = ServiceKind.JELLYFIN,
        name = "Heime",
        baseUrl = "https://media.example/jellyfin",
        alternateUrl = "https://lan.example/jellyfin",
        token = "short-lived-secret",
        userId = "adult",
        state = ConnectionState.CONNECTED,
    )

    @Test fun `load spec has no route token or user`() {
        val spec = restOfSeasonSpec(connection, "profile", PlayableItem("one", "Episode", "Episode"), listOf(
            PlayableItem("two", "Episode 2", "Episode"),
        ))

        val json = spec.receiverPayload().toString()

        assertFalse(json.contains("media.example"))
        assertFalse(json.contains("short-lived-secret"))
        assertFalse(json.contains("adult"))
        assertTrue(json.contains("one"))
        assertTrue(json.contains("two"))
    }

    @Test fun `credentials are redacted in diagnostic text and only in load envelope`() {
        val envelope = CastCredentialEnvelope.from(connection, "cast-device")

        assertFalse(envelope.toString().contains("short-lived-secret"))
        assertTrue(envelope.loadCredentials().contains("short-lived-secret"))
    }

    @Test fun `rest of season does not make a cross series or service queue`() {
        val selected = PlayableItem("one", "Episode 1", "Episode", season = 1, seriesId = "series-a")
        val spec = restOfSeasonSpec(connection, "profile", selected, listOf(
            PlayableItem("two", "Episode 2", "Episode", season = 1, seriesId = "series-a"),
            PlayableItem("other", "Other series", "Episode", season = 1, seriesId = "series-b"),
            PlayableItem("movie", "Film", "Movie"),
        ))

        assertTrue(spec.items.map { it.itemId } == listOf("one", "two"))
        assertTrue(spec.service == ServiceKind.JELLYFIN)
    }
}
