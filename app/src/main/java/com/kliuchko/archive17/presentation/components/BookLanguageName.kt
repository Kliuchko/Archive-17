package com.kliuchko.archive17.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun bookLanguageName(code: String): String {
    val displayLocale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val languageTag = when (code.lowercase(Locale.ROOT)) {
        "eng" -> "en"
        "rus" -> "ru"
        "ukr" -> "uk"
        "ita" -> "it"
        "spa" -> "es"
        "fre", "fra" -> "fr"
        "ger", "deu" -> "de"
        "pol" -> "pl"
        "por" -> "pt"
        "chi", "zho" -> "zh"
        "jpn" -> "ja"
        "ara" -> "ar"
        "kor" -> "ko"
        "tur" -> "tr"
        "dut", "nld" -> "nl"
        "swe" -> "sv"
        "fin" -> "fi"
        "cze", "ces" -> "cs"
        "heb" -> "he"
        "hin" -> "hi"
        "ben" -> "bn"
        else -> code.take(2)
    }
    return Locale.forLanguageTag(languageTag)
        .getDisplayLanguage(displayLocale)
        .replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(displayLocale) else character.toString()
        }
        .ifBlank { code.uppercase(Locale.ROOT) }
}
