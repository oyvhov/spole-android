package app.reelstack.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun MediaArtwork(
    url: String?,
    @DrawableRes fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val fallback = painterResource(fallbackRes)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url ?: fallbackRes)
            .crossfade(260)
            .build(),
        contentDescription = contentDescription,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = contentScale,
        modifier = modifier,
    )
}
