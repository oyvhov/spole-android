package app.reelstack.cast

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.player.PlayableItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Public Cast load description. It is safe for media custom data, receiver queue data and tests.
 * A route, user id and access token deliberately cannot be added to this type.
 */
data class CastLoadSpec(
    val service: ServiceKind,
    val profileId: String,
    val items: List<CastQueueItem>,
    val startIndex: Int = 0,
) {
    init {
        require(service in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
        require(profileId.length <= 160)
        require(items.isNotEmpty() && items.size <= 100)
        require(startIndex in items.indices)
    }

    fun receiverPayload(): JSONObject = JSONObject().apply {
        put("version", 1)
        put("service", service.name)
        put("profileId", profileId)
        put("startIndex", startIndex)
        put("items", JSONArray(items.map(CastQueueItem::receiverPayload)))
    }

    override fun toString(): String = "CastLoadSpec(service=${service.name}, items=${items.size}, startIndex=$startIndex)"
}

data class CastQueueItem(
    val itemId: String,
    val title: String,
    val subtitle: String = "",
    val artworkPath: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val sourceId: String? = null,
    val resumePositionMs: Long = 0,
) {
    init {
        require(itemId.isNotBlank() && itemId.length <= 512)
        require(title.isNotBlank() && title.length <= 512)
        require(resumePositionMs >= 0)
        require(audioStreamIndex == null || audioStreamIndex >= 0)
        require(subtitleStreamIndex == null || subtitleStreamIndex >= 0)
    }

    fun receiverPayload(): JSONObject = JSONObject().apply {
        put("itemId", itemId); put("title", title); put("resumePositionMs", resumePositionMs)
        subtitle.takeIf(String::isNotBlank)?.let { put("subtitle", it) }
        artworkPath?.let { put("artworkPath", it) }
        audioStreamIndex?.let { put("audioStreamIndex", it) }
        subtitleStreamIndex?.let { put("subtitleStreamIndex", it) }
        sourceId?.let { put("sourceId", it) }
    }
}

/**
 * In-memory only. This serializes exclusively into the Google Cast load-credentials field.
 * Never retain it in state, a URI, a queue item, a log line or SharedPreferences.
 */
data class CastCredentialEnvelope(
    val service: ServiceKind,
    val baseUrl: String,
    val alternateUrl: String,
    val userId: String,
    val accessToken: String,
    val deviceId: String,
) {
    init {
        require(service in setOf(ServiceKind.JELLYFIN, ServiceKind.EMBY))
        require(baseUrl.isNotBlank() && userId.isNotBlank() && accessToken.isNotBlank() && deviceId.isNotBlank())
    }

    fun loadCredentials(): String = JSONObject().apply {
        put("version", 1); put("service", service.name); put("baseUrl", baseUrl)
        alternateUrl.takeIf(String::isNotBlank)?.let { put("alternateUrl", it) }
        put("userId", userId); put("accessToken", accessToken); put("deviceId", deviceId)
    }.toString()

    override fun toString(): String = "CastCredentialEnvelope(service=${service.name}, redacted=true)"

    companion object {
        fun from(connection: ServiceConnection, deviceId: String) = CastCredentialEnvelope(
            service = connection.kind,
            baseUrl = connection.baseUrl,
            alternateUrl = connection.alternateUrl,
            userId = connection.userId,
            accessToken = connection.token,
            deviceId = deviceId,
        )
    }
}

data class CastLoadRequest(val spec: CastLoadSpec, val credentials: CastCredentialEnvelope) {
    init { require(spec.service == credentials.service) }
    override fun toString(): String = "CastLoadRequest($spec, credentials=redacted)"
}

/** The one supported queue action: the selected episode followed by the same season in order. */
fun restOfSeasonSpec(
    connection: ServiceConnection,
    profileId: String,
    selected: PlayableItem,
    followingEpisodes: List<PlayableItem>,
    audio: Int? = null,
    subtitle: Int? = null,
    sourceId: String? = null,
): CastLoadSpec = CastLoadSpec(
    service = connection.kind,
    profileId = profileId,
    items = (listOf(selected) + followingEpisodes.filter { episode ->
        episode.type == "Episode" && episode.seriesId == selected.seriesId && episode.season == selected.season
    }).distinctBy(PlayableItem::id).mapIndexed { index, item ->
        CastQueueItem(item.id, item.title, item.subtitle, audioStreamIndex = if (index == 0) audio else null,
            subtitleStreamIndex = if (index == 0) subtitle else null, sourceId = if (index == 0) sourceId else null,
            resumePositionMs = if (index == 0) item.resumeMs else 0)
    },
)
