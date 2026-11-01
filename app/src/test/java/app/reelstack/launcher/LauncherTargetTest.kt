package app.reelstack.launcher

import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherTargetTest {
    private val main = connection("adult")
    private val kid = UserProfile("kid-one", "Barn ein", isKid = true)
    private val otherKid = UserProfile("kid-two", "Barn to", isKid = true)
    private val kidConnection = connection(kid.id, token = "fake-child-token")
    private val otherKidConnection = connection(otherKid.id, token = "fake-other-child-token")
    private val kids = listOf(kid, otherKid)
    private val childConnections = mapOf(
        kid.id to listOf(kidConnection),
        otherKid.id to listOf(otherKidConnection),
    )

    @Test fun opensMainUserInMainMode() {
        assertEquals(LauncherTarget.Open(main), resolveLauncherTarget(link(main.userId), listOf(main), "", kids))
    }

    @Test fun switchesToKnownKidFromMainMode() {
        assertEquals(LauncherTarget.SwitchToProfile(kid), resolveLauncherTarget(link(kid.id), listOf(main), "", kids))
    }

    @Test fun blocksMainUserInKidMode() {
        assertEquals(
            LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(link(main.userId), listOf(main), kid.id, kids, childConnections),
        )
    }

    @Test fun blocksAnotherKidInKidMode() {
        assertEquals(
            LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(link(otherKid.id), listOf(main), kid.id, kids, childConnections),
        )
    }

    @Test fun opensActiveKidWithItsOwnConnection() {
        assertEquals(
            LauncherTarget.Open(kidConnection),
            resolveLauncherTarget(link(kid.id), listOf(main), kid.id, kids, childConnections),
        )
    }

    @Test fun rejectsUnknownUserInMainMode() {
        assertEquals(LauncherTarget.NotSignedIn, resolveLauncherTarget(link("unknown"), listOf(main), "", kids))
    }

    @Test fun rejectsUnknownUserInKidMode() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(link("unknown"), listOf(main), kid.id, kids, childConnections),
        )
    }

    @Test fun requiresMainConnectionForRequestedService() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(link(main.userId, ServiceKind.EMBY), listOf(main), "", kids),
        )
    }

    @Test fun rejectsMainConnectionWithEmptyToken() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(link(main.userId), listOf(main.copy(token = "")), "", kids),
        )
    }

    @Test fun usesFirstMainConnectionWithMatchingKindAndNonemptyToken() {
        assertEquals(
            LauncherTarget.Open(main),
            resolveLauncherTarget(link(main.userId), listOf(main.copy(token = ""), main), "", kids),
        )
    }

    @Test fun missingChildConnectionCannotFallBackToAdult() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(link(kid.id), listOf(main), kid.id, kids),
        )
    }

    @Test fun emptyChildTokenCannotFallBackToAdult() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(
                link(kid.id), listOf(main), kid.id, kids,
                mapOf(kid.id to listOf(kidConnection.copy(token = ""))),
            ),
        )
    }

    @Test fun mismatchedChildUserCannotFallBackToAdult() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(
                link(kid.id), listOf(main), kid.id, kids,
                mapOf(kid.id to listOf(kidConnection.copy(userId = "different-user"))),
            ),
        )
    }

    @Test fun childConnectionForAnotherServiceCannotAuthorizePlayback() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(
                link(kid.id), listOf(main), kid.id, kids,
                mapOf(kid.id to listOf(kidConnection.copy(kind = ServiceKind.EMBY))),
            ),
        )
    }

    @Test fun opensEmbyChildUsingItsServiceUserIdRatherThanLocalProfileId() {
        val embyMain = main.copy(kind = ServiceKind.EMBY)
        val embyKid = kidConnection.copy(kind = ServiceKind.EMBY, userId = "emby-child")
        assertEquals(
            LauncherTarget.Open(embyKid),
            resolveLauncherTarget(
                link(embyKid.userId, ServiceKind.EMBY), listOf(main, embyMain), kid.id, kids,
                mapOf(kid.id to listOf(kidConnection, embyKid)),
            ),
        )
    }

    @Test fun switchesToChildUsingItsServiceUserIdRatherThanLocalProfileId() {
        val embyMain = main.copy(kind = ServiceKind.EMBY)
        val embyKid = kidConnection.copy(kind = ServiceKind.EMBY, userId = "emby-child")
        assertEquals(
            LauncherTarget.SwitchToProfile(kid),
            resolveLauncherTarget(
                link(embyKid.userId, ServiceKind.EMBY), listOf(embyMain), "", kids,
                mapOf(kid.id to listOf(embyKid)),
            ),
        )
    }

    @Test fun blocksOtherChildUsingItsServiceUserIdRatherThanLocalProfileId() {
        val other = otherKidConnection.copy(userId = "other-service-user")
        assertEquals(
            LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(
                link(other.userId), listOf(main), kid.id, kids,
                childConnections + (otherKid.id to listOf(other)),
            ),
        )
    }

    private val eilev = UserProfile("emby-eilev", "Eilev", isKid = false)
    private val eilevAccount = ServiceConnection(ServiceKind.EMBY, "Emby", "https://emby.example.test", "fake-eilev-token", userId = "eilev")
    private val mainEmby = eilevAccount.copy(token = "fake-main-emby-token")
    private val withEilev = listOf(eilev, kid)
    private val eilevConnections = childConnections + (eilev.id to listOf(eilevAccount))
    private fun profileLink(userId: String, kind: ServiceKind = ServiceKind.EMBY) = LauncherLink(kind, userId, "", LauncherAction.PROFILE)

    @Test fun profileLinkSwitchesFromMainToTheOwnProfileWithThatAccount() {
        assertEquals(LauncherTarget.SwitchToProfile(eilev),
            resolveLauncherTarget(profileLink("eilev"), listOf(main, mainEmby), "", withEilev, eilevConnections))
    }

    @Test fun profileLinkOpensWhenThatProfileIsAlreadyActive() {
        assertEquals(LauncherTarget.Open(eilevAccount),
            resolveLauncherTarget(profileLink("eilev"), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections))
    }

    @Test fun profileLinkForAnAccountOnlyInTheMainProfileOpensTheMainProfile() {
        val toMain = resolveLauncherTarget(profileLink("adult", ServiceKind.JELLYFIN), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections)
        assertEquals("", (toMain as LauncherTarget.SwitchToProfile).profile.id)
        assertEquals(LauncherTarget.Open(main),
            resolveLauncherTarget(profileLink("adult", ServiceKind.JELLYFIN), listOf(main, mainEmby), "", withEilev, eilevConnections))
    }

    @Test fun profileLinkCannotLeaveKidModeOrNameAnUnknownAccount() {
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(profileLink("eilev"), listOf(main, mainEmby), kid.id, withEilev, eilevConnections))
        assertEquals(LauncherTarget.NotSignedIn,
            resolveLauncherTarget(profileLink("nobody"), listOf(main, mainEmby), "", withEilev, eilevConnections))
    }

    @Test fun anOwnProfileIsNotKidModeForOtherLinks() {
        // Its own account opens with its own sign-in; the main user's title takes the main profile back first.
        assertEquals(LauncherTarget.Open(eilevAccount),
            resolveLauncherTarget(link("eilev", ServiceKind.EMBY), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections))
        val back = resolveLauncherTarget(link(main.userId), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections)
        assertEquals("", (back as LauncherTarget.SwitchToProfile).profile.id)
        assertEquals(LauncherTarget.SwitchToProfile(kid),
            resolveLauncherTarget(link(kid.id), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections))
    }

    private fun discoverLink(userId: String, kind: ServiceKind = ServiceKind.JELLYFIN) =
        LauncherLink(kind, userId, "", LauncherAction.DISCOVER, discover = "seerr-movie-603")

    @Test fun discoverOpensAsTheGrownUpWhoHasTheAccount() {
        assertEquals(LauncherTarget.Open(main), resolveLauncherTarget(discoverLink(main.userId), listOf(main), "", kids))
        assertEquals(LauncherTarget.Open(eilevAccount),
            resolveLauncherTarget(discoverLink("eilev", ServiceKind.EMBY), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections))
    }

    @Test fun discoverIsNeverForAChild() {
        // A title link switches to the child; Discover is for grown-ups only.
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(discoverLink(kid.id), listOf(main), "", kids, childConnections))
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(discoverLink(kid.id), listOf(main), "", kids))
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(discoverLink(kid.id), listOf(main), kid.id, kids, childConnections))
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(discoverLink(main.userId), listOf(main), kid.id, kids, childConnections))
        assertEquals(LauncherTarget.BlockedInKidMode,
            resolveLauncherTarget(discoverLink(kid.id), listOf(main, mainEmby), eilev.id, withEilev, eilevConnections))
    }

    @Test fun discoverNeedsTheAccountSignedIn() {
        assertEquals(LauncherTarget.NotSignedIn, resolveLauncherTarget(discoverLink("unknown"), listOf(main), "", kids))
        assertEquals(LauncherTarget.NotSignedIn, resolveLauncherTarget(discoverLink(main.userId), emptyList(), "", kids))
        assertEquals(LauncherTarget.NotSignedIn,
            resolveLauncherTarget(discoverLink(main.userId), listOf(main.copy(token = "")), "", kids))
    }

    @Test fun requiresMainSignInEvenForActiveKid() {
        assertEquals(
            LauncherTarget.NotSignedIn,
            resolveLauncherTarget(link(kid.id), emptyList(), kid.id, kids, childConnections),
        )
    }

    private fun link(userId: String, kind: ServiceKind = ServiceKind.JELLYFIN) =
        LauncherLink(kind, userId, "item", LauncherAction.PLAY)

    private fun connection(userId: String, token: String = "fake-main-token") = ServiceConnection(
        kind = ServiceKind.JELLYFIN,
        name = "Testtenar",
        baseUrl = "https://media.example.test",
        token = token,
        userId = userId,
    )
}
