package app.reelstack

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.theme.ReelstackTheme
import org.junit.Rule
import org.junit.Test

class MediaArtworkTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authenticatedArtworkFailureKeepsTheScreenRunning() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as ReelstackApplication
        val repository = application.container.connectionRepository
        val original = repository.get(ServiceKind.EMBY)

        repository.save(
            ServiceConnection(
                kind = ServiceKind.EMBY,
                name = "Artwork test",
                baseUrl = "http://127.0.0.1:1",
                token = "test-token",
            ),
        )

        try {
            composeRule.setContent {
                ReelstackTheme {
                    MediaArtwork(
                        url = "http://127.0.0.1:1/Items/episode/Images/Primary",
                        fallbackRes = R.drawable.media_placeholder,
                        contentDescription = "Authenticated artwork",
                        modifier = Modifier.size(120.dp),
                        source = ServiceKind.EMBY,
                    )
                }
            }

            composeRule.onNodeWithContentDescription("Authenticated artwork").assertIsDisplayed()
            composeRule.waitForIdle()
        } finally {
            if (original.baseUrl.isBlank()) {
                repository.delete(ServiceKind.EMBY)
            } else {
                repository.save(original)
            }
        }
    }
}
