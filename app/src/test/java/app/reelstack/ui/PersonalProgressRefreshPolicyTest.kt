package app.reelstack.ui

import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class PersonalProgressRefreshPolicyTest {
    private val jellyfin = PlaybackSession("Viewer", "Phone", "Series", "Episode 1", .2f, 20, false, "HD", false,
        sessionId = "session", source = ServiceKind.JELLYFIN, season = 1, episode = 1)
    private val sources = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)

    @Test fun startsStopsPausesAndEpisodeChangesRefreshPersonalShelvesOnBothServers() {
        for (source in sources) {
            val session = jellyfin.copy(source = source)
            assertTrue(personalProgressChanged(emptyList(), listOf(session), sources))
            assertTrue(personalProgressChanged(listOf(session), emptyList(), sources))
            assertTrue(personalProgressChanged(listOf(session), listOf(session.copy(paused = true)), sources))
            assertTrue(personalProgressChanged(listOf(session), listOf(session.copy(episode = 2, subtitle = "Episode 2")), sources))
        }
    }

    @Test fun timeTicksAndAnUnavailableOrUnrequestedServerDoNotReloadTheFeed() {
        assertFalse(personalProgressChanged(listOf(jellyfin), listOf(jellyfin.copy(progress = .3f, remainingMinutes = 10)), sources))
        assertFalse(personalProgressChanged(listOf(jellyfin), emptyList(), setOf(ServiceKind.EMBY)))
        assertFalse(personalProgressChanged(listOf(jellyfin), emptyList(), emptySet()))
    }
}
