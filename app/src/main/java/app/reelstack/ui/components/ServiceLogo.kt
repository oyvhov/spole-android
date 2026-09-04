package app.reelstack.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import app.reelstack.R
import app.reelstack.data.model.ServiceKind

@Composable
fun ServiceLogo(
    kind: ServiceKind,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val drawable = when (kind) {
        ServiceKind.JELLYFIN -> R.drawable.ic_service_jellyfin
        ServiceKind.EMBY -> R.drawable.ic_service_emby
        else -> R.drawable.ic_launcher
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
