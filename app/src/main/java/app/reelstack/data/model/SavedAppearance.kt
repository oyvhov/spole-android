package app.reelstack.data.model

import kotlinx.serialization.json.*

/** Store only visual choices. Loading a look must never change navigation, accounts or playback. */
data class SavedAppearance(val name: String, val values: Map<String, String>) {
    fun applyTo(base: Personalization): Personalization = base.copy(
        accent = AccentPalette.decode(values["accent"]), visualTheme = VisualTheme.decode(values["theme"]),
        artworkSize = ArtworkSize.decode(values["size"]), artworkCorners = ArtworkCorners.decode(values["corners"]),
        focusStyle = FocusStyle.decode(values["focus"]), highContrast = values["contrast"].toBoolean(),
        reduceMotion = values["motion"].toBoolean(), seasonalOrnament = values["ornament"].toBoolean(),
        detailBackdrop = values["backdrop"].toBoolean(), heroCompact = values["compact"].toBoolean(),
        heroLogo = values["logo"].toBoolean(), heroRotate = values["rotate"].toBoolean())
    companion object {
        fun capture(name: String, value: Personalization) = SavedAppearance(name.trim().take(32), mapOf(
            "accent" to value.accent.name, "theme" to value.visualTheme.name, "size" to value.artworkSize.name,
            "corners" to value.artworkCorners.name, "focus" to value.focusStyle.name,
            "contrast" to value.highContrast.toString(), "motion" to value.reduceMotion.toString(),
            "ornament" to value.seasonalOrnament.toString(), "backdrop" to value.detailBackdrop.toString(),
            "compact" to value.heroCompact.toString(), "logo" to value.heroLogo.toString(), "rotate" to value.heroRotate.toString()))
    }
}

fun encodeAppearances(items: List<SavedAppearance>): String = buildJsonArray {
    items.filter { it.name.isNotBlank() }.distinctBy { it.name }.take(12).forEach { item -> add(buildJsonObject {
        put("name", item.name); put("values", buildJsonObject { item.values.forEach { (k, v) -> put(k, v) } })
    }) }
}.toString()

fun decodeAppearances(raw: String?): List<SavedAppearance> = runCatching {
    Json.parseToJsonElement(raw ?: "[]").jsonArray.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val name = (obj["name"] as? JsonPrimitive)?.contentOrNull?.trim()?.take(32)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val values = obj["values"] as? JsonObject ?: return@mapNotNull null
        SavedAppearance(name, values.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }.toMap())
    }.distinctBy { it.name }.take(12)
}.getOrDefault(emptyList())
