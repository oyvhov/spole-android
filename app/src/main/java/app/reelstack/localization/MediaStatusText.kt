package app.reelstack.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.reelstack.R

/** Presentation only: preserves Seerr status precedence without changing request eligibility. */
@Composable
fun localizedSeerrStatus(status: Int?, inLibrary: Boolean = false, requested: Boolean = false): String =
    stringResource(when {
        status == 6 -> R.string.status_blocked
        status == 5 || inLibrary -> R.string.status_available
        status == 4 -> R.string.status_partial
        status == 3 -> R.string.status_requested
        status == 2 -> R.string.status_pending
        requested -> R.string.status_added
        status == 7 -> R.string.status_removed
        else -> R.string.status_requestable
    })
