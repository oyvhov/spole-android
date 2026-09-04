package app.reelstack.data.model

enum class ServiceKind(val displayName: String, val role: String) {
    JELLYFIN("Jellyfin", "Medietenar"),
    EMBY("Emby", "Medietenar"),
    SEERR("Seerr", "Bestillingar"),
    RADARR("Radarr", "Filmar"),
    SONARR("Sonarr", "Seriar"),
}

enum class ConnectionState {
    DEMO,
    TESTING,
    CONNECTED,
    ERROR,
}

enum class HomeSection {
    NOW_PLAYING,
    RECENT_MOVIES,
    RECENT_SERIES,
    UPCOMING,
    DOWNLOADS,
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
) {
    val key: String
        get() = "${source?.name ?: "DEMO"}:${sessionId ?: "$userName:$deviceName:$title"}"
}

data class LibraryMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val progress: Float? = null,
    val artworkRes: Int,
    val source: ServiceKind,
    val artworkUrl: String? = null,
)

data class UpcomingMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val airDateEpochMillis: Long,
    val artworkRes: Int,
    val source: ServiceKind,
    val artworkUrl: String? = null,
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
    val artworkRes: Int? = null,
    val artworkUrl: String? = null,
)

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
)
