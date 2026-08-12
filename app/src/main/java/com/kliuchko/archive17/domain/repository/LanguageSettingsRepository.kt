package com.kliuchko.archive17.domain.repository

import com.kliuchko.archive17.domain.model.BookLanguage
import kotlinx.coroutines.flow.StateFlow

interface LanguageSettingsRepository {
    val preferredBookLanguage: StateFlow<BookLanguage>

    fun setPreferredBookLanguage(language: BookLanguage)
}
