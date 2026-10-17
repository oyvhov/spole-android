package app.reelstack.player

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.repository.LocalPlaybackStore
import app.reelstack.data.repository.PlaybackProgressOutbox
import app.reelstack.offline.OfflinePlaybackSource
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class OfflinePlaybackProgressTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val account = ServiceConnection(ServiceKind.JELLYFIN, "Fixture", "https://example.test", "fixture", "viewer")
    private val scope get() = PlaybackProgressOutbox.scope("", account)
    private val baseline = PlaybackUserState(120_000_000, false, 0)
    private fun source() = OfflinePlaybackSource("download", "Film", "", "Movie", ServiceKind.JELLYFIN,
        "service-hash", Uri.parse("https://example.test/video"), "cache", "film", baseline, scope)
    private fun outbox() = PlaybackProgressOutbox(context)
    private fun journal() = LocalPlaybackStore(context)
    private fun recorder(profile: String = "") = OfflinePlaybackProgress(profile, journal(), outbox())
    @Before fun reset() { outbox().clear(); journal().clear() }

    @Test fun repeatedOfflineSessionsKeepTheOriginalServerBaselineAndResumeAfterRecreation() {
        recorder().record(account, source(), 60_000, 240_000, false)
        recorder().record(account, source(), 90_000, 240_000, false)
        val entry = outbox().pending(scope).single()
        assertEquals(baseline, entry.baseline)
        assertEquals(90_000L, recorder().resumePosition(account, source()))
        assertFalse(entry.completed)
    }
    @Test fun actualEndPersistsCompletionAndRewatchCanReplaceIt() {
        recorder().record(account, source(), 240_000, 240_000, true)
        assertTrue(outbox().pending(scope).single().completed)
        assertEquals(0L, recorder().resumePosition(account, source()))
        recorder().record(account, source(), 35_000, 240_000, false)
        assertFalse(outbox().pending(scope).single().completed)
        assertEquals(35_000L, recorder().resumePosition(account, source()))
    }
    @Test fun oldDownloadsWithoutBaselineRemainLocalAndAreNeverGuessedToBeUnplayed() {
        recorder().record(account, source().copy(progressBaseline = null), 60_000, 240_000, false)
        assertTrue(outbox().pending(scope).isEmpty())
        assertEquals(60_000L, recorder().resumePosition(account, source().copy(progressBaseline = null)))
    }
    @Test fun tokenAndProfileChangesCannotAdoptTheDownloadBaseline() {
        val changed = account.copy(token = "replacement")
        recorder().record(changed, source(), 60_000, 240_000, false)
        assertTrue(outbox().pending(PlaybackProgressOutbox.scope("", changed)).isEmpty())
        recorder("child").record(account, source(), 80_000, 240_000, false)
        assertTrue(outbox().pending(PlaybackProgressOutbox.scope("child", account)).isEmpty())
    }
    @Test fun downloadBaselineProvidesResumeBeforeAnyLocalPlayback() {
        assertEquals(12_000L, recorder().resumePosition(account, source()))
    }
    @Test fun aCheckpointTheServerRefusedStillResumesAfterTheLocalJournalExpires() {
        recorder().record(account, source(), 90_000, 240_000, false)
        outbox().block(outbox().pending(scope).single())
        // The journal keeps a day; the outbox keeps the refused checkpoint for thirty.
        journal().clear()
        assertEquals(90_000L, recorder().resumePosition(account, source()))
    }
}
