package app.reelstack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.R
import app.reelstack.data.model.UserProfile
import app.reelstack.ui.theme.*
import coil3.compose.AsyncImage

@Composable
fun ProfileSwitcher(
    profiles: List<UserProfile>,
    activeProfileId: String,
    isKidMode: Boolean,
    onSelectProfile: (UserProfile) -> Unit,
    onAddProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteProfile: (UserProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("profile-switcher"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.profile_switch_title),
                color = Primary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(profiles, key = { it.id }) { profile ->
                ProfileRow(
                    profile = profile,
                    isSelected = profile.id == activeProfileId,
                    isKidMode = isKidMode,
                    onClick = { onSelectProfile(profile) },
                    onDelete = { onDeleteProfile(profile) },
                )
            }

            if (!isKidMode) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ProfileActionButton(
                        icon = SpoleIcons.Add,
                        label = stringResource(R.string.profile_add_action),
                        testTag = "profile-action-add",
                        onClick = onAddProfile,
                    )
                }
                item {
                    ProfileActionButton(
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

@Composable
private fun ProfileRow(
    profile: UserProfile,
    isSelected: Boolean,
    isKidMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(shape)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else SurfaceRaised)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Primary else ControlOutline,
                shape = shape,
            )
            .focusOutline(interaction, shape)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("profile-row-${if (profile.isMain) "main" else profile.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!profile.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val initial = profile.name.trim().firstOrNull()?.uppercase()
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
                text = profile.name,
                color = Primary,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = if (profile.isMain) stringResource(R.string.profile_main) else stringResource(R.string.profile_kids_section),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
        }

        if (!isKidMode && !profile.isMain && !isSelected) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Slett ${profile.name}" }
                    .testTag("delete-profile-${profile.id}"),
            ) {
                Icon(SpoleIcons.Delete, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = Muted,
            ),
        )
    }
}

@Composable
private fun ProfileActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, ControlOutline, shape)
            .focusOutline(interaction, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            color = Primary,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    }
}
