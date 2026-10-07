package app.reelstack.data.network

/** Projected metadata only. Scope is a one-way account/profile hash, never a credential. */
interface CatalogueMetadataStore {
    data class Entry(val scope: String, val path: String, val at: Long, val payload: String)
    fun read(scope: String, path: String): Entry?
    fun write(entry: Entry)
    fun clear(scope: String)
}
