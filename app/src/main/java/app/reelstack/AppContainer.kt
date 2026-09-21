package app.reelstack

import android.content.Context
import app.reelstack.data.network.HttpTransport
import app.reelstack.data.network.ServiceConnectionTester
import app.reelstack.data.network.JellyfinAuthenticationClient
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.MediaSnapshotStore
import app.reelstack.data.repository.AppPreferencesRepository
import app.reelstack.data.repository.DeviceIdentity

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val deviceId = DeviceIdentity.get(appContext)
    val connectionRepository = ConnectionRepository(appContext)
    val pinSecurity = app.reelstack.data.security.PinSecurity(appContext)
    val accountProfileClient = app.reelstack.data.network.AccountProfileClient(deviceId = deviceId)
    val connectionTester = ServiceConnectionTester(deviceId = deviceId)
    val seerrAuthenticationClient = app.reelstack.data.network.SeerrAuthenticationClient()
    val embyAuthenticationClient = app.reelstack.data.network.EmbyAuthenticationClient(deviceId = deviceId)
    val embyConnectClient = app.reelstack.data.network.EmbyConnectClient()
    val jellyfinAuthenticationClient = JellyfinAuthenticationClient(
        deviceId = deviceId,
    )
    val preferencesRepository = AppPreferencesRepository(appContext)
    val mediaServerClient = app.reelstack.data.network.MediaServerClient(deviceId = deviceId, includeLibrary = preferencesRepository::includesLibrary)
    // The repository formats dates; it should do so in the language the app is set to, which is not
    // always the device's.
    val mediaSyncRepository = MediaSyncRepository(
        locale = androidx.core.os.ConfigurationCompat.getLocales(
            app.reelstack.localization.AppLanguages.wrap(appContext).resources.configuration,
        )[0] ?: java.util.Locale.getDefault(),
        use24HourClock = android.text.format.DateFormat.is24HourFormat(appContext),
        words = { it.text(appContext) },
        mediaServerClient = mediaServerClient,
    )
    val sessionSocket = app.reelstack.data.network.JellyfinSessionSocket(deviceId = deviceId)
    val mediaSnapshotStore = MediaSnapshotStore(appContext)
    val localPlaybackStore = app.reelstack.data.repository.LocalPlaybackStore(appContext)
    /** Per-profile, app-private offline tree. The network downloader is allowed no other target. */
    val offlineStorage by lazy { app.reelstack.offline.OfflineStorage(appContext) }
    val offlineDownloads by lazy { app.reelstack.offline.OfflineDownloadRepository(appContext) }
    val watchNextSync by lazy { app.reelstack.player.WatchNextSync(this) }
    /**
     * What the cached rows belong to.
     *
     * The language is part of it, because the rows hold text: "Film · 2024" is written when the
     * row is cached, not when it is drawn. Switching language changes the fingerprint, the old rows
     * stop matching, and the next sync writes them in the new language — instead of a home screen
     * that stays half-nynorsk until something happens to refresh it.
     */
    fun mediaFingerprint(connections: List<app.reelstack.data.model.ServiceConnection>): String =
        MediaSnapshotStore.fingerprint(connections) +
            preferencesRepository.librarySelectionFingerprint(connections) +
            "|profile=" + connectionRepository.activeProfileId +
            "|lang=" + app.reelstack.localization.AppLanguages.selected(appContext).tag

    val requestTrackingRepository = app.reelstack.data.repository.RequestTrackingRepository(appContext)
    val requestHistoryRepository = app.reelstack.data.repository.RequestHistoryRepository()
    val requestRulesClient = app.reelstack.data.network.RequestRulesClient()

    /**
     * The widget runs inside a broadcast receiver, which has seconds rather than the app's
     * patience. These two clients make the same calls against a transport that gives up early, so
     * an unreachable server stops occupying a thread long after the widget already drew "Fekk
     * ikkje kontakt".
     */
    val widgetMediaServerClient = app.reelstack.data.network.MediaServerClient(
        transport = HttpTransport(connectTimeoutMs = 2_500, readTimeoutMs = 3_500),
        deviceId = deviceId,
        includeLibrary = preferencesRepository::includesLibrary,
    )
    val widgetAccountProfileClient = app.reelstack.data.network.AccountProfileClient(
        transport = HttpTransport(connectTimeoutMs = 2_500, readTimeoutMs = 3_500),
        deviceId = deviceId,
    )
}
