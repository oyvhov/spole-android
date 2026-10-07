package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.ui.ReelstackViewModel
import org.junit.Assert.*
import org.junit.Test

/** Synthetic state on an isolated AVD; never run this package on a household profile. */
class ProfileSessionBoundaryTest {
    @Test fun bothRetainedRootsDropAdultRowsBeforeRenderingAChildProfile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = (instrumentation.targetContext.applicationContext as ReelstackApplication).container
        val repository = container.connectionRepository
        val originalProfile = repository.activeProfileId
        val originalOnboarding = container.preferencesRepository.onboardingCompleted
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                repository.activeProfileId = ""
                container.preferencesRepository.onboardingCompleted = true
                val first = ReelstackViewModel(container)
                val retained = ReelstackViewModel(container)
                store.put("first", first); store.put("retained", retained)
                assertTrue(retained.uiState.value.resume.isNotEmpty())
                val adultProjection = retained.homeUiState.value.sessionScope
                repository.activeProfileId = "synthetic-boundary-child"
                first.synchronizeSession(); retained.synchronizeSession()
                for (model in listOf(first, retained)) {
                    val state = model.uiState.value
                    assertEquals(repository.captureSession(), state.sessionScope)
                    assertTrue(state.isKidMode)
                    assertTrue(state.allProfileConnections.isEmpty())
                    assertTrue(state.resume.isEmpty())
                    assertTrue(state.sessions.isEmpty())
                    assertTrue(state.upcoming.isEmpty())
                    assertTrue(state.recentReleases.isEmpty())
                    assertTrue(state.discover.isEmpty())
                    assertTrue(state.activity.isEmpty())
                    assertNull(state.contentDetails)
                }
                // A cached projection has an explicit owner, so the root can withhold it.
                assertNotEquals(adultProjection, retained.uiState.value.sessionScope)
            }
        } finally {
            instrumentation.runOnMainSync {
                store.clear()
                repository.activeProfileId = originalProfile
                container.preferencesRepository.onboardingCompleted = originalOnboarding
            }
        }
    }
}
