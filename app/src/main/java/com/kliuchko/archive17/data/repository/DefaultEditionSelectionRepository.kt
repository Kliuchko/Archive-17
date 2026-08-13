package com.kliuchko.archive17.data.repository

import android.content.Context
import com.kliuchko.archive17.domain.repository.EditionSelectionRepository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class DefaultEditionSelectionRepository(context: Context) : EditionSelectionRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun selectedEditionId(workId: String): String? = preferences
        .getString(workId.preferenceKey(), null)
        ?.takeIf(String::isNotBlank)

    override fun selectEdition(workId: String, editionId: String) {
        if (workId.isBlank() || editionId.isBlank()) return
        preferences.edit()
            .putString(workId.preferenceKey(), editionId)
            .apply()
    }

    private fun String.preferenceKey(): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val PREFERENCES_NAME = "selected_book_editions"
    }
}
