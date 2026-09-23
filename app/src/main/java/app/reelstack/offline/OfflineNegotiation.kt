package app.reelstack.offline

import android.content.Context
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.repository.DeviceIdentity
import app.reelstack.player.AndroidPlaybackCapabilities
import app.reelstack.player.MediaPlaybackClient

/**
 * A download is stored untouched, so it is not limited by a streaming bitrate. The negotiation
 * still goes through PlaybackInfo, because only the server can say whether the original file may
 * be read directly.
 */
internal const val OFFLINE_NEGOTIATION_BITRATE = 1_000_000_000

/**
 * The client a download negotiates with: the same device capabilities, decoder checks and bundled
 * FFmpeg audio that decide online direct play. The default conservative profile only allows
 * H.264 with stereo AAC/MP3, so almost every real film or episode was refused as "not direct".
 */
internal fun offlineNegotiationClient(context: Context): MediaPlaybackClient {
    val capabilities = AndroidPlaybackCapabilities(context.applicationContext)
    return MediaPlaybackClient(
        deviceId = DeviceIdentity.get(context.applicationContext),
        capabilities = capabilities::snapshot,
        sourceSupported = capabilities::canDirectPlay,
        videoSupported = capabilities::canDecodeVideo,
    )
}

/**
 * Asks the server for the untouched file behind [remoteId] as this account.
 *
 * Subtitles are left out of the negotiation on purpose. The downloaded file keeps every embedded
 * track, and the local player chooses among them. Asking for an image subtitle made the server
 * plan a burn-in transcode, which turned an ordinary Blu-ray rip into a refusal.
 */
internal fun negotiateOfflineDownload(
    client: MediaPlaybackClient,
    profileId: String,
    connection: ServiceConnection,
    remoteId: String,
    audioIndex: Int? = null,
    versionId: String? = null,
): OfflineDownloadRequest {
    val userId = client.verify(connection)
    val item = client.item(connection, userId, remoteId)
    val plan = client.prepare(connection, userId, item, bitrate = OFFLINE_NEGOTIATION_BITRATE,
        audio = audioIndex, subtitle = -1, sourceId = versionId)
    return OfflineDownloadRequest(
        profileId = profileId,
        connection = connection,
        candidate = OfflineMediaCandidate(
            itemId = item.id,
            service = connection.kind,
            requiresTranscode = !plan.direct,
            directDownloadUrl = plan.url,
        ),
        title = item.title,
        subtitle = item.subtitle,
        mediaType = item.type,
    )
}
