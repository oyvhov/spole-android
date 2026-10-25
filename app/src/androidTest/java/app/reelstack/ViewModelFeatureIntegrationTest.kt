package app.reelstack

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.canRequest
import app.reelstack.ui.*
import org.junit.Assert.*
import org.junit.Test

/** Exercises the public facade and the actual feature graph on the isolated test AVD. */
class ViewModelFeatureIntegrationTest {
    @Test fun requestDraftHasOneOwnerAcrossTheFirstIdleFrame() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val store = ViewModelStore()
        val originalProfile = container.connectionRepository.activeProfileId
        val originalConnections = container.connectionRepository.list("")
        val originalOnboarding = container.preferencesRepository.onboardingCompleted
        lateinit var model: ReelstackViewModel
        try {
            instrumentation.runOnMainSync {
                container.connectionRepository.activeProfileId = ""
                app.reelstack.data.model.ServiceKind.entries.forEach { container.connectionRepository.delete(it, "") }
                container.preferencesRepository.onboardingCompleted = true
                model = ReelstackViewModel(container)
                store.put("features", model)
                val media = model.uiState.value.discover.first { it.canRequest }
                model.requestMedia(media.id)
                model.setRequestNotification(false)
            }
            instrumentation.waitForIdleSync()
            assertEquals(AppSheet.RequestComposer, model.uiState.value.activeSheet)
            assertNotNull(model.uiState.value.requestDraft)
            assertFalse(model.uiState.value.requestDraft!!.notify)
            instrumentation.runOnMainSync { model.closeSheet() }
            assertNull(model.uiState.value.requestDraft)
        } finally { instrumentation.runOnMainSync {
            store.clear()
            originalConnections.filter { it.baseUrl.isNotBlank() }.forEach { container.connectionRepository.save(it, "") }
            container.connectionRepository.activeProfileId = originalProfile
            container.preferencesRepository.onboardingCompleted = originalOnboarding
        } }
    }

    @Test fun awayAndBackCreatesFreshFeaturesAndSynchronousScreenProjections() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val container = AppContainer(instrumentation.targetContext)
        val repository = container.connectionRepository
        val original = repository.activeProfileId
        val originalConnections = repository.list("")
        val originalOnboarding = container.preferencesRepository.onboardingCompleted
        val store = ViewModelStore()
        try {
            instrumentation.runOnMainSync {
                repository.activeProfileId = ""
                app.reelstack.data.model.ServiceKind.entries.forEach { repository.delete(it, "") }
                container.preferencesRepository.onboardingCompleted = true
                val model = ReelstackViewModel(container)
                store.put("features", model)
                model.openGlobalSearch()
                model.setGlobalSearchQuery("adult")
                model.openLibraryDetails(model.uiState.value.recentMovies.first().id)
                val oldOwner = model.uiState.value.sessionScope
                repository.activeProfileId = "feature-boundary-child"
                model.synchronizeSession()
                assertTrue(model.uiState.value.isKidMode)
                assertFalse(model.uiState.value.globalSearchOpen)
                assertNull(model.uiState.value.contentDetails)
                assertEquals(model.uiState.value.sessionScope, model.homeUiState.value.sessionScope)
                assertEquals(repository.captureSession(), model.renderingSession.value)
                repository.activeProfileId = ""
                model.synchronizeSession()
                assertNotEquals(oldOwner, model.uiState.value.sessionScope)
                assertEquals("", model.uiState.value.globalSearch.query)
                assertNull(model.uiState.value.requestDraft)
                assertFalse(model.returnFromDetail())
                model.openSheet(AppSheet.ConnectionEditor(app.reelstack.data.model.ServiceKind.JELLYFIN))
                assertNotNull(model.connectionDraft.value)
                model.closeSheet()
                assertNull(model.connectionDraft.value)
            }
        } finally {
            instrumentation.runOnMainSync {
                store.clear()
                originalConnections.filter { it.baseUrl.isNotBlank() }.forEach { repository.save(it, "") }
                repository.activeProfileId = original
                container.preferencesRepository.onboardingCompleted = originalOnboarding
            }
        }
    }
}
