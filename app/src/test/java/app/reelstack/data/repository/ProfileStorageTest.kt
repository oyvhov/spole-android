package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ConnectionState
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.security.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

class InMemoryTokenStore : TokenStore {
    val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) {
        if (value.isBlank()) map.remove(key) else map[key] = value
    }
    override fun get(key: String): String? = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun hasStoredValue(key: String): Boolean = map.containsKey(key)
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProfileStorageTest {
    private lateinit var context: Context
    private lateinit var tokenStore: InMemoryTokenStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE).edit().clear().commit()
        tokenStore = InMemoryTokenStore()
    }

    @Test
    fun `empty profileId produces exact legacy keys for backward compatibility`() {
        val repo = ConnectionRepository(context, tokenStore = tokenStore)
        assertEquals("", repo.activeProfileId)
        assertFalse(repo.isKidMode)

        assertEquals("jellyfin", repo.prefixFor(ServiceKind.JELLYFIN, ""))
        assertEquals("emby", repo.prefixFor(ServiceKind.EMBY, ""))
        assertEquals("kid.123.jellyfin", repo.prefixFor(ServiceKind.JELLYFIN, "123"))

        // Write legacy connection with empty profile ID
        val legacyConnection = ServiceConnection(
            kind = ServiceKind.JELLYFIN,
            name = "Stue",
            baseUrl = "http://192.168.1.100:8096",
            token = "secret-token-adult",
            userId = "adult-user-id",
        )
        repo.save(legacyConnection, "")

        // Verify keys directly in SharedPreferences and tokenStore
        val prefs = context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE)
        assertEquals("http://192.168.1.100:8096", prefs.getString("jellyfin.url", null))
        assertEquals("adult-user-id", prefs.getString("jellyfin.user_id", null))
        assertEquals("secret-token-adult", tokenStore.get("jellyfin.token"))

        val read = repo.get(ServiceKind.JELLYFIN, "")
        assertEquals(ConnectionState.CONNECTED, read.state)
        assertEquals("secret-token-adult", read.token)
        assertEquals("adult-user-id", read.userId)
    }

    @Test
    fun `two profiles against same server store distinct tokens and accounts`() {
        val repo = ConnectionRepository(context, tokenStore = tokenStore)

        val adult = ServiceConnection(
            kind = ServiceKind.JELLYFIN,
            name = "Heime",
            baseUrl = "https://jellyfin.example.com",
            token = "adult-token",
            userId = "adult-id",
        )
        repo.save(adult, "")

        val kid = ServiceConnection(
            kind = ServiceKind.JELLYFIN,
            name = "Heime",
            baseUrl = "https://jellyfin.example.com",
            token = "kid-token",
            userId = "kid-id",
        )
        repo.save(kid, "kid-profile-1")

        // Main account still has its token
        val readAdult = repo.get(ServiceKind.JELLYFIN, "")
        assertEquals("adult-token", readAdult.token)
        assertEquals("adult-id", readAdult.userId)

        // Kid account has its token
        val readKid = repo.get(ServiceKind.JELLYFIN, "kid-profile-1")
        assertEquals("kid-token", readKid.token)
        assertEquals("kid-id", readKid.userId)

        // Both use the same baseUrl
        assertEquals(readAdult.baseUrl, readKid.baseUrl)
        assertNotEquals(readAdult.token, readKid.token)
    }

    @Test
    fun `active profile persists and switches cleanly`() {
        var repo = ConnectionRepository(context, tokenStore = tokenStore)
        assertEquals("", repo.activeProfileId)

        repo.activeProfileId = "kid1"
        assertEquals("kid1", repo.activeProfileId)
        assertTrue(repo.isKidMode)

        // Recreate repo to verify persistence
        repo = ConnectionRepository(context, tokenStore = tokenStore)
        assertEquals("kid1", repo.activeProfileId)
        assertTrue(repo.isKidMode)

        repo.activeProfileId = ""
        assertFalse(repo.isKidMode)
    }

    @Test
    fun `deleting a kid profile does not touch the main profile`() {
        val repo = ConnectionRepository(context, tokenStore = tokenStore)
        repo.save(ServiceConnection(ServiceKind.JELLYFIN, "Server", "https://jf.local", "token-adult", "adult"), "")
        repo.save(ServiceConnection(ServiceKind.JELLYFIN, "Server", "https://jf.local", "token-kid", "kid"), "kid-1")
        repo.registerKidProfile("kid-1", "Ola", null)

        val profilesBefore = repo.listProfiles()
        assertEquals(2, profilesBefore.size)

        repo.deleteProfile("kid-1")

        val profilesAfter = repo.listProfiles()
        assertEquals(1, profilesAfter.size)
        assertEquals("", profilesAfter.first().id)

        // Main account is completely intact
        val main = repo.get(ServiceKind.JELLYFIN, "")
        assertEquals("token-adult", main.token)
        assertEquals("https://jf.local", main.baseUrl)

        // Kid account is gone
        val deleted = repo.get(ServiceKind.JELLYFIN, "kid-1")
        assertEquals("", deleted.token)
    }
}
