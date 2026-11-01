package app.reelstack.launcher

import android.app.Application
import android.net.Uri
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LauncherProfilesTest {
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "http://jf.local:8096", "secret-jf", userId = "oyvind")
    private val emby = ServiceConnection(ServiceKind.EMBY, "Emby", "http://emby.local:8096", "secret-emby", userId = "eilev")
    private val profiles = listOf(UserProfile("", "Øyvind", false), UserProfile("emby-eilev", "Eilev", false,
        "http://emby.local:8096/Users/eilev/Images/Primary?tag=a"), UserProfile("kid", "Barn", true))
    private val connections = mapOf("" to listOf(jellyfin, emby), "emby-eilev" to listOf(emby), "kid" to listOf(
        ServiceConnection(ServiceKind.JELLYFIN, "Jellyfin", "http://jf.local:8096", "", userId = "child")))

    @Test fun oneRowPerProfileWithASignedInLibraryAccountAndNoSecrets() {
        val rows = launcherProfiles(profiles) { connections[it].orEmpty() }
        assertEquals(listOf("", "emby-eilev"), rows.map { it.id })
        // The main profile opens with the account that has no own profile; Eilev has his.
        assertEquals(LauncherProfileRow("", "Øyvind", "jellyfin", "oyvind", "http://jf.local:8096/Users/oyvind/Images/Primary", false), rows[0])
        assertEquals(LauncherProfileRow("emby-eilev", "Eilev", "emby", "eilev", "http://emby.local:8096/Users/eilev/Images/Primary?tag=a", false), rows[1])
        assertFalse(rows.toString().contains("secret"))
    }

    private fun select(user: String, kind: ServiceKind = ServiceKind.EMBY, active: String = "", pin: Boolean = false) =
        launcherSelection(kind, user, active, profiles, { connections[it].orEmpty() }, pin)

    @Test fun stovasProfileSwitchTakesSpoleToThatPersonsProfile() {
        assertEquals(LauncherSelection.To("emby-eilev"), select("eilev"))
        assertEquals(LauncherSelection.To(""), select("oyvind", ServiceKind.JELLYFIN, active = "emby-eilev"))
        assertEquals(LauncherSelection.Already, select("eilev", active = "emby-eilev"))
        assertEquals(LauncherSelection.Unknown, select("nobody"))
    }

    @Test fun stovaCannotTakeSpoleOutOfAChildsProfilePastItsPin() {
        assertEquals(LauncherSelection.Blocked, select("oyvind", ServiceKind.JELLYFIN, active = "kid", pin = true))
        assertEquals(LauncherSelection.To(""), select("oyvind", ServiceKind.JELLYFIN, active = "kid", pin = false))
    }

    @Test fun theProviderSwitchesForNoOneButStova() {
        val provider = Robolectric.setupContentProvider(LauncherProfilesProvider::class.java, "app.reelstack.launcher")
        val extras = android.os.Bundle().apply { putString("kind", "emby"); putString("user", "eilev") }
        assertEquals(null, provider.call("select_profile", null, extras))
    }

    @Test fun onlyStovaMayAsk() {
        assertTrue(launcherMayList("app.stova"))
        assertTrue(launcherMayList("app.stova.debug"))
        assertFalse(launcherMayList("com.example.other"))
        assertFalse(launcherMayList(null))
    }

    @Test fun theProviderAnswersAnyoneElseWithNoRows() {
        val provider = Robolectric.setupContentProvider(LauncherProfilesProvider::class.java, "app.reelstack.launcher")
        provider.query(Uri.parse("content://app.reelstack.launcher/profiles"), null, null, null, null)!!.use {
            assertEquals(listOf("id", "name", "kind", "user", "avatar", "kid"), it.columnNames.toList())
            assertEquals(0, it.count)
        }
    }

    @Test fun outsiderGetsEmptyDiscoverCursorsWithTheColumns() {
        val provider = Robolectric.setupContentProvider(LauncherProfilesProvider::class.java, "app.reelstack.launcher")
        val person = "kind=emby&user=eilev"
        mapOf(
            "discover?$person" to LauncherDiscoverColumns.ITEM,
            "discover/requests?$person&refresh=1" to LauncherDiscoverColumns.REQUEST,
            "discover/title?$person&type=movie&tmdb=603" to LauncherDiscoverColumns.TITLE,
            "discover/seasons?$person&type=tv&tmdb=1399" to LauncherDiscoverColumns.SEASON,
            "discover/related?$person&type=movie&tmdb=603" to LauncherDiscoverColumns.ITEM,
            "discover/search?$person&q=matrix&page=2" to LauncherDiscoverColumns.ITEM,
        ).forEach { (path, columns) ->
            provider.query(Uri.parse("content://app.reelstack.launcher/$path"), null, null, null, null).use {
                assertEquals(path, columns.toList(), it.columnNames.toList())
                assertEquals(path, 0, it.count)
                // No `seerr`, as from a Spole without these paths: Stova reads it as no answer and shows nothing.
                assertFalse(path, it.extras.containsKey("seerr"))
                assertTrue(path, it.extras.isEmpty)
            }
        }
    }
}
