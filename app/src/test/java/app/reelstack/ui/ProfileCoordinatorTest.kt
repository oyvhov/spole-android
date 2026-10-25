package app.reelstack.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.network.*
import app.reelstack.data.repository.*
import app.reelstack.data.security.*
import app.reelstack.offline.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class ProfileCoordinatorTest {
    private lateinit var context: Context
    private lateinit var repository: ConnectionRepository
    private lateinit var pin: PinSecurity
    private val state = MutableStateFlow(ReelstackUiState())
    private var switched: String? = null

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        listOf("reelstack_connections", "reelstack_pin").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        val tokens = InMemoryTokenStore()
        repository = ConnectionRepository(context, tokenStore = tokens)
        pin = PinSecurity(context, tokenStore = tokens)
    }

    private fun TestScope.feature(security: PinSecurity = pin): ProfileCoordinator {
        state.value = ReelstackUiState(sessionScope = repository.captureSession(), activeProfileId = repository.activeProfileId,
            isKidMode = repository.isKidMode)
        return ProfileCoordinator(SessionUiState(state), backgroundScope, AccountProfileClient(), context, repository,
            EmbyAuthenticationClient(), JellyfinAuthenticationClient(), OfflineDownloadRepository(context), OfflineStorage(context), security,
            switchProfileNow = { switched = it; repository.activeProfileId = it },
            closeSheet = { state.value = state.value.copy(activeSheet = null) },
            ioDispatcher = StandardTestDispatcher(testScheduler))
    }

    @Test fun `profiles load from repository without a second state owner`() = runTest {
        repository.registerKidProfile("child", "Fixture", null)
        feature().loadProfiles()
        assertEquals(listOf("", "child"), state.value.profiles.map { it.id })
        assertFalse(state.value.isKidMode)
    }

    @Test fun `child to adult requires configured PIN`() = runTest {
        pin.setPin("1234")
        repository.activeProfileId = "child"
        feature().selectProfile(UserProfile("", "Adult", isKid = false))
        assertNull(switched)
        assertEquals(AppSheet.PinPrompt(""), state.value.activeSheet)
        assertEquals("child", repository.activeProfileId)
    }

    @Test fun `unreadable persisted PIN still prevents an adult switch`() = runTest {
        repository.activeProfileId = "child"
        val unreadable = object : TokenStore {
            override fun put(key: String, value: String) = Unit
            override fun get(key: String): String? = null
            override fun remove(key: String) = Unit
            override fun hasStoredValue(key: String): Boolean = key == PinSecurity.KEY_HASH
        }
        feature(PinSecurity(context, unreadable)).selectProfile(UserProfile("", "Adult", isKid = false))
        assertNull(switched)
        assertTrue(state.value.activeSheet is AppSheet.PinPrompt)
    }

    @Test fun `adult to child switches directly without PIN`() = runTest {
        feature().selectProfile(UserProfile("child", "Child", isKid = true))
        assertEquals("child", switched)
        assertEquals("child", repository.activeProfileId)
    }

    @Test fun `settings PIN setup keeps the adult profile active`() = runTest {
        val feature = feature()
        feature.requestPinSetup()
        feature.submitPin("4321", "", isSetup = true)
        assertTrue(pin.isPinConfigured())
        assertEquals("", repository.activeProfileId)
        assertNull(switched)
        assertNull(state.value.activeSheet)
    }

    @Test fun `incorrect PIN reports an error and locks out after threshold`() = runTest {
        pin.setPin("9876")
        repository.activeProfileId = "child"
        val feature = feature()
        feature.selectProfile(UserProfile("", "Adult", isKid = false))
        repeat(3) { feature.submitPin("0000", "", false) }
        assertNotNull(state.value.pinError)
        assertTrue(state.value.pinLockoutSeconds > 0)
        assertNull(switched)
    }

    @Test fun `PIN cannot be disabled from a child or without correct verification`() = runTest {
        pin.setPin("4321")
        assertFalse(feature().clearPinProtection("1111"))
        repository.activeProfileId = "child"
        assertFalse(feature().clearPinProtection("4321"))
        assertTrue(pin.isPinConfigured())
        repository.activeProfileId = ""
        assertTrue(feature().clearPinProtection("4321"))
        assertFalse(pin.isPinConfigured())
    }

    @Test fun `deleting a profile is adult only and refreshes the same snapshot`() = runTest {
        repository.registerKidProfile("child", "Child", null)
        repository.activeProfileId = "child"
        feature().deleteKidProfile(UserProfile("child", "Child", isKid = true))
        assertEquals(2, repository.listProfiles().size)
        repository.activeProfileId = ""
        feature().deleteKidProfile(UserProfile("child", "Child", isKid = true))
        assertEquals(1, state.value.profiles.size)
    }
}
