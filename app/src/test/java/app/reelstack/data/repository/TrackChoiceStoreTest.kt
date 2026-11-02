package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.PlayableItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Audio and subtitles, remembered per series.
 *
 * Jellyfin remembers a choice per episode, which is the wrong unit: someone who switches the
 * subtitles off in the third episode means the whole series. The memory is local, belongs to one
 * profile, and holds only a description of the track, never an address or a credential.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class TrackChoiceStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var clock = 1_000L
    private val store = TrackChoiceStore(context) { clock++ }

    private val japanese = RememberedTrack("jpn", "Original", false, false, "eac3", 6, 1)
    private val englishSdh = RememberedTrack("eng", null, false, true, "subrip", 0, 3)

    @Test fun nothingRememberedReadsAsNothing() {
        assertEquals(TrackChoices(), store.read("JELLYFIN||show"))
    }

    @Test fun anAudioChoiceComesBackWithEveryField() {
        store.rememberAudio("JELLYFIN||show", japanese)
        assertEquals(TrackChoices(audio = japanese), store.read("JELLYFIN||show"))
    }

    @Test fun aSubtitleChoiceComesBackAndSoDoesOff() {
        store.rememberSubtitle("JELLYFIN||show", RememberedSubtitle.Track(englishSdh))
        assertEquals(RememberedSubtitle.Track(englishSdh), store.read("JELLYFIN||show").subtitle)
        store.rememberSubtitle("JELLYFIN||show", RememberedSubtitle.Off)
        assertEquals(RememberedSubtitle.Off, store.read("JELLYFIN||show").subtitle)
    }

    @Test fun audioAndSubtitlesAreRememberedSeparately() {
        store.rememberAudio("JELLYFIN||show", japanese)
        store.rememberSubtitle("JELLYFIN||show", RememberedSubtitle.Off)
        assertEquals(TrackChoices(japanese, RememberedSubtitle.Off), store.read("JELLYFIN||show"))
        // Changing one leaves the other alone.
        store.rememberAudio("JELLYFIN||show", englishSdh)
        assertEquals(TrackChoices(englishSdh, RememberedSubtitle.Off), store.read("JELLYFIN||show"))
        store.rememberSubtitle("JELLYFIN||show", RememberedSubtitle.Track(englishSdh))
        assertEquals(TrackChoices(englishSdh, RememberedSubtitle.Track(englishSdh)), store.read("JELLYFIN||show"))
    }

    @Test fun theMemoryOutlivesTheStoreObject() {
        store.rememberAudio("JELLYFIN||show", japanese)
        assertEquals(japanese, TrackChoiceStore(context).read("JELLYFIN||show").audio)
    }

    @Test fun aDamagedEntryReadsAsNothingInsteadOfBreakingPlayback() {
        context.getSharedPreferences(TrackChoiceStore.PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString("JELLYFIN||show", "{not json").putString("JELLYFIN||other", "[1,2]")
            .putString("JELLYFIN||half", """{"a":{"forced":"yes"},"s":42}""").commit()
        assertEquals(TrackChoices(), store.read("JELLYFIN||show"))
        assertEquals(TrackChoices(), store.read("JELLYFIN||other"))
        assertEquals(TrackChoices(), store.read("JELLYFIN||half"))
    }

    // --- the key ---------------------------------------------------------------------------

    private val episode = PlayableItem("ep1", "Serie", "Episode", seriesId = "show")

    @Test fun everyEpisodeOfASeriesSharesOneKey() {
        val key = TrackChoiceStore.key(ServiceKind.JELLYFIN, "", episode)
        assertEquals(key, TrackChoiceStore.key(ServiceKind.JELLYFIN, "", episode.copy(id = "ep2", season = 2, episode = 7)))
    }

    @Test fun aFilmIsKeyedByItself() {
        val film = PlayableItem("film", "Film", "Movie")
        assertNotEquals(TrackChoiceStore.key(ServiceKind.JELLYFIN, "", film),
            TrackChoiceStore.key(ServiceKind.JELLYFIN, "", PlayableItem("other-film", "Film", "Movie")))
        assertNotEquals(TrackChoiceStore.key(ServiceKind.JELLYFIN, "", film), TrackChoiceStore.key(ServiceKind.JELLYFIN, "", episode))
    }

    @Test fun eachProfileAndEachServerKeepsItsOwnChoice() {
        val main = TrackChoiceStore.key(ServiceKind.JELLYFIN, "", episode)
        val child = TrackChoiceStore.key(ServiceKind.JELLYFIN, "kid.abc", episode)
        val otherChild = TrackChoiceStore.key(ServiceKind.JELLYFIN, "kid.def", episode)
        val emby = TrackChoiceStore.key(ServiceKind.EMBY, "", episode)
        assertEquals(4, setOf(main, child, otherChild, emby).size)
        store.rememberSubtitle(child, RememberedSubtitle.Off)
        store.rememberAudio(main, japanese)
        assertNull(store.read(main).subtitle)
        assertNull(store.read(child).audio)
        assertEquals(TrackChoices(), store.read(emby))
        assertEquals(TrackChoices(), store.read(otherChild))
    }

    // --- bounded ---------------------------------------------------------------------------------

    @Test fun theOldestSeriesIsForgottenWhenTheMemoryIsFull() {
        for (n in 0 until TrackChoiceStore.MAX_ENTRIES + 5) store.rememberAudio("JELLYFIN||show$n", japanese)
        val remembered = (0 until TrackChoiceStore.MAX_ENTRIES + 5).count { store.read("JELLYFIN||show$it").audio != null }
        assertEquals(TrackChoiceStore.MAX_ENTRIES, remembered)
        for (n in 0 until 5) assertNull("show$n is the oldest", store.read("JELLYFIN||show$n").audio)
        assertEquals(japanese, store.read("JELLYFIN||show${TrackChoiceStore.MAX_ENTRIES + 4}").audio)
    }

    @Test fun usingASeriesAgainKeepsItFromBeingTheOldest() {
        for (n in 0 until TrackChoiceStore.MAX_ENTRIES) store.rememberAudio("JELLYFIN||show$n", japanese)
        store.rememberSubtitle("JELLYFIN||show0", RememberedSubtitle.Off)
        store.rememberAudio("JELLYFIN||new", japanese)
        assertEquals(RememberedSubtitle.Off, store.read("JELLYFIN||show0").subtitle)
        assertNull(store.read("JELLYFIN||show1").audio)
    }
}
