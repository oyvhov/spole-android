package app.reelstack.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class SessionScopeTest {
    private lateinit var repo: ConnectionRepository
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("reelstack_connections", 0).edit().clear().commit()
        repo = ConnectionRepository(context, tokenStore = InMemoryTokenStore())
    }
    private fun connection(kind: ServiceKind = ServiceKind.JELLYFIN) =
        ServiceConnection(kind, "Test", "https://media.example", "fixture", userId = "adult")

    @Test fun switchAwayAndBackRevokesOldLoginWithoutChangingStoredAccounts() {
        repo.save(connection())
        val old = repo.captureSession()
        repo.activeProfileId = "child"
        assertTrue(repo.captureSession().kidsMode)
        repo.activeProfileId = ""
        assertFalse(repo.isCurrent(old))
        assertFalse(repo.saveIfCurrent(old, listOf(connection().copy(token = "late"))))
        assertEquals("fixture", repo.get(ServiceKind.JELLYFIN).token)
    }
    @Test fun linkedLoginSavesBothAccountsUnderTheCapturedProfile() {
        val owner = repo.captureSession()
        assertTrue(repo.saveIfCurrent(owner, listOf(connection(), connection(ServiceKind.SEERR))))
        assertEquals("fixture", repo.get(ServiceKind.JELLYFIN).token)
        assertEquals("fixture", repo.get(ServiceKind.SEERR).token)
        assertFalse(repo.isCurrent(owner))
    }
    @Test fun changingAnotherChildAccountDoesNotLendItToTheActiveProfile() {
        repo.save(connection())
        val adult = repo.get(ServiceKind.JELLYFIN)
        repo.save(connection().copy(token = "child", userId = "child"), "child")
        assertEquals(adult.token, repo.get(ServiceKind.JELLYFIN).token)
    }

    @Test fun calendarWriteCannotAcquireAChildScopeAfterItsAdultSessionWasRevoked() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CalendarFollowStore(context)
        val account = connection(ServiceKind.SEERR).copy(userId = "7")
        repo.save(account)
        val owner = repo.captureSession()
        val adultScope = store.scope(owner.profileId, account, "7")
        val childScope = store.scope("calendar-child", account, "7")
        val title = app.reelstack.data.model.CalendarTitle(123, "tv", "Adult follow")
        val release = java.util.concurrent.CountDownLatch(1)
        val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val pending = worker.submit<Boolean> {
                release.await()
                repo.withCurrentSession(owner) { store.set(adultScope, title, true); true } ?: false
            }
            repo.activeProfileId = "calendar-child"
            release.countDown()
            assertFalse(pending.get(5, java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(store.read(adultScope).titles.isEmpty())
            assertTrue(store.read(childScope).titles.isEmpty())
            repo.activeProfileId = ""
            assertEquals(true, repo.withCurrentSession(repo.captureSession()) { store.set(adultScope, title, true); true })
            assertEquals(listOf(title.key), store.read(adultScope).titles.map { it.key })
        } finally { release.countDown(); worker.shutdownNow() }
    }
}
