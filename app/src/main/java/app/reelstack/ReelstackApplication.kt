package app.reelstack

import android.app.Application
import app.reelstack.background.BackgroundRefreshScheduler

class ReelstackApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        BackgroundRefreshScheduler.schedule(this, container.preferencesRepository.wifiOnly)
    }
}
