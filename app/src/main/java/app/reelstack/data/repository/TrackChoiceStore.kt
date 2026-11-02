package app.reelstack.data.repository

import android.content.Context
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.PlayableItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * An audio or subtitle track as a description rather than a number.
 *
 * Stream numbers change from one file to the next, so the next episode is searched for the closest
 * thing to this: the same language first, then title, forced and hearing-impaired flags, and only
 * then codec, channel count and position. [position] is the place among the tracks of its kind.
 */
data class RememberedTrack(
    val language: String?,
    val title: String?,
    val forced: Boolean,
    val hearingImpaired: Boolean,
    val codec: String?,
    val channels: Int,
    val position: Int,
)

/** Subtitles can be chosen to be off, and that choice is remembered like any other. */
sealed interface RememberedSubtitle {
    data object Off : RememberedSubtitle
    data class Track(val track: RememberedTrack) : RememberedSubtitle
}

/** What a series (or a film) remembers: no opinion is null, an explicit "no subtitles" is [RememberedSubtitle.Off]. */
data class TrackChoices(val audio: RememberedTrack? = null, val subtitle: RememberedSubtitle? = null)

/**
 * Audio and subtitle choice, remembered per profile and per series.
 *
 * Jellyfin keeps a choice per episode, which is the wrong unit: someone who switches the subtitles
 * off in the third episode means the whole series. The key holds the service, the active profile
 * and the series (a film is its own series), so a child profile never inherits what a parent
 * picked and the other way round.
 *
 * Local only, and nothing here is secret: languages, flags and a codec name. The oldest series are
 * forgotten beyond [MAX_ENTRIES], so the file cannot grow for as long as the app is installed.
 */
class TrackChoiceStore(context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(key: String): TrackChoices {
        val entry = entry(key) ?: return TrackChoices()
        return TrackChoices(audio = entry["a"]?.let(::decodeTrack), subtitle = entry["s"]?.let(::decodeSubtitle))
    }

    @Synchronized fun rememberAudio(key: String, track: RememberedTrack) =
        write(key, "a", encodeTrack(track))

    @Synchronized fun rememberSubtitle(key: String, choice: RememberedSubtitle) = write(key, "s", when (choice) {
        RememberedSubtitle.Off -> JsonPrimitive("off")
        is RememberedSubtitle.Track -> encodeTrack(choice.track)
    })

    private fun entry(key: String): JsonObject? = runCatching {
        Json.parseToJsonElement(preferences.getString(key, null) ?: return null).jsonObject
    }.getOrNull()

    private fun write(key: String, field: String, value: JsonElement) {
        val current = entry(key).orEmpty()
        val updated = JsonObject(current + mapOf(field to value, "u" to JsonPrimitive(clock())))
        preferences.edit().putString(key, updated.toString()).apply()
        prune()
    }

    private fun updated(key: String): Long = (entry(key)?.get("u") as? JsonPrimitive)?.longOrNull ?: Long.MIN_VALUE

    private fun prune() {
        val keys = preferences.all.keys
        if (keys.size <= MAX_ENTRIES) return
        val oldest = keys.sortedBy(::updated).take(keys.size - MAX_ENTRIES)
        preferences.edit().apply { oldest.forEach(::remove) }.apply()
    }

    private fun encodeTrack(track: RememberedTrack) = buildJsonObject {
        put("lang", track.language); put("title", track.title)
        put("forced", track.forced); put("sdh", track.hearingImpaired)
        put("codec", track.codec); put("ch", track.channels); put("pos", track.position)
    }

    /** All fields or nothing: half a description is more likely a damaged file than a choice. */
    private fun decodeTrack(element: JsonElement): RememberedTrack? {
        val track = element as? JsonObject ?: return null
        fun text(name: String) = (track[name] as? JsonPrimitive)?.contentOrNull
        fun flag(name: String) = (track[name] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
        fun number(name: String) = (track[name] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        return RememberedTrack(
            language = text("lang"), title = text("title"),
            forced = flag("forced") ?: return null, hearingImpaired = flag("sdh") ?: return null,
            codec = text("codec"), channels = number("ch") ?: return null, position = number("pos") ?: return null,
        )
    }

    private fun decodeSubtitle(element: JsonElement): RememberedSubtitle? =
        if (element is JsonPrimitive && element.isString && element.content == "off") RememberedSubtitle.Off
        else decodeTrack(element)?.let(RememberedSubtitle::Track)

    companion object {
        const val PREFERENCES = "track_choices"

        /** Enough for years of series; each entry is a few hundred bytes. */
        const val MAX_ENTRIES = 200

        /** The series an episode belongs to, or the film itself, for one profile on one server. */
        fun key(kind: ServiceKind, profileId: String, item: PlayableItem): String =
            "${kind.name}|$profileId|${item.seriesId.ifBlank { item.id }}"
    }
}
