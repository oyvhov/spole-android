package app.reelstack

import android.app.Application
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
     * Two things matter on a television. The first is the memory cache: a wall of covers is the
     * whole screen, and re-decoding a poster because it fell out of a small cache is the stutter
     * people feel when they scroll back up. The second is that Spole now puts Jellyfin's own
     * content tag in every artwork address, so the URL *is* the content hash — a replaced poster
     * gets a different address. That makes the server's cache headers irrelevant and lets the disk
     * cache keep a picture for as long as there is room, rather than revalidating it over the
     * network every time a list scrolls past.
     */
    override fun newImageLoader(context: coil3.PlatformContext): coil3.ImageLoader =
        coil3.ImageLoader.Builder(context)
            .memoryCache {
                coil3.memory.MemoryCache.Builder()
                    .maxSizePercent(context, 0.30)
                    .build()
            }
            .diskCache {
                coil3.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork").toOkioPath())
                    .maxSizeBytes(192L * 1024 * 1024)
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
            BackgroundRefreshScheduler.schedule(
                this@ReelstackApplication,
                container.preferencesRepository.wifiOnly,
            )
        }
    }
}
