package app.reelstack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.reelstack.R
import app.reelstack.data.model.Personalization
import app.reelstack.data.model.SubtitleLanguage

@Composable
internal fun SubtitleLanguageSettings(value: Personalization, onChange: (Personalization) -> Unit) {
    ThemeChoice(stringResource(R.string.subtitle_preferred_language), value.preferredSubtitleLanguage,
        SubtitleLanguage.entries, "subtitle-language", { subtitleLanguageLabel(it) }) { language ->
        onChange(value.copy(preferredSubtitleLanguage = language,
            fallbackSubtitleLanguage = value.fallbackSubtitleLanguage.takeUnless { it == language } ?: SubtitleLanguage.NONE))
    }
    if (value.preferredSubtitleLanguage !in setOf(SubtitleLanguage.SERVER, SubtitleLanguage.NONE)) {
        ThemeChoice(stringResource(R.string.subtitle_fallback_language), value.fallbackSubtitleLanguage,
            SubtitleLanguage.entries.filter { it != SubtitleLanguage.SERVER && it != value.preferredSubtitleLanguage },
            "subtitle-fallback", { subtitleLanguageLabel(it) }) {
            onChange(value.copy(fallbackSubtitleLanguage = it))
        }
    }
}

@Composable
private fun subtitleLanguageLabel(language: SubtitleLanguage) = stringResource(when (language) {
    SubtitleLanguage.NORWEGIAN -> R.string.subtitle_language_norwegian
    SubtitleLanguage.ENGLISH -> R.string.subtitle_language_english
    SubtitleLanguage.SWEDISH -> R.string.subtitle_language_swedish
    SubtitleLanguage.DANISH -> R.string.subtitle_language_danish
    SubtitleLanguage.GERMAN -> R.string.subtitle_language_german
    SubtitleLanguage.FRENCH -> R.string.subtitle_language_french
    SubtitleLanguage.SPANISH -> R.string.subtitle_language_spanish
    SubtitleLanguage.SERVER -> R.string.subtitle_language_server
    SubtitleLanguage.NONE -> R.string.player_off
})
