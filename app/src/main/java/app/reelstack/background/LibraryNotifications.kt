package app.reelstack.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import app.reelstack.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.reelstack.MainActivity
import app.reelstack.data.model.TrackedRequest

/**
 * Each kind of update is its own Android channel, so "it is ready to watch" and "the download
 * failed" can be tuned — or silenced — separately in system settings. A single channel forced one
 * choice on both, and turning off a failure you did not care about also lost the ready alert.
 */
enum class NotificationEvent(val channelId: String, @StringRes val channelName: Int, val importance: Int) {
    READY("library-ready", R.string.notification_channel_ready, NotificationManager.IMPORTANCE_DEFAULT),
    DOWNLOADING("request-downloading", R.string.notification_channel_downloading, NotificationManager.IMPORTANCE_LOW),
    FAILED("request-failed", R.string.notification_channel_failed, NotificationManager.IMPORTANCE_DEFAULT),
}

object LibraryNotifications {
    fun allowed(context: Context): Boolean = (Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Registers every channel up front so they are visible in system settings before the first alert. */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationEvent.entries.forEach { event ->
            manager.createNotificationChannel(
                NotificationChannel(event.channelId, context.getString(event.channelName), event.importance),
            )
        }
    }

    fun show(context: Context, scope: String, item: TrackedRequest,
             stillEligible: () -> Boolean = { true }, artworkLoader: (String?) -> Bitmap? = ::notificationArtwork,
             event: NotificationEvent = NotificationEvent.READY): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = event.channelId
        ensureChannels(context)
        if (!allowed(context) || manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val intent = Intent(context, MainActivity::class.java).putExtra("open_requests", true)
        val pending = PendingIntent.getActivity(context, (scope + item.key + event.name).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val seasons = item.seasons.sorted().joinToString(", ")
        val subtitle = when (event) {
            NotificationEvent.READY ->
                if (item.seasons.isEmpty()) context.getString(R.string.notification_movie_ready)
                else context.getString(R.string.notification_season_ready, seasons)
            NotificationEvent.DOWNLOADING ->
                item.percent?.let { context.getString(R.string.notification_downloading_percent, it) }
                    ?: context.getString(R.string.notification_downloading_started)
            NotificationEvent.FAILED ->
                context.getString(R.string.notification_request_failed)
        }
        // Artwork is optional. Never attach server credentials or follow an image redirect.
        val picture = artworkLoader(item.artworkUrl)
        if (!stillEligible()) return false
        return runCatching {
            val builder = NotificationCompat.Builder(context, channel)
                .setSmallIcon(app.reelstack.R.drawable.ic_notification_library)
                .setContentTitle(
                    context.getString(
                        when (event) {
                            NotificationEvent.READY -> R.string.notification_title_ready
                            NotificationEvent.DOWNLOADING -> R.string.notification_title_downloading
                            NotificationEvent.FAILED -> R.string.notification_title_failed
                        },
                        item.title,
                    ),
                )
                .setContentText(subtitle)
                .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            if (picture != null) builder.setLargeIcon(picture).setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(picture).setSummaryText(subtitle))
            manager.notify((scope + item.key + event.name).hashCode(), builder.build())
            true
        }.getOrDefault(false)
    }

    private fun notificationArtwork(url: String?): Bitmap? = runCatching {
        val uri = java.net.URI(url ?: return null)
        // Seerr posters come from TMDB. Do not fetch arbitrary URLs from a persisted follow.
        require(uri.scheme == "https" && uri.host == "image.tmdb.org" && uri.userInfo == null && uri.port == -1)
        val connection = uri.toURL().openConnection() as java.net.HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 3_000; connection.readTimeout = 3_000
            if (connection.responseCode != 200 || connection.contentLengthLong > 2_097_152) return null
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2_097_152) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            if (bytes.size > 2_097_152) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
                while (bounds.outWidth / inSampleSize > 512 || bounds.outHeight / inSampleSize > 768) inSampleSize *= 2
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } finally { connection.disconnect() }
    }.getOrNull()
}
