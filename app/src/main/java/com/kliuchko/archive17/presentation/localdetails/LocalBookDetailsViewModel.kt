package com.kliuchko.archive17.presentation.localdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kliuchko.archive17.domain.model.ReadingStatus
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalBookDetailsViewModel(
    private val bookId: String,
    private val repository: LocalBookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocalBookDetailsUiState())
    val uiState: StateFlow<LocalBookDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLocalBook(bookId).collect { book ->
                _uiState.update {
                    it.copy(
                        book = book,
                        isLoading = false,
                        errorMessage = if (book == null && !it.isDeleted) {
                            "Книга больше не доступна."
                        } else {
                            it.errorMessage
                        },
                    )
                }
            }
        }
    }

    fun updateMetadata(title: String, author: String?) {
        runOperation(successMessage = "Сведения о книге сохранены.") {
            repository.updateMetadata(bookId, title, author)
        }
    }

    fun updateStatus(status: ReadingStatus) {
        runOperation(successMessage = "Статус книги изменён.") {
            repository.updateReadingStatus(bookId, status)
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            when (val result = repository.deleteLocalBook(bookId)) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isSaving = false, isDeleted = true)
                }
                is RepositoryResult.Cached -> Unit
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun runOperation(
        successMessage: String,
        operation: suspend () -> RepositoryResult<*>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, message = null) }
            when (val result = operation()) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isSaving = false, message = successMessage)
                }
                is RepositoryResult.Cached -> _uiState.update { it.copy(isSaving = false) }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
            }
        }
    }
}
