package app.reelstack.player

import org.junit.Assert.assertEquals
import org.junit.Test

class RemotePlaybackPolicyTest {
    @Test fun hiddenDirectionsRevealOrSeekAndSelectToggles() {
        assertEquals(RemotePlaybackAction.TOGGLE, remotePlaybackAction(RemotePlaybackKey.SELECT, false, true, false))
        assertEquals(RemotePlaybackAction.REVEAL, remotePlaybackAction(RemotePlaybackKey.DOWN, false, true, false))
        assertEquals(RemotePlaybackAction.REWIND, remotePlaybackAction(RemotePlaybackKey.LEFT, false, true, false))
        assertEquals(RemotePlaybackAction.FORWARD, remotePlaybackAction(RemotePlaybackKey.RIGHT, false, true, false))
    }
    @Test fun visibleDirectionsBelongToFocusedControls() {
        listOf(RemotePlaybackKey.SELECT, RemotePlaybackKey.UP, RemotePlaybackKey.DOWN, RemotePlaybackKey.LEFT, RemotePlaybackKey.RIGHT).forEach {
            assertEquals(RemotePlaybackAction.DEFAULT, remotePlaybackAction(it, true, true, false))
        }
    }
    @Test fun explicitPlayAndPauseAreIdempotent() {
        assertEquals(RemotePlaybackAction.IGNORE, remotePlaybackAction(RemotePlaybackKey.PLAY, false, true, false))
        assertEquals(RemotePlaybackAction.IGNORE, remotePlaybackAction(RemotePlaybackKey.PAUSE, true, false, false))
        assertEquals(RemotePlaybackAction.TOGGLE, remotePlaybackAction(RemotePlaybackKey.PLAY, true, false, false))
        assertEquals(RemotePlaybackAction.TOGGLE, remotePlaybackAction(RemotePlaybackKey.PAUSE, false, true, false))
    }
    @Test fun busyErrorResumePromptAndMenusBlockMediaCommandsButAllowNavigation() {
        listOf(RemotePlaybackKey.TOGGLE, RemotePlaybackKey.PLAY, RemotePlaybackKey.PAUSE,
            RemotePlaybackKey.REWIND, RemotePlaybackKey.FORWARD).forEach {
            assertEquals(RemotePlaybackAction.IGNORE, remotePlaybackAction(it, true, true, true))
        }
        assertEquals(RemotePlaybackAction.DEFAULT, remotePlaybackAction(RemotePlaybackKey.SELECT, true, false, true))
    }
}
