package app.reelstack.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import app.reelstack.data.model.*

class KidsPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("spole_kids_preferences", Context.MODE_PRIVATE)

    fun read(profileId: String): KidsPreferences {
        val p = "${profileId.trim().removePrefix("kid.")}."
        return KidsPreferences(
            world = KidsWorld.decode(preferences.getString(p + "world", null)),
            allowAppearance = preferences.getBoolean(p + "appearance", true),
            decorations = preferences.getBoolean(p + "decorations", true),
            // v2 deliberately defaults old profiles to the readable mobile/tablet layout.
            // The old preference was introduced before the label was reliably wired to the home.
            libraryTitlesBelow = preferences.getBoolean(p + "library_titles_below_v2", true),
            reduceMotion = preferences.getBoolean(p + "reduce_motion", false),
            autoplay = preferences.getBoolean(p + "autoplay", false),
            episodeLimit = preferences.getInt(p + "episode_limit", 3).coerceIn(1, 3),
            subtitles = SubtitleLanguage.decode(preferences.getString(p + "subtitles", null), SubtitleLanguage.NORWEGIAN),
        )
    }

    fun save(profileId: String, value: KidsPreferences) {
        require(profileId.isNotBlank())
        val p = "${profileId.trim().removePrefix("kid.")}."
        preferences.edit {
            putString(p + "world", value.world.name)
            putBoolean(p + "appearance", value.allowAppearance)
            putBoolean(p + "decorations", value.decorations)
            putBoolean(p + "library_titles_below_v2", value.libraryTitlesBelow)
            putBoolean(p + "reduce_motion", value.reduceMotion)
            putBoolean(p + "autoplay", value.autoplay)
            putInt(p + "episode_limit", value.episodeLimit.coerceIn(1, 3))
            putString(p + "subtitles", value.subtitles.name)
        }
    }

    /** The child-facing write cannot change parental playback or permission choices. */
    fun saveAppearance(profileId: String, world: KidsWorld, decorations: Boolean) {
        val current = read(profileId)
        if (current.allowAppearance) save(profileId, current.copy(world = world, decorations = decorations))
    }

    fun observe(profileId: String, change: (KidsPreferences) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> change(read(profileId)) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
