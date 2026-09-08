package app.reelstack.player

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class PlayableItem(
    val id: String, val title: String, val type: String,
    val subtitle: String = "", val durationMs: Long = 0, val resumeMs: Long = 0,
    val played: Boolean = false,
)

data class PlaybackTrack(val index: Int, val label: String, val language: String?, val isText: Boolean = false)

/** No secrets or server paths belong in intents, saved state or diagnostic text. */
data class PlaybackPlan(
    val item: PlayableItem, val sourceId: String, val sessionId: String,
    val url: String, val direct: Boolean, val audio: List<PlaybackTrack>, val subtitles: List<PlaybackTrack>,
    val audioIndex: Int?, val subtitleIndex: Int, val subtitleUrl: String?,
) {
    override fun toString() = "PlaybackPlan(direct=$direct)"
}

internal fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
internal fun JsonObject.str(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.num(key: String) = (get(key) as? JsonPrimitive)?.longOrNull
internal fun JsonObject.flag(key: String) = (get(key) as? JsonPrimitive)?.booleanOrNull == true
internal fun JsonObject.obj(key: String) = get(key) as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonObject.objects(key: String) = (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

fun parsePlayable(item: JsonObject): PlayableItem {
    val type = item.str("Type")
    require(type in setOf("Movie", "Episode", "Series", "Season")) { "Denne medietypen kan ikkje spelast her." }
    require(type in setOf("Series", "Season") || !item.flag("IsMissing") && item.str("LocationType") != "Virtual") { "Denne episoden ligg ikkje i biblioteket enno." }
    val id = item.str("Id").also { require(it.isNotBlank()) { "Tittelen manglar ein gyldig ID." } }
    val duration = ((item.num("RunTimeTicks") ?: 0) / 10_000).coerceAtLeast(0)
    val user = item.obj("UserData")
    val resume = ((user.num("PlaybackPositionTicks") ?: 0) / 10_000).coerceAtLeast(0)
    val subtitle = if (type == "Episode") listOfNotNull(
        item.num("ParentIndexNumber")?.let { "S%02d".format(it) },
        item.num("IndexNumber")?.let { "E%02d".format(it) },
    ).joinToString(" ") + " · " + item.str("Name") else ""
    return PlayableItem(id, if (type == "Episode") item.str("SeriesName").ifBlank { item.str("Name") } else item.str("Name"),
        type, subtitle, duration, if (user.flag("Played") || duration > 0 && resume >= duration) 0 else resume, user.flag("Played"))
}

/** Same-origin, same-base-path only; remove server-generated credentials before Media3 sees a URI. */
fun safePlaybackUrl(baseUrl: String, value: String): String {
    val base = URI(EndpointValidator.normalizeBaseUrl(baseUrl).trimEnd('/') + "/")
    require(value.none { it.isISOControl() || it == '\\' }) { "Utrygg medieadresse." }
    val input = URI(value)
    val resolved = when {
        input.isAbsolute || input.rawAuthority != null -> base.resolve(input)
        value.startsWith(base.path) -> base.resolve(input)
        else -> base.resolve(value.trimStart('/'))
    }
    fun port(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme == "https") 443 else 80
    require(resolved.scheme.equals(base.scheme, true) && resolved.host.equals(base.host, true) && port(resolved) == port(base) &&
        resolved.rawUserInfo == null && resolved.rawFragment == null) { "Mediestraumen peikar utanfor Jellyfin-tenaren." }
    val path = resolved.path.orEmpty()
    require(path.startsWith(base.path) && path.split('/').none { it == "." || it == ".." } &&
        path.none { it == '\\' || it.isISOControl() } && !path.contains('%')) { "Utrygg mediestig." }
    val query = resolved.rawQuery?.split('&')?.filterNot {
        URLDecoder.decode(it.substringBefore('='), "UTF-8").lowercase() in setOf("api_key", "apikey", "token", "access_token", "x-emby-token")
    }?.joinToString("&")?.takeIf(String::isNotBlank)
    return "${resolved.scheme}://${resolved.rawAuthority}${resolved.rawPath}" + (query?.let { "?$it" } ?: "")
}

/** Conservative phone profile. Unsupported/high-bit-depth media is handled by the server, not guessed. */
fun phonePlaybackProfile(bitrate: Int): JsonObject = buildJsonObject {
    put("Name", "Spole Android")
    put("MaxStreamingBitrate", bitrate)
    put("MaxStaticBitrate", bitrate)
    putJsonArray("DirectPlayProfiles") { add(buildJsonObject {
        put("Type", "Video"); put("Container", "mp4,m4v,mkv,mov")
        put("VideoCodec", "h264"); put("AudioCodec", "aac,mp3")
    }) }
    putJsonArray("TranscodingProfiles") { add(buildJsonObject {
        put("Type", "Video"); put("Container", "ts"); put("Protocol", "hls")
        put("VideoCodec", "h264"); put("AudioCodec", "aac"); put("Context", "Streaming")
        put("MaxAudioChannels", "2"); put("MinSegments", 2); put("SegmentLength", 3)
        put("CopyTimestamps", false); put("EnableSubtitlesInManifest", false)
    }) }
    putJsonArray("CodecProfiles") {
        add(buildJsonObject {
            put("Type", "Video"); put("Codec", "h264")
            putJsonArray("Conditions") {
                for ((name, value) in listOf("Width" to "1920", "Height" to "1080", "VideoBitDepth" to "8", "VideoLevel" to "42")) {
                    add(buildJsonObject { put("Condition", "LessThanEqual"); put("Property", name); put("Value", value); put("IsRequired", false) })
                }
            }
        })
        add(buildJsonObject {
            put("Type", "VideoAudio")
            putJsonArray("Conditions") { add(buildJsonObject {
                put("Condition", "LessThanEqual"); put("Property", "AudioChannels"); put("Value", "2"); put("IsRequired", false)
            }) }
        })
    }
    putJsonArray("SubtitleProfiles") {
        add(buildJsonObject { put("Format", "vtt"); put("Method", "External") })
        for (format in listOf("pgssub", "dvdsub", "dvbsub")) add(buildJsonObject { put("Format", format); put("Method", "Encode") })
    }
}

class JellyfinPlaybackClient(
    private val transport: JsonHttpTransport = HttpTransport(connectTimeoutMs = 5_000, readTimeoutMs = 8_000),
    private val deviceId: String,
) {
    fun headers(connection: ServiceConnection) = mapOf("Authorization" to jellyfinAuthorization(deviceId, connection.token))

    fun verify(connection: ServiceConnection): String {
        require(connection.kind == ServiceKind.JELLYFIN && connection.token.isNotBlank()) { "Logg inn på Jellyfin for å spele av." }
        val user = read(connection, "Users/Me")
        val id = user.str("Id")
        require(id.isNotBlank() && (connection.userId.isBlank() || connection.userId.equals(id, true))) { "Jellyfin-kontoen er endra. Logg inn på nytt." }
        require(!user.obj("Policy").flag("IsDisabled") && user.obj("Policy")["EnableMediaPlayback"] != JsonPrimitive(false)) {
            "Denne Jellyfin-kontoen har ikkje løyve til å spele av."
        }
        return id
    }

    fun item(c: ServiceConnection, user: String, id: String) = parsePlayable(read(c, "Users/${enc(user)}/Items/${enc(id)}"))

    fun children(c: ServiceConnection, user: String, parent: PlayableItem, start: Int = 0): Pair<List<PlayableItem>, Boolean> {
        val path = if (parent.type == "Series") "Shows/${enc(parent.id)}/Seasons?userId=${enc(user)}" else
            "Items?userId=${enc(user)}&ParentId=${enc(parent.id)}&IncludeItemTypes=Episode&SortBy=IndexNumber&SortOrder=Ascending"
        val json = read(c, "$path&IsMissing=false&EnableUserData=true&StartIndex=$start&Limit=100")
        val items = json.objects("Items")
        return items.mapNotNull { runCatching { parsePlayable(it) }.getOrNull() } to (start + items.size < (json.num("TotalRecordCount") ?: 0))
    }

    fun prepare(c: ServiceConnection, user: String, item: PlayableItem, bitrate: Int,
        audio: Int? = null, subtitle: Int? = null, compatible: Boolean = false, sourceId: String? = null): PlaybackPlan {
        require(item.type in setOf("Movie", "Episode")) { "Vel ein episode først." }
        val payload = buildJsonObject {
            put("UserId", user); put("DeviceProfile", phonePlaybackProfile(bitrate)); put("MaxStreamingBitrate", bitrate)
            // A VOD timeline starting at zero makes Media3 seek positions and server progress identical.
            put("StartTimeTicks", 0); put("IsPlayback", true); put("AutoOpenLiveStream", false)
            put("EnableDirectPlay", !compatible && audio == null && subtitle == null)
            put("EnableDirectStream", false); put("EnableTranscoding", true)
            put("AllowVideoStreamCopy", !compatible); put("AllowAudioStreamCopy", !compatible)
            put("MaxAudioChannels", 2)
            audio?.let { put("AudioStreamIndex", it) }; subtitle?.let { put("SubtitleStreamIndex", it) }
            sourceId?.let { put("MediaSourceId", it) }
        }
        val response = post(c, "Items/${enc(item.id)}/PlaybackInfo", payload)
        require(response.str("ErrorCode").isBlank()) { "Jellyfin fann ikkje eit format denne eininga kan spele. Prøv Jellyfin-appen." }
        val source = response.objects("MediaSources").firstOrNull { !it.flag("RequiresOpening") && (sourceId == null || it.str("Id") == sourceId) &&
            (it.flag("SupportsDirectPlay") || it.str("TranscodingUrl").isNotBlank()) }
            ?: error("Ingen spelbar versjon. Kontroller avspelings- og omkodingsløyva i Jellyfin.")
        val mediaSourceId = source.str("Id").also { require(it.isNotBlank()) { "Jellyfin manglar mediekjelde." } }
        val session = response.str("PlaySessionId").also { require(it.isNotBlank()) { "Jellyfin manglar avspelingsøkt." } }
        val streams = source.objects("MediaStreams")
        fun tracks(type: String) = streams.filter { it.str("Type") == type }.mapNotNull {
            val index = it.num("Index")?.toInt() ?: return@mapNotNull null
            PlaybackTrack(index, it.str("DisplayTitle").ifBlank { it.str("Language").ifBlank { "$type ${index + 1}" } },
                it.str("Language").takeIf(String::isNotBlank), it.flag("IsTextSubtitleStream") || it.str("Codec") in setOf("srt", "subrip", "ass", "ssa", "webvtt", "vtt", "mov_text"))
        }
        val subtitles = tracks("Subtitle")
        val selectedSubtitle = subtitle ?: source.num("DefaultSubtitleStreamIndex")?.toInt() ?: -1
        val selectedAudio = audio ?: source.num("DefaultAudioStreamIndex")?.toInt()
        val subtitleTrack = subtitles.firstOrNull { it.index == selectedSubtitle }
        val direct = source.flag("SupportsDirectPlay") && !compatible && audio == null && subtitle == null
        // Image subtitles need burn-in; re-negotiate explicitly instead of silently dropping them.
        if (direct && subtitleTrack != null && !subtitleTrack.isText) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, compatible, mediaSourceId)
        }
        val url = if (direct) "Videos/${enc(item.id)}/stream?static=true&MediaSourceId=${enc(mediaSourceId)}&PlaySessionId=${enc(session)}" else
            source.str("TranscodingUrl").also { require(it.isNotBlank()) { "Jellyfin kan ikkje tilpasse denne fila. Prøv Jellyfin-appen." } }
        val subtitleUrl = subtitleTrack?.takeIf { it.isText }?.let {
            safePlaybackUrl(c.baseUrl, "Videos/${enc(item.id)}/${enc(mediaSourceId)}/Subtitles/${it.index}/Stream.vtt")
        }
        return PlaybackPlan(item, mediaSourceId, session, safePlaybackUrl(c.baseUrl, url), direct, tracks("Audio"), subtitles,
            selectedAudio, selectedSubtitle, subtitleUrl)
    }

    fun report(c: ServiceConnection, plan: PlaybackPlan, event: String, positionMs: Long, paused: Boolean) {
        require(event in setOf("", "/Progress", "/Stopped"))
        post(c, "Sessions/Playing$event", buildJsonObject {
            put("ItemId", plan.item.id); put("MediaSourceId", plan.sourceId); put("PlaySessionId", plan.sessionId)
            put("PositionTicks", positionMs.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE / 10_000) * 10_000)
            put("IsPaused", paused); put("CanSeek", true); put("PlayMethod", if (plan.direct) "DirectPlay" else "Transcode")
            plan.audioIndex?.let { put("AudioStreamIndex", it) }; put("SubtitleStreamIndex", plan.subtitleIndex)
        })
    }

    private fun read(c: ServiceConnection, path: String) = decode(transport.get(EndpointValidator.resolve(c.baseUrl, path), headers(c)))
    private fun post(c: ServiceConnection, path: String, body: JsonObject) = decode(transport.post(EndpointValidator.resolve(c.baseUrl, path), headers(c), body.toString()))
    private fun decode(response: HttpResponse): JsonObject {
        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> error("Jellyfin avviste avspelinga. Kontroller innlogging og avspelingsløyve.")
            404 -> error("Denne tittelen er ikkje tilgjengeleg i Jellyfin no.")
            in 300..399 -> error("Medieadressa er flytta. Kontroller Jellyfin-adressa i innstillingane.")
            else -> error("Fekk ikkje starta avspelinga frå Jellyfin. Prøv igjen.")
        }
        return if (response.body.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(response.body).jsonObject
    }
}

fun playbackTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}
