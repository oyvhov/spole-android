package app.reelstack.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
import app.reelstack.player.PlaybackSegment.Kind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Each kind of stretch says what it skips, in the reader's own language.
 *
 * One label for every kind would be the quiet way to get this wrong: "Skip intro" over a recap is
 * not a mistake anyone would find in a code review, only on a sofa.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = app.reelstack.SheetTestApplication::class)
class SegmentButtonLabelTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()

    private fun labels(language: AppLanguage): List<String> {
        val context = AppLanguages.wrap(base, language)
        return listOf(
            SegmentAction.Skip(Kind.INTRO, 1), SegmentAction.Skip(Kind.OUTRO, 1),
            SegmentAction.Skip(Kind.RECAP, 1), SegmentAction.Skip(Kind.PREVIEW, 1),
            SegmentAction.NextEpisode,
        ).map { context.getString(it.labelRes()) }
    }

    @Test fun nynorskNamesWhatIsSkipped() {
        assertEquals(
            listOf("Hopp over introen", "Hopp over rulleteksten", "Hopp over resymé", "Hopp over førehandsvising", "Neste episode"),
            labels(AppLanguage.NYNORSK),
        )
    }

    @Test fun bokmalNamesWhatIsSkipped() {
        assertEquals(
            listOf("Hopp over intro", "Hopp over rulletekst", "Hopp over resymé", "Hopp over forhåndsvisning", "Neste episode"),
            labels(AppLanguage.BOKMAL),
        )
    }

    @Test fun englishNamesWhatIsSkipped() {
        assertEquals(
            listOf("Skip intro", "Skip credits", "Skip recap", "Skip preview", "Next episode"),
            labels(AppLanguage.ENGLISH),
        )
    }

    @Test fun everyKindHasItsOwnLabel() {
        for (language in listOf(AppLanguage.NYNORSK, AppLanguage.BOKMAL, AppLanguage.ENGLISH))
            assertEquals(language.name, 5, labels(language).toSet().size)
    }
}
