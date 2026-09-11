package app.reelstack.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import app.reelstack.data.model.LibraryIcon

internal fun LibraryIcon.vector() = when(this) {
    LibraryIcon.LIBRARY -> SpoleIcons.Library
    LibraryIcon.MOVIES -> app.reelstack.ui.components.SpoleIcons.Movie
    LibraryIcon.SERIES -> app.reelstack.ui.components.SpoleIcons.Screen
    LibraryIcon.KIDS -> Icons.Rounded.ChildCare
    LibraryIcon.DOCUMENTARY -> Icons.Rounded.Public
    LibraryIcon.MUSIC -> Icons.Rounded.MusicNote
    LibraryIcon.CONCERT -> Icons.Rounded.Mic
    LibraryIcon.ANIMATION -> Icons.Rounded.AutoAwesome
    LibraryIcon.SPORT -> Icons.Rounded.SportsSoccer
    LibraryIcon.FAVOURITES -> Icons.Rounded.FavoriteBorder
}
