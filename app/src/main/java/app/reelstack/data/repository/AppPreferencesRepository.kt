package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.ServiceKind

class AppPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = preferences.edit { putBoolean(KEY_NOTIFICATIONS, value) }

    var wifiOnly: Boolean
        get() = preferences.getBoolean(KEY_WIFI_ONLY, false)
        set(value) = preferences.edit { putBoolean(KEY_WIFI_ONLY, value) }

    var selectedServer: ServiceKind
        get() = preferences.getString(KEY_SELECTED_SERVER, null)
            ?.let { saved -> ServiceKind.entries.firstOrNull { it.name == saved } }
            ?.takeIf { it == ServiceKind.JELLYFIN || it == ServiceKind.EMBY }
            ?: ServiceKind.JELLYFIN
        set(value) = preferences.edit { putString(KEY_SELECTED_SERVER, value.name) }

    private companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_WIFI_ONLY = "wifi_only"
        const val KEY_SELECTED_SERVER = "selected_server"
    }
}
