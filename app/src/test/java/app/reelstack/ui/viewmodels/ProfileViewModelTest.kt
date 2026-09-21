package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.model.UserProfile
import app.reelstack.data.network.EmbyAuthenticationClient
import app.reelstack.data.network.JellyfinAuthenticationClient
import app.reelstack.data.repository.ConnectionRepository
import app.reelstack.data.repository.InMemoryTokenStore
import app.reelstack.data.security.PinSecurity
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    private lateinit var context: Context
    private lateinit var tokenStore: InMemoryTokenStore
    private lateinit var connectionRepository: ConnectionRepository
    private lateinit var pinSecurity: PinSecurity
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("reelstack_connections", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("reelstack_pin", Context.MODE_PRIVATE).edit().clear().commit()

        tokenStore = InMemoryTokenStore()
        connectionRepository = ConnectionRepository(context, tokenStore = tokenStore)
        pinSecurity = PinSecurity(context, tokenStore = tokenStore)

        viewModel = ProfileViewModel(
            connectionRepository = connectionRepository,
            pinSecurity = pinSecurity,
            jellyfinAuthClient = JellyfinAuthenticationClient(),
            embyAuthClient = EmbyAuthenticationClient(),
            appContext = context,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `loadProfiles loads main profile and saved kids profiles`() {
        val adultConn = ServiceConnection(ServiceKind.JELLYFIN, "Heime", "http://jf.local", "token1", "u1")
        connectionRepository.save(adultConn, "")
        val kidConn = ServiceConnection(ServiceKind.JELLYFIN, "Heime", "http://jf.local", "token2", "kid1")
        connectionRepository.save(kidConn, "kid1")
        connectionRepository.registerKidProfile("kid1", "Astrid", null)

        viewModel.loadProfiles()

        val state = viewModel.uiState.value
        assertEquals(2, state.profiles.size)
        assertEquals("", state.profiles[0].id)
        assertEquals("kid1", state.profiles[1].id)
        assertFalse(state.isKidMode)
    }

    @Test
    fun `selectProfile from kid mode to adult mode prompts PIN when configured`() {
        pinSecurity.setPin("1234")
        connectionRepository.activeProfileId = "kid1"
        viewModel.loadProfiles()
        assertTrue(viewModel.uiState.value.isKidMode)

        var promptedTargetId: String? = null
        var promptedIsSetup: Boolean? = null
        var switchSucceeded = false

        val adultProfile = UserProfile(id = "", name = "Hovudkonto", isKid = false)
        viewModel.selectProfile(
            profile = adultProfile,
            onSwitchSuccess = { switchSucceeded = true },
            onPromptPin = { target, isSetup ->
                promptedTargetId = target
                promptedIsSetup = isSetup
            },
        )

        assertFalse(switchSucceeded)
        assertEquals("", promptedTargetId)
        assertEquals(false, promptedIsSetup)
    }

    @Test
    fun `selectProfile from adult to kid switches directly when PIN is disabled`() {
        assertFalse(pinSecurity.isPinConfigured())
        val kidProfile = UserProfile(id = "kid1", name = "Ola", isKid = true)

        var switchSucceeded = false

        viewModel.selectProfile(
            profile = kidProfile,
            onSwitchSuccess = { switchSucceeded = true },
            onPromptPin = { _, _ -> error("PIN should not be requested when protection is disabled") },
        )

        assertTrue(switchSucceeded)
        assertEquals("kid1", connectionRepository.activeProfileId)
    }

    @Test
    fun `submitPin with setup configures PIN and switches profile`() {
        assertFalse(pinSecurity.isPinConfigured())

        var switchedProfileId: String? = null
        viewModel.submitPin(
            pin = "4321",
            targetProfileId = "kid1",
            isSetup = true,
            onSwitchSuccess = { switchedProfileId = it },
        )

        assertTrue(pinSecurity.isPinConfigured())
        assertEquals("kid1", switchedProfileId)
        assertEquals("kid1", connectionRepository.activeProfileId)
        assertTrue(viewModel.uiState.value.isKidMode)
    }

    @Test
    fun `submitPin with incorrect PIN reports error and locks out after threshold`() {
        pinSecurity.setPin("9876")

        var switched = false
        viewModel.submitPin("0000", "", isSetup = false, onSwitchSuccess = { switched = true })
        assertFalse(switched)
        assertNotNull(viewModel.uiState.value.pinError)

        viewModel.submitPin("0000", "", isSetup = false, onSwitchSuccess = { switched = true })
        viewModel.submitPin("0000", "", isSetup = false, onSwitchSuccess = { switched = true })

        assertTrue(viewModel.uiState.value.pinLockoutSeconds > 0)
    }

    @Test
    fun `deleteKidProfile removes kid and refreshes state`() {
        connectionRepository.registerKidProfile("kid-to-delete", "Kari", null)
        viewModel.loadProfiles()
        assertEquals(2, viewModel.uiState.value.profiles.size)

        val kid = UserProfile("kid-to-delete", "Kari", isKid = true)
        viewModel.deleteKidProfile(kid)

        assertEquals(1, viewModel.uiState.value.profiles.size)
        assertEquals("", viewModel.uiState.value.profiles[0].id)
    }
}
