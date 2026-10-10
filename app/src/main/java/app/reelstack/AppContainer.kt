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
    val serverDiscovery by lazy {
        app.reelstack.data.network.LanServerDiscovery({ app.reelstack.data.network.lanBroadcastTargets(appContext) })
    }
    val serverProbe = app.reelstack.data.network.ServerProbe()
    val jellyfinAuthenticationClient = JellyfinAuthenticationClient(
        deviceId = deviceId,
    )
    val preferencesRepository = AppPreferencesRepository(appContext)
    val calendarFollowStore = app.reelstack.data.repository.CalendarFollowStore(appContext)
    /** Smart shelves' last answers, so a tile, a Home row or a shelf page is drawn at once. Cleared with the feed cache. */
    val shelfAnswerCache = app.reelstack.data.network.FileShelfAnswerCache(java.io.File(appContext.cacheDir, "smart-shelves"))
    val mediaServerClient = app.reelstack.data.network.MediaServerClient(deviceId = deviceId, includeLibrary = preferencesRepository::includesLibrary,
        // Shelf questions read a whole catalogue; a Home row's nine seconds is too short for some servers.
        patientTransport = app.reelstack.data.network.HttpTransport(connectTimeoutMs = 7_000, readTimeoutMs = 40_000),
        shelfCache = shelfAnswerCache)
    // The repository formats dates; it should do so in the language the app is set to, which is not
    // always the device's.
    val mediaSyncRepository = MediaSyncRepository(
        locale = androidx.core.os.ConfigurationCompat.getLocales(
            app.reelstack.localization.AppLanguages.wrap(appContext).resources.configuration,
        )[0] ?: java.util.Locale.getDefault(),
        use24HourClock = android.text.format.DateFormat.is24HourFormat(appContext),
        words = { it.text(appContext) },
        mediaServerClient = mediaServerClient,
        calendarClient = app.reelstack.data.network.PersonalCalendarClient(metadataStore =
            app.reelstack.data.repository.cache.CacheDatabase.get(appContext).catalogueStore()),
        seerrReleaseClient = app.reelstack.data.network.SeerrReleaseClient(metadataStore =
            app.reelstack.data.repository.cache.CacheDatabase.get(appContext).catalogueStore()),
        calendarSelectionProvider = { profileId, connection, userId ->
            calendarFollowStore.read(calendarFollowStore.scope(profileId, connection, userId))
        },
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
            // Home reads the libraries chosen for its rows, not the ones listed in the Library tab.
            preferencesRepository.homeLibrariesFingerprint(connections) +
            "|profile=" + connectionRepository.activeProfileId +
            "|lang=" + app.reelstack.localization.AppLanguages.selected(appContext).tag +
            "|calendar=" + calendarFollowStore.revision

    /** Home's library choice for each media server that has an address in [connections]. */
    fun homeLibraries(connections: List<app.reelstack.data.model.ServiceConnection>) =
        connections.filter { it.kind in app.reelstack.data.model.HOME_MEDIA_SOURCES && it.baseUrl.isNotBlank() }
            .associate { it.kind to preferencesRepository.homeLibraries(it) }

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
