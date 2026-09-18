package app.reelstack.data.security

/** Key-value store contract for sensitive credentials (tokens, salts, hashes). */
interface TokenStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun remove(key: String)
    fun hasStoredValue(key: String): Boolean
}
