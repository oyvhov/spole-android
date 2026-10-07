package app.reelstack.data.repository

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.repository.cache.CacheDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ArtworkCacheMigrationTest {
    @Test fun oldTokenMetadataIsRemovedWithoutRemovingAccountOrOfflineData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null).callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE cached_media (artworkUrl TEXT, libraryMetadata TEXT)")
                    db.execSQL("CREATE TABLE cache_meta (fingerprint TEXT)")
                    db.execSQL("CREATE TABLE unrelated_account (value TEXT)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.use {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO cached_media VALUES ('https://media.example/image?api_key=fixture', '{\"posterUrl\":\"https://media.example?api_key=fixture\"}')")
            db.execSQL("INSERT INTO cache_meta VALUES ('old')")
            db.execSQL("INSERT INTO unrelated_account VALUES ('preserve')")
            CacheDatabase.ARTWORK_TOKEN_MIGRATION.migrate(db)
            db.query("SELECT COUNT(*) FROM cached_media").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.query("SELECT COUNT(*) FROM cache_meta").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
            db.query("SELECT value FROM unrelated_account").use { it.moveToFirst(); assertEquals("preserve", it.getString(0)) }
        }
    }
}
