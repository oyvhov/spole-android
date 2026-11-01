package app.reelstack.launcher

import android.app.Application
import android.net.Uri
import app.reelstack.R
import app.reelstack.data.model.LibraryMedia
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.repository.AccountRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Stova profile that follows a Spole person shows that person's rows, which Spole fetches with its
 * own sign-in: Eilev's Emby rows in Stova without Eilev's password there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LauncherRowsTest {
    private val jellyfin = ServiceConnection(ServiceKind.JELLYFIN, "Midttunet", "http://jf.local:8096", "secret-jf", userId = "oyvind")
    private val emby = ServiceConnection(ServiceKind.EMBY, "Heime", "http://emby.local:8096", "secret-emby", userId = "eilev")
    private val profiles = listOf(UserProfile("", "Øyvind", false), UserProfile("emby-eilev", "Eilev", false), UserProfile("kid", "Barn", true))
    private val connections = mapOf("" to listOf(emby, jellyfin), "emby-eilev" to listOf(emby),
        "kid" to listOf(ServiceConnection(ServiceKind.JELLYFIN, "Midttunet", "http://jf.local:8096", "secret-kid", userId = "child")))

    @Test fun thePersonIsFoundAsSelectProfileFindsThem() {
        fun find(kind: ServiceKind, user: String) = launcherProfileFor(kind, user, profiles) { connections[it].orEmpty() }?.id
        // Eilev's own profile before the main one that also has his account.
        assertEquals("emby-eilev", find(ServiceKind.EMBY, "eilev"))
        assertEquals("", find(ServiceKind.JELLYFIN, "oyvind"))
        assertEquals("kid", find(ServiceKind.JELLYFIN, "child"))
        assertNull(find(ServiceKind.JELLYFIN, "nobody"))
    }

    @Test fun onlySignedInLibraryAccountsAreAsked() {
        val seerr = ServiceConnection(ServiceKind.SEERR, "Seerr", "http://seerr.local", "secret-seerr", userId = "1")
        val signedOut = emby.copy(token = "")
        assertEquals(listOf(jellyfin, emby), launcherLibraryAccounts(listOf(seerr, emby, signedOut, jellyfin)))
    }

    @Test fun cardsCarryWhatStovaShowsAndNoSecret() {
        val episode = LibraryMedia("emby-ep1", "Pingu", "S1 E2 · Pingu and the Ghost", progress = .4f,
            artworkRes = R.drawable.media_placeholder, source = ServiceKind.EMBY, remoteId = "ep1", mediaType = "Episode",
            season = 1, episode = 2, runtimeMinutes = 5, facts = listOf("Episode", "1990"),
            heroUrl = "http://emby.local:8096/Items/s1/Images/Thumb?maxWidth=1920&quality=90&tag=t",
            backdropUrl = "http://emby.local:8096/Items/s1/Images/Backdrop/0?tag=b", logoUrl = "http://emby.local:8096/Items/s1/Images/Logo",
            tmdbRating = 72f)
        val film = LibraryMedia("emby-f1", "Oppdrag Nemo", "2003", artworkRes = R.drawable.media_placeholder, source = ServiceKind.EMBY,
            remoteId = "f1", mediaType = "Movie", posterUrl = "http://emby.local:8096/Items/f1/Images/Primary", criticRating = 99)
        val cards = launcherRowItems(emby, AccountRows(listOf(episode), listOf(film), emptyList()))
        assertEquals(listOf("continue", "latest_movies"), cards.map { it.row })
        val first = cards[0]
        assertEquals("emby", first.kind)
        assertEquals("eilev", first.user)
        assertEquals("Heime", first.server)
        assertEquals("ep1", first.id)
        assertEquals("Pingu", first.seriesName)
        assertEquals(1990, first.year)
        assertEquals(7.2, first.community!!, .001)
        // The wide card asks for the hero art at card size, as Spole's own shelf does.
        assertEquals("http://emby.local:8096/Items/s1/Images/Thumb?maxWidth=1080&quality=85&tag=t", first.wide)
        assertNull(cards[1].seriesName)
        assertEquals(99, cards[1].critic)
        assertFalse(cards.toString().contains("secret"))
    }

    @Test fun nobodyButStovaGetsRowsOrActions() {
        val provider = Robolectric.setupContentProvider(LauncherProfilesProvider::class.java, "app.reelstack.launcher")
        provider.query(Uri.parse("content://app.reelstack.launcher/rows?kind=emby&user=eilev"), null, null, null, null)!!.use {
            assertEquals(listOf("row", "kind", "user", "server", "id", "type", "title", "series", "season", "episode",
                "year", "runtime", "progress", "overview", "wide", "poster", "backdrop", "logo", "community", "critic"), it.columnNames.toList())
            assertEquals(0, it.count)
        }
        val extras = android.os.Bundle().apply {
            putString("kind", "emby"); putString("user", "eilev"); putString("item", "ep1"); putString("action", "watched")
        }
        assertNull(provider.call("item_action", null, extras))
    }

    @Test fun onlyTheTwoActionsExist() {
        assertEquals(LauncherItemAction.WATCHED, LauncherItemAction.of("watched"))
        assertEquals(LauncherItemAction.REMOVE_RESUME, LauncherItemAction.of("remove_resume"))
        assertNull(LauncherItemAction.of("delete"))
        assertNull(LauncherItemAction.of(null))
    }
}
