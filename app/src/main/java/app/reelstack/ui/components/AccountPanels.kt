package app.reelstack.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import app.reelstack.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reelstack.data.model.ServiceAccount
import app.reelstack.data.model.ServiceConnection
import app.reelstack.data.model.ServiceKind
import app.reelstack.ui.ReelstackUiState
import app.reelstack.ui.theme.Caution
import app.reelstack.ui.theme.Muted
import app.reelstack.ui.theme.Primary
import app.reelstack.ui.theme.SurfaceRaised
import app.reelstack.ui.theme.Text as TextColor

/**
 * Shows who you are signed in as. It deliberately does not appear before anything is configured:
 * with no connections every row here duplicated the "Tenestene dine" list below it, and the two
 * looked like different things that led to the same sheet.
 */
@Composable
fun SettingsAccounts(
    state: ReelstackUiState,
    onConnectionClick: (ServiceKind) -> Unit,
    /** Off where the page already wrote the section heading in its own style. */
    heading: Boolean = true,
) {
    val configured = listOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY)
        .filter { kind -> state.connections.any { it.kind == kind && it.baseUrl.isNotBlank() } }
    if (configured.isEmpty()) return
    Column(Modifier.padding(top = 20.dp)) {
        if (heading) Text(stringResource(R.string.flow_account_title), color = TextColor,
            fontSize = 21.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
        Surface(color = SurfaceRaised, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("settings-accounts"),
        ) {
          Column {
            configured.forEach { kind ->
                SettingsAccountPanel(state, kind, compact = kind != ServiceKind.SEERR,
                    onClick = { onConnectionClick(kind) })
            }
          }
        }
    }
}

@Composable
private fun SettingsAccountPanel(state: ReelstackUiState, source: ServiceKind, compact: Boolean = false, onClick: () -> Unit) {
    val connection = state.connections.firstOrNull { it.kind == source }
    val loading = source in state.loadingAccounts
    val hasError = source in state.accountErrors
    val account = state.verifiedPanelAccount(source)
    val isSeerr = source == ServiceKind.SEERR
    val overviewOnly = isSeerr && (account?.isPersonal == false || connection.isSeerrApiKey())

    Surface(
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("settings-account-${source.name.lowercase()}"),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (compact && account == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ServiceSymbol(source, Modifier.size(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(source.displayName, color = TextColor, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (loading) stringResource(R.string.flow_account_loading) else if (hasError) stringResource(R.string.flow_account_saved_unverified) else stringResource(R.string.flow_signed_out),
                            color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    if (!loading) AccountAction(onClick,
                        stringResource(if (hasError) R.string.flow_retry else R.string.flow_login), source)
                }
                return@Column
            }
            if (account != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (compact) ServiceSymbol(source, Modifier.size(24.dp))
                    else AccountAvatar(account, connection, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (compact) source.displayName else account.displayName,
                            color = TextColor,
                            fontSize = if (isSeerr) 21.sp else 16.sp,
                            lineHeight = if (isSeerr) 26.sp else 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(if (compact) account.displayName else account.source.displayName, color = Muted, fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 3.dp))
                        if (account.isAdmin && !compact) Text(stringResource(R.string.flow_admin), color = Primary, fontSize = 11.sp, lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 5.dp))
                    }
                    if (!isSeerr || (account.isPersonal && !overviewOnly)) {
                        AccountEditButton(onClick, stringResource(R.string.flow_account_edit, source.displayName))
                    }
                }
            } else {
                // Every row in this card puts its action in the same place: trailing, on the row.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (loading) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        ServiceSymbol(source, Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(source.displayName, color = TextColor, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            when {
                                loading -> stringResource(R.string.flow_account_loading)
                                hasError -> stringResource(R.string.flow_account_error)
                                overviewOnly -> stringResource(R.string.flow_overview_only)
                                else -> stringResource(R.string.flow_signed_out)
                            },
                            color = if (hasError && !loading) Caution else Muted,
                            fontSize = 13.sp, lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (!loading) {
                        AccountAction(onClick, if (hasError) stringResource(R.string.flow_retry) else stringResource(R.string.flow_login), source)
                    }
                }
            }

            if (overviewOnly && account != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.flow_overview_only), color = Muted, fontSize = 12.sp,
                        lineHeight = 18.sp, modifier = Modifier.weight(1f))
                    if (!loading) AccountAction(onClick, stringResource(R.string.flow_login), source)
                }
            }
        }
    }
}

