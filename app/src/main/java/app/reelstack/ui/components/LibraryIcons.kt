package app.reelstack.ui.components

import app.reelstack.data.model.LibraryIcon

internal fun LibraryIcon.vector() = when(this) {
    LibraryIcon.LIBRARY -> SpoleIcons.Library
    LibraryIcon.MOVIES -> app.reelstack.ui.components.SpoleIcons.Movie
    LibraryIcon.SERIES -> app.reelstack.ui.components.SpoleIcons.Screen
    LibraryIcon.KIDS -> SpoleIcons.Kids
    LibraryIcon.DOCUMENTARY -> SpoleIcons.Documentary
    LibraryIcon.MUSIC -> SpoleIcons.Music
    LibraryIcon.CONCERT -> SpoleIcons.Concert
    LibraryIcon.ANIMATION -> SpoleIcons.Animation
    LibraryIcon.SPORT -> SpoleIcons.Sport
    LibraryIcon.FAVOURITES -> app.reelstack.ui.components.SpoleIcons.Heart
}
