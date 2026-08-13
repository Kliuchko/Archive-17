package com.kliuchko.archive17.domain.repository

interface EditionSelectionRepository {
    fun selectedEditionId(workId: String): String?

    fun selectEdition(workId: String, editionId: String)
}
