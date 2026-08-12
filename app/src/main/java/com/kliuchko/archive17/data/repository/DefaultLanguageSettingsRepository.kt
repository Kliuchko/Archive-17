package com.kliuchko.archive17.data.repository

import android.content.Context
import com.kliuchko.archive17.domain.model.BookLanguage
import com.kliuchko.archive17.domain.repository.LanguageSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultLanguageSettingsRepository(context: Context) : LanguageSettingsRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _preferredBookLanguage = MutableStateFlow(readBookLanguage())

    override val preferredBookLanguage: StateFlow<BookLanguage> =
        _preferredBookLanguage.asStateFlow()

    override fun setPreferredBookLanguage(language: BookLanguage) {
        preferences.edit().putString(KEY_BOOK_LANGUAGE, language.name).apply()
        _preferredBookLanguage.value = language
    }

    private fun readBookLanguage(): BookLanguage =
        preferences.getString(KEY_BOOK_LANGUAGE, null)
            ?.let { stored -> BookLanguage.entries.firstOrNull { it.name == stored } }
            ?: BookLanguage.DEVICE

    private companion object {
        const val PREFERENCES_NAME = "language_settings"
        const val KEY_BOOK_LANGUAGE = "preferred_book_language"
    }
}
