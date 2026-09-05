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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.reelstack.MainActivity
import app.reelstack.data.model.TrackedRequest

object LibraryNotifications {
    private const val CHANNEL = "library-ready"
    fun allowed(context: Context): Boolean = (Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun show(context: Context, scope: String, item: TrackedRequest,
             stillEligible: () -> Boolean = { true }, artworkLoader: (String?) -> Bitmap? = ::notificationArtwork): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Klart i biblioteket", NotificationManager.IMPORTANCE_DEFAULT))
        if (!allowed(context) || manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        val intent = Intent(context, MainActivity::class.java).putExtra("open_requests", true)
        val pending = PendingIntent.getActivity(context, (scope + item.key).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val subtitle = if (item.seasons.isEmpty()) "Filmen er klar til å sjå." else
            "Sesong ${item.seasons.sorted().joinToString(", ")} er klar til å sjå."
        // Artwork is optional. Never attach server credentials or follow an image redirect.
        val picture = artworkLoader(item.artworkUrl)
        if (!stillEligible()) return false
        return runCatching {
            val builder = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(app.reelstack.R.drawable.ic_notification_library)
                .setContentTitle("${item.title} · i biblioteket").setContentText(subtitle)
                .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            if (picture != null) builder.setLargeIcon(picture).setStyle(NotificationCompat.BigPictureStyle()
                .bigPicture(picture).setSummaryText(subtitle))
            manager.notify((scope + item.key).hashCode(), builder.build())
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
