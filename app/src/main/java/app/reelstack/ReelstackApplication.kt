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
    override fun newImageLoader(context: coil3.PlatformContext): coil3.ImageLoader {
        // This namespace belonged to token-bearing artwork URLs. It contains only disposable
        // artwork; account storage, progress and offline media live elsewhere.
        val legacy = context.cacheDir.resolve("coil3_image_cache")
        check(legacy.canonicalFile.parentFile == context.cacheDir.canonicalFile)
        if (legacy.exists()) runCatching { legacy.deleteRecursively() }
        return coil3.ImageLoader.Builder(context)
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(
                        context,
                        0.20,
                    )
                    .maxSizeBytes(minOf(32L * 1024 * 1024, (Runtime.getRuntime().maxMemory() * 0.20).toLong()))
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil3_image_cache_v2").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .components {
                add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory = { app.reelstack.data.network.HttpTransport.sharedClient }))
            }
            .build()
    }

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
            // The Wi-Fi-only download rule is read by OfflineDownloadRuntime when its manager is
            // built. Starting the download service from here crashed a process that WorkManager
            // had started in the background.
        }
    }
}
