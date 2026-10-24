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
        assertEquals(LauncherTarget.SwitchToKid(kid), resolveLauncherTarget(link(kid.id), listOf(main), "", kids))
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
            LauncherTarget.SwitchToKid(kid),
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
