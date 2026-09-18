package app.reelstack.data.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.repository.InMemoryTokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(tokenStore.hasStoredValue("kid.pin.salt"))
        assertTrue(tokenStore.hasStoredValue("kid.pin.hash"))
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
    fun `verifyPin returns Success for correct PIN`() {
        val pinSec = PinSecurity(context, tokenStore = tokenStore)
        pinSec.setPin("8492")

        val result = pinSec.verifyPin("8492")
        assertTrue(result is PinResult.Success)
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
}
