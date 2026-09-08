package app.reelstack

import android.app.Application
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.diagnostics.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReelstackApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // Reading the preference touches disk, and scheduling talks to WorkManager's database.
        // Neither belongs on the main thread during startup: the first frame should not wait on
        // a decision about a refresh that will not run for another half hour anyway.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            BackgroundRefreshScheduler.schedule(
                this@ReelstackApplication,
                container.preferencesRepository.wifiOnly,
            )
        }
    }
}
