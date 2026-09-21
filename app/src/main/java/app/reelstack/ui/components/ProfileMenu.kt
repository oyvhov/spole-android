package app.reelstack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.reelstack.R
import app.reelstack.data.model.UserProfile
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

/**
 * Profiles fall out of the account picture rather than arriving as a sheet from the bottom.
 *
 * Switching who is watching is a small, local act — it belongs to the corner you pressed, not to a
 * panel that takes the whole screen. The menu scales open from its own top-right corner so the
 * motion reads as the avatar unfolding, and each row fades in just behind the one above it.
 */
@Composable
fun ProfileMenu(
    profiles: List<UserProfile>,
    activeProfileId: String,
    isKidMode: Boolean,
    /**
     * The signed-in person's own name, from the media account rather than the connection.
     *
     * `ConnectionRepository` stores the server's nickname — "Heimetenar" here — which names the
     * machine, not whoever is watching. The menu asks who is watching, so it says Øyvind.
     */
    mainAccountName: String? = null,
    mainAccountAvatarUrl: String? = null,
    onSelectProfile: (UserProfile) -> Unit,
    onAddProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteProfile: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visible.targetState = true }

    // A television draws to the panel edge, so the menu keeps clear of the overscan the way every
    // other screen in the app does.
    val television = (androidx.compose.ui.platform.LocalConfiguration.current.uiMode and
        android.content.res.Configuration.UI_MODE_TYPE_MASK) ==
        android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val edge = if (television) 48.dp else 12.dp
    val top = if (television) 96.dp else 8.dp

    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(160)) +
                scaleIn(
                    animationSpec = tween(220, easing = LinearOutSlowInEasing),
                    initialScale = 0.86f,
                    // The corner the avatar sits in. Everything unfolds from that point.
                    transformOrigin = TransformOrigin(1f, 0f),
                ) +
                slideInVertically(tween(220, easing = LinearOutSlowInEasing)) { -it / 10 },
            exit = fadeOut(tween(110)) + scaleOut(tween(110), targetScale = 0.92f),
        ) {
            Surface(
                modifier = modifier
                    .padding(top = top, end = edge)
                    .widthIn(min = 280.dp, max = 340.dp)
                    .testTag("profile-menu"),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), RoundedCornerShape(26.dp))
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.profile_switch_title),
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
                    )

                    profiles.forEachIndexed { index, profile ->
                        val resolved = if (profile.isMain && !isKidMode) {
                            profile.copy(
                                name = mainAccountName?.takeIf { it.isNotBlank() } ?: profile.name,
                                avatarUrl = profile.avatarUrl ?: mainAccountAvatarUrl,
                            )
                        } else {
                            profile
                        }
                        StaggeredRow(index = index) {
                            ProfileMenuRow(
                                profile = resolved,
                                selected = profile.id == activeProfileId,
                                canDelete = !isKidMode && !profile.isMain && profile.id != activeProfileId,
                                onClick = { onSelectProfile(profile) },
                                onDelete = { onDeleteProfile(profile) },
                            )
                        }
                    }

                    if (!isKidMode) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Divider),
                        )
                        Spacer(Modifier.height(8.dp))

                        StaggeredRow(index = profiles.size) {
                            ProfileMenuAction(
                                icon = SpoleIcons.Add,
                                label = stringResource(R.string.profile_add_action),
                                testTag = "profile-action-add",
                                onClick = onAddProfile,
                            )
                        }
                        StaggeredRow(index = profiles.size + 1) {
                            ProfileMenuAction(
                                icon = SpoleIcons.Settings,
                                label = stringResource(R.string.home_account_settings),
                                testTag = "profile-action-settings",
                                onClick = onOpenSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Keep utility-popup content stable so focus and screenshots never race delayed rows. */
@Composable
private fun StaggeredRow(index: Int, content: @Composable () -> Unit) {
    content()
}

@Composable
private fun ProfileMenuRow(
    profile: UserProfile,
    selected: Boolean,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .background(if (selected || focused) MaterialTheme.colorScheme.surfaceVariant else Surface)
            .focusOutline(interaction, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("profile-row-${if (profile.isMain) "main" else profile.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileOrb(profile = profile, selected = selected)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            RolePill(
                text = if (profile.isMain) {
                    stringResource(R.string.profile_role_main)
                } else {
                    stringResource(R.string.profile_role_kid)
                },
            )
        }

        if (canDelete) {
            val deleteDescription = stringResource(R.string.profile_delete_action)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onDelete)
                    .semantics { contentDescription = deleteDescription }
                    .testTag("delete-profile-${profile.id}"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SpoleIcons.Delete, contentDescription = null, tint = Muted, modifier = Modifier.size(19.dp))
            }
        } else if (selected) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    SpoleIcons.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * The account picture as a planet: the portrait inside one ring in the accent.
 *
 * The ring is the whole decoration. No moon, no glow, no second colour — the plan asks for the
 * kid's one accent and nothing beyond it.
 */
@Composable
private fun ProfileOrb(profile: UserProfile, selected: Boolean) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 56.dp else 50.dp)
                .clip(CircleShape)
                .background(SurfaceRaised)
                .then(if (selected) Modifier.border(2.5.dp, Primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                val initial = profile.name.trim().firstOrNull()?.uppercase()
                if (initial != null) {
                    Text(initial, color = Primary, fontSize = 22.sp, lineHeight = 26.sp)
                } else {
                    Icon(SpoleIcons.Person, contentDescription = null, tint = Muted, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** A quiet capsule for the role, so the person's name owns the row on its own. */
@Composable
private fun RolePill(text: String) {
    Text(
        text = text.uppercase(),
        color = Muted,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceRaised)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ProfileMenuAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(if (focused) MaterialTheme.colorScheme.surfaceVariant else Surface)
            .focusOutline(interaction, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, lineHeight = 21.sp)
    }
}
