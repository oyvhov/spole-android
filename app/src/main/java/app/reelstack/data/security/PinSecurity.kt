package app.reelstack.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinResult {
    data object Success : PinResult
    data class Incorrect(val attemptsRemaining: Int, val delaySeconds: Int = 0) : PinResult
    data class LockedOut(val secondsRemaining: Int) : PinResult
    data object Corrupted : PinResult
}

class PinSecurity(
    context: Context,
    private val tokenStore: TokenStore = EncryptedTokenStore(context),
    private val preferences: SharedPreferences = context.getSharedPreferences("reelstack_pin", Context.MODE_PRIVATE),
    private val random: SecureRandom = SecureRandom(),
) {
    companion object {
        const val VERSION_LEGACY = 1
        const val VERSION_PBKDF2 = 2

        const val KEY_VERSION = "kid.pin.version"
        const val KEY_SALT = "kid.pin.salt"
        const val KEY_HASH = "kid.pin.hash"
        const val KEY_ITERATIONS = "kid.pin.iterations"

        const val DEFAULT_ITERATIONS = 10_000
        const val KEY_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16

        private const val PREF_FAILED_ATTEMPTS = "pin_failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "pin_lockout_until"
        private const val PREF_LAST_ATTEMPT_MS = "pin_last_attempt_ms"

        fun sha256(bytes: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(bytes)

        fun pbkdf2HmacSha256(
            pin: CharArray,
            salt: ByteArray,
            iterations: Int = DEFAULT_ITERATIONS,
            keyLengthBits: Int = KEY_LENGTH_BITS,
        ): ByteArray {
            val spec = PBEKeySpec(pin, salt, iterations, keyLengthBits)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        }
    }

    /**
     * True if a PIN has been set.
     * Note: Checks if either salt or hash is present so corrupted state is detected
     * as "configured but damaged" rather than silently unconfigured.
     */
    fun isPinConfigured(): Boolean {
        val hash = tokenStore.get(KEY_HASH)
        val salt = tokenStore.get(KEY_SALT)
        return !hash.isNullOrBlank() || !salt.isNullOrBlank()
    }

    fun pinVersion(): Int {
        val ver = tokenStore.get(KEY_VERSION)?.toIntOrNull()
        if (ver != null) return ver
        // If hash & salt exist but no version key, it's legacy v1 (SHA-256)
        return if (isPinConfigured()) VERSION_LEGACY else VERSION_PBKDF2
    }

    fun setPin(pin: String) {
        val sanitized = pin.trim()
        require(sanitized.length == 4 && sanitized.all { it.isDigit() }) {
            "PIN må vere nøyaktig 4 siffer."
        }
        val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
        val hash = pbkdf2HmacSha256(sanitized.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS)

        tokenStore.put(KEY_VERSION, VERSION_PBKDF2.toString())
        tokenStore.put(KEY_ITERATIONS, DEFAULT_ITERATIONS.toString())
        tokenStore.put(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        tokenStore.put(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))

        resetLockout()
    }

    /**
     * Sets a legacy v1 SHA-256 PIN. Exclusively used for migration testing.
     */
    internal fun setLegacyPinForTesting(pin: String) {
        val sanitized = pin.trim()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val hash = sha256(salt + sanitized.toByteArray(Charsets.UTF_8))
        tokenStore.remove(KEY_VERSION)
        tokenStore.remove(KEY_ITERATIONS)
        tokenStore.put(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        tokenStore.put(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
        resetLockout()
    }

    fun remainingLockoutSeconds(): Int {
        val until = preferences.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val lastAttempt = preferences.getLong(PREF_LAST_ATTEMPT_MS, 0L)
        val now = System.currentTimeMillis()

        // Clock tampering detection: if clock jumped backward before last attempt, maintain lockout
        if (lastAttempt > 0L && now < lastAttempt && until > lastAttempt) {
            val remaining = (until - lastAttempt) / 1000L
            return if (remaining > 0) remaining.toInt() else 0
        }

        val diff = (until - now) / 1000L
        return if (diff > 0) diff.toInt() else 0
    }

    fun verifyPin(enteredPin: String): PinResult {
        val lockoutSeconds = remainingLockoutSeconds()
        if (lockoutSeconds > 0) {
            return PinResult.LockedOut(lockoutSeconds)
        }

        val storedSaltB64 = tokenStore.get(KEY_SALT)
        val storedHashB64 = tokenStore.get(KEY_HASH)

        if (storedSaltB64.isNullOrBlank() && storedHashB64.isNullOrBlank()) {
            // Truly unconfigured
            return PinResult.Success
        }

        // Fail-closed: If either salt or hash is missing or unreadable, FAIL CLOSED!
        if (storedSaltB64.isNullOrBlank() || storedHashB64.isNullOrBlank()) {
            return PinResult.Corrupted
        }

        val salt = runCatching { Base64.decode(storedSaltB64, Base64.NO_WRAP) }.getOrNull()
        val storedHash = runCatching { Base64.decode(storedHashB64, Base64.NO_WRAP) }.getOrNull()

        if (salt == null || storedHash == null || salt.isEmpty() || storedHash.isEmpty()) {
            return PinResult.Corrupted
        }

        val sanitized = enteredPin.trim()
        val version = pinVersion()

        val isMatch = when (version) {
            VERSION_PBKDF2 -> {
                val iterations = tokenStore.get(KEY_ITERATIONS)?.toIntOrNull() ?: DEFAULT_ITERATIONS
                val enteredHash = pbkdf2HmacSha256(
                    pin = sanitized.toCharArray(),
                    salt = salt,
                    iterations = iterations,
                    keyLengthBits = storedHash.size * 8,
                )
                MessageDigest.isEqual(storedHash, enteredHash)
            }
            VERSION_LEGACY -> {
                val enteredHash = sha256(salt + sanitized.toByteArray(Charsets.UTF_8))
                val legacyMatch = MessageDigest.isEqual(storedHash, enteredHash)
                if (legacyMatch) {
                    // Transparent migration to PBKDF2 v2!
                    setPin(sanitized)
                }
                legacyMatch
            }
            else -> {
                // Unknown future version -> fail closed
                return PinResult.Corrupted
            }
        }

        if (isMatch) {
            resetLockout()
            return PinResult.Success
        }

        // Record failed attempt
        val now = System.currentTimeMillis()
        val failed = preferences.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val delaySeconds = delayForFailedAttempts(failed)
        preferences.edit {
            putInt(PREF_FAILED_ATTEMPTS, failed)
            putLong(PREF_LAST_ATTEMPT_MS, now)
            if (delaySeconds > 0) {
                putLong(PREF_LOCKOUT_UNTIL, now + (delaySeconds * 1000L))
            }
        }

        return if (delaySeconds > 0) {
            PinResult.LockedOut(delaySeconds)
        } else {
            PinResult.Incorrect(attemptsRemaining = maxOf(0, 3 - failed), delaySeconds = 0)
        }
    }

    fun clearPin() {
        tokenStore.remove(KEY_VERSION)
        tokenStore.remove(KEY_ITERATIONS)
        tokenStore.remove(KEY_SALT)
        tokenStore.remove(KEY_HASH)
        resetLockout()
    }

    fun resetLockout() {
        preferences.edit {
            remove(PREF_FAILED_ATTEMPTS)
            remove(PREF_LOCKOUT_UNTIL)
            remove(PREF_LAST_ATTEMPT_MS)
        }
    }

    private fun delayForFailedAttempts(failedCount: Int): Int = when {
        failedCount >= 5 -> 60
        failedCount == 4 -> 15
        failedCount == 3 -> 5
        else -> 0
    }
}
