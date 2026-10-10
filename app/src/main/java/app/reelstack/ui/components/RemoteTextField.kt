package app.reelstack.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

/**
 * A one-line text field the remote can walk past.
 *
 * On a television the keyboard waits for OK. Focus alone starts no input session, so the keyboard
 * never opens on the way to the button below, and up and down always leave the field. Compose's
 * String-based fields start a session on focus whatever `showKeyboardOnFocus` says, and on Google
 * TV that session opens the keyboard and keeps the remote; this field is built on [TextFieldState]
 * because only that path honours the flag.
 */
@Composable
internal fun RemoteTextField(
    state: TextFieldState,
    television: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    maxLength: Int? = null,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    autoCorrect: Boolean? = null,
    shape: Shape? = null,
    colors: TextFieldColors? = null,
    /** What the keyboard's action key does; null keeps the default, which closes the keyboard. */
    onImeAction: (() -> Unit)? = null,
) {
    // True from OK until focus leaves: the session the remote asked for.
    var typing by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(state,
        modifier.onFocusChanged { if (!it.isFocused) typing = false }.onPreviewKeyEvent { event ->
            if (!television || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                    // The first OK starts the session, which opens the keyboard; later ones reopen it.
                    if (typing) keyboard?.show() else typing = true
                    true
                }
                else -> false
            }
        },
        enabled = enabled,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, null) } },
        supportingText = supportingText?.let { { Text(it) } },
        isError = isError,
        inputTransformation = maxLength?.let { InputTransformation.maxLength(it) },
        keyboardOptions = KeyboardOptions(capitalization = capitalization, autoCorrectEnabled = autoCorrect,
            keyboardType = keyboardType, imeAction = imeAction, showKeyboardOnFocus = !television || typing),
        onKeyboardAction = onImeAction?.let { action -> KeyboardActionHandler { action() } },
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = shape ?: OutlinedTextFieldDefaults.shape,
        colors = colors ?: OutlinedTextFieldDefaults.colors(),
    )
}

/**
 * The same field for a value that lives elsewhere, such as a connection draft. What the owner types
 * goes out through [onValueChange]; a value set from outside, like an address filled in from the
 * network, comes back in. A value that is only the echo of something typed a moment ago is never
 * written back, so fast typing cannot lose a letter to a late echo.
 */
@Composable
internal fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    television: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Unspecified,
    autoCorrect: Boolean? = null,
    shape: Shape? = null,
    colors: TextFieldColors? = null,
    onImeAction: (() -> Unit)? = null,
) {
    val state = rememberTextFieldState(value)
    // Seeded with the starting value, so the field's first reading is not sent back as a change.
    val sent = remember { ArrayDeque<String>().apply { addLast(value) } }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { typed ->
            if (typed == sent.lastOrNull()) return@collect
            sent.addLast(typed)
            while (sent.size > 8) sent.removeFirst()
            currentOnValueChange(typed)
        }
    }
    LaunchedEffect(value) {
        if (value != state.text.toString() && value !in sent) {
            sent.clear()
            sent.addLast(value)
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }
    RemoteTextField(state, television, imeAction, modifier, label = label, placeholder = placeholder,
        supportingText = supportingText, isError = isError, enabled = enabled, keyboardType = keyboardType,
        capitalization = capitalization, autoCorrect = autoCorrect, shape = shape, colors = colors, onImeAction = onImeAction)
}
