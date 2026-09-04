package app.reelstack

import android.content.Context
import app.reelstack.data.network.ServiceConnectionTester
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.MediaSyncRepository
import app.reelstack.data.repository.MediaSnapshotStore
import app.reelstack.data.repository.AppPreferencesRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val connectionRepository = ConnectionRepository(appContext)
    val connectionTester = ServiceConnectionTester()
    val mediaSyncRepository = MediaSyncRepository()
    val mediaSnapshotStore = MediaSnapshotStore(appContext)
    val preferencesRepository = AppPreferencesRepository(appContext)
}
