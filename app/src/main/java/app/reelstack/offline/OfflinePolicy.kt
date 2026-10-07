package app.reelstack.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

internal fun offlineNetworkAllowed(wifiOnly: Boolean, wifi: Boolean, ethernet: Boolean, cellular: Boolean): Boolean =
    !wifiOnly || (wifi || ethernet) && !cellular

internal fun offlineNetworkAllowed(context: Context, wifiOnly: Boolean): Boolean {
    if (!wifiOnly) return true
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return offlineNetworkAllowed(true, capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET), capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
}

/** Applies to the actual shared Media3 cache, never evicts a film the user chose to keep. */
internal object OfflineSpacePolicy {
    const val RESERVE_BYTES = 512L * 1024 * 1024
    const val MAX_CACHE_BYTES = 32L * 1024 * 1024 * 1024
    fun allows(cacheBytes: Long, availableBytes: Long, totalBytes: Long, additionalBytes: Long = 0): Boolean {
        val budget = minOf(MAX_CACHE_BYTES, totalBytes - totalBytes / 5)
        return additionalBytes >= 0 && cacheBytes <= budget && additionalBytes <= budget - cacheBytes &&
            availableBytes >= RESERVE_BYTES && additionalBytes <= availableBytes - RESERVE_BYTES
    }
}
