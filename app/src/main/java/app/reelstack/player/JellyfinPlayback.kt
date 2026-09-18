package app.reelstack.player

import app.reelstack.R
import app.reelstack.data.network.serviceError
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.SubtitleLanguage
import app.reelstack.data.model.MediaTrack
import app.reelstack.data.model.preferredSubtitleIndex
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
    val artworkUrl: String? = null,
    val lastPlayedEpochMillis: Long? = null,
)

data class PlaybackTrack(
    val index: Int, val label: String, val language: String?, val isText: Boolean = false,
    /** Stable across Emby/Jellyfin stream reordering, unlike [index]. */
    val key: String = trackKey(label, language, null, false),
)

internal fun trackKey(label: String, language: String?, codec: String?, forced: Boolean): String =
    listOf(language.orEmpty().lowercase().trim(), codec.orEmpty().lowercase().trim(),
        label.lowercase().replace(Regex("\\s+"), " ").trim(), forced.toString()).joinToString("|")

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

/**
 * What the server is actually doing with the file.
 *
 * "Direct or not" was all the plan carried, and it is not enough to answer the one question every
 * self-hosting household asks: *why is my server transcoding?* Copying the picture and converting
 * only the sound costs a server almost nothing; re-encoding the picture is what makes a fan spin up
 * and a stream stutter. Those two were the same word.
 */
enum class PlaybackMode {
    /** The file is sent untouched. */
    DIRECT_PLAY,

    /** Both streams are copied; only the container changes, usually to HLS. */
    DIRECT_STREAM,

    /** The picture is copied and the sound is converted — the common case on a TV without EAC3. */
    AUDIO_TRANSCODE,

    /** The server re-encodes the picture. The expensive one. */
    FULL_TRANSCODE,
    ;

    val copiesVideo: Boolean get() = this != FULL_TRANSCODE
}

/** No secrets or server paths belong in intents, saved state or diagnostic text. */
data class PlaybackPlan(
    val item: PlayableItem, val sourceId: String, val sessionId: String,
    val url: String, val mode: PlaybackMode, val audio: List<PlaybackTrack>, val subtitles: List<PlaybackTrack>,
    val audioIndex: Int?, val subtitleIndex: Int, val subtitleUrl: String?,
    /**
     * The server's own words for why it could not send the file as it is, such as
     * `AudioCodecNotSupported`. Free text from the server, so it is mapped to a sentence for
     * display and never shown raw.
     */
    val transcodeReasons: List<String> = emptyList(),
    val compatibility: PlaybackCompatibility = PlaybackCompatibility.DIRECT,
    val sourceFrameRate: Float = 0f,
) {
    /** Kept for every caller that only needs "is the file being sent untouched". */
    val direct: Boolean get() = mode == PlaybackMode.DIRECT_PLAY

    override fun toString() = "PlaybackPlan(mode=$mode)"
}

