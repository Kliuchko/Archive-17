package com.kliuchko.archive17.presentation.profile

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.kliuchko.archive17.domain.model.AppLanguage
import com.kliuchko.archive17.domain.model.BookLanguage
import com.kliuchko.archive17.domain.repository.LanguageSettingsRepository
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    private val languageSettingsRepository: LanguageSettingsRepository,
) : ViewModel() {
    val preferredBookLanguage: StateFlow<BookLanguage> =
        languageSettingsRepository.preferredBookLanguage

    fun currentAppLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return AppLanguage.entries.firstOrNull { it.languageTag == tag }
            ?: AppLanguage.SYSTEM
    }

    fun setAppLanguage(language: AppLanguage) {
        val locales = language.languageTag
            ?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun setPreferredBookLanguage(language: BookLanguage) {
        languageSettingsRepository.setPreferredBookLanguage(language)
    }
}
