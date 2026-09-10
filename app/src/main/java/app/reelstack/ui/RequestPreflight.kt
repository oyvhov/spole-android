package app.reelstack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.reelstack.R
import app.reelstack.data.model.RequestDraft
import app.reelstack.data.model.quotaExceeded

@Composable
internal fun RequestPreflight(draft: RequestDraft, onRetry: () -> Unit) {
    val rules = draft.rules
    val quota = rules?.quota
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).testTag("request-preflight"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(when {
            rules == null -> R.string.rules_unknown
            !rules.canRequest -> R.string.rules_no_permission
            rules.approvalRequired -> R.string.rules_approval_required
            else -> R.string.rules_automatic_approval
        }), style = MaterialTheme.typography.bodyMedium)
        Text(when {
            quota?.unlimited == true -> stringResource(R.string.rules_unlimited)
            quota?.remaining != null -> pluralStringResource(
                if (draft.media.mediaType == "tv") R.plurals.rules_seasons_remaining else R.plurals.rules_movies_remaining,
                quota.remaining, quota.remaining)
            else -> stringResource(R.string.rules_quota_unknown)
        }, style = MaterialTheme.typography.bodyMedium)
        if (quota?.limit != null && quota.limit > 0 && quota.days != null) {
            Text(if (quota.days == 0) stringResource(R.string.rules_no_period)
                else pluralStringResource(R.plurals.rules_period, quota.days, quota.days), style = MaterialTheme.typography.bodySmall)
        }
        if (draft.quotaExceeded) Text(stringResource(if (draft.media.mediaType == "tv") R.string.rules_exceeded else R.string.rules_movie_exhausted), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("request-quota-exceeded"))
        if (quota == null || draft.quotaExceeded) TextButton(onClick = onRetry, enabled = !draft.sending && draft.savingWatch == null) {
            Text(stringResource(R.string.rules_recheck))
        }
    }
}
