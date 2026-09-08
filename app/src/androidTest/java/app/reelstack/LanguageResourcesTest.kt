package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.localization.AppLanguage
import app.reelstack.localization.AppLanguages
import org.junit.Assert.*
import org.junit.Test

class LanguageResourcesTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun explicitEnglishAndNynorskSelectRealAndroidResources() {
        val english = AppLanguages.wrap(context, AppLanguage.ENGLISH)
        val nynorsk = AppLanguages.wrap(context, AppLanguage.NYNORSK)
        assertEquals("Settings", english.getString(R.string.nav_settings))
        assertEquals("Innstillingar", nynorsk.getString(R.string.nav_settings))
        assertEquals("Resume from 1:20", english.getString(R.string.player_resume, "1:20"))
        assertEquals("Hald fram frå 1:20", nynorsk.getString(R.string.player_resume, "1:20"))
    }

    @Test fun playbackPluralFormsAndServiceParametersAreNotConcatenated() {
        val english = AppLanguages.wrap(context, AppLanguage.ENGLISH)
        val nynorsk = AppLanguages.wrap(context, AppLanguage.NYNORSK)
        assertEquals("1 playback", english.resources.getQuantityString(R.plurals.home_playback_count, 1, 1))
        assertEquals("2 avspelingar", nynorsk.resources.getQuantityString(R.plurals.home_playback_count, 2, 2))
        assertEquals("Emby · Films", english.getString(R.string.settings_movies, "Emby"))
    }

    @Test fun wrappingLanguageDoesNotChangeStoredAccountsOrSystemLocale() {
        val locales = android.content.res.Resources.getSystem().configuration.locales.toLanguageTags()
        val selected = AppLanguages.selected(context)
        val preferences = context.getSharedPreferences("reelstack_preferences", android.content.Context.MODE_PRIVATE).all
        AppLanguages.wrap(context, AppLanguage.ENGLISH).getString(R.string.player_pause)
        assertEquals(locales, android.content.res.Resources.getSystem().configuration.locales.toLanguageTags())
        assertEquals(selected, AppLanguages.selected(context))
        assertEquals(preferences, context.getSharedPreferences("reelstack_preferences", android.content.Context.MODE_PRIVATE).all)
    }

    @Test fun legacyConnectionWithoutOtherPreferencesKeepsNynorsk() {
        isolated { fixture ->
            fixture.getSharedPreferences("reelstack_connections", android.content.Context.MODE_PRIVATE)
                .edit().putString("jellyfin.url", "https://fixture.example").commit()
            assertEquals(AppLanguage.NYNORSK, AppLanguages.selected(fixture))
            fixture.getSharedPreferences("reelstack_connections", android.content.Context.MODE_PRIVATE).edit().clear().commit()
            assertEquals(AppLanguage.NYNORSK, AppLanguages.selected(fixture))
        }
    }

    @Test fun brandNewInstallationFollowsSystemWithoutTouchingAccounts() {
        isolated { fixture ->
            assertEquals(AppLanguage.SYSTEM, AppLanguages.selected(fixture))
            assertTrue(fixture.getSharedPreferences("reelstack_connections", android.content.Context.MODE_PRIVATE).all.isEmpty())
        }
    }

    private fun isolated(block: (android.content.Context) -> Unit) {
        val prefix = "locale_fixture_${java.util.UUID.randomUUID()}_"
        val names = mutableSetOf<String>()
        val fixture = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                names += prefix + name
                return super.getSharedPreferences(prefix + name, mode)
            }
        }
        try { block(fixture) } finally { names.forEach { context.deleteSharedPreferences(it) } }
    }
}
