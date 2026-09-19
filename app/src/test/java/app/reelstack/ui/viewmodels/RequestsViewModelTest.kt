package app.reelstack.ui.viewmodels

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.DiscoverMedia
import app.reelstack.data.model.ServiceKind
import app.reelstack.data.network.MediaServerClient
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
class RequestsViewModelTest {

    private lateinit var context: Context
    private lateinit var viewModel: RequestsViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = RequestsViewModel(
            mediaServerClient = MediaServerClient(),
            appContext = context,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun testMedia() = DiscoverMedia(
        id = "test-series-1",
        title = "Test Show",
        metadata = "2024",
        artworkRes = 0,
        inLibrary = false,
        mediaType = "tv",
    )

    @Test
    fun `openRequestComposer initializes draft with media`() {
        val media = testMedia()
        viewModel.openRequestComposer(media, null)

        val draft = viewModel.uiState.value.requestDraft
        assertNotNull(draft)
        assertEquals("test-series-1", draft?.media?.id)
        assertFalse(draft?.sending ?: true)
    }

    @Test
    fun `toggleSeason adds and removes seasons`() {
        val media = testMedia()
        viewModel.openRequestComposer(media, null)

        viewModel.toggleSeason(1, checked = true)
        assertEquals(setOf(1), viewModel.uiState.value.requestDraft?.selected)

        viewModel.toggleSeason(2, checked = true)
        assertEquals(setOf(1, 2), viewModel.uiState.value.requestDraft?.selected)

        viewModel.toggleSeason(1, checked = false)
        assertEquals(setOf(2), viewModel.uiState.value.requestDraft?.selected)
    }

    @Test
    fun `setNotify updates notification preference`() {
        val media = testMedia()
        viewModel.openRequestComposer(media, null)

        viewModel.setNotify(true)
        assertTrue(viewModel.uiState.value.requestDraft?.notify == true)

        viewModel.setNotify(false)
        assertFalse(viewModel.uiState.value.requestDraft?.notify == true)
    }

    @Test
    fun `closeRequestComposer clears draft`() {
        val media = testMedia()
        viewModel.openRequestComposer(media, null)
        assertNotNull(viewModel.uiState.value.requestDraft)

        viewModel.closeRequestComposer()
        assertNull(viewModel.uiState.value.requestDraft)
    }
}
