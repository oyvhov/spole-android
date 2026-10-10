package app.reelstack.ui

import app.reelstack.data.model.PlaybackSession
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Test

class PlaybackRefreshScheduleTest {
    private val sources = setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY)

    @Test fun jellyfinPushDoesNotDelayEmbyStartsEvenWhenNothingWasPlaying() {
        val schedule = PlaybackRefreshSchedule()
        assertEquals(sources, schedule.due(sources, true, false, 0))
        schedule.refreshed(sources, 0)
        assertEquals(setOf(ServiceKind.EMBY), schedule.due(sources, true, false, 3_000))
        assertEquals(1_000L, schedule.waitMillis(sources, true, false, 2_000))
        assertEquals(sources, schedule.due(sources, true, false, 60_000))
    }

    @Test fun anOpenPopupRefreshesBothServersEveryTwoSeconds() {
        val schedule = PlaybackRefreshSchedule()
        schedule.refreshed(sources, 0)
        assertTrue(schedule.due(sources, true, true, 1_999).isEmpty())
        assertEquals(sources, schedule.due(sources, true, true, 2_000))
    }

    @Test fun lostJellyfinPushRestoresFastPolling() {
        val schedule = PlaybackRefreshSchedule()
        schedule.refreshed(sources, 0)
        assertEquals(sources, schedule.due(sources, false, false, 3_000))
    }

    @Test fun noMediaConnectionHasNoBusyLoop() {
        val schedule = PlaybackRefreshSchedule()
        assertTrue(schedule.due(emptySet(), false, true, 0).isEmpty())
        assertEquals(60_000L, schedule.waitMillis(emptySet(), false, true, 0))
    }

    @Test fun stoppedEmbySessionDisappearsWithoutErasingJellyfin() {
        val jellyfin = session(ServiceKind.JELLYFIN, "j1")
        val emby = session(ServiceKind.EMBY, "e1")
        assertEquals(listOf(jellyfin), mergePlaybackSessions(listOf(jellyfin, emby), emptyList(), setOf(ServiceKind.EMBY)))
    }

    @Test fun changedAndNewSessionsKeepExistingOrderAcrossServers() {
        val first = session(ServiceKind.EMBY, "e1")
        val other = session(ServiceKind.JELLYFIN, "j1")
        val second = session(ServiceKind.EMBY, "e2")
        val paused = first.copy(paused = true, progress = .8f)
        assertEquals(listOf(paused, other, second), mergePlaybackSessions(listOf(first, other),
            listOf(second, paused), setOf(ServiceKind.EMBY)))
    }

    @Test fun sameSessionIdOnDifferentServersCannotReplaceTheOtherSession() {
        val jf = session(ServiceKind.JELLYFIN, "same")
        val emby = session(ServiceKind.EMBY, "same")
        assertEquals(listOf(jf, emby.copy(paused = true)), mergePlaybackSessions(listOf(jf, emby),
            listOf(emby.copy(paused = true)), setOf(ServiceKind.EMBY)))
    }

    private fun session(source: ServiceKind, id: String) = PlaybackSession(
        "Testperson", "Testskjerm", "Ein film", "", .3f, 20, false, "1080p", false,
        sessionId = id, source = source,
    )
}
