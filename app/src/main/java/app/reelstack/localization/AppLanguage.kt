package app.reelstack.localization

/** Persist stable tags, never a translated label. Adding a locale must not change stored values. */
enum class AppLanguage(val tag: String, val nativeName: String) {
    SYSTEM("", ""),
    NYNORSK("nn", "Norsk nynorsk"),
    ENGLISH("en", "English");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM

        fun initial(saved: String?, existingInstallation: Boolean): AppLanguage =
            if (saved != null) fromTag(saved) else if (existingInstallation) NYNORSK else SYSTEM
    }
}
