package app.reelstack.launcher

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stova shows a Quick Connect code; Spole approves it as an account it is signed in with on that server. */
class LauncherApprovalTest {
    private val main = UserProfile("", "Øyvind", isKid = false)
    private val kid = UserProfile("k1", "Ola", isKid = true)
    private fun jellyfin(url: String, user: String, token: String = "t", alternate: String = "") =
        ServiceConnection(ServiceKind.JELLYFIN, "Midttunet", url, token, user, alternateUrl = alternate)
    private val home = "http://192.168.10.5:8096"
    private fun request(caller: String? = "app.stova.debug", url: String? = "$home/", id: String? = "srv", code: String? = "123456", child: Boolean = false) =
        LauncherApproval.request(caller, url, id, code, child)

    @Test fun onlyStovaWithACodeForAValidAddressIsHeard() {
        assertEquals(LauncherApproval.Request("app.stova.debug", home, "srv", "123456", false), request())
        assertEquals("app.stova", request(caller = "app.stova")?.launcher)
        assertNull("another app", request(caller = "com.example.other"))
        assertNull("started without a result, Spole cannot see who asks", request(caller = null))
        assertNull(request(code = "12ab56"))
        assertNull(request(code = null))
        assertNull(request(url = "not an address with spaces"))
        assertEquals("the server id is optional", "", request(id = null)?.serverId)
        assertEquals("", request(id = "../x")?.serverId)
    }

    @Test fun accountsOnThatServerAreOfferedByAddressOrIdChildrenFirstForAChild() {
        val connections = mapOf(
            "" to listOf(jellyfin(home, "adult"), ServiceConnection(ServiceKind.EMBY, "Emby", home, "t", "emby-user")),
            "k1" to listOf(jellyfin("https://media.example.org", "kid", alternate = home)),
        )
        val profiles = listOf(main, kid)
        fun choices(r: LauncherApproval.Request, serverId: (String) -> String? = { null }) =
            LauncherApproval.choices(r, profiles, { connections[it].orEmpty() }, serverId).map { it.profile.id to it.connection.userId }
        assertEquals(listOf("" to "adult", "k1" to "kid"), choices(request()!!))
        assertEquals(listOf("k1" to "kid", "" to "adult"), choices(request(child = true)!!))
        val elsewhere = request(url = "http://10.0.0.9:8096")!!
        assertTrue("another server, and no id to match", choices(elsewhere.copy(serverId = "")).isEmpty())
        assertEquals("the same server under another address, by its id", listOf("" to "adult"),
            choices(elsewhere) { url -> if (url == home) "srv" else null })
    }

    @Test fun aSignedOutAccountIsNeverOffered() {
        val choices = LauncherApproval.choices(request()!!, listOf(main), { listOf(jellyfin(home, "adult", token = "")) }) { null }
        assertTrue(choices.isEmpty())
    }

    @Test fun theLauncherHearsWhereATitleStoppedWithoutAnythingSecret() {
        assertEquals(mapOf("kind" to "jellyfin", "user" to "u", "item" to "m1", "position_ms" to 30_000L, "duration_ms" to 60_000L, "ended" to false),
            LauncherProgress.extras(ServiceKind.JELLYFIN, "u", "m1", 30_000, 60_000, false))
        assertEquals("emby", LauncherProgress.extras(ServiceKind.EMBY, "u", "m1", 0, 0, true)?.get("kind"))
        assertNull(LauncherProgress.extras(ServiceKind.SEERR, "u", "m1", 0, 0, false))
        assertNull(LauncherProgress.extras(ServiceKind.JELLYFIN, "", "m1", 0, 0, false))
        assertEquals("never a negative position", 0L, LauncherProgress.extras(ServiceKind.JELLYFIN, "u", "m1", -5, 10, false)?.get("position_ms"))
    }
}
