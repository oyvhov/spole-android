package app.reelstack.data.repository.cache

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The dashboard cache used to be one JSON blob in SharedPreferences: every read parsed every row,
 * and nothing could be read a page at a time. Rows now live in SQLite, keyed by the account
 * fingerprint, so a stale copy is a cheap `DELETE` and a rail is a `LIMIT`ed query.
 */
@Entity(tableName = "cached_media")
data class CachedMediaRow(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    /** Identifies the exact set of signed-in accounts this row belongs to. */
    val fingerprint: String,
    /** Which rail the row belongs to: see [CacheSection]. */
    val section: String,
    /** Preserves the server's ordering without depending on insertion order. */
    val position: Int,
    val id: String,
    val title: String,
    val subtitle: String,
    val source: String,
    val mediaType: String,
    val progress: Float?,
    val artworkUrl: String?,
    val remoteId: String?,
    val overview: String?,
    val facts: String,
    val genres: String,
    val dateLabel: String?,
    val airDateEpochMillis: Long?,
    val inLibrary: Boolean = false,
    val requested: Boolean = false,
    val seerrStatus: Int?,
    val lastActivityEpochMillis: Long? = null,
    @androidx.room.ColumnInfo(defaultValue = "''") val libraryMetadata: String = "",
)

@Entity(tableName = "cache_meta")
data class CacheMetaRow(
    @PrimaryKey val fingerprint: String,
    val schema: Int,
    val refreshedAtEpochMillis: Long,
)

enum class CacheSection { RESUME, NEXT_UP, FAVOURITES, RECENT_MOVIES, RECENT_SERIES, UPCOMING, RECENT_RELEASES, DISCOVER, RECOMMENDATIONS }

@Entity(tableName = "catalogue_metadata", primaryKeys = ["scope", "path"])
data class CatalogueMetadataRow(val scope: String, val path: String, val at: Long, val payload: String)

@Dao
interface CatalogueMetadataDao {
    @Query("SELECT * FROM catalogue_metadata WHERE scope = :scope AND path = :path LIMIT 1")
    fun read(scope: String, path: String): CatalogueMetadataRow?
    @Query("SELECT * FROM catalogue_metadata WHERE scope = :scope ORDER BY path LIMIT :limit OFFSET :offset")
    fun page(scope: String, limit: Int, offset: Int): List<CatalogueMetadataRow>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(row: CatalogueMetadataRow)
    @Query("SELECT COALESCE(SUM(LENGTH(payload) * 2 + 256), 0) FROM catalogue_metadata") fun bytes(): Long
    @Query("DELETE FROM catalogue_metadata WHERE rowid IN (SELECT rowid FROM catalogue_metadata ORDER BY at LIMIT 100)")
    fun trimOldest()
    @Query("DELETE FROM catalogue_metadata WHERE at < :before") fun expire(before: Long)
    @Query("DELETE FROM catalogue_metadata") fun clear()
    @Query("DELETE FROM catalogue_metadata WHERE scope = :scope") fun clearScope(scope: String)
    @androidx.room.Transaction fun writeBounded(row: CatalogueMetadataRow) {
        expire(row.at - 6 * 60 * 60_000)
        if (row.payload.length * 2L + 256 > 8L * 1024 * 1024) return
        insert(row)
        while (bytes() > 32L * 1024 * 1024) trimOldest()
    }
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_meta WHERE fingerprint = :fingerprint AND schema = :schema LIMIT 1")
    fun meta(fingerprint: String, schema: Int): CacheMetaRow?

    /**
     * Reads one rail. [limit] and [offset] are what a JSON blob could never offer: Home asks for
     * the first handful, and anything that wants more pages asks for them without re-reading
     * everything that came before.
     */
    @Query(
        "SELECT * FROM cached_media WHERE fingerprint = :fingerprint AND section = :section " +
            "ORDER BY position ASC LIMIT :limit OFFSET :offset",
    )
    fun page(fingerprint: String, section: String, limit: Int, offset: Int): List<CachedMediaRow>

    @Query("SELECT COUNT(*) FROM cached_media WHERE fingerprint = :fingerprint AND section = :section")
    fun count(fingerprint: String, section: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRows(rows: List<CachedMediaRow>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMeta(meta: CacheMetaRow)

    @Query("DELETE FROM cached_media")
    fun clearRows()

    @Query("DELETE FROM cache_meta")
    fun clearMeta()

    @androidx.room.Transaction
    fun replaceAll(meta: CacheMetaRow, rows: List<CachedMediaRow>) {
        clearRows()
        clearMeta()
        insertMeta(meta)
        insertRows(rows)
    }
}

// Version 4: the cached text changed meaning. Rows written by an older build hold a media line
// with the type word baked in ("Film · 2024"); this build composes that line from the type and
// would write it twice. The cache is a cache, so it is dropped rather than migrated.
@Database(entities = [CachedMediaRow::class, CacheMetaRow::class, CatalogueMetadataRow::class], version = 5, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun catalogueMetadataDao(): CatalogueMetadataDao

    fun catalogueStore(): app.reelstack.data.network.CatalogueMetadataStore = object : app.reelstack.data.network.CatalogueMetadataStore {
        override fun read(scope: String, path: String) = catalogueMetadataDao().read(scope, path)?.let {
            app.reelstack.data.network.CatalogueMetadataStore.Entry(it.scope, it.path, it.at, it.payload)
        }
        override fun write(entry: app.reelstack.data.network.CatalogueMetadataStore.Entry) =
            catalogueMetadataDao().writeBounded(CatalogueMetadataRow(entry.scope, entry.path, entry.at, entry.payload))
        override fun clear(scope: String) = catalogueMetadataDao().clearScope(scope)
    }

    companion object {
        @Volatile private var instance: CacheDatabase? = null

        val ARTWORK_TOKEN_MIGRATION = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // These tables are disposable. Do not retain token URLs in artwork or metadata JSON.
                db.query("PRAGMA secure_delete = ON").use { it.moveToFirst() }
                db.execSQL("DELETE FROM cached_media")
                db.execSQL("DELETE FROM cache_meta")
                db.execSQL("CREATE TABLE IF NOT EXISTS catalogue_metadata (scope TEXT NOT NULL, path TEXT NOT NULL, at INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(scope, path))")
            }
        }

        fun get(context: Context): CacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CacheDatabase::class.java,
                "spole-cache.db",
            )
                .addMigrations(ARTWORK_TOKEN_MIGRATION)
                .addMigrations(object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE cached_media ADD COLUMN libraryMetadata TEXT NOT NULL DEFAULT ''")
                    }
                })
                // Unknown older schemas may be rebuilt; current account and offline stores are separate.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
