package app.reelstack

import android.app.Application
import android.content.res.Configuration
import okio.Path.Companion.toOkioPath
import app.reelstack.background.BackgroundRefreshScheduler
import app.reelstack.diagnostics.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReelstackApplication : Application(), coil3.SingletonImageLoader.Factory {
    val container by lazy { AppContainer(this) }

    /**
     * One tuned image loader instead of the library's defaults.
     *
     * On TV, fast card navigation stresses both decode and network. A larger memory cache keeps
     * recently used covers warm, a larger shared disk cache avoids re-fetching, and cache-control
     * keeps reused images local when the server allows it.
     */
    override fun newImageLoader(context: coil3.PlatformContext): coil3.ImageLoader =
        coil3.ImageLoader.Builder(context)
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(
                        context,
                        if (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION) 0.40 else 0.30,
                    )
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("coil3_image_cache").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory())
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // Reading the preference touches disk, and scheduling talks to WorkManager's database.
        // Neither belongs on the main thread during startup: the first frame should not wait on
        // a decision about a refresh that will not run for another half hour anyway.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.watchNextSync.start()
            BackgroundRefreshScheduler.schedule(
                this@ReelstackApplication,
                container.preferencesRepository.wifiOnly,
            )
        }
    }
}
