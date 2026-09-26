package app.reelstack.ui

import androidx.annotation.StringRes
import app.reelstack.R
import java.util.Locale

/** Translate only standard provider genre names; custom library genres stay exactly as supplied. */
@StringRes
internal fun standardGenreResource(name: String): Int? = when (name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")) {
    "action" -> R.string.genre_action
    "adventure" -> R.string.genre_adventure
    "animation" -> R.string.genre_animation
    "comedy" -> R.string.genre_comedy
    "crime" -> R.string.genre_crime
    "documentary" -> R.string.genre_documentary
    "drama" -> R.string.genre_drama
    "family" -> R.string.genre_family
    "fantasy" -> R.string.genre_fantasy
    "history" -> R.string.genre_history
    "horror" -> R.string.genre_horror
    "music" -> R.string.genre_music
    "mystery" -> R.string.genre_mystery
    "romance" -> R.string.genre_romance
    "science fiction", "sci-fi", "sci fi" -> R.string.genre_science_fiction
    "thriller" -> R.string.genre_thriller
    "tv movie" -> R.string.genre_tv_movie
    "war" -> R.string.genre_war
    "western" -> R.string.genre_western
    "reality" -> R.string.genre_reality
    "reality tv" -> R.string.genre_reality_tv
    "kids" -> R.string.genre_kids
    "news" -> R.string.genre_news
    "talk" -> R.string.genre_talk
    "soap" -> R.string.genre_soap
    "sci-fi & fantasy", "science fiction & fantasy" -> R.string.genre_scifi_fantasy
    "action & adventure" -> R.string.genre_action_adventure
    "war & politics" -> R.string.genre_war_politics
    else -> null
}
