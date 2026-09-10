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
)

@Entity(tableName = "cache_meta")
data class CacheMetaRow(
    @PrimaryKey val fingerprint: String,
    val schema: Int,
    val refreshedAtEpochMillis: Long,
)

enum class CacheSection { RESUME, NEXT_UP, RECENT_MOVIES, RECENT_SERIES, UPCOMING, RECENT_RELEASES, DISCOVER, RECOMMENDATIONS }

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

@Database(entities = [CachedMediaRow::class, CacheMetaRow::class], version = 2, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile private var instance: CacheDatabase? = null

        fun get(context: Context): CacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CacheDatabase::class.java,
                "spole-cache.db",
            )
                // The cache is disposable by definition; rebuilding it costs one refresh.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
