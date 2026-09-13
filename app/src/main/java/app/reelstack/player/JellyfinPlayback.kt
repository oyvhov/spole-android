package app.reelstack.player

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.*
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class PlaybackChapter(
    val name: String,
    val startPositionMs: Long,
    val imageUrl: String? = null,
)

data class PlayableItem(
    val id: String, val title: String, val type: String,
    val subtitle: String = "", val durationMs: Long = 0, val resumeMs: Long = 0,
    val played: Boolean = false,
    val chapters: List<PlaybackChapter> = emptyList(),
    /**
     * The numbers behind [subtitle]. The player wrote "S03 E01" into its header because that was
     * the only form it had; with the numbers kept as numbers it can say the same thing the rest of
     * the app says, in the reader's own language.
     */
    val season: Int? = null,
    val episode: Int? = null,
    /** Needed to ask the server what comes after this episode. Empty for a film. */
    val seriesId: String = "",
    val logoUrl: String? = null,
)

data class PlaybackTrack(val index: Int, val label: String, val language: String?, val isText: Boolean = false)

/**
 * A stretch of the file the server has marked: the title sequence, or the closing credits.
 *
 * Jellyfin 10.10 answers for this itself; before that the Intro Skipper plugin did, in seconds and
 * under a different address. Both are asked for, because a household that added the plugin years
 * ago should not lose the one feature they installed it for by upgrading the app.
 */
data class PlaybackSegment(val kind: Kind, val startMs: Long, val endMs: Long) {
    enum class Kind { INTRO, OUTRO }
}

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

fun parsePlayable(item: JsonObject, baseUrl: String? = null): PlayableItem {
    val type = item.str("Type")
    require(type in setOf("Movie", "Episode", "Series", "Season")) { "Denne medietypen kan ikkje spelast her." }
    require(type in setOf("Series", "Season") || !item.flag("IsMissing") && item.str("LocationType") != "Virtual") { "Denne episoden ligg ikkje i biblioteket enno." }
    val id = item.str("Id").also { require(it.isNotBlank()) { "Tittelen manglar ein gyldig ID." } }
    val duration = ((item.num("RunTimeTicks") ?: 0) / 10_000).coerceAtLeast(0)
    val user = item.obj("UserData")
    val resume = ((user.num("PlaybackPositionTicks") ?: 0) / 10_000).coerceAtLeast(0)
    val season = item.num("ParentIndexNumber")?.toInt()
    val episode = item.num("IndexNumber")?.toInt()
    val subtitle = if (type == "Episode") listOfNotNull(
        season?.let { "S%02d".format(it) },
        episode?.let { "E%02d".format(it) },
    ).joinToString(" ") + " · " + item.str("Name") else ""
    val chapters = item.objects("Chapters").mapIndexedNotNull { index, chapterObj ->
        val name = chapterObj.str("Name").ifBlank { "Kapittel ${index + 1}" }
        val startTicks = chapterObj.num("StartPositionTicks") ?: 0L
        val startMs = (startTicks / 10_000).coerceAtLeast(0)
        val imageTag = chapterObj.str("ImageTag")
        val imageUrl = if (baseUrl != null) {
            "${baseUrl.trimEnd('/')}/Items/${enc(id)}/Images/Chapter/$index?maxWidth=320&quality=85" +
                (if (imageTag.isNotBlank()) "&tag=${enc(imageTag)}" else "")
        } else null
        PlaybackChapter(name, startMs, imageUrl)
    }
    return PlayableItem(id, if (type == "Episode") item.str("SeriesName").ifBlank { item.str("Name") } else item.str("Name"),
        type, subtitle, duration, if (user.flag("Played") || duration > 0 && resume >= duration) 0 else resume, user.flag("Played"),
        chapters = chapters, season = season, episode = episode,
        seriesId = item.str("SeriesId"), logoUrl = playableLogoUrl(item, baseUrl))
}

