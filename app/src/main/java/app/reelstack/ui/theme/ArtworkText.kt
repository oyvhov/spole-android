package app.reelstack.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The three text colours over artwork: heroes, poster captions and the lines on a picture.
 *
 * Solid values, no alpha. Alpha changes its effect as soon as something lies beneath: white at
 * 60 % over a dark scrim is a calm secondary line, and over a bright poster it is a smudge, so the
 * line meant to be quiet becomes unreadable on exactly the posters that are bright. A solid grey is
 * the same grey on every poster. These are not `Muted`, which follows the chosen mood and belongs on
 * a surface; text on a picture is neutral, because the picture is not the mood's.
 *
 * Contrast on the app's ink: 18:1, 12:1 and 6.5:1. The quietest level is the system grey (#989898),
 * and it stays at 4.5:1 or better on the lightest dark surface a caption can land on.
 */
/** Titles and anything the eye should find first. */
val ArtworkTextPrimary = Color(0xFFFFFFFF)

/** Synopsis, episode captions, ratings: the second line under a title. */
val ArtworkTextSecondary = Color(0xFFCCCCCC)

/** Facts such as year and runtime, and the dots between them. */
val ArtworkTextQuiet = Color(0xFF989898)
