package app.reelstack.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.seasonal
import app.reelstack.data.repository.AppPreferencesRepository
import java.time.LocalDate

val LocalPersonalization = staticCompositionLocalOf { Personalization() }

/**
 * The personalization the screen shows today. With «Etter kalenderen» the season is laid over the
 * stored mood here and nowhere else, so settings keep editing what was actually chosen and the app
 * goes back to it the day the season ends. The date is read again whenever the screen returns.
 */
@Composable
internal fun rememberPersonalization(): Personalization {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppPreferencesRepository(context) }
    var value by remember(repository) { mutableStateOf(repository.personalization) }
    DisposableEffect(repository) {
        val stop = repository.observePersonalization { value = it }
        onDispose(stop)
    }
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }
    return remember(value, today) { value.seasonal(today) }
}
