package app.reelstack.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.reelstack.data.model.*
import app.reelstack.data.repository.KidsPreferencesRepository
import app.reelstack.ui.ReelstackUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-port-xhdpi", application = app.reelstack.SheetTestApplication::class)
class KidsSettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val child = UserProfile("ui-child", "Alex", true)
    private val state = ReelstackUiState(profiles = listOf(child), connections = listOf(
        ServiceConnection(ServiceKind.EMBY, "Media", "https://media.example", "test", userId = "adult")))

    private fun capture(name: String) {
        rule.waitForIdle()
        val output = java.io.File("build/reports/kids-ui/$name.png")
        output.parentFile?.mkdirs()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun childRowOpensParentalControlsAndPersistsAppearancePermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        KidsPreferencesRepository(context).save(child.id, KidsPreferences())
        rule.setContent {
            app.reelstack.ui.theme.ReelstackTheme {
                Surface { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ServicesSettings(state, {}, {}, {}, {})
                } }
            }
        }
        rule.onNodeWithTag("child-settings-ui-child").performScrollTo().performClick()
        capture("phone-parent-profile")
        rule.onNodeWithTag("child-allow-appearance").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(KidsPreferencesRepository(context).read(child.id).allowAppearance) }
        rule.onNodeWithTag("child-bedtime-enabled").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(KidsPreferencesRepository(context).read(child.id).bedtime.enabled) }
        rule.onNodeWithTag("child-bedtime-time").performScrollTo().assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(20, KidsPreferencesRepository(context).read(child.id).bedtime.hour) }
        rule.onNodeWithTag("child-subtitles").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("child-settings-back").performScrollTo().performClick()
        rule.onNodeWithTag("settings-add-child").performScrollTo().assertIsDisplayed()
    }

    @Test fun accountShortcutOpensServicesOnPhoneAtDoubleFontScale() {
        var opened: ServiceKind? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 2f)) {
                app.reelstack.ui.theme.ReelstackTheme { Surface(Modifier.fillMaxSize()) {
                    MobileSettingsScreen(state.copy(accountsSettingsRequest = 1), PaddingValues(0.dp),
                        { opened = it }, {}, {}, { _, _ -> }, {}, {})
                }
                }
            }
        }
        rule.onNodeWithTag("tv-service-EMBY").performScrollTo().assertIsDisplayed().performClick()
        capture("phone-services-font2")
        rule.runOnIdle { assertEquals(ServiceKind.EMBY, opened) }
        rule.onNodeWithTag("child-settings-ui-child").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-television-xhdpi")
    fun accountShortcutOpensServicesOnTv() {
        rule.setContent {
            app.reelstack.ui.theme.ReelstackTheme { Surface(Modifier.fillMaxSize()) {
                TvSettingsScreen(state.copy(accountsSettingsRequest = 1), PaddingValues(0.dp), {}, {}, {}, { _, _ -> }, {}, {})
            }
            }
        }
        rule.onNodeWithTag("tv-service-EMBY").assertIsDisplayed()
        capture("tv-services")
        rule.onNodeWithTag("child-settings-ui-child").performScrollTo().performClick()
        rule.onNodeWithTag("child-autoplay").performScrollTo().assertIsDisplayed()
        capture("tv-parent-playback")
    }
}
