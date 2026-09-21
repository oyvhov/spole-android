package app.reelstack.data.security

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.repository.InMemoryTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PinSecurityTest {

    private lateinit var context: Context
    private lateinit var tokenStore: InMemoryTokenStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reelstack_pin", Context.MODE_PRIVATE).edit().clear().commit()
        tokenStore = InMemoryTokenStore()
    }

    @Test
    fun `isPinConfigured returns false when not set and true when set`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        assertFalse(pinSec.isPinConfigured())

        pinSec.setPin("1234")
        assertTrue(pinSec.isPinConfigured())

        // Plaintext PIN is NEVER stored in tokenStore
        assertFalse(tokenStore.hasStoredValue("1234"))
        assertTrue(tokenStore.hasStoredValue(PinSecurity.KEY_SALT))
        assertTrue(tokenStore.hasStoredValue(PinSecurity.KEY_HASH))
        assertTrue(tokenStore.hasStoredValue(PinSecurity.KEY_VERSION))
        assertEquals(PinSecurity.VERSION_PBKDF2.toString(), tokenStore.get(PinSecurity.KEY_VERSION))
    }

    @Test
    fun `rejects non-4-digit pin`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        try {
            pinSec.setPin("123")
            fail("Expected IllegalArgumentException for 3 digits")
        } catch (_: IllegalArgumentException) {}

        try {
            pinSec.setPin("12345")
            fail("Expected IllegalArgumentException for 5 digits")
        } catch (_: IllegalArgumentException) {}

        try {
            pinSec.setPin("12ab")
            fail("Expected IllegalArgumentException for letters")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun `verifyPin returns Success for correct PIN with PBKDF2`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("8492")

        val result = pinSec.verifyPin("8492")
        assertTrue(result is PinResult.Success)
        assertEquals(PinSecurity.VERSION_PBKDF2, pinSec.pinVersion())
    }

    @Test
    fun `successful v2 10k PIN is rehashed to 100k and remains valid`() {
        seedV2Pin("1357", 10_000)
        val oldSalt = tokenStore.get(PinSecurity.KEY_SALT)
        val oldHash = tokenStore.get(PinSecurity.KEY_HASH)
        val pinSec = PinSecurity(context, tokenStore = tokenStore)

        assertTrue(pinSec.verifyPin("1357") is PinResult.Success)
        assertEquals(PinSecurity.DEFAULT_ITERATIONS.toString(), tokenStore.get(PinSecurity.KEY_ITERATIONS))
        assertNotEquals(oldSalt, tokenStore.get(PinSecurity.KEY_SALT))
        assertNotEquals(oldHash, tokenStore.get(PinSecurity.KEY_HASH))
        assertTrue(pinSec.verifyPin("1357") is PinResult.Success)
    }

    @Test
    fun `v2 rehash writes a complete PIN record in one transaction`() {
        val atomicStore = object : TokenStore {
            val values = mutableMapOf<String, String>()
            var transactions = 0
            override fun put(key: String, value: String) {
                if (key == PinSecurity.KEY_HASH) error("A rehash must not write individual PIN fields")
                values[key] = value
            }
            override fun putAll(values: Map<String, String>) {
                transactions++
                this.values.putAll(values)
            }
            override fun get(key: String): String? = values[key]
            override fun remove(key: String) { values.remove(key) }
            override fun hasStoredValue(key: String): Boolean = values.containsKey(key)
        }
        val salt = ByteArray(PinSecurity.SALT_LENGTH_BYTES) { index -> (index + 1).toByte() }
        atomicStore.values.putAll(mapOf(
            PinSecurity.KEY_VERSION to PinSecurity.VERSION_PBKDF2.toString(),
            PinSecurity.KEY_ITERATIONS to "10000",
            PinSecurity.KEY_SALT to Base64.encodeToString(salt, Base64.NO_WRAP),
            PinSecurity.KEY_HASH to Base64.encodeToString(PinSecurity.pbkdf2HmacSha256("1357".toCharArray(), salt, 10_000), Base64.NO_WRAP),
        ))

        assertTrue(PinSecurity(context, atomicStore).verifyPin("1357") is PinResult.Success)
        assertEquals(1, atomicStore.transactions)
        assertEquals(PinSecurity.DEFAULT_ITERATIONS.toString(), atomicStore.get(PinSecurity.KEY_ITERATIONS))
    }

    @Test
    fun `incorrect v2 10k PIN does not rewrite its existing hash`() {
        seedV2Pin("1357", 10_000)
        val oldSalt = tokenStore.get(PinSecurity.KEY_SALT)
        val oldHash = tokenStore.get(PinSecurity.KEY_HASH)
        val pinSec = PinSecurity(context, tokenStore = tokenStore)

        assertTrue(pinSec.verifyPin("0000") is PinResult.Incorrect)
        assertEquals("10000", tokenStore.get(PinSecurity.KEY_ITERATIONS))
        assertEquals(oldSalt, tokenStore.get(PinSecurity.KEY_SALT))
        assertEquals(oldHash, tokenStore.get(PinSecurity.KEY_HASH))
    }

    @Test
    fun `v2 rejects missing malformed or unreasonable iteration count`() {
        for (invalid in listOf<String?>(null, "not-a-number", "0", "999999999")) {
            seedV2Pin("1357", 10_000)
            if (invalid == null) tokenStore.remove(PinSecurity.KEY_ITERATIONS)
            else tokenStore.put(PinSecurity.KEY_ITERATIONS, invalid)
            assertTrue("$invalid must fail closed", PinSecurity(context, tokenStore).verifyPin("1357") is PinResult.Corrupted)
        }
    }

    @Test
    fun `v2 metadata without a readable version is corrupted rather than treated as legacy`() {
        seedV2Pin("1357", 10_000)
        tokenStore.remove(PinSecurity.KEY_VERSION)

        assertEquals(PinResult.Corrupted, PinSecurity(context, tokenStore).verifyPin("1357"))
    }

    @Test
    fun `unreadable persisted PIN material stays configured and fails closed`() {
        val unreadable = object : TokenStore {
            override fun put(key: String, value: String) = Unit
            override fun get(key: String): String? = null
            override fun remove(key: String) = Unit
            override fun hasStoredValue(key: String): Boolean = key == PinSecurity.KEY_HASH
        }
        val pinSec = PinSecurity(context, tokenStore = unreadable)
        assertTrue(pinSec.isPinConfigured())
        assertTrue(pinSec.verifyPin("1357") is PinResult.Corrupted)
    }

    private fun seedV2Pin(pin: String, iterations: Int) {
        val salt = ByteArray(PinSecurity.SALT_LENGTH_BYTES) { index -> (index + 1).toByte() }
        val hash = PinSecurity.pbkdf2HmacSha256(pin.toCharArray(), salt, iterations)
        tokenStore.put(PinSecurity.KEY_VERSION, PinSecurity.VERSION_PBKDF2.toString())
        tokenStore.put(PinSecurity.KEY_ITERATIONS, iterations.toString())
        tokenStore.put(PinSecurity.KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        tokenStore.put(PinSecurity.KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
    }

    @Test
    fun `transparently migrates legacy SHA-256 PIN to PBKDF2 v2 upon verification`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setLegacyPinForTesting("7351")

        assertEquals(PinSecurity.VERSION_LEGACY, pinSec.pinVersion())
        val oldHash = tokenStore.get(PinSecurity.KEY_HASH)

        // Verifying with correct PIN migrates to v2
        val result = pinSec.verifyPin("7351")
        assertTrue(result is PinResult.Success)

        // Storage is now upgraded to PBKDF2 v2
        assertEquals(PinSecurity.VERSION_PBKDF2, pinSec.pinVersion())
        assertEquals(PinSecurity.VERSION_PBKDF2.toString(), tokenStore.get(PinSecurity.KEY_VERSION))
        assertEquals(PinSecurity.DEFAULT_ITERATIONS.toString(), tokenStore.get(PinSecurity.KEY_ITERATIONS))

        // Hash has changed to the PBKDF2 hash
        val newHash = tokenStore.get(PinSecurity.KEY_HASH)
        assertNotEquals(oldHash, newHash)

        // Subsequent verifications succeed with upgraded PBKDF2
        val result2 = pinSec.verifyPin("7351")
        assertTrue(result2 is PinResult.Success)
    }

    @Test
    fun `fails closed on corrupted salt`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("4321")

        // Corrupt salt with invalid base64 / nonsense
        tokenStore.put(PinSecurity.KEY_SALT, "!!!NOT_BASE64!!!")

        assertTrue(pinSec.isPinConfigured())
        val result = pinSec.verifyPin("4321")
        // MUST NEVER return Success
        assertTrue("Expected Corrupted result but got $result", result is PinResult.Corrupted)
    }

    @Test
    fun `fails closed on corrupted hash`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("4321")

        // Corrupt hash
        tokenStore.put(PinSecurity.KEY_HASH, "")

        assertTrue(pinSec.isPinConfigured())
        val result = pinSec.verifyPin("4321")
        // MUST NEVER return Success
        assertTrue("Expected Corrupted result but got $result", result is PinResult.Corrupted)
    }

    @Test
    fun `fails closed on unsupported or future version`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("4321")
        tokenStore.put(PinSecurity.KEY_VERSION, "999")

        val result = pinSec.verifyPin("4321")
        assertTrue("Expected Corrupted on unsupported future version", result is PinResult.Corrupted)
    }

    @Test
    fun `progressive lockout on incorrect attempts`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("9999")

        // 1st attempt
        val res1 = pinSec.verifyPin("0000")
        assertTrue(res1 is PinResult.Incorrect)
        assertEquals(2, (res1 as PinResult.Incorrect).attemptsRemaining)

        // 2nd attempt
        val res2 = pinSec.verifyPin("0000")
        assertTrue(res2 is PinResult.Incorrect)
        assertEquals(1, (res2 as PinResult.Incorrect).attemptsRemaining)

        // 3rd attempt -> 5s lockout
        val res3 = pinSec.verifyPin("0000")
        assertTrue(res3 is PinResult.LockedOut)
        assertEquals(5, (res3 as PinResult.LockedOut).secondsRemaining)

        // While locked out, any attempt returns LockedOut
        val resLocked = pinSec.verifyPin("9999")
        assertTrue(resLocked is PinResult.LockedOut)
    }

    @Test
    fun `lockout survives app restart`() {
        var pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("5555")

        // Fail 3 times to trigger lockout
        pinSec.verifyPin("1111")
        pinSec.verifyPin("1111")
        pinSec.verifyPin("1111")
        assertTrue(pinSec.remainingLockoutSeconds() > 0)

        // Simulate app restart by constructing a new instance reading from SharedPreferences
        pinSec = PinSecurity(context, tokenStore = tokenStore)
        assertTrue(pinSec.remainingLockoutSeconds() > 0)
        val result = pinSec.verifyPin("5555")
        assertTrue(result is PinResult.LockedOut)
    }

    @Test
    fun `tampering system clock backwards maintains lockout`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("1234")

        // Trigger lockout
        pinSec.verifyPin("0000")
        pinSec.verifyPin("0000")
        pinSec.verifyPin("0000")
        assertTrue(pinSec.remainingLockoutSeconds() > 0)

        // Manipulate prefs to simulate clock jumping 1 hour backwards while lockout_until is in the future
        val prefs = context.getSharedPreferences("reelstack_pin", Context.MODE_PRIVATE)
        val lastAttempt = prefs.getLong("pin_last_attempt_ms", 0L)
        prefs.edit()
            .putLong("pin_last_attempt_ms", System.currentTimeMillis() + 3600_000L) // in the future
            .putLong("pin_lockout_until", System.currentTimeMillis() + 3605_000L)
            .commit()

        // Clock moving backwards must not clear lockout
        assertTrue(pinSec.remainingLockoutSeconds() > 0)
        val res = pinSec.verifyPin("1234")
        assertTrue(res is PinResult.LockedOut)
    }

    @Test
    fun `resetLockout clears lockout state and allows retry`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("2468")

        // Fail to trigger lockout
        pinSec.verifyPin("0000")
        pinSec.verifyPin("0000")
        pinSec.verifyPin("0000")
        assertTrue(pinSec.remainingLockoutSeconds() > 0)

        pinSec.resetLockout()
        assertEquals(0, pinSec.remainingLockoutSeconds())

        val res = pinSec.verifyPin("2468")
        assertTrue(res is PinResult.Success)
    }
}
