package app.reelstack.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.network.PublicUser
import app.reelstack.ui.components.SpoleIcons
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

@Composable
fun AddProfileSheet(
    publicUsers: List<PublicUser>,
    loading: Boolean,
    error: String? = null,
    onAddUser: (PublicUser, password: String) -> Unit,
    onAddManual: (username: String, password: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordTargetUser by remember { mutableStateOf<PublicUser?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showManualForm by remember { mutableStateOf(publicUsers.isEmpty()) }
    var manualUsername by remember { mutableStateOf("") }
    var manualPassword by remember { mutableStateOf("") }

    LaunchedEffect(publicUsers) {
        if (publicUsers.isEmpty()) {
            showManualForm = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("add-profile-sheet"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_add_title),
                color = Primary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(44.dp).testTag("add-profile-close-button"),
            ) {
                Icon(SpoleIcons.Close, contentDescription = "Lukk", tint = Muted)
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.profile_add_subtitle),
            color = Muted,
            fontSize = 14.sp,
        )

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = Warning,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.testTag("add-profile-error"),
            )
        }

        Spacer(Modifier.height(18.dp))

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(36.dp))
            }
        } else if (publicUsers.isEmpty() || showManualForm) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (publicUsers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_add_no_users),
                        color = Muted,
                        fontSize = 14.sp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.profile_add_manual_title),
                        color = Primary,
                        fontSize = 16.sp,
                    )
                }

                OutlinedTextField(
                    value = manualUsername,
                    onValueChange = { manualUsername = it },
                    label = { Text(stringResource(R.string.profile_add_username_label)) },
                    singleLine = true,
                    colors = profileFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("add-profile-manual-username"),
                )

                OutlinedTextField(
                    value = manualPassword,
                    onValueChange = { manualPassword = it },
                    label = { Text(stringResource(R.string.profile_add_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = profileFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("add-profile-manual-password"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (publicUsers.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showManualForm = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted),
                            border = BorderStroke(1.dp, ControlOutline),
                        ) {
                            Text("Avbryt")
                        }
                    }

                    Button(
                        onClick = {
                            if (manualUsername.isNotBlank()) {
                                onAddManual(manualUsername.trim(), manualPassword)
                            }
                        },
                        enabled = manualUsername.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.testTag("add-profile-manual-submit"),
                    ) {
                        Text(stringResource(R.string.profile_add_submit_button))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(publicUsers, key = { it.id }) { user ->
                    PublicUserRow(
                        user = user,
                        onClick = {
                            if (user.hasPassword) {
                                passwordInput = ""
                                passwordTargetUser = user
                            } else {
                                onAddUser(user, "")
                            }
                        },
                    )
                }
                item {
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(12.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clip(shape)
                            .border(1.dp, ControlOutline, shape)
                            .focusOutline(interaction, shape)
                            .clickable(interactionSource = interaction, indication = null) {
                                showManualForm = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("add-profile-manual-toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_add_manual_title),
                            color = Primary,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }

    // Password input dialog for users that have a password
    passwordTargetUser?.let { target ->
        AlertDialog(
            onDismissRequest = { passwordTargetUser = null },
            title = {
                Text(
                    text = stringResource(R.string.profile_add_password_required, target.name),
                    color = Primary,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("Passord", color = Muted) },
                        modifier = Modifier.fillMaxWidth().testTag("add-profile-password-input"),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pass = passwordInput
                        passwordTargetUser = null
                        onAddUser(target, pass)
                    },
                    modifier = Modifier.testTag("add-profile-confirm-password-button"),
                ) {
                    Text("Logg inn", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { passwordTargetUser = null }) {
                    Text("Avbryt", color = Muted)
                }
            },
        )
    }
}

@Composable
private fun PublicUserRow(
    user: PublicUser,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .background(SurfaceRaised)
            .border(1.dp, ControlOutline, shape)
            .focusOutline(interaction, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("public-user-row-${user.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!user.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val initial = user.name.trim().firstOrNull()?.uppercase()
                if (initial != null) {
                    Text(initial, color = Primary, fontSize = 20.sp, lineHeight = 24.sp)
                } else {
                    Icon(SpoleIcons.Person, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = Primary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            )
            if (user.hasPassword) {
                Text(
                    text = "Krev passord",
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        if (user.hasPassword) {
            Icon(
                imageVector = SpoleIcons.Lock,
                contentDescription = "Passordbeskytta",
                tint = Muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary.copy(alpha = 0.72f),
    unfocusedBorderColor = ControlOutline,
    disabledBorderColor = ControlOutline.copy(alpha = 0.6f),
    focusedContainerColor = SurfaceRaised,
    unfocusedContainerColor = SurfaceRaised,
    disabledContainerColor = SurfaceRaised,
    cursorColor = Primary,
    focusedTextColor = Primary,
    unfocusedTextColor = Primary,
)
