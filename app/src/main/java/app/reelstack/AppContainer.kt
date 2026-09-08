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
    val accountProfileClient = app.reelstack.data.network.AccountProfileClient(deviceId = deviceId)
    val connectionTester = ServiceConnectionTester(deviceId = deviceId)
    val seerrAuthenticationClient = app.reelstack.data.network.SeerrAuthenticationClient()
    val embyAuthenticationClient = app.reelstack.data.network.EmbyAuthenticationClient(deviceId = deviceId)
    val jellyfinAuthenticationClient = JellyfinAuthenticationClient(
        deviceId = deviceId,
    )
    val mediaServerClient = app.reelstack.data.network.MediaServerClient(deviceId = deviceId)
    val mediaSyncRepository = MediaSyncRepository(mediaServerClient = mediaServerClient)
    val mediaSnapshotStore = MediaSnapshotStore(appContext)
    val preferencesRepository = AppPreferencesRepository(appContext)
    val requestTrackingRepository = app.reelstack.data.repository.RequestTrackingRepository(appContext)

    /**
     * The widget runs inside a broadcast receiver, which has seconds rather than the app's
     * patience. These two clients make the same calls against a transport that gives up early, so
     * an unreachable server stops occupying a thread long after the widget already drew "Fekk
     * ikkje kontakt".
     */
    val widgetMediaServerClient = app.reelstack.data.network.MediaServerClient(
        transport = HttpTransport(connectTimeoutMs = 2_500, readTimeoutMs = 3_500),
        deviceId = deviceId,
    )
    val widgetAccountProfileClient = app.reelstack.data.network.AccountProfileClient(
        transport = HttpTransport(connectTimeoutMs = 2_500, readTimeoutMs = 3_500),
        deviceId = deviceId,
    )
}
