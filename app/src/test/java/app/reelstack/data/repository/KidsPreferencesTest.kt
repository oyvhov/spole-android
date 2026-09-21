package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class KidsPreferencesTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("spole_kids_preferences", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun choicesSurviveReopeningAndDoNotAffectSibling() {
        KidsPreferencesRepository(context).save("one", KidsPreferences(world = KidsWorld.OCEAN, autoplay = true, episodeLimit = 2))
        val reopened = KidsPreferencesRepository(context)
        assertEquals(KidsWorld.OCEAN, reopened.read("one").world)
        assertEquals(2, reopened.read("one").episodeLimit)
        assertEquals(KidsPreferences(), reopened.read("two"))
    }

    @Test fun childAppearanceCannotChangeParentalChoicesOrBypassLock() {
        val repo = KidsPreferencesRepository(context)
        val parent = KidsPreferences(autoplay = true, episodeLimit = 2, subtitles = SubtitleLanguage.NONE)
        repo.save("one", parent)
        repo.saveAppearance("one", KidsWorld.FOREST, false)
        assertEquals(parent.copy(world = KidsWorld.FOREST, decorations = false), repo.read("one"))
        repo.save("one", parent.copy(allowAppearance = false))
        repo.saveAppearance("one", KidsWorld.AURORA, false)
        assertEquals(parent.copy(allowAppearance = false), repo.read("one"))
    }

    @Test fun firstEpisodeCountsTowardAutoplayLimit() {
        assertFalse(KidsPreferences().canAutoplay(0))
        assertFalse(KidsPreferences(autoplay = true, episodeLimit = 1).canAutoplay(0))
        val three = KidsPreferences(autoplay = true, episodeLimit = 3)
        assertTrue(three.canAutoplay(0))
        assertTrue(three.canAutoplay(1))
        assertFalse(three.canAutoplay(2))
    }

    @Test fun playbackUsesChildSettingsWithoutChangingAdult() {
        val adult = Personalization(autoResume = false, autoPlayNextEpisode = true, preferredSubtitleLanguage = SubtitleLanguage.ENGLISH)
        val child = KidsPreferences(subtitles = SubtitleLanguage.NONE).playbackOptions(adult)
        assertTrue(child.autoResume)
        assertFalse(child.autoPlayNextEpisode)
        assertEquals(SubtitleLanguage.NONE, child.preferredSubtitleLanguage)
        assertFalse(adult.autoResume)
        assertTrue(adult.autoPlayNextEpisode)
        assertEquals(SubtitleLanguage.ENGLISH, adult.preferredSubtitleLanguage)
    }

    @Test fun unknownThemeAndInvalidLimitHaveSafeDefaults() {
        assertEquals(KidsWorld.SPACE, KidsWorld.decode("removed-theme"))
        val repo = KidsPreferencesRepository(context)
        repo.save("one", KidsPreferences(episodeLimit = 99))
        assertEquals(3, repo.read("one").episodeLimit)
    }

    @Test fun libraryTitlesDefaultToReadableMobileLayoutAndCanBeToggled() {
        val prefs = context.getSharedPreferences("spole_kids_preferences", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("one.library_titles_below", false).commit()
        val repo = KidsPreferencesRepository(context)
        assertTrue(repo.read("one").libraryTitlesBelow)

        repo.save("one", KidsPreferences(libraryTitlesBelow = false))
        assertFalse(repo.read("one").libraryTitlesBelow)
        repo.save("one", KidsPreferences(libraryTitlesBelow = true))
        assertTrue(repo.read("one").libraryTitlesBelow)
    }

    @Test fun bedtimeIsPerProfileAndUsesTheDeviceLocalClock() {
        val repo = KidsPreferencesRepository(context)
        val bedtime = KidsBedtime(enabled = true, hour = 19, minute = 30)
        repo.save("one", KidsPreferences(bedtime = bedtime))

        assertEquals(bedtime, repo.read("one").bedtime)
        assertFalse(repo.read("two").bedtime.enabled)
        assertFalse(bedtime.isReached(LocalTime.of(19, 29)))
        assertTrue(bedtime.isReached(LocalTime.of(19, 30)))
        assertTrue(bedtime.isReached(LocalTime.of(23, 59)))
        assertFalse(bedtime.copy(enabled = false).isReached(LocalTime.of(23, 59)))
    }

    @Test fun bedtimeClockCyclesInHalfHourStepsAndStorageBoundsCorruptValues() {
        val bedtime = KidsBedtime(enabled = true, hour = 23, minute = 30)
        assertEquals(KidsBedtime(enabled = true, hour = 0, minute = 0), bedtime.nextHalfHour())

        val prefs = context.getSharedPreferences("spole_kids_preferences", Context.MODE_PRIVATE)
        prefs.edit().putInt("one.bedtime_hour", 99).putInt("one.bedtime_minute", -4).commit()
        assertEquals(KidsBedtime(hour = 23, minute = 0), KidsPreferencesRepository(context).read("one").bedtime)
    }
}