/** Reads one query value out of a server-built URL, whatever case the server spelled it in. */
internal fun playbackUrlValue(url: String, key: String): String {
    val query = url.substringAfter('?', "")
    if (query.isBlank()) return ""
    return query.split('&').firstNotNullOfOrNull { pair ->
        val name = pair.substringBefore('=')
        if (!name.equals(key, ignoreCase = true)) null
        else runCatching { URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") }.getOrDefault("")
    }.orEmpty()
}

/**
 * Which of the four things the server settled on.
 *
 * Both servers build the transcoding URL themselves and spell out `VideoCodec=copy` /
 * `AudioCodec=copy` in it when a stream is only being remuxed. That is the one signal both Jellyfin
 * and Emby agree on, across versions, so it is what this reads — rather than a field that only one
 * of them sets.
 */
internal fun playbackModeFor(source: JsonObject, direct: Boolean): PlaybackMode {
    if (direct) return PlaybackMode.DIRECT_PLAY
    val url = source.str("TranscodingUrl")
    val videoCopy = playbackUrlValue(url, "VideoCodec").equals("copy", ignoreCase = true)
    val audioCopy = playbackUrlValue(url, "AudioCodec").equals("copy", ignoreCase = true)
    return when {
        videoCopy && audioCopy -> PlaybackMode.DIRECT_STREAM
        videoCopy -> PlaybackMode.AUDIO_TRANSCODE
        else -> PlaybackMode.FULL_TRANSCODE
    }
}

/** The reasons the server gave, from whichever of the three places that server puts them. */
internal fun playbackTranscodeReasons(source: JsonObject): List<String> {
    fun split(value: String) = value.split(',').map(String::trim).filter(String::isNotEmpty)
    (source["TranscodeReasons"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
        ?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    split(source.str("TranscodeReasons")).takeIf { it.isNotEmpty() }?.let { return it }
    return split(playbackUrlValue(source.str("TranscodingUrl"), "TranscodeReasons"))
}

internal fun enc(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
internal fun JsonObject.str(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.num(key: String) = (get(key) as? JsonPrimitive)?.longOrNull
internal fun JsonObject.flag(key: String) = (get(key) as? JsonPrimitive)?.booleanOrNull == true
internal fun JsonObject.obj(key: String) = get(key) as? JsonObject ?: JsonObject(emptyMap())
internal fun JsonObject.objects(key: String) = (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

fun parsePlayable(item: JsonObject, baseUrl: String? = null): PlayableItem {
    val type = item.str("Type")
    if (type !in setOf("Movie", "Episode", "Video", "Series", "Season")) serviceError(R.string.player_err_media_type)
    if (type !in setOf("Series", "Season") && (item.flag("IsMissing") || item.str("LocationType") == "Virtual")) serviceError(R.string.player_err_episode_missing)
    val id = item.str("Id").also { if (it.isBlank()) serviceError(R.string.player_err_server_incomplete) }
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
        lastPlayedEpochMillis = runCatching { java.time.Instant.parse(user.str("LastPlayedDate")).toEpochMilli() }.getOrNull(),
        seriesId = item.str("SeriesId"), logoUrl = playableLogoUrl(item, baseUrl),
        artworkUrl = playableLandscapeUrl(item, baseUrl) ?: if (!baseUrl.isNullOrBlank() && item.obj("ImageTags").str("Primary").isNotBlank())
            "${baseUrl.trimEnd('/')}/Items/${enc(id)}/Images/Primary?maxWidth=320&quality=85&tag=${enc(item.obj("ImageTags").str("Primary"))}" else null)
}

/** A new movie on the resume shelf needs landscape art before the feed refresh arrives. */
internal fun playableLandscapeUrl(item: JsonObject, baseUrl: String?): String? {
    if (baseUrl.isNullOrBlank() || item.str("Type") != "Movie") return null
    val backdrop = (item["BackdropImageTags"] as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull
    val thumb = item.obj("ImageTags").str("Thumb")
    val type = if (!backdrop.isNullOrBlank()) "Backdrop/0" else if (thumb.isNotBlank()) "Thumb" else return null
    val tag = backdrop?.takeIf { it.isNotBlank() } ?: thumb
    return "${baseUrl.trimEnd('/')}/Items/${enc(item.str("Id"))}/Images/$type?maxWidth=960&quality=90&tag=${enc(tag)}"
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
    require(value.none { it.isISOControl() || it == '\\' }) { "unsafe media URL" }
    val input = URI(value)
    val resolved = when {
        input.isAbsolute || input.rawAuthority != null -> base.resolve(input)
        value.startsWith(base.path) -> base.resolve(input)
        else -> base.resolve(value.trimStart('/'))
    }
    fun port(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme == "https") 443 else 80
    require(resolved.scheme.equals(base.scheme, true) && resolved.host.equals(base.host, true) && port(resolved) == port(base) &&
        resolved.rawUserInfo == null && resolved.rawFragment == null) { "media stream points outside the server" }
    val path = resolved.path.orEmpty()
    require(path.startsWith(base.path) && path.split('/').none { it == "." || it == ".." } &&
        path.none { it == '\\' || it.isISOControl() } && !path.contains('%')) { "unsafe media path" }
    val query = resolved.rawQuery?.split('&')?.filterNot {
        URLDecoder.decode(it.substringBefore('='), "UTF-8").lowercase() in setOf("api_key", "apikey", "token", "access_token", "x-emby-token")
    }?.joinToString("&")?.takeIf(String::isNotBlank)
    return "${resolved.scheme}://${resolved.rawAuthority}${resolved.rawPath}" + (query?.let { "?$it" } ?: "")
}

/** Safe fallback when detection fails or a decoder rejects an advertised format. */
fun phonePlaybackProfile(bitrate: Int): JsonObject = devicePlaybackProfile(bitrate, DevicePlaybackCapabilities.CONSERVATIVE)

enum class PlaybackCompatibility {
    DIRECT,
    AUDIO_ONLY,
    FULL,
}

class MediaPlaybackClient(
    private val transport: JsonHttpTransport = HttpTransport(connectTimeoutMs = 5_000, readTimeoutMs = 8_000),
    private val deviceId: String,
    private val capabilities: () -> DevicePlaybackCapabilities = { DevicePlaybackCapabilities.CONSERVATIVE },
    private val sourceSupported: (JsonObject, Int?) -> Boolean = { _, _ -> true },
    private val videoSupported: (JsonObject) -> Boolean = { true },
) {
    fun headers(connection: ServiceConnection): Map<String, String> = when (connection.kind) {
        ServiceKind.JELLYFIN -> mapOf("Authorization" to jellyfinAuthorization(deviceId, connection.token))
        ServiceKind.EMBY -> mapOf(
            "X-Emby-Token" to connection.token,
            "X-Emby-Authorization" to embyAuthorization(deviceId, connection.userId),
        )
        else -> emptyMap()
    }

    fun verify(connection: ServiceConnection): String {
        if (connection.kind !in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY) || connection.token.isBlank()) serviceError(R.string.player_err_sign_in_media)
        val user = read(connection, playbackProfilePath(connection))
        val id = user.str("Id")
        if (id.isBlank() || connection.userId.isNotBlank() && !connection.userId.equals(id, true)) serviceError(R.string.player_err_account_switched)
        if (user.obj("Policy").flag("IsDisabled") || user.obj("Policy")["EnableMediaPlayback"] == JsonPrimitive(false)) {
            serviceError(R.string.player_err_no_permission)
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
        if (c.kind == ServiceKind.EMBY) return runCatching {
            embyPlaybackSegments(read(c, "Users/${enc(user)}/Items/${enc(itemId)}?Fields=Chapters"))
        }.getOrDefault(emptyList())
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
        audio: Int? = null, subtitle: Int? = null, compatible: Boolean = false, sourceId: String? = null,
        preferredLanguage: SubtitleLanguage = SubtitleLanguage.SERVER, fallbackLanguage: SubtitleLanguage = SubtitleLanguage.NONE): PlaybackPlan =
        prepare(c, user, item, bitrate, audio, subtitle,
            if (compatible) PlaybackCompatibility.FULL else PlaybackCompatibility.DIRECT,
            sourceId, preferredLanguage, fallbackLanguage)

    internal fun prepare(c: ServiceConnection, user: String, item: PlayableItem, bitrate: Int,
        audio: Int?, subtitle: Int?, compatibility: PlaybackCompatibility, sourceId: String?,
        preferredLanguage: SubtitleLanguage, fallbackLanguage: SubtitleLanguage): PlaybackPlan {
        if (item.type !in setOf("Movie", "Episode", "Video")) serviceError(R.string.player_err_pick_episode)
        val detected = if (compatibility == PlaybackCompatibility.FULL) DevicePlaybackCapabilities.CONSERVATIVE else
            runCatching { capabilities() }.getOrDefault(DevicePlaybackCapabilities.CONSERVATIVE)
        val payload = buildJsonObject {
            put("UserId", user); put("DeviceProfile", devicePlaybackProfile(bitrate, detected)); put("MaxStreamingBitrate", bitrate)
            // A VOD timeline starting at zero makes Media3 seek positions and server progress identical.
            put("StartTimeTicks", 0); put("IsPlayback", true); put("AutoOpenLiveStream", false)
            put("EnableDirectPlay", compatibility == PlaybackCompatibility.DIRECT)
            put("EnableDirectStream", compatibility != PlaybackCompatibility.FULL); put("EnableTranscoding", true)
            put("AllowVideoStreamCopy", compatibility != PlaybackCompatibility.FULL)
            put("AllowAudioStreamCopy", compatibility == PlaybackCompatibility.DIRECT)
            put("MaxAudioChannels", detected.maxAudioChannels)
            audio?.let { put("AudioStreamIndex", it) }; subtitle?.let { put("SubtitleStreamIndex", it) }
            sourceId?.let { put("MediaSourceId", it) }
        }
        val response = decode(playbackRequest {
            transport.post(EndpointValidator.resolve(c.baseUrl, "Items/${enc(item.id)}/PlaybackInfo"), headers(c), payload.toString())
        })
        if (response.str("ErrorCode").isNotBlank()) serviceError(R.string.player_err_no_format)
        val source = response.objects("MediaSources").firstOrNull { !it.flag("RequiresOpening") && (sourceId == null || it.str("Id") == sourceId) &&
            (it.flag("SupportsDirectPlay") || originalDirectStreamUrl(it) != null || it.str("TranscodingUrl").isNotBlank()) }
            ?: serviceError(R.string.player_err_no_version)
        val mediaSourceId = source.str("Id").also { if (it.isBlank()) serviceError(R.string.player_err_server_incomplete) }
        val session = response.str("PlaySessionId").also { if (it.isBlank()) serviceError(R.string.player_err_server_incomplete) }
        val streams = source.objects("MediaStreams")
        fun tracks(type: String) = streams.filter { it.str("Type") == type }.mapNotNull {
            val index = it.num("Index")?.toInt() ?: return@mapNotNull null
            val label = it.str("DisplayTitle").ifBlank { it.str("Language").ifBlank { "$type ${index + 1}" } }
            val language = it.str("Language").takeIf(String::isNotBlank)
            val codec = it.str("Codec").takeIf(String::isNotBlank)
            val forced = it.flag("IsForced")
            PlaybackTrack(index, label, language, it.flag("IsTextSubtitleStream") || codec in
                setOf("srt", "subrip", "ass", "ssa", "webvtt", "vtt", "mov_text"),
                trackKey(label, language, codec, forced))
        }
        val subtitles = tracks("Subtitle")
        val serverSubtitle = source.num("DefaultSubtitleStreamIndex")?.toInt() ?: -1
        val selectedSubtitle = subtitle ?: preferredSubtitleIndex(subtitles.map { track ->
            MediaTrack(track.index, track.label, track.language, track.index == serverSubtitle,
                streams.firstOrNull { it.num("Index")?.toInt() == track.index }?.flag("IsForced") == true)
        }, preferredLanguage, fallbackLanguage, serverSubtitle)
        val selectedAudio = audio ?: source.num("DefaultAudioStreamIndex")?.toInt() ?: tracks("Audio").firstOrNull()?.index
        // The server must negotiate the selected language too, especially for burnt-in subtitles.
        if (subtitle == null && selectedSubtitle != serverSubtitle) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, compatibility, mediaSourceId,
                preferredLanguage, fallbackLanguage)
        }
        val subtitleTrack = subtitles.firstOrNull { it.index == selectedSubtitle }
        val directStream = originalDirectStreamUrl(source)
        val direct = (source.flag("SupportsDirectPlay") || directStream != null) && compatibility == PlaybackCompatibility.DIRECT
        // A rejected audio route must not turn a supported H.264/HEVC picture into a full transcode.
        if (compatibility != PlaybackCompatibility.FULL && !videoSupported(source)) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, PlaybackCompatibility.FULL,
                mediaSourceId, preferredLanguage, fallbackLanguage)
        }
        if (direct && !sourceSupported(source, selectedAudio)) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, PlaybackCompatibility.AUDIO_ONLY,
                mediaSourceId, preferredLanguage, fallbackLanguage)
        }
        // Image subtitles need burn-in; re-negotiate explicitly instead of silently dropping them.
        if (direct && subtitleTrack != null && !subtitleTrack.isText) {
            return prepare(c, user, item, bitrate, selectedAudio, selectedSubtitle, PlaybackCompatibility.FULL,
                mediaSourceId, preferredLanguage, fallbackLanguage)
        }
        val url = if (direct) directStream ?: "Videos/${enc(item.id)}/stream?static=true&MediaSourceId=${enc(mediaSourceId)}&PlaySessionId=${enc(session)}" else
            source.str("TranscodingUrl").also { if (it.isBlank()) serviceError(R.string.player_err_cannot_adapt) }
        val subtitleUrl = subtitleTrack?.takeIf { it.isText }?.let {
            safePlaybackUrl(c.baseUrl, "Videos/${enc(item.id)}/${enc(mediaSourceId)}/Subtitles/${it.index}/Stream.vtt")
        }
        return PlaybackPlan(item, mediaSourceId, session, safePlaybackUrl(c.baseUrl, url),
            playbackModeFor(source, direct), tracks("Audio"), subtitles,
            selectedAudio, selectedSubtitle, subtitleUrl, playbackTranscodeReasons(source), compatibility,
            if (direct) sourceVideoFrameRate(source) else 0f)
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
            put("IsPaused", paused); put("CanSeek", true)
            // The server's own dashboard shows this. Reporting a remux as a full transcode made
            // Spole look like the heaviest client on the server when it was the lightest.
            put("PlayMethod", when (plan.mode) {
                PlaybackMode.DIRECT_PLAY -> "DirectPlay"
                PlaybackMode.DIRECT_STREAM -> "DirectStream"
                else -> "Transcode"
            })
            if (c.kind == ServiceKind.EMBY && event == "/Progress") put("EventName", "TimeUpdate")
            plan.audioIndex?.let { put("AudioStreamIndex", it) }; put("SubtitleStreamIndex", plan.subtitleIndex)
        })
    }

    /**
     * What the server says it is doing, rather than what its URL implies.
     *
     * [playbackModeFor] reads `VideoCodec=copy` out of the transcoding URL, which is how Jellyfin
     * spells a picture it is copying. Emby does not spell it that way: it writes the target codec
     * into the URL whether it re-encodes the picture or passes it through untouched, so every Emby
     * session was labelled a full transcode — on the overlay and in the `PlayMethod` this client
     * reports back to the server's own dashboard.
     *
     * The session carries `TranscodingInfo` with `IsVideoDirect` and `IsAudioDirect`, which both
     * servers set from the decision they actually made. Null means the server has not answered, is
     * not willing to (a user without session access), or has not opened the session yet — in every
     * one of those cases the caller keeps what it had rather than showing a guess as a correction.
     */
    fun serverPlaybackMode(c: ServiceConnection, sourceId: String): PlaybackMode? {
        val sessions = runCatching {
            readArray(c, "Sessions?DeviceId=${enc(deviceId)}")
        }.getOrNull() ?: return null
        val ours = sessions.firstOrNull { it.str("DeviceId") == deviceId && it.obj("PlayState").str("MediaSourceId") == sourceId }
            ?: sessions.singleOrNull { it.str("DeviceId") == deviceId && it["NowPlayingItem"] != null }
            ?: return null
        val info = ours["TranscodingInfo"] as? JsonObject ?: return null
        // An older server that leaves both flags out cannot answer the question; do not read their
        // absence as "nothing is being copied".
        if (info["IsVideoDirect"] == null && info["IsAudioDirect"] == null) return null
        return when {
            info.flag("IsVideoDirect") && info.flag("IsAudioDirect") -> PlaybackMode.DIRECT_STREAM
            info.flag("IsVideoDirect") -> PlaybackMode.AUDIO_TRANSCODE
            else -> PlaybackMode.FULL_TRANSCODE
        }
    }

    private fun read(c: ServiceConnection, path: String) = decode(playbackRequest { transport.get(EndpointValidator.resolve(c.baseUrl, path), headers(c)) })
    private fun readArray(c: ServiceConnection, path: String): List<JsonObject> {
        val response = playbackRequest { transport.get(EndpointValidator.resolve(c.baseUrl, path), headers(c)) }
        decodeStatus(response)
        if (response.body.isBlank()) return emptyList()
        return (Json.parseToJsonElement(response.body) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    }
    private fun post(c: ServiceConnection, path: String, body: JsonObject) = decode(transport.post(EndpointValidator.resolve(c.baseUrl, path), headers(c), body.toString()))
    private fun decode(response: HttpResponse): JsonObject {
        decodeStatus(response)
        return if (response.body.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(response.body).jsonObject
    }
    private fun decodeStatus(response: HttpResponse) {
        when (response.statusCode) {
            in 200..299 -> Unit
            401, 403 -> serviceError(R.string.player_err_refused)
            404 -> serviceError(R.string.player_err_not_available)
            in 300..399 -> serviceError(R.string.player_err_moved)
            else -> serviceError(R.string.player_err_server_failed)
        }
    }
}

/** Retains source compatibility for existing callers while both servers share playback. */
typealias JellyfinPlaybackClient = MediaPlaybackClient

internal fun playbackProfilePath(connection: ServiceConnection): String = when (connection.kind) {
    ServiceKind.JELLYFIN -> "Users/Me"
    ServiceKind.EMBY -> {
        if (connection.userId.isBlank()) serviceError(R.string.player_err_emby_sign_in)
        "Users/${enc(connection.userId)}"
    }
    else -> serviceError(R.string.player_err_service_no_playback)
}

internal fun embyPlaybackSegments(item: JsonObject): List<PlaybackSegment> {
    val chapters = item.objects("Chapters")
    fun marker(type: String) = chapters.firstOrNull { it.str("MarkerType") == type }?.num("StartPositionTicks")?.div(10_000)
    val duration = item.num("RunTimeTicks")?.div(10_000)
    return listOfNotNull(
        marker("IntroStart")?.let { start -> marker("IntroEnd")?.let { PlaybackSegment(PlaybackSegment.Kind.INTRO, start, it) } },
        marker("CreditsStart")?.let { start -> duration?.let { PlaybackSegment(PlaybackSegment.Kind.OUTRO, start, it) } },
    ).filter { it.startMs >= 0 && it.endMs > it.startMs && (duration == null || it.endMs <= duration) }
}

fun playbackTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}
