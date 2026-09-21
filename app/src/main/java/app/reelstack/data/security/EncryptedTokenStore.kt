package app.reelstack.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedTokenStore(context: Context) : TokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun put(key: String, value: String) {
        if (value.isBlank()) {
            remove(key)
            return
        }
        preferences.edit { putString(key, encrypt(value)) }
    }

    override fun putAll(values: Map<String, String>) {
        val encrypted = values.mapValues { (_, value) ->
            require(value.isNotBlank()) { "Tomme løyndomar skal fjernast, ikkje skrivast." }
            encrypt(value)
        }
        // One SharedPreferences transaction means salt, hash, version and KDF cost become
        // visible together. Rehashing can therefore never leave a mixed PIN record on disk.
        preferences.edit { encrypted.forEach { (key, value) -> putString(key, value) } }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return listOf(cipher.iv, encrypted).joinToString(SEPARATOR) {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
    }

    override fun get(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null
        val parts = stored.split(SEPARATOR, limit = 2)
        if (parts.size != 2) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    override fun remove(key: String) {
        preferences.edit { remove(key) }
    }

    /**
     * Whether something was written under [key], regardless of whether it can still be read.
     *
     * The Keystore key can go away underneath us — a restore onto a different device, some OEM
     * update paths — and then [get] returns null for a value that is very much still there. Told
     * apart from "never signed in", that is a sentence the user can act on instead of a home
     * screen that quietly falls back to demo content.
     */
    override fun hasStoredValue(key: String): Boolean = preferences.contains(key)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "reelstack_secrets"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "reelstack.connection.tokens.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"
    }
}
