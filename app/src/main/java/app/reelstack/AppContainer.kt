package app.reelstack

import android.content.Context
import android.provider.Settings
import app.reelstack.data.network.ServiceConnectionTester
import app.reelstack.data.network.JellyfinAuthenticationClient
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.MediaSnapshotStore
import app.reelstack.data.repository.AppPreferencesRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val deviceId = Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.ANDROID_ID,
    ) ?: "homereel-android"
    val connectionRepository = ConnectionRepository(appContext)
    val accountProfileClient = app.reelstack.data.network.AccountProfileClient(deviceId = deviceId)
    val connectionTester = ServiceConnectionTester(deviceId = deviceId)
    val seerrAuthenticationClient = app.reelstack.data.network.SeerrAuthenticationClient()
    val embyAuthenticationClient = app.reelstack.data.network.EmbyAuthenticationClient(deviceId = deviceId)
    val jellyfinAuthenticationClient = JellyfinAuthenticationClient(
        deviceId = deviceId,
    )
    val mediaSyncRepository = MediaSyncRepository(
        mediaServerClient = app.reelstack.data.network.MediaServerClient(deviceId = deviceId),
    )
    val mediaSnapshotStore = MediaSnapshotStore(appContext)
    val preferencesRepository = AppPreferencesRepository(appContext)
    val requestTrackingRepository = app.reelstack.data.repository.RequestTrackingRepository(appContext)
}
