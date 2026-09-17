package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.data.model.PlaybackSession

/**
 * What a live session says about itself, in the reader's language.
 *
 * The parser used to write "20 min att" and "Direkteavspeling" straight into the model, which made
 * the Now playing card the one place an English installation still met nynorsk. It carries the
 * minute count and a plain flag now, and the words are chosen here — once, so the home rail, the
 * compact card and the remote sheet cannot drift apart.
 */
@Composable
fun sessionTimeLeft(session: PlaybackSession): String =
    if (session.remainingMinutes > 0) {
        pluralStringResource(R.plurals.session_time_left, session.remainingMinutes, session.remainingMinutes)
    } else {
        stringResource(R.string.session_finishing_soon)
    }

@Composable
fun sessionMethod(session: PlaybackSession): String = stringResource(
    if (session.transcoding) R.string.session_method_transcode else R.string.session_method_direct,
)

/** The person and the device, with a word for whatever the server left out. */
@Composable
fun sessionWho(session: PlaybackSession): String =
    session.userName.ifBlank { stringResource(R.string.session_unknown_person) }

@Composable
fun sessionDevice(session: PlaybackSession): String =
    session.deviceName.ifBlank { stringResource(R.string.session_unknown_device) }
