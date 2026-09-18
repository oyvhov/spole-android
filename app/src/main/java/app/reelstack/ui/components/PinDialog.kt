package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.ui.theme.*

@Composable
fun PinEntrySheet(
    title: String,
    subtitle: String,
    error: String? = null,
    lockoutSeconds: Int = 0,
    isSetup: Boolean = false,
    onPinComplete: (String) -> Unit,
    onForgotPin: ((password: String) -> Unit)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var enteredPin by remember { mutableStateOf("") }
    var setupFirstPin by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var recoveryPassword by remember { mutableStateOf("") }
    val mismatchText = stringResource(R.string.profile_pin_mismatch)

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun handlePinComplete(pin: String) {
        if (isSetup) {
            if (setupFirstPin == null) {
                setupFirstPin = pin
                enteredPin = ""
                localError = null
            } else {
                if (setupFirstPin == pin) {
                    onPinComplete(pin)
                } else {
                    localError = mismatchText
                    setupFirstPin = null
                    enteredPin = ""
                }
            }
        } else {
            onPinComplete(pin)
            enteredPin = ""
        }
    }

    val digitsDescription = stringResource(
        R.string.profile_pin_digits_entered,
        enteredPin.length,
        4,
    )

    val currentSubtitle = when {
        lockoutSeconds > 0 -> stringResource(R.string.profile_pin_locked, lockoutSeconds)
        isSetup && setupFirstPin != null -> stringResource(R.string.profile_set_pin_confirm)
        else -> subtitle
    }
    val displayError = localError ?: error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && lockoutSeconds == 0) {
                    val digit = when (event.key) {
                        Key.Zero, Key.NumPad0 -> "0"
                        Key.One, Key.NumPad1 -> "1"
                        Key.Two, Key.NumPad2 -> "2"
                        Key.Three, Key.NumPad3 -> "3"
                        Key.Four, Key.NumPad4 -> "4"
                        Key.Five, Key.NumPad5 -> "5"
                        Key.Six, Key.NumPad6 -> "6"
                        Key.Seven, Key.NumPad7 -> "7"
                        Key.Eight, Key.NumPad8 -> "8"
                        Key.Nine, Key.NumPad9 -> "9"
                        else -> null
                    }
                    if (digit != null && enteredPin.length < 4) {
                        val updated = enteredPin + digit
                        enteredPin = updated
                        if (updated.length == 4) {
                            handlePinComplete(updated)
                        }
                        return@onKeyEvent true
                    }
                    if (event.key == Key.Backspace && enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        return@onKeyEvent true
                    }
                }
                false
            }
            .testTag("pin-entry-sheet"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Primary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(44.dp).testTag("pin-close-button"),
            ) {
                Icon(SpoleIcons.Close, contentDescription = "Lukk", tint = Muted)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = currentSubtitle,
            color = if (lockoutSeconds > 0) Warning else Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.align(Alignment.Start),
        )

        if (!displayError.isNullOrBlank() && lockoutSeconds == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = displayError,
                color = Warning,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.align(Alignment.Start).testTag("pin-error-text"),
            )
        }

        Spacer(Modifier.height(24.dp))

        // 4 Digit dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .semantics { contentDescription = digitsDescription }
                .testTag("pin-dots-row"),
        ) {
            for (i in 0 until 4) {
                val filled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (filled) Primary else SurfaceRaised)
                        .border(1.5.dp, if (filled) Primary else ControlOutline, CircleShape)
                        .testTag("pin-dot-$i"),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Number Pad
        val enabled = lockoutSeconds == 0
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫"),
            )

            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    for (digit in row) {
                        PinKeyButton(
                            label = digit,
                            enabled = enabled,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (digit) {
                                    "C" -> {
                                        enteredPin = ""
                                        setupFirstPin = null
                                        localError = null
                                    }
                                    "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                    else -> {
                                        if (enteredPin.length < 4) {
                                            val updated = enteredPin + digit
                                            enteredPin = updated
                                            if (updated.length == 4) {
                                                handlePinComplete(updated)
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }

        if (onForgotPin != null && lockoutSeconds == 0) {
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = { showForgotDialog = true },
                modifier = Modifier.testTag("pin-forgot-button"),
            ) {
                Text(
                    text = stringResource(R.string.profile_pin_forgot),
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
        }
    }

    if (showForgotDialog && onForgotPin != null) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text(stringResource(R.string.profile_pin_forgot), color = Primary) },
            text = {
                Column {
                    Text(stringResource(R.string.profile_pin_forgot_hint), color = Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recoveryPassword,
                        onValueChange = { recoveryPassword = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("recovery-password-input"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotDialog = false
                        onForgotPin(recoveryPassword)
                    },
                    modifier = Modifier.testTag("recovery-confirm-button"),
                ) {
                    Text(stringResource(R.string.profile_pin_reset_action), color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Avbryt", color = Muted)
                }
            },
        )
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(SurfaceRaised)
            .border(1.dp, ControlOutline, shape)
            .focusOutline(interaction, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .testTag("pin-key-$label"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) Primary else Muted,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
