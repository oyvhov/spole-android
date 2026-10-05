package app.reelstack.data.repository

import android.content.SharedPreferences
import app.reelstack.data.security.TokenStore

/** Run before loading connections. Only exact obsolete service records are removed. */
internal fun removeObsoleteServices(preferences: SharedPreferences, tokens: TokenStore) {
    if (preferences.getBoolean("personal_calendar_migration_v1", false)) return
    val profiles = preferences.getStringSet("kid_profile_ids", emptySet()).orEmpty()
    val prefixes = buildSet {
        listOf("radarr", "sonarr").forEach { service ->
            add(service)
            profiles.forEach { add("kid.${it.removePrefix("kid.")}.$service") }
        }
        // Also clean orphaned exact profile/service records from older profile formats.
        preferences.all.keys.forEach { key ->
            if (key.startsWith("kid.")) listOf(".radarr.", ".sonarr.").forEach { marker ->
                if (marker in key) add(key.substringBefore(marker) + marker.dropLast(1))
            }
        }
    }
    prefixes.forEach { tokens.remove("$it.token") }
    val edit = preferences.edit()
    preferences.all.keys.filter { key -> prefixes.any { key.startsWith("$it.") } }.forEach(edit::remove)
    edit.putBoolean("personal_calendar_migration_v1", true)
    check(edit.commit()) { "Unable to migrate obsolete service settings" }
}
