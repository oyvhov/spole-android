package app.reelstack.data.repository

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class DeviceIdentityTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reelstack_device", 0).edit().clear().commit()
        context.getSharedPreferences("reelstack_connections", 0).edit().clear().commit()
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "legacy-device")
    }
    @Test fun freshInstallGetsAStableRandomId() {
        val id = DeviceIdentity.get(context)
        assertEquals(id, UUID.fromString(id).toString())
        assertEquals(id, DeviceIdentity.get(context))
    }
    @Test fun oldConnectedInstallPreservesItsServerIdentity() {
        context.getSharedPreferences("reelstack_connections", 0).edit().putString("jellyfin.url", "https://media.example").commit()
        assertEquals("legacy-device", DeviceIdentity.get(context))
    }
}
