package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.decodeHomeSections

class AppPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    var personalization: app.reelstack.data.model.Personalization
        get() = app.reelstack.data.model.Personalization(
            accent = app.reelstack.data.model.AccentPalette.decode(preferences.getString("accent_palette", null)),
            artworkSize = app.reelstack.data.model.ArtworkSize.decode(preferences.getString("artwork_size", null)),
            autoResume = preferences.getBoolean("auto_resume", true),
            sidebarExpanded = if (preferences.contains("sidebar_expanded")) preferences.getBoolean("sidebar_expanded", true) else null,
        )
        set(value) = preferences.edit {
            putString("accent_palette", value.accent.name)
            putString("artwork_size", value.artworkSize.name)
            putBoolean("auto_resume", value.autoResume)
            value.sidebarExpanded?.let { putBoolean("sidebar_expanded", it) } ?: remove("sidebar_expanded")
        }

    fun observePersonalization(onChange: (app.reelstack.data.model.Personalization) -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf("accent_palette", "artwork_size", "auto_resume", "sidebar_expanded")) onChange(personalization)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChange(personalization)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var onboardingCompleted: Boolean
        get() = preferences.getBoolean("onboarding_completed", false)
        set(value) = preferences.edit { putBoolean("onboarding_completed", value) }

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = preferences.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    var wifiOnly: Boolean
        get() = preferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = preferences.edit { putBoolean(KEY_WIFI_ONLY, value) }

    var visibleHomeSections: Set<HomeSection>
        get() {
            val saved = preferences.getStringSet(KEY_HOME_SECTIONS, null)
            val decoded = decodeHomeSections(saved)
            // Add the new shared rails once for existing installations. After the first read, the
            // normal setter owns the choice so a user can turn them off again.
            if (saved != null && saved.isNotEmpty() &&
                preferences.getInt(KEY_HOME_SECTIONS_VERSION, 0) < HOME_SECTIONS_VERSION
            ) {
                val migrated = decoded + HomeSection.RECOMMENDATIONS + HomeSection.RECENT_RELEASES +
                    HomeSection.CONTINUE_WATCHING
                preferences.edit {
                    putStringSet(KEY_HOME_SECTIONS, migrated.mapTo(mutableSetOf()) { it.name })
                    putInt(KEY_HOME_SECTIONS_VERSION, HOME_SECTIONS_VERSION)
                }
                return migrated
            }
            return decoded
        }
        set(value) = preferences.edit {
            putStringSet(KEY_HOME_SECTIONS, value.mapTo(mutableSetOf()) { it.name })
            putInt(KEY_HOME_SECTIONS_VERSION, HOME_SECTIONS_VERSION)
        }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_HOME_SECTIONS = "home_sections"
        const val KEY_HOME_SECTIONS_VERSION = "home_sections_version"
        const val HOME_SECTIONS_VERSION = 3
    }
}
