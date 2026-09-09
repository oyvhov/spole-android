package app.reelstack.data.model

enum class AccentPalette(val argb: Long, val softArgb: Long, val containerArgb: Long) {
    LIME(0xFFD5F478, 0xFFDCE9BD, 0xFF344024),
    OCEAN(0xFF96D5FF, 0xFFC8E5F7, 0xFF243B4A),
    IRIS(0xFFCAB8FF, 0xFFE1D6FA, 0xFF39304C),
    CORAL(0xFFFFB5A0, 0xFFFADACF, 0xFF4B332C);

    companion object {
        fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: LIME
    }
}

enum class ArtworkSize(val scale: Float) {
    COMPACT(.85f), STANDARD(1f), LARGE(1.2f);
    companion object {
        fun decode(value: String?) = entries.firstOrNull { it.name == value } ?: STANDARD
    }
}

/** Device-local display choices, never account permissions or credentials. */
data class Personalization(
    val accent: AccentPalette = AccentPalette.LIME,
    val artworkSize: ArtworkSize = ArtworkSize.STANDARD,
    val autoResume: Boolean = true,
)
