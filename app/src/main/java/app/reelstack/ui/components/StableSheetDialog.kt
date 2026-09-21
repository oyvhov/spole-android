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
import kotlinx.coroutines.withTimeoutOrNull

internal val LocalSheetKeyboardEntry = staticCompositionLocalOf { false }

/** Entrance and exit durations, and the bounds that keep either from becoming a focus trap. */
internal const val ENTER_MS = 320
internal const val EXIT_MS = 200
private const val ENTER_TIMEOUT_MS = 900L
private const val EXIT_TIMEOUT_MS = 600L

/**
 * Whether this particular request is allowed to take the sheet away.
 *
 * Two different gestures arrive here and they are not the same promise. An outside tap and the
 * toolbar button are *guarded*: a sheet that is committing a Seerr request has good reason to
 * refuse them. Back is not guarded, because it is the only way out of a full-screen modal, and a
 * request that never returns would otherwise strand the reader in the sheet for good.
 *
 * [closing] latches. That is deliberate — a second exit animation over the first would be a mess —
 * but it is also what turned a lost frame into a permanent trap once, so [runSheetExit] guarantees
 * the dismissal that clears it.
 */
internal fun sheetShouldLeave(guarded: Boolean, canDismiss: Boolean, closing: Boolean): Boolean =
    !closing && (!guarded || canDismiss)

/**
 * Plays the exit and then dismisses, whatever the exit does.
 *
 * On 16 September 2026 Spole was found on the TV home screen behind a `DIM_BEHIND` dialog window
 * that was focused, drew nothing and ignored Back. The dismissal was the *last statement after* the
 * animation, so an animation that was cancelled, or that waited on a frame clock which was not
 * running, simply never reached it — while `closing` stayed latched and swallowed every later press.
 *
 * `finally` covers all three endings: finished, timed out, and cancelled. The timeout is the second
 * half of the same promise — an animation that hangs cannot hold the dismissal hostage either.
 */
internal suspend fun runSheetExit(
    timeoutMs: Long = EXIT_TIMEOUT_MS,
    dismiss: () -> Unit,
    animateOut: suspend () -> Unit,
) {
    try {
        withTimeoutOrNull(timeoutMs) { animateOut() }
    } finally {
        dismiss()
    }
}

/** A reading modal: one measured surface, one non-overshooting entrance, no drag anchors. */
@Composable
internal fun StableSheetDialog(
    dismissEnabled: Boolean,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
    onCloseStarted: () -> Unit = {},
    /**
     * Handles a logical back step inside a sheet, for example series → the episode it came from.
     * Returning true keeps this dialog alive; only a real dismissal is allowed to start its exit
     * animation.  That distinction matters on TV, where a full-screen sheet at alpha zero exposes
     * the page behind it for a frame.
     */
    onBack: (() -> Boolean)? = null,
    content: @Composable (entered: Boolean, closing: Boolean, close: () -> Unit) -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var entered by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val canDismiss by rememberUpdatedState(dismissEnabled)
    val dismissLatest by rememberUpdatedState(onDismiss)
    val closeStartedLatest by rememberUpdatedState(onCloseStarted)
    val backLatest by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()
    val hostView = LocalView.current
    val openerInput = LocalInputModeManager.current
    // Dialog owns a new Android view; its initial input mode can differ from the opener.
    val keyboardEntry = remember { openerInput.inputMode == InputMode.Keyboard }
    val dismissLabel = stringResource(R.string.sheet_dismiss)
    val dialogLabel = stringResource(R.string.sheet_title)
    // Leaving is not allowed to depend on an animation finishing.
    //
    // The sheet is a real dialog window: it holds focus for as long as it is composed. When the
    // exit animation was the thing that called `onDismiss`, a cancelled or never-scheduled
    // animation left the window in place with `closing` latched true — invisible, because the
    // surface's alpha follows the same progress value, and deaf, because every later Back press hit
    // the same latch. The dismissal now happens in `finally`, so a lost frame clock, a cancelled
    // scope or a composition going away all still take the sheet with them.
    val leave = leave@{ guarded: Boolean ->
        if (!sheetShouldLeave(guarded = guarded, canDismiss = canDismiss, closing = closing)) return@leave
        closing = true
        closeStartedLatest()
        scope.launch {
            runSheetExit(dismiss = { dismissLatest() }) { progress.animateTo(0f, tween(EXIT_MS)) }
        }
        Unit
    }
    // What the content and the scrim call. A sheet that is committing something still refuses this.
    val close = { leave(true) }
    // Dismiss, whether it comes from system Back or accessibility, has the same escape guarantee.
    val backOrLeave = {
        if (backLatest?.invoke() != true) leave(false)
    }
    LaunchedEffect(Unit) {
        // Android honours the user's system touch-feedback preference.
        hostView.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        // Bounded on purpose. If the window is composed while it cannot draw, `animateTo` waits on
        // a frame clock that is not running, and an unbounded wait here is exactly what produced a
        // fully transparent sheet that still owned the focus. Past the bound the sheet simply
        // appears, which is always better than a modal nobody can see.
        withTimeoutOrNull(ENTER_TIMEOUT_MS) {
            progress.animateTo(1f, tween(ENTER_MS, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
        }
        if (progress.value < 1f && !closing) progress.snapTo(1f)
        entered = true
    }
    // The last line of defence. If the host state did not actually drop the sheet after the
    // dismissal ran, the window would stay — transparent, focused and, with `closing` latched,
    // deaf to Back. Rather than leave that, put the sheet back on screen: a visible modal the user
    // can read and close beats an invisible one they cannot.
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        kotlinx.coroutines.delay(EXIT_TIMEOUT_MS + 400)
        closing = false
        progress.snapTo(1f)
        entered = true
    }
    Dialog(
        // Back is not a suggestion. Outside taps and the toolbar button still respect a sheet that
        // is committing something, but the remote's Back key is the one way out that must work in
        // every state — a request that is already on its way finishes on its own.
        onDismissRequest = backOrLeave,
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
                            if (!closing) dismiss { backOrLeave(); true }
                        },
                    shape = if (fullScreen) RoundedCornerShape(0.dp) else if (policy.useCenteredDialog) RoundedCornerShape(28.dp)
                        else RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = app.reelstack.ui.theme.Surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
                        if (fullScreen) SeasonalBackdrop(Modifier.matchParentSize())
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
