package app.reelstack.localization

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun existingUsersKeepNynorsk() = assertEquals(AppLanguage.NYNORSK, AppLanguage.initial(null, true))
    @Test fun newUsersFollowTheirDevice() = assertEquals(AppLanguage.SYSTEM, AppLanguage.initial(null, false))
    @Test fun explicitEnglishSurvivesMigration() = assertEquals(AppLanguage.ENGLISH, AppLanguage.initial("en", true))
    @Test fun explicitSystemIsNotMistakenForMissingChoice() = assertEquals(AppLanguage.SYSTEM, AppLanguage.initial("", true))
    @Test fun unknownLanguageFallsBackSafely() = assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("broken"))
    @Test fun tagsAreStableAndUnique() = assertEquals(listOf("", "nn", "en"), AppLanguage.entries.map { it.tag })
}
