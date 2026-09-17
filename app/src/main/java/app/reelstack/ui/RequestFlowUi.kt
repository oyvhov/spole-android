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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import app.reelstack.background.LibraryNotifications
import app.reelstack.data.model.*
import app.reelstack.ui.components.MediaArtwork
import app.reelstack.ui.components.RequestIdentity
import app.reelstack.ui.components.SheetToolbar
import app.reelstack.ui.components.focusOutline
import app.reelstack.ui.theme.*
import app.reelstack.ui.theme.Text as TextColor

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
        // Who you are and what you are looking at is worth one screenful only once. On a series
        // the header stays put and the seasons — the thing the sheet exists for — own the height
        // that is left, so a twelve-season show scrolls its list rather than its introduction. A
        // single-title request has no list under it, so it keeps one scroll from top to bottom.
        val header: @Composable ColumnScope.() -> Unit = {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
            // A series sheet is a list you act on, so its header pays for itself in one line: the
            // toolbar already says "Seasons" and the picture is the one you just came from. A
            // single-title request has nothing below it, so it keeps the full introduction.
            val wideMedia = app.reelstack.ui.layout.WindowLayoutPolicy(maxWidth.value, maxHeight.value).useSideBySideMedia
            val posterWidth = if (isSeries) 48.dp else if (wideMedia) 132.dp else 82.dp
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaArtwork(draft.media.artworkUrl, null, Modifier.width(posterWidth).height(posterWidth * 1.5f).clip(RoundedCornerShape(10.dp)), fallbackRes = draft.media.artworkRes, ContentScale.Fit, ServiceKind.SEERR)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(draft.media.title,
                        fontSize = if (isSeries) 17.sp else 22.sp, lineHeight = if (isSeries) 22.sp else 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!isSeries) Text(stringResource(R.string.flow_movie_intro), color = Muted,
                        fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            }
            RequestIdentity(state, onAccount)
        }
        if (isSeries) Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).testTag("request-header")) { header() }
        Column(Modifier.weight(1f).testTag("request-scroll").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            if (!isSeries) {
                header()
                if (ready && state.configuredCount > 0) RequestPreflight(draft, onRetry)
                app.reelstack.ui.components.RequestJourney(null, Modifier.padding(top = 20.dp, bottom = 8.dp))
            }
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
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                draft.seasons.forEach { season ->
                    val enabled = season.canRequest && canAdd && !busy && draft.error == null && draft.mediaStatus != 6
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).defaultMinSize(minHeight = 56.dp)
                            .toggleable(season.number in draft.selected, enabled = enabled, role = Role.Checkbox,
                                onValueChange = { onSeason(season.number, it) }).testTag("request-season-${season.number}")
                            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(app.reelstack.localization.seasonDisplayName(season), fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                                Text(listOfNotNull(app.reelstack.localization.seasonDescription(season), season.episodes.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.flow_episode_count, it, it) }).joinToString(" · "),
                                    color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
                            }
                            if (season.canRequest && canAdd) Checkbox(checked = season.number in draft.selected, onCheckedChange = null, enabled = enabled)
                            else if (!season.canWatch) Icon(if (season.status == 5) app.reelstack.ui.components.SpoleIcons.DoneCircle else app.reelstack.ui.components.SpoleIcons.Info,
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
                                else Icon(if (watched) app.reelstack.ui.components.SpoleIcons.Bell else app.reelstack.ui.components.SpoleIcons.BellOff,
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
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry, enabled = !busy) { Text(stringResource(R.string.flow_recheck)) }
                }
                }
            }
            // What sending costs you is read on the way to the button, so on a series it sits
            // after the list rather than in front of it. Two sentences of reference text above the
            // seasons pushed the second one under the fold on a two-season show.
            if (isSeries && ready && state.configuredCount > 0) RequestPreflight(draft, onRetry)
            if (ready && hasSelection && canAdd) {
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = SurfaceRaised)
            Row(Modifier.fillMaxWidth().toggleable(draft.notify, enabled = !draft.sending, role = Role.Checkbox, onValueChange = onNotify)
                .padding(vertical = 8.dp).testTag("request-notification"), verticalAlignment = Alignment.CenterVertically) {
                Icon(app.reelstack.ui.components.SpoleIcons.Bell, null, tint = Primary, modifier = Modifier.size(22.dp))
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
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onRetry, enabled = !draft.sending) { Text(stringResource(R.string.flow_recheck)) }
            }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.fillMaxWidth().background(Surface).padding(horizontal = 24.dp, vertical = 12.dp)) {
            if (state.configuredCount == 0) Text(stringResource(R.string.flow_preview), color = Muted,
                fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
            if (isSeries && ready && draft.error == null && (!canAdd || draft.seasons.none { it.canRequest })) {
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onDismiss, enabled = !draft.sending, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text(stringResource(R.string.flow_done)) }
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
    val television = app.reelstack.ui.components.isTelevision()
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
                  // The poster stays a poster.
                  //
                  // The title used to be printed across the bottom of it, on top of the title the
                  // poster already carries — "Practical Magic 2" over the PRACTICAL MAGIC logo. The
                  // words now sit under the picture, where they are legible and appear once.
                  Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                      .focusOutline(detailsInteraction, artworkShape).clip(artworkShape)) {
                    MediaArtwork(item.artworkUrl, null, Modifier.fillMaxSize(), fallbackRes = app.reelstack.R.drawable.media_placeholder, ContentScale.Fit, ServiceKind.SEERR)
                    // A request and a title you only follow looked identical. This says which.
                    if (item.availabilityOnly) Text(
                        stringResource(R.string.flow_badge_following),
                        color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .72f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                  }
                  Column(Modifier.fillMaxWidth().padding(top = 9.dp)) {
                        Text(item.title, color = TextColor, fontSize = 15.sp,
                            lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text((if (item.seasons.isEmpty()) stringResource(R.string.flow_film) else stringResource(R.string.flow_season_numbers, item.seasons.sorted().joinToString(", "))) +
                            (if (item.is4k) " · 4K" else ""), color = Muted,
                            fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(vertical = 4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when (item.stage) {
                                    RequestStage.AVAILABLE -> app.reelstack.ui.components.SpoleIcons.DoneCircle
                                    RequestStage.DOWNLOADING -> app.reelstack.ui.components.SpoleIcons.Download
                                    RequestStage.FAILED, RequestStage.DECLINED -> app.reelstack.ui.components.SpoleIcons.Alert
                                    else -> app.reelstack.ui.components.SpoleIcons.Clock
                                }, null,
                                tint = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.FAILED, RequestStage.DECLINED -> Warning
                                    else -> Muted
                                }, modifier = Modifier.size(16.dp),
                            )
                            Text(
                                app.reelstack.localization.requestStageLabel(item.stage) + (item.percent?.let { " · $it %" } ?: ""),
                                color = when (item.stage) {
                                    RequestStage.AVAILABLE -> Success
                                    RequestStage.DECLINED, RequestStage.FAILED -> Warning
                                    else -> TextColor
                                },
                                fontSize = 13.sp, lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    // Under the card, not on top of the artwork. As a circle in the poster's
                    // corner it covered the picture on every card, focused or not — and on a title
                    // whose name runs along the top edge, it covered that too.
                    if (television && showActions && active) Box {
                        IconButton(onClick = { options = true }, interactionSource = notifyInteraction,
                            modifier = Modifier.focusOutline(notifyInteraction, CircleShape, glow = false)
                                .testTag("request-options-${item.key}")) {
                            Icon(app.reelstack.ui.components.SpoleIcons.Tune,
                                stringResource(R.string.activity_more_options), tint = Muted)
                        }
                        DropdownMenu(expanded = options, onDismissRequest = { options = false }) {
                            DropdownMenuItem(text = { Text(notificationLabel) }, onClick = {
                                if (!item.notify && Build.VERSION.SDK_INT >= 33 && !LibraryNotifications.allowed(context))
                                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else onNotify(!item.notify)
                                options = false
                            })
                            if (item.availabilityOnly || item.requestId != null) DropdownMenuItem(
                                text = { Text(stringResource(if (item.availabilityOnly) R.string.flow_unfollow else R.string.flow_withdraw)) },
                                enabled = !cancelling,
                                onClick = { options = false; if (item.availabilityOnly) onCancel() else confirmCancel = true })
                        }
                    }
                  }
            if (showActions && !television) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            if (item.notify) app.reelstack.ui.components.SpoleIcons.Bell else app.reelstack.ui.components.SpoleIcons.BellOff,
                            null, tint = if (item.notify) Primary else Muted,
                        )
                    }
                }
            }
            if (showActions && !television && item.availabilityOnly) {
                if (active) Text(stringResource(R.string.flow_watching_note), color = Muted, fontSize = 12.sp,
                    lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = onCancel, enabled = !cancelling,
                    modifier = Modifier.padding(start = 4.dp).testTag("remove-watch-${item.key}")) {
                    Text(if (cancelling) stringResource(R.string.flow_removing) else stringResource(R.string.flow_unfollow))
                }
            } else if (showActions && !television && active) {
                // Withdrawing is only offered once Seerr has given the request an id: without it
                // there is nothing to withdraw, and a dead button would be worse than none.
                if (item.requestId != null) {
                    app.reelstack.ui.components.SpoleSecondaryButton(onClick = { options = !options }, modifier = Modifier.testTag("request-options-${item.key}")) {
                        Text(stringResource(if (options) R.string.activity_less_options else R.string.activity_more_options))
                    }
                }
                if (item.requestId != null && options) {
                    app.reelstack.ui.components.SpoleSecondaryButton(
                        onClick = { confirmCancel = true },
                        enabled = !cancelling,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).testTag("cancel-request-${item.key}"),
                    ) {
                        if (cancelling) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Warning, strokeWidth = 2.dp)
                            Text(stringResource(R.string.flow_withdrawing), color = Muted, fontSize = 13.sp, lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 8.dp))
                        } else {
                            Icon(app.reelstack.ui.components.SpoleIcons.Close, null, tint = Warning, modifier = Modifier.size(16.dp))
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
                app.reelstack.ui.components.SpoleSecondaryButton(onClick = { confirmCancel = false; onCancel() }) {
                    Text(stringResource(R.string.flow_withdraw), color = Warning)
                }
            },
            dismissButton = { app.reelstack.ui.components.SpoleSecondaryButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.flow_keep)) } },
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
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (item.seasons.isNotEmpty() && item.availableSeasons.isNotEmpty()) {
                Text(pluralStringResource(R.plurals.flow_season_fraction, item.seasons.size, item.availableSeasons.size, item.seasons.size), color = PrimarySoft,
                    fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
