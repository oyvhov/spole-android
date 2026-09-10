package app.reelstack.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.reelstack.R

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.background.LibraryNotifications
import app.reelstack.data.model.*
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*

@Composable
fun RequestComposer(state: ReelstackUiState, onSeason: (Int, Boolean) -> Unit, onNotify: (Boolean) -> Unit,
                    onConfirm: () -> Unit, onDismiss: () -> Unit, onRetry: () -> Unit, onAccount: () -> Unit,
                    onSeasonWatch: (Int, Boolean) -> Unit = { _, _ -> }, entered: Boolean = true) {
    val draft = state.requestDraft ?: return
    val context = LocalContext.current
    val confirm by rememberUpdatedState(onConfirm)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { confirm() }
    val isSeries = draft.media.mediaType == "tv"
    val ready = entered && !draft.loading
    val contentAlpha by animateFloatAsState(if (ready) 1f else 0f, tween(180), label = "season-content")
    val canAdd = state.configuredCount == 0 || state.accounts[ServiceKind.SEERR]?.canRequestType(if (isSeries) "tv" else "movie") == true
    var pendingWatch by remember(draft.media.id) { mutableStateOf<Int?>(null) }
    val watchAction by rememberUpdatedState(onSeasonWatch)
    val watchPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingWatch?.let { watchAction(it, true) }
        pendingWatch = null
    }
    val busy = draft.sending || draft.savingWatch != null
    val hasSelection = !isSeries || draft.selected.isNotEmpty()
    val television = androidx.compose.ui.platform.LocalConfiguration.current.uiMode and android.content.res.Configuration.UI_MODE_TYPE_MASK == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
    val confirmFocus = remember(draft.media.id) { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(ready, television) {
        if (television && ready && !isSeries && canAdd && draft.error == null && draft.rules?.canRequest != false && !draft.quotaExceeded) {
            androidx.compose.runtime.withFrameNanos { }; confirmFocus.requestFocus()
        }
    }
    Column(Modifier.fillMaxSize().testTag("request-composer")) {
        SheetToolbar(if (isSeries) stringResource(R.string.flow_seasons) else stringResource(R.string.flow_new_request), stringResource(R.string.flow_close_request), onDismiss, enabled = !draft.sending)
        Column(Modifier.weight(1f).testTag("request-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            val posterWidth = if (app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useSideBySideMedia) 132.dp else 82.dp
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(draft.media.artworkUrl, draft.media.artworkRes, null,
                    Modifier.width(posterWidth).height(posterWidth * 1.5f).clip(RoundedCornerShape(10.dp)), ContentScale.Fit, ServiceKind.SEERR)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(draft.media.title, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (isSeries) stringResource(R.string.flow_series_intro) else stringResource(R.string.flow_movie_intro), color = Muted,
                        fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            }
            RequestIdentity(state, onAccount)
            if (ready && state.configuredCount > 0) RequestPreflight(draft, onRetry)
            if (!isSeries) app.reelstack.ui.components.RequestJourney(null, Modifier.padding(top = 20.dp, bottom = 8.dp))
            if (!ready) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("seasons-loading"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.flow_checking), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceRaised))
                    }
                }
            } else if (isSeries) {
                Column(Modifier.fillMaxWidth().graphicsLayer { alpha = contentAlpha }) {
                draft.nextEpisode?.let { app.reelstack.localization.nextEpisodeDescription(it) }?.let { next ->
                    Text(next, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 20.dp).testTag("next-episode"))
                }
                Text(if (draft.seasons.any { it.canRequest } && canAdd) stringResource(R.string.flow_choose_only)
                    else stringResource(R.string.flow_status_source), color = Muted, fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                draft.seasons.forEach { season ->
                    val enabled = season.canRequest && canAdd && !busy && draft.error == null && draft.mediaStatus != 6
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).defaultMinSize(minHeight = 72.dp)
                            .toggleable(season.number in draft.selected, enabled = enabled, role = Role.Checkbox,
                                onValueChange = { onSeason(season.number, it) }).testTag("request-season-${season.number}")
                            .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(app.reelstack.localization.seasonDisplayName(season), fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                                Text(listOfNotNull(app.reelstack.localization.seasonDescription(season), season.episodes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.flow_episode_count, it, it) }).joinToString(" · "),
                                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (season.canRequest && canAdd) Checkbox(checked = season.number in draft.selected, onCheckedChange = null, enabled = enabled)
                            else if (!season.canWatch) Icon(if (season.status == 5) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                null, tint = if (season.status == 5) Success else Muted, modifier = Modifier.size(24.dp).padding(end = 2.dp))
                        }
                        if (season.canWatch && draft.mediaStatus != 6 && state.accounts[ServiceKind.SEERR]?.isPersonal == true) {
                            val watched = season.number in draft.watchedSeasons
                            val watchLabel = stringResource(if (watched) R.string.flow_watch_off else R.string.flow_watch_on,
                                app.reelstack.localization.seasonDisplayName(season).lowercase(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))
                            IconToggleButton(watched, enabled = !busy && draft.error == null,
                                onCheckedChange = { enabledWatch ->
                                    if (enabledWatch && state.notificationsEnabled && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context)) {
                                        pendingWatch = season.number
                                        watchPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else onSeasonWatch(season.number, enabledWatch)
                                }, modifier = Modifier.testTag("watch-season-${season.number}").semantics {
                                    contentDescription = watchLabel
                                }) {
                                if (draft.savingWatch == season.number) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(if (watched) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                                    null, tint = if (watched) Primary else Muted)
                            }
                        }
                    }
                    HorizontalDivider(color = SurfaceRaised)
                }
                if (draft.seasons.any { it.status == 4 }) {
                    Text(stringResource(R.string.flow_partial_note),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 14.dp))
                }
                if (draft.seasons.any { it.canWatch } && state.accounts[ServiceKind.SEERR]?.isPersonal == true) {
                    Text(stringResource(R.string.flow_watch_note),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
                }
                draft.watchError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 12.dp))
                    TextButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.flow_recheck)) }
                }
                }
            }
            if (ready && hasSelection && canAdd) {
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = SurfaceRaised)
            Row(Modifier.fillMaxWidth().toggleable(draft.notify, enabled = !draft.sending, role = Role.Checkbox, onValueChange = onNotify)
                .padding(vertical = 8.dp).testTag("request-notification"), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NotificationsActive, null, tint = Primary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(stringResource(R.string.flow_notify_ready), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    Text(if (isSeries) stringResource(R.string.flow_notify_seasons) else stringResource(R.string.flow_notify_movie),
                        color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Checkbox(draft.notify, onCheckedChange = null, enabled = !draft.sending)
            }
            Text(when {
                !state.notificationsEnabled -> stringResource(R.string.flow_notifications_off)
                !LibraryNotifications.allowed(context) -> stringResource(R.string.flow_android_permission)
                else -> stringResource(R.string.flow_background_note)
            }, color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (ready && isSeries && !hasSelection && draft.watchedSeasons.isNotEmpty() &&
                (!state.notificationsEnabled || !LibraryNotifications.allowed(context))) {
                Text(stringResource(R.string.flow_permission_off),
                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
            }
            draft.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 14.dp))
                TextButton(onClick = onRetry, enabled = !draft.sending) { Text(stringResource(R.string.flow_recheck)) }
            }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 24.dp, vertical = 12.dp)) {
            if (state.configuredCount == 0) Text(stringResource(R.string.flow_preview), color = Muted,
                fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
            if (isSeries && ready && draft.error == null && (!canAdd || draft.seasons.none { it.canRequest })) {
                TextButton(onClick = onDismiss, enabled = !draft.sending, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(stringResource(R.string.flow_done)) }
            } else Button(onClick = {
                if (draft.notify && state.notificationsEnabled && state.configuredCount > 0 && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context))
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else onConfirm()
            }, enabled = ready && !busy && canAdd && draft.rules?.canRequest != false && !draft.quotaExceeded && draft.error == null && hasSelection,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).focusRequester(confirmFocus).testTag("confirm-request")) {
                if (draft.sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                Text(if (draft.sending) stringResource(R.string.flow_sending) else if (isSeries && draft.selected.isEmpty()) stringResource(R.string.flow_choose_seasons)
                    else if (isSeries) pluralStringResource(R.plurals.flow_send_seasons, draft.selected.size, draft.selected.size) else stringResource(R.string.flow_send),
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
fun TrackedRequestCard(
    item: TrackedRequest,
    onDetails: () -> Unit,
    onNotify: (Boolean) -> Unit,
    onCancel: () -> Unit = {},
    cancelling: Boolean = false,
    showActions: Boolean = true,
    detailsEnabled: Boolean = true,
) {
    val context = LocalContext.current
    var confirmCancel by rememberSaveable(item.key) { mutableStateOf(false) }
    val notifyAction by rememberUpdatedState(onNotify)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifyAction(true) }
    val active = item.stage != RequestStage.AVAILABLE
    val detailsInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val notifyInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val artworkShape = RoundedCornerShape(app.reelstack.ui.theme.ReelLayout.ArtworkCorner)
    var options by rememberSaveable(item.key) { mutableStateOf(false) }
    val notificationLabel = stringResource(if (item.notify) R.string.flow_notify_on_title else R.string.flow_notify_off_title, item.title)
    Box(Modifier.widthIn(max = 300.dp).fillMaxWidth().testTag("tracked-request-${item.key}")) {
        Column {
                Column(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = detailsEnabled, interactionSource = detailsInteraction, indication = app.reelstack.ui.components.mediaCardIndication(),
                            onClickLabel = stringResource(R.string.flow_detail_named, item.title), onClick = onDetails)
                        .testTag("tracked-details-${item.key}"),
                ) {
                  Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).focusOutline(detailsInteraction, artworkShape).clip(artworkShape)) {
                    MediaArtwork(
                        item.artworkUrl, app.reelstack.R.drawable.media_placeholder, null,
                        Modifier.fillMaxSize(), ContentScale.Fit, ServiceKind.SEERR,
                    )
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to androidx.compose.ui.graphics.Color.Transparent,
                        .6f to androidx.compose.ui.graphics.Color.Transparent,
                        1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .94f))))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Icon(
                                when (item.stage) {
                                    RequestStage.AVAILABLE -> Icons.Rounded.CheckCircle
                                    RequestStage.DOWNLOADING -> Icons.Rounded.Download
                                    RequestStage.FAILED, RequestStage.DECLINED -> Icons.Rounded.ErrorOutline
                                    else -> Icons.Rounded.Schedule
                                }, null,
                                tint = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.FAILED, RequestStage.DECLINED -> Warning
                                    else -> androidx.compose.ui.graphics.Color.White
                                }, modifier = Modifier.size(16.dp),
                            )
                            Text(
                                app.reelstack.localization.requestStageLabel(item.stage) + (item.percent?.let { " · $it %" } ?: ""),
                                color = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.DECLINED, RequestStage.FAILED -> Warning
                                    else -> androidx.compose.ui.graphics.Color.White
                                },
                                fontSize = 13.sp, lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                  }
                  Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp,
                      lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                      overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                      modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp))
                  Text(
                      (if (item.seasons.isEmpty()) stringResource(R.string.flow_film) else stringResource(R.string.flow_season_numbers, item.seasons.sorted().joinToString(", "))) +
                          (if (item.is4k) " · 4K" else "") + (if (item.availabilityOnly) " · " + stringResource(R.string.flow_watch_only) else ""),
                      color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                      modifier = Modifier.padding(top = 4.dp, start = 2.dp, bottom = 8.dp),
                  )
                }
            if (showActions) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (active && !item.availabilityOnly && item.stage != RequestStage.UNKNOWN) CompactRequestProgress(item, Modifier.weight(1f).padding(end = 4.dp))
                if (active) {
                    IconToggleButton(
                        interactionSource = notifyInteraction,
                        checked = item.notify,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context)) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else onNotify(enabled)
                        },
                        modifier = Modifier.focusOutline(notifyInteraction, CircleShape).semantics {
                            contentDescription = notificationLabel
                        },
                    ) {
                        Icon(
                            if (item.notify) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            null, tint = if (item.notify) Primary else Muted,
                        )
                    }
                }
            }
            if (showActions && item.availabilityOnly) {
                if (active) Text(stringResource(R.string.flow_watching_note), color = Muted, fontSize = 12.sp,
                    lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
                TextButton(onClick = onCancel, enabled = !cancelling,
                    modifier = Modifier.padding(start = 4.dp).testTag("remove-watch-${item.key}")) {
                    Text(if (cancelling) stringResource(R.string.flow_removing) else stringResource(R.string.flow_unfollow))
                }
            } else if (showActions && active) {
                // Withdrawing is only offered once Seerr has given the request an id: without it
                // there is nothing to withdraw, and a dead button would be worse than none.
                if (item.requestId != null) {
                    TextButton(onClick = { options = !options }, modifier = Modifier.testTag("request-options-${item.key}")) {
                        Text(stringResource(if (options) R.string.activity_less_options else R.string.activity_more_options))
                    }
                }
                if (item.requestId != null && options) {
                    TextButton(
                        onClick = { confirmCancel = true },
                        enabled = !cancelling,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).testTag("cancel-request-${item.key}"),
                    ) {
                        if (cancelling) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Warning, strokeWidth = 2.dp)
                            Text(stringResource(R.string.flow_withdrawing), color = Muted, fontSize = 13.sp, lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 8.dp))
                        } else {
                            Icon(Icons.Rounded.Close, null, tint = Warning, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.flow_withdraw), color = Warning, fontSize = 13.sp, lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.flow_withdraw_title, item.title)) },
            text = {
                Text(
                    stringResource(R.string.flow_withdraw_note),
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; onCancel() }) {
                    Text(stringResource(R.string.flow_withdraw), color = Warning)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.flow_keep)) } },
            containerColor = SurfaceRaised,
            shape = RoundedCornerShape(28.dp),
        )
    }
}

@Composable
private fun CompactRequestProgress(item: TrackedRequest, modifier: Modifier = Modifier) {
    val filled = when (item.stage) {
        RequestStage.AVAILABLE -> 3
        RequestStage.DOWNLOADING, RequestStage.IMPORTING -> 2
        RequestStage.REQUESTED -> 1
        else -> 0
    }
    val progressLabel = stringResource(R.string.flow_progress, app.reelstack.localization.requestStageLabel(item.stage))
    Column(modifier.clearAndSetSemantics { contentDescription = progressLabel }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                Box(Modifier.weight(1f).height(3.dp).clip(CircleShape)
                    .background(if (index < filled) Primary else Divider))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Text(app.reelstack.localization.requestStageExplanation(item.stage), color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                maxLines = 2, modifier = Modifier.weight(1f))
            if (item.seasons.isNotEmpty() && item.availableSeasons.isNotEmpty()) {
                Text(pluralStringResource(R.plurals.flow_season_fraction, item.seasons.size, item.availableSeasons.size, item.seasons.size), color = PrimarySoft,
                    fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