/**
 * Several rows in this card carry the same visible label, so the spoken label names the service
 * the button actually belongs to.
 */
@Composable
private fun AccountAction(onClick: () -> Unit, label: String, source: ServiceKind) {
    val spokenLabel = stringResource(R.string.flow_service_action, label, source.displayName)
    TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).semantics {
            contentDescription = spokenLabel
        },
    ) {
        Text(label, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun RequestIdentity(state: ReelstackUiState, onSignIn: () -> Unit) {
    val source = ServiceKind.SEERR
    val connection = state.connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() } ?: return
    val loading = source in state.loadingAccounts
    val hasError = source in state.accountErrors
    val account = state.verifiedPanelAccount(source)
    val overviewOnly = account?.isPersonal == false || connection.isSeerrApiKey()
    val personalAccount = account?.takeIf { it.isPersonal && !overviewOnly }

    Surface(
        color = SurfaceRaised,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("request-identity"),
    ) {
        if (personalAccount != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            ) {
                AccountAvatar(personalAccount, connection, Modifier.size(28.dp))
                Text(stringResource(R.string.flow_as_person, personalAccount.displayName), color = TextColor, fontSize = 13.sp, lineHeight = 19.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                AccountEditButton(onSignIn, stringResource(R.string.flow_request_account_edit))
            }
        } else {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (loading) {
                        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        when {
                            loading -> stringResource(R.string.flow_request_account_loading)
                            hasError -> stringResource(R.string.flow_seerr_account_error)
                            overviewOnly -> stringResource(R.string.flow_overview_only)
                            else -> stringResource(R.string.flow_personal_login)
                        },
                        color = if (hasError && !loading) Caution else Muted,
                        fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f),
                    )
                }
                if (!loading) {
                    if (hasError || overviewOnly) {
                        Text(
                            if (hasError) stringResource(R.string.flow_open_login)
                            else stringResource(R.string.flow_jellyfin_identity),
                            color = Muted, fontSize = 12.sp, lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    TextButton(onClick = onSignIn, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (hasError) stringResource(R.string.flow_retry) else stringResource(R.string.flow_jellyfin_login), fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountEditButton(onClick: () -> Unit, description: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(app.reelstack.ui.components.SpoleIcons.Edit, contentDescription = description, tint = Primary, modifier = Modifier.size(18.dp))
    }
}

// Keep verified profiles stable during refresh; the ViewModel clears them when credentials change.
fun ReelstackUiState.verifiedPanelAccount(source: ServiceKind): ServiceAccount? =
    accounts[source]?.takeIf {
        source !in accountErrors && it.source == source && it.displayName.isNotBlank()
    }

/** Prefer a verified personal identity, never the owner represented by a shared API key. */
fun ReelstackUiState.preferredHomeAccount(): ServiceAccount? =
    listOf(ServiceKind.SEERR, ServiceKind.JELLYFIN, ServiceKind.EMBY).firstNotNullOfOrNull { source ->
        val connection = connections.firstOrNull { it.kind == source && it.baseUrl.isNotBlank() && it.token.isNotBlank() }
        verifiedPanelAccount(source)?.takeIf { it.isPersonal && connection != null && !connection.isSeerrApiKey() }
    }

private fun ServiceConnection?.isSeerrApiKey(): Boolean =
    this != null && kind == ServiceKind.SEERR && baseUrl.isNotBlank() && token.isNotBlank() && !sessionCookie
