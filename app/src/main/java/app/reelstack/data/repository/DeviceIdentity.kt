package app.reelstack.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import java.util.UUID

/**
 * The device name Spole gives itself when it talks to Jellyfin and Emby.
 *
 * This used to be `Settings.Secure.ANDROID_ID`, which is a durable hardware-scoped identifier:
 * it survives uninstalls, is shared with every app signed by the same key, and has to be declared
 * as a device identifier in a store listing. Nothing here needs any of that. A random value made
 * once per installation identifies the device to the user's own server just as well, and it
 * disappears when the app does.
 *
 * Existing installations keep the identity they already have. Jellyfin registers a device against
 * the id in the `Authorization` header, so handing it a fresh value on upgrade would leave a
 * duplicate entry in the server's device list for everyone who already signed in.
 */
object DeviceIdentity {
    fun get(context: Context): String {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        preferences.getString(KEY, null)?.takeIf(String::isNotBlank)?.let { return it }

        val identity = legacyIdentity(context) ?: UUID.randomUUID().toString()
        preferences.edit { putString(KEY, identity) }
        return identity
    }

    /**
     * Read once, on the first launch after upgrading, and then never again — the value is copied
     * into our own storage above. A new installation never reaches this.
     */
    @SuppressLint("HardwareIds")
    private fun legacyIdentity(context: Context): String? =
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.takeIf { it.isNotBlank() && it != "9774d56d682e549c" }

    private const val PREFERENCES_NAME = "reelstack_device"
    private const val KEY = "device_id"
}
