package com.kliuchko.archive17.domain.model

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
}

enum class BookLanguage(val languageTag: String?) {
    DEVICE(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
}
