package app.reelstack.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class DemoLanguageTest {
    @Test fun descriptionsMediaLinesProgressAndDatesFollowTheSelectedLanguage() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        data class Example(val language: AppLanguage, val seasons: String, val description: String, val remaining: String, val today: String)
        for ((language, seasons, description, remaining, today) in listOf(
            Example(AppLanguage.NYNORSK, "Serie · 2 sesongar", "Ein soldat", "20 min att", "I dag"),
            Example(AppLanguage.BOKMAL, "Serie · 2 sesonger", "En soldat", "20 min igjen", "I dag"),
            Example(AppLanguage.ENGLISH, "Series · 2 seasons", "A soldier", "20 min left", "Today"),
        )) {
            val context = AppLanguages.wrap(base, language)
            assertEquals(seasons, demoRecentSeries(context).first().subtitle)
            assertTrue(demoRecentMovies(context).first().overview!!.startsWith(description))
            assertTrue(demoResume(context).first().subtitle.endsWith(remaining))
            assertEquals(today, demoUpcoming(context).first().dateLabel)
            assertTrue(demoRecentMovies(context).all { it.remoteId == null && it.artworkUrl == null })
        }
    }
}
