package app.reelstack.ui.components

import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent

/** Open the menu on release, so the held OK key cannot activate a newly focused menu item. */
@Composable
internal fun Modifier.tvCardPress(onClick: () -> Unit, onLongClick: (() -> Unit)?): Modifier {
    if (!isTelevision() || onLongClick == null) return this
    var pressedAt by remember { mutableStateOf<Long?>(null) }
    return onFocusChanged { if (!it.hasFocus) pressedAt = null }.onPreviewKeyEvent { event ->
        val key = event.nativeKeyEvent
        if (key.keyCode !in setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) false
        else {
            when (key.action) {
                KeyEvent.ACTION_DOWN -> if (pressedAt == null) pressedAt = key.eventTime
                KeyEvent.ACTION_UP -> {
                    val start = pressedAt
                    pressedAt = null
                    if (start != null && !key.isCanceled) {
                        if (key.eventTime - start >= ViewConfiguration.getLongPressTimeout()) onLongClick() else onClick()
                    }
                }
            }
            true
        }
    }
}
