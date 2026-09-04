package app.reelstack.data.model

enum class ServiceKind(val displayName: String, val role: String) {
    JELLYFIN("Jellyfin", "Media server"),
    EMBY("Emby", "Media server"),
    SEERR("Seerr", "Requests"),
    RADARR("Radarr", "Movies"),
    SONARR("Sonarr", "Series"),
}

enum class ConnectionState {
    DEMO,
    TESTING,
    CONNECTED,
    ERROR,
}

data class ServiceConnection(
    val kind: ServiceKind,
    val name: String,
    val baseUrl: String,
    val token: String = "",
    val userId: String = "",
    val state: ConnectionState = ConnectionState.DEMO,
    val latencyMs: Long? = null,
    val detail: String? = null,
)

data class PlaybackSession(
    val userName: String,
    val deviceName: String,
    val title: String,
    val subtitle: String,
    val progress: Float,
    val timeLeft: String,
    val streamMethod: String,
    val quality: String,
    val paused: Boolean,
    val artworkUrl: String? = null,
    val sessionId: String? = null,
    val source: ServiceKind? = null,
)

data class LibraryMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float? = null,
    val artworkRes: Int,
    val source: ServiceKind,
)

enum class IncomingState {
    DOWNLOADING,
    REQUESTED,
    READY,
}

data class IncomingMedia(
    val id: String,
    val title: String,
    val source: ServiceKind,
    val status: String,
    val state: IncomingState,
    val artworkRes: Int,
    val artworkUrl: String? = null,
)

data class DiscoverMedia(
    val id: String,
    val title: String,
    val metadata: String,
    val artworkRes: Int,
    val inLibrary: Boolean,
    val requested: Boolean = false,
    val artworkUrl: String? = null,
    val remoteId: Int? = null,
    val mediaType: String? = null,
)

data class ActivityEvent(
    val id: String,
    val title: String,
    val detail: String,
    val time: String,
    val progress: Int? = null,
    val complete: Boolean = false,
    val source: ServiceKind? = null,
)

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
)
