package app.reelstack.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import app.reelstack.data.model.Personalization
import app.reelstack.data.repository.AppPreferencesRepository

val LocalPersonalization = staticCompositionLocalOf { Personalization() }

@Composable
internal fun rememberPersonalization(): Personalization {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { AppPreferencesRepository(context) }
    var value by remember(repository) { mutableStateOf(repository.personalization) }
    DisposableEffect(repository) {
        val stop = repository.observePersonalization { value = it }
        onDispose(stop)
    }
    return value
}
