package app.reelstack.ui.components

/** Empty service metadata and explicit placeholders add no useful caption. */
internal fun readableMediaText(value: String?): String? = value?.trim()?.takeUnless {
    it.isBlank() || it.uppercase(java.util.Locale.ROOT) in setOf("TBA", "N/A", "-")
}
