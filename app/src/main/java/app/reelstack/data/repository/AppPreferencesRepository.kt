package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.decodeHomeSections

class AppPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

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
