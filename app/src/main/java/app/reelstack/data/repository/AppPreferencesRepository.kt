package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.HomeSection

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
            val saved = preferences.getStringSet(KEY_HOME_SECTIONS, null) ?: return HomeSection.entries.toSet()
            return buildSet {
                saved.mapNotNullTo(this) { name ->
                    HomeSection.entries.firstOrNull { it.name == name }
                }
                if ("RECENTLY_ADDED" in saved) {
                    add(HomeSection.RECENT_MOVIES)
                    add(HomeSection.RECENT_SERIES)
                }
            }
        }
        set(value) = preferences.edit { putStringSet(KEY_HOME_SECTIONS, value.mapTo(mutableSetOf()) { it.name }) }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_HOME_SECTIONS = "home_sections"
    }
}
