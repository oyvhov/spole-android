package app.reelstack

import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.*
import app.reelstack.data.repository.CalendarFollowStore
import app.reelstack.data.repository.removeObsoleteServices
import app.reelstack.data.security.TokenStore
import org.junit.Assert.*
import org.junit.Test

class CalendarMigrationTest {
    private class Tokens : TokenStore {
        val values = mutableMapOf<String,String>()
        val removed = mutableListOf<String>()
        var failAt: String? = null
        override fun get(key: String) = values[key]
        override fun put(key: String,value: String) { values[key]=value }
        override fun remove(key: String) { if(key==failAt) error("Interrupted migration"); removed+=key;values.remove(key) }
        override fun hasStoredValue(key: String)=key in values
    }
    @Test fun migrationRemovesExactOldRecordsPreservesAccountsAndIsIdempotent() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val preferences=context.getSharedPreferences("calendar-migration-fixture",0)
        preferences.edit().clear().putStringSet("kid_profile_ids",setOf("child"))
            .putString("radarr.url","https://old.example").putString("sonarr.last_url","https://old.example")
            .putString("kid.child.sonarr.url","https://old.example").putString("kid.orphan.radarr.url","https://old.example")
            .putString("jellyfin.url","https://library.example").putString("seerr.url","https://seerr.example")
            .putString("kid.child.jellyfin.url","https://library.example").putString("other.sonarr.note","keep").commit()
        val tokens=Tokens()
        listOf("radarr.token","sonarr.token","kid.child.sonarr.token","kid.orphan.radarr.token","jellyfin.token","seerr.token").forEach { tokens.put(it,"secret") }
        try {
            removeObsoleteServices(preferences,tokens)
            assertFalse(preferences.all.keys.any { it.startsWith("radarr.") || it.startsWith("sonarr.") || it.startsWith("kid.child.sonarr.") || it.startsWith("kid.orphan.radarr.") })
            assertEquals("secret",tokens.get("jellyfin.token"));assertEquals("secret",tokens.get("seerr.token"))
            assertEquals("https://library.example",preferences.getString("kid.child.jellyfin.url",null))
            assertEquals("keep",preferences.getString("other.sonarr.note",null))
            assertFalse(tokens.values.keys.any { it.startsWith("radarr.") || it.startsWith("sonarr.") || ".radarr." in it || ".sonarr." in it })
            val count=tokens.removed.size;removeObsoleteServices(preferences,tokens);assertEquals(count,tokens.removed.size)
        } finally { preferences.edit().clear().commit() }
    }
    @Test fun followsStayLocalAndSeparateByProfileUserServerButSurviveRenewedCookie() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=CalendarFollowStore(context)
        val connection=ServiceConnection(ServiceKind.SEERR,"Seerr","https://calendar-fixture.example","cookie","7",sessionCookie=true)
        val scope=store.scope("fixture-profile",connection,"7")
        try {
            store.set(scope,CalendarTitle(42,"tv","Series"),true)
            assertEquals(listOf("tv:42"),store.read(scope).titles.map { it.key })
            assertEquals(scope,store.scope("fixture-profile",connection.copy(token="renewed"),"7"))
            assertTrue(store.read(store.scope("other-profile",connection,"7")).titles.isEmpty())
            assertTrue(store.read(store.scope("fixture-profile",connection,"8")).titles.isEmpty())
            assertTrue(store.read(store.scope("fixture-profile",connection.copy(baseUrl="https://other-fixture.example"),"7")).titles.isEmpty())
            store.set(scope,CalendarTitle(42,"tv","Series"),false)
            assertTrue(store.read(scope).titles.single().hidden)
            store.set(scope,CalendarTitle(42,"tv","Series"),true)
            assertFalse(store.read(scope).titles.single().hidden)
        } finally { context.getSharedPreferences("calendar_follows",0).edit().remove(scope).commit() }
    }
    @Test fun interruptedMigrationCanRetryWithoutDeletingTheRemainingServices() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val preferences=context.getSharedPreferences("calendar-migration-interrupted",0)
        val tokens=Tokens()
        preferences.edit().clear().putString("radarr.url","old").putString("sonarr.url","old").commit()
        tokens.put("jellyfin.token","keep");tokens.put("radarr.token","old");tokens.put("sonarr.token","old")
        try {
            tokens.failAt="sonarr.token"
            assertTrue(runCatching { removeObsoleteServices(preferences,tokens) }.isFailure)
            assertFalse(preferences.getBoolean("personal_calendar_migration_v1",false))
            tokens.failAt=null;removeObsoleteServices(preferences,tokens)
            assertTrue(preferences.getBoolean("personal_calendar_migration_v1",false))
            assertEquals("keep",tokens.get("jellyfin.token"));assertNull(tokens.get("sonarr.token"))
        } finally { preferences.edit().clear().commit() }
    }
}
