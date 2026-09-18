package app.reelstack.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom

sealed interface PinResult {
    data object Success : PinResult
    data class Incorrect(val attemptsRemaining: Int, val delaySeconds: Int = 0) : PinResult
    data class LockedOut(val secondsRemaining: Int) : PinResult
}

class PinSecurity(
    context: Context,
    private val tokenStore: TokenStore = EncryptedTokenStore(context),
    private val preferences: SharedPreferences = context.getSharedPreferences("reelstack_pin", Context.MODE_PRIVATE),
    private val random: SecureRandom = SecureRandom(),
) {
    companion object {
        private const val KEY_SALT = "kid.pin.salt"
        private const val KEY_HASH = "kid.pin.hash"
        private const val PREF_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "pin_lockout_until"

        fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(bytes)
    }

    fun isPinConfigured(): Boolean =
        !tokenStore.get(KEY_HASH).isNullOrBlank() && !tokenStore.get(KEY_SALT).isNullOrBlank()

    fun setPin(pin: String) {
        val sanitized = pin.trim()
        require(sanitized.length == 4 && sanitized.all { it.isDigit() }) {
            "PIN må vere nøyaktig 4 siffer."
        }
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = sha256(salt + sanitized.toByteArray(Charsets.UTF_8))

        tokenStore.put(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        tokenStore.put(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))

        // Reset any lockouts when a new PIN is set
        resetLockout()
    }

    fun remainingLockoutSeconds(): Int {
        val until = preferences.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val diff = (until - System.currentTimeMillis()) / 1000L
        return if (diff > 0) diff.toInt() else 0
    }

    fun verifyPin(enteredPin: String): PinResult {
        val lockoutSeconds = remainingLockoutSeconds()
        if (lockoutSeconds > 0) {
            return PinResult.LockedOut(lockoutSeconds)
        }

        val storedSaltB64 = tokenStore.get(KEY_SALT)
        val storedHashB64 = tokenStore.get(KEY_HASH)

        if (storedSaltB64.isNullOrBlank() || storedHashB64.isNullOrBlank()) {
            // No PIN is set
            return PinResult.Success
        }

        val salt = runCatching { Base64.decode(storedSaltB64, Base64.NO_WRAP) }.getOrNull()
        val storedHash = runCatching { Base64.decode(storedHashB64, Base64.NO_WRAP) }.getOrNull()

        if (salt == null || storedHash == null) {
            return PinResult.Success
        }

        val sanitized = enteredPin.trim()
        val enteredHash = sha256(salt + sanitized.toByteArray(Charsets.UTF_8))

        if (MessageDigest.isEqual(storedHash, enteredHash)) {
            resetLockout()
            return PinResult.Success
        }

        // Incorrect PIN
        val failed = preferences.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val delaySeconds = delayForFailedAttempts(failed)
        preferences.edit {
            putInt(PREF_FAILED_ATTEMPTS, failed)
            if (delaySeconds > 0) {
                putLong(PREF_LOCKOUT_UNTIL, System.currentTimeMillis() + (delaySeconds * 1000L))
            }
        }

        return if (delaySeconds > 0) {
            PinResult.LockedOut(delaySeconds)
        } else {
            PinResult.Incorrect(attemptsRemaining = 3 - failed, delaySeconds = 0)
        }
    }

    fun clearPin() {
        tokenStore.remove(KEY_SALT)
        tokenStore.remove(KEY_HASH)
        resetLockout()
    }

    fun resetLockout() {
        preferences.edit {
            remove(PREF_FAILED_ATTEMPTS)
            remove(PREF_LOCKOUT_UNTIL)
        }
    }

    private fun delayForFailedAttempts(failedCount: Int): Int = when {
        failedCount >= 5 -> 60
        failedCount == 4 -> 15
        failedCount == 3 -> 5
        else -> 0
    }
}
