package app.reelstack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.ui.layout.WindowLayoutPolicy
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

internal val LocalSheetKeyboardEntry = staticCompositionLocalOf { false }

/** A reading modal: one measured surface, one non-overshooting entrance, no drag anchors. */
@Composable
internal fun StableSheetDialog(
    dismissEnabled: Boolean,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
    content: @Composable (entered: Boolean, closing: Boolean, close: () -> Unit) -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var entered by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val canDismiss by rememberUpdatedState(dismissEnabled)
    val dismissLatest by rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()
    val hostView = LocalView.current
    val openerInput = LocalInputModeManager.current
    // Dialog owns a new Android view; its initial input mode can differ from the opener.
    val keyboardEntry = remember { openerInput.inputMode == InputMode.Keyboard }
    val dismissLabel = stringResource(R.string.sheet_dismiss)
    val dialogLabel = stringResource(R.string.sheet_title)
    val close = {
        if (canDismiss && !closing) {
            closing = true
            scope.launch {
                progress.animateTo(0f, tween(200))
                dismissLatest()
            }
        }
    }
    LaunchedEffect(Unit) {
        // Android honours the user's system touch-feedback preference.
        hostView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        progress.animateTo(1f, tween(320, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
        entered = true
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            // Only Compose owns the entrance/scrim; a platform dialog fade must not run
            // over the top of it or dim the page before our first animation frame.
            window?.apply {
                setDimAmount(0f)
                setWindowAnimations(0)
                WindowCompat.getInsetsController(this, decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
        // The dialog's constraints, not LocalWindowInfo's asynchronously updated size, own
        // geometry. Insets/keyboard can constrain the surface; media responses cannot.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val policy = WindowLayoutPolicy(maxWidth.value, maxHeight.value)
            if (!fullScreen) Box(Modifier.fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color(0xB8040308))
                .semantics { contentDescription = dismissLabel }
                // Outside-tap dismissal is not a visible remote-control destination.
                .focusProperties { canFocus = false }
                .clickable(enabled = dismissEnabled && !closing,
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = close))
            BoxWithConstraints(Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .imePadding()) {
                val sheetHeight = if (fullScreen) maxHeight else policy.dialogHeightDp(maxHeight.value).dp
                Surface(
                    modifier = Modifier.align(if (policy.useCenteredDialog) Alignment.Center else Alignment.BottomCenter)
                        .then(if (fullScreen) Modifier else Modifier.widthIn(max = policy.dialogWidthDp.dp))
                        .fillMaxWidth().height(sheetHeight).testTag(if (fullScreen) "tv-detail-page" else "adaptive-dialog")
                        // Translation is draw-only: it never remeasures the body or retargets
                        // an anchor. A tween cannot bounce past its final position.
                        .graphicsLayer {
                            translationY = if (fullScreen) 0f else (1f - progress.value) * if (policy.useCenteredDialog) 40.dp.toPx() else size.height
                            alpha = if (fullScreen || policy.useCenteredDialog) progress.value else 1f
                        }
                        .semantics {
                            paneTitle = dialogLabel
                            if (dismissEnabled && !closing) dismiss { close(); true }
                        },
                    shape = if (fullScreen) RoundedCornerShape(0.dp) else if (policy.useCenteredDialog) RoundedCornerShape(28.dp)
                        else RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = app.reelstack.ui.theme.Surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                        // Reading sheets should stop at the edge, not stretch artwork/text while
                        // the toolbar stays still. Keep overscroll on normal feeds unchanged.
                        CompositionLocalProvider(androidx.compose.foundation.LocalOverscrollFactory provides null,
                            LocalSheetKeyboardEntry provides keyboardEntry) {
                            content(entered, closing, close)
                        }
                    }
                }
            }
        }
    }
}
