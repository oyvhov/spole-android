package app.reelstack.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

/**
 * Hands a title to the media server's own Android client when one is installed, and falls back to
 * the browser when it is not.
 *
 * "Opne i Jellyfin" used to be a plain `ACTION_VIEW` on the server's web URL, which in practice
 * always landed in a browser: the app that can actually play the title is only reachable if it has
 * claimed that URL, and Android will not reveal such a handler unless the manifest asks (see the
 * `<queries>` block). Resolving the handler ourselves means we can say which app we are opening,
 * and launch it explicitly instead of hoping a chooser appears.
 */
object NativeClientLauncher {

    data class Target(val packageName: String?, val label: String)

    /**
     * The non-browser app that handles [url], if any. A browser is identified by also handling a
     * bare scheme-only intent, which is what every browser registers and no media client does.
     */
    fun resolve(context: Context, url: String): Target {
        val handlers = runCatching { nonBrowserHandlers(context, url) }.getOrDefault(emptyList())
        val packageName = handlers.firstOrNull() ?: return Target(null, "Opne i nettlesaren")
        val label = runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return Target(packageName, label?.let { "Opne i $it" } ?: "Opne i appen")
    }

    /** Returns false when nothing could handle the link, so the caller can say so. */
    fun open(context: Context, url: String, packageName: String?): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        packageName?.let(intent::setPackage)
        context.startActivity(intent)
        true
    }.getOrElse {
        // An app can be uninstalled between resolving and tapping; the browser is still there.
        if (packageName == null) false
        else runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

    private fun nonBrowserHandlers(context: Context, url: String): List<String> {
        val manager = context.packageManager
        val uri = url.toUri()
        val scheme = uri.scheme ?: return emptyList()
        val browsers = manager
            .queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.fromParts(scheme, "", null)), PackageManager.MATCH_ALL)
            .mapTo(mutableSetOf()) { it.activityInfo.packageName }
        return manager.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .filter { it !in browsers && it != context.packageName }
            .distinct()
    }
}
