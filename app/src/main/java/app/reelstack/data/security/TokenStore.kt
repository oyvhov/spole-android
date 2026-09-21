package app.reelstack.data.security

/** Key-value store contract for sensitive credentials (tokens, salts, hashes). */
interface TokenStore {
    fun put(key: String, value: String)
    /**
     * Replaces one logical secret record. Secure stores should commit this as one operation;
     * the default keeps small in-memory test stores compatible.
     */
    fun putAll(values: Map<String, String>) {
        values.forEach { (key, value) -> put(key, value) }
    }
    fun get(key: String): String?
    fun remove(key: String)
    fun hasStoredValue(key: String): Boolean
}
