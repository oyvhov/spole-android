package app.reelstack.data.repository

import android.content.Context
import androidx.core.content.edit
import app.reelstack.data.model.HomeSection
import app.reelstack.data.model.decodeHomeSections
import kotlinx.serialization.json.*

class AppPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("reelstack_preferences", Context.MODE_PRIVATE)

    var personalization: app.reelstack.data.model.Personalization
        get() = app.reelstack.data.model.Personalization(
            accent = app.reelstack.data.model.AccentPalette.decode(preferences.getString("accent_palette", null)),
            artworkSize = app.reelstack.data.model.ArtworkSize.decode(preferences.getString("artwork_size", null)),
            autoResume = preferences.getBoolean("auto_resume", true),
            sidebarExpanded = if (preferences.contains("sidebar_expanded")) preferences.getBoolean("sidebar_expanded", true) else null,
            menuOrder = preferences.getString("menu_order", null)?.split(',') ?: app.reelstack.data.model.DEFAULT_MENU,
            hiddenMenuItems = preferences.getStringSet("menu_hidden", emptySet()).orEmpty().toSet(),
            showNextUp = preferences.getBoolean("show_next_up", true),
            combineContinueWatching = preferences.getBoolean("combine_continue", false),
            showHero = preferences.getBoolean("show_hero", true),
            showRatings = preferences.getBoolean("show_ratings", true),
            showQuality = preferences.getBoolean("show_quality", true),
            slowStartup = preferences.getBoolean("slow_startup", true),
            visualTheme = app.reelstack.data.model.VisualTheme.decode(preferences.getString("visual_theme", null)),
            artworkCorners = app.reelstack.data.model.ArtworkCorners.decode(preferences.getString("artwork_corners", null)),
            focusStyle = app.reelstack.data.model.FocusStyle.decode(preferences.getString("focus_style", null)),
            highContrast = preferences.getBoolean("high_contrast", false),
        )
        set(value) = preferences.edit {
            putString("accent_palette", value.accent.name)
            putString("artwork_size", value.artworkSize.name)
            putBoolean("auto_resume", value.autoResume)
            value.sidebarExpanded?.let { putBoolean("sidebar_expanded", it) } ?: remove("sidebar_expanded")
            putString("menu_order", value.menuOrder.joinToString(","))
            putStringSet("menu_hidden", value.hiddenMenuItems - setOf("HOME", "SETTINGS"))
            putBoolean("show_next_up", value.showNextUp)
            putBoolean("combine_continue", value.combineContinueWatching)
            putBoolean("show_hero", value.showHero)
            putBoolean("show_ratings", value.showRatings)
            putBoolean("show_quality", value.showQuality)
            putBoolean("slow_startup", value.slowStartup)
            putString("visual_theme", value.visualTheme.name)
            putString("artwork_corners", value.artworkCorners.name)
            putString("focus_style", value.focusStyle.name)
            putBoolean("high_contrast", value.highContrast)
        }

    fun observePersonalization(onChange: (app.reelstack.data.model.Personalization) -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in setOf("accent_palette", "artwork_size", "auto_resume", "sidebar_expanded", "menu_order", "menu_hidden",
                    "show_next_up", "combine_continue", "show_hero", "show_ratings", "show_quality", "slow_startup",
                    "visual_theme", "artwork_corners", "focus_style", "high_contrast")) onChange(personalization)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChange(personalization)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Null preserves the existing default; an empty saved set explicitly selects nothing. */
    fun selectedLibraryIds(connection: app.reelstack.data.model.ServiceConnection): Set<String>? =
        preferences.getStringSet(libraryKey(connection), null)?.toSet()

    fun setSelectedLibraryIds(connection: app.reelstack.data.model.ServiceConnection, ids: Set<String>) {
        preferences.edit { putStringSet(libraryKey(connection), ids.toSet()) }
    }

    fun libraryShortcuts(connection: app.reelstack.data.model.ServiceConnection): List<Pair<String, String>> = runCatching {
        Json.parseToJsonElement(preferences.getString("pins_" + libraryKey(connection), "{}")!!).jsonObject
            .map { it.key to it.value.jsonPrimitive.content }
            .filter { selectedLibraryIds(connection)?.contains(it.first) != false }
    }.getOrDefault(emptyList())

    fun setLibraryShortcuts(connection: app.reelstack.data.model.ServiceConnection, shortcuts: List<Pair<String, String>>) {
        preferences.edit { putString("pins_" + libraryKey(connection), buildJsonObject {
            shortcuts.distinctBy { it.first }.forEach { (id, name) -> put(id, name) }
        }.toString()) }
    }

    fun libraryIcons(connection: app.reelstack.data.model.ServiceConnection): Map<String, app.reelstack.data.model.LibraryIcon> = runCatching {
        Json.parseToJsonElement(preferences.getString("icons_" + libraryKey(connection), "{}")!!).jsonObject.mapNotNull { (key, value) ->
            app.reelstack.data.model.LibraryIcon.entries.find { it.name == value.jsonPrimitive.content }?.let { key to it }
        }.toMap()
    }.getOrDefault(emptyMap())

    fun setLibraryIcons(connection: app.reelstack.data.model.ServiceConnection, icons: Map<String, app.reelstack.data.model.LibraryIcon>) {
        preferences.edit { putString("icons_" + libraryKey(connection), buildJsonObject { icons.forEach { (id, icon) -> put(id, icon.name) } }.toString()) }
    }

    fun includesLibrary(connection: app.reelstack.data.model.ServiceConnection,
        view: app.reelstack.data.network.RemoteLibraryView): Boolean =
        selectedLibraryIds(connection)?.contains(view.id) ?: !app.reelstack.data.model.isExcludedHomeLibrary(view.name)

    fun librarySelectionFingerprint(connections: List<app.reelstack.data.model.ServiceConnection>): String =
        connections.filter { it.kind == app.reelstack.data.model.ServiceKind.JELLYFIN }.sortedBy { it.identity }
            .joinToString("|") { connection -> libraryKey(connection) + ":" +
                (selectedLibraryIds(connection)?.sorted()?.joinToString(",") ?: "default") }

    private fun libraryKey(connection: app.reelstack.data.model.ServiceConnection): String {
        val identity = "${connection.kind}|${connection.identity.trimEnd('/')}|${connection.userId}"
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        return "library_selection_" + digest.joinToString("") { "%02x".format(it) }
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