/** Use declared artwork only; never add speculative requests or credentials to image URLs. */
internal fun playableLogoUrl(item: JsonObject, baseUrl: String?): String? {
    if (baseUrl.isNullOrBlank()) return null
    val ownTag = item.obj("ImageTags").str("Logo")
    val parentTag = item.str("ParentLogoImageTag").ifBlank { item.str("SeriesLogoImageTag") }
    val id = if (ownTag.isNotBlank()) item.str("Id")
        else item.str("ParentLogoItemId").ifBlank { item.str("SeriesId") }
    val tag = ownTag.ifBlank { parentTag }
    if (id.isBlank() || tag.isBlank()) return null
    return "${baseUrl.trimEnd('/')}/Items/${enc(id)}/Images/Logo?maxWidth=480&quality=90&tag=${enc(tag)}"
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

/** Safe fallback when detection fails or a decoder rejects an advertised format. */
fun phonePlaybackProfile(bitrate: Int): JsonObject = devicePlaybackProfile(bitrate, DevicePlaybackCapabilities.CONSERVATIVE)

class JellyfinPlaybackClient(
    private val transport: JsonHttpTransport = HttpTransport(connectTimeoutMs = 5_000, readTimeoutMs = 8_000),
    private val deviceId: String,
    private val capabilities: () -> DevicePlaybackCapabilities = { DevicePlaybackCapabilities.CONSERVATIVE },
    private val sourceSupported: (JsonObject, Int?) -> Boolean = { _, _ -> true },
    private val videoSupported: (JsonObject) -> Boolean = { true },
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
        // Best effort: an older server that does not know the endpoint must not block playback.
        runCatching { announceCapabilities(connection) }
        return id
    }

    fun item(c: ServiceConnection, user: String, id: String) =
        parsePlayable(read(c, "Users/${enc(user)}/Items/${enc(id)}?Fields=Chapters"), c.baseUrl)

    /**
     * The marked stretches of this item, if anything has marked them.
     *
     * Absence is the normal case — most libraries have never been scanned for intros — so every
     * failure here is silent and the player simply never offers to skip anything.
     */
    fun segments(c: ServiceConnection, user: String, itemId: String): List<PlaybackSegment> {
        val modern = runCatching {
            read(c, "MediaSegments/${enc(itemId)}?includeSegmentTypes=Intro&includeSegmentTypes=Outro")
        }.getOrNull()?.objects("Items")?.mapNotNull { entry ->
            val kind = when (entry.str("Type").lowercase()) {
                "intro" -> PlaybackSegment.Kind.INTRO
                "outro", "credits" -> PlaybackSegment.Kind.OUTRO
                else -> return@mapNotNull null
            }
            val start = (entry.num("StartTicks") ?: return@mapNotNull null) / 10_000
            val end = (entry.num("EndTicks") ?: return@mapNotNull null) / 10_000
            PlaybackSegment(kind, start.coerceAtLeast(0), end).takeIf { it.endMs > it.startMs }
        }.orEmpty()
        if (modern.isNotEmpty()) return modern

        // The plugin's own route, in seconds. `Valid` is its way of saying "nothing found here".
        val plugin = runCatching { read(c, "Episode/${enc(itemId)}/IntroTimestamps/v1") }.getOrNull()
            ?: return emptyList()
        if (!plugin.flag("Valid")) return emptyList()
        val start = ((plugin["IntroStart"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: return emptyList())
        val end = ((plugin["IntroEnd"] as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull() ?: return emptyList())
        if (end <= start) return emptyList()
        return listOf(PlaybackSegment(PlaybackSegment.Kind.INTRO, (start * 1000).toLong(), (end * 1000).toLong()))
    }

    /**
     * The episode after this one, in the order the server keeps them.
     *
     * `adjacentTo` answers with the previous, the current and the next in one request, which is
     * both cheaper than listing a season and correct across a season boundary — the last episode of
     * one season is followed by the first of the next, and a client counting index numbers would
     * have stopped there. A film, a missing series id or a last episode all answer with null, and
     * the player simply does not offer anything.
     */
    fun nextEpisode(c: ServiceConnection, user: String, item: PlayableItem): PlayableItem? {
        if (item.type != "Episode" || item.seriesId.isBlank()) return null
        val json = runCatching {
            read(c, "Shows/${enc(item.seriesId)}/Episodes?userId=${enc(user)}" +
                "&adjacentTo=${enc(item.id)}&Fields=Chapters&IsMissing=false&EnableUserData=true")
        }.getOrNull() ?: return null
        val items = json.objects("Items")
        val here = items.indexOfFirst { it.str("Id") == item.id }
        val next = items.getOrNull(here + 1)?.takeIf { here >= 0 } ?: return null
        return runCatching { parsePlayable(next, c.baseUrl) }.getOrNull()
    }

    fun children(c: ServiceConnection, user: String, parent: PlayableItem, start: Int = 0): Pair<List<PlayableItem>, Boolean> {
        val path = if (parent.type == "Series") "Shows/${enc(parent.id)}/Seasons?userId=${enc(user)}" else
            "Items?userId=${enc(user)}&ParentId=${enc(parent.id)}&IncludeItemTypes=Episode&SortBy=IndexNumber&SortOrder=Ascending&Fields=Chapters"
        val json = read(c, "$path&IsMissing=false&EnableUserData=true&StartIndex=$start&Limit=100")
        val items = json.objects("Items")
        return items.mapNotNull { runCatching { parsePlayable(it, c.baseUrl) }.getOrNull() } to (start + items.size < (json.num("TotalRecordCount") ?: 0))
    }

    fun prepare(c: ServiceConnection, user: String, item: PlayableItem, bitrate: Int,
        audio: Int? = null, subtitle: Int? = null, compatible: Boolean = false, sourceId: String? = null): PlaybackPlan {
        require(item.type in setOf("Movie", "Episode")) { "Vel ein episode først." }
        val detected = if (compatible) DevicePlaybackCapabilities.CONSERVATIVE else
            runCatching { capabilities() }.getOrDefault(DevicePlaybackCapabilities.CONSERVATIVE)
        val payload = buildJsonObject {
            put("UserId", user); put("DeviceProfile", devicePlaybackProfile(bitrate, detected)); put("MaxStreamingBitrate", bitrate)
            // A VOD timeline starting at zero makes Media3 seek positions and server progress identical.
            put("StartTimeTicks", 0); put("IsPlayback", true); put("AutoOpenLiveStream", false)
            put("EnableDirectPlay", !compatible)
            put("EnableDirectStream", false); put("EnableTranscoding", true)
            put("AllowVideoStreamCopy", !compatible); put("AllowAudioStreamCopy", !compatible)
            put("MaxAudioChannels", detected.maxAudioChannels)
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
        val selectedAudio = audio ?: source.num("DefaultAudioStreamIndex")?.toInt() ?: tracks("Audio").firstOrNull()?.index
        val subtitleTrack = subtitles.firstOrNull { it.index == selectedSubtitle }
        val direct = source.flag("SupportsDirectPlay") && !compatible
        // HLS may still copy video while converting audio. Validate that original video too.
        if (!compatible && (!videoSupported(source) || direct && !sourceSupported(source, selectedAudio))) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, true, mediaSourceId)
        }
        // Image subtitles need burn-in; re-negotiate explicitly instead of silently dropping them.
        if (direct && subtitleTrack != null && !subtitleTrack.isText) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, true, mediaSourceId)
        }
        val url = if (direct) "Videos/${enc(item.id)}/stream?static=true&MediaSourceId=${enc(mediaSourceId)}&PlaySessionId=${enc(session)}" else
            source.str("TranscodingUrl").also { require(it.isNotBlank()) { "Jellyfin kan ikkje tilpasse denne fila. Prøv Jellyfin-appen." } }
        val subtitleUrl = subtitleTrack?.takeIf { it.isText }?.let {
            safePlaybackUrl(c.baseUrl, "Videos/${enc(item.id)}/${enc(mediaSourceId)}/Subtitles/${it.index}/Stream.vtt")
        }
        return PlaybackPlan(item, mediaSourceId, session, safePlaybackUrl(c.baseUrl, url), direct, tracks("Audio"), subtitles,
            selectedAudio, selectedSubtitle, subtitleUrl)
    }

    /** Playback support is separate from receiving remote commands. Until a remote-command
     * receiver exists, do not advertise Spole as a controllable "Play on" target. */
    fun announceCapabilities(c: ServiceConnection) {
        post(c, "Sessions/Capabilities/Full", buildJsonObject {
            put("PlayableMediaTypes", buildJsonArray { add("Video") })
            put("SupportedCommands", buildJsonArray { })
            put("SupportsMediaControl", false)
            put("SupportsPersistentIdentifier", true)
        })
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
