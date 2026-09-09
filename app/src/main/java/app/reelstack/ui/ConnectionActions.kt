package app.reelstack.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*

@Composable
internal fun ConnectionTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, warning: Boolean = false) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(onClick, enabled = enabled, interactionSource = interaction,
        colors = ButtonDefaults.textButtonColors(contentColor = if (warning) Warning else Primary),
        modifier = modifier.heightIn(min = 48.dp).focusOutline(interaction, RoundedCornerShape(24.dp))) {
        Text(label)
    }
}

/** Remote entry chooses the safe action; focus never signs an account out. */
@Composable
internal fun SignOutConfirmation(kind: ServiceKind, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val cancelFocus = remember { FocusRequester() }
    // Dialog creates another Android window; capture the caller's selected language first.
    val title = stringResource(R.string.account_sign_out_title, kind.displayName)
    val note = stringResource(R.string.account_sign_out_note)
    val confirmLabel = stringResource(R.string.account_sign_out)
    val cancelLabel = stringResource(R.string.account_cancel)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val readingInteraction = remember { MutableInteractionSource() }
    val window = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    val textHeight = with(androidx.compose.ui.platform.LocalDensity.current) { window.height.toDp() * .28f }
        .coerceIn(80.dp, 240.dp)
    AlertDialog(onDismissRequest = onCancel,
        title = { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { Text(title) } },
        text = { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) { Text(note,
            Modifier.heightIn(max = textHeight).testTag("sign-out-note")
                .onPreviewKeyEvent { event ->
                    val direction = when (event.key) { Key.DirectionDown -> 1; Key.DirectionUp -> -1; else -> 0 }
                    val canScroll = if (direction > 0) scroll.canScrollForward else direction < 0 && scroll.canScrollBackward
                    if (direction != 0 && canScroll) {
                        if (event.type == KeyEventType.KeyDown) scope.launch {
                            scroll.scrollTo((scroll.value + direction * scroll.viewportSize / 2).coerceIn(0, scroll.maxValue))
                        }
                        true
                    } else false
                }
                .focusOutline(readingInteraction, RoundedCornerShape(8.dp))
                .verticalScroll(scroll).focusable(interactionSource = readingInteraction)) } },
        confirmButton = { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) {
            ConnectionTextAction(confirmLabel, onConfirm, Modifier.testTag("sign-out-confirm"), warning = true) } },
        dismissButton = {
            LaunchedEffect(Unit) { if (keyboard) cancelFocus.requestFocus() }
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides density) {
                ConnectionTextAction(cancelLabel, onCancel,
                    Modifier.focusRequester(cancelFocus).testTag("sign-out-cancel"))
            }
        }, containerColor = SurfaceRaised, shape = RoundedCornerShape(28.dp))
}
