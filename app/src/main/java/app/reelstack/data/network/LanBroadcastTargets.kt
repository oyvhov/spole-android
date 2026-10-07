package app.reelstack.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress

/**
 * The broadcast addresses of every local network this device is on, plus 255.255.255.255.
 *
 * Some routers drop the limited broadcast and pass only the subnet's own (`192.168.1.255`), and
 * some Android builds do the opposite, so both are sent. Cellular and VPN networks are skipped:
 * a media server is not on the other side of either, and a probe there only costs time.
 */
fun lanBroadcastTargets(context: Context): List<InetAddress> {
    val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return listOf(LanServerDiscovery.GLOBAL_BROADCAST)
    val subnets = runCatching {
        @Suppress("DEPRECATION")
        connectivity.allNetworks.mapNotNull { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
            val local = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!local || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
            connectivity.getLinkProperties(network)
        }.flatMap { it.linkAddresses }.mapNotNull { link ->
            (link.address as? Inet4Address)?.takeUnless { it.isLoopbackAddress || it.isLinkLocalAddress }
                ?.let { ipv4Broadcast(it, link.prefixLength) }
        }
    }.getOrDefault(emptyList())
    return (subnets + LanServerDiscovery.GLOBAL_BROADCAST).distinct()
}
